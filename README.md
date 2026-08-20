# unq-pdes-2026c2-ctv

Aplicación web para gestión y compra de paquetes turísticos.

Proyecto de la materia Prácticas de Desarrollo de Software – UNQ, 2do semestre 2026.

---

## 📝 Release Notes - Entrega 1

### ✅ Core

En progreso...

## 📝 Release Notes - Entrega 2

En progreso...

## 📝 Release Notes - Entrega 3

En progreso...

### 🚀 Estructura
```
├── backend/           # Spring Boot - CTV (Kotlin)
├── flights-service/   # Spring Boot - API Vuelos (Kotlin)
├── frontend/          # React + TypeScript + Vite
└── docker-compose.yml
```

---

## 🛠️ Tech Stack

**Backend / Flights Service:** Spring Boot 4.1.0 + Kotlin 2.3.21 + Java 25 (Gradle 9.5.1)  
**Frontend:** React 19 + TypeScript 6 + Vite 8  
**BD:** PostgreSQL 16 (2 instancias)  
**Testing:** JUnit 5 (H2 en memoria)  
**CI/CD:** GitHub Actions  
**Containerización:** Docker + Docker Compose

_Pendiente:_ JaCoCo (cobertura) · Swagger/OpenAPI (API docs)

---

## 🚀 Cómo Correr

```bash
# Todo
docker-compose up

# Backend: http://localhost:8080
# Flights: http://localhost:8081
# Swagger: http://localhost:8080/swagger-ui.html
```

---

## 🧪 Testing

```bash
./gradlew test
./gradlew jacocoTestReport
```
