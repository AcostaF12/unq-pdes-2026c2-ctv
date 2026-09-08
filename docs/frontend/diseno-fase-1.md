# Diseño frontend — Fase 1

Fuente de verdad para el frontend de **Compra Tu Viaje** mientras el backend solo expone auth, perfil, ciudades y hoteles.

## Alcance

**Adentro**

| Capacidad | API |
|-----------|-----|
| Registro de comprador | `POST /auth/register` |
| Login | `POST /auth/login` |
| Perfil | `GET /users/me` |
| Listar / ver hoteles | `GET /hotels`, `GET /hotels/{id}` |
| Listar ciudades | `GET /cities` |
| ABM de hoteles | `POST/PUT/DELETE /hotels` (solo `ADMIN`) |

**Afuera (no mockear)**

Paquetes, compras, favoritos, reviews, gestión de agencia, vuelos, ruta `/trips`.

Cuando existan APIs de paquetes, `/packages` pasa a ser el home y `/hotels` queda como catálogo/admin.

## Roles

| Rol | Qué puede hacer en esta fase |
|-----|------------------------------|
| `BUYER` | Entrar, ver catálogo, detalle, perfil, salir |
| `AGENCY` | Igual que buyer. El DTO trae `agency`; se muestra en perfil |
| `ADMIN` | Lo mismo + crear, editar y eliminar hoteles |

El registro público siempre crea un `BUYER`. Usuarios seed: `buyer` / `buyer123`, `agency` / `agency123`, `facosta` / `facosta`.

## Mapa de información

| Pantalla | Datos | Origen |
|----------|-------|--------|
| Login / Register | credenciales | formulario → `/auth/*` |
| Sesión | `token`, `user` | localStorage + hidratación con `/users/me` |
| Catálogo | lista de hoteles, ciudades para filtrar | `GET /hotels`, `GET /cities` |
| Detalle | un hotel | `GET /hotels/{id}` |
| Alta / edición | `name`, `cityCode`, `photoUrl` | ciudades + `POST/PUT /hotels` |
| Perfil | nombre, usuario, rol, agencia opcional | `/users/me` o sesión hidratada |

Filtro por ciudad: **solo en cliente**. `GET /hotels` no acepta query params.

Las `photoUrl` del seed (`https://images.ctv.demo/...`) no cargan. Toda foto usa fallback visual si falla.

## Rutas

| Ruta | Quién | Pantalla |
|------|-------|----------|
| `/` | todos | redirect a `/hotels` (sin sesión → login) |
| `/login`, `/register` | público | existentes. Si hay sesión → `/hotels` |
| `/hotels` | autenticado | catálogo (home) |
| `/hotels/new` | `ADMIN` | alta. Declarar **antes** de `:id` |
| `/hotels/:id` | autenticado | detalle |
| `/hotels/:id/edit` | `ADMIN` | edición |
| `/profile` | autenticado | perfil |
| `*` | todos | 404 |

## Flujos

```
visita → ¿JWT? → no → /login o /register
                 → sí → /hotels

login/register OK → persistir token+user → /hotels
401 en API autenticada → limpiar sesión → /login
admin sin rol en alta/edición → mensaje de permiso, no pantalla rota
hotel inexistente → mensaje 404 en detalle
```

## Contratos (no cambiar backend)

Auth: `{ token, user }`

```
user = {
  id, username, role, firstName, lastName,
  agency?: { id, name }   // solo AgencyUser
}
```

`role` es `BUYER` | `AGENCY` | `ADMIN`.

Hotel: `{ id, name, city: { code, name }, photoUrl }`

Alta/edición: `{ name, cityCode, photoUrl }`

Ciudades: `[{ code, name }]`

Error: `{ httpCode, httpStatus, errorData: { description }, timestamp }`

Mutaciones de hoteles sin rol admin → `403`. Token inválido → `401`. Hotel inexistente → `404`.

## Sistema visual

Extender `frontend/src/styles/tokens.css`: arena, terracota, marino, sombras hard, radius 0, Archivo Black + Space Grotesk.

Componentes nuevos mínimos:

- `AppShell` — header, nav, logout (`Button` ghost)
- `HotelCard` — foto con fallback, nombre, ciudad
- `HotelPhoto` — `onError` → bloque con código IATA
- `EmptyState` — vacío / error / sin permiso
- `Select` — mismo look que `Input`

Reusar `Button` e `Input`. No agregar UI kit ni Tailwind.

## Estados de UI

| Estado | Tratamiento |
|--------|-------------|
| Loading | texto/bloque "Cargando…" en el área de contenido |
| Vacío | `EmptyState` (catálogo sin resultados o filtro sin match) |
| Error de red | mensaje + opción de reintentar |
| 401 | logout y `/login` |
| 403 | "Esta acción es solo para administradores." |
| 404 de hotel | "No encontramos este hotel." |
| Foto rota | fallback con código de ciudad, nunca ícono roto |

## Arquitectura frontend

- Sin dependencias nuevas.
- Axios único en `api/client.ts` con Bearer e interceptor 401 (excepto `/auth/login` y `/auth/register`).
- `AuthContext` + guards `RequireAuth`, `RequireAdmin`, `RequireGuest`.
- CSS co-located por componente/página.
- Formularios: react-hook-form + Zod, igual que login/register.
