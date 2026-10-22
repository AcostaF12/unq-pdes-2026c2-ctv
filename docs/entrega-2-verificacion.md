# Verificación de la entrega 2

La entrega se verifica combinando pruebas locales, recorrido con servicios reales y evidencias del PR/CI. Una compilación local exitosa no demuestra que Sonar haya aprobado el PR ni que las imágenes se hayan publicado.

## Ejecución local del 22 de octubre de 2026

- Backend: build y ktlint correctos; 416 pruebas, sin fallos ni omisiones. Incluye 7 pruebas sobre PostgreSQL real.
- Servicio de vuelos: build y ktlint correctos; 59 pruebas sin fallos.
- Frontend: build correcto, 201 pruebas sin fallos. ESLint sin errores; advertencia existente en `Profile.tsx` por la dependencia `prefsTick` de `useMemo`.
- Cypress: 13 escenarios funcionales correctos en las 6 suites contra el stack Docker.
- Swagger servido por el backend: contratos de usuarios, cambios de permisos y ventas globales disponibles, con esquema paginado de usuarios.

Pendiente para acreditar la entrega remota: revisión del PR, ejecución de CI/Sonar y publicación de las imágenes del commit. Estos resultados locales no reemplazan esas evidencias.

## Matriz funcional

| Flujo | Administrador | Vendedor (`AGENCY`) | Comprador (`BUYER`) |
| --- | --- | --- | --- |
| Usuarios y roles | Crear, editar rol/agencia, habilitar/deshabilitar | Sin acceso | Sin acceso |
| Agencias | Crear y renombrar | Consultar | Consultar |
| Hoteles y vuelos | Gestionar hoteles y cargar vuelos | Consultar | Consultar |
| Paquetes | Gestionar para cualquier agencia | Gestionar los de su agencia | Buscar y consultar |
| Compra, favoritos y reseña | Consulta del catálogo | Consulta del catálogo | Comprar, favoritos, reseña con compra previa |
| Ventas e historial | Todas las agencias y detalle | Solo ventas de su agencia | Solo sus compras |

La API comprueba estos permisos; ocultar botones es solo una ayuda visual. El estado de cuenta y rol se consultan con cada petición autenticada. El frontend vuelve a consultar `/users/me` al recuperar el foco.

Reglas de cuentas: el registro público siempre crea compradores. Un vendedor necesita una agencia existente; los otros roles no admiten agencia. Deshabilitar bloquea el login y los tokens existentes con HTTP 401. El administrador no puede deshabilitarse ni quitarse su propio rol. Los cambios de rol conservan el identificador, la contraseña y el historial; reasignar un vendedor no transfiere los paquetes ni ventas de la agencia original.

`POST /admin/users` recibe `username`, `password`, `firstName`, `lastName`, `role` y `agencyId` si el rol es `AGENCY`. `PUT /admin/users/{id}` recibe `firstName`, `lastName`, `role`, `enabled` obligatorio y `agencyId` solo para vendedor. El listado paginado usa `GET /admin/users?page=0&size=20` y admite hasta 100 registros por página. Ninguna respuesta administrativa incluye contraseñas ni hashes.

## Pruebas reproducibles

Requisitos: Docker Desktop en ejecución, Java 25 (o toolchain Gradle configurado), Node.js 24 y dependencias instaladas con `npm ci` dentro de `frontend/`.

Desde cada directorio:

```powershell
# backend/
.\gradlew.bat build

# flights-service/
.\gradlew.bat build

# frontend/
npm run lint
npm test
npm run build
```

En Linux/macOS usar `./gradlew`. El build ejecuta tests y ktlint; JaCoCo deja cobertura en `build/reports/jacoco/test/html/index.html`. Con Docker disponible, `PostgresQueriesTest` verifica Flyway, consultas y cambios de rol sobre PostgreSQL real, además de los tests H2.

Desde la raíz, levantar el entorno aislado:

```powershell
docker compose -f docker-compose.e2e.yml up --build -d --wait --wait-timeout 180
```

Desde `frontend/`:

```powershell
npm run e2e:run -- --browser electron
```

Los escenarios nuevos de `admin-users.cy.js` crean una agencia y un vendedor desde la interfaz, verifican una venta como vendedor y administrador, comprueban el bloqueo de una cuenta con token previo y cambian su rol. También rechazan las pantallas y APIs administrativas para comprador y vendedor. `admin-management.cy.js` cubre vuelos, publicación/edición/baja de paquetes y compra. Los nombres de los datos son únicos por ejecución; quedan en las bases temporales hasta apagar el entorno.

Al terminar, desde la raíz:

```powershell
docker compose -f docker-compose.e2e.yml down
```

Para el recorrido manual usar la [guía administrativa](admin.md). El entorno de prueba sirve la interfaz en http://localhost:18090 y Swagger en http://localhost:18080/swagger-ui/index.html. Para desarrollo y demo se usan los puertos 8090/8080 indicados en el README.

## Evidencias para la rúbrica

| Requisito | Evidencia del repositorio | Qué comprobar para entregar |
| --- | --- | --- |
| Enunciado y stack | README y definición del proyecto | Que el alcance acordado esté descrito |
| Frontend y backend | `frontend/`, `backend/`, `flights-service/` | Build y recorrido completo con datos persistidos |
| MER / clases | `docs/mer.puml`, `docs/mer.png` | Usuario, agencia, roles, estado y relaciones actualizados |
| App Admin / usuario común | `/admin`, `/agency`, `/trips`, `/purchases` | Matriz de permisos y escenarios anteriores |
| README / Swagger | README, `docs/admin.md`, `/v3/api-docs` | Contratos nuevos presentes y comandos correctos |
| Docker / Compose | Dockerfiles, `docker-compose.yml`, `docker-compose.e2e.yml`, `docker-compose.hub.yml` | Servicios healthy y arranque con imágenes publicadas |
| Git / Issues | Historial y tablero del repositorio | Vincular el PR con los issues correspondientes |
| Code Review / branch / PR | PR con diff y revisión | Aprobación de otra persona y resolución de observaciones |
| CI | `.github/workflows/*` | Ejecución verde asociada al SHA del PR |
| Sonar / Quality Gate | Jobs de backend y flights-service con espera del gate | Resultado remoto del SHA, cobertura y observaciones resueltas |
| C. Delivery / Docker Hub | Jobs `publish` y Compose Hub | Tags SHA y `latest` de las tres imágenes después del merge a main |
| Test unitario / integración | JUnit, Vitest, tests PostgreSQL | Reportes sin fallos; PostgreSQL sin omitir por falta de Docker |
| Test interfaz / e2e | Cypress; workflow `e2e.yml` | Suite verde contra ambos servicios y sus bases reales |

La publicación requiere los secrets `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN`; Sonar requiere `SONAR_TOKEN`. No se debe dar por aprobada esa parte solo porque los workflows están definidos. Adjuntar al PR los enlaces a las ejecuciones y a las imágenes del commit entregado.
