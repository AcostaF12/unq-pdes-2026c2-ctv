// Throughput de compra: varios compradores compran a la vez un paquete con lugares de sobra.
// No es el caso del último asiento: todas las compras tienen que salir 201 y no puede haber sobreventa.
// El p95 de POST /purchases queda en el resumen para ver la cola del lock, sin un techo copiado del catálogo.
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { ADMIN, BASE_URL, loginOrFail, params, queryString, requestOrFail } from './lib/common.js';
import { summaryOutputs } from './lib/summary.js';

const SEATS = Number(__ENV.SEATS || 20);
const BUYERS = Number(__ENV.BUYERS || 10);
const DESTINATION = 'PAR';
const ORIGIN = 'BUE';
const EXPECTED_LEFT = SEATS - BUYERS;

const succeeded = new Counter('purchases_succeeded');
const unexpected = new Counter('purchases_unexpected');
const seatsMismatch = new Counter('seats_mismatch');
const purchaseDuration = new Trend('purchase_duration', true);

export const options = {
  setupTimeout: '3m',
  scenarios: {
    purchase: {
      executor: 'per-vu-iterations',
      vus: BUYERS,
      iterations: 1,
      maxDuration: '12s',
      gracefulStop: '2s',
      exec: 'purchase',
    },
  },
  thresholds: {
    purchases_succeeded: [`count==${BUYERS}`],
    purchases_unexpected: ['count==0'],
    seats_mismatch: ['count==0'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate==1'],
  },
};

function isoDate(daysFromToday) {
  const date = new Date(Date.now() + daysFromToday * 24 * 60 * 60 * 1000);
  return date.toISOString().slice(0, 10);
}

function createFlight(token, origin, destination, flightDate) {
  const body = {
    airline: 'k6 Load Test',
    flightDate,
    departureTime: '10:00:00',
    origin,
    destination,
    capacity: SEATS,
    availability: SEATS,
  };
  return requestOrFail('POST', '/flights', body, token, 201, 'POST /flights').json('id');
}

export function setup() {
  if (SEATS < BUYERS) {
    exec.test.abort(`SEATS (${SEATS}) tiene que ser >= BUYERS (${BUYERS}): este escenario no prueba el último asiento.`);
  }

  const runId = `${Date.now().toString(36)}${Math.floor(Math.random() * 1296).toString(36)}`;
  const adminToken = loginOrFail(ADMIN.username, ADMIN.password);

  const offset = 400 + Math.floor(Math.random() * 180);
  const outboundFlightId = createFlight(adminToken, ORIGIN, DESTINATION, isoDate(offset));
  const returnFlightId = createFlight(adminToken, DESTINATION, ORIGIN, isoDate(offset + 7));

  const hotels = requestOrFail('GET', '/hotels', null, adminToken, 200, 'GET /hotels').json();
  const hotel = hotels.find((h) => h.city && h.city.code === DESTINATION);
  const agencies = requestOrFail('GET', '/agencies', null, adminToken, 200, 'GET /agencies').json();
  const agency = agencies.find((a) => a.active);
  if (!hotel || !agency) {
    exec.test.abort(`Falta un hotel en ${DESTINATION} o una agencia activa en el seed.`);
  }

  const packageName = `k6 throughput ${runId}`;
  const travelPackage = requestOrFail(
    'POST',
    '/packages',
    {
      name: packageName,
      hotelId: hotel.id,
      outboundFlightId,
      returnFlightId,
      price: 1000,
      agencyId: agency.id,
    },
    adminToken,
    201,
    'POST /packages',
  ).json();

  const buyerTokens = [];
  for (let i = 0; i < BUYERS; i += 1) {
    const res = requestOrFail(
      'POST',
      '/auth/register',
      { username: `k6t-${runId}-${i}`, password: 'k6-load-test', firstName: 'Carga', lastName: `K6 ${i}` },
      null,
      201,
      'POST /auth/register',
    );
    buyerTokens.push(res.json('token'));
  }

  console.log(
    `Paquete ${travelPackage.id} con ${SEATS} lugares para ${BUYERS} compradores, vuelos ${outboundFlightId}/${returnFlightId}`,
  );
  return { adminToken, packageId: travelPackage.id, packageName, outboundFlightId, returnFlightId, buyerTokens };
}

export function purchase(data) {
  const token = data.buyerTokens[exec.scenario.iterationInTest];
  const res = http.post(
    `${BASE_URL}/purchases`,
    JSON.stringify({ packageId: data.packageId }),
    params('POST /purchases', token, { responseCallback: http.expectedStatuses(201) }),
  );
  purchaseDuration.add(res.timings.duration);
  succeeded.add(res.status === 201 ? 1 : 0);
  const isUnexpected = res.status !== 201;
  unexpected.add(isUnexpected ? 1 : 0);
  if (isUnexpected) {
    console.warn(`Compra con respuesta inesperada: ${res.status} ${res.error || ''} ${res.body}`);
  }
  check(res, { 'compra responde 201': (r) => r.status === 201 });
}

export function verify(data) {
  [data.outboundFlightId, data.returnFlightId].forEach((id) => {
    const availability = flightAvailability(id, data.adminToken);
    seatsMismatch.add(availability === EXPECTED_LEFT ? 0 : 1);
  });
}

function flightAvailability(id, token) {
  const res = http.get(`${BASE_URL}/flights/${id}`, params('GET /flights/{id}', token));
  return check(res, { 'vuelo consultable': (r) => r.status === 200 }) ? res.json('availability') : null;
}

// Cancela las compras de la corrida, devuelve los lugares y archiva el paquete.
export function teardown(data) {
  if (!data || !data.packageId) {
    return;
  }
  verify(data);
  const sales = http.get(
    `${BASE_URL}/admin/sales?${queryString({ packageName: data.packageName, size: 100 })}`,
    params('GET /admin/sales', data.adminToken),
  );
  check(sales, { 'ventas de la corrida consultables': (r) => r.status === 200 });
  const purchases =
    sales.status === 200
      ? sales.json().content.filter((p) => p.travelPackage.id === data.packageId && p.status === 'ACTIVE')
      : [];
  check(purchases, { [`hay ${BUYERS} compras para cancelar`]: (p) => p.length === BUYERS });

  purchases.forEach((p) => {
    const res = http.del(`${BASE_URL}/purchases/${p.id}`, null, params('DELETE /purchases/{id}', data.adminToken));
    check(res, { 'compra cancelada (204)': (r) => r.status === 204 });
  });

  [data.outboundFlightId, data.returnFlightId].forEach((id) => {
    const availability = flightAvailability(id, data.adminToken);
    check(availability, { [`vuelo con sus ${SEATS} lugares libres tras cancelar`]: (a) => a === SEATS });
  });

  const res = http.put(
    `${BASE_URL}/packages/${data.packageId}/status`,
    JSON.stringify({ active: false }),
    params('PUT /packages/{id}/status', data.adminToken),
  );
  check(res, { 'paquete archivado': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return summaryOutputs('purchase-throughput', data);
}
