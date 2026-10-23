# Backlog: creación en Android

## Alcance

Mejorar primero la creación de actividades, rutinas y proyectos en Android. La app ya tiene editores separados, resumen de duración y controles para ordenar etapas, bloques y rutinas. Las oportunidades principales están en hacer más fácil empezar, encontrar elementos reutilizables y entender el plan antes de guardarlo.

La actualización de la web queda para una etapa posterior, una vez que el flujo de Android esté definido y probado.

## Prioridad alta

### [x] AND-CRE-01 — Crear una actividad desde una plantilla

**Situación:** existen plantillas integradas en `BuiltInTemplates.kt`, pero el editor de actividad ofrece guardar como plantilla y no presenta esas plantillas como punto de partida.

**Trabajo:** al crear una actividad, ofrecer “Desde cero” y “Usar plantilla”. Mostrar nombre, propósito y duración estimada de cada plantilla; al elegir una, cargar una copia editable de sus etapas.

**Criterios de aceptación:**

- Se puede comenzar desde cero o desde una plantilla integrada.
- La selección carga título, descripción, categoría y etapas para editarlos.
- Editar la copia nunca modifica la plantilla original.
- La lista distingue las plantillas integradas de las creadas por la persona.

### [x] AND-CRE-02 — Guiar la creación sin quitar el acceso rápido

**Trabajo:** organizar los tres editores en pasos claros: datos básicos, secuencia o etapas, y revisión. Mantener visibles el progreso y el tiempo total. En edición, abrir directamente el contenido principal.

**Criterios de aceptación:**

- Actividad: datos básicos → etapas → resumen.
- Rutina: datos básicos → actividades y bloques → resumen.
- Proyecto: datos básicos → rutinas y actividades vinculadas → resumen.
- Volver al paso anterior conserva lo escrito.
- Guardar lleva a la parte pendiente si faltan datos y muestra el error junto al campo o sección correspondiente.
- En Android, guardar queda accesible al avanzar por formularios largos.

### [x] AND-CRE-03 — Encontrar y añadir actividades y rutinas con menos pasos

**Trabajo:** mejorar los selectores usados al armar rutinas y proyectos. Incluir búsqueda y tarjetas informativas con duración y cantidad de etapas o actividades. Permitir crear una actividad sin abandonar la rutina y volver con ella ya añadida.

**Criterios de aceptación:**

- Los selectores permiten buscar por nombre.
- Cada resultado muestra duración y contenido resumido.
- Hay estados claros cuando no existen elementos o la búsqueda no encuentra resultados.
- Desde una rutina se puede crear una actividad nueva y añadirla a la secuencia al terminar.
- Si se cancela la actividad nueva, se vuelve al selector y la rutina conserva sus cambios.
- La selección de una rutina en un proyecto muestra su duración antes de añadirla.

## Prioridad media

### AND-CRE-04 — Revisar el plan antes de guardarlo

**Trabajo:** incorporar un resumen legible de la actividad, rutina o proyecto: elementos y orden, duración total y partes opcionales u omitidas.

**Criterios de aceptación:**

- El resumen refleja la duración de las etapas activas y las repeticiones de bloques.
- En proyectos, la duración refleja las etapas incluidas en las rutinas personalizadas.
- Desde el resumen se puede volver a editar cada sección.

### AND-CRE-05 — Aclarar actividades vinculadas dentro de un proyecto

**Trabajo:** diferenciar con texto breve las rutinas que forman la secuencia del proyecto de las actividades individuales vinculadas que se pueden iniciar por separado.

**Criterios de aceptación:**

- Los nombres de sección explican qué se ejecuta al lanzar la secuencia completa.
- Las actividades vinculadas indican que son accesos individuales y no se suman a la secuencia.
- El resumen del proyecto mantiene esta distinción.

### [x] AND-CRE-06 — Recuperar un borrador si se sale del editor

**Trabajo:** guardar automáticamente los datos no guardados del editor y recuperarlos al volver. Si no se implementa el guardado automático en la primera versión, advertir antes de descartar cambios.

**Criterios de aceptación:**

- El borrador se conserva al salir accidentalmente o recrearse la pantalla.
- Se puede descartar el borrador explícitamente después de guardar o al comenzar una creación nueva.
- No se mezclan datos de dos actividades, rutinas o proyectos distintos.

## Edición en Android

### [x] AND-EDT-01 — Duplicar etapas y bloques desde el editor

**Trabajo:** facilitar la reutilización de contenido dentro de una actividad o rutina con una acción de duplicado contextual.

**Criterios de aceptación:**

- Una etapa se duplica a continuación de la original con su contenido y configuración.
- Un bloque de rutina se duplica a continuación del original e incluye sus actividades, etapas, orden y repeticiones.
- La copia recibe identificadores nuevos, se edita independientemente y actualiza el resumen y la duración.
- La duplicación se conserva en el borrador automático.

### [x] AND-EDT-02 — Volver al mismo lugar de la biblioteca

**Trabajo:** restaurar el contexto de la lista al salir de un editor.

**Criterios de aceptación:**

- Se conservan pestaña, búsqueda, filtro y elemento seleccionado.
- Cada pestaña conserva su propia posición de desplazamiento.
- El contexto se restaura en diseños de teléfono y pantalla ancha.

### [x] AND-EDT-03 — Editar con historial, selección y revisión directa

**Trabajo:** agregar deshacer y rehacer en los tres editores, reordenamiento por arrastre, eliminación de varios elementos y accesos desde la revisión a su contenido.

**Criterios de aceptación:**

- Deshacer y rehacer cubren los cambios de datos del editor; la escritura se agrupa y el historial vive durante la sesión abierta.
- Las etapas, bloques, actividades de rutinas y rutinas de proyectos se pueden reordenar arrastrando.
- Una pulsación larga inicia selección múltiple; se pueden quitar elementos en lote y deshacer el cambio.
- Quitar actividades vinculadas solo elimina su vínculo al proyecto.
- Tocar un dato en la revisión abre el paso correcto y desplaza la lista al elemento cuando corresponde.
- El borrador conserva el estado editado, pero al retomarlo comienza un historial vacío.

## Etapa posterior: actualizar la web

### AND-CRE-07 — Llevar el flujo acordado a la web

Cuando los pasos y etiquetas de Android estén definidos, actualizar los editores web para compartir el mismo orden, nombres, plantillas, búsqueda y resumen. Mantener los controles adecuados a cada tamaño de pantalla.

**Criterios de aceptación:**

- Los mismos conceptos y pasos aparecen en Android y web.
- La información guardada sigue siendo compatible entre ambas versiones.
- Se verifica crear y editar los tres tipos de elementos en ambas plataformas.

## Orden sugerido

1. AND-CRE-01: plantillas de actividad.
2. AND-CRE-02: pasos y resumen.
3. AND-CRE-03: búsqueda y creación desde los selectores.
4. AND-CRE-04 y AND-CRE-05: revisión y claridad del proyecto.
5. AND-CRE-06: recuperación del borrador.
6. AND-EDT-01 y AND-EDT-02: duplicación y retorno al contexto.
7. AND-EDT-03: controles de edición.
8. AND-CRE-07: paridad web.
