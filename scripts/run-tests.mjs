import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const isWindows = process.platform === 'win32'
const frontendDir = path.join(root, 'frontend')

const acceptanceArgs = [
  'compose',
  '-p',
  'ctv-acceptance-local',
  '-f',
  'acceptance-tests/docker-compose.yml',
  '-f',
  'acceptance-tests/docker-compose.isolated.yml',
]
const e2eArgs = ['compose', '-f', 'docker-compose.e2e.yml']

const startedProjects = new Set()
let shuttingDown = false

function usage() {
  console.error('Uso: node scripts/run-tests.mjs [all|unit|acceptance|e2e]')
}

function prefixStream(stream, label, target) {
  let pending = ''
  stream.setEncoding('utf8')
  stream.on('data', (chunk) => {
    pending += chunk
    const lines = pending.split(/\r?\n/)
    pending = lines.pop() ?? ''
    for (const line of lines) {
      target.write(`[${label}] ${line}\n`)
    }
  })
  stream.on('end', () => {
    if (pending.length > 0) {
      target.write(`[${label}] ${pending}\n`)
    }
  })
}

function run(command, args, cwd, options = {}) {
  const label = options.label
  return new Promise((resolve) => {
    const child = spawn(command, args, {
      cwd,
      env: process.env,
      shell: isWindows,
      stdio: label ? ['ignore', 'pipe', 'pipe'] : (options.stdio ?? 'inherit'),
    })

    if (label) {
      prefixStream(child.stdout, label, process.stdout)
      prefixStream(child.stderr, label, process.stderr)
    }

    child.on('close', (code) => resolve(code ?? 1))
    child.on('error', (error) => {
      const message = label ? `[${label}] ${error.message}` : error.message
      console.error(message)
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

function gradleTest(moduleDir, label) {
  const command = isWindows ? 'gradlew.bat' : './gradlew'
  return run(command, ['test', '--console=plain'], moduleDir, { label })
}

async function ensureFrontendDeps() {
  const modules = path.join(frontendDir, 'node_modules')
  if (existsSync(path.join(modules, 'vitest')) && existsSync(path.join(modules, 'cypress'))) {
    return 0
  }
  console.log('Instalando dependencias del frontend (npm ci)...')
  return run('npm', ['ci'], frontendDir)
}

async function dockerReady() {
  return (await run('docker', ['info'], root, { stdio: 'ignore' })) === 0
}

async function composeDown(args) {
  await run('docker', [...args, 'down', '--remove-orphans'], root)
}

async function cleanupStarted() {
  if (startedProjects.has('acceptance')) {
    await composeDown(acceptanceArgs)
  }
  if (startedProjects.has('e2e')) {
    await composeDown(e2eArgs)
  }
}

async function runUnit() {
  console.log('\n▶ Tests unitarios e integración (en paralelo)')
  const jobs = [
    ['backend', () => gradleTest(path.join(root, 'backend'), 'backend')],
    ['flights-service', () => gradleTest(path.join(root, 'flights-service'), 'flights')],
    [
      'frontend',
      async () => {
        const install = await ensureFrontendDeps()
        if (install !== 0) {
          return install
        }
        return run('npm', ['test'], frontendDir, { label: 'frontend' })
      },
    ],
  ]

  return Promise.all(
    jobs.map(async ([name, fn]) => {
      const started = Date.now()
      const code = await fn()
      return {
        name,
        ok: code === 0,
        duration: formatDuration(Date.now() - started),
        code,
      }
    }),
  )
}

async function runAcceptance() {
  if (!(await dockerReady())) {
    console.error('Docker no responde. Abrí Docker Desktop y volvé a correr la aceptación.')
    return 1
  }

  startedProjects.add('acceptance')
  let code = 1
  try {
    code = await run(
      'docker',
      [...acceptanceArgs, 'up', '--build', '-d', '--wait', '--wait-timeout', '180', 'backend'],
      root,
    )
    if (code === 0) {
      code = await run('docker', [...acceptanceArgs, 'run', '--rm', '--no-deps', 'acceptance-tests'], root)
    }
  } finally {
    await composeDown(acceptanceArgs)
    startedProjects.delete('acceptance')
  }
  return code
}

async function runE2e() {
  if (!(await dockerReady())) {
    console.error('Docker no responde. Abrí Docker Desktop y volvé a correr Cypress.')
    return 1
  }

  const install = await ensureFrontendDeps()
  if (install !== 0) {
    return install
  }

  startedProjects.add('e2e')
  let code = 1
  try {
    code = await run(
      'docker',
      [...e2eArgs, 'up', '--build', '-d', '--wait', '--wait-timeout', '180'],
      root,
    )
    if (code === 0) {
      code = await run(
        'npm',
        ['run', 'e2e:run', '--', '--browser', 'electron'],
        frontendDir,
      )
    }
  } finally {
    await composeDown(e2eArgs)
    startedProjects.delete('e2e')
  }
  return code
}

async function timed(name, fn) {
  const started = Date.now()
  console.log(`\n▶ ${name}`)
  const code = await fn()
  return {
    name,
    ok: code === 0,
    duration: formatDuration(Date.now() - started),
    code,
  }
}

function printSummary(results) {
  console.log('\nResumen')
  const width = Math.max(...results.map((result) => result.name.length))
  for (const result of results) {
    const status = result.ok ? 'ok' : 'falló'
    console.log(`  ${result.name.padEnd(width)}  ${status.padEnd(5)}  ${result.duration}`)
  }
}

async function main() {
  const requested = process.argv[2] ?? 'all'
  if (!['all', 'unit', 'acceptance', 'e2e'].includes(requested)) {
    usage()
    process.exit(2)
  }

  const results = []
  if (requested === 'all' || requested === 'unit') {
    results.push(...(await runUnit()))
  }
  if (requested === 'all' || requested === 'acceptance') {
    results.push(await timed('aceptación', runAcceptance))
  }
  if (requested === 'all' || requested === 'e2e') {
    results.push(await timed('e2e', runE2e))
  }

  printSummary(results)
  process.exit(results.every((result) => result.ok) ? 0 : 1)
}

process.on('SIGINT', () => {
  if (shuttingDown) {
    return
  }
  shuttingDown = true
  console.log('\nCorte pedido. Apagando los entornos de test...')
  cleanupStarted().finally(() => process.exit(130))
})

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error)
  process.exit(1)
})
