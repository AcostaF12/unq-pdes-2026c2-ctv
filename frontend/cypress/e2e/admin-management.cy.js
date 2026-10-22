import {
  apiLogin, clearBrowserSession, createPackageViaApi, fieldByLabel, isoDateIn,
  loginThroughUI, openAdminSection, registerBuyer, selectOptionContaining, uniqueSuffix,
} from '../support/helpers'

const HOTEL_OPTION = 'Hotel Palacio de París (PAR)'

function packageRow(name) {
  return cy.contains('[data-testid="package-item"]', name)
}

function findDespegarId(token) {
  return cy.request({
    url: `${Cypress.expose('backendUrl')}/agencies`, headers: { Authorization: `Bearer ${token}` }, log: false,
  }).then(({ body }) => {
    const agency = body.find((item) => item.name === 'Despegar')
    expect(agency, 'agencia inicial Despegar').to.exist
    return agency.id
  })
}

function fillFlight({ airline, origin, destination, date, time, capacity = 5, availability = 5 }) {
  cy.get('[data-testid="flight-form"]').within(() => {
    fieldByLabel('Aerolínea').type(airline)
    fieldByLabel('Origen').select(origin)
    fieldByLabel('Destino').select(destination)
    fieldByLabel('Fecha').type(date)
    fieldByLabel('Hora de salida').type(time)
    fieldByLabel('Capacidad').type(String(capacity))
    fieldByLabel('Lugares disponibles').type(String(availability))
    cy.contains('button', /^Crear vuelo$/).click()
  })
}

function forbiddenFlightBody() {
  return {
    airline: 'Prohibido E2E', flightDate: isoDateIn(30), departureTime: '08:00:00',
    origin: 'BUE', destination: 'PAR', capacity: 1, availability: 1,
  }
}

describe('Administración: acceso', () => {
  it('muestra la sección al administrador y la oculta a comprador y agencia', () => {
    loginThroughUI('vferreyra', 'vferreyra')
    cy.contains('nav[aria-label="Principal"] a', /^Administración$/).click()
    cy.location('pathname').should('eq', '/admin')
    cy.get('[data-testid="admin-hub"]').should('be.visible')
    cy.contains('h1', /^Administración$/).should('be.visible')
    cy.get('[data-testid="admin-card-packages"]').should('be.visible')
    cy.get('[data-testid="admin-card-flights"]').should('be.visible')
    cy.get('[data-testid="admin-card-hotels"]').should('be.visible')
    cy.get('nav[aria-label="Principal"]').contains('a', /^Nuevo hotel$/).should('not.exist')

    clearBrowserSession()
    registerBuyer().then((buyer) => {
      loginThroughUI(buyer.username, buyer.password)
      cy.get('nav[aria-label="Principal"]').contains('a', 'Administración').should('not.exist')
      for (const path of ['/admin', '/admin/flights', '/admin/packages']) {
        cy.visit(path)
        cy.contains('Sin permiso').should('be.visible')
        cy.contains('Esta acción es solo para administradores.').should('be.visible')
        cy.get('[data-testid="admin-hub"]').should('not.exist')
      }
      // la API también protege la operación si se saltea la interfaz
      cy.request({
        method: 'POST', url: `${Cypress.expose('backendUrl')}/flights`,
        headers: { Authorization: `Bearer ${buyer.token}` },
        body: forbiddenFlightBody(), failOnStatusCode: false, log: false,
      }).its('status').should('eq', 403)
    })

    clearBrowserSession()
    loginThroughUI('agency', 'agency123')
    cy.get('nav[aria-label="Principal"]').contains('a', 'Administración').should('not.exist')
    cy.visit('/admin/packages')
    cy.contains('Sin permiso').should('be.visible')
    cy.get('[data-testid="admin-hub"]').should('not.exist')
    apiLogin('agency', 'agency123').then((token) => {
      cy.request({
        method: 'POST', url: `${Cypress.expose('backendUrl')}/flights`,
        headers: { Authorization: `Bearer ${token}` },
        body: forbiddenFlightBody(), failOnStatusCode: false, log: false,
      }).its('status').should('eq', 403)
    })
  })
})

describe('Administración: vuelos y paquetes', () => {
  it('carga vuelos de ida y vuelta, publica un paquete y un comprador lo encuentra y lo compra', () => {
    const suffix = uniqueSuffix()
    const airline = `Cypress Admin ${suffix}`
    const packageName = `Admin E2E ${suffix}`
    const outboundDate = isoDateIn(61)
    const returnDate = isoDateIn(68)
    cy.intercept('POST', '**/flights').as('createFlight')
    cy.intercept('POST', '**/packages').as('createPackage')
    cy.intercept('POST', '**/purchases').as('purchase')
    cy.intercept({ method: 'GET', pathname: '/packages', query: { name: packageName } }).as('search')

    loginThroughUI('vferreyra', 'vferreyra')
    openAdminSection('flights', 'Gestionar vuelos')
    cy.location('pathname').should('eq', '/admin/flights')
    cy.contains('h1', /^Vuelos$/).should('be.visible')

    fillFlight({ airline, origin: 'BUE', destination: 'PAR', date: outboundDate, time: '08:30' })
    cy.wait('@createFlight').its('response.statusCode').should('eq', 201)
    cy.get('[role="status"]').should('contain.text', 'Vuelo creado.')
    fillFlight({ airline, origin: 'PAR', destination: 'BUE', date: returnDate, time: '20:15' })
    cy.wait('@createFlight').its('response.statusCode').should('eq', 201)
    // la fila se identifica por aerolínea única + ruta + fecha, porque otros vuelos pueden coincidir en fecha y hora
    cy.contains(
      '[data-testid="flight-item"]',
      new RegExp(`${airline}Buenos Aires \\(BUE\\) → Paris \\(PAR\\)${outboundDate} · 08:30`),
    ).contains('5 / 5 lugares').should('be.visible')
    cy.contains(
      '[data-testid="flight-item"]',
      new RegExp(`${airline}Paris \\(PAR\\) → Buenos Aires \\(BUE\\)${returnDate} · 20:15`),
    ).contains('5 / 5 lugares').should('be.visible')

    cy.contains('a', /^← Administración$/).click()
    cy.get('[data-testid="admin-card-packages"]').contains('a', 'Gestionar paquetes').click()
    cy.contains('h1', /^Paquetes$/).should('be.visible')
    cy.contains('button', /^Nuevo paquete$/).click()
    cy.get('[data-testid="package-form"]').within(() => {
      fieldByLabel('Nombre').type(packageName)
      fieldByLabel('Agencia').select('Despegar')
      fieldByLabel('Hotel').select(HOTEL_OPTION)
    })
    // los vuelos se eligen de listas filtradas por hotel y por ruta, sin escribir ids
    selectOptionContaining('[data-testid="package-outbound-flight"]', airline)
    cy.get('[data-testid="package-outbound-flight"] option:selected').should('contain.text', 'BUE → PAR')
    selectOptionContaining('[data-testid="package-return-flight"]', airline)
    cy.get('[data-testid="package-return-flight"] option:selected').should('contain.text', 'PAR → BUE')
    cy.get('[data-testid="package-form"]').within(() => {
      fieldByLabel('Precio').type('2100')
      cy.contains('button', /^Crear paquete$/).click()
    })
    cy.wait('@createPackage').then(({ request, response }) => {
      expect(response.statusCode).to.eq(201)
      expect(request.body.agencyId, 'el administrador envía la agencia').to.be.a('number')
      expect(response.body.agency.name).to.eq('Despegar')
    })
    cy.get('[role="status"]').should('contain.text', 'Paquete creado.')
    cy.get('[data-testid="package-form"]').should('not.exist')
    packageRow(packageName).within(() => {
      cy.contains('Agencia: Despegar').should('be.visible')
      cy.contains('Buenos Aires → Paris').should('be.visible')
      cy.contains('Hotel: Hotel Palacio de París').should('be.visible')
      cy.contains('USD 2100').should('be.visible')
    })

    // el comprador ve el paquete y sus dos vuelos
    clearBrowserSession()
    registerBuyer().then((buyer) => loginThroughUI(buyer.username, buyer.password))
    cy.get('[data-cy="search-name"]').type(packageName)
    cy.contains('button', /^Buscar$/).click()
    cy.wait('@search').its('response.statusCode').should('eq', 200)
    cy.get('a[href^="/trips/"]').should('have.length', 1)
    cy.contains('a', packageName).click()
    cy.contains('h1', packageName).should('be.visible')
    cy.contains('p', 'Despegar · Hotel Palacio de París').should('be.visible')
    cy.contains('p', /^USD 2100$/).should('be.visible')
    cy.get('.flight-card').should('have.length', 2)
    cy.get('.flight-card').eq(0).within(() => {
      cy.contains('Ida').should('be.visible')
      cy.contains('BUE → PAR').should('be.visible')
      cy.contains(airline).should('be.visible')
      cy.contains('08:30').should('be.visible')
    })
    cy.get('.flight-card').eq(1).within(() => {
      cy.contains('Vuelta').should('be.visible')
      cy.contains('PAR → BUE').should('be.visible')
      cy.contains(airline).should('be.visible')
      // es-AR puede mostrar la hora en formato de 12 horas
      cy.contains(/20:15|08:15\s*p/i).should('be.visible')
    })

    cy.contains('button', /^Comprar$/).should('be.enabled').click()
    cy.get('[role="dialog"]').should('be.visible')
    cy.contains('button', /^Confirmar compra$/).click()
    cy.wait('@purchase').then(({ response }) => {
      expect(response.statusCode).to.eq(201)
      expect(response.body.travelPackage.name).to.eq(packageName)
    })
    cy.contains('Compra realizada. Los asientos de ida y vuelta quedaron reservados.').should('be.visible')
    cy.contains('button', /^Seguir explorando$/).click()
    cy.contains('nav a', /^Mis compras$/).click()
    cy.contains('article', packageName).within(() => {
      cy.contains('Despegar').should('be.visible')
      cy.contains('USD 2100').should('be.visible')
    })
  })

  it('edita el precio de un paquete sin compras y lo elimina tras confirmar', () => {
    const packageName = `Admin E2E editar ${uniqueSuffix()}`
    let packageId
    cy.intercept('PUT', '**/packages/*').as('updatePackage')
    cy.intercept('DELETE', '**/packages/*').as('deletePackage')

    apiLogin('vferreyra', 'vferreyra').then((token) => {
      findDespegarId(token)
        .then((agencyId) => createPackageViaApi({ token, agencyId, name: packageName, price: 1500 }))
        .then((created) => { packageId = created.id })
    })
    loginThroughUI('vferreyra', 'vferreyra')
    openAdminSection('packages', 'Gestionar paquetes')
    packageRow(packageName).contains('USD 1500').should('be.visible')

    cy.get(`button[aria-label="Editar ${packageName}"]`).click()
    cy.get('[data-testid="package-form"]').within(() => {
      cy.contains('h2', /^Editar paquete$/).should('be.visible')
      fieldByLabel('Nombre').should('have.value', packageName)
      fieldByLabel('Agencia').find('option:selected').should('have.text', 'Despegar')
      fieldByLabel('Precio').clear().type('1999')
      cy.contains('button', /^Guardar cambios$/).click()
    })
    cy.wait('@updatePackage').its('response.statusCode').should('eq', 200)
    cy.get('[role="status"]').should('contain.text', 'Paquete actualizado.')
    packageRow(packageName).contains('USD 1999').should('be.visible')
    cy.reload()
    packageRow(packageName).contains('USD 1999').should('be.visible')

    // cancelar no elimina nada
    cy.get(`button[aria-label="Eliminar ${packageName}"]`).click()
    packageRow(packageName).find('[data-testid="confirm-delete"]').should('be.visible')
      .and('contain.text', `¿Eliminar el paquete ${packageName}?`)
    cy.contains('[data-testid="confirm-delete"] button', /^Cancelar$/).click()
    cy.get('[data-testid="confirm-delete"]').should('not.exist')
    packageRow(packageName).should('be.visible')

    cy.get(`button[aria-label="Eliminar ${packageName}"]`).click()
    cy.contains('[data-testid="confirm-delete"] button', /^Confirmar eliminación$/).click()
    cy.wait('@deletePackage').its('response.statusCode').should('eq', 204)
    cy.get('[role="status"]').should('contain.text', 'Paquete eliminado.')
    cy.contains('[data-testid="package-item"]', packageName).should('not.exist')
    cy.then(() => apiLogin('vferreyra', 'vferreyra')).then((token) => {
      cy.request({
        url: `${Cypress.expose('backendUrl')}/packages/${packageId}`,
        headers: { Authorization: `Bearer ${token}` }, failOnStatusCode: false, log: false,
      }).its('status').should('eq', 404)
    })
  })

  it('muestra el error del servidor al eliminar un paquete que ya tiene compras', () => {
    const packageName = `Admin E2E comprado ${uniqueSuffix()}`
    cy.intercept('DELETE', '**/packages/*').as('deletePackage')

    apiLogin('vferreyra', 'vferreyra').then((token) => {
      findDespegarId(token)
        .then((agencyId) => createPackageViaApi({ token, agencyId, name: packageName }))
        .then((created) => {
          registerBuyer().then((buyer) => {
            cy.request({
              method: 'POST', url: `${Cypress.expose('backendUrl')}/purchases`,
              headers: { Authorization: `Bearer ${buyer.token}` }, body: { packageId: created.id }, log: false,
            }).its('status').should('eq', 201)
          })
        })
    })
    loginThroughUI('vferreyra', 'vferreyra')
    openAdminSection('packages', 'Gestionar paquetes')
    cy.get(`button[aria-label="Eliminar ${packageName}"]`).click()
    cy.contains('[data-testid="confirm-delete"] button', /^Confirmar eliminación$/).click()
    cy.wait('@deletePackage').its('response.statusCode').should('be.within', 400, 409)
    cy.get('[data-testid="confirm-delete"]').within(() => {
      cy.get('[role="alert"]').should('contain.text', 'Cannot delete a package that already has purchases.')
      cy.contains('button', /^Confirmar eliminación$/).should('be.enabled')
    })
    cy.get('[role="status"]').should('not.exist')
    packageRow(packageName).should('be.visible')
    cy.reload()
    packageRow(packageName).should('be.visible')
  })
})

describe('Agencia: alta de paquetes con selector de vuelos', () => {
  it('filtra vuelos por origen y fechas al crear y editar un paquete y luego lo elimina', () => {
    const suffix = uniqueSuffix()
    const airline = `Cypress Agencia ${suffix}`
    const packageName = `Agencia E2E ${suffix}`
    const outboundDate = isoDateIn(75)
    const returnDate = isoDateIn(82)
    cy.intercept('POST', '**/packages').as('createPackage')
    cy.intercept('PUT', '**/packages/*').as('updatePackage')
    cy.intercept('DELETE', '**/packages/*').as('deletePackage')
    cy.intercept({
      method: 'GET', pathname: '/flights',
      query: { origin: 'BUE', destination: 'PAR' },
    }).as('filteredOutbound')
    cy.intercept({
      method: 'GET', pathname: '/flights',
      query: { origin: 'PAR', destination: 'BUE' },
    }).as('filteredReturn')

    const flightsUrl = Cypress.expose('flightsUrl')
    cy.request('POST', `${flightsUrl}/flights`, {
      airline, flightDate: outboundDate, departureTime: '07:45:00',
      origin: 'BUE', destination: 'PAR', capacity: 4, availability: 4,
    }).its('status').should('eq', 201)
    cy.request('POST', `${flightsUrl}/flights`, {
      airline, flightDate: returnDate, departureTime: '19:10:00',
      origin: 'PAR', destination: 'BUE', capacity: 4, availability: 4,
    }).its('status').should('eq', 201)

    loginThroughUI('agency', 'agency123')
    cy.contains('nav[aria-label="Principal"] a', /^Agencia$/).click()
    cy.contains('h1', /^Paquetes de la agencia$/).should('be.visible')
    cy.get('[data-testid="package-form"]').within(() => {
      cy.contains('label', /^Agencia$/).should('not.exist')
      cy.get('[data-testid="package-outbound-flight"]').should('be.disabled')
      fieldByLabel('Nombre').type(packageName)
      fieldByLabel('Hotel').select(HOTEL_OPTION)
      fieldByLabel('Origen de ida').select('BUE')
      fieldByLabel('Fecha de vuelta').should('be.disabled')
      fieldByLabel('Fecha de ida').find(`option[value="${outboundDate}"]`).should('exist')
      fieldByLabel('Fecha de ida').select(outboundDate)
    })
    cy.wait('@filteredOutbound').its('response.statusCode').should('eq', 200)
    selectOptionContaining('[data-testid="package-outbound-flight"]', airline)
    cy.get('[data-testid="package-outbound-flight"] option:selected').should('contain.text', outboundDate)
    cy.wait('@filteredReturn').its('response.statusCode').should('eq', 200)
    cy.get('[data-testid="package-return-date"]').select(returnDate)
    cy.get('[data-testid="package-return-flight"]').should('be.enabled')
    selectOptionContaining('[data-testid="package-return-flight"]', airline)
    cy.get('[data-testid="package-form"]').within(() => {
      fieldByLabel('Precio').type('1850')
      cy.contains('button', /^Crear paquete$/).click()
    })
    cy.wait('@createPackage').then(({ request, response }) => {
      expect(response.statusCode).to.eq(201)
      expect(request.body).not.to.have.property('agencyId')
      expect(response.body.agency.name).to.eq('Despegar')
    })
    cy.get('[role="status"]').should('contain.text', 'Paquete creado.')
    cy.contains('li', packageName).within(() => {
      cy.contains('Buenos Aires → Paris').should('be.visible')
      cy.contains('USD 1850').should('be.visible')
    })

    cy.get(`button[aria-label="Editar ${packageName}"]`).click()
    cy.get('[data-testid="package-return-flight"]').should('not.have.value', '')
    cy.get('[data-testid="package-flight-origin"]').select('BUE')
    cy.get('[data-testid="package-outbound-flight"]').should('have.value', '')
    cy.get('[data-testid="package-return-flight"]').should('have.value', '').and('be.disabled')
    cy.get('[data-testid="package-outbound-date"]').select(outboundDate)
    selectOptionContaining('[data-testid="package-outbound-flight"]', airline)
    cy.get('[data-testid="package-return-date"]').select(returnDate)
    selectOptionContaining('[data-testid="package-return-flight"]', airline)
    cy.get('[data-testid="package-return-date"]').select('')
    cy.get('[data-testid="package-outbound-flight"]').should('not.have.value', '')
    cy.get('[data-testid="package-return-flight"]').should('have.value', '')
    cy.get('[data-testid="package-return-date"]').select(returnDate)
    selectOptionContaining('[data-testid="package-return-flight"]', airline)
    cy.get('[data-testid="package-form"]').within(() => {
      fieldByLabel('Precio').clear().type('1950')
      cy.contains('button', /^Guardar cambios$/).click()
    })
    cy.wait('@updatePackage').then(({ request, response }) => {
      expect(response.statusCode).to.eq(200)
      expect(request.body).not.to.have.property('agencyId')
      expect(response.body.price).to.eq(1950)
    })
    cy.contains('li', packageName).contains('USD 1950').should('be.visible')
    cy.reload()
    cy.contains('li', packageName).contains('USD 1950').should('be.visible')

    cy.contains('li', packageName).contains('button', /^Eliminar$/).click()
    cy.wait('@deletePackage').its('response.statusCode').should('eq', 204)
    cy.contains('li', packageName).should('not.exist')
  })
})
