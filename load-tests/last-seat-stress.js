// Stress del último asiento: ~30 compradores compran a la vez un paquete con 5 lugares.
// Prueba que el lock pesimista (paquete en el backend, vuelo en flights-service) no sobrevende:
// exactamente 5 compras deben salir 201, el resto 400 y ninguna 5xx.
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { ADMIN, BASE_URL, loginOrFail, params, requestOrFail } from './lib/common.js';
import { summaryOutputs } from './lib/summary.js';

const SEATS = Number(__ENV.SEATS || 5);
const BUYERS = Number(__ENV.BUYERS || 30);
const DESTINATION = 'PAR';
const ORIGIN = 'BUE';

const succeeded = new Counter('purchases_succeeded');
const rejected = new Counter('purchases_rejected');
const unexpected = new Counter('purchases_unexpected');
const seatsLeft = new Counter('seats_left_after');

export const options = {
  setupTimeout: '3m',
  scenarios: {
    // Todos los VUs arrancan juntos y cada uno hace una sola compra con su propio comprador.
    purchase: {
      executor: 'per-vu-iterations',
      vus: BUYERS,
      iterations: 1,
      maxDuration: '45s',
      exec: 'purchase',
    },
    // Al terminar, revisa el stock de los dos vuelos en flights-service.
    verify: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      startTime: '50s',
      exec: 'verify',
    },
  },
  thresholds: {
    purchases_succeeded: [`count==${SEATS}`],
    purchases_rejected: [`count==${BUYERS - SEATS}`],
    purchases_unexpected: ['count==0'],
    seats_left_after: ['count==0'],
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
  const runId = `${Date.now().toString(36)}${Math.floor(Math.random() * 1296).toString(36)}`;
  const adminToken = loginOrFail(ADMIN.username, ADMIN.password);

  // Vuelos propios de esta corrida, con fecha futura para que el paquete se pueda comprar.
  const offset = 120 + Math.floor(Math.random() * 180);
  const outboundFlightId = createFlight(adminToken, ORIGIN, DESTINATION, isoDate(offset));
  const returnFlightId = createFlight(adminToken, DESTINATION, ORIGIN, isoDate(offset + 7));

  const hotels = requestOrFail('GET', '/hotels', null, adminToken, 200, 'GET /hotels').json();
  const hotel = hotels.find((h) => h.city && h.city.code === DESTINATION);
  const agencies = requestOrFail('GET', '/agencies', null, adminToken, 200, 'GET /agencies').json();
  const agency = agencies.find((a) => a.active);
  if (!hotel || !agency) {
    exec.test.abort(`Falta un hotel en ${DESTINATION} o una agencia activa en el seed.`);
  }

  const travelPackage = requestOrFail(
    'POST',
    '/packages',
    {
      name: `k6 último asiento ${runId}`,
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

  // Un comprador nuevo por VU, con nombre único por corrida.
  const buyerTokens = [];
  for (let i = 0; i < BUYERS; i += 1) {
    const res = requestOrFail(
      'POST',
      '/auth/register',
      { username: `k6-${runId}-${i}`, password: 'k6-load-test', firstName: 'Carga', lastName: `K6 ${i}` },
      null,
      201,
      'POST /auth/register',
    );
    buyerTokens.push(res.json('token'));
  }

  console.log(`Paquete ${travelPackage.id} (${agency.name}, ${hotel.name}), vuelos ${outboundFlightId}/${returnFlightId}`);
  return { adminToken, packageId: travelPackage.id, outboundFlightId, returnFlightId, buyerTokens };
}

export function purchase(data) {
  const token = data.buyerTokens[exec.vu.idInTest - 1];
  // 400 es la respuesta esperada cuando no queda lugar, así que no cuenta como request fallido.
  const res = http.post(
    `${BASE_URL}/purchases`,
    JSON.stringify({ packageId: data.packageId }),
    params('POST /purchases', token, { responseCallback: http.expectedStatuses(201, 400) }),
  );
  succeeded.add(res.status === 201 ? 1 : 0);
  rejected.add(res.status === 400 ? 1 : 0);
  unexpected.add(res.status !== 201 && res.status !== 400 ? 1 : 0);
  check(res, { 'compra responde 201 o 400': (r) => r.status === 201 || r.status === 400 });
}

export function verify(data) {
  [data.outboundFlightId, data.returnFlightId].forEach((id) => {
    const res = http.get(`${BASE_URL}/flights/${id}`, params('GET /flights/{id}', data.adminToken));
    const ok = check(res, { 'vuelo consultable': (r) => r.status === 200 });
    // Si se vendieron los 5 asientos de ida y de vuelta, no queda ninguno libre.
    seatsLeft.add(ok ? res.json('availability') : SEATS);
  });
}

// Archiva el paquete para que no quede en el catálogo de la demo.
export function teardown(data) {
  const res = http.put(
    `${BASE_URL}/packages/${data.packageId}/status`,
    JSON.stringify({ active: false }),
    params('PUT /packages/{id}/status', data.adminToken),
  );
  check(res, { 'paquete archivado': (r) => r.status === 200 });
}

export function handleSummary(data) {
  return summaryOutputs('last-seat-stress', data);
}
