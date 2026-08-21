# unq-pdes-2026c2-ctv

Aplicación web para gestión y compra de paquetes turísticos.

Proyecto de la materia Prácticas de Desarrollo de Software – UNQ, 2do semestre 2026.

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
├── monitoring/        # Configuración de Prometheus
└── docker-compose.yml
```

---

## 🛠️ Tech Stack

**Backend / Flights Service:** Spring Boot 4.1.0 + Kotlin 2.3.21 + Java 25 (Gradle 9.5.1)  
**Frontend:** React 19 + TypeScript 6 + Vite 8  
**BD:** PostgreSQL 16 (2 instancias)  
**Testing:** JUnit 5 (H2 en memoria) + JaCoCo (cobertura) · Vitest (frontend)  
**API Docs:** Swagger/OpenAPI (springdoc) — solo backend  
**Observabilidad:** Spring Boot Actuator + Prometheus  
**CI/CD:** GitHub Actions  
**Containerización:** Docker + Docker Compose

---

## 🚀 Cómo Correr

Levantar toda la app (2 bases de datos, backend, flights-service, frontend, Prometheus y Zipkin) con un solo comando:

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
| Zipkin | http://localhost:9411 |

---

## 🧪 Testing

```bash
./gradlew test              # tests
./gradlew jacocoTestReport  # reporte de cobertura (build/reports/jacoco/test/html)
```
