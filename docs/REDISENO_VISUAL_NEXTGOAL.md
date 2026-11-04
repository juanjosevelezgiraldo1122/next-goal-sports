# Rediseño visual de NextGoal

## Entendimiento y supuestos

NextGoal es una aplicación Android de fútbol en español, hecha con Jetpack Compose y tema oscuro. El rediseño cubre splash, inicio, partidos y rango de fechas, ligas y tabla, detalle de partido, búsqueda de equipos y jugadores, noticias, perfil local y la navegación inferior. Se conserva la lógica actual, el estado de pantalla, el orden de destinos, los proveedores y las rutas. Solo se ajustan tokens, composición y presentación de estados.

Se considera a la API actual como fuente de verdad. Cuando una respuesta no incluya marcador, minuto, estadística, imagen, plantilla o zona, no se sintetiza ese dato. Se muestra `—`, un contenedor neutro o un aviso explicativo. Los escudos de la API mantienen su forma y proporción.

## 1. Diagnóstico visual

La versión previa era legible, aunque varias superficies tenían casi el mismo tono y radio; eso hacía que títulos, controles y tarjetas compitieran por atención. El azul brillante se repetía en demasiados elementos y restaba valor a las señales semánticas del directo. Algunas etiquetas de partidos y estadísticas usaban tamaños de 9–11 sp; los metadatos de competiciones se recortaban de forma brusca. Las tarjetas no tenían una gramática común y la tabla teñía las primeras cuatro posiciones sin que el modelo de datos identificara explícitamente una zona deportiva. Los estados de carga y aviso parecían mensajes técnicos planos.

El sistema actualizado organiza la atención en tres niveles: marcador y titular, acciones y navegación, luego metadatos. Los fondos tienen más profundidad, las tarjetas comparten una forma y borde consistentes y el color vivo queda reservado a acciones o señales con significado.

## 2. Dirección creativa: Noche de estadio

Una interfaz de transmisión nocturna: tinta azul profunda como lienzo, lima como foco y acción, cian como señal secundaria y coral para indicar el partido en juego. El tono es seguro, enérgico y editorial. La lima hace que la selección se entienda rápidamente; el coral conserva el código visual reconocible del directo. La luz se usa con moderación para que escudos, nombres y marcadores sigan siendo el contenido principal.

## 3. Sistema de diseño

### Paleta

| Token | Hex | Función |
|---|---|---|
| Fondo profundo | `#080D12` | Fondo de aplicación |
| Superficie | `#101925` | Tarjetas y contenedores |
| Superficie elevada | `#182536` | Hero, selección y capas interactivas |
| Elevación adicional | `#202F40` | Capas de énfasis y skeleton |
| Texto principal | `#F4F7F8` | Títulos, clubes y marcadores |
| Texto secundario | `#A4B1BF` | Ayuda y metadatos |
| Texto terciario | `#8795A5` | Metadato de baja prioridad |
| Borde | `#2A394A` | División de superficies |
| Lima NextGoal | `#C7F36B` | Acción primaria y selección |
| Cian | `#70CDE5` | Acento informativo secundario |
| Coral en vivo | `#FF6670` | En directo y errores de red |
| Éxito/ventaja | `#70D8A6` | Resultado positivo publicado |
| Atención/empate | `#F4C96A` | Advertencia y estado neutral destacado |
| Derrota/error | `#FF8290` | Resultado negativo y error |
| Cancha | `#17362B` | Gráficos del campo |

La aplicación usa un esquema oscuro con texto y acentos de contraste alto. En botones lima se usa texto oscuro. Los estados siempre se nombran; no dependen únicamente de un color. Los degradados están limitados a la tarjeta de partido destacado.

### Tipografía y cifras

La familia es Sans Serif del sistema Android, para conservar buena lectura y escalado del usuario. Se usa ExtraBold para el marcador principal, Bold para títulos y SemiBold para controles. La escala es: titulares 30/36, 24/30 y 21/27 sp; títulos 19/25, 16/22 y 14/20 sp; cuerpo 16/24, 14/21 y 12/18 sp; etiquetas 12/17, 12/16 y 11/15 sp. El marcador usa `tnum` para alinear cifras y `—` cuando no existe dato.

### Espaciado, formas y profundidad

Escala de 4 dp: 4, 8, 12, 16, 24, 32 y 40 dp. Márgenes base de 16 dp; separaciones entre tarjetas de 8–12 dp y separaciones editoriales de 20–24 dp. Radios: 8 dp para elementos pequeños, 12 dp para controles, 18 dp para tarjetas, 24 dp para paneles destacados y 30 dp en contenedores amplios. Borde de 1 dp; elevación discreta para el partido destacado. Se prefiere separar con superficie y espaciado, no con sombras duras.

### Iconografía y escudos

Se conserva Material Icons y se mantiene una misma familia de iconos. Los iconos funcionales tienen descripción para TalkBack. Cada escudo se ajusta con `ContentScale.Fit` dentro de una base clara, dejando margen para emblemas circulares, altos, anchos o transparentes. Sin URL se muestra el contenedor neutro vacío; nunca se inventa un escudo.

## 4. Biblioteca de componentes y estados

- **Partido en vivo destacado:** tarjeta de 24 dp con competición, equipos, escudos, marcador tabular y badge coral `EN VIVO`; solo se muestra el minuto si viene en el dato. Sede únicamente cuando la API la entrega.
- **Fila de partido:** tarjeta de 18 dp con competición y estado arriba, clubes con sus escudos, marcador o hora publicados. Los estados dicen `EN VIVO`, `PAUSADO`, `FINAL`, `APLAZADO` o la hora programada.
- **Badge EN VIVO:** punto coral pulsante y texto. La etiqueta permanece visible si se reduce o desactiva la animación.
- **Chips de liga y pestañas:** selección lima con texto oscuro; alternativa en superficie profunda con borde. Objetivos táctiles de al menos 48 dp donde el componente permite definirlos.
- **Tabla:** posiciones y valores alineados; cifras tabulares. El líder puede destacarse. No se colorean zonas de clasificación o descenso porque la fuente/modelo actual no las identifica; se explica esta limitación en un banner.
- **Tarjetas estadísticas:** valor real, `—` cuando falta; título y fuente/competición como contexto. No se convierten valores nulos en ceros.
- **Fila de jugador y ficha:** iniciales o icono neutral si el proveedor no incluye foto; nombre, posición, equipo, partidos recientes y estadísticas solo cuando están disponibles. Escudos reales preservados.
- **Ficha de equipo:** escudo, nombre, estadísticas publicadas, siguiente partido y plantilla recibida. Los apartados ausentes muestran un estado honesto.
- **Banner informativo:** superficie elevada, icono informativo y texto explicativo. Los avisos del plan gratuito siguen visibles, pero ya no parecen errores.
- **Carga:** skeletons con brillo sutil y sin texto o cifras inventadas. Los listados usan placeholders de superficie; el detalle mantiene el contexto de carga.
- **Vacío:** explicación concreta, por ejemplo que no hay partidos publicados para ese periodo.
- **Error:** tarjeta con icono, mensaje de origen y acción `Reintentar` si la pantalla ya dispone de callback. No se presenta como fallo visual cuando la fuente simplemente no tiene cobertura.
- **Barra inferior:** conserva únicamente Inicio, Partidos, Ligas, Noticias y Perfil; selección lima, etiquetas visibles y mismo comportamiento de navegación.

Estados presionados usan el ripple de Material. El estado seleccionado mantiene un fondo propio además del color. El estado de carga no muestra resultados de muestra. Las imágenes de noticias usan la URL recibida; si no hay imagen, aparece un estado neutro con icono editorial.

## 5. Diseño pantalla por pantalla

### Inicio — prioridad principal

Cabecera de marca y acciones, campo visual de búsqueda, estado del directo, partido destacado, ligas destacadas, próximos partidos y últimos resultados. El marcador en vivo recibe la tarjeta de mayor contraste y tamaño. Sin partido en vivo aparece un banner discreto, seguido de agenda y resultados. Una sola columna vertical permite recorrer la portada con scroll y conserva las acciones importantes arriba.

### Partidos

Cabecera con regreso, actualización y rango de fecha. Controles anterior/siguiente cambian la ventana temporal existente; pestañas conservan `Todos`, `En vivo`, `Próximos` y `Finalizados`. Las tarjetas son compartidas con Inicio. El vacío incluye el periodo y el filtro que ya seleccionó la persona. Lista desplazable bajo los filtros; no se altera el cálculo de rangos ni el callback de detalle.

### Ligas y tabla

Cabecera, título descriptivo, chips de competiciones desplazables y tabla de una sola superficie. El nombre del club y su emblema conservan espacio; números se alinean por columna. En teléfonos estrechos el listado se desplaza verticalmente y no se comprimen datos. El banner informa que la API actual no incluye zonas de clasificación/descenso.

### Detalle de partido

Regreso visible; panel destacado con fase, jornada, equipos y escudos; marcador al centro; pestañas de estadísticas, alineaciones y cronología. Las secciones sin cobertura mantienen los avisos de transparencia. Los sucesos muestran el minuto recibido y su tipo. El cambio entre pestañas usa un crossfade corto, sin tocar rutas ni estado de navegación.

### Búsqueda

Regreso, título y campo de búsqueda. Selector de competición, equipos y jugadores en filas diferenciadas, y fichas de equipo/jugador con próximos partidos y estadísticas reales. El estado de búsqueda mantiene los resultados y callback actuales. Valores que no llegan de la API se muestran como raya o aviso de plan.

### Noticias

Cabecera, titular destacado con imagen real de la fuente cuando existe y lista de artículos. Si no hay imagen se usa un icono editorial neutro. Categoría, hora y titular mantienen jerarquía. No se sustituye una foto ausente por una fotografía inventada.

### Perfil

Título y pestañas actuales de cuenta/plantilla. La tarjeta de cuenta local conserva la lógica de inicio y registro, sesiones y almacenamiento en el dispositivo; sus datos se ordenan como identidad, correo y acción. La plantilla usa filas táctiles con iconografía neutral y abre la ficha existente. Los avisos de disponibilidad se mantienen.

### Splash

Logo actual centrado, descriptor breve en español y animación de entrada ya existente con escala visual más controlada. Se conserva el temporizador y el destino de salida.

### Barra inferior

Los cinco destinos actuales permanecen en el mismo orden y con la misma acción. Selección lima con indicador propio; otros destinos con texto legible. La barra se mantiene abajo y la app en orientación vertical.

### Variación por tamaño

Los márgenes laterales parten de 16 dp. Tarjetas de Inicio y partido se expanden al ancho disponible; nombres largos se recortan con elipsis, nunca reemplazan una acción. En pantallas grandes se puede limitar el ancho del contenido a una columna cómoda; en pantallas pequeñas listas largas conservan scroll. Se respeta la escala de fuente Android.

## 6. Motion

El punto de directo pulsa entre 0.48 y 1 de opacidad en 900 ms; es una respiración, no un destello. El skeleton recorre el ancho con un brillo cada 1.5 s. Un marcador que cambia con una nueva respuesta usa crossfade de 180 ms; no se anima cuando el valor no cambia. Pestañas del detalle cruzan en 180 ms. Los controles conservan respuesta táctil inmediata. Se mantienen las transiciones y el tiempo de salida del splash existente. No se introduce trabajo en hilos ni animación de gráficos pesados.

## 7. Accesibilidad

La paleta está diseñada para superar WCAG AA en texto normal (4.5:1) y texto grande/iconos esenciales (3:1); las combinaciones finales deben verificarse en emulador y con brillo bajo. Se usan etiquetas visibles, contraste alto y estado por texto/icono además de color. Los escudos tienen descripción de lector de pantalla. Botones, chips, tabs y controles deben ofrecer objetivos de 48 dp, TalkBack, orden de foco y tamaño de fuente del sistema. Los marcadores se leen como local/visitante y conservan cifras tabulares. Las animaciones son cortas y respetan la escala de animación del sistema.

## 8. Guía de implementación segura

Archivos visuales a revisar en orden:

1. `ui/theme/Color.kt`, `Type.kt`, `Theme.kt`: colores, escala tipográfica, formas y esquema.
2. `ui/components/CommonComponents.kt`, `MatchCard.kt`, `BottomNavBar.kt`: estados reutilizados, escudos, tarjetas y navegación visual.
3. `ui/home/HomeScreen.kt`: jerarquía de portada y directo.
4. `ui/matches/MatchesScreen.kt` y `ui/leagues/LeaguesScreen.kt`: filtros, rango, tabla y vacíos.
5. `ui/matchdetail/MatchDetailScreen.kt` y `ui/search/SearchScreen.kt`: fichas y estados de cobertura.
6. `ui/news/NewsScreen.kt` y `ui/profile/ProfileScreen.kt`: editorial y cuenta/plantilla.
7. `ui/splash/SplashScreen.kt`: cierre de identidad y movimiento.

Los ViewModels, `data/repository`, `data/remote`, modelos, `ui/navigation` y configuración de red permanecen fuera del rediseño. Cada fase se compila antes de la siguiente; los cambios se limitan a la capa Compose.

## 9. Lista de verificación final

- [ ] La barra conserva exactamente cinco destinos y la orientación sigue vertical.
- [ ] Inicio conserva directo, competiciones, próximos partidos y resultados recientes.
- [ ] Los filtros y rango de Partidos siguen cargando previos, próximos y finalizados.
- [ ] Regreso, refresh, detalle y selección de pestañas conservan callbacks actuales.
- [ ] Búsqueda mantiene equipos, goleadores, fichas y competición seleccionada.
- [ ] Cuenta local, registro, inicio de sesión, plantilla y perfiles conservan persistencia/estado.
- [ ] Ningún marcador/estadística ausente se muestra como cero.
- [ ] La tabla no sugiere zonas de ascenso o descenso sin datos.
- [ ] Las imágenes de noticias provienen solo de sus URLs; escudos conservan escala y proporción.
- [ ] Avisos del plan gratuito y estados sin datos permanecen visibles.
- [ ] Errores permiten reintento donde ya existe la acción; carga no presenta datos ficticios.
- [ ] Se revisa con texto ampliado, TalkBack, teléfono estrecho y tablet.
- [ ] `:app:assembleDebug` termina correctamente y se verifica instalación en emulador.
- [ ] El diff no contiene cambios en repositorios, ViewModels, API, modelos o rutas.