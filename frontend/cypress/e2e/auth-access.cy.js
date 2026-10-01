import { loginThroughUI, registerBuyer } from '../support/helpers'

describe('Autenticación y permisos', () => {
  it('rechaza una contraseña incorrecta y permite volver a intentar', () => {
    cy.intercept('POST', '**/auth/login').as('login')
    cy.visit('/login')
    cy.get('input[name="username"]').type('buyer')
    cy.get('input[name="password"]').type('incorrecta', { log: false })
    cy.contains('button', /^Ingresar$/).click()
    cy.wait('@login').its('response.statusCode').should('eq', 401)
    cy.get('[role="alert"]').should('contain.text', 'Usuario o contraseña incorrectos.')
    cy.location('pathname').should('eq', '/login')
    cy.get('input[name="password"]').clear().type('buyer123', { log: false })
    cy.contains('button', /^Ingresar$/).should('be.enabled').click()
    cy.wait('@login').its('response.statusCode').should('eq', 200)
    cy.location('pathname').should('eq', '/trips')
    cy.contains('h1', /^Paquetes$/).should('be.visible')
  })

  it('impide que un comprador acceda al formulario de creación y a su API', () => {
    registerBuyer().then((buyer) => {
      loginThroughUI(buyer.username, buyer.password)
      cy.get('nav').contains('Nuevo hotel').should('not.exist')
      cy.visit('/hotels/new')
      cy.contains('Sin permiso').should('be.visible')
      cy.contains('Esta acción es solo para administradores.').should('be.visible')
      cy.get('input[name="name"]').should('not.exist')

      // la API también debe proteger la operación si se saltea la interfaz
      cy.request({
        method: 'POST', url: `${Cypress.expose('backendUrl')}/hotels`,
        headers: { Authorization: `Bearer ${buyer.token}` },
        body: { name: 'Hotel prohibido E2E', cityCode: 'PAR', photoUrl: 'http://localhost:18090/logo.png' },
        failOnStatusCode: false, log: false,
      }).its('status').should('eq', 403)
    })
  })
})
