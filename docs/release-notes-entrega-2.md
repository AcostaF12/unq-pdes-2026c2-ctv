# Release notes - Entrega 2

Estas notas resumen los cambios de pruebas y CI para el segundo checkpoint. CI es la ejecución automática de controles cuando subimos código a GitHub. Los cambios están integrados en `dev` mediante el [PR #13](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/pull/13).

## Qué agregamos

Conectamos las pruebas de aceptación y de interfaz al CI, agregamos revisión de estilo en Kotlin y reforzamos los controles previos al deploy manual. También validamos los archivos que definen el propio CI.

Backend y vuelos ya tenían tests con JUnit y reportes de cobertura con JaCoCo. Agregamos ktlint a ambos módulos y formateamos el código existente en commits separados. Ahora el build compila, ejecuta tests y revisa el estilo. CI no corrige el formato automáticamente: si encuentra un problema, falla.

El frontend mantiene ESLint, Vitest y el build de Vite. Los nuevos controles no reemplazan estas suites ni garantizan por sí solos una cobertura completa.

## Qué probamos

### Aceptación con Cucumber

Los 11 escenarios llaman a las APIs reales y comprueban estas reglas:

- Un comprador puede iniciar sesión; una agencia puede publicar un paquete y un comprador no.
- Las búsquedas respetan origen, destino y filtros avanzados.
- Una compra aparece en el historial, reduce los asientos de ambos vuelos y conserva el precio aunque la agencia cambie el paquete.
- Si no hay lugar en el regreso, se recupera el asiento reservado en la ida y no queda una compra.
- No se puede reseñar sin compra previa ni repetir la reseña del mismo comprador sobre un paquete.

Ajustamos los tests a las respuestas paginadas de la API y corregimos los perfiles de ejecución de Cucumber. En CI, el resultado se imprime en consola.

### Interfaz con Cypress

Los cinco tests recorren la aplicación desde el navegador:

1. Login, búsqueda por nombre/origen/destino y consulta del detalle.
2. Contraseña incorrecta y reintento exitoso.
3. Restricción del formulario de hoteles y su API para un comprador.
4. Creación, edición y eliminación de un hotel por un administrador.
5. Compra e historial con viaje y precio conservados al recargar.

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

El deploy manual reutiliza los workflows de pruebas. Su secuencia es:

1. Ejecutar build, tests y lint de los tres módulos, Sonar, Cucumber, Cypress y validación de workflows.
2. Si todo pasa, construir y publicar las tres imágenes con el identificador del commit.
3. Prender la VM, descargar esas imágenes y levantar la aplicación sin compilar en Azure.
4. Comprobar la salud del backend.

Si un control falla, no se publica ni se despliega. Las imágenes de pruebas y de publicación corresponden al mismo código, aunque no son exactamente los mismos archivos binarios. Si falla la descarga, el script se detiene antes de levantar la aplicación. La acción `stop` sigue independiente.

## Verificación y pendientes

Durante la implementación pasaron localmente 277 tests del backend, 59 de vuelos, los 11 escenarios de Cucumber y los cinco tests de Cypress. También verificamos builds y lint.

En GitHub confirmamos builds y Sonar exitosos para ambos servicios, incluida la espera del Quality Gate, y una ejecución exitosa de la validación de workflows. La evidencia está en los [checks del PR #13](https://github.com/AcostaF12/unq-pdes-2026c2-ctv/pull/13/checks).

Queda pendiente comprobar una ejecución real del deploy manual con los workflows reutilizados. No desplegamos a Azure durante estos cambios.

Para exigir todos los controles antes de un merge, también falta revisar los filtros por carpeta, distinguir los nombres de los jobs `build` y configurar los checks obligatorios en `dev` y `main`. Esa configuración requiere un administrador del repositorio. El YAML bloquea el deploy cuando fallan sus dependencias, pero no activa por sí solo la protección del merge.
