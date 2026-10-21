# Guion de demo

Recorrido de unos 8 minutos. Cada paso indica qué mostrar y qué decir.

## Antes de empezar

```bash
docker compose up --build -d --wait
```

Con todo `healthy`, abrir estas pestañas:

| Pestaña | URL |
|---------|-----|
| Aplicación | http://localhost:8090 |
| Swagger | http://localhost:8080/swagger-ui/index.html |
| Grafana | http://localhost:3001 (`admin` / `admin`) |
| Zipkin | http://localhost:9411 |
| Kibana | http://localhost:5601 |

Para volver a los datos iniciales entre ensayos: `docker compose down -v` y volver a levantar.

## 1. Comprador (`buyer` / `buyer123`)

1. **Login.** Ingresar con `buyer`. La navegación solo muestra lo que su rol puede usar.
2. **Paquetes** (`/trips`). Filtrar por destino, rango de precio y ordenar por precio. El filtrado, la paginación y el orden se resuelven en el backend.
3. **Detalle** de un paquete: carrusel de fotos, vuelo de ida y de vuelta, puntaje promedio y reseñas.
4. **Favoritos.** Marcar un paquete y verlo en `/favorites`.
5. **Compra.** Comprar un paquete que no diga "Ya comprado". El backend reserva los asientos de ida y de vuelta en `flights-service`, un servicio aparte con su propia base. Si falla la segunda reserva, cancela la primera.
6. **Mis compras** (`/purchases`). La compra aparece con el precio abonado. El historial guarda una copia de lo que se pagó, así que no cambia si el paquete se edita después.

## 2. Agencia (`agency` / `agency123`)

1. Ingresar como `agency` (Despegar).
2. **Mis paquetes** (`/agency`): crear y eliminar paquetes de la agencia.
3. **Ventas** (`/agency/sales`): compras realizadas sobre sus paquetes.
4. Mostrar que un comprador que entra a `/agency` ve "Sin permiso".

## 3. Administrador (`vferreyra` / `vferreyra`)

1. **Hoteles** (`/hotels`): alta, edición y baja desde la interfaz. Esas rutas solo existen para el rol administrador.
2. Más funciones de administración (paquetes de cualquier agencia y carga de vuelos): ver [Sección Administración](admin.md).

## 4. Lo que hay detrás

- **Swagger.** Contrato completo de la API.
- **Resiliencia.** El backend protege las llamadas a `flights-service` con Resilience4j (reintentos, timeout y circuit breaker). El estado del breaker figura en `http://localhost:8080/actuator/health`.
- **Grafana.** Dashboard *CTV - Overview*: servicios activos, uptime, memoria de la JVM y requests por segundo.
- **Zipkin.** Buscar trazas del servicio `backend` y abrir una.
- **Kibana.** La primera vez no hay vista de datos: *Stack Management → Data Views → Create*, nombre y patrón `ctv-logs-*`, campo de tiempo `@timestamp`. Luego, en *Discover*, filtrar por `service`.

## 5. Calidad

- **Tests.** 277 de backend y 97 de frontend, más pruebas end-to-end con Cypress.
- **CI.** GitHub Actions corre lint, tests y build en cada módulo, y Sonar con Quality Gate. Los badges están en el [README](../README.md).
- **Ejecutar las pruebas.** Ver la sección *Testing* del README.
