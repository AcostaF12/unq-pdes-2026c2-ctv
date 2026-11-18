// Carga sobre el login: ~10 usuarios iniciando sesión durante 1 minuto.
// Cada login verifica la contraseña con BCrypt, que es caro en CPU a propósito.
import { check, sleep } from 'k6';
import { BUYER, login, pick } from './lib/common.js';
import { summaryOutputs } from './lib/summary.js';

// Compradores del seed; todos usan la misma contraseña de demo.
const USERS = [BUYER.username, 'buyer2'];

export const options = {
  scenarios: {
    login: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:POST /auth/login}': ['p(95)<1500'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const res = login(pick(USERS), BUYER.password);
  check(res, {
    'login responde 200': (r) => r.status === 200,
    'login devuelve token': (r) => r.status === 200 && typeof r.json('token') === 'string',
  });
  sleep(0.5 + Math.random() * 0.5);
}

export function handleSummary(data) {
  return summaryOutputs('login-load', data);
}
