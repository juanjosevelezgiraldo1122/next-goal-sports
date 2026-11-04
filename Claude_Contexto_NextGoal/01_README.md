# NextGoal — App de fútbol en Jetpack Compose

Proyecto de Android Studio listo para abrir, 100% Kotlin + Jetpack Compose,
que replica el diseño de tus mockups (splash, inicio, partidos en vivo,
detalle de partido, ligas/tabla de posiciones, noticias y perfil de jugador).

## 1. Cómo abrir el proyecto

1. Abre Android Studio (Iguana o más reciente) → **Open** → selecciona la carpeta `NextGoalApp`.
2. Deja que Gradle sincronice (usa Gradle 8.7 / AGP 8.5.2 / Kotlin 1.9.24, se descargan solos).
3. Copia `local.properties.example` a `local.properties` (en la raíz del proyecto) y:
   - Deja/ajusta la línea `sdk.dir=...` apuntando a tu Android SDK (Android Studio la agrega sola normalmente).
   - Pega tu API key gratuita (paso 2 abajo) en `FOOTBALL_DATA_API_KEY=`.
4. Ejecuta la app en un emulador o dispositivo con Android 8.0 (API 26) o superior.

## 2. Datos en vivo REALES — obtén tu API key gratis (2 minutos)

La app usa **football-data.org**, una API REST real y gratuita, para:

- Marcadores en vivo, partidos de hoy y próximos, en todas las competiciones que cubre el plan gratuito.
- Tabla de posiciones real y actualizada de cada liga.
- **Escudos reales de los equipos** (se cargan directamente desde las URLs que devuelve la propia API con Coil — no hay imágenes falsas ni inventadas).
- Goles, tarjetas y cambios reales en la línea de tiempo del detalle del partido (con minuto y nombre real del jugador).
- Plantillas de equipos y perfiles de jugadores reales (nombre, posición, nacionalidad, equipo, e historial de partidos recientes).

Pasos:
1. Regístrate gratis en https://www.football-data.org/client/register
2. Copia el token que te dan por correo.
3. Pégalo en `local.properties`:
   ```
   FOOTBALL_DATA_API_KEY=tu_token_aqui
   ```
4. Vuelve a compilar. Ya tienes datos reales y en vivo.

El plan gratuito tiene un límite de ~10 peticiones/minuto; la app ya maneja ese
error mostrando un mensaje claro en pantalla en vez de fallar en silencio.

## 3. Cosas importantes que debes saber (para que no haya sorpresas)

- **Liga MX no está en el plan gratuito de football-data.org.** Por eso el
  selector de "Ligas destacadas" muestra Champions League, La Liga, Premier
  League, Bundesliga, Serie A y Ligue 1 — todas con datos reales y en vivo.
  Si más adelante tienes acceso a otra API que sí cubra Liga MX, solo hay que
  añadir su código en `data/repository/FootballRepository.kt` (lista
  `FEATURED_COMPETITIONS`).
- **Estadísticas del partido (posesión %, remates, tiros de esquina) y
  alineaciones/formaciones no están incluidas en el plan gratuito.** En vez de
  inventar esos números, esas pestañas muestran un aviso honesto explicando
  esto y quedan listas para conectarse a un proveedor de estadísticas más
  completo (por ejemplo API-Football en RapidAPI) cuando quieras — el punto de
  conexión está marcado con comentarios `TODO`-style en
  `MatchDetailScreen.kt` y `Mappers.kt` (`statsAvailable` / `lineupsAvailable`).
- **Goles/asistencias/temporada de un jugador** tampoco vienen en el plan
  gratuito a nivel de jugador. El perfil de jugador sí es 100% real en cuanto
  a nombre, posición, nacionalidad, equipo actual e historial de partidos
  recientes (con marcador real). Se indica claramente en la propia pantalla
  cuándo un dato no está disponible, en vez de mostrar un número falso.
- **Noticias**: no existe una API de noticias de fútbol gratuita y sin
  registro que sea confiable, así que la pestaña de Noticias usa artículos de
  ejemplo claramente redactados por mí para completar el diseño. Está
  aislado en `data/repository/NewsRepository.kt` — cambia esa clase para
  conectarla a NewsAPI.org, GNews, o un RSS real cuando quieras, sin tocar la UI.
- El **logo y la insignia de carga** son los archivos PNG reales que me
  compartiste (los mismos de tus capturas), integrados directamente en
  `res/drawable/` y usados en la pantalla de Splash y como ícono de la app.

## 4. Estructura del proyecto

```
NextGoalApp/
  app/src/main/java/com/nextgoal/app/
    data/
      remote/          -> Retrofit (FootballApiService, RetrofitInstance) + DTOs reales de la API
      repository/      -> FootballRepository (datos reales), NewsRepository (ejemplo), Mappers
      model/            -> Modelos de UI (MatchUi, StandingRowUi, PlayerUi, etc.)
    di/                 -> AppGraph (inyección de dependencias manual, sin Hilt)
    ui/
      theme/            -> Colores/tipografía oscuros del diseño NextGoal
      navigation/       -> NavGraph + rutas + bottom bar
      components/       -> Tarjetas de partido, escudos (Coil), badges de EN VIVO, estados de carga/error
      splash/           -> Pantalla de bienvenida con tu logo real
      home/             -> Inicio: partido en vivo destacado, ligas, próximos partidos
      matches/          -> "Ver todos" los partidos
      leagues/          -> Tabla de posiciones real por liga
      matchdetail/      -> Marcador, estadísticas, alineaciones, cronología real
      news/             -> Noticias
      profile/          -> Plantilla real + perfil real de jugador
  README.md
  local.properties.example
```

## 5. Arquitectura

MVVM simple: `ViewModel` + `StateFlow` + una factoría manual (`ViewModelFactories.kt`)
en vez de Hilt, para que el proyecto compile sin configuración adicional de
procesadores de anotaciones. Retrofit + OkHttp + Gson para la red, Coil para
las imágenes (escudos reales), Navigation-Compose para la navegación.
