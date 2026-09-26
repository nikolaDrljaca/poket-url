import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// ---- Config ----------------------------------------------------------
const BASE_URL = 'http://0.0.0.0:5000';
const SEED_URL_COUNT = 50; // how many short URLs to pre-create for read traffic

// ---- Custom metrics ----------------------------------------------------
const shortenDuration = new Trend('shorten_duration', true);
const redirectDuration = new Trend('redirect_duration', true);
const redirectFailRate = new Rate('redirect_fail_rate');

// ---- Sample data for generating "random" long URLs -----------------
const sampleDomains = new SharedArray('domains', function() {
    return [
        'https://example.com/articles',
        'https://news.example.org/story',
        'https://blog.example.net/post',
        'https://docs.example.io/guide',
        'https://shop.example.com/product',
    ];
});

function randomLongUrl() {
    const domain = sampleDomains[Math.floor(Math.random() * sampleDomains.length)];
    const id = Math.floor(Math.random() * 1e9);
    return `${domain}/${id}`;
}

// ---- Load profile ------------------------------------------------------
export const options = {
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    scenarios: {
        typical_usage: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 200 },   // warm up
                { duration: '2m', target: 6000 },   // ramp to expected load
                { duration: '5m', target: 6000 },   // sustain
                { duration: '30s', target: 0 },    // ramp down
            ],
            gracefulRampDown: '10s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        'shorten_duration': ['p(95)<500', 'p(99)<1000'],
        'redirect_duration': ['p(95)<300', 'p(99)<1000'],
        'redirect_fail_rate': ['rate<0.01'],
    },
};

// ---- Setup: seed the system with existing short URLs to visit -------
export function setup() {
    const codes = [];

    for (let i = 0; i < SEED_URL_COUNT; i++) {
        const payload = JSON.stringify({ url: randomLongUrl() });
        const params = { headers: { 'Content-Type': 'application/json' } };
        const res = http.post(`${BASE_URL}/shorten`, payload, params);

        if (res.status === 201) {
            try {
                const body = JSON.parse(res.body);
                if (body.key) codes.push(body.key);
            } catch (e) {
                // ignore malformed seed response, continue
                console.log(`JSON parse failed: ${e}`);
            }
        }
    }

    if (codes.length === 0) {
        throw new Error('Setup failed: could not seed any short URLs. Is the API reachable at ' + BASE_URL + '?');
    }

    return { seedCodes: codes };
}

// ---- VU behavior: create 1, visit 10 others over ~12s ----------------
export default function(data) {
    // Local pool for this iteration: seeded codes + whatever this VU creates
    const localCodes = data.seedCodes.slice();

    // 1) Create a short URL
    const payload = JSON.stringify({ url: randomLongUrl() });
    const params = { headers: { 'Content-Type': 'application/json' } };

    const shortenRes = http.post(`${BASE_URL}/shorten`, payload, params, {
        tags: { name: 'ShortenURL' },
    });
    shortenDuration.add(shortenRes.timings.duration);

    const shortenOk = check(shortenRes, {
        'shorten status is 201': (r) => r.status === 201,
        'shorten response has key': (r) => {
            try {
                return !!JSON.parse(r.body).key;
            } catch (e) {
                return false;
            }
        },
    });

    if (shortenOk) {
        try {
            const body = JSON.parse(shortenRes.body);
            localCodes.push(body.key);
        } catch (e) {
            // ignore
        }
    }

    // Brief pause before the user starts clicking through links
    sleep(randomBetween(0.2, 0.6));

    // 2) Visit 10 short URLs, ~1.1-1.3s apart (viewing time), targeting ~12s total
    const visitsToMake = 10;
    for (let i = 0; i < visitsToMake; i++) {
        const code = localCodes[Math.floor(Math.random() * localCodes.length)];

        const redirectRes = http.get(`${BASE_URL}/r/${code}`, {
            redirects: 0, // don't auto-follow, we just want to verify the 302
            tags: { name: 'ResolveShortURL' },
        });
        redirectDuration.add(redirectRes.timings.duration);

        const redirectOk = check(redirectRes, {
            'redirect status is 302': (r) => r.status === 302,
            'redirect has Location header': (r) => !!r.headers['Location'],
        });
        redirectFailRate.add(!redirectOk);

        // Simulated "time spent viewing the destination page"
        sleep(randomBetween(0.9, 1.3));
    }
}

function randomBetween(min, max) {
    return Math.random() * (max - min) + min;
}
