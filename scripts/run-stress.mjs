import { spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const isWindows = process.platform === 'win32'
const hostUrl = process.env.BASE_URL || 'http://localhost:8080'
const k6Image = 'grafana/k6:2.4.0'

// Más chico que la CI, con la misma regla: no sobrevender y no devolver un asiento dos veces.
const scenarios = [
  { name: 'last-seat-stress', env: { SEATS: '5', BUYERS: '12' } },
  { name: 'purchase-throughput', env: { SEATS: '8', BUYERS: '6' } },
  { name: 'cancel-rebuy', env: { SEATS: '5' } },
  { name: 'double-cancel', env: { ATTEMPTS: '20' } },
]

function run(command, args, options = {}) {
  return new Promise((resolve) => {
    const child = spawn(command, args, {
      cwd: root,
      env: process.env,
      shell: isWindows,
      stdio: options.stdio ?? 'inherit',
    })
    child.on('close', (code) => resolve(code ?? 1))
    child.on('error', (error) => {
      console.error(error.message)
      resolve(1)
    })
  })
}

function formatDuration(ms) {
  const seconds = Math.round(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  if (minutes === 0) {
    return `${rest}s`
  }
  return `${minutes}m ${rest}s`
}

async function backendIsUp() {
  try {
    const response = await fetch(`${hostUrl.replace(/\/+$/, '')}/actuator/health`, {
      signal: AbortSignal.timeout(10_000),
    })
    if (!response.ok) {
      return false
    }
    const body = await response.json()
    return body.status === 'UP'
  } catch {
    return false
  }
}

function k6BaseUrl() {
  const url = new URL(hostUrl)
  if (url.hostname === 'localhost' || url.hostname === '127.0.0.1') {
    url.hostname = 'host.docker.internal'
  }
  return url.toString().replace(/\/+$/, '')
}

function k6Args(scenario) {
  const envArgs = ['-e', `BASE_URL=${k6BaseUrl()}`]
  for (const [key, value] of Object.entries(scenario.env)) {
    envArgs.push('-e', `${key}=${value}`)
  }
  return [
    'run',
    '--rm',
    '--add-host',
    'host.docker.internal:host-gateway',
    '-v',
    `${path.join(root, 'load-tests')}:/scripts`,
    '-w',
    '/scripts',
    ...envArgs,
    k6Image,
    'run',
    `${scenario.name}.js`,
  ]
}

async function main() {
  const requested = process.argv[2]
  const selected = requested ? scenarios.filter((scenario) => scenario.name === requested) : scenarios
  if (selected.length === 0) {
    console.error(`Escenario desconocido: ${requested}`)
    console.error(`Uso: node scripts/run-stress.mjs [${scenarios.map((scenario) => scenario.name).join('|')}]`)
    process.exit(2)
  }

  if (!(await backendIsUp())) {
    console.error(`El backend no está UP en ${hostUrl}/actuator/health. Levantalo con npm start y volvé a correr.`)
    process.exit(1)
  }

  const results = []
  for (const scenario of selected) {
    console.log(`\n▶ ${scenario.name}`)
    const started = Date.now()
    const code = await run('docker', k6Args(scenario))
    results.push({
      name: scenario.name,
      ok: code === 0,
      duration: formatDuration(Date.now() - started),
    })
  }

  console.log('\nResumen')
  const width = Math.max(...results.map((result) => result.name.length))
  for (const result of results) {
    console.log(`  ${result.name.padEnd(width)}  ${result.ok ? 'ok' : 'falló'}  ${result.duration}`)
  }
  process.exit(results.every((result) => result.ok) ? 0 : 1)
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error)
  process.exit(1)
})
