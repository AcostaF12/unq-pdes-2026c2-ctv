# unq-pdes-2026c2-ctv

Compra Tu Viaje: aplicación web para buscar, comprar y gestionar paquetes turísticos, con tres perfiles: comprador, agencia y administrador.

Proyecto de la materia Prácticas de Desarrollo de Software – UNQ, 2do semestre 2026.

### Backend

[![Backend CI](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/backend.yml/badge.svg)](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/backend.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-backend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-backend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-backend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-backend)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-backend&metric=bugs)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-backend)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-backend&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-backend)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-backend&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-backend)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-backend&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-backend)

### Flights Service

[![Flights Service CI](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/flights-service.yml/badge.svg)](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/flights-service.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-flights-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-flights-service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-flights-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-flights-service)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-flights-service&metric=bugs)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-flights-service)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-flights-service&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-flights-service)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-flights-service&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-flights-service)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=acostaf12_unq-pdes-2026c2-ctv-flights-service&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-flights-service)

### Frontend

[![Frontend CI](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/frontend.yml/badge.svg)](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/frontend.yml)

### Aceptación y E2E

[![Acceptance and E2E CI](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/e2e.yml/badge.svg)](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/e2e.yml)
[![Workflow Validation CI](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/workflow-validation.yml/badge.svg)](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/workflow-validation.yml)

### Kubernetes

[![Kubernetes CI](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/kubernetes.yml/badge.svg)](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/workflows/kubernetes.yml)

---

## 📝 Release Notes - Entrega 1

Release [`ctv-version-1`](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/releases/tag/ctv-version-1).

- Base del proyecto: backend, servicio de vuelos y frontend, cada servicio Kotlin con su PostgreSQL.
- Modelo y MER de usuarios, agencias, ciudades, hoteles, paquetes y vuelos, con datos de ejemplo.
- Autenticación con JWT y autorización por rol: comprador, agencia y administrador.
- Swagger del backend, tests unitarios, de integración y de arquitectura (ArchUnit), con cobertura de JaCoCo.
- CI en GitHub Actions con análisis de SonarCloud, Docker Compose y observabilidad: Prometheus, Grafana, Zipkin y ELK.
- Deploy en una VM de Azure con las imágenes publicadas en GHCR.

## 📝 Release Notes - Entrega 2

Release [`ctv-version-2`](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/releases/tag/ctv-version-2). Detalle completo en el [release de GitHub `ctv-version-2`](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/releases/tag/ctv-version-2).

- Catálogo con buscador avanzado (nombre, origen, destino, rango de precios, agencia, hotel y puntaje), paginado y ordenable.
- Favoritos, compra de paquetes y reseñas de quien compró. La compra vende ida y vuelta en el servicio de vuelos, compensa si falla la vuelta y bloquea el último asiento.
- Historial de compras y ventas con el precio y los datos congelados al momento de la compra.
- Agencias: ABM de sus paquetes eligiendo hotel y vuelos de una lista, archivo de ofertas vendidas y consulta de ventas.
- Administración: usuarios, agencias, hoteles, vuelos y paquetes; consultas de favoritos, reseñas y compras; cuatro reportes Top 5.
- Esquema versionado con Flyway y seed idempotente que no pisa los datos al reiniciar.
- Tests de aceptación con Cucumber, tests de interfaz con Cypress, ktlint, y el deploy manual que repite todos los controles antes de publicar.

## 📝 Release Notes - Entrega 3

- **Tests de stress con k6:** catálogo, login, último asiento y pico de tráfico, con el workflow "Load Tests" contra el runner o la VM. Contra la VM pasaron los cuatro escenarios sin errores, y en el del último asiento se vendieron 5 de 30 intentos, sin sobreventa ([resultados](docs/load-tests.md#resultados)).
- **Observabilidad:** métricas en Prometheus y Grafana (dashboard `CTV - Overview`), logs por request con `traceId` de los dos servicios en ELK (data view `ctv-logs-*`) y traza distribuida backend → flights-service en Zipkin.
- **Kubernetes:** manifiestos con Kustomize en `k8s/` (Postgres en `StatefulSet`, 2 réplicas de backend y frontend, probes, `ConfigMap` y `Secret`) y el workflow "Kubernetes CI", que levanta un cluster kind, hace un smoke test y comprueba que se repone un pod borrado ([guía](docs/kubernetes.md)).

---

## 🚀 Estructura

```
├── backend/                  # Spring Boot - CTV (Kotlin)
├── flights-service/          # Spring Boot - API de vuelos (Kotlin), interna
├── frontend/                 # React + TypeScript + Vite
│   └── cypress/              # Tests de interfaz (Cypress)
├── acceptance-tests/         # Tests de aceptación (Cucumber)
├── load-tests/               # Tests de carga y stress (k6)
├── monitoring/               # Configuración de Prometheus, Grafana, Logstash y el data view de Kibana
├── k8s/                      # Manifiestos de Kubernetes (Kustomize) y cluster kind
├── docs/                     # MER, flujo, guion de demo, administración, seed, carga y Kubernetes
├── scripts/                  # Validaciones de la CI, build de imágenes y reintentos de Gradle
├── .github/workflows/        # CI, publicación de imágenes y deploy
├── docker-compose.yml        # Entorno local completo
├── docker-compose.prod.yml   # Overlay para la VM: imágenes de GHCR
├── docker-compose.e2e.yml    # Entorno aislado para Cypress
└── package.json              # Atajos npm para levantar el entorno y correr las pruebas
```

---

## 🛠️ Tecnologías

**Backend / Flights Service:** Spring Boot 4.1.0 + Kotlin 2.3.21 + Java 25 (Gradle 9.5.1)  
**Frontend:** React 19 + TypeScript 6 + Vite 8 + TanStack Query  
**BD:** PostgreSQL 16 (2 instancias), con migraciones Flyway (`V1` a `V7` más `V7.1` solo para PostgreSQL en el backend, `V1` y `V2` en flights-service)  
**Seguridad:** Spring Security + JWT  
**Resiliencia:** Resilience4j (reintentos y circuit breaker hacia el servicio de vuelos)  
**Pruebas:** JUnit 5 + H2 en memoria + Testcontainers (PostgreSQL) + ArchUnit + JaCoCo · Vitest · Cucumber · Cypress · k6  
**Calidad:** SonarCloud · ktlint · ESLint  
**Documentación de la API:** Swagger/OpenAPI (springdoc), solo backend  
**Observabilidad:** Spring Boot Actuator + Prometheus (métricas) + Grafana (dashboards) + Zipkin (trazas) + ELK / Elasticsearch + Logstash + Kibana (logs)  
**CI/CD:** GitHub Actions + GitHub Container Registry  
**Containerización:** Docker + Docker Compose · Kubernetes (Kustomize, kind)  
**Nube:** Azure (VM)

---

## 🚀 Cómo correr

Con Docker Desktop abierto, desde la raíz del repositorio, levantar la aplicación (2 bases de datos, backend, flights-service y frontend):

```bash
docker compose up --build -d --wait
```

El monitoreo está partido. `monitoring` prende los tres perfiles y es el de la demo completa y de la VM:

```bash
docker compose --profile monitoring up --build -d --wait
```

| Perfil | Qué suma | Cuándo |
|--------|----------|--------|
| `metrics` | Prometheus y Grafana | Medir k6. Es el liviano |
| `logs` | Elasticsearch, Logstash y Kibana | Mostrar logs |
| `traces` | Zipkin | Mostrar una traza |
| `monitoring` | Los tres | Demo completa |

Para medir sin Elasticsearch ni Zipkin: `docker compose --profile metrics up --build -d --wait`. Para ver métricas en Grafana y logs en Kibana, sin Zipkin: `--profile metrics --profile logs`.

La primera vez tarda porque descarga imágenes y compila. `--wait` espera a que pasen los healthchecks. Sin el perfil `logs` o `traces` pueden aparecer avisos de conexión a Logstash y Zipkin en los logs; la aplicación funciona igual.

Con Node, desde la raíz: `npm start`, `npm run start:metrics`, `npm run start:logs`, `npm run start:traces`, `npm run start:observe` (métricas y logs) y `npm run start:monitoring`.

Para cambiar la configuración (por ejemplo, los puertos de las bases), copiar el archivo de ejemplo y editarlo antes de levantar. Docker Compose lo lee solo:

```bash
cp .env.example .env
```

`.env` no se versiona. Las variables se describen en la sección Configuración.

### Comandos útiles

```bash
docker compose ps                              # estado de los contenedores
docker compose logs -f backend                 # logs de un servicio
docker compose --profile monitoring down       # apagar todo, conservando los datos
docker compose --profile monitoring down -v    # apagar y borrar los volúmenes (datos desde cero)
```

Atajos equivalentes: `npm run ps`, `npm run logs` y `npm run down`.

Los datos de ejemplo se cargan una sola vez y se conservan entre reinicios. Para empezar de cero, usar `down -v`.

### Servicios expuestos

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:8090 |
| Backend | http://localhost:8080 |
| Swagger (Backend) | http://localhost:8080/swagger-ui/index.html |
| Prometheus (`metrics`, solo local) | http://localhost:9090 |
| Grafana (`metrics`) | http://localhost:3001 (usuario `admin` / contraseña `admin`) |
| Zipkin (`traces`) | http://localhost:9411 |
| Kibana (`logs`) | http://localhost:5601 |

Con el perfil `monitoring` (los tres):

- **Grafana** abre en `CTV - Overview` (servicios activos, uptime, heap usado contra el máximo y requests por segundo, sin `/actuator`). `CTV - Load` muestra el p95 por endpoint (sin `/actuator`), los 5xx, el pool de Hikari, el circuit breaker y el p95 de la búsqueda. Los dos dashboards se enlazan. Grafana no consulta logs: eso queda en Kibana, así el perfil `metrics` no deja un datasource caído.
- **Zipkin** muestra cada llamada del backend a flights-service como una sola traza con los dos servicios; la pestaña Dependencies dibuja el arco backend → flights-service.
- **Kibana** arranca con el data view `ctv-logs-*` (campo de tiempo `@timestamp`), creado por el job `kibana-setup`. Para recrearlo: `docker compose --profile logs run --rm kibana-setup`. Los logs incluyen `traceId` y `spanId`, así que se puede filtrar por el `traceId` de una traza de Zipkin. El backend y flights-service loguean cada request (método, ruta, status y duración, sin `/actuator/**`) con su `traceId`, así que esa búsqueda trae líneas de los dos servicios.

### Usuarios de demo

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Comprador | `buyer`, `buyer2` | `buyer123` |
| Agencia | `agency` (Despegar), `agency2`, `agency3`, `agency4` | `agency123` |
| Administrador | `vferreyra`, `facosta`, `adisanto` | igual al usuario |

Son credenciales de ejemplo, no de producción. El dataset completo está en [seed y ciclo de vida](docs/seed-y-ciclo-de-vida.md).

### Desarrollo con hot reload

Con Java 25 y Node.js 24, levantar solo las bases en Docker y cada módulo en una terminal:

```bash
docker compose up -d backend-postgres flights-postgres
cd flights-service && ./gradlew bootRun
cd backend && ./gradlew bootRun
cd frontend && npm ci && npm run dev    # http://localhost:5173
```

Si los puertos `5433` o `5434` están ocupados, cambiar `BACKEND_DB_PORT` y `FLIGHTS_DB_PORT` y correr el servicio correspondiente con `DB_PORT` apuntando al puerto elegido. Sin el monitoreo, `TRACING_SAMPLING_PROBABILITY=0` evita los avisos de Zipkin.

---

## ⚙️ Configuración

Los servicios se configuran con variables de entorno. Todas tienen un valor por defecto para desarrollo local; Docker Compose ya define las necesarias.

[`.env.example`](.env.example) reúne todas las variables con valores de ejemplo seguros, agrupadas por servicio. Para usarlo: `cp .env.example .env`. Docker Compose toma de `.env` las que interpola (`BACKEND_DB_PORT`, `FLIGHTS_DB_PORT` y, en el overlay de la VM, `SEED_ENABLED`, `CORS_ALLOWED_ORIGIN_PATTERNS` y `JWT_SECRET`); el resto queda como referencia para correr un servicio fuera de Docker.

`JWT_SECRET` trae solo un placeholder y nunca se versiona. En local es opcional (el backend usa un secreto de desarrollo público), pero con el overlay `docker-compose.prod.yml` es obligatoria: sin ella Compose no levanta. Debe tener al menos 32 bytes y no puede llevar comillas, espacios, `$`, backticks ni barras invertidas. Para generarla:

```bash
openssl rand -base64 48
```

En la VM la define el deploy a partir del secreto `JWT_SECRET` del repositorio en GitHub: escribe `/opt/ctv/.env` con permisos 600, reemplaza solo la línea de `JWT_SECRET` y conserva el resto (por ejemplo, CORS y seed).

### Backend

| Variable | Por defecto | Ejemplo | Para qué |
|----------|---------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` | Perfil de Spring |
| `DB_HOST` / `DB_PORT` | `localhost` / `5433` | `backend-postgres` / `5432` | Conexión a PostgreSQL |
| `DB_NAME` | `backend_dev` | `backend` | Nombre de la base |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | `ctv` / `s3cret` | Credenciales de la base |
| `FLIGHTS_SERVICE_URL` | `http://localhost:8081` | `http://flights-service:8081` | URL del servicio de vuelos |
| `JWT_SECRET` | secreto de desarrollo | salida de `openssl rand -base64 48` | Firma de los tokens (32 bytes o más). Obligatoria con el overlay de la VM |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | `3600000` | Duración del token |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | `http://localhost:*,http://127.0.0.1:*` | `http://64.236.199.81:*` | Orígenes permitidos para el frontend |
| `SEED_ENABLED` | `true` en `dev`, `false` en `prod` | `false` | Carga de datos de ejemplo |
| `DDL_AUTO` | `validate` | `validate` | Modo de Hibernate. El esquema lo maneja Flyway; Hibernate solo lo valida |

Los valores por defecto de la base son los del perfil `dev`. Con `prod`, el backend usa `5432` y la base `backend`, y flights-service usa la base `flights` (el puerto sigue en `5434`). Compose y Kubernetes definen `DB_HOST`, `DB_PORT` y `DB_NAME` siempre, así que esos valores solo cuentan fuera de Docker.

### Flights Service

| Variable | Por defecto | Ejemplo | Para qué |
|----------|---------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` | Perfil de Spring |
| `SERVER_PORT` | `8081` | `8081` | Puerto HTTP |
| `DB_HOST` / `DB_PORT` | `localhost` / `5434` | `flights-postgres` / `5432` | Conexión a PostgreSQL |
| `DB_NAME` | `flights_dev` | `flights` | Nombre de la base |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | `ctv` / `s3cret` | Credenciales de la base |
| `SEED_ENABLED` | `true` en `dev`, `false` en `prod` | `false` | Carga de vuelos de ejemplo |
| `DDL_AUTO` | `validate` | `validate` | Modo de Hibernate. El esquema lo maneja Flyway; Hibernate solo lo valida |

### Observabilidad (backend y flights-service)

| Variable | Por defecto | Ejemplo | Para qué |
|----------|---------|---------|----------|
| `ZIPKIN_ENDPOINT` | `http://localhost:9411/api/v2/spans` | `http://zipkin:9411/api/v2/spans` | Destino de las trazas |
| `TRACING_SAMPLING_PROBABILITY` | `1.0` | `0` | Porcentaje de requests trazados (`0` lo apaga) |
| `LOGSTASH_HOST` / `LOGSTASH_PORT` | `localhost` / `5000` | `logstash` / `5000` | Destino de los logs JSON |

### Frontend y Docker Compose

| Variable | Por defecto | Ejemplo | Para qué |
|----------|---------|---------|----------|
| `VITE_API_BASE_URL` | `http://localhost:8080` | `http://64.236.199.81:8080` | URL del backend. Se fija al construir la imagen (build-arg) |
| `BACKEND_DB_PORT` | `5433` | `15433` | Puerto del host para la base del backend |
| `FLIGHTS_DB_PORT` | `5434` | `15434` | Puerto del host para la base de vuelos |

Ejemplo, levantar con otros puertos para las bases:

```bash
BACKEND_DB_PORT=15433 FLIGHTS_DB_PORT=15434 docker compose up --build -d --wait
```

---

## 🐳 Arquitectura de Docker

Cada módulo propio tiene un Dockerfile multi-stage: se compila en una imagen (JDK o Node) y se corre en otra más chica (JRE o nginx).

- Las imágenes corren sin root: `backend` y `flights-service` con el usuario `app` (uid 10001) y `frontend` con `nginx` (uid 101) sobre `nginxinc/nginx-unprivileged`, que escucha en el puerto 8080 dentro del contenedor (en el host sigue siendo 8090 en local y 18090 en e2e).
- Los Dockerfiles de Kotlin resuelven las dependencias de Gradle en una capa propia antes de copiar `src`: cambiar código no vuelve a descargarlas.
- Todas las imágenes base y de terceros usan versiones fijas: Temurin `25.0.5_8-jdk`/`25.0.5_8-jre`, Node `24.22-alpine` y `nginxinc/nginx-unprivileged:1.31-alpine` en los Dockerfiles, y las de la tabla en Compose.

`docker-compose.yml` define **12 servicios** y **5 redes**. Por defecto levanta los 5 de la aplicación. Los otros 7 están en tres perfiles (`metrics`, `logs`, `traces`); `monitoring` es la unión y los prende todos. `kibana-setup` es un job que termina al crear el data view:

| Contenedor | Perfil | Imagen | Puerto local | En la VM | Rol |
|------------|--------|--------|--------------|----------|-----|
| `frontend` | siempre | build de `frontend/` (`nginx-unprivileged:1.31-alpine`) | 8090 → 8080 | 8090 | Sirve la SPA |
| `backend` | siempre | build de `backend/` (Temurin 25.0.5) | 8080 | 8080 | API de CTV |
| `flights-service` | siempre | build de `flights-service/` (Temurin 25.0.5) | interno | interno | API de vuelos, solo la usa el backend |
| `backend-postgres` | siempre | `postgres:16.16` | 5433 | no se publica | Base del backend |
| `flights-postgres` | siempre | `postgres:16.16` | 5434 | no se publica | Base de vuelos |
| `prometheus` | `metrics` | `prom/prometheus:v3.16.0` | 9090 | no se publica | Métricas |
| `grafana` | `metrics` | `grafana/grafana:13.3.1` | 3001 | 3001 | Dashboards |
| `elasticsearch` | `logs` | `docker.elastic.co/elasticsearch/elasticsearch:8.15.3` | 9200 | no se publica | Índice de logs |
| `logstash` | `logs` | `docker.elastic.co/logstash/logstash:8.15.3` | 5000 | no se publica | Recibe los logs |
| `kibana` | `logs` | `docker.elastic.co/kibana/kibana:8.15.3` | 5601 | 5601 | Consulta de logs |
| `kibana-setup` | `logs` | `curlimages/curl:8.17.0` | — | — | Job de una sola vez: crea el data view `ctv-logs-*` y termina |
| `zipkin` | `traces` | `openzipkin/zipkin:3.6.1` | 9411 | 9411 | Trazas |

En la VM, `docker-compose.prod.yml` quita los puertos de las bases, Elasticsearch, Logstash y Prometheus (`ports: !reset []`): solo se alcanzan por las redes internas (Grafana llega a Prometheus por la red interna).

Las redes aíslan quién habla con quién:

| Red | Contenedores | Qué aísla |
|-----|--------------|-----------|
| `frontend-net` | frontend | nginx no ve bases ni APIs |
| `backend-net` | backend, su Postgres, Prometheus, Grafana, Logstash | El servicio de vuelos no accede a la base del negocio |
| `flights-net` | flights-service, su Postgres, backend, Prometheus, Logstash | El stock de vuelos |
| `tracing-net` | backend, flights-service, Zipkin | Las trazas |
| `logging-net` | Logstash, Elasticsearch, Kibana, kibana-setup | El índice de logs. Kibana es la pantalla de ELK |

El arranque respeta las dependencias con healthchecks: las bases, después vuelos, después el backend. Los datos viven en volúmenes nombrados.

Otros archivos de Compose:

- `docker-compose.prod.yml`: overlay para la VM. Usa las imágenes publicadas en GHCR en lugar de compilar, exige `JWT_SECRET` y no publica los puertos internos.
- `acceptance-tests/docker-compose.yml`: entorno de Cucumber, con bases en memoria (`tmpfs`).
- `docker-compose.e2e.yml`: entorno de Cypress, con puertos propios (18080, 18081, 18090) para convivir con el de desarrollo.

---

## ☸️ Kubernetes

`k8s/` despliega la misma aplicación en un cluster de Kubernetes con Kustomize: PostgreSQL como `StatefulSet` con volumen persistente, backend y frontend con 2 réplicas cada uno, flights-service solo interno, probes de Actuator y configuración en `ConfigMap`/`Secret`. El cluster repone solo los pods que se caen. El monitoreo sigue en Compose.

```bash
kind create cluster --config k8s/kind-config.yaml
docker build -t ctv-frontend:local --build-arg VITE_API_BASE_URL=http://localhost:8080 frontend
kind load docker-image --name ctv ctv-frontend:local
kubectl apply -k k8s/
```

Frontend en http://localhost:8090 y backend en http://localhost:8080, igual que con Compose. El paso a paso (también con el Kubernetes de Docker Desktop), la demo de autorecuperación y escalado y las decisiones están en [docs/kubernetes.md](docs/kubernetes.md).

---

## 🧪 Pruebas

Desde la raíz, con Java 25, Node.js 24 y Docker Desktop abierto:

```bash
npm test
```

Corre todas las suites de corrección y deja un resumen al final. Si alguna falla, el comando sigue con el resto y termina con error, así un solo recorrido muestra todo lo que se rompió.

| Comando | Qué corre |
|---------|-----------|
| `npm test` | Todo lo de esta sección, en este orden |
| `npm run test:unit` | Backend, flights-service y Vitest, en paralelo |
| `npm run test:acceptance` | Cucumber, en un Compose propio |
| `npm run test:e2e` | Cypress contra `docker-compose.e2e.yml` |
| `npm run test:stress` | Concurrencia de k6 contra la app ya levantada |

`npm test` no incluye k6. `npm run test:stress` corre la parte de concurrencia (último asiento, throughput, cancelar y recomprar, doble cancelación) contra el backend de `npm start`. El pico de catálogo y el login bajo carga siguen en el workflow manual Load Tests.

### Cómo contarlo

Cada suite responde una pregunta. No se pisan.

| Pregunta | Respuesta |
|----------|-----------|
| ¿El código respeta las capas? | ArchUnit, dentro de `npm run test:unit`. Cinco reglas por servicio: el controller no toca el repositorio, el servicio se llama `Service`. |
| ¿La regla se cumple con Spring levantado? | Integración. H2 para el flujo. Postgres, con Testcontainers, donde H2 no alcanza: Flyway, las consultas y el lock del último asiento. |
| ¿Los dos servicios cierran el recorrido por HTTP? | Cucumber, 14 escenarios, en un Compose aparte. `npm run test:acceptance`. |
| ¿La pantalla hace ese recorrido? | Cypress, 14 tests, contra la app compilada. `npm run test:e2e`. |
| ¿Se puede vender un asiento de más? | k6, la suite corta. En Kubernetes corre contra las 2 réplicas del backend. `npm run test:stress` y el workflow Kubernetes CI. |
| ¿Cómo se ve mientras pasa? | Grafana `CTV - Load`: p95, 5xx, pool y circuit breaker. Las líneas de esa ventana, en Kibana. |

La aceptación usa el proyecto Compose `ctv-acceptance-local` y no publica los puertos 8080 ni 8081, así que puede convivir con `npm start`. Cypress usa 18080, 18081 y 18090, y apaga ese entorno al terminar. La primera vez tarda porque construye las imágenes.

| Suite | Herramienta | Dónde | Cómo correrla |
|-------|-------------|-------|---------------|
| Unitarios e integración | JUnit 5, H2, Testcontainers | `backend/`, `flights-service/` | `npm run test:unit` (o `./gradlew test` en cada módulo) |
| Arquitectura | ArchUnit | `backend/`, `flights-service/` | Incluidos en `./gradlew test` |
| Cobertura | JaCoCo | `backend/`, `flights-service/` | `./gradlew jacocoTestReport` (`build/reports/jacoco/test/html`) |
| Estilo | ktlint | `backend/`, `flights-service/` | `./gradlew build` |
| Frontend | Vitest, ESLint | `frontend/` | Vitest entra en `npm run test:unit`. Lint y build siguen en `frontend/`: `npm run lint` y `npm run build` |
| Aceptación | Cucumber | `acceptance-tests/` | `npm run test:acceptance` |
| Interfaz (E2E) | Cypress | `frontend/cypress/` | `npm run test:e2e` |
| Carga y stress | k6 | `load-tests/` | `npm run test:stress` y Actions → Load Tests (ver [docs/load-tests.md](docs/load-tests.md)) |

Cantidad de pruebas por tipo:

| Tipo | Backend | Flights Service | Frontend | Total |
|------|---------|-----------------|----------|-------|
| Unitarios (incluye 5 reglas ArchUnit por servicio) | 163 | 32 | 236 (Vitest, 37 archivos) | 431 |
| Integración (Spring, H2 y Testcontainers) | 299 | 49 | — | 348 |
| Aceptación (Cucumber) | 14 escenarios | | | 14 |
| Interfaz (Cypress) | | | 14 tests en 7 archivos | 14 |
| Carga y stress (k6) | 7 escenarios | | | 7 |

Los números de backend y flights-service cuentan métodos de test; con los casos de los tests parametrizados, JUnit ejecuta 465 y 88.

Cucumber y Cypress corren contra los servicios y bases reales, en entornos Docker separados del de desarrollo. Los tests de PostgreSQL con Testcontainers necesitan Docker; sin Docker se omiten.

En la CI, los jobs de aceptación y e2e construyen las imágenes con `scripts/ci-build-images.sh`: usa `docker buildx bake` con la caché de GitHub Actions por servicio y reintenta hasta 3 veces (por ejemplo, ante un 429 de Maven Central). Después levantan el entorno con `docker compose up --no-build`.

---

## 🔄 CI/CD

| Workflow | Cuándo | Qué hace |
|----------|--------|----------|
| Backend CI / Flights Service CI | PR y push a `dev` o `main`, si cambia el módulo, su workflow o `scripts/ci-gradle.sh` | Build, tests, ktlint y SonarCloud. En `main` espera el Quality Gate y, en un push, publica la imagen |
| Frontend CI | PR y push a `dev` o `main`, si cambia el frontend o su workflow | Lint, Vitest y build. En un push a `main`, publica la imagen |
| Acceptance and E2E CI | PR y push a `dev` o `main` | Cucumber y Cypress contra la aplicación en Docker |
| Workflow Validation CI | PR y push a `dev` o `main` | actionlint y `scripts/check-ci.rb` |
| Kubernetes CI | PR y push a `dev` o `main`, si cambia `k8s/`, las imágenes, `load-tests/`, los scripts del cluster o su workflow; también manual | Cluster kind con las imágenes del commit: aplica `k8s/`, smoke test, la suite corta de concurrencia contra las 2 réplicas y borra un pod del backend para comprobar que el otro sigue atendiendo |
| Deploy Demo (Azure VM) | Manual | Repite todos los controles, publica las imágenes del commit y despliega en la VM |
| Load Tests | Manual | Corre los escenarios de k6 contra la aplicación en el runner o en la VM |

Backend CI y Flights Service CI guardan la caché de dependencias de Gradle solo en `main` y `dev` (las demás ramas y los PR la leen sin escribirla). El build corre con `scripts/ci-gradle.sh`, que reintenta hasta 3 veces solo ante errores transitorios del repositorio (429, 5xx, timeouts); una falla de compilación, de tests o de ktlint corta en el primer intento. Además, `gradle.properties` le da a Gradle más reintentos por request antes de descartar Maven Central.

En cada push a `main`, después de los controles, las imágenes de `backend`, `flights-service` y `frontend` se publican en GitHub Container Registry (`ghcr.io/acostaf12/ctv-*`) con `latest` y el SHA del commit. La publicación se autentica con el `GITHUB_TOKEN` del workflow; la imagen del frontend se construye con `VITE_API_BASE_URL` apuntando a la VM (secreto `VM_HOST`). Un PR nunca publica ni despliega.

### Flujo de Git

- Cada cambio se hace en una rama `feature/*`, `fix/*`, `ci/*` o `docs/*` creada desde `dev`, y entra por un pull request con la CI en verde.
- `dev` integra el trabajo en curso.
- En cada entrega, `dev` se mergea a `main` por pull request y se crea el tag `ctv-version-N` con su release.

---

## ☁️ Despliegue en la nube (Azure)

La app corre en una VM de Azure de 2 vCPU, serie B, que permanece **apagada** la mayor parte del tiempo y solo se prende para las demos. La serie B rinde mientras le quedan créditos de CPU; un stress largo los gasta y después la VM se siente lenta aunque el código no haya cambiado. La VM consume las imágenes ya construidas en GHCR (no compila nada).

El overlay `docker-compose.prod.yml` le pone techo de memoria a cada contenedor y deja el heap de las dos JVM en 256 MB, para que Elasticsearch y Kibana no empujen al sistema a usar swap. Elasticsearch, Kibana y Logstash además tienen un tope de CPU para que la app siga respondiendo mientras indexan. Zipkin recibe una de cada veinte trazas. Prometheus, Grafana y la pantalla de operación se actualizan cada 5 segundos, también en la VM. El scrape no puede ser más corto: el timeout tiene que entrar en ese intervalo y la serie B se queda sin créditos de CPU si Prometheus pide las métricas todo el tiempo. Para no llenar ese intervalo, Prometheus se queda solo con las series de los tableros y el backend no recalcula un percentil extra en cada request.

**Prender y desplegar:**
Actions → `Deploy Demo (Azure VM)` → Run workflow → elegir la rama → `action: deploy`

El workflow valida el secreto `JWT_SECRET` (job `preflight`) y, en paralelo, repite los controles (backend, flights-service, frontend, aceptación, e2e y validación de workflows); si todo pasa, publica las imágenes con el SHA del commit y prende la VM. En la VM sincroniza `docker-compose.yml`, `docker-compose.prod.yml` y `monitoring/` desde el commit desplegado (con permisos de lectura para todos, así Grafana puede leer el provisioning), actualiza `JWT_SECRET` y `CORS_ALLOWED_ORIGIN_PATTERNS=http://<VM_HOST>:8090` en `/opt/ctv/.env` (sin tocar el resto de las líneas), levanta esa versión con el perfil `monitoring` y verifica la salud del backend. El frontend de la VM (`:8090`) llama a la API (`:8080`) desde otro origen, así que el health check también hace un preflight CORS con ese `Origin` y falla el deploy si el backend no lo acepta. Si cambió `monitoring/`, reinicia Prometheus, Grafana y Logstash.

**Apagar al terminar la demo:**
Actions → `Deploy Demo (Azure VM)` → Run workflow → `action: stop`

Apagar la VM no depende de que pasen los tests. El workflow necesita estos secretos del repositorio en GitHub: `AZURE_CREDENTIALS`, `VM_HOST`, `VM_USER`, `VM_SSH_KEY`, `SONAR_TOKEN` y `JWT_SECRET`.

VM: `northcentralus`, IP pública `64.236.199.81` (fija mientras no se borre el recurso — no cambia entre `stop`/`deploy`).

| Servicio | URL (con la VM prendida) |
|----------|-----|
| Frontend | http://64.236.199.81:8090 |
| Backend | http://64.236.199.81:8080 |
| Swagger (Backend) | http://64.236.199.81:8080/swagger-ui/index.html |
| Grafana | http://64.236.199.81:3001 |
| Zipkin | http://64.236.199.81:9411 |
| Kibana | http://64.236.199.81:5601 |

La VM usa el perfil `prod` de Spring. El esquema lo administra Flyway y los datos se conservan entre deploys; el overlay de la VM habilita la seed de ejemplo, que se carga una sola vez. Para una instalación sin datos de ejemplo, usar `SEED_ENABLED=false`.

En la VM solo se publican 8080, 8090, 3001, 5601 y 9411; las bases (5433, 5434), Elasticsearch (9200), Logstash (5000) y Prometheus (9090) no son accesibles desde afuera.

---

## 📚 Documentación

- [MER](docs/mer.puml) ([imagen](docs/mer.png))
- [Flujo de la aplicación](docs/flujo-de-la-aplicacion.md)
- [Guion de demo](docs/demo.md)
- [Funciones del administrador](docs/admin.md)
- [Seed y ciclo de vida de los datos](docs/seed-y-ciclo-de-vida.md)
- [Tests de carga](docs/load-tests.md)
- [Kubernetes](docs/kubernetes.md)
- [Migración de agencias](docs/agency-migration.md)
- READMEs de [frontend](frontend/README.md), [flights-service](flights-service/README.md) y [tests de aceptación](acceptance-tests/README.md)
