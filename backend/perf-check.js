/**
 * NFR-01 Performance Check Script
 * Validates that GET /api/properties search & browse p95 latency is under 300 ms
 * with 50 concurrent users over 500 requests against a local running server.
 *
 * Usage: node backend/perf-check.js
 */

const http = require('http');

const TOTAL_REQUESTS = 500;
const CONCURRENCY = 50;
const URL = 'http://localhost:8080/api/properties?minPrice=500000&maxPrice=3000000&size=12&sort=price,desc';

const latencies = [];
let completed = 0;
let errors = 0;

function sendRequest() {
  const start = Date.now();
  http.get(URL, (res) => {
    res.on('data', () => {});
    res.on('end', () => {
      latencies.push(Date.now() - start);
      completed++;
      if (completed + errors < TOTAL_REQUESTS) {
        sendRequest();
      } else if (completed + errors >= TOTAL_REQUESTS) {
        evaluate();
      }
    });
  }).on('error', (err) => {
    errors++;
    console.error('Request failed:', err.message);
    if (completed + errors >= TOTAL_REQUESTS) {
      evaluate();
    }
  });
}

function evaluate() {
  if (latencies.length === 0) {
    console.error('No successful requests to evaluate.');
    return;
  }

  latencies.sort((a, b) => a - b);
  const p50 = latencies[Math.floor(latencies.length * 0.50)];
  const p95 = latencies[Math.floor(latencies.length * 0.95)];
  const p99 = latencies[Math.floor(latencies.length * 0.99)];

  console.log('\n--- NFR-01 Latency Benchmark (50 Concurrent Users) ---');
  console.log(`Target: p95 < 300 ms on 5,000-10,000 listings`);
  console.log(`Total Requests: ${completed} (Errors: ${errors})`);
  console.log(`p50: ${p50} ms`);
  console.log(`p95: ${p95} ms`);
  console.log(`p99: ${p99} ms`);
  console.log(`Outcome: ${p95 < 300 ? 'PASSED ✅' : 'FAILED ❌'}\n`);
}

console.log(`Starting benchmark against ${URL} with ${CONCURRENCY} workers...`);
for (let i = 0; i < CONCURRENCY; i++) {
  sendRequest();
}
