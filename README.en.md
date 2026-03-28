<img align="right" src="https://github.com/michaelmassee/Petanque-Turnier-Manager/raw/master/doku/images/petanqueturniermanager-logo-256px.png" alt="Logo" height="120">

# Pétanque Tournament Manager

[![License: EUPL v1.2](https://img.shields.io/badge/License-EUPL_1.2-blue.svg)](https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12)
[![Wiki](https://img.shields.io/badge/Doku-Projekt_Wiki-green.svg)](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki)
[![Spenden mit PayPal](https://img.shields.io/badge/Spenden-PayPal-blue.svg)](https://www.paypal.me/@michaelmassee1)

*Read this in other languages: [🇩🇪 DE](README.md) | [🇬🇧 EN](README.en.md) | [🇫🇷 FR](README.fr.md) | [🇪🇸 ES](README.es.md) | [🇳🇱 NL](README.nl.md)*


---

## 🎯 Introduction
The **Pétanque Tournament Manager** is a powerful, open-source tournament management software that integrates seamlessly as an extension into **LibreOffice Calc**.

The software was specifically designed to make the organization and execution of Pétanque and Boule tournaments as simple and efficient as possible. Since it runs completely offline and directly within the spreadsheet, it is extremely resource-efficient. This makes it the ideal solution for use on the boules pitch – even on older laptops running a lightweight Linux system, for example.

**Advantages at a glance:**
* **OS Independent:** Runs reliably on Linux, macOS, and Windows.
* **Multilingual:** The user interface supports multiple languages, including German (DE), English (EN), French (FR), Spanish (ES), and Dutch (NL).
* **Free & Open Source:** No license fees, no advertising.
* **Resource Efficient:** Perfectly suited for older hardware.
* **Everything in one place:** No external database required; everything is calculated and stored directly in LibreOffice Calc.

Available in Languages: 🇩🇪 DE | 🇬🇧 EN | 🇫🇷 FR | 🇪🇸 ES | 🇳🇱 NL

---

## 🛠️ Limitless Customization: Make it *your* tournament!

Arguably the biggest unique selling point of this tournament manager is its foundation: **LibreOffice Calc**. Because all data, tables, and rankings are written directly into regular Calc spreadsheets, you are not trapped in a rigid program structure.

> **💡 Full control with built-in Calc tools:** You can expand **any** generated tournament system completely freely and according to your own wishes!

* **International Tournaments:** Thanks to the built-in multi-language support (German, English, French, Spanish, and Dutch), you are perfectly equipped for tournaments with international guests or cross-border leagues.
* **Custom Evaluations:** Use VLOOKUP, pivot tables, or your own formulas to extract team-internal statistics, point averages, or individual special rankings from the tournament data.
* **Visual Customization:** Build your own dashboards, insert club logos, change the layout for printing, or design presentation views for a projector (e.g., for a live display of the current ranking).
* **Custom Macros & Public API:** If you need advanced functions, you can always write your own LibreOffice macros (Basic, Python, etc.) that interact with the generated tables. The plugin provides a **public API** for this: All menu actions can be called from macros via the `ptm:` protocol, and the `PtmPublicService` returns tournament data (system, current round, matchday, operating status) directly. Custom Calc formulas (`PTM.ALG.*`) additionally allow data access directly in cells. → [Macros & Formulas – complete API documentation (German)](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Makros-und-Formeln)

The extension handles the complex logic (draw, ranking calculation according to Buchholz, etc.) – what you ultimately do with the data is completely up to you!

---

## 🏆 Supported Tournament Systems
The extension already offers a wide selection of proven tournament modes suitable for small club championships up to large tournaments. The list is continuously expanding:

* **Supermêlée / Mêlée:**
  Ideal for casual tournaments. Players are drawn into new teams and against new opponents in every round (or for the entire tournament).
* **League:**
  For organizing a club or regional league with fixed teams. The match schedule is generated in advance and determines who plays against whom and when. The ranking is calculated by wins, point difference, and direct comparison. Supports HTML export of results and the match schedule.
* **Swiss System (Formule Suisse):**
  The fairest system for tournaments with many participants and limited time. Includes automatic calculation of wins, point difference, and (Fine) Buchholz points for exact ranking determination.
* **Round Robin:**
  The classic league system where every team competes against every other team over the course of the tournament.
* **Knockout System (Single Elimination):**
  For classic final rounds and pure knockout tournaments. A win means advancing, a loss means immediate elimination. The pairings follow the cross-system (1st vs. Last, 2nd vs. Second to Last...), so top teams meet later on. For participant numbers that are not a power of two, a cadrage (preliminary round) is automatically calculated.
* **Maastricht System:**
  Combines the Swiss System with knockout final rounds. Teams are paired using the Swiss algorithm over several preliminary rounds (2–5). Afterwards, teams are divided into performance groups (A, B, C, D) based on their number of wins – each group then plays its own knockout final. Result: four tournament winners, fair group allocation, and exciting finals.

(In progress)
* **Poule System:**
  Classic mode with a group phase (Poules) and subsequent division into knockout brackets (A, B, C, D tournament).

---

## 💻 System Requirements

To use the Tournament Manager smoothly, the following requirements must be met:

* **LibreOffice:** version 25.x (or newer)
* **Java (JRE/JDK):** version 25 or higher
* **Operating System:** Linux, macOS, or Windows

---

## ⚙️ Installation & Setup

Setup is done in three simple steps. Please make sure that the architecture of your Java version (32-bit or 64-bit) absolutely matches your LibreOffice installation.

### Step 1: Install Java
If Java is not yet installed, download and install a current version. We recommend:
* **Temurin Adoptium JDK (LTS):** [Free download here](https://adoptium.net/)
* **Oracle Java:** [Official download here](https://www.oracle.com/java/technologies/downloads/)

### Step 2: Enable Java in LibreOffice
For LibreOffice to run the extension, the Java Runtime Environment must be linked:
1. Open **LibreOffice**.
2. Navigate in the menu bar to: `Tools` ▸ `Options` ▸ `LibreOffice` ▸ `Advanced`. *(On macOS: `LibreOffice` ▸ `Preferences...`)*
3. Under the heading **"Java Options"**, check the box for **"Use a Java runtime environment"**.
4. Select the installed JRE from the list that appears.
   *(If the list is empty, click "Add" and manually select your Java installation folder.)*
5. Confirm the settings with `OK` and restart LibreOffice just to be safe.

### Step 3: Install the Extension
1. Download the latest version of the extension (filename: `PetanqueTurnierManager-vx.xx.oxt`) from the [Releases](https://github.com/michaelmassee/Petanque-Turnier-Manager/releases) section.
2. Double-click the `.oxt` file. LibreOffice will automatically open the **Extension Manager** and ask about the installation.
3. Confirm the installation and accept the license terms (if necessary).
4. Close the Extension Manager. The installation is now complete!

---

## 🚀 First Steps & Usage

1. Start **LibreOffice Calc** and open a completely blank, new spreadsheet.
2. Look at the top menu bar of Calc. You will now find a new menu item called **"PétTurnMngr"**.
3. Click on this tab. The extension's control center opens, where you can add players, select the tournament mode, and generate the rounds.
4. From here on, the program will guide you through the tournament via menus. Simply enter the results of the played matches into the generated fields; the tables and next rounds are calculated automatically at the push of a button.

---

## 📖 Documentation & Help
The complete documentation, detailed explanations of the individual menu items, and troubleshooting tips can be found in our official Wiki (currently in German):
👉 **[Go to the Project Wiki here](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki)**

---

## 🤝 Contributing
Since this is an open-source project, contributions from the community are always welcome! Whether it's reporting bugs (Issues), submitting code improvements (Pull Requests), or expanding the documentation – we appreciate any support.

**License:** This project is licensed under the [EUPL-1.2 License](LICENSE).