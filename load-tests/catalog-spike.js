// Pico sobre el catálogo: de 5 a 50 usuarios y de vuelta a 5 en ~3 minutos.
import { sleep } from 'k6';
import { BUYER, loginOrFail } from './lib/common.js';
import { searchCatalog } from './lib/catalog.js';
import { summaryOutputs } from './lib/summary.js';

// Para una prueba corta: BASE_VUS, PEAK_VUS y TIME_SCALE (factor sobre las duraciones, 1 = ~3 min).
const BASE_VUS = Number(__ENV.BASE_VUS || 5);
const PEAK_VUS = Number(__ENV.PEAK_VUS || 50);
const TIME_SCALE = Number(__ENV.TIME_SCALE || 1);
const seconds = (value) => `${Math.max(1, Math.round(value * TIME_SCALE))}s`;

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: BASE_VUS,
      stages: [
        { duration: seconds(30), target: BASE_VUS }, // base
        { duration: seconds(20), target: PEAK_VUS }, // subida brusca
        { duration: seconds(60), target: PEAK_VUS }, // pico sostenido
        { duration: seconds(20), target: BASE_VUS }, // bajada
        { duration: seconds(30), target: BASE_VUS }, // recuperación
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
