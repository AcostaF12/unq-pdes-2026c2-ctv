// Carga sostenida sobre el catálogo: ~20 usuarios buscando durante 2 minutos.
import { sleep } from 'k6';
import { BUYER, loginOrFail } from './lib/common.js';
import { searchCatalog } from './lib/catalog.js';
import { summaryOutputs } from './lib/summary.js';

export const options = {
  scenarios: {
    catalog: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 20),
      duration: __ENV.DURATION || '2m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:GET /packages}': ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
};

// Un único login: todos los VUs comparten el token del comprador (dura 24 h).
export function setup() {
  return { token: loginOrFail(BUYER.username, BUYER.password) };
}

export default function (data) {
  searchCatalog(data.token);
  sleep(0.5 + Math.random()); // tiempo de lectura entre búsquedas
}

export function handleSummary(data) {
  return summaryOutputs('catalog-load', data);
}
