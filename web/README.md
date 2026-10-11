# TaskPie Desktop (Web / PWA)

Versión de escritorio de **TaskPie** construida con React, TypeScript, Vite y CSS moderno nativo, con soporte completo de PWA instalable y paridad funcional 100% con la aplicación de Android.

## Características Principales

- **Gestión Visual del Tiempo**: Bloques de actividades divididos en gráficos circulares (*pie charts* / *donut charts*) interactivos con códigos de color e instrucciones por etapa.
- **Rutinas Secuenciales**: Encadenamiento de actividades con soporte de avance automático o pausa con confirmación manual entre bloques.
- **Proyectos Contenedores**: Agrupación de actividades y rutinas bajo metas temáticas.
- **Temporizador de Alta Precisión**:
  - Motor desacoplado impulsado por **Web Worker** para evitar ralentizaciones o congelamientos cuando la pestaña está en segundo plano o minimizada.
  - Alertas acústicas sintetizadas con la **Web Audio API** (tonos de cambio de etapa y fanfarria triunfal).
  - Anuncios de voz en español mediante **Web Speech API** (*VoiceAnnouncementManager*: "Iniciando etapa", "Mitad del tiempo", "Último minuto", cuenta regresiva 5..1, "Sesión completada").
  - Notificaciones nativas de escritorio del sistema operativo.
  - Dock flotante inferior persistente y vista inmersiva a pantalla completa (*ActiveSessionModal*).
  - Celebración con confeti al finalizar la sesión y registro de métricas.
- **Persistencia Local y Sincronización**:
  - Almacenamiento local mediante **IndexedDB** (funciona 100% offline).
  - Precarga de plantillas integradas (*BuiltInTemplates*: Sprint de escritura, anti-inercia, descanso activo, estudio, tareas del hogar).
  - Exportación e importación de copias de seguridad en formato JSON compatible con el esquema de Room de Android.
- **Atajos de Teclado**:
  - `Espacio`: Pausar o reanudar el temporizador.
  - `S`: Saltar inmediatamente la etapa actual.
  - `N`: Avanzar al siguiente bloque cuando hay pausa manual.
  - `M`: Alternar voz (TTS activado/silenciado).
  - `F`: Alternar pantalla completa / modo inmersivo.
  - `Esc`: Minimizar sesión al dock inferior / cerrar ventanas modales.

## Ejecución Local

```bash
cd web
npm install
npm run dev
```

Abre en tu navegador: [http://localhost:5173/](http://localhost:5173/)

## Compilación de Producción

```bash
npm run build
```
