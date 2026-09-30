describe('Comprador: login, búsqueda y detalle', () => {
  it('encuentra un paquete por nombre, origen y destino y consulta sus detalles', () => {
    const packageName = 'París Romántico'

    cy.intercept('POST', '**/auth/login').as('login')
    cy.intercept({
      method: 'GET',
      pathname: '/packages',
      query: { name: packageName, origin: 'BUE', destination: 'PAR' },
    }).as('search')
    cy.intercept('GET', '**/packages/*').as('detail')

    // ingresar desde el formulario, como lo haría un comprador
    cy.visit('/login')
    cy.get('input[name="username"]').type('buyer')
    cy.get('input[name="password"]').type('buyer123', { log: false })
    cy.contains('button', /^Ingresar$/).click()
    cy.wait('@login').its('response.statusCode').should('eq', 200)
    cy.location('pathname').should('eq', '/trips')
    cy.contains('h1', /^Paquetes$/).should('be.visible')

    // esperar las ciudades disponibles y aplicar los tres filtros
    cy.get('[data-cy="search-origin"] option[value="BUE"]').should('exist')
    cy.get('[data-cy="search-name"]').type(packageName)
    cy.get('[data-cy="search-origin"]').select('BUE')
    cy.get('[data-cy="search-destination"]').select('PAR')
    cy.contains('button', /^Buscar$/).click()
    cy.wait('@search').its('response.statusCode').should('eq', 200)
    cy.get('a[href^="/trips/"]').should('have.length', 1)
    cy.contains('h2', packageName).should('be.visible')
    cy.contains('h2', 'Londres Clásico').should('not.exist')

    // abrir el resultado y comprobar información visible del paquete
    cy.contains('a', packageName).click()
    cy.wait('@detail').its('response.statusCode').should('eq', 200)
    cy.location('pathname').should('match', /^\/trips\/\d+$/)
    cy.contains('h1', packageName).should('be.visible')
    cy.contains('p', 'Despegar · Hotel Palacio de París').should('be.visible')
    cy.contains('p', /^USD 1500$/).should('be.visible')
    cy.contains('p', 'BUE → PAR').should('be.visible')
    cy.contains('p', 'PAR → BUE').should('be.visible')
  })
})
