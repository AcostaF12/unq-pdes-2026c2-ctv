# Compra Tu Viaje

Aplicación web para buscar y comprar paquetes turísticos. Un paquete junta un hotel, un vuelo de ida y uno de vuelta, y lo publica una agencia.

Hay tres roles. El comprador busca, guarda favoritos, compra y reseña. La agencia arma sus paquetes y mira sus ventas. El administrador carga hoteles, vuelos, agencias, usuarios y paquetes de cualquier agencia.

Los vuelos no viven en la base de la aplicación. Los administra `flights-service`, un servicio aparte. Al comprar, el backend reserva el asiento de ida y el de vuelta; si el segundo falla, cancela el primero.

Proyecto de Prácticas de Desarrollo de Software, UNQ, segundo semestre de 2026.

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

## Índice

- [Cómo levantarlo](#cómo-levantarlo)
- [Cómo está armado](#cómo-está-armado)
- [Qué hay en esta entrega](#qué-hay-en-esta-entrega)
- [Calidad, imágenes y deploy](#calidad-imágenes-y-deploy)
- [Dónde seguir leyendo](#dónde-seguir-leyendo)

La lectura larga del proyecto (arquitectura, compra, infraestructura, pruebas y decisiones) está en [docs/Entrega-2.html](docs/Entrega-2.html). Este archivo es la portada: alcanza para clonar, levantar y saber qué mirar.

## Cómo levantarlo

Hace falta Docker. La primera vez tarda varios minutos: compila los dos servicios Kotlin y baja las imágenes de Elasticsearch, Logstash y Kibana.

```bash
docker compose up --build
```

Cuando el backend queda `healthy`, el frontend responde en http://localhost:8090.

```bash
docker compose ps            # estado y health de cada servicio
docker compose logs -f backend
docker compose down          # apaga y conserva los volúmenes
docker compose down -v       # apaga y borra los datos
```

### Desarrollo con recarga

Solo las bases en Docker. El resto corre en la máquina, con los puertos del profile `dev` (`5433` para el backend, `5434` para vuelos). Cada comando va en su terminal.

```bash
docker compose up -d backend-postgres flights-postgres

cd flights-service && ./gradlew bootRun   # :8081
cd backend && ./gradlew bootRun           # :8080
cd frontend && npm ci && npm run dev      # :5173
```

Sin Zipkin ni Logstash, los servicios Kotlin avisan que no pueden conectar. No afecta el funcionamiento. `TRACING_SAMPLING_PROBABILITY=0` silencia los avisos de Zipkin.

Si `5433` o `5434` están ocupados, el compose acepta `BACKEND_DB_PORT` y `FLIGHTS_DB_PORT`. En ese caso el servicio se levanta con `DB_PORT` apuntando al puerto elegido.

Flyway administra el esquema. En `dev`, el backend borra y vuelve a cargar los datos de ejemplo en cada arranque. En `prod` no borra: solo agrega lo que falte y valida el esquema (`ddl-auto=validate`).

### Servicios

| Servicio | URL |
|----------|-----|
| Frontend (Docker) | http://localhost:8090 |
| Frontend (`npm run dev`) | http://localhost:5173 |
| Backend | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (`admin` / `admin`) |
| Zipkin | http://localhost:9411 |
| Kibana | http://localhost:5601 |

`flights-service` escucha en el puerto `8081` dentro de la red de Compose y cuando se corre con `bootRun`. El compose principal no lo publica al host: solo lo usa el backend.

La primera vez que se abre Kibana no hay vista de datos. En Stack Management, Data Views, crear una con patrón `ctv-logs-*` y campo de tiempo `@timestamp`.

### Usuarios de prueba

Los carga `DataBootstrap` en `dev` y en `prod`. La contraseña de cada administrador es igual al nombre de usuario.

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Comprador | `buyer`, `buyer2` | `buyer123` |
| Agencia (Despegar) | `agency` | `agency123` |
| Admin | `facosta`, `vferreyra`, `adisanto` | igual al usuario |

El registro público siempre crea un comprador. Agencias y administradores salen del seed.

## Cómo está armado

```
├── backend/            # API principal. Kotlin, Spring Boot. Puerto 8080
├── flights-service/    # Vuelos, con su propia base. Puerto 8081
├── frontend/           # React, TypeScript y Vite. En Docker, nginx en el 8090
├── acceptance-tests/   # Escenarios Cucumber contra las APIs reales
├── monitoring/         # Prometheus, Grafana y Logstash
└── docker-compose.yml
```

El navegador baja la SPA y habla con el backend por REST, con un JWT. El backend tiene su PostgreSQL 16 y es el único que llama a `flights-service`, que tiene otra base. No hay clave foránea entre las dos: el paquete guarda el id del vuelo.

Las llamadas a vuelos pasan por Resilience4j: hasta 3 reintentos y circuit breaker. El timeout de 3 segundos está en la config y todavía no está anotado en el cliente. El estado del circuito está en http://localhost:8080/actuator/health.

El detalle de las capas, el modelo y la secuencia de compra está en [docs/Entrega-2.html](docs/Entrega-2.html). El diagrama de entidades está en [docs/mer.puml](docs/mer.puml).

**Backend y vuelos:** Spring Boot 4.1.0, Kotlin 2.3.21, Java 25, Gradle 9.5.1.  
**Frontend:** React 19, TypeScript 6, Vite 8.  
**Contrato de la API:** Swagger, solo en el backend.

## Qué hay en esta entrega

La entrega 1 dejó la base: autenticación, hoteles, ciudades, el servicio de vuelos y el primer despliegue.

En la entrega 2 el producto se puede usar de punta a punta y se puede mostrar cómo se construye.

Un comprador filtra el catálogo (destino, precio, orden y paginación resueltos en el backend), abre el detalle, marca favoritos y compra. El historial guarda una copia de lo que pagó: si después cambia el precio o el nombre del paquete, la compra no se mueve. Solo puede reseñar un paquete que compró, y una sola vez.

Una agencia publica sus paquetes eligiendo los vuelos de una lista, no escribiendo ids, y consulta sus ventas. Un administrador, además de los hoteles, carga vuelos, arma paquetes para cualquier agencia y administra usuarios, agencias y el historial de ventas. Quien no tiene el rol ve "Sin permiso" en la interfaz; la API responde 403.

La compra cruza dos sistemas. Si no hay lugar en el vuelo de vuelta, el backend cancela la reserva de ida y no deja una compra a medias. Si `flights-service` no responde, el circuit breaker corta rápido y el usuario recibe 503.

Cada pull request corre lint, tests y build. Backend y vuelos pasan por el Quality Gate de Sonar. Hay escenarios Cucumber contra las APIs y recorridos Cypress sobre un Docker aparte. En `main`, después de esos controles, se publican las imágenes. El deploy a la VM de Azure es manual y solo corre si los mismos controles pasan.

El recorrido para mostrarlo está en [docs/demo.md](docs/demo.md). Lo que puede hacer un administrador, en [docs/admin.md](docs/admin.md).

## Calidad, imágenes y deploy

Desde la carpeta de cada módulo:

```bash
# backend o flights-service
./gradlew test
./gradlew jacocoTestReport   # build/reports/jacoco/test/html

# frontend
npm run lint
npm test
npm run build                # incluye el chequeo de tipos
```

Cucumber y Cypress tienen su propio entorno para no pisar los datos de desarrollo. Los comandos están en [acceptance-tests/README.md](acceptance-tests/README.md) y en [frontend/README.md](frontend/README.md#pruebas-e2e).

En cada push a `main`, si pasan los controles del módulo (incluido el Quality Gate de Sonar en backend y vuelos), el CI publica tres imágenes con las etiquetas `latest` y el SHA del commit: en GitHub Container Registry (`ghcr.io/acostaf12/ctv-*`) y en Docker Hub.

| Imagen | Contenido |
|--------|-----------|
| `<usuario>/ctv-backend` | API principal |
| `<usuario>/ctv-flights-service` | servicio de vuelos |
| `<usuario>/ctv-frontend` | frontend con nginx. La URL de la API queda fija en el build (`http://localhost:8080` en la imagen de Hub) |

Para levantar el stack desde Docker Hub, sin compilar:

```bash
docker compose -f docker-compose.hub.yml up -d --wait
```

`DOCKERHUB_NAMESPACE` (por defecto `acostaf12`) y `CTV_IMAGE_TAG` (por defecto `latest`; también sirve un SHA) son opcionales. Publicar exige los secrets `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN`. Si faltan, el job de `main` falla con un mensaje explícito. Los pull requests no publican.

La VM de Azure queda apagada salvo en las demos. Las imágenes las toma de GHCR: en la VM no se compila.

- Prender y desplegar `main`: Actions, `Deploy Demo (Azure VM)`, Run workflow, `action: deploy`.
- Apagar al terminar: el mismo workflow, `action: stop`.

Región `northcentralus`. IP pública `64.236.199.81` mientras el recurso exista. Con la VM prendida: frontend en el puerto `8090`, backend en `8080`, Grafana en `3001`, Zipkin en `9411`, Kibana en `5601`.

## Dónde seguir leyendo

| Documento | Para qué |
|-----------|----------|
| [docs/Entrega-2.html](docs/Entrega-2.html) | Lectura del proyecto: qué hicimos, arquitectura, compra, CI, pruebas y límites |
| [docs/demo.md](docs/demo.md) | Guion corto para recorrer la aplicación |
| [docs/admin.md](docs/admin.md) | Pantallas y permisos del administrador |
| [docs/entrega-2-verificacion.md](docs/entrega-2-verificacion.md) | Matriz de roles, pruebas de integración y evidencias de la rúbrica |
| [docs/mer.puml](docs/mer.puml) | Modelo de datos de las dos bases |
| [frontend/README.md](frontend/README.md) | Scripts, variables y Cypress |
| [acceptance-tests/README.md](acceptance-tests/README.md) | Cómo correr Cucumber |
| Swagger | http://localhost:8080/swagger-ui/index.html con el backend levantado |

[docs/frontend/diseno-fase-1.md](docs/frontend/diseno-fase-1.md) y [docs/frontend/backlog.md](docs/frontend/backlog.md) describen la primera etapa del frontend, cuando el home era el catálogo de hoteles. No reflejan la aplicación actual.
