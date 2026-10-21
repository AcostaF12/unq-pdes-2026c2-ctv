# unq-pdes-2026c2-ctv

Aplicación web para gestión y compra de paquetes turísticos.

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

---

## 📝 Release Notes - Entrega 1

En progreso...

## 📝 Release Notes - Entrega 2

Resumen de lo que contiene el repositorio en esta entrega.

**Aplicación**
- **Backend** (Spring Boot + Kotlin): registro, login con token y perfil de usuario; hoteles (alta, edición y baja solo para administradores) y ciudades; paquetes turísticos con búsqueda avanzada (filtros combinados, paginación y orden resueltos en el backend); favoritos; compras con historial que conserva una copia inmutable de lo pagado; reseñas (solo con compra previa y una por comprador); ventas por agencia. Contrato documentado con Swagger/OpenAPI.
- **flights-service** (Spring Boot + Kotlin): servicio interno de vuelos con su propia base de datos. Al comprar, el backend reserva los asientos de ida y de vuelta y, si falla la segunda reserva, cancela la primera. Las llamadas están protegidas con Resilience4j (reintentos, timeout y circuit breaker).
- **Frontend** (React + TypeScript + Vite): login y registro, catálogo con filtros y detalle de paquete, favoritos, compras, panel de agencia (paquetes y ventas), gestión de hoteles para administradores y perfil. Cada rol ve solo la navegación que puede usar.
- Datos de ejemplo y usuarios de prueba cargados por `DataBootstrap`; el schema lo administra Flyway.

**Calidad**
- Tests unitarios y de integración (JUnit 5 + JaCoCo en los servicios Kotlin, Vitest en el frontend) y ktlint en ambos servicios Kotlin.
- Análisis de Sonar con Quality Gate bloqueante en backend y flights-service.
- 11 escenarios de aceptación con Cucumber contra las APIs reales y pruebas end-to-end con Cypress sobre un entorno Docker aislado.
- `actionlint` y `scripts/check-ci.rb` validan los workflows y las reglas del CI.

**CI/CD y operación**
- Un workflow de GitHub Actions por módulo (lint, tests y build en cada PR) más el de aceptación/e2e y el de validación de workflows.
- En cada push a `main`, y solo después de que pasan los controles, se publican las imágenes de `backend`, `flights-service` y `frontend` en GitHub Container Registry y en Docker Hub (ver [Imágenes en Docker Hub](#-imágenes-en-docker-hub)).
- Deploy manual a una VM de Azure para las demos (ver [Deploy Cloud](#️-deploy-cloud-azure)).
- Docker Compose para levantar todo el stack, con bases, Prometheus, Grafana, Zipkin y ELK (Elasticsearch, Logstash y Kibana).

Más detalle: [guion de demo](docs/demo.md) y [sección de administración](docs/admin.md).

## 📝 Release Notes - Entrega 3

En progreso...

### 🚀 Estructura
```
├── backend/           # Spring Boot - CTV (Kotlin)
├── flights-service/   # Spring Boot - API Vuelos (Kotlin) — interno
├── frontend/          # React + TypeScript + Vite
├── monitoring/        # Configuración de Prometheus, Grafana y Logstash
└── docker-compose.yml
```

---

## 🛠️ Tech Stack

**Backend / Flights Service:** Spring Boot 4.1.0 + Kotlin 2.3.21 + Java 25 (Gradle 9.5.1)  
**Frontend:** React 19 + TypeScript 6 + Vite 8  
**BD:** PostgreSQL 16 (2 instancias)  
**Testing:** JUnit 5 (H2 en memoria) + JaCoCo (cobertura) · Vitest (frontend)  
**API Docs:** Swagger/OpenAPI (springdoc) — solo backend  
**Observabilidad:** Spring Boot Actuator + Prometheus (métricas) + Grafana (dashboards) + Zipkin (trazas) + ELK / Elasticsearch + Logstash + Kibana (logs)  
**CI/CD:** GitHub Actions  
**Containerización:** Docker + Docker Compose

---

## 🚀 Cómo Correr

### Todo con Docker

Levanta las 2 bases de datos, backend, flights-service, frontend, Prometheus, Grafana, Zipkin y ELK con un solo comando:

```bash
docker compose up --build
```

La primera vez tarda varios minutos (compila los dos servicios Kotlin y baja las imágenes de ELK). Cuando el backend queda `healthy` ya se puede entrar al frontend.

```bash
docker compose ps            # estado y health de cada servicio
docker compose logs -f backend
docker compose down          # apaga y conserva los volúmenes
docker compose down -v       # apaga y borra los datos (arranque limpio)
```

### Desarrollo con hot reload

Solo las bases en Docker y el resto en la máquina. Los puertos por defecto del profile `dev` ya apuntan a ellas (`5433` backend, `5434` flights).

```bash
docker compose up -d backend-postgres flights-postgres

cd flights-service && ./gradlew bootRun   # :8081
cd backend && ./gradlew bootRun           # :8080
cd frontend && npm ci && npm run dev      # :5173
```

Cada comando va en su propia terminal. Sin Zipkin ni Logstash levantados, los servicios Kotlin loguean avisos de reconexión a Logstash y de spans descartados que no afectan el funcionamiento. `TRACING_SAMPLING_PROBABILITY=0` silencia los de Zipkin.

Si un puerto del host está ocupado (por ejemplo `5434`), el compose acepta `BACKEND_DB_PORT` y `FLIGHTS_DB_PORT`; en ese caso levantar el servicio con `DB_PORT` apuntando al puerto elegido.

El schema lo administra Flyway. En `dev`, el backend borra y vuelve a cargar los datos de ejemplo en cada arranque.

### Servicios expuestos

| Servicio | URL |
|----------|-----|
| Frontend (Docker) | http://localhost:8090 |
| Frontend (`npm run dev`) | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Swagger (Backend) | http://localhost:8080/swagger-ui/index.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (usuario `admin` / pass `admin`) |
| Zipkin | http://localhost:9411 |
| Kibana (logs) | http://localhost:5601 |

Para presentar la aplicación, ver el [guion de demo](docs/demo.md).

La sección de administración (login `vferreyra` / `vferreyra`) está descripta en [docs/admin.md](docs/admin.md).

### Usuarios de prueba

Los carga `DataBootstrap` en `dev` y `prod`.

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Comprador | `buyer` | `buyer123` |
| Comprador | `buyer2` | `buyer123` |
| Agencia (Despegar) | `agency` | `agency123` |
| Admin | `facosta`, `vferreyra`, `adisanto` | igual al usuario |

---

## 🐳 Imágenes en Docker Hub

En cada push a `main` (una vez que pasan los controles de cada módulo, incluido el Quality Gate de Sonar) el CI publica tres imágenes, con las etiquetas `latest` y el SHA del commit:

| Imagen | Contenido |
|--------|-----------|
| `<usuario>/ctv-backend` | API principal |
| `<usuario>/ctv-flights-service` | servicio de vuelos |
| `<usuario>/ctv-frontend` | frontend servido con nginx (apunta a `http://localhost:8080`) |

Para levantar todo el stack desde esas imágenes, sin compilar nada:

```bash
docker compose -f docker-compose.hub.yml up -d --wait
```

Los puertos y usuarios son los mismos que en la sección anterior. Variables opcionales: `DOCKERHUB_NAMESPACE` (por defecto `acostaf12`; cambiarlo si las imágenes viven en otro usuario u organización) y `CTV_IMAGE_TAG` (por defecto `latest`; también se puede usar un SHA).

Para publicar hay que cargar en el repositorio los secrets `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN` (un access token de Docker Hub con permiso de escritura). Opcionalmente, la variable `DOCKERHUB_NAMESPACE` reemplaza al usuario como namespace. Si faltan, el job de publicación en `main` falla con un mensaje explícito; los PR no publican nada.

Las imágenes de GitHub Container Registry (`ghcr.io/acostaf12/ctv-*`) siguen publicándose porque las usa el deploy de Azure.

---

## ☁️ Deploy Cloud (Azure)

La app corre en una VM de Azure que permanece **apagada** la mayor parte del tiempo y solo se prende para las demos. Las imágenes de `backend`, `flights-service` y `frontend` se publican automáticamente a GitHub Container Registry en cada push a `main` (ver workflows en `.github/workflows/`), y la VM las consume ya construidas (no compila nada).

**Prender + deployar la última versión de `main`:**
Actions → `Deploy Demo (Azure VM)` → Run workflow → `action: deploy`

**Apagar al terminar la demo:**
Actions → `Deploy Demo (Azure VM)` → Run workflow → `action: stop`

VM: `northcentralus`, IP pública `64.236.199.81` (fija mientras no se borre el recurso — no cambia entre `stop`/`deploy`).

| Servicio | URL (con la VM prendida) |
|----------|-----|
| Frontend | http://64.236.199.81:8090 |
| Backend | http://64.236.199.81:8080 |
| Swagger (Backend) | http://64.236.199.81:8080/swagger-ui/index.html |
| Grafana | http://64.236.199.81:3001 |
| Zipkin | http://64.236.199.81:9411 |
| Kibana | http://64.236.199.81:5601 |

La VM usa el profile `prod` de Spring. El schema lo administra Flyway (`ddl-auto=validate`) y `DataBootstrap` carga los datos de ejemplo que falten.

## 🧪 Testing

Backend y flights-service (desde su carpeta):

```bash
./gradlew test              # tests
./gradlew jacocoTestReport  # reporte de cobertura (build/reports/jacoco/test/html)
```

Frontend (desde `frontend/`):

```bash
npm run lint
npm test
npm run build               # incluye el chequeo de tipos
```

Estos mismos pasos son los que corre el CI de cada módulo.

Pruebas end-to-end con Cypress contra un entorno Docker aparte (puertos `18080` y `18090`): ver [`frontend/README.md`](frontend/README.md#pruebas-e2e).
