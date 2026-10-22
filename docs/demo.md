# Guion de demo

Recorrido de unos 13 minutos. Qué mostrar en cada paso. La explicación de arquitectura, compra e infraestructura está en [Entrega-2.html](Entrega-2.html). Lo que puede hacer el administrador, en [admin.md](admin.md).

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

Para volver a los datos iniciales: `docker compose down -v` y levantar de nuevo.

La primera vez, Kibana no tiene vista de datos. En Stack Management, Data Views, crear `ctv-logs-*` con campo de tiempo `@timestamp`. Conviene dejarla hecha antes de la demo.

## 1. Comprador (`buyer` / `buyer123`)

1. **Login.** La navegación muestra solo lo que su rol puede usar.
2. **Paquetes** (`/trips`). Filtrar por destino y precio, y ordenar. La URL cambia: el filtrado, la paginación y el orden se resuelven en el backend.
3. **Detalle.** Fotos, vuelo de ida y de vuelta, puntaje y reseñas.
4. **Favoritos.** Marcar un paquete y verlo en `/favorites`.
5. **Compra.** Elegir un paquete que no diga que ya está comprado. El backend reserva ida y vuelta en `flights-service`. Si falla la vuelta, cancela la ida.
6. **Mis compras** (`/purchases`). Aparece el precio abonado. Probar un filtro. El historial guarda una copia de lo pagado: no cambia si después se edita el paquete. El detalle está en `/purchases/:id`.

## 2. Agencia (`agency` / `agency123`)

1. Entrar como `agency` (Despegar).
2. **Mis paquetes** (`/agency`). Crear uno eligiendo los vuelos de la lista, sin escribir ids. Se puede eliminar si nadie lo compró.
3. **Ventas** (`/agency/sales`). Compras de sus paquetes.
4. Un comprador que abre `/agency` ve "Sin permiso".

## 3. Administrador (`vferreyra` / `vferreyra`)

1. **Hoteles** (`/hotels`). Alta, edición y baja.
2. **Panel** (`/admin`).
   - `/admin/flights`: cargar un vuelo de ida y uno de vuelta.
   - `/admin/packages`: crear un paquete para Despegar con esos vuelos.
   - `/admin/agencies` y `/admin/users`: una agencia o una cuenta, si hay tiempo.
   - `/admin/sales`: ventas de todas las agencias.
3. Entrar como `buyer` y abrir `/admin`. Tiene que decir "Sin permiso".

## 4. Detrás de la pantalla

- **Swagger.** Contrato de la API.
- **Resiliencia.** `docker compose stop flights-service`, intentar comprar y mostrar el 503. El circuito figura en http://localhost:8080/actuator/health. Después, `docker compose start flights-service`.
- **Grafana.** Dashboard *CTV - Overview*.
- **Zipkin.** Una traza del servicio `backend`.
- **Kibana.** Discover, filtrar por `service`.

## 5. Cómo se entrega

La [verificación de la entrega 2](entrega-2-verificacion.md) contiene la matriz de roles y los comandos para repetir las pruebas.

- Los badges del [README](../README.md) apuntan a GitHub Actions y a Sonar.
- En `main`, las imágenes se publican en GHCR y en Docker Hub después de los controles.
- La VM de Azure se prende con el workflow `Deploy Demo (Azure VM)` y conviene apagarla con `stop` al terminar. Suele estar apagada.
