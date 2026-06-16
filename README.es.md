<img align="right" src="images/petanqueturniermanager-logo-256px.png" alt="Logo" height="120">

# Gestor de Torneos de Petanca

[![CI](https://github.com/michaelmassee/Petanque-Turnier-Manager/actions/workflows/ci.yml/badge.svg)](https://github.com/michaelmassee/Petanque-Turnier-Manager/actions/workflows/ci.yml)
[![License: EUPL v1.2](https://img.shields.io/badge/License-EUPL_1.2-blue.svg)](https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12)
[![Wiki](https://img.shields.io/badge/Doku-Projekt_Wiki-green.svg)](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki)
[![Donar con PayPal](https://img.shields.io/badge/Donar-PayPal-blue.svg)](https://www.paypal.me/michaelmassee1)

*Read this in other languages: [🇩🇪 DE](README.md) | [🇬🇧 EN](README.en.md) | [🇫🇷 FR](README.fr.md) | [🇪🇸 ES](README.es.md) | [🇳🇱 NL](README.nl.md)*


---

> *Built with the precision of a software engineer and the passion of a pétanque player.*

## 🎯 Introducción
El **Gestor de Torneos de Petanca** (Pétanque-Turnier-Manager) es un potente software de código abierto para la gestión de torneos que se integra a la perfección como una extensión en **LibreOffice Calc**.

El software fue diseñado específicamente para hacer que la organización y ejecución de torneos de petanca y bochas sea lo más sencilla y eficiente posible. Dado que funciona completamente sin conexión y directamente en la hoja de cálculo, consume muy pocos recursos. Esto lo convierte en la solución ideal para su uso en la pista de petanca, incluso en portátiles antiguos que funcionan, por ejemplo, con un sistema Linux ligero.

**Ventajas de un vistazo:**
* **Independiente del sistema operativo:** Funciona de forma fiable en Linux, macOS y Windows.
* **Multilingüe:** La interfaz de usuario admite varios idiomas, incluidos alemán (DE), inglés (EN), francés (FR), español (ES) y holandés (NL).
* **Gratuito y de Código Abierto:** Sin costes de licencia, sin publicidad.
* **Eficiente en recursos:** Perfectamente adecuado para hardware antiguo.
* **Todo en un solo lugar:** No se necesita una base de datos externa; todo se calcula y almacena directamente en LibreOffice Calc.
* **Base de datos de jugadores integrada:** Gestiona jugadores y clubes de forma centralizada y pásalos directamente a la lista de inscripción mediante búsqueda/filtro – con importación/exportación y un visor web propio.
* **Servidor web integrado:** Muestra todos los datos del torneo en vivo en televisores, tabletas o móviles – directamente desde LibreOffice, sin software externo.
* **Página de bienvenida del torneo:** Página de bienvenida totalmente configurable para proyector o televisor con logotipo, imagen de fondo, animaciones, contadores en vivo y barra de estado – ideal para la mesa de inscripción el día del torneo.

Available in Languages: 🇩🇪 DE | 🇬🇧 EN | 🇫🇷 FR | 🇪🇸 ES | 🇳🇱 NL

---

## 🛠️ Personalización sin límites: ¡Haz que sea *tu* torneo!

El mayor punto de venta único de este gestor de torneos es su base: **LibreOffice Calc**. Debido a que todos los datos, tablas y clasificaciones se escriben directamente en hojas de cálculo regulares de Calc, no estás atrapado en una estructura de programa rígida.

> **💡 Control total con las herramientas de Calc:** ¡Puedes ampliar **cualquier** sistema de torneo generado de forma completamente libre y según tus propios deseos!

* **Torneos internacionales:** Gracias al soporte multilingüe integrado, estás perfectamente equipado para torneos con invitados internacionales.
* **Evaluaciones propias:** Usa BUSCARV, tablas dinámicas o tus propias fórmulas para extraer estadísticas internas de los equipos, promedios de puntos o clasificaciones especiales a partir de los datos del torneo.
* **Personalización visual:** Crea tus propios paneles, inserta logotipos de clubes, cambia el diseño para imprimir o diseña vistas de presentación para un proyector (por ejemplo, para mostrar la clasificación en vivo).
* **Macros personalizadas y API pública:** Si necesitas funciones avanzadas, puedes escribir tus propias macros de LibreOffice (Basic, Python, etc.) para interactuar con las tablas. → [Macros y Fórmulas – documentación completa de la API (en alemán)](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Makros-und-Formeln)

---

## 🏆 Sistemas de torneo compatibles

* **Supermêlée / Mêlée:** Ideal para torneos informales. Se sortean nuevos equipos y oponentes en cada ronda (o para todo el torneo).
* **Liga:** Para organizar una liga de club o regional con equipos fijos. El calendario de partidos se genera de antemano y determina quién juega contra quién y cuándo. La clasificación se calcula según las victorias, la diferencia de puntos y el enfrentamiento directo. Soporta exportación HTML de resultados y calendarios.
* **Sistema Suizo (Formule Suisse):** El sistema más justo para torneos con muchos participantes y tiempo limitado. Incluye cálculo automático de victorias, diferencia de puntos y puntos Buchholz (y Buchholz fino) para una clasificación exacta.
* **Todos contra todos (Round Robin):** El sistema clásico de liga en el que cada equipo se enfrenta a todos los demás equipos a lo largo del torneo.
* **Sistema de eliminación directa (Knockout):** Para rondas finales clásicas y torneos de eliminación directa pura. Una victoria significa avanzar, una derrota la eliminación inmediata. Los emparejamientos siguen el sistema cruzado (1.º vs. último, 2.º vs. penúltimo …), de modo que los mejores equipos se enfrentan solo tarde. Si el número de participantes no es una potencia de dos, se calcula automáticamente un «cadrage» (ronda preliminar).
* **Sistema de Maastricht:** Combina el Sistema Suizo con rondas finales de eliminación directa. Los equipos se emparejan según el algoritmo suizo en varias rondas preliminares (2–5). Después, los equipos se dividen en grupos de nivel (A, B, C, D) según su número de victorias – cada grupo disputa su propia final en formato knockout. Resultado: cuatro ganadores del torneo, una distribución justa y finales emocionantes.
* **Sistema Poule A/B:** Modo clásico con fase de grupos (Poules) según el principio de doble eliminación ligera, seguido de una división en Torneo A (cuadro principal) y Torneo B (consolación).
* **Sistema de KO en Cascada (Sistema ABCD Extendido):** Amplía el sistema clásico ABCD de eliminación directa con tantos niveles adicionales como se desee (E, F, G, H …). En lugar de ser eliminados temprano, los equipos perdedores descienden progresivamente hacia consolaciones de nivel inferior. Tras un número mínimo configurable de rondas, cada nivel pasa al formato de eliminación directa pura, con cadrage propio si es necesario. Adecuado para torneos medianos y grandes (desde 16 equipos).
* **Formule X:** Sistema moderno de rondas del pétanque francés – ideal para campos grandes y torneos con límite de tiempo. Todos los equipos juegan el mismo número de rondas, nadie queda eliminado. La clasificación se obtiene mediante una puntuación acumulada clara (bono de victoria + puntos propios + diferencia de puntos) – sin necesidad de Buchholz. La 1.ª ronda se sortea libremente; a partir de la 2.ª, los emparejamientos siguen la clasificación: 1.º vs. 2.º, 3.º vs. 4.º, etc.
* **Trip-Tête / Sistema Trio:** Formato por equipos de tres jugadores en el que cada encuentro consta de Triplette, Doublette y Tête-à-tête. Tras la Triplette conjunta, el equipo se reparte tácticamente: dos jugadores disputan la Doublette y un jugador disputa el Tête-à-tête. Adecuado para torneos compactos por equipos, variados y con una puntuación clara de los encuentros.

---

## 👥 Base de datos de jugadores integrada – mantén una vez, reutiliza siempre

En lugar de volver a teclear la lista de participantes en cada torneo, el Pétanque Tournament Manager incluye una **base de datos de jugadores integrada** dentro de la propia extensión. Jugadores, clubes y etiquetas se introducen una sola vez y quedan disponibles de inmediato para cada nuevo torneo.

* 🧑‍🤝‍🧑 **Gestión centralizada de jugadores y clubes** – nombre, apellido, club, número de licencia y etiquetas libres (p. ej. „Femenino", „Juvenil", „Club de origen").
* 🔍 **Búsqueda y filtros rápidos** – búsqueda a partir de 2 caracteres, combinable con filtros por club y etiqueta.
* ➕ **Traspaso directo a la lista de inscripción** – doble clic añade un jugador; se pueden agrupar varios jugadores como equipo y escribirlos juntos en la lista. Los jugadores ya inscritos pueden ocultarse opcionalmente para evitar duplicados.
* 📥 **Importación / Exportación** – formato CSV y un formato de archivo DB propio. Comparte el censo del club entre torneos o importa listas existentes.
* 🌐 **Visor web propio** – un frontend en navegador de solo lectura para la base de datos de jugadores, ideal para la mesa de inscripción o la oficina del torneo.

> **💡 Práctico para torneos de club:** Registra una sola vez a todos los socios activos – a partir del siguiente torneo basta con teclear las primeras letras del apellido y el jugador entra en la lista con su club y número de licencia.

---

## 🌐 Servidor web integrado – Visualización en vivo el día del torneo

El Pétanque Tournament Manager incluye un **servidor web completamente integrado** – directamente desde LibreOffice Calc, sin software externo ni conexión a internet.

Mientras se desarrolla el torneo, todas las hojas se pueden ver en un navegador en **televisores, portátiles, tabletas o móviles** – en tiempo real, actualizadas automáticamente:

* 📋 **Listas de participantes** – ¿quién juega?
* 🎯 **Ronda actual** – ¿qué partidas están en curso?
* 🏆 **Clasificaciones** – actualizadas en vivo tras cada ronda
* 📊 **Tablas personalizadas** – publica cualquier hoja de Calc

> **💡 Así de sencillo:** Inicia el servidor web desde el menú → abre la URL en un navegador → listo. Todos los dispositivos en la misma red Wi-Fi ven los datos del torneo en vivo.

**Detalles técnicos:**
* Interfaz React con **Server-Sent Events (SSE)** – actualizaciones instantáneas sin recargar la página
* Hasta **10 URLs simultáneas** configurables
* Zoom, centrado y encabezados/pies de página configurables por puerto
* Todos los sistemas de torneo compatibles: Supermêlée, Sistema suizo, Todos contra todos, K.-O., Sistema de Maastricht, Formule X, Trip-Tête

---

## 🎬 Página de bienvenida del torneo – la pantalla de bienvenida para el día del torneo

Además de las tablas de datos, el servidor web integrado incluye su propia **página de bienvenida del torneo**, que puedes mostrar al público en la mesa de inscripción, en el proyector o en el televisor del club. Se configura mediante su propio diálogo y no requiere software externo.

* 🖼️ **Logotipo y fondo:** Logotipo del club, imagen de fondo y pie de página propios, colores de fondo y texto a elección (selector de color nativo de LibreOffice).
* ✍️ **Descripción del torneo en varias líneas:** Nombre del torneo, eslogan y texto descriptivo con animación de fundido, deslizamiento o máquina de escribir en bucle infinito.
* 🔢 **Contadores en vivo:** «Inscritos» y «Activos» se cuentan en tarjetas animadas – directamente desde la lista de inscripción.
* 🧭 **Barra de estado:** Sistema de torneo actual, progreso y frases rotativas.
* 🔍 **Zoom 10–500 %:** El diseño se adapta a cualquier tamaño de pantalla; la imagen de fondo permanece sin escalar.

> **💡 Configuración rápida:** Abre el diálogo de configuración en el menú PétTurnMngr → elige un logotipo, introduce la descripción → inicia el servidor web → abre la URL en el proyector o televisor.

---

## 💻 Requisitos del sistema

* **LibreOffice:** versión 25.x (o superior)
* **Java (JRE/JDK):** versión 25 o superior
* **Sistema operativo:** Linux, Windows o macOS 11 (Big Sur) o superior

---

## ⚙️ Instalación y configuración

### Recomendado: instalación con el instalador

Descarga el paquete de instalación correspondiente desde las [GitHub Releases](https://github.com/michaelmassee/Petanque-Turnier-Manager/releases):

* **Linux:** [`PetanqueTurnierManager-Installer-<version>-linux.AppImage`](https://github.com/michaelmassee/Petanque-Turnier-Manager/releases)
* **Windows:** [`PetanqueTurnierManager-Installer-<version>-windows.exe`](https://github.com/michaelmassee/Petanque-Turnier-Manager/releases)
* **macOS (Apple Silicon, M1/M2/M3/…):** [`PetanqueTurnierManager-Installer-<version>-macos-arm64.dmg`](https://github.com/michaelmassee/Petanque-Turnier-Manager/releases)
* **macOS (Intel):** [`PetanqueTurnierManager-Installer-<version>-macos-intel.dmg`](https://github.com/michaelmassee/Petanque-Turnier-Manager/releases)

> **Nota (macOS):** Requiere macOS 11 (Big Sur) o superior. macOS 10.15 (Catalina) y anteriores no son compatibles (entorno de ejecución Java 25).

Inicia el instalador y sigue las instrucciones. El instalador configura la extensión para LibreOffice.

### Alternativa: instalación manual

#### Paso 1: Instalar Java
* **Temurin Adoptium JDK (LTS):** [Descarga gratuita aquí](https://adoptium.net/)
* **Oracle Java:** [Descarga oficial aquí](https://www.oracle.com/java/technologies/downloads/)

#### Paso 2: Activar Java en LibreOffice
1. Abre **LibreOffice**. Ve a `Herramientas` ▸ `Opciones` ▸ `LibreOffice` ▸ `Avanzado`. *(En macOS: `LibreOffice` ▸ `Preferencias...`)*
2. Marca la casilla **"Usar un entorno de ejecución de Java"**.
3. Selecciona el JRE instalado en la lista. Confirma con `OK` y reinicia LibreOffice.

#### Paso 3: Instalar la extensión
1. Descarga la última versión de la extensión (`PetanqueTurnierManager-vx.xx.oxt`) de la sección de [Releases](https://github.com/michaelmassee/Petanque-Turnier-Manager/releases).
2. Haz doble clic en el archivo `.oxt`. LibreOffice abrirá el **Gestor de extensiones**.
3. Confirma la instalación.

---

## 🚀 Primeros pasos y uso

1. Inicia **LibreOffice Calc** y abre una hoja de cálculo nueva y vacía.
2. En la barra de menú superior encontrarás un nuevo elemento llamado **"PétTurnMngr"**.
3. Haz clic en él para abrir el centro de control. Aquí puedes agregar jugadores, seleccionar el modo de torneo y generar las rondas.
4. Simplemente ingresa los resultados en los campos generados; las tablas y las siguientes rondas se calculan automáticamente con un clic.

---

## 📖 Documentación y ayuda
Encuentra la documentación completa en nuestro Wiki oficial (actualmente en alemán):
👉 **[Ir al Wiki del proyecto aquí](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki)**

**Licencia:** Este proyecto está bajo la [Licencia EUPL-1.2](LICENSE).
