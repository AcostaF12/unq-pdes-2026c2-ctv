# Compra tu Viaje

Aplicación web para buscar y comprar paquetes turísticos. Los compradores consultan viajes y su historial de compras, las agencias publican paquetes y los administradores gestionan los datos de la aplicación.

Trabajo práctico de Prácticas de Desarrollo de Software, Universidad Nacional de Quilmes, segundo cuatrimestre de 2026.

## Levantar y apagar la aplicación

Necesitás Docker Desktop instalado y abierto. Desde la raíz del repositorio:

```bash
docker compose up --build -d --wait
```

La primera ejecución tarda porque descarga imágenes y compila los servicios. Cuando termine de arrancar, abrí [Compra tu Viaje](http://localhost:8090).

El entorno incluye el frontend, el backend, el servicio de vuelos, dos bases PostgreSQL y las herramientas de monitoreo.

Para ver el estado y seguir los logs del backend:

```bash
docker compose ps
docker compose logs -f backend
```

Para apagarlo:

```bash
docker compose down
```

Este comando conserva los volúmenes de Docker. Sin embargo, el backend reinicia sus datos de ejemplo al arrancar con el perfil `dev`. No uses este entorno para guardar datos que quieras conservar.

### Usar las imágenes de Docker Hub

Para arrancar sin compilar, una vez publicadas las imágenes:

```bash
docker compose -f docker-compose.hub.yml up -d --wait
```

Se usan `acostaf12/ctv-backend`, `acostaf12/ctv-flights-service` y `acostaf12/ctv-frontend`. Podés cambiar el namespace con `DOCKERHUB_NAMESPACE` y la etiqueta con `CTV_IMAGE_TAG`, que por defecto es `latest`. Los puertos y usuarios son los mismos.

### Desarrollo con hot reload

Levantá las bases en Docker y ejecutá cada módulo en una terminal distinta. Necesitás Java 25 y Node.js 24:

```bash
docker compose up -d backend-postgres flights-postgres
```

```bash
cd flights-service && ./gradlew bootRun
```

```bash
cd backend && ./gradlew bootRun
```

```bash
cd frontend && npm ci && npm run dev
```

El frontend de desarrollo abre en http://localhost:5173. Las bases usan los puertos `5433` y `5434`. Si están ocupados, el compose acepta `BACKEND_DB_PORT` y `FLIGHTS_DB_PORT`; ejecutá el servicio correspondiente con `DB_PORT` apuntando al puerto elegido.

Sin el monitoreo levantado pueden aparecer avisos de conexión a Logstash y Zipkin. `TRACING_SAMPLING_PROBABILITY=0` desactiva el envío de trazas.

## Direcciones y usuarios de prueba

| Servicio | Dirección local |
| --- | --- |
| Aplicación | http://localhost:8090 |
| API del backend | http://localhost:8080 |
| Swagger del backend | http://localhost:8080/swagger-ui/index.html |
| Prometheus, métricas | http://localhost:9090 |
| Grafana, paneles de métricas | http://localhost:3001 |
| Zipkin, trazas de solicitudes | http://localhost:9411 |
| Kibana, consulta de logs | http://localhost:5601 |

La aplicación carga estas cuentas de ejemplo:

| Rol | Usuario | Contraseña |
| --- | --- | --- |
| Comprador | `buyer` | `buyer123` |
| Comprador | `buyer2` | `buyer123` |
| Agencia | `agency` | `agency123` |
| Administrador | `vferreyra`, `facosta`, `adisanto` | igual al usuario |

Para entrar a Grafana, usá `admin` como usuario y contraseña. Estas credenciales son para pruebas, no para producción.

Para presentar la aplicación: [guion de demo](docs/demo.md) y [funciones del administrador](docs/admin.md).

## Ejecutar los tests

### Backend y servicio de vuelos

Necesitás Java 25. Desde `backend/` o `flights-service/`, según el módulo que quieras probar:

```bash
./gradlew test
./gradlew jacocoTestReport
```

El reporte de cobertura queda en `build/reports/jacoco/test/html/index.html`, dentro de cada módulo. Para compilar y ejecutar también la revisión de estilo con ktlint:

```bash
./gradlew build
```

### Frontend

Con Node.js 24 instalado, desde `frontend/`:

```bash
npm ci
npm test
npm run lint
npm run build
```

### Aceptación e interfaz

Cucumber prueba las reglas de negocio a través de las APIs. Cypress prueba recorridos desde el navegador. Ambos usan servicios reales en entornos Docker separados del entorno de desarrollo.

- [Cómo ejecutar Cucumber](acceptance-tests/README.md).
- [Cómo levantar el entorno e2e y ejecutar Cypress](frontend/README.md#pruebas-e2e).

## Estructura y tecnologías

| Carpeta | Contenido |
| --- | --- |
| `backend/` | API de Compra tu Viaje, con Kotlin y Spring Boot. |
| `flights-service/` | API interna de vuelos, con Kotlin y Spring Boot. |
| `frontend/` | Interfaz con React, TypeScript y Vite. |
| `acceptance-tests/` | Escenarios de aceptación con Cucumber. |
| `frontend/cypress/` | Tests de interfaz con Cypress. |
| `monitoring/` | Configuración de métricas, paneles y logs. |
| `.github/workflows/` | Controles de CI y deploy manual. |

Los servicios Kotlin usan Java 25 y Gradle. Cada servicio tiene su propia base PostgreSQL 16. Los tests usan JUnit y JaCoCo en Kotlin, y Vitest en el frontend. Swagger documenta la API del backend.

## CI y deploy de la demo

GitHub Actions ejecuta los controles de los módulos, Cucumber y Cypress. Sonar analiza la calidad del backend y del servicio de vuelos. Los controles de cada módulo tienen filtros por carpeta, por lo que no todos se ejecutan en todos los PR.

Consultá las ejecuciones en [GitHub Actions](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions) y los análisis en Sonar:

- [Backend](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-backend).
- [Servicio de vuelos](https://sonarcloud.io/summary/new_code?id=acostaf12_unq-pdes-2026c2-ctv-flights-service).

En cada push a `main`, después de los controles de cada módulo, sus imágenes se publican en Docker Hub y GitHub Container Registry con `latest` y el SHA del commit. Docker Hub requiere los secretos `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN`; la variable opcional `DOCKERHUB_NAMESPACE` cambia el namespace. Si faltan las credenciales, la publicación falla.

Abrir un PR no despliega la aplicación. El deploy de la demo se inicia manualmente desde GitHub Actions:

1. Abrí `Deploy Demo (Azure VM)` y seleccioná `Run workflow`.
2. Elegí la rama que querés desplegar y la acción `deploy`.
3. El workflow ejecuta los controles. Si pasan, publica las imágenes del commit en GitHub Container Registry, prende la VM y levanta esa versión.
4. Al terminar la demo, ejecutá el mismo workflow con la acción `stop` para apagar la VM.

Apagar la VM no depende de que pasen los tests. Estas acciones requieren los secretos de Azure, SSH y Sonar configurados en GitHub.

La VM de demo está en `northcentralus`. Cuando está encendida, estas son sus direcciones:

| Servicio | Dirección de la demo |
| --- | --- |
| Aplicación | http://64.236.199.81:8090 |
| API del backend | http://64.236.199.81:8080 |
| Swagger | http://64.236.199.81:8080/swagger-ui/index.html |
| Grafana | http://64.236.199.81:3001 |
| Zipkin | http://64.236.199.81:9411 |
| Kibana | http://64.236.199.81:5601 |

La demo usa el perfil `prod`. Ambos servicios validan el esquema administrado por Flyway y conservan los datos al reiniciar. El overlay de demo habilita la seed versionada; para una instalación sin datos de ejemplo usar `SEED_ENABLED=false`.

## Notas de las entregas

[Release notes de la Entrega 2](docs/release-notes-entrega-2.md).

La seed completa, sus credenciales, la persistencia y el archivo de paquetes se describen en [seed y ciclo de vida](docs/seed-y-ciclo-de-vida.md). La integración administrativa se documenta en [revisión de la PR 19](docs/pr-19-integracion.md).
