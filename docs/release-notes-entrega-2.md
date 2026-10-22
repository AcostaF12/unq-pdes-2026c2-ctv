# Release notes - Entrega 2

Estas notas resumen la aplicación y los cambios de pruebas y CI del segundo checkpoint, integrados en `dev` mediante los [PR #13](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/pull/13) y [PR #15](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/pull/15). CI es la ejecución automática de controles cuando subimos código a GitHub.

## Qué agregamos

La aplicación permite buscar paquetes, guardarlos como favoritos, comprarlos, consultar el historial y dejar reseñas tras una compra. Las agencias crean y eliminan sus paquetes y consultan sus ventas. El administrador gestiona hoteles y paquetes de cualquier agencia y carga vuelos. Los formularios permiten elegir vuelos de ida y vuelta sin escribir IDs. También mejoramos el catálogo, sus filtros, los puntajes y el detalle de las compras. El recorrido para mostrar estas funciones está en el [guion de demo](demo.md).

Conectamos las pruebas de aceptación y de interfaz al CI, agregamos revisión de estilo en Kotlin y reforzamos los controles previos al deploy manual. También validamos los archivos que definen el propio CI.

Backend y vuelos ya tenían tests con JUnit y reportes de cobertura con JaCoCo. Agregamos ktlint a ambos módulos y formateamos el código existente en commits separados. Ahora el build compila, ejecuta tests y revisa el estilo. CI no corrige el formato automáticamente: si encuentra un problema, falla.

El frontend mantiene ESLint, Vitest y el build de Vite. Los nuevos controles no reemplazan estas suites ni garantizan por sí solos una cobertura completa.

## Qué probamos

### Aceptación con Cucumber

Los 13 escenarios llaman a las APIs reales y comprueban estas reglas:

- Un comprador puede iniciar sesión; una agencia puede publicar un paquete y un comprador no.
- Las búsquedas respetan origen, destino y filtros avanzados.
- Una compra aparece en el historial, reduce los asientos de ambos vuelos y conserva el precio aunque la agencia cambie el paquete.
- Si no hay lugar en el regreso, se recupera el asiento reservado en la ida y no queda una compra.
- No se puede reseñar sin compra previa ni repetir la reseña del mismo comprador sobre un paquete.
- Un administrador carga vuelos y publica un paquete que un comprador encuentra; una agencia no puede cargar vuelos.

Ajustamos los tests a las respuestas paginadas de la API y corregimos los perfiles de ejecución de Cucumber. En CI, el resultado se imprime en consola.

### Interfaz con Cypress

Los diez tests recorren la aplicación desde el navegador:

1. Login, búsqueda por nombre/origen/destino y consulta del detalle.
2. Contraseña incorrecta y reintento exitoso.
3. Restricción del formulario de hoteles y su API para un comprador.
4. Creación, edición y eliminación de un hotel por un administrador.
5. Compra e historial con viaje y precio conservados al recargar.
6. Acceso a la sección administrativa según el rol.
7. Carga de vuelos, publicación de un paquete por un administrador y compra desde el catálogo.
8. Edición del precio y eliminación de un paquete sin compras.
9. Rechazo de la eliminación de un paquete con compras.
10. Creación de un paquete por una agencia mediante los selectores de hotel y vuelos, y su eliminación.

Cucumber y Cypress usan entornos Docker aislados. Cypress corre por terminal con Electron sin ventana visible, pero sigue probando un navegador real. Antes de empezar, CI espera que los servicios estén saludables.

## Calidad y validación del CI

Sonar analiza backend y vuelos, incluyendo la cobertura de JaCoCo. Ahora CI espera hasta 300 segundos el resultado del Quality Gate, las condiciones de calidad configuradas en Sonar. Si no pasa, falla el job.

Una ejecución interna sin `SONAR_TOKEN` también falla. Los PR de forks omiten Sonar con un aviso porque no reciben los secretos; sus tests y lint siguen corriendo. No agregamos Sonar al frontend ni un mínimo fijo de 80 % de cobertura.

Además, automatizamos dos controles:

- `actionlint` revisa la sintaxis y las expresiones de los workflows.
- `check-ci.rb` comprueba nuestras reglas, como exigir controles antes de publicar, esperar Sonar y mantener independiente la opción de apagar la VM.

Los resultados quedan en los logs de GitHub Actions. No subimos videos, capturas ni archivos de resultados. Si falla Cucumber o Cypress, se muestran logs de la aplicación y se intenta apagar los contenedores igualmente.

## Qué pasa en un PR y en un deploy

En PR y push a `dev` o `main`, GitHub ejecuta los controles aplicables. Los CI de backend, vuelos y frontend mantienen filtros por carpeta; aceptación, e2e y validación de workflows no tienen esos filtros.

Los jobs de publicación se omiten en los PR. Es intencional: abrir un PR no publica imágenes ni despliega en Azure.

En push a `main`, los workflows de los módulos publican en Docker Hub y GHCR después de sus controles. Agregamos `docker-compose.hub.yml` para usar las imágenes publicadas sin compilar. La publicación requiere credenciales de Docker Hub y todavía debe verificarse para esta versión.

El deploy manual reutiliza los workflows de pruebas. Su secuencia es:

1. Ejecutar build, tests y lint de los tres módulos, Sonar, Cucumber, Cypress y validación de workflows.
2. Si todo pasa, construir y publicar las tres imágenes con el identificador del commit.
3. Prender la VM, descargar esas imágenes y levantar la aplicación sin compilar en Azure.
4. Comprobar la salud del backend.

Si un control falla, no se publica ni se despliega. Las imágenes de pruebas y de publicación corresponden al mismo código, aunque no son exactamente los mismos archivos binarios. Si falla la descarga, el script se detiene antes de levantar la aplicación. La acción `stop` sigue independiente.

## Verificación y pendientes

Los [checks del PR #13](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/pull/13/checks) conservan la evidencia de la implementación inicial del CI. No representan por sí solos el estado de la versión actual.

En el commit `bea677a` de `dev`, pasaron [Cucumber y Cypress](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/actions/runs/39690660425), frontend y validación de workflows. Backend y vuelos pasaron build y tests, pero fallaron al consultar el Quality Gate: Sonar respondió que la organización no permite acceder a datos de ramas secundarias. Ambos proyectos tienen `main` como rama principal. Hay que resolver esa restricción; todavía no podemos afirmar que la versión actual pasó el Quality Gate.

Queda pendiente comprobar una ejecución real del deploy manual con los workflows reutilizados. No desplegamos a Azure durante estos cambios.

Para exigir todos los controles antes de un merge, también falta revisar los filtros por carpeta, distinguir los nombres de los jobs `build` y configurar los checks obligatorios en `dev` y `main`. Esa configuración requiere un administrador del repositorio. El YAML bloquea el deploy cuando fallan sus dependencias, pero no activa por sí solo la protección del merge.
