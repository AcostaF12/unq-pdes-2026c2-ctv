const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8080'
const FLIGHTS_URL = process.env.FLIGHTS_URL ?? 'http://localhost:8081'

async function request(baseUrl, path, { method = 'GET', token, body } = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(token === undefined ? {} : { Authorization: `Bearer ${token}` }),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })

  const text = await response.text()
  let parsedBody = null
  if (text !== '') {
    try {
      parsedBody = JSON.parse(text)
    } catch {
      parsedBody = text
    }
  }

  return { status: response.status, body: parsedBody }
}

export const backendRequest = (path, options) => request(BACKEND_URL, path, options)
export const flightsRequest = (path, options) => request(FLIGHTS_URL, path, options)

export async function waitForServices(timeoutMs = 120_000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    try {
      const [backend, flights] = await Promise.all([
        backendRequest('/actuator/health'),
        flightsRequest('/actuator/health'),
      ])
      if (backend.status === 200 && flights.status === 200) return
    } catch {
      // The containers may still be starting.
    }
    await new Promise((resolve) => setTimeout(resolve, 1_000))
  }
  throw new Error('Backend and flights-service did not become healthy in time.')
}
