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
