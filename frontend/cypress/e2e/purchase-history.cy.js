import { loginThroughUI, registerBuyer } from '../support/helpers'

describe('Comprador: compra e historial', () => {
  let buyer
  let travelPackage

  beforeEach(() => {
    const backendUrl = Cypress.expose('backendUrl')
    const flightsUrl = Cypress.expose('flightsUrl')
    const packageName = `Compra E2E ${crypto.randomUUID().slice(0, 8)}`
    let agencyToken
    let hotelId
    let outboundId
    let returnId

    registerBuyer().then((created) => { buyer = created })
    cy.request({
      method: 'POST', url: `${backendUrl}/auth/login`,
      body: { username: 'agency', password: 'agency123' }, log: false,
    }).then(({ body }) => { agencyToken = body.token })
    cy.then(() => cy.request({
      url: `${backendUrl}/hotels`, headers: { Authorization: `Bearer ${agencyToken}` },
    })).then(({ body }) => {
      const hotel = body.find((item) => item.name === 'Hotel Palacio de París')
      expect(hotel, 'hotel inicial de París').to.exist
      hotelId = hotel.id
    })

    const flightDate = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10)
    const returnDate = new Date(Date.now() + 37 * 86400000).toISOString().slice(0, 10)
    cy.request('POST', `${flightsUrl}/flights`, {
      airline: 'Cypress E2E', flightDate, departureTime: '08:00:00',
      origin: 'BUE', destination: 'PAR', capacity: 2, availability: 2,
    }).then(({ status, body }) => {
      expect(status).to.eq(201)
      outboundId = body.id
    })
    cy.request('POST', `${flightsUrl}/flights`, {
      airline: 'Cypress E2E', flightDate: returnDate, departureTime: '20:00:00',
      origin: 'PAR', destination: 'BUE', capacity: 2, availability: 2,
    }).then(({ status, body }) => {
      expect(status).to.eq(201)
      returnId = body.id
    })
    cy.then(() => cy.request({
      method: 'POST', url: `${backendUrl}/packages`,
      headers: { Authorization: `Bearer ${agencyToken}` },
      body: { name: packageName, hotelId, outboundFlightId: outboundId, returnFlightId: returnId, price: 1750 },
    })).then(({ status, body }) => {
      expect(status).to.eq(201)
      travelPackage = body
    })
  })

  it('compra desde el detalle y conserva el viaje y el precio en su historial al recargar', () => {
    cy.intercept('POST', '**/purchases').as('purchase')
    cy.intercept({ method: 'GET', pathname: '/purchases/me' }).as('history')
    cy.intercept({ method: 'GET', pathname: '/packages', query: { name: travelPackage.name } }).as('search')
    loginThroughUI(buyer.username, buyer.password)

    // el comprador nuevo empieza sin compras.
    cy.contains('nav a', /^Mis compras$/).click()
    cy.wait('@history').its('response.statusCode').should('eq', 200)
    cy.contains('Todavía no hay viajes comprados').should('be.visible')
    cy.contains('a', /^Ver paquetes$/).click()
    cy.get('[data-cy="search-name"]').type(travelPackage.name)
    cy.contains('button', /^Buscar$/).click()
    cy.wait('@search').its('response.statusCode').should('eq', 200)
    cy.contains('a', travelPackage.name).click()
    cy.contains('h1', travelPackage.name).should('be.visible')

    // la operación bajo prueba se realiza con el botón 
    cy.contains('button', /^Comprar$/).should('be.enabled').click()
    cy.wait('@purchase').then(({ response }) => {
      expect(response.statusCode).to.eq(201)
      expect(response.body.travelPackage.id).to.eq(travelPackage.id)
    })
    cy.contains('Compra realizada. Los asientos de ida y vuelta quedaron reservados.').should('be.visible')
    cy.contains('button', /^Ya comprado$/).should('be.disabled')
    cy.contains('nav a', /^Mis compras$/).click()
    cy.contains('h1', /^Mis compras$/).should('be.visible')
    cy.contains('article', travelPackage.name).within(() => {
      cy.contains('Despegar').should('be.visible')
      cy.contains('Buenos Aires → Paris').should('be.visible')
      cy.contains('Hotel: Hotel Palacio de París').should('be.visible')
      cy.contains('Precio abonado').should('be.visible')
      cy.contains('USD 1750').should('be.visible')
    })

    // recargar fuerza a recuperar el historial persistido 
    cy.reload()
    cy.wait('@history').its('response.statusCode').should('eq', 200)
    cy.get('article.purchase-ticket').should('have.length', 1)
    cy.contains('article', travelPackage.name).should('be.visible')
    cy.contains('article', travelPackage.name).contains('a', /^Ver viaje$/).click()
    cy.contains('button', /^Ya comprado$/).should('be.disabled')
  })
})
