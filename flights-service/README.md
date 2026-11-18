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

## Pruebas

Desde esta carpeta, con Java 25: `bash gradlew test ktlintCheck`.

Con Docker abierto también se ejecutan nueve casos de búsqueda contra PostgreSQL 16
mediante Testcontainers. Cubren las combinaciones de origen, destino y fecha, el orden
de los resultados y la exclusión de vuelos sin lugares. Sin Docker, esos casos se omiten;
el resto de los tests usa H2. En GitHub Actions hay Docker disponible.
