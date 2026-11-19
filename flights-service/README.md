# API de vuelos

El servicio expone las rutas del enunciado y conserva las rutas REST existentes.

## Buscar vuelos disponibles

`GET /vuelos` acepta los filtros opcionales `origin`, `destination` y `date`, con fecha `YYYY-MM-DD`. Sólo devuelve vuelos con disponibilidad mayor a cero.

Ejemplo: `/vuelos?origin=BUE&destination=PAR&date=2026-12-01`.

`GET /flights` conserva el mismo comportamiento y los mismos filtros.

## Registrar una venta

`POST /venta` recibe JSON:

```json
{"flightId": 1, "passengerName": "Ana Perez"}
```

Devuelve `201` con la venta registrada y descuenta un asiento. Rechaza con `400` los campos obligatorios ausentes o inválidos y los vuelos sin lugares. Si el vuelo no existe, devuelve `404`.

La ruta anterior `POST /flights/{id}/sales`, con `passengerName` en el cuerpo, sigue disponible. Ambas usan la misma operación de venta.

## Cancelar una venta

`DELETE /flights/sales/{id}` anula la venta y devuelve el asiento al vuelo. Responde `204`; si la venta no existe, `404`. El backend la usa al cancelar una compra y para compensar una venta de ida cuando falla la de vuelta.

## Otras rutas

- `GET /flights/{id}`: un vuelo por ID.
- `POST /flights`: alta de un vuelo (el backend la expone solo al administrador).
- `/internal/demo/*`: catálogo y reservas de la seed de demo. Solo se habilitan con la seed y no se publican desde el backend.

## Logs

Cada request se loguea en una línea INFO con método, ruta, status y duración (por ejemplo, `GET /flights -> 200 (67 ms)`), salvo `/actuator/**`. Las ventas registradas y canceladas también quedan en INFO, y las rechazadas por falta de lugar en WARN, sin el nombre del pasajero. Cada línea lleva el `traceId` del backend, así que en Kibana una misma traza trae los logs de los dos servicios.

## Pruebas

Desde esta carpeta, con Java 25: `bash gradlew test ktlintCheck`.

Con Docker abierto también corren dos suites contra PostgreSQL 16 mediante Testcontainers:

- Nueve casos de búsqueda: las combinaciones de origen, destino y fecha, el orden de los resultados y la exclusión de vuelos sin lugares.
- Cinco casos del ciclo de vida: Flyway y la seed sobre una base real, el contrato de las reservas de demo y la venta simultánea del último asiento.

Sin Docker, esas suites se omiten; el resto de los tests usa H2. En GitHub Actions hay Docker disponible.
