# Backlog del frontend — Fase 1

Criterio general: no agregar dependencias, no cambiar contratos del backend, no simular (mockear) paquetes, compras, favoritos ni reseñas.

## Etapa A — Sesión y estructura de la app

**Hecho cuando**

- Login y registro guardan `token` + `user` y navegan a `/hotels`.
- Axios manda `Authorization: Bearer`.
- 401 (fuera de `/auth/login` y `/auth/register`) limpia sesión y vuelve a login.
- Recargar con JWT válido mantiene la sesión (`GET /users/me`).
- `AppShell` muestra Hoteles, Perfil, Salir; el administrador ve “Nuevo hotel”.
- Sin sesión no se entra a rutas privadas.
- Con sesión, `/login` y `/register` redirigen a `/hotels`.
- Tests de Login/Register apuntan a `/hotels` (ya no `/trips`).

**Probar:** `npm test` · login `buyer` / `buyer123` · recargar · logout · token inválido.

## Etapa B — Catálogo y detalle

**Hecho cuando**

- `/hotels` lista hoteles con filtro por ciudad en el cliente.
- Hay estados de carga, vacío, error de red y reintento.
- `/hotels/:id` muestra nombre, ciudad y foto con imagen alternativa si la URL no carga.
- Hotel inexistente muestra mensaje claro.

**Probar:** catálogo con seed · filtrar París · detalle · foto rota no se ve ícono quebrado.

## Etapa C — ABM del administrador y perfil

**Hecho cuando**

- El administrador crea, edita y borra (con confirmación).
- Ciudad es un select de `GET /cities`.
- Comprador y agencia no ven el alta ni rompen al entrar a `/hotels/new`.
- `/profile` muestra nombre, usuario, rol y agencia si aplica.
- 403 de API se muestra sin romper la página.

**Probar:** `facosta` / `facosta` ABM · `buyer` no ve el alta · `agency` ve agencia en perfil.

## Prueba manual rápida por rol

1. `buyer` / `buyer123` — catálogo, detalle, perfil, no alta.
2. `agency` / `agency123` — igual + agencia en perfil.
3. `facosta` / `facosta` — crear, editar, borrar hotel.
4. Recargar logueado no pierde sesión.
5. 401 / token inválido vuelve a login.

Comandos: `npm test` y `npm run lint` en `frontend/`. UI local: `npm run dev` o `http://localhost:8090` con compose.
