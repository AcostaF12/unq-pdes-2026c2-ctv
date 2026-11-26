// Varios pedidos cancelan la misma compra al mismo tiempo.
// Tiene que ganar uno solo: un 204, el resto 400, y cada vuelo recupera un único lugar.
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, params } from './lib/common.js';
import {
  cancelActiveAndArchive,
  flightAvailability,
  prepareRoundTrip,
  purchasePackage,
} from './lib/round-trip.js';
import { summaryOutputs } from './lib/summary.js';

const ATTEMPTS = Number(__ENV.ATTEMPTS || 20);
const SEATS = 1;

const cancelsSucceeded = new Counter('cancels_succeeded');
const cancelsRejected = new Counter('cancels_rejected');
const cancelsUnexpected = new Counter('cancels_unexpected');
const seatsMismatch = new Counter('seats_mismatch');
const cancelDuration = new Trend('cancel_duration', true);

export const options = {
  setupTimeout: '3m',
  scenarios: {
    cancel: {
      executor: 'per-vu-iterations',
      vus: ATTEMPTS,
      iterations: 1,
      maxDuration: '10s',
      gracefulStop: '2s',
      exec: 'cancel',
    },
  },
  thresholds: {
    cancels_succeeded: ['count==1'],
    cancels_rejected: [`count==${ATTEMPTS - 1}`],
    cancels_unexpected: ['count==0'],
    seats_mismatch: ['count==0'],
    checks: ['rate==1'],
  },
};

export function setup() {
  if (!Number.isInteger(ATTEMPTS) || ATTEMPTS < 2) {
    exec.test.abort(`ATTEMPTS (${ATTEMPTS}) tiene que ser un entero mayor a uno.`);
  }
  const data = prepareRoundTrip({
    seats: SEATS,
    buyerCount: 1,
    packagePrefix: 'k6 double-cancel',
    usernamePrefix: 'k6dc',
    dayOffset: 1000,
  });
  const purchaseId = purchasePackage(data.packageId, data.buyerTokens[0]).json('id');
  console.log(`Compra ${purchaseId} con ${ATTEMPTS} cancelaciones simultáneas.`);
  return { ...data, purchaseId };
}

export function cancel(data) {
  const res = http.del(
    `${BASE_URL}/purchases/${data.purchaseId}`,
    null,
    params('DELETE /purchases/{id}', data.adminToken, { responseCallback: http.expectedStatuses(204, 400) }),
  );
  cancelDuration.add(res.timings.duration);
  cancelsSucceeded.add(res.status === 204 ? 1 : 0);
  cancelsRejected.add(res.status === 400 ? 1 : 0);
  const isUnexpected = res.status !== 204 && res.status !== 400;
  cancelsUnexpected.add(isUnexpected ? 1 : 0);
  if (isUnexpected) {
    console.warn(`Cancelación inesperada: ${res.status} ${res.error || ''} ${res.body}`);
  }
  check(res, { 'cancelación responde 204 o 400': (response) => response.status === 204 || response.status === 400 });
}

export function verify(data) {
  [data.outboundFlightId, data.returnFlightId].forEach((id) => {
    const availability = flightAvailability(id, data.adminToken);
    seatsMismatch.add(availability === SEATS ? 0 : 1);
    check(availability, { 'el vuelo recuperó su único lugar': (value) => value === SEATS });
  });
}

export function teardown(data) {
  if (!data || !data.packageId) {
    return;
  }
  verify(data);
  cancelActiveAndArchive(data);
}

export function handleSummary(data) {
  return summaryOutputs('double-cancel', data);
}
