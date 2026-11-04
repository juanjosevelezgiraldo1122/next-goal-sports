# ⚽ NextGoal

### El fútbol se vive minuto a minuto.

NextGoal es una app Android para quienes quieren tener la jornada a mano: seguir partidos, consultar resultados y tablas, y descubrir equipos y jugadores desde un mismo lugar. Su interfaz oscura toma inspiración de las noches de estadio, con acentos vivos y una lectura clara para que el marcador sea siempre protagonista.

> **Un proyecto hecho para fans del fútbol.** Abre la app, elige un partido y sigue cada historia hasta el pitazo final.

## Lo que puedes encontrar

- **Inicio:** partidos en vivo cuando están disponibles, próximos encuentros, últimos resultados y ligas destacadas.
- **Partidos:** calendario con filtros para ver todos, en vivo, próximos o finalizados, además de navegar por rangos de fechas.
- **Ligas:** competiciones y tablas de posiciones con información recibida del proveedor.
- **Detalle del partido:** marcador y cronología de eventos cuando la API ofrece esos datos.
- **Búsqueda:** consulta equipos y jugadores, y revisa la información disponible en sus fichas.
- **Noticias:** una sección de lectura con artículos de muestra; todavía no es un feed periodístico en tiempo real.
- **Perfil:** espacio local de cuenta y acceso a plantillas y perfiles disponibles.

## Datos claros, fútbol real

Los partidos, resultados, tablas, escudos y parte de la información de equipos provienen de [football-data.org](https://www.football-data.org/). La cobertura depende de la competición y del plan de la API. Algunas estadísticas —como posesión, alineaciones o goles de temporada por jugador— pueden no estar disponibles; NextGoal las presenta como no disponibles en vez de rellenarlas con cifras inventadas.

La pestaña de noticias utiliza contenido de muestra incluido en el proyecto. No representa noticias verificadas ni se actualiza desde un medio externo.

## Pon NextGoal en marcha

1. Abre Android Studio y selecciona **Open**.
2. Elige la carpeta raíz `nexgoal-version-final`, donde está `settings.gradle.kts`.
3. Espera a que termine la sincronización de Gradle y selecciona un emulador o dispositivo.
4. Pulsa **Run ▶**.

Para cargar datos de fútbol, configura tu token de football-data.org en el archivo `local.properties`, en la raíz del proyecto:

```properties
FOOTBALL_DATA_API_KEY=tu_token
```

Android Studio suele completar `sdk.dir` automáticamente en ese archivo. Puedes usar `local.properties.example` como referencia. **No compartas ni publiques `local.properties`: contiene configuración local y puede incluir una clave privada.**

La app requiere Android 8.0 (API 26) o posterior. Para ver datos en línea, el emulador o teléfono debe tener conexión a internet y acceso a la API.

## Un vistazo a la tecnología

- Kotlin y Jetpack Compose
- Material 3 y Navigation Compose
- Retrofit y OkHttp para las peticiones de red
- Coil para cargar escudos e imágenes disponibles
- MVVM con `ViewModel` y `StateFlow`

La app mantiene separadas las pantallas, los componentes visuales y el acceso a datos para que sea más sencillo seguir mejorando NextGoal.

## Para seguir construyendo

NextGoal es un proyecto en evolución. Algunas secciones muestran información limitada por el proveedor y la fuente de noticias aún es de muestra. La idea es ampliar la cobertura y conectar nuevas fuentes con transparencia, cuidando que cada dato que aparece en pantalla sea honesto.

**Gracias por vivir el fútbol con NextGoal.** 🏟️
