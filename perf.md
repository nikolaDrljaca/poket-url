```
# 10_000 requests from 500 connections on the write path
hey -n 10000 -c 500  -m POST -H "Content-Type: application/json" -d '{"url": "https://www.example.com/some/long/path"}' http://localhost:5000/shorten | tee -a poketurl-results.txt

docker compose up -d --build
```

```
# Warmup — confirm baseline still holds
hey -n 10000 -c 10 ...

# Where you currently saturate
hey -n 10000 -c 500 ...

# Push beyond current ceiling
hey -n 50000 -c 1000 ...

# Sustained load — tests queue backpressure and GC over time
hey -n 100000 -c 500 ...

# Sustain 1500 open connections over 60 seconds
hey -z 60s -c 1500 ...
```
