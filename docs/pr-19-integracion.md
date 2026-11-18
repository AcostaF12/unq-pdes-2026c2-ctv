# Revisión e integración de la PR 19

La rama de administración se integra contra `dev`, conservando las funciones ya incorporadas por las PR 16, 17, 18 y 21.

## Decisiones

| Área | Resultado integrado |
| --- | --- |
| Usuarios y roles | Alta, edición, habilitación y asignación de agencia desde `/admin/users`. La API conserva el listado paginado anterior y agrega estado de cuenta. No expone contraseñas. |
| Agencias | Se mantiene el alta atómica de agencia y cuenta responsable de `dev`, junto con edición, desactivación y reactivación. Se conserva la validación de nombres duplicados de la PR 19. |
| Acceso | El vendedor necesita cuenta habilitada y agencia activa. Login y tokens existentes respetan ambos controles. El administrador no puede bloquearse ni quitarse su propio rol. |
| Consultas y reportes | Se conservan favoritos, reseñas, compras y los cuatro reportes Top 5. La consulta de usuarios sin edición sigue disponible en `/admin/queries/users`. |
| Ventas | Se incorpora historial global con filtros y acceso del administrador al detalle. Las operaciones históricas se conservan al cambiar roles. |
| Paquetes | Se mantiene la edición por la agencia propietaria de `dev` y se agrega archivo/publicación. Se excluyen del catálogo los archivados y los de agencias inactivas. |
| Seed | Se incorpora la carga versionada, transaccional e idempotente. Preserva IDs, permisos, credenciales, ediciones e historial. Usa IDs reales del servicio de vuelos. |
| Persistencia | Se mantiene `V3__agency_active.sql` de `dev`. Usuarios se agrega en V4 y seed/paquetes en V5. Vuelos pasa a Flyway y validación de esquema. |

## Cambios que no se deben subir

- La segunda migración V3 de la versión original de la PR: chocaba con la migración de agencias ya integrada. Se reemplaza por V4 sin modificar V3.
- Un segundo controlador para `GET /admin/users`: produciría un mapeo ambiguo al iniciar Spring. Se unifica en el controlador de gestión.
- La versión anterior de agencias que creaba solo su nombre: perdería el alta conjunta de cuenta y la desactivación ya disponibles en `dev`.
- Archivos locales de herramientas, capturas, resultados de pruebas, dependencias y cambios de documentación ajenos a este trabajo.
- Reinicialización destructiva de vuelos con `create-drop`, supuestos sobre IDs consecutivos y recarga de datos en cada arranque.

La PR conserva el historial existente; la integración se agrega mediante commits, sin forzar ni reescribir la rama remota. El destino de la PR es `dev`. Los resultados locales y los controles remotos deben corresponder al último commit antes de aprobar el merge.

Para reproducir las pruebas y el recorrido funcional ver [verificación de entrega 2](entrega-2-verificacion.md) y [seed y ciclo de vida](seed-y-ciclo-de-vida.md).
