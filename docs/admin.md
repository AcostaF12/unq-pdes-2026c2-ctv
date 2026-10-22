# Sección Administración

Guía de lo que puede hacer el rol administrador en la aplicación.

## Cómo entrar

1. Levantar el stack (`docker compose up --build -d --wait`, o `docker compose -f docker-compose.hub.yml up -d --wait` para usar las imágenes de Docker Hub).
2. Abrir http://localhost:8090 e iniciar sesión con:

| Usuario | Contraseña |
|---------|------------|
| `vferreyra` | `vferreyra` |

`facosta` y `adisanto` también son administradores (contraseña igual al usuario). Los carga `DataBootstrap`.

La navegación muestra las opciones de administración solo al rol administrador. Un comprador o una agencia que intente abrir esas rutas ve "Sin permiso", y la API responde con error de autorización.

## Qué puede hacer un administrador

### Consultas de sólo lectura

Desde el panel se accede a cuatro listados paginados:

| Ruta | Información |
| --- | --- |
| `/admin/users` | Usuario, nombre, rol y agencia vinculada, sin credenciales. |
| `/admin/favorites` | Comprador, paquete guardado y agencia. |
| `/admin/reviews` | Comprador, paquete, puntaje de 0 a 10 y comentario. |
| `/admin/purchases` | Comprador, paquete, agencia, fecha y precio abonado originales de la compra. |

Las pantallas y los endpoints del mismo nombre son exclusivos de ADMIN. Las respuestas usan `content`, `page`, `size`, `totalElements` y `totalPages`. La API admite `page` desde 0 y `size` entre 1 y 100, con valores por defecto 0 y 20. No hay edición ni borrado desde estas consultas y no se consulta el servicio de vuelos.

Las compras muestran sus datos históricos aunque el paquete cambie. Las consultas incluyen registros de agencias inactivas. Los reportes Top 5 se implementarán aparte.

### Agencias

En `/admin/agencies` se listan las agencias y su estado. El administrador puede crear una agencia junto con su cuenta, editar su nombre, desactivarla y reactivarla. No se borran agencias.

La desactivación requiere confirmación. Bloquea el login y las solicitudes con tokens anteriores mientras la agencia esté inactiva, oculta sus paquetes del catálogo de venta y rechaza nuevas compras. Conserva las compras, favoritos y reseñas. Al reactivarla, sus cuentas y paquetes vuelven a estar disponibles; un token anterior no vencido vuelve a ser válido.

Los campos de cuenta se cargan al crear la agencia. Esta pantalla no cambia usuarios ni contraseñas de cuentas existentes.

### Hoteles

- Ver el catálogo (`/hotels`).
- Crear, editar y eliminar hoteles desde la interfaz.

### Paquetes de cualquier agencia

Ruta: `/admin/packages`. La entrada de la sección es `/admin`.

- Cargar paquetes eligiendo la agencia a la que pertenecen.
- Editar y eliminar paquetes de cualquier agencia.

### Vuelos

Ruta: `/admin/flights`.

- Cargar vuelos en `flights-service` para poder usarlos en los paquetes.
- Las agencias y los administradores eligen los vuelos de ida y de vuelta de un paquete desde un selector, en lugar de escribir identificadores.

## Verificación rápida

1. Entrar como `vferreyra` y confirmar que aparece la navegación de administración.
2. Crear un hotel, editarlo y eliminarlo.
3. Cargar un vuelo de ida y uno de vuelta.
4. Crear un paquete para la agencia Despegar usando esos vuelos y comprobar que aparece en el catálogo y en el panel de la agencia (`agency` / `agency123`).
5. Entrar como `buyer` / `buyer123` y confirmar que las rutas de administración no están disponibles.

## Referencias

- Contrato de la API: Swagger en http://localhost:8080/swagger-ui/index.html.
- Guion de demo: [demo.md](demo.md).
