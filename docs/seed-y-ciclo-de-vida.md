# Seed de demo y ciclo de vida

La seed permite recorrer administración, venta, compra y reseñas con datos relacionados. Se ejecuta una vez por versión de datos y conserva el trabajo realizado desde la aplicación.

## Dataset inicial

| Datos | Cantidad |
| --- | ---: |
| Ciudades | 10 |
| Hoteles | 30 |
| Agencias | 4 |
| Administradores | 3 |
| Compradores | 6 |
| Vendedores, uno por agencia | 4 |
| Paquetes | 28 |
| Vuelos de ida y vuelta | 54 |
| Compras con snapshots | 26 |
| Reseñas con compra previa | 19 |
| Favoritos | 15 |
| Reservas de demo, dos por compra | 52 |

Hay nueve destinos, tres variantes de fecha, distintos precios y puntajes, paquetes sin reseñas y vuelos con poca disponibilidad. Las fechas de salida se calculan al crear el catálogo; no se desplazan al reiniciar. Las compras tienen fechas anteriores para probar filtros y el historial. La disponibilidad también incluye ocupación inicial de algunos vuelos para demostrar los últimos lugares.

Las cantidades son las de una base nueva. Si se inicializa una base parcial, se conservan los datos existentes y se agregan los faltantes. No se agrega actividad de comprador a cuentas de ejemplo que ya tengan otro rol o estén deshabilitadas.

## Accesos de demo

| Rol | Usuarios | Contraseña |
| --- | --- | --- |
| Administrador | `vferreyra`, `facosta`, `adisanto` | Igual al usuario |
| Comprador | `buyer`, `buyer2`, `lmartinez`, `tfernandez`, `sgomez`, `jperez` | `buyer123` |
| Vendedor Despegar | `agency` | `agency123` |
| Vendedor Almundo | `agency2` | `agency123` |
| Vendedor Flecha Tours | `agency3` | `agency123` |
| Vendedor Rumbo Austral | `agency4` | `agency123` |

Son credenciales públicas de ejemplo. La seed está habilitada por defecto en `dev` y deshabilitada en `prod`. `SEED_ENABLED=true/false` la controla en ambos servicios. El overlay `docker-compose.prod.yml` usado para la VM de demo la habilita explícitamente; para una instalación sin datos de ejemplo usar `SEED_ENABLED=false`.

## Cómo se carga

1. Flyway prepara y valida los esquemas de los dos servicios. Vuelos usa `validate` también en producción, sin `create-drop`.
2. Vuelos agrega o identifica sus vuelos de demo mediante claves únicas `ctv-demo-v1:DESTINO:VARIANTE:TRAMO`. Conserva los IDs reales, fechas, ventas y stock. Un vuelo anterior sin clave solo se adopta si la coincidencia de ruta, horario y aerolínea es inequívoca.
3. El backend bloquea el registro `ctv-demo-v1` de `demo_seed_runs`. Si ya terminó, no vuelve a cargar ni a recrear elementos borrados.
4. Antes de escribir, valida el grafo de fixtures y consulta el catálogo interno de vuelos. Los paquetes usan los IDs devueltos por ese catálogo; no dependen del orden de inserción.
5. Carga las tablas en orden mediante mapas de referencias y lecturas agrupadas. Cada cuenta nueva recibe su propio hash con sal; las contraseñas existentes no se recalculan.
6. Crea compras, reserva los asientos de ida y vuelta mediante un lote remoto y después agrega reseñas y favoritos. Cada reserva tiene una clave única; reintentar el lote no descuenta otro asiento.
7. Marca la versión como completa al confirmar la transacción local. Si falla, todos los cambios locales se revierten. Si el lote remoto llegó a confirmarse, sus claves permiten reintentar sin duplicarlo.

El catálogo y el lote interno de demo están en `/internal/demo/*` del servicio de vuelos y solo se habilitan con la seed. No se publican desde el backend ni forman parte de la navegación. El Compose principal mantiene vuelos dentro de la red interna.

## Paquetes publicados y archivados

- Crear un paquete lo publica. El catálogo de compradores lista únicamente los publicados.
- El administrador o la agencia propietaria puede archivarlo. Desaparece del catálogo y la API rechaza nuevas compras.
- El detalle sigue accesible desde favoritos o el historial; compras, precios abonados y reseñas se conservan.
- La agencia conserva el paquete en su panel y el administrador lo encuentra mediante el filtro de estado.
- Publicarlo otra vez requiere vuelos vigentes y asientos disponibles. Editar un paquete archivado conserva su estado.
- La eliminación física sigue limitada a paquetes sin compras, reseñas ni favoritos. Para una oferta vendida se usa archivar.

`PUT /packages/{id}/status` recibe `{ "active": false }` para archivar y `true` para publicar. `GET /admin/packages` incluye ambos estados y admite `active=true/false`. Estas operaciones están protegidas en la API.

Compra y cambio de estado bloquean la misma fila del paquete para resolver carreras. La venta de asientos bloquea el vuelo en PostgreSQL: dos compradores no pueden vender el último asiento simultáneamente. Cancelar una reserva también bloquea la venta y el vuelo antes de devolver stock.

## Reiniciar y reinicializar

Reiniciar los servicios conserva las cuentas, permisos, estados, historial, fechas y disponibilidad. No borra ni vuelve a llenar la base.

Para una demo completamente nueva usar el entorno temporal de pruebas:

```powershell
docker compose -f docker-compose.e2e.yml down
docker compose -f docker-compose.e2e.yml up --build -d --wait --wait-timeout 180
```

Ese entorno usa PostgreSQL con almacenamiento temporal. Apagarlo elimina sus datos de prueba. En el Compose normal los volúmenes son persistentes; respaldar ambas bases antes de eliminarlos. No borrar únicamente la base de vuelos: los paquetes guardan referencias a sus IDs reales.

No se ofrece un endpoint de reset desde administración. Para nuevas versiones del dataset se agrega una nueva versión/migración, en lugar de borrar datos al arrancar.

## Verificaciones

- `DemoDataSeederTest`: grafo completo, IDs de vuelos desplazados, base parcial, roles existentes, rollback y repetición sin cambios ni nuevas reservas.
- `SeedDataTest`: referencias, unicidad, compra previa de reseñas y claves del catálogo.
- `PackageLifecycleTest`: aislamiento por rol/agencia, archivo, publicación, rechazo de compra e historial.
- `FlightLifecyclePostgresTest`: Flyway en PostgreSQL, persistencia de la seed, lote idempotente y competencia por el último asiento.
- Cypress `package-lifecycle.cy.js`: archivo y publicación desde el panel, rechazo de compra y acceso al historial.

Los builds de ambos servicios ejecutan ktlint, tests y cobertura; las pruebas PostgreSQL necesitan Docker. Ver los comandos generales en [verificación de la entrega 2](entrega-2-verificacion.md).
