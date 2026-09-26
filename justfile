up:
    @echo 'Starting local stack.'
    docker compose -f docker-compose.local.yaml up -d --build

down:
    @echo 'Stopping local stack.'
    docker compose -f docker-compose.local.yaml down

load-test:
    @echo 'Running load test.'
    k6 run perf/load-test.js
