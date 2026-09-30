import { defineConfig } from 'cypress'

export default defineConfig({
  viewportWidth: 1280,
  viewportHeight: 800,
  video: true,
  expose: {
    backendUrl: 'http://localhost:18080',
    flightsUrl: 'http://localhost:18081',
  },
  e2e: {
    baseUrl: 'http://localhost:18090',
    specPattern: 'cypress/e2e/**/*.cy.{js,ts}',
    supportFile: false,
  },
})
