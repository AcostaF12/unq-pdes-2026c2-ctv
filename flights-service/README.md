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

## Tests

Desde esta carpeta, con Java 25: `bash gradlew test ktlintCheck`.
