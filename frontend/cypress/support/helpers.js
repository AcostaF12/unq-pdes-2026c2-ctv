export function loginThroughUI(username, password) {
  cy.intercept('POST', '**/auth/login').as('login')
  cy.visit('/login')
  cy.get('input[name="username"]').type(username)
  cy.get('input[name="password"]').type(password, { log: false })
  cy.contains('button', /^Ingresar$/).click()
  cy.wait('@login').its('response.statusCode').should('eq', 200)
  cy.location('pathname').should('eq', '/trips')
  cy.contains('h1', /^Paquetes$/).should('be.visible')
}

export function registerBuyer() {
  const username = `e2e_${crypto.randomUUID().slice(0, 8)}`
  const password = 'buyer123'

  return cy.request({
    method: 'POST',
    url: `${Cypress.expose('backendUrl')}/auth/register`,
    body: { username, password, firstName: 'Comprador', lastName: 'E2E' },
    log: false,
  }).then((response) => {
    expect(response.status).to.eq(201)
    return { username, password, token: response.body.token }
  })
}

export function uniqueSuffix() {
  return crypto.randomUUID().slice(0, 8)
}

export function isoDateIn(days) {
  return new Date(Date.now() + days * 86400000).toISOString().slice(0, 10)
}

export function apiLogin(username, password) {
  return cy.request({
    method: 'POST',
    url: `${Cypress.expose('backendUrl')}/auth/login`,
    body: { username, password },
    log: false,
  }).then(({ body }) => body.token)
}

// Cierra la sesión del navegador sin pasar por la interfaz para poder entrar con otro rol.
export function clearBrowserSession() {
  cy.clearAllLocalStorage()
  cy.clearAllSessionStorage()
}

// Prepara por API un paquete BUE <-> PAR propio (vuelos y paquete con nombres únicos) y devuelve sus datos.
export function createPackageViaApi({ token, agencyId, name, price = 1500 }) {
  const backendUrl = Cypress.expose('backendUrl')
  const flightsUrl = Cypress.expose('flightsUrl')
  const auth = { Authorization: `Bearer ${token}` }
  const result = {}

  cy.request({ url: `${backendUrl}/hotels`, headers: auth, log: false }).then(({ body }) => {
    const hotel = body.find((item) => item.name === 'Hotel Palacio de París')
    expect(hotel, 'hotel inicial de París').to.exist
    result.hotelId = hotel.id
  })
  cy.request('POST', `${flightsUrl}/flights`, {
    airline: 'Cypress API', flightDate: isoDateIn(45), departureTime: '08:00:00',
    origin: 'BUE', destination: 'PAR', capacity: 3, availability: 3,
  }).then(({ body }) => { result.outboundId = body.id })
  cy.request('POST', `${flightsUrl}/flights`, {
    airline: 'Cypress API', flightDate: isoDateIn(52), departureTime: '20:00:00',
    origin: 'PAR', destination: 'BUE', capacity: 3, availability: 3,
  }).then(({ body }) => { result.returnId = body.id })
  return cy.then(() => cy.request({
    method: 'POST', url: `${backendUrl}/packages`, headers: auth,
    body: {
      name, hotelId: result.hotelId, outboundFlightId: result.outboundId,
      returnFlightId: result.returnId, price, ...(agencyId ? { agencyId } : {}),
    },
  })).then(({ status, body }) => {
    expect(status).to.eq(201)
    return body
  })
}

// Devuelve el control asociado a una etiqueta exacta; sirve dentro de `within`.
export function fieldByLabel(label) {
  return cy.contains('label', new RegExp(`^${label}$`)).invoke('attr', 'for')
    .then((id) => cy.get(`[id="${id}"]`))
}

// Elige una opción de un select por parte de su texto, sin depender de ids numéricos.
export function selectOptionContaining(selectSelector, text) {
  cy.get(selectSelector).find('option').contains(text).then(($option) => {
    cy.get(selectSelector).select($option.val())
  })
}

export function openAdminSection(cardId, linkLabel) {
  cy.contains('nav[aria-label="Principal"] a', /^Administración$/).click()
  cy.get('[data-testid="admin-hub"]').should('be.visible')
  cy.get(`[data-testid="admin-card-${cardId}"]`).contains('a', linkLabel).click()
}
