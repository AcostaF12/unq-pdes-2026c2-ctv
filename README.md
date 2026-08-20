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

**Backend:** Spring Boot 3.3.5 + Kotlin 2.0.10 + Java 25  
**BD:** PostgreSQL (2 instancias)  
**Testing:** JUnit 5 + Mockito + AssertJ + JaCoCo  
**API Docs:** Swagger/OpenAPI  
**CI/CD:** GitHub Actions  
**Containerización:** Docker + Docker Compose

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
