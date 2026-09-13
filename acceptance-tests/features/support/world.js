import { setWorldConstructor } from '@cucumber/cucumber'

class CtvWorld {
  constructor() {
    this.response = null
    this.token = null
    this.currentUsername = null
    this.package = null
    this.packagePrice = null
    this.outboundFlight = null
    this.returnFlight = null
    this.originalOutboundAvailability = null
  }
}

setWorldConstructor(CtvWorld)
