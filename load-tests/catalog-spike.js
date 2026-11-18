// Pico sobre el catálogo: de 5 a 50 usuarios y de vuelta a 5 en ~3 minutos.
import { sleep } from 'k6';
import { BUYER, loginOrFail } from './lib/common.js';
import { searchCatalog } from './lib/catalog.js';
import { summaryOutputs } from './lib/summary.js';

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 5 }, // base
        { duration: '20s', target: 50 }, // subida brusca
        { duration: '1m', target: 50 }, // pico sostenido
        { duration: '20s', target: 5 }, // bajada
        { duration: '30s', target: 5 }, // recuperación
      ],
      gracefulRampDown: '10s',
    },
  },
  // El pico tolera algo de degradación: se exige que casi todo responda y se informa el p95.
  thresholds: {
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
  },
};

export function setup() {
  return { token: loginOrFail(BUYER.username, BUYER.password) };
}

export default function (data) {
  searchCatalog(data.token);
  sleep(0.5 + Math.random());
}

export function handleSummary(data) {
  return summaryOutputs('catalog-spike', data);
}
