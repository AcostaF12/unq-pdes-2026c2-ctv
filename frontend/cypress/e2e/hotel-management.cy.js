import { loginThroughUI } from '../support/helpers'

describe('Administrador: gestión de hoteles', () => {
  it('crea un hotel, edita sus datos y lo elimina desde la interfaz', () => {
    const hotelName = `Hotel E2E ${crypto.randomUUID().slice(0, 8)}`
    const updatedName = `${hotelName} actualizado`
    let hotelId
    cy.intercept('POST', '**/hotels').as('createHotel')
    cy.intercept('PUT', '**/hotels/*').as('updateHotel')
    cy.intercept('DELETE', '**/hotels/*').as('deleteHotel')
    loginThroughUI('vferreyra', 'vferreyra')

    // crear un hotel sin modificar los otros hoteles
    cy.contains('nav a', /^Nuevo hotel$/).click()
    cy.contains('h1', /^Nuevo hotel$/).should('be.visible')
    cy.get('input[name="name"]').type(hotelName)
    cy.get('select[name="cityCode"]').select('PAR')
    cy.get('input[name="photoUrl"]').type('http://localhost:18090/logo.png')
    cy.contains('button', /^Crear hotel$/).click()
    cy.wait('@createHotel').then(({ response }) => {
      expect(response.statusCode).to.eq(201)
      hotelId = response.body.id
    })
    cy.contains('h1', hotelName).should('be.visible')
    cy.contains('p', 'Paris · PAR').should('be.visible')

    // editar desde el detalle del hotel y comprobar persistencia tras recargar
    cy.contains('a', /^Editar$/).click()
    cy.contains('h1', /^Editar hotel$/).should('be.visible')
    cy.get('input[name="name"]').should('have.value', hotelName).clear().type(updatedName)
    cy.get('select[name="cityCode"]').select('ROM')
    cy.contains('button', /^Guardar$/).click()
    cy.wait('@updateHotel').its('response.statusCode').should('eq', 200)
    cy.contains('h1', updatedName).should('be.visible')
    cy.contains('p', 'Rome · ROM').should('be.visible')
    cy.reload()
    cy.contains('h1', updatedName).should('be.visible')
    cy.contains('p', 'Rome · ROM').should('be.visible')

    // confirmar la eliminación solo de este hotel 
    cy.on('window:confirm', (message) => {
      expect(message).to.eq(`¿Eliminar ${updatedName}?`)
      return true
    })
    cy.contains('button', /^Eliminar$/).click()
    cy.wait('@deleteHotel').its('response.statusCode').should('eq', 204)
    cy.location('pathname').should('eq', '/hotels')
    cy.contains('h1', /^Hoteles$/).should('be.visible')
    cy.contains('a', updatedName).should('not.exist')
    cy.then(() => cy.visit(`/hotels/${hotelId}`))
    cy.contains('No encontramos este hotel').should('be.visible')
  })
})
