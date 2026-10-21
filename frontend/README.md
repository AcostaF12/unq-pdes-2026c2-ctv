# Frontend

React 19 + TypeScript + Vite. Consume la API del backend (`http://localhost:8080` por defecto).

## Scripts

```bash
npm ci            # instala dependencias según el lock
npm run dev       # servidor de desarrollo en http://localhost:5173
npm run lint
npm test          # Vitest + Testing Library
npm run build     # tsc -b + vite build; deja el reporte de bundle en dist/stats.html
```

## Configuración

| Variable | Default | Uso |
|----------|---------|-----|
| `VITE_API_BASE_URL` | `http://localhost:8080` | URL base de la API. Se resuelve al compilar, no al ejecutar la imagen. |

Para otra URL en Docker se pasa como build arg: `docker build --build-arg VITE_API_BASE_URL=http://host:8080 .`

## Estructura

```
src/
├── api/          # cliente axios y un módulo por recurso
├── auth/         # contexto de sesión y guards de rutas por rol
├── components/   # componentes de UI reutilizables
├── pages/        # una carpeta por pantalla
├── query/        # QueryClient y claves de TanStack Query
├── router/       # rutas y carga diferida de páginas
└── styles/       # tokens de diseño y tipografías
```

El diseño de la fase 1 y el backlog están en [`docs/frontend`](../docs/frontend).

## Pruebas e2e

Desde la raíz del repositorio, con Docker Desktop abierto:

```bash
docker compose -f docker-compose.e2e.yml up --build -d --wait --wait-timeout 180
```

Abrir http://localhost:18090. Comprador: `buyer` / `buyer123`.
Administrador: `vferreyra` / `vferreyra`. El backend está en http://localhost:18080.
La API de vuelos se publica en http://localhost:18081 solo para preparar datos e2e.

Este entorno usa el proyecto Docker `ctv-e2e` y bases PostgreSQL temporales,
separadas del entorno de desarrollo y del módulo Cucumber. Reutiliza sus
servicios mediante `extends`; no ejecuta los escenarios Cucumber.
Los datos iniciales incluyen paquetes, hoteles y vuelos con fechas relativas
al día de arranque. Los tests deberán preparar sus datos antes de modificar el estado.

Para apagarlo y eliminar los datos de prueba:

```bash
docker compose -f docker-compose.e2e.yml down
```

Para empezar otra ejecución con los datos iniciales, ejecutar `down` y luego
el comando `up` anterior.

### Cypress

Con Node.js instalado, desde `frontend/`:

```bash
npm ci
npm run e2e:open
```

`e2e:open` abre la interfaz de Cypress para ver las acciones en el navegador.
`npm run e2e:run` ejecuta los tests sin abrir la interfaz, como se hará en CI.
El entorno Docker anterior debe estar levantado. Los escenarios se escribirán
en `frontend/cypress/e2e/` con nombres terminados en `.cy.js` o `.cy.ts`;
La suite verifica login/búsqueda/detalle, compra/historial, creación/edición/eliminación
de hoteles, contraseña incorrecta y permisos del comprador. Se usan servicios reales.
`admin-management.cy.js` cubre la sección Administración: acceso por rol, alta de vuelos de ida y
vuelta, alta de un paquete para una agencia, su compra por un comprador, edición y eliminación
(incluido el rechazo al borrar un paquete ya comprado) y el alta de paquetes desde la agencia con
el selector de vuelos. Los vuelos no tienen endpoint de baja: quedan hasta ejecutar `down`.
La compra prepara un comprador, dos vuelos y un paquete propios mediante APIs;
la operación de compra se hace desde la interfaz. El hotel usa un nombre único
y se elimina al finalizar el recorrido. Los datos restantes se eliminan con `down`.
La configuración está en `frontend/cypress.config.js` y apunta a http://localhost:18090.
No se generan videos ni capturas automáticas de fallos.

Para ejecutar toda la suite en Electron: `npm run e2e:run -- --browser electron`.
El workflow `Acceptance and E2E CI` ejecuta este comando en cada PR y push a `dev` o `main`,
levanta el entorno Docker y lo apaga al finalizar. Los resultados y los logs
de la aplicación en caso de fallo se consultan en GitHub Actions; no sube archivos.
