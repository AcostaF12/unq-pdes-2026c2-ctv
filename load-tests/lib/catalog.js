// Búsqueda del catálogo con filtros, páginas y orden variados, compartida por catalog-load y catalog-spike.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, params, pick, queryString } from './common.js';

// Destinos con paquetes en el seed (SeedFlightIds.DESTINATIONS); el origen de todos es BUE.
const DESTINATIONS = ['PAR', 'LON', 'ROM', 'NYC', 'RIO', 'BCN', 'CUN', 'TYO', 'DXB'];
const NAMES = ['Par', 'Roma', 'Londres', 'Tokio', 'Caribe', 'Río'];
const SORTS = ['name,asc', 'price,asc', 'price,desc', 'destination.name,asc', 'rating,desc'];
const PRICE_RANGES = [
  [0, 1500],
  [1000, 2500],
  [2000, 5000],
];

export function randomCatalogQuery() {
  const query = { page: pick([0, 0, 0, 1, 2]), size: pick([10, 20]), sort: pick(SORTS) };
  const mode = Math.random();
  if (mode < 0.25) {
    // Sin filtros: la primera vista del catálogo.
  } else if (mode < 0.5) {
    query.destination = pick(DESTINATIONS);
  } else if (mode < 0.65) {
    query.origin = 'BUE';
    query.destination = pick(DESTINATIONS);
  } else if (mode < 0.85) {
    const [min, max] = pick(PRICE_RANGES);
    query.minPrice = min;
    query.maxPrice = max;
  } else {
    query.name = pick(NAMES);
  }
  return query;
}

export function searchCatalog(token) {
  const res = http.get(`${BASE_URL}/packages?${queryString(randomCatalogQuery())}`, params('GET /packages', token));
  check(res, {
    'catálogo responde 200': (r) => r.status === 200,
    'catálogo devuelve una página': (r) => r.status === 200 && Array.isArray(r.json().content),
  });
  return res;
}
