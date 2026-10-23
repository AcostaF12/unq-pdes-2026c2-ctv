# Flujo de la aplicación

Cómo se lee Compra tu Viaje cuando alguien entra, busca un paquete y lo compra. Este documento sigue un clic. El [guion de demo](demo.md) dice qué mostrar. [Seed y ciclo de vida](seed-y-ciclo-de-vida.md) dice qué datos ya vienen cargados.

## Cómo levantarlo

Desde la raíz del repo, con Docker Desktop abierto:

```bash
npm start
```

Eso ejecuta `docker compose up --build -d --wait`. Compila frontend, backend y vuelos, levanta las dos bases y no vuelve hasta que los chequeos de salud pasan. La primera vez tarda varios minutos. El monitoreo (Grafana, Prometheus, Zipkin, ELK) se activa con `npm run start:monitoring`.

| Para qué | Comando | Qué queda |
| --- | --- | --- |
| Ver si está sano | `npm run ps` | El estado de cada contenedor |
| Seguir lo que pasa | `npm run logs` | Las últimas líneas de todos los servicios |
| Apagar, conservando datos | `npm run down` | Contenedores apagados, volúmenes intactos |

Cuando `npm start` termina, la app está en http://localhost:8090. La API está en http://localhost:8080. Swagger está en http://localhost:8080/swagger-ui/index.html.

Cuentas para entrar: `buyer` / `buyer123`, `agency` / `agency123`, `vferreyra` / `vferreyra`. El resto está en [seed y ciclo de vida](seed-y-ciclo-de-vida.md).

`npm start` no es el modo de desarrollo con recarga. Ese modo, con las bases en Docker y cada programa en su terminal, está en el [README](../README.md#desarrollo-con-hot-reload) y abre el frontend en http://localhost:5173.

## La idea en una frase

La pantalla habla solo con la API del negocio, en el puerto 8080. Esa API guarda paquetes, usuarios, hoteles y compras en `backend_dev`. Si hace falta un asiento, llama por adentro a `flights-service`, que guarda vuelos y ventas en `flights_dev`. La pantalla nunca habla con vuelos ni con ninguna base.

```text
navegador :8090
    │  HTTP + token
    ▼
API del negocio :8080  ──►  backend_dev :5433
    │  HTTP interno, sin token del usuario
    ▼
servicio de vuelos :8081  ──►  flights_dev :5434
```

Dentro de Docker, vuelos no publica el 8081 al host. Solo el backend lo alcanza, en `http://flights-service:8081`. Las dos bases no se referencian entre sí: el paquete guarda el número del vuelo, no una clave foránea cruzada.

## Qué hace cada pieza

| Pieza | Dónde vive | Qué hace | Qué no hace |
| --- | --- | --- | --- |
| Pantalla | `frontend/`, puerto 8090 | Pinta rutas, manda el token y muestra el error que devuelve la API | No reserva asientos ni decide permisos |
| API del negocio | `backend/`, puerto 8080 | Login, catálogo, compras, favoritos, admin | No es dueña del stock de asientos |
| Servicio de vuelos | `flights-service/`, puerto 8081 interno | Alta de vuelos, venta y cancelación de un asiento | No conoce paquetes, agencias ni usuarios |
| Base del negocio | Postgres `backend_dev`, puerto 5433 | Usuarios, agencias, ciudades, hoteles, paquetes, compras, reseñas, favoritos | No guarda filas de vuelo |
| Base de vuelos | Postgres `flights_dev`, puerto 5434 | Vuelos y ventas de asiento | No guarda compras de paquetes |

Las redes de Docker separan quién puede hablar con quién: la pantalla solo está en `frontend-net`; el backend llega a su base, a vuelos y a las trazas; vuelos llega a su base. Prometheus, Grafana, Zipkin y Kibana miran el sistema. Si están apagados, la compra sigue funcionando.

## Un clic, de la pantalla a la base

1. La ruta de React decide qué página mostrar. El catálogo es `/trips`. El mapa está en `frontend/src/router/AppRouter.tsx`.
2. La página pide datos con el cliente de `frontend/src/api/client.ts`. La base es `http://localhost:8080`, salvo que el build traiga `VITE_API_BASE_URL`.
3. Si hay sesión, el cliente agrega `Authorization: Bearer …`. El token salió de `POST /auth/login` y dura 24 horas.
4. En cada pedido, el backend vuelve a leer el usuario. Si la cuenta está apagada o la agencia quedó inactiva, responde 401 y la pantalla cierra la sesión. Si el rol no alcanza, responde 403. El menú esconde pantallas; la API es la que autoriza.
5. El controller recibe el pedido, el service decide y el repository habla con `backend_dev`.
6. Solo algunas operaciones salen de esa base y llaman a vuelos. El cliente es `FlightsClient`.

La página 0 es la primera. La pantalla pinta `content` y arma los botones con `totalPages`. La página siguiente es otro pedido, no un recorte en el navegador.

## Qué hace cada rol

La raíz `/` redirige a `/trips`.

| Rol | Entra con | Recorre | La API exige |
| --- | --- | --- | --- |
| Invitado | login o registro | `/login`, `/register` | `/auth/**` es público |
| Comprador | `buyer` | `/trips`, `/trips/:id`, `/favorites`, `/purchases` | Comprar, favoritos y reseñas piden rol comprador |
| Agencia | `agency` | `/agency`, `/agency/sales` | Sus paquetes y sus ventas piden rol agencia |
| Admin | `vferreyra` | `/admin/…`, hoteles | `/admin/**` y el alta de hoteles, agencias y vuelos piden rol admin |

Un comprador que abre `/admin` ve “Sin permiso” en la pantalla. Si llama igual a `GET /admin/reports`, la API responde 403.

### Comprador

1. En `/trips` filtra origen, destino, precio y orden. Eso es `GET /packages` con query params. El filtro y el orden se resuelven en la API, no en el navegador.
2. En `/trips/:id`, `GET /packages/{id}` trae hotel, reseñas y, si vuelos responde, la ida y la vuelta.
3. Marcar favorito es `POST /favorites` con `{ "packageId": … }`.
4. Comprar es `POST /purchases` con el mismo campo. El detalle de esa compra está más abajo.
5. `/purchases` lee `GET /purchases/me`. El precio es el que se pagó, guardado en la compra. Si mañana cambia el paquete, el ticket no cambia.

### Agencia

En `/agency`, crear un paquete es `POST /packages` con nombre, hotel, vuelo de ida, vuelo de vuelta y precio. La agencia no manda su id: el backend usa la agencia de la sesión. `/agency/sales` es `GET /purchases/agency`.

### Admin

| Pantalla | Qué hace |
| --- | --- |
| `/admin/users` y `/admin/agencies` | Personas y agencias |
| `/admin/flights` | Alta de un vuelo. El backend lo reenvía a vuelos |
| `/admin/packages` | Ofertas de cualquier agencia. Acá sí manda `agencyId` |
| `/admin/sales` | Historial global |
| `/admin/reports` | Un solo `GET /admin/reports` con cuatro rankings de hasta cinco filas |
| Archivar | `PUT /packages/{id}/status` con `{ "active": false }`. Sale del catálogo y rechaza compras. El historial sigue |

El detalle de pantallas de admin está en [admin.md](admin.md).

## La compra, paso a paso

Ocurre en `PurchaseService.purchase`. La pantalla solo hace `POST /purchases`.

1. Confirma que quien llama es comprador.
2. Bloquea la fila del paquete en `backend_dev`, para no cruzarse con un archivo en el mismo momento.
3. Rechaza la compra si la agencia está inactiva o el paquete está archivado.
4. Pide a vuelos que venda el asiento de ida. Vuelos bloquea la fila del vuelo en `flights_dev`. Si otra persona se quedó con el último lugar, esta espera, relee y ve cero.
5. Pide el asiento de vuelta. Si esa venta falla, cancela la ida y devuelve el error. La compra local no se guarda.
6. Guarda la compra en `backend_dev` con el precio, la agencia y una copia del paquete en ese momento. Si ese guardado falla, cancela la ida y la vuelta.

No hay una transacción única entre las dos bases. Si la cancelación también falla, el error se traga y puede quedar un asiento vendido sin compra. No hay una cola de reintentos. El último asiento sí queda cerrado con el bloqueo de la fila del vuelo.

Si vuelos no responde, el backend reintenta tres veces, con 500 ms, solo cuando no hay conexión. Si falla la mitad de las últimas diez llamadas, deja de llamar durante diez segundos y el usuario recibe 503. Un “sin lugar” no se reintenta: es un resultado del negocio, no una caída.

## Qué guarda cada base

`backend_dev` tiene usuarios, agencias, ciudades, hoteles, paquetes, compras, reseñas, favoritos y la marca de que los datos de demo ya se cargaron. `flights_dev` tiene vuelos y ventas de asiento.

El paquete apunta a dos números de vuelo. La compra guarda esos números y el precio de ese momento. No hay una línea de clave foránea de una base a la otra.

Flyway versiona cada esquema por su lado. Los datos de demo se cargan una sola vez. Reiniciar los servicios no borra usuarios, compras ni stock. Para volver al dataset inicial hay que borrar los volúmenes a propósito (`docker compose down -v`) y volver a hacer `npm run up`.

## Dónde mirarlo mientras corre

| Pantalla | Dirección | Qué muestra |
| --- | --- | --- |
| App | http://localhost:8090 | El recorrido de comprador, agencia y admin |
| Swagger | http://localhost:8080/swagger-ui/index.html | Las rutas de la API del negocio. No lista la API interna de vuelos |
| Salud | http://localhost:8080/actuator/health | Si el corte de protección hacia vuelos está abierto |
| Grafana | http://localhost:3001 (`admin` / `admin`) | Servicios vivos, memoria y pedidos por segundo |
| Zipkin | http://localhost:9411 | El recorrido de una compra: entra al backend y sigue a vuelos |
| Kibana | http://localhost:5601 | Logs de los dos programas. La primera vez hay que crear la vista `ctv-logs-*` |

## Si preguntan, abrís

| Pregunta | Archivo |
| --- | --- |
| Qué página corresponde a cada URL | `frontend/src/router/AppRouter.tsx` y `adminRoutes.tsx` |
| Cómo viaja el token | `frontend/src/api/client.ts` |
| Quién puede llamar cada ruta | `backend/.../config/SecurityConfig.kt` |
| La compra y la cancelación de la ida | `PurchaseService.purchase` |
| El último asiento | `FlightService.sell` y `FlightRepository.findForUpdate` |
| La llamada de una API a la otra | `FlightsClient` |
| Los datos que ya vienen | [seed-y-ciclo-de-vida.md](seed-y-ciclo-de-vida.md) |
