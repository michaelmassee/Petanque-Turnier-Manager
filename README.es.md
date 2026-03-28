<img align="right" src="https://github.com/michaelmassee/Petanque-Turnier-Manager/raw/master/doku/images/petanqueturniermanager-logo-256px.png" alt="Logo" height="120">

# Gestor de Torneos de Petanca

[![License: EUPL v1.2](https://img.shields.io/badge/License-EUPL_1.2-blue.svg)](https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12)
[![Wiki](https://img.shields.io/badge/Doku-Projekt_Wiki-green.svg)](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki)
[![Donar con PayPal](https://img.shields.io/badge/Donar-PayPal-blue.svg)](https://www.paypal.me/michaelmassee1)

*Read this in other languages: [🇩🇪 DE](README.md) | [🇬🇧 EN](README.en.md) | [🇫🇷 FR](README.fr.md) | [🇪🇸 ES](README.es.md) | [🇳🇱 NL](README.nl.md)*


---

## 🎯 Introducción
El **Gestor de Torneos de Petanca** (Pétanque-Turnier-Manager) es un potente software de código abierto para la gestión de torneos que se integra a la perfección como una extensión en **LibreOffice Calc**.

El software fue diseñado específicamente para hacer que la organización y ejecución de torneos de petanca y bochas sea lo más sencilla y eficiente posible. Dado que funciona completamente sin conexión y directamente en la hoja de cálculo, consume muy pocos recursos. Esto lo convierte en la solución ideal para su uso en la pista de petanca, incluso en portátiles antiguos que funcionan, por ejemplo, con un sistema Linux ligero.

**Ventajas de un vistazo:**
* **Independiente del sistema operativo:** Funciona de forma fiable en Linux, macOS y Windows.
* **Multilingüe:** La interfaz de usuario admite varios idiomas, incluidos alemán (DE), inglés (EN), francés (FR), español (ES) y holandés (NL).
* **Gratuito y de Código Abierto:** Sin costes de licencia, sin publicidad.
* **Eficiente en recursos:** Perfectamente adecuado para hardware antiguo.
* **Todo en un solo lugar:** No se necesita una base de datos externa; todo se calcula y almacena directamente en LibreOffice Calc.

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
* **Liga:** Para organizar una liga de club o regional con equipos fijos. Soporta exportación HTML de resultados y calendarios.
* **Sistema Suizo (Formule Suisse):** El sistema más justo para torneos con muchos participantes y tiempo limitado. Incluye cálculo automático de victorias, diferencia de puntos y puntos Buchholz.
* **Todos contra todos (Round Robin):** El sistema clásico de liga.
* **Sistema de eliminación directa (Knockout):** Para rondas finales clásicas. Si el número de participantes no es una potencia de dos, se calcula automáticamente un "cadrage" (ronda preliminar).
* **Sistema de Maastricht:** Combina el Sistema Suizo con rondas finales de eliminación directa. Los equipos se emparejan según el algoritmo suizo en varias rondas preliminares (2–5). Después, los equipos se dividen en grupos de nivel (A, B, C, D) según su número de victorias – cada grupo disputa su propia final en formato knockout. Resultado: cuatro ganadores del torneo, una distribución justa y finales emocionantes.

(En progreso)
* **Sistema de Poules:** Modo clásico con fase de grupos (Poules) y posterior división en cuadros de eliminación (Torneo A, B, C, D).

---

## 💻 Requisitos del sistema

* **LibreOffice:** versión 25.x (o superior)
* **Java (JRE/JDK):** versión 25 o superior
* **Sistema operativo:** Linux, macOS o Windows

---

## ⚙️ Instalación y configuración

### Paso 1: Instalar Java
* **Temurin Adoptium JDK (LTS):** [Descarga gratuita aquí](https://adoptium.net/)
* **Oracle Java:** [Descarga oficial aquí](https://www.oracle.com/java/technologies/downloads/)

### Paso 2: Activar Java en LibreOffice
1. Abre **LibreOffice**. Ve a `Herramientas` ▸ `Opciones` ▸ `LibreOffice` ▸ `Avanzado`. *(En macOS: `LibreOffice` ▸ `Preferencias...`)*
2. Marca la casilla **"Usar un entorno de ejecución de Java"**.
3. Selecciona el JRE instalado en la lista. Confirma con `OK` y reinicia LibreOffice.

### Paso 3: Instalar la extensión
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