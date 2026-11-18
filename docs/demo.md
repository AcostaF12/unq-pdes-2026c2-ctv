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
2. **Mis paquetes** (`/agency`): crear, editar y eliminar paquetes de la agencia. Al editar, se cargan los valores actuales y se puede guardar o cancelar.
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

- **Tests.** Suites de backend, vuelos y frontend, 13 escenarios de aceptación con Cucumber y 10 tests end-to-end con Cypress. Consultar los logs de CI para los resultados de la versión presentada.
- **CI.** GitHub Actions corre lint, tests y build en cada módulo, y Sonar con Quality Gate. Los badges están en el [README](../README.md).
- **Ejecutar las pruebas.** Ver la sección *Testing* del README.

## Extensión del recorrido: usuarios y ciclo de vida

1. Crear una agencia con su cuenta responsable y agregar otro vendedor desde `/admin/users`.
2. Comprar un paquete y consultar su detalle desde `/admin/sales`.
3. Archivar el paquete y comprobar que se conserva el historial y se rechazan nuevas compras. Publicarlo otra vez.
4. Deshabilitar al vendedor: su sesión anterior y un nuevo login deben ser rechazados. Habilitarlo y cambiar su rol; verificar los permisos en la siguiente petición.
5. Desactivar la agencia: bloquear sus vendedores y nuevas compras, conservando los datos históricos.
6. Recorrer `/admin/reports`, favoritos, reseñas y compras globales.
7. Reiniciar ambos servicios y comprobar que la seed no restablece los cambios.

Ver el [dataset y sus credenciales](seed-y-ciclo-de-vida.md).
