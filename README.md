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

En progreso...

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

Levantar toda la app (2 bases de datos, backend, flights-service, frontend, Prometheus, Grafana, Zipkin y ELK) con un solo comando:

```bash
docker compose up --build
```

### Servicios expuestos

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:8090 |
| Backend | http://localhost:8080 |
| Swagger (Backend) | http://localhost:8080/swagger-ui/index.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 (usuario `admin` / pass `admin`) |
| Zipkin | http://localhost:9411 |
| Kibana (logs) | http://localhost:5601 |

---

## 🧪 Testing

```bash
./gradlew test              # tests
./gradlew jacocoTestReport  # reporte de cobertura (build/reports/jacoco/test/html)
```
