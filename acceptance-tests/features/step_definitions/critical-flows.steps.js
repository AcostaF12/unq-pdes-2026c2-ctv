import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { Given, When, Then } from '@cucumber/cucumber'
import { backendRequest, flightsRequest } from '../support/api.js'

async function login(username, password) {
  return backendRequest('/auth/login', {
    method: 'POST',
    body: { username, password },
  })
}

async function loginAsAgency(world) {
  const response = await login('agency', 'agency123')
  assert.equal(response.status, 200)
  world.token = response.body.token
  world.currentUsername = 'agency'
}

async function registerBuyer(world) {
  const suffix = randomUUID().slice(0, 8)
  const username = `buyer_${suffix}`
  const response = await backendRequest('/auth/register', {
    method: 'POST',
    body: {
      username,
      password: 'buyer123',
      firstName: 'Buyer',
      lastName: suffix,
    },
  })
  assert.equal(response.status, 201)
  world.token = response.body.token
  world.currentUsername = username
}

async function findHotel(token, destination) {
  const response = await backendRequest('/hotels', { token })
  assert.equal(response.status, 200)
  const hotel = response.body.find((candidate) => candidate.city.code === destination)
  assert.ok(hotel, `No hotel found in ${destination}.`)
  return hotel
}

async function findFlight(origin, destination) {
  const query = new URLSearchParams({ origin, destination })
  const response = await flightsRequest(`/flights?${query}`)
  assert.equal(response.status, 200)
  assert.ok(response.body.length > 0, `No flight found from ${origin} to ${destination}.`)
  return response.body[0]
}

async function createPackage(world, origin, destination, options = {}) {
  await loginAsAgency(world)
  const hotel = await findHotel(world.token, destination)
  const outbound = options.outbound ?? await findFlight(origin, destination)
  const returnFlight = options.returnFlight ?? await findFlight(destination, origin)
  const name = `Aceptación ${origin}-${destination} ${randomUUID().slice(0, 8)}`
  const price = options.price ?? 1750

  const response = await backendRequest('/packages', {
    method: 'POST',
    token: world.token,
    body: {
      name,
      hotelId: hotel.id,
      outboundFlightId: outbound.id,
      returnFlightId: returnFlight.id,
      price,
    },
  })
  assert.equal(response.status, 201, JSON.stringify(response.body))
  world.package = response.body
  world.packagePrice = price
  world.outboundFlight = outbound
  world.returnFlight = returnFlight
  return response
}

Given('existe el comprador inicial {string}', function (username) {
  this.currentUsername = username
})

When('inicia sesión con la contraseña {string}', async function (password) {
  this.response = await login(this.currentUsername, password)
})

Then('la autenticación responde con status {int}', function (status) {
  assert.equal(this.response.status, status)
})

Then('recibe un token para el rol {string}', function (role) {
  assert.ok(this.response.body.token)
  assert.equal(this.response.body.user.role, role)
})

Given('inició sesión la agencia inicial', async function () {
  await loginAsAgency(this)
})

Given('existen vuelos de ida y vuelta entre {string} y {string}', async function (origin, destination) {
  this.outboundFlight = await findFlight(origin, destination)
  this.returnFlight = await findFlight(destination, origin)
})

When('publica un paquete nuevo para {string} con precio {int}', async function (destination, price) {
  const hotel = await findHotel(this.token, destination)
  const name = `Aceptación BUE-${destination} ${randomUUID().slice(0, 8)}`
  this.packagePrice = price
  this.response = await backendRequest('/packages', {
    method: 'POST',
    token: this.token,
    body: {
      name,
      hotelId: hotel.id,
      outboundFlightId: this.outboundFlight.id,
      returnFlightId: this.returnFlight.id,
      price,
    },
  })
  this.package = this.response.body
})

Then('la publicación responde con status {int}', function (status) {
  assert.equal(this.response.status, status, JSON.stringify(this.response.body))
})

Then('el paquete queda asociado a la agencia {string}', function (agencyName) {
  assert.equal(this.package.agency.name, agencyName)
})

Given('existe un paquete nuevo entre {string} y {string}', async function (origin, destination) {
  await createPackage(this, origin, destination)
})

Given('inició sesión un comprador nuevo', async function () {
  await registerBuyer(this)
})

When('busca paquetes desde {string} hacia {string}', async function (origin, destination) {
  const query = new URLSearchParams({ origin, destination })
  this.response = await backendRequest(`/packages?${query}`, { token: this.token })
})

Then('la búsqueda responde con status {int}', function (status) {
  assert.equal(this.response.status, status)
})

Then('el paquete nuevo aparece en los resultados', function () {
  assert.ok(this.response.body.some((candidate) => candidate.id === this.package.id))
})

When('compra el paquete nuevo', async function () {
  this.response = await backendRequest('/purchases', {
    method: 'POST',
    token: this.token,
    body: { packageId: this.package.id },
  })
})

When('intenta comprar el paquete nuevo', async function () {
  this.response = await backendRequest('/purchases', {
    method: 'POST',
    token: this.token,
    body: { packageId: this.package.id },
  })
})

Then('la compra responde con status {int}', function (status) {
  assert.equal(this.response.status, status, JSON.stringify(this.response.body))
})

Then('la compra es rechazada con status {int}', function (status) {
  assert.equal(this.response.status, status, JSON.stringify(this.response.body))
})

Then('la compra figura en su historial con el precio publicado', async function () {
  const response = await backendRequest('/purchases/me', { token: this.token })
  assert.equal(response.status, 200)
  const purchase = response.body.find((candidate) => candidate.packageId === this.package.id)
  assert.ok(purchase, 'The new purchase was not found in the buyer history.')
  assert.equal(Number(purchase.purchasePrice), Number(this.packagePrice))
})

Given('existe un paquete cuyo vuelo de regreso no tiene disponibilidad', async function () {
  const suffix = randomUUID().slice(0, 8)
  const date = new Date(Date.now() + 60 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
  const outboundResponse = await flightsRequest('/flights', {
    method: 'POST',
    body: {
      airline: `Acceptance ${suffix}`,
      flightDate: date,
      departureTime: '09:00:00',
      origin: 'BUE',
      destination: 'PAR',
      capacity: 1,
      availability: 1,
    },
  })
  assert.equal(outboundResponse.status, 201)

  const returnResponse = await flightsRequest('/flights', {
    method: 'POST',
    body: {
      airline: `Acceptance ${suffix}`,
      flightDate: date,
      departureTime: '18:00:00',
      origin: 'PAR',
      destination: 'BUE',
      capacity: 1,
      availability: 0,
    },
  })
  assert.equal(returnResponse.status, 201)

  this.originalOutboundAvailability = outboundResponse.body.availability
  await createPackage(this, 'BUE', 'PAR', {
    outbound: outboundResponse.body,
    returnFlight: returnResponse.body,
  })
})

Then('no queda una compra registrada', async function () {
  const response = await backendRequest('/purchases/me', { token: this.token })
  assert.equal(response.status, 200)
  assert.ok(!response.body.some((candidate) => candidate.packageId === this.package.id))
})

Then('el vuelo de ida recupera su disponibilidad original', async function () {
  const response = await flightsRequest(`/flights/${this.outboundFlight.id}`)
  assert.equal(response.status, 200)
  assert.equal(response.body.availability, this.originalOutboundAvailability)
})
