# NextGoal

Aplicación Android de resultados de fútbol hecha íntegramente con Kotlin y Jetpack Compose.

## Abrir en Android Studio

1. Abre esta carpeta desde Android Studio.
2. Espera a que finalice la sincronización de Gradle.
3. Ejecuta la configuración `app` en un emulador o dispositivo Android 8.0 o superior.

El proyecto está configurado para Gradle 8.9 y Android Gradle Plugin 8.7.3; Android Studio descargará el wrapper si todavía no está cacheado en el equipo.

La app incluye inicio con la identidad de NextGoal, partidos en vivo y próximos, detalle con estadísticas y eventos publicados por la API, tablas de ligas, noticias con enlace a la fuente original y un perfil con cuenta, foto y datos personales editables. Los escudos se cargan desde las URLs oficiales entregadas por la fuente de datos.

## Cuenta y perfil

Al abrir la app sin una sesión activa aparece el flujo de registro e inicio de sesión. La cuenta local conserva nombre, correo, teléfono, contraseña protegida mediante hash, foto seleccionada desde la galería, equipo favorito y preferencias.

Cambiar correo, teléfono o contraseña abre una verificación de seis dígitos antes de guardar. En esta copia para Android Studio el gateway de verificación está preparado como punto de integración y muestra el código únicamente en compilaciones `debug`; el envío real por correo o SMS requiere conectar Firebase Authentication o un backend con proveedor de correo/SMS. No se presenta ese código local como un envío real.

## Datos en tiempo real

La app consulta la jornada actual, los partidos en vivo y las tablas mediante `football-data.org`. Los partidos se solicitan para Liga de Campeones, Premier League, La Liga, Bundesliga, Serie A, Ligue 1, Eredivisie, Primeira Liga, Championship, Brasileirão y Liga MX, y se agrupan en la pantalla por su competición real. La clave se guarda en `local.properties`, que está excluido del control de versiones:

```properties
FOOTBALL_DATA_TOKEN=tu_clave_de_football_data
```

Con la clave, el botón de actualizar y el refresco automático consultan partidos, escudos, tablas, estadísticas y eventos disponibles. La pantalla `Partidos` ofrece filtros `EN VIVO`, `HOY`, `MAÑANA`, `PRÓXIMOS` y `FINALIZADOS`, además de un filtro por competición. Los partidos en vivo se refrescan automáticamente cada 30 segundos; el proveedor puede no publicar estadísticas o ciertas competiciones en todos los planes, y en ese caso se muestra claramente que el dato no está disponible en lugar de inventarlo. Las noticias se leen de feeds RSS y cada noticia enlaza con su artículo original.

## Idioma y hora

La interfaz está en español y las fechas y horas de partidos, noticias, actualizaciones y fichas se muestran con la zona horaria fija `America/Bogota` y el formato regional `es-CO`. La zona horaria del computador o del emulador no cambia la hora que muestra NextGoal.

## Buscador mundial

La lupa permite buscar jugadores y equipos por nombre. Los resultados y las fichas se consultan en vivo mediante TheSportsDB, con foto del jugador, equipo actual, posición, nacionalidad, biografía, logo, liga, país, estadio, capacidad, año de fundación, descripción y sitio oficial cuando la fuente lo publica. La búsqueda admite equipos de distintas ligas y países; la disponibilidad exacta de campos depende de la cobertura de la fuente.

Cada ficha también consulta el calendario del equipo actual mediante sus identificadores reales. Muestra el próximo partido, el partido anterior, marcador, estado, competición, estadio y las estadísticas detalladas que la fuente publique. En jugadores se utiliza su equipo actual; si la fuente no publica un identificador o estadísticas individuales, la app lo indica y no inventa valores.

## Estructura

- `app/src/main/java/com/nextgoal/app/NextGoalApp.kt`: navegación y pantallas Compose.
- `app/src/main/java/com/nextgoal/app/FootballRepository.kt`: consulta y conversión de datos en vivo.
- `app/src/main/java/com/nextgoal/app/NextGoalViewModel.kt`: estado, actualización y perfil persistente.
- `app/src/main/res/drawable-nodpi/`: logo y distintivo de carga enviados para NextGoal.
