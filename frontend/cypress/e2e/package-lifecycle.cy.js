import {
  apiLogin,
  clearBrowserSession,
  createPackageViaApi,
  fieldByLabel,
  loginThroughUI,
  registerBuyer,
  uniqueSuffix,
} from '../support/helpers'

describe('Ciclo de vida de paquetes', () => {
  it('archiva una oferta vendida, conserva su historial y vuelve a publicarla', () => {
    const name = `AA Lifecycle ${uniqueSuffix()}`
    let id
    let buyer
    cy.intercept('PUT', '**/packages/*/status').as('status')
    apiLogin('vferreyra', 'vferreyra').then((token) => {
      cy.request({
        url: `${Cypress.expose('backendUrl')}/agencies`,
        headers: { Authorization: `Bearer ${token}` },
      })
        .then(({ body }) =>
          createPackageViaApi({ token, agencyId: body.find((a) => a.name === 'Despegar').id, name }),
        )
        .then((trip) => {
          id = trip.id
        })
    })
    registerBuyer().then((created) => {
      buyer = created
      cy.then(() =>
        cy.request({
          method: 'POST',
          url: `${Cypress.expose('backendUrl')}/purchases`,
          headers: { Authorization: `Bearer ${buyer.token}` },
          body: { packageId: id },
          log: false,
        }),
      )
    })
    loginThroughUI('vferreyra', 'vferreyra')
    cy.visit('/admin/packages')
    cy.get(`button[aria-label="Archivar ${name}"]`).click()
    cy.wait('@status').its('response.body.active').should('eq', false)
    fieldByLabel('Estado del paquete').select('archived')
    cy.contains('[data-testid="package-item"]', name).should('contain.text', 'Archivado')
    cy.reload()
    fieldByLabel('Estado del paquete').select('archived')
    cy.contains('[data-testid="package-item"]', name).should('contain.text', 'Archivado')

    clearBrowserSession()
    cy.then(() => loginThroughUI(buyer.username, buyer.password))
    cy.then(() => cy.visit(`/trips/${id}`))
    cy.contains('button', 'Paquete archivado').should('be.disabled')
    cy.visit('/purchases')
    cy.contains('article', name).should('be.visible')
    cy.then(() =>
      cy.request({
        method: 'POST',
        url: `${Cypress.expose('backendUrl')}/purchases`,
        headers: { Authorization: `Bearer ${buyer.token}` },
        body: { packageId: id },
        failOnStatusCode: false,
        log: false,
      }),
    )
      .its('status')
      .should('eq', 400)

    clearBrowserSession()
    loginThroughUI('vferreyra', 'vferreyra')
    cy.visit('/admin/packages')
    fieldByLabel('Estado del paquete').select('archived')
    cy.get(`button[aria-label="Publicar ${name}"]`).click()
    cy.wait('@status').its('response.body.active').should('eq', true)
    fieldByLabel('Estado del paquete').select('active')
    cy.contains('[data-testid="package-item"]', name).should('contain.text', 'Publicado')
  })
})
