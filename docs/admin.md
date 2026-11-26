# Sección Administración

Guía de lo que puede hacer el rol administrador en la aplicación.

## Cómo entrar

1. Levantar el stack (`docker compose up --build -d --wait`).
2. Abrir http://localhost:8090 e iniciar sesión con:

| Usuario | Contraseña |
|---------|------------|
| `vferreyra` | `vferreyra` |

`facosta` y `adisanto` también son administradores (contraseña igual al usuario). Los carga la seed de demo (`DemoDataSeeder`, con los datos de `SeedData`), que `DataBootstrap` ejecuta al arrancar si `SEED_ENABLED` está activo.

La navegación muestra las opciones de administración solo al rol administrador. Un comprador o una agencia que intente abrir esas rutas ve "Sin permiso", y la API responde con error de autorización.

## Qué puede hacer un administrador

### Consultas de sólo lectura

Desde el panel se accede a cuatro listados paginados:

| Ruta | Información |
| --- | --- |
| `/admin/queries/users` | Usuario, nombre, rol y agencia vinculada, sin credenciales. |
| `/admin/favorites` | Comprador, paquete guardado y agencia. |
| `/admin/reviews` | Comprador, paquete, puntaje de 0 a 10 y comentario. |
| `/admin/purchases` | Comprador, paquete, agencia, fecha y precio abonado originales de la compra. |

Las pantallas y sus endpoints (`GET /admin/users`, `GET /admin/favorites`, `GET /admin/reviews` y `GET /admin/purchases`) son exclusivos de ADMIN. Las respuestas usan `content`, `page`, `size`, `totalElements` y `totalPages`. La API admite `page` desde 0 y `size` entre 1 y 100, con valores por defecto 0 y 20. No hay edición ni borrado desde estas consultas y no se consulta el servicio de vuelos.

Las compras muestran sus datos históricos aunque el paquete cambie. Las consultas incluyen registros de agencias inactivas.

### Reportes Top 5

En `/admin/reports`, desde "Ver reportes", se muestran cuatro rankings de sólo lectura. El endpoint `GET /admin/reports` también es exclusivo de ADMIN.

- Compradores con más compras: cantidad de compras por comprador.
- Agencias con más ventas: cantidad de compras vendidas por agencia.
- Destinos más vendidos: cantidad de compras por código de destino histórico, guardado al comprar. Editar el destino del paquete no mueve ventas anteriores a otro destino.
- Destinos mejor puntuados: promedio de todas las reseñas de los paquetes del destino actual. Cada reseña pesa lo mismo, incluso si el puntaje es cero. Se muestra la cantidad de reseñas que forma el promedio.

Se considera todo el historial, incluidas agencias inactivas. Cada reporte devuelve como máximo cinco filas; los grupos sin compras o sin reseñas no aparecen. La base agrupa, ordena y limita los resultados, sin cargar todo el historial ni consultar la API de vuelos.

Los empates por cantidad se resuelven por nombre de comprador/agencia y luego ID, o por código de destino. Los empates de puntaje se resuelven por cantidad de reseñas y luego código. El orden usa los promedios sin redondear; la pantalla los muestra con hasta dos decimales. Si un código de destino tiene distintos nombres históricos, se muestra el mayor nombre en orden alfabético, agrupando siempre por código. Los nombres de compradores y agencias son los actuales.

### Agencias

En `/admin/agencies` se listan las agencias y su estado. El administrador puede crear una agencia junto con su cuenta, editar su nombre, desactivarla y reactivarla. No se borran agencias.

La desactivación requiere confirmación. Bloquea el login y las solicitudes con tokens anteriores mientras la agencia esté inactiva, oculta sus paquetes del catálogo de venta y rechaza nuevas compras. Conserva las compras, favoritos y reseñas. Al reactivarla, sus cuentas y paquetes vuelven a estar disponibles; un token anterior no vencido vuelve a ser válido.

Los campos de cuenta se cargan al crear la agencia. Esta pantalla no cambia usuarios ni contraseñas de cuentas existentes.

### Hoteles

Ruta: `/admin/hotels`. Desde ahí se crea, edita y elimina sin salir del panel. Cada fila tiene "Ver ficha", que abre el hotel en el catálogo público (`/hotels/:id`).

El orden para armar una oferta es Hotel, Vuelos y Paquete. Esa secuencia está en las tres pantallas y en la navegación del panel.

### Paquetes de cualquier agencia

Ruta: `/admin/packages`. La entrada de la sección es `/admin`.

- Cargar paquetes eligiendo la agencia a la que pertenecen.
- Editar y eliminar paquetes de cualquier agencia.

### Vuelos

Ruta: `/admin/flights`.

- Cargar vuelos en `flights-service` para poder usarlos en los paquetes.
- Las agencias y los administradores eligen los vuelos de ida y de vuelta de un paquete desde un selector, en lugar de escribir identificadores.

### Operación

En `/admin/system`, desde "Ver operación", el administrador ve el instante del backend sin abrir Actuator ni Prometheus:

- Salud del backend, de su base y del servicio de vuelos.
- Circuit breaker hacia vuelos: cerrado, abierto o probando, y el porcentaje de fallos cuando hay muestras.
- Pool de conexiones (activas, libres, pendientes y tope) e hilos que atienden pedidos.
- Requests acumulados desde el arranque, errores 4xx y 5xx, promedio y los cinco endpoints con más tráfico, con su p95.
- El recorrido de una compra y de una cancelación, el p95 de `POST /purchases` y las dos suites de prueba: la corta (`npm run test:stress` y Kubernetes) y Load Tests contra la VM.

La pantalla y Prometheus se actualizan cada 5 segundos, también en la VM. Prometheus guarda solo las series que dibujan los tableros. El chequeo de vuelos se reutiliza dos segundos. Pasado ese tiempo el pedido devuelve el último resultado y la renovación corre en segundo plano, así la pantalla no espera el HTTP a vuelos. El p95 se calcula solo para los cinco endpoints que se muestran. No scrapea Prometheus y no dispara las pruebas: las corre el workflow. La observabilidad de Grafana, Kibana y Zipkin ya está armada. La cancelación queda cubierta por `PurchaseCancellationControllerTest`: la segunda vez responde 400 y cada asiento se devuelve una sola vez.

Corridas de referencia, ambas en verde:

- [Kubernetes CI](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/runs/45822899741): smoke y autorecuperación, todavía sin la suite corta. En este workflow esa suite corre después del smoke y antes de borrar un pod.
- [Load Tests en la VM](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/runs/45822461627): `catalog-load`, `login-load`, `last-seat-stress` y `catalog-spike`.

Abajo hay enlaces a Grafana (3001), Kibana (5601), Zipkin (9411) y Prometheus (9090), en el mismo host desde el que se abrió la aplicación. Hace falta el perfil `monitoring`. Se pueden pisar con `VITE_GRAFANA_URL`, `VITE_KIBANA_URL`, `VITE_ZIPKIN_URL` y `VITE_PROMETHEUS_URL` al construir el frontend. En la VM, Prometheus no publica puerto.

## Verificación rápida

1. Entrar como `vferreyra` y confirmar que aparece la navegación de administración.
2. Crear un hotel, editarlo y eliminarlo.
3. Cargar un vuelo de ida y uno de vuelta.
4. Crear un paquete para la agencia Despegar usando esos vuelos y comprobar que aparece en el catálogo y en el panel de la agencia (`agency` / `agency123`).
5. Entrar como `buyer` / `buyer123` y confirmar que las rutas de administración no están disponibles.
6. En `/admin/system`, confirmar salud, circuit breaker y los enlaces a Grafana, Kibana y Zipkin.

## Referencias

- Contrato de la API: Swagger en http://localhost:8080/swagger-ui/index.html.
- Guion de demo: [demo.md](demo.md).

## Usuarios, ventas y paquetes archivados

El panel agrega gestión de usuarios y vendedores, roles, estado de cuenta e historial global de ventas. `/admin/users` permite crear y editar cuentas; `/admin/queries/users` conserva la consulta sin edición. Un vendedor necesita cuenta habilitada y agencia activa. El administrador no puede deshabilitarse ni quitarse su propio rol.

El panel de paquetes permite archivar una oferta vendida y publicarla nuevamente con vuelos vigentes y lugares disponibles. Archivar conserva compras, favoritos y reseñas. Ver [seed y ciclo de vida](seed-y-ciclo-de-vida.md).
