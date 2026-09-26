# Notes

```
-- k6 results
-- 2 cpu cores, 256mb memory
█ THRESHOLDS

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%

    redirect_duration
    ✓ 'p(95)<300' p(95)=174.66ms
    ✓ 'p(99)<1000' p(99)=749.64ms

    redirect_fail_rate
    ✓ 'rate<0.01' rate=0.00%

    shorten_duration
    ✗ 'p(95)<500' p(95)=628.25ms
    ✗ 'p(99)<1000' p(99)=1.53s


  █ TOTAL RESULTS

    checks_total.......: 3516554 7162.732396/s
    checks_succeeded...: 100.00% 3516554 out of 3516554
    checks_failed......: 0.00%   0 out of 3516554

    ✓ shorten status is 201
    ✓ shorten response has key
    ✓ redirect status is 302
    ✓ redirect has Location header

    CUSTOM
    redirect_duration..............: avg=47.59ms  min=130.88µs med=11.55ms max=1.58s  p(90)=105ms    p(95)=174.66ms p(99)=749.64ms
    redirect_fail_rate.............: 0.00%   0 out of 1598415
    shorten_duration...............: avg=123.01ms min=624.42µs med=30.36ms max=6.39s  p(90)=270.05ms p(95)=628.25ms p(99)=1.53s

    HTTP
    http_req_duration..............: avg=54.45ms  min=130.88µs med=13.1ms  max=6.39s  p(90)=116.83ms p(95)=197.57ms p(99)=844.1ms
      { expected_response:true }...: avg=54.45ms  min=130.88µs med=13.1ms  max=6.39s  p(90)=116.83ms p(95)=197.57ms p(99)=844.1ms
    http_req_failed................: 0.00%   0 out of 1758327
    http_reqs......................: 1758327 3581.468041/s

    EXECUTION
    iteration_duration.............: avg=12s      min=10.01s   med=11.93s  max=18.93s p(90)=12.83s   p(95)=13.16s   p(99)=13.87s
    iterations.....................: 159187  324.241824/s
    vus............................: 4       min=0            max=5000
    vus_max........................: 5000    min=5000         max=5000

    NETWORK
    data_received..................: 178 MB  363 kB/s
    data_sent......................: 153 MB  312 kB/s
```

Next available (hyper)optimizations:
1. use connection pooling, separate read from write, only single writer
2. Batch inserts
3. Increase to 512mb to let the jvm breathe


