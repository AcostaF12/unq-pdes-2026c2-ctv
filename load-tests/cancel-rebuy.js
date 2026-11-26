// Cancela y vuelve a comprar el mismo paquete al mismo tiempo.
// Arranca lleno. Cada lugar se libera y otro comprador intenta tomarlo.
// El stock final de los dos vuelos tiene que coincidir con las compras que siguen activas.
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, params } from './lib/common.js';
import {
  activePurchases,
  cancelActiveAndArchive,
  flightAvailability,
  prepareRoundTrip,
  purchasePackage,
} from './lib/round-trip.js';
import { summaryOutputs } from './lib/summary.js';

const SEATS = Number(__ENV.SEATS || 5);

const cancelsSucceeded = new Counter('cancels_succeeded');
const cancelsUnexpected = new Counter('cancels_unexpected');
const purchasesSucceeded = new Counter('purchases_succeeded');
const purchasesRejected = new Counter('purchases_rejected');
const purchasesUnexpected = new Counter('purchases_unexpected');
const rebuyFinished = new Counter('rebuy_finished');
const seatsMismatch = new Counter('seats_mismatch');
const cancelDuration = new Trend('cancel_duration', true);
const rebuyDuration = new Trend('rebuy_duration', true);

export const options = {
  setupTimeout: '3m',
  scenarios: {
    cancel: {
      executor: 'per-vu-iterations',
      vus: SEATS,
      iterations: 1,
      maxDuration: '12s',
      gracefulStop: '2s',
      exec: 'cancel',
    },
    rebuy: {
      executor: 'per-vu-iterations',
      vus: SEATS,
      iterations: 1,
      maxDuration: '12s',
      gracefulStop: '2s',
      exec: 'rebuy',
    },
  },
  thresholds: {
    cancels_succeeded: [`count==${SEATS}`],
    cancels_unexpected: ['count==0'],
    purchases_unexpected: ['count==0'],
    rebuy_finished: [`count==${SEATS}`],
    seats_mismatch: ['count==0'],
    checks: ['rate==1'],
  },
};

export function setup() {
  if (!Number.isInteger(SEATS) || SEATS < 1) {
    exec.test.abort(`SEATS (${SEATS}) tiene que ser un entero mayor a cero.`);
  }
  const data = prepareRoundTrip({
    seats: SEATS,
    buyerCount: SEATS * 2,
    packagePrefix: 'k6 cancel-rebuy',
    usernamePrefix: 'k6cr',
    dayOffset: 700,
  });
  const purchaseIds = [];
  for (let i = 0; i < SEATS; i += 1) {
    purchaseIds.push(purchasePackage(data.packageId, data.buyerTokens[i]).json('id'));
  }
  console.log(`Paquete ${data.packageId} lleno (${SEATS}). ${SEATS} cancelan y ${SEATS} recompran.`);
  return { ...data, purchaseIds };
}

export function cancel(data) {
  const purchaseId = data.purchaseIds[exec.scenario.iterationInTest];
  const token = data.buyerTokens[exec.scenario.iterationInTest];
  const res = http.del(
    `${BASE_URL}/purchases/${purchaseId}`,
    null,
    params('DELETE /purchases/{id}', token, { responseCallback: http.expectedStatuses(204, 400) }),
  );
  cancelDuration.add(res.timings.duration);
  cancelsSucceeded.add(res.status === 204 ? 1 : 0);
  const isUnexpected = res.status !== 204 && res.status !== 400;
  cancelsUnexpected.add(isUnexpected ? 1 : 0);
  if (isUnexpected) {
    console.warn(`Cancelación inesperada: ${res.status} ${res.error || ''} ${res.body}`);
  }
  check(res, { 'cancelación responde 204': (response) => response.status === 204 });
}

export function rebuy(data) {
  const token = data.buyerTokens[SEATS + exec.scenario.iterationInTest];
  const res = http.post(
    `${BASE_URL}/purchases`,
    JSON.stringify({ packageId: data.packageId }),
    params('POST /purchases', token, { responseCallback: http.expectedStatuses(201, 400) }),
  );
  rebuyDuration.add(res.timings.duration);
  purchasesSucceeded.add(res.status === 201 ? 1 : 0);
  purchasesRejected.add(res.status === 400 ? 1 : 0);
  rebuyFinished.add(res.status === 201 || res.status === 400 ? 1 : 0);
  const isUnexpected = res.status !== 201 && res.status !== 400;
  purchasesUnexpected.add(isUnexpected ? 1 : 0);
  if (isUnexpected) {
    console.warn(`Recompra inesperada: ${res.status} ${res.error || ''} ${res.body}`);
  }
  check(res, { 'recompra responde 201 o 400': (response) => response.status === 201 || response.status === 400 });
}

export function verify(data) {
  const active = activePurchases(data);
  const outbound = flightAvailability(data.outboundFlightId, data.adminToken);
  const inbound = flightAvailability(data.returnFlightId, data.adminToken);
  const expected = active === null ? null : data.seats - active.length;
  const matches = active !== null && outbound === expected && inbound === expected && outbound >= 0;
  seatsMismatch.add(matches ? 0 : 1);
  check(matches, { 'stock igual en ida y vuelta, sin sobreventa': (value) => value === true });
}

export function teardown(data) {
  if (!data || !data.packageId) {
    return;
  }
  verify(data);
  cancelActiveAndArchive(data);
}

export function handleSummary(data) {
  return summaryOutputs('cancel-rebuy', data);
}
