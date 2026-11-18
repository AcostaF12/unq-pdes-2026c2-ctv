import { BeforeAll, setDefaultTimeout } from '@cucumber/cucumber'
import { waitForServices } from './api.js'

setDefaultTimeout(30_000)

BeforeAll(async () => {
  await waitForServices()
})
