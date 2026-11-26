// Arma un paquete ida y vuelta propio de una corrida de k6, con compradores nuevos.
// Los escenarios de concurrencia lo usan para no pisar el catálogo de la demo.
import exec from 'k6/execution';
import http from 'k6/http';
import { check } from 'k6';
import { ADMIN, BASE_URL, loginOrFail, params, queryString, requestOrFail } from './common.js';

const DESTINATION = 'PAR';
const ORIGIN = 'BUE';

export function isoDate(daysFromToday) {
  const date = new Date(Date.now() + daysFromToday * 24 * 60 * 60 * 1000);
  return date.toISOString().slice(0, 10);
}

function createFlight(token, origin, destination, flightDate, seats) {
  return requestOrFail(
    'POST',
    '/flights',
    {
      airline: 'k6 Load Test',
      flightDate,
      departureTime: '10:00:00',
      origin,
      destination,
      capacity: seats,
      availability: seats,
    },
    token,
    201,
    'POST /flights',
  ).json('id');
}

export function prepareRoundTrip({ seats, buyerCount, packagePrefix, usernamePrefix, dayOffset }) {
  const runId = `${Date.now().toString(36)}${Math.floor(Math.random() * 1296).toString(36)}`;
  const adminToken = loginOrFail(ADMIN.username, ADMIN.password);
  const offset = dayOffset + Math.floor(Math.random() * 180);
  const outboundFlightId = createFlight(adminToken, ORIGIN, DESTINATION, isoDate(offset), seats);
  const returnFlightId = createFlight(adminToken, DESTINATION, ORIGIN, isoDate(offset + 7), seats);

  const hotels = requestOrFail('GET', '/hotels', null, adminToken, 200, 'GET /hotels').json();
  const hotel = hotels.find((item) => item.city && item.city.code === DESTINATION);
  const agencies = requestOrFail('GET', '/agencies', null, adminToken, 200, 'GET /agencies').json();
  const agency = agencies.find((item) => item.active);
  if (!hotel || !agency) {
    exec.test.abort(`Falta un hotel en ${DESTINATION} o una agencia activa en el seed.`);
  }

  const packageName = `${packagePrefix} ${runId}`;
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
  for (let i = 0; i < buyerCount; i += 1) {
    const res = requestOrFail(
      'POST',
      '/auth/register',
      {
        username: `${usernamePrefix}-${runId}-${i}`,
        password: 'k6-load-test',
        firstName: 'Carga',
        lastName: `K6 ${i}`,
      },
      null,
      201,
      'POST /auth/register',
    );
    buyerTokens.push(res.json('token'));
  }

  return {
    adminToken,
    packageId: travelPackage.id,
    packageName,
    outboundFlightId,
    returnFlightId,
    buyerTokens,
    seats,
  };
}

export function purchasePackage(packageId, token) {
  return requestOrFail(
    'POST',
    '/purchases',
    { packageId },
    token,
    201,
    'POST /purchases',
  );
}

export function flightAvailability(id, token) {
  const res = http.get(`${BASE_URL}/flights/${id}`, params('GET /flights/{id}', token));
  return check(res, { 'vuelo consultable': (response) => response.status === 200 })
    ? res.json('availability')
    : null;
}

export function activePurchases(data) {
  const sales = http.get(
    `${BASE_URL}/admin/sales?${queryString({ packageName: data.packageName, size: 100 })}`,
    params('GET /admin/sales', data.adminToken),
  );
  if (!check(sales, { 'ventas de la corrida consultables': (response) => response.status === 200 })) {
    return null;
  }
  return sales.json().content.filter((purchase) => purchase.travelPackage.id === data.packageId && purchase.status === 'ACTIVE');
}

export function cancelActiveAndArchive(data) {
  if (!data || !data.packageId) {
    return;
  }
  const purchases = activePurchases(data) || [];
  purchases.forEach((purchase) => {
    const res = http.del(
      `${BASE_URL}/purchases/${purchase.id}`,
      null,
      params('DELETE /purchases/{id}', data.adminToken),
    );
    check(res, { 'compra cancelada (204)': (response) => response.status === 204 });
  });

  [data.outboundFlightId, data.returnFlightId].forEach((id) => {
    const availability = flightAvailability(id, data.adminToken);
    check(availability, {
      [`vuelo con sus ${data.seats} lugares libres tras cancelar`]: (value) => value === data.seats,
    });
  });

  const archived = http.put(
    `${BASE_URL}/packages/${data.packageId}/status`,
    JSON.stringify({ active: false }),
    params('PUT /packages/{id}/status', data.adminToken),
  );
  check(archived, { 'paquete archivado': (response) => response.status === 200 });
}
