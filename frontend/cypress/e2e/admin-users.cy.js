import {
  apiLogin,
  clearBrowserSession,
  createPackageViaApi,
  fieldByLabel,
  loginThroughUI,
  registerBuyer,
  uniqueSuffix,
} from '../support/helpers'

describe('Administración de usuarios, vendedores y agencias', () => {
  it('crea agencia y vendedor, sigue una venta, bloquea la cuenta y cambia sus permisos', () => {
    const suffix = uniqueSuffix()
    const username = `aaa_seller_${suffix}`
    const password = 'seller123'
    const agencyName = `Agencia roles ${suffix}`
    const packageName = `Roles ${suffix}`
    let sellerId
    let sellerToken
    let adminToken
    let purchaseId
    cy.intercept('POST', '**/agencies').as('createAgency')
    cy.intercept('POST', '**/admin/users').as('createUser')
    cy.intercept('PUT', '**/admin/users/*').as('updateUser')

    loginThroughUI('vferreyra', 'vferreyra')
    cy.visit('/admin/agencies')
    fieldByLabel('Nombre de agencia').type(agencyName)
    fieldByLabel('Usuario').type(`owner_${suffix}`)
    fieldByLabel('Contraseña').type(password, { log: false })
    fieldByLabel('Nombre del responsable').type('Ana')
    fieldByLabel('Apellido del responsable').type('Responsable')
    cy.contains('button', /^Crear agencia$/).click()
    cy.wait('@createAgency').its('response.statusCode').should('eq', 201)
    cy.contains('[role="status"]', 'Agencia creada.').should('be.visible')

    cy.visit('/admin/users')
    cy.contains('button', /^Nuevo usuario$/).click()
    fieldByLabel('Usuario').type(username)
    fieldByLabel('Contraseña inicial').type(password, { log: false })
    fieldByLabel('Nombre').type('Lara')
    fieldByLabel('Apellido').type('Vendedora')
    fieldByLabel('Rol').select('AGENCY')
    fieldByLabel('Agencia').select(agencyName)
    cy.contains('button', /^Crear usuario$/).click()
    cy.wait('@createUser').then(({ response }) => {
      expect(response.statusCode).to.eq(201)
      expect(response.body.role).to.eq('AGENCY')
      expect(response.body.agency.name).to.eq(agencyName)
      expect(response.body).not.to.have.property('password')
      sellerId = response.body.id
    })
    cy.contains('li', `@${username}`).should('contain.text', 'Vendedor').and('contain.text', agencyName)
    apiLogin('vferreyra', 'vferreyra').then((token) => {
      adminToken = token
    })
    apiLogin(username, password)
      .then((token) => {
        sellerToken = token
        return createPackageViaApi({ token, name: packageName })
      })
      .then((trip) => {
        expect(trip.agency.name).to.eq(agencyName)
        registerBuyer()
          .then((buyer) =>
            cy.request({
              method: 'POST',
              url: `${Cypress.expose('backendUrl')}/purchases`,
              headers: { Authorization: `Bearer ${buyer.token}` },
              body: { packageId: trip.id },
              log: false,
            }),
          )
          .then(({ body }) => {
            purchaseId = body.id
          })
      })

    clearBrowserSession()
    loginThroughUI(username, password)
    cy.visit('/agency/sales')
    cy.contains('article', packageName).should('be.visible')
    cy.visit('/admin/users')
    cy.contains('Sin permiso').should('be.visible')
    clearBrowserSession()
    loginThroughUI('vferreyra', 'vferreyra')
    cy.visit('/admin/sales')
    fieldByLabel('Paquete').type(packageName)
    cy.contains('button', 'Filtrar ventas').click()
    cy.contains('article', packageName).within(() => {
      cy.contains(agencyName).should('be.visible')
      cy.contains('a', 'Ver detalle').click()
    })
    cy.then(() => cy.location('pathname').should('eq', `/admin/sales/${purchaseId}`))
    cy.contains('h1', packageName).should('be.visible')
    cy.contains('a', '← Ventas').click()
    cy.location('pathname').should('eq', '/admin/sales')

    cy.visit('/admin/users')
    cy.get(`button[aria-label="Editar ${username}"]`).click()
    fieldByLabel('Estado').select('disabled')
    cy.contains('button', 'Guardar cambios').click()
    cy.wait('@updateUser').its('response.statusCode').should('eq', 200)
    cy.contains('li', `@${username}`).should('contain.text', 'Deshabilitado')
    cy.then(() =>
      cy.request({
        url: `${Cypress.expose('backendUrl')}/users/me`,
        headers: { Authorization: `Bearer ${sellerToken}` },
        failOnStatusCode: false,
        log: false,
      }),
    )
      .its('status')
      .should('eq', 401)
    cy.request({
      method: 'POST',
      url: `${Cypress.expose('backendUrl')}/auth/login`,
      body: { username, password },
      failOnStatusCode: false,
      log: false,
    })
      .its('status')
      .should('eq', 401)

    cy.get(`button[aria-label="Editar ${username}"]`).click()
    fieldByLabel('Estado').select('enabled')
    fieldByLabel('Rol').select('BUYER')
    cy.contains('button', 'Guardar cambios').click()
    cy.wait('@updateUser').then(({ response }) => {
      expect(response.body.role).to.eq('BUYER')
      expect(response.body.agency).to.eq(null)
    })
    cy.then(() =>
      cy.request({
        url: `${Cypress.expose('backendUrl')}/packages/agency`,
        headers: { Authorization: `Bearer ${sellerToken}` },
        failOnStatusCode: false,
        log: false,
      }),
    )
      .its('status')
      .should('eq', 403)
    // El historial sigue consultable luego de cambiar el rol del vendedor.
    cy.then(() =>
      cy.request({
        url: `${Cypress.expose('backendUrl')}/admin/sales?packageName=${encodeURIComponent(packageName)}`,
        headers: { Authorization: `Bearer ${adminToken}` },
        log: false,
      }),
    )
      .its('body.totalElements')
      .should('eq', 1)
    cy.then(() => expect(sellerId).to.be.a('number'))
  })

  it('conserva las consultas y los cuatro reportes integrados en dev', () => {
    loginThroughUI('vferreyra', 'vferreyra')
    for (const [path, heading] of [
      ['/admin/queries/users', 'Usuarios'],
      ['/admin/favorites', 'Favoritos'],
      ['/admin/reviews', 'Reseñas'],
      ['/admin/purchases', 'Compras'],
    ]) {
      cy.visit(path)
      cy.contains('h1', heading).should('be.visible')
      cy.get('table tbody tr').should('have.length.greaterThan', 0)
      cy.contains('No se pudo cargar la consulta').should('not.exist')
    }
    cy.visit('/admin/reports')
    cy.contains('h1', 'Reportes Top 5').should('be.visible')
    cy.get('table').should('have.length', 4)
    cy.get('table tbody tr').should('have.length.greaterThan', 0)
  })

  for (const account of [
    { username: 'buyer', password: 'buyer123' },
    { username: 'agency', password: 'agency123' },
  ]) {
    it(`rechaza las pantallas y APIs administrativas para ${account.username}`, () => {
      loginThroughUI(account.username, account.password)
      for (const path of ['/admin/users', '/admin/agencies', '/admin/sales']) {
        cy.visit(path)
        cy.contains('Sin permiso').should('be.visible')
      }
      apiLogin(account.username, account.password).then((token) => {
        for (const path of ['/admin/users', '/admin/sales']) {
          cy.request({
            url: `${Cypress.expose('backendUrl')}${path}`,
            headers: { Authorization: `Bearer ${token}` },
            failOnStatusCode: false,
            log: false,
          })
            .its('status')
            .should('eq', 403)
        }
      })
    })
  }
})
