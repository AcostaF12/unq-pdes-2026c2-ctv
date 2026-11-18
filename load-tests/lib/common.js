// Helpers compartidos por los escenarios de carga. Solo usan módulos de k6, sin dependencias externas.
import http from 'k6/http';
import { fail } from 'k6';

export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');

// Usuarios de demo del seed (docs/seed-y-ciclo-de-vida.md). Se pueden pisar por variable de entorno.
export const BUYER = {
  username: __ENV.BUYER_USER || 'buyer',
  password: __ENV.BUYER_PASSWORD || 'buyer123',
};
export const ADMIN = {
  username: __ENV.ADMIN_USER || 'vferreyra',
  password: __ENV.ADMIN_PASSWORD || 'vferreyra',
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// Parámetros de un request: JSON, token opcional y un tag "name" para agrupar las métricas por endpoint.
export function params(name, token, extra = {}) {
  const headers = token ? { ...JSON_HEADERS, Authorization: `Bearer ${token}` } : JSON_HEADERS;
  return { headers, tags: { name }, ...extra };
}

export function login(username, password) {
  return http.post(`${BASE_URL}/auth/login`, JSON.stringify({ username, password }), params('POST /auth/login'));
}

// Login que corta el test si falla: se usa en setup(), donde sin token no tiene sentido seguir.
export function loginOrFail(username, password) {
  const res = login(username, password);
  if (res.status !== 200) {
    fail(`El login de ${username} contra ${BASE_URL} devolvió ${res.status}: ${res.body}`);
  }
  return res.json('token');
}

// POST/GET que cortan el test si la respuesta no es la esperada. Para preparar datos en setup().
export function requestOrFail(method, path, body, token, expectedStatus, name) {
  const payload = body === null ? null : JSON.stringify(body);
  const res = http.request(method, `${BASE_URL}${path}`, payload, params(name || `${method} ${path}`, token));
  if (res.status !== expectedStatus) {
    fail(`${method} ${path} devolvió ${res.status} (se esperaba ${expectedStatus}): ${res.body}`);
  }
  return res;
}

export function pick(values) {
  return values[Math.floor(Math.random() * values.length)];
}

export function queryString(values) {
  return Object.keys(values)
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(values[key])}`)
    .join('&');
}
