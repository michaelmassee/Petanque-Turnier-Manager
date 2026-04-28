<img align="right" src="images/petanqueturniermanager-logo-256px.png" alt="Logo" height="120">

# Pétanque-Turnier-Manager

[![License: EUPL v1.2](https://img.shields.io/badge/License-EUPL_1.2-blue.svg)](https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12)
[![Wiki](https://img.shields.io/badge/Doku-Projekt_Wiki-green.svg)](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki)
[![Spenden mit PayPal](https://img.shields.io/badge/Spenden-PayPal-blue.svg)](https://www.paypal.me/michaelmassee1)

*Read this in other languages: [🇩🇪 DE](README.md) | [🇬🇧 EN](README.en.md) | [🇫🇷 FR](README.fr.md) | [🇪🇸 ES](README.es.md) | [🇳🇱 NL](README.nl.md)*



---

## 🎯 Einleitung
Der **Pétanque-Turnier-Manager** ist eine leistungsstarke, Open-Source-Turnierleitungssoftware, die nahtlos als Erweiterung (Extension) in **LibreOffice Calc** integriert wird.

Die Software wurde speziell dafür entwickelt, die Organisation und Durchführung von Pétanque- und Boule-Turnieren so einfach und effizient wie möglich zu gestalten. Da sie komplett offline und direkt in der Tabellenkalkulation läuft, ist sie extrem ressourcenschonend. Das macht sie zur idealen Lösung für den Einsatz auf dem Bouleplatz – selbst auf älteren Laptops, die beispielsweise mit einem schlanken Linux-System betrieben werden.

**Vorteile auf einen Blick:**
* **Betriebssystemunabhängig:** Läuft zuverlässig unter Linux, macOS und Windows.
* **Mehrsprachig:** Die Benutzeroberfläche unterstützt mehrere Sprachen, darunter Deutsch (DE), Englisch (EN), Französisch (FR), Spanisch (ES) und Niederländisch (NL).
* **Kostenlos & Open Source:** Keine Lizenzgebühren, keine Werbung.
* **Ressourcenschonend:** Perfekt für ältere Hardware geeignet.
* **Alles an einem Ort:** Keine externe Datenbank nötig; alles wird direkt in LibreOffice Calc berechnet und gespeichert.
* **Eingebauter Webserver:** Alle Turnierdaten live auf Fernseher, Tablet oder Handy anzeigen – direkt aus LibreOffice, ohne externe Software.

Available in Languages: 🇩🇪 DE | 🇬🇧 EN | 🇫🇷 FR | 🇪🇸 ES | 🇳🇱 NL

---

## 🛠️ Grenzenlose Anpassbarkeit: Mach es zu *deinem* Turnier!

Das wohl größte Alleinstellungsmerkmal dieses Turnier-Managers ist seine Basis: **LibreOffice Calc**. Da alle Daten, Tabellen und Ranglisten direkt in reguläre Calc-Tabellenblätter geschrieben werden, bist du nicht in einem starren Programmgerüst gefangen.

> **💡 Volle Kontrolle mit Calc-Bordmitteln:** Du kannst **jedes** generierte Turniersystem völlig frei und nach deinen eigenen Wünschen erweitern!

* **Internationale Turniere:** Dank der integrierten Mehrsprachigkeit (Deutsch, Englisch, Französisch, Spanisch und Niederländisch) bist du bestens für Turniere mit internationalen Gästen oder für grenzübergreifende Ligen gerüstet.
* **Eigene Auswertungen:** Nutze SVERWEIS, Pivot-Tabellen oder eigene Formeln, um teaminterne Statistiken, Punkteschnitte oder individuelle Sonderwertungen aus den Turnierdaten zu ziehen.
* **Optische Anpassung:** Baue dir eigene Dashboards, füge Vereinslogos ein, ändere das Layout für den Ausdruck oder gestalte Präsentations-Ansichten für einen Beamer (z. B. für die Live-Anzeige der aktuellen Rangliste).
* **Eigene Makros & öffentliche API:** Wenn du fortgeschrittene Funktionen brauchst, kannst du jederzeit eigene LibreOffice-Makros (Basic, Python etc.) schreiben, die mit den generierten Tabellen interagieren. Das Plugin stellt dafür eine **öffentliche API** bereit: Alle Menüaktionen sind per `ptm:`-Protokoll aus Makros aufrufbar, und der `PtmPublicService` liefert Turnierdaten (System, aktuelle Runde, Spieltag, Betriebszustand) direkt als Rückgabewert. Eigene Calc-Formeln (`PTM.ALG.*`) ermöglichen zusätzlich den Datenzugriff direkt in Zellen. → [Makros & Formeln – vollständige API-Dokumentation](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Makros-und-Formeln)

Die Erweiterung erledigt die komplexe Logik (Auslosung, Ranglistenberechnung nach Buchholz etc.) – was du am Ende aus den Daten machst, bleibt komplett dir überlassen!

---

## 🏆 Unterstützte Turniersysteme
Die Erweiterung bietet bereits eine breite Auswahl an bewährten Turniermodi, die für kleine Vereinsmeisterschaften bis hin zu großen Turnieren geeignet sind. Die Liste wird kontinuierlich erweitert:

* **Supermêlée / Mêlée:**
  Ideal für lockere Turniere. Spieler werden in jeder Runde (oder für das gesamte Turnier) neuen Teams und Gegnern zugelost.
* **Liga:**
  Für die Organisation einer Vereins- oder Regionalliga mit festen Teams. Der Spielplan wird vorab generiert und legt fest, wer wann gegeneinander spielt. Die Rangliste wird nach Siegen, Kugeldifferenz und direktem Vergleich berechnet. Unterstützt HTML-Export der Ergebnisse und des Spielplans.
* **Schweizer System (Formule Suisse):**
  Das fairste System für Turniere mit vielen Teilnehmern und begrenzter Zeit. Inklusive automatischer Berechnung von Siegen, Kugeldifferenz und (Fein-)Buchholz-Punkten zur exakten Ranglistenermittlung.
* **Jeder gegen Jeden (Round Robin):**
  Das klassische Ligasystem, bei dem jedes Team im Laufe des Turniers gegen jedes andere Team antritt.
* **K.-o.-System (Direktausscheidung):**
  Für klassische Finalrunden und reine K.-o.-Turniere. Ein Sieg bedeutet Weiterkommen, eine Niederlage das sofortige Ausscheiden. Die Paarungen folgen dem Überkreuz-System (1 vs. Letzter, 2 vs. Vorletzter …), sodass Top-Teams erst spät aufeinandertreffen. Bei Teilnehmerzahlen, die keine Zweierpotenz ergeben, wird automatisch eine Cadrage (Vorrunde) berechnet.
* **Maastrichter System:**
  Kombiniert das Schweizer System mit K.-o.-Finalrunden. In mehreren Vorrunden (2–5) werden die Teams nach dem Schweizer Algorithmus ausgeglichen gepaart. Anschließend werden die Teams nach Siegen in Leistungsklassen (A, B, C, D) eingeteilt – jede Klasse trägt ihr eigenes K.-o.-Finale aus. Ergebnis: vier Turniersieger, faire Gruppenaufteilung, spannende Finalrunden.
* **Poule-A/B-System:**
  Klassischer Modus mit Gruppenphase (Poules) im Double-Elimination-Light-Verfahren und anschließender Aufteilung in A-Turnier (Hauptturnier) und B-Turnier (Consolante).
* **Kaskaden-KO-System (Erweitertes ABCD-System):**
  Erweitert das klassische ABCD-KO-System um beliebig viele Ebenen (E, F, G, H …). Anstatt früh auszuscheiden, kaskadieren unterlegene Teams schrittweise in tieferliegende Consolante-Turniere. Nach einer konfigurierbaren Mindestanzahl von Runden wechselt jede Ebene in den reinen K.-o.-Modus – mit eigener Cadrage falls nötig. Geeignet für mittlere bis große Turniere (ab 16 Teams).
* **Formule X:**
  Modernes Rundensystem aus dem französischen Pétanque-Turnierbetrieb – ideal für große Teilnehmerfelder und Turniere mit Zeitlimit. Alle Teams spielen gleich viele Runden, niemand scheidet aus. Die Rangliste entsteht durch eine klare Summenwertung (Siegaufschlag + eigene Punkte + Differenzpunkte) – ganz ohne Buchholz. Runde 1 wird frei gelost, danach werden jeweils Platz 1 vs. 2, 3 vs. 4 usw. gepaart.

---

## 🌐 Eingebauter Webserver – Live-Anzeige am Turniertag

Der Pétanque-Turnier-Manager hat einen **vollständig eingebauten Webserver** – direkt aus LibreOffice Calc heraus, ohne externe Software oder Internetverbindung.

Während das Turnier läuft, können alle Sheets per Browser auf **Fernseher, Laptop, Tablet oder Handy** angezeigt werden – in Echtzeit, automatisch aktualisiert:

* 📋 **Teilnehmerlisten** – wer spielt mit?
* 🎯 **Aktuelle Spielrunde** – welche Paarungen laufen gerade?
* 🏆 **Ranglisten** – live aktualisiert nach jeder Runde
* 📊 **Eigene Tabellen** – beliebige Calc-Sheets veröffentlichen

> **💡 So einfach geht's:** Webserver im Menü starten → URL im Browser öffnen → fertig. Alle Geräte im gleichen WLAN sehen die Turnierdaten live.

**Technische Details:**
* React-Frontend mit **Server-Sent Events (SSE)** – sofortige Aktualisierung ohne Seitenreload
* Bis zu **10 URLs gleichzeitig** konfigurierbar
* Zoom, Zentrierung und Kopf-/Fußzeilen pro Port einstellbar
* Alle Turniersysteme unterstützt: Supermêlée, Schweizer System, Jeder-gegen-Jeden, K.-O., Maastrichter System, Formule X

---

## 💻 Systemvoraussetzungen

Um den Turnier-Manager reibungslos nutzen zu können, müssen folgende Voraussetzungen erfüllt sein:

* **LibreOffice:** ab Version 25.x (oder neuer)
* **Java (JRE/JDK):** ab Version 25
* **Betriebssystem:** Linux, macOS oder Windows

---

## ⚙️ Installation & Einrichtung

Die Einrichtung erfolgt in drei einfachen Schritten. Bitte achte darauf, dass die Architektur deiner Java-Version (32-Bit oder 64-Bit) zwingend mit deiner LibreOffice-Installation übereinstimmen muss.

### Schritt 1: Java installieren
Falls noch kein Java installiert ist, lade eine aktuelle Version herunter und installiere sie. Wir empfehlen:
* **Temurin Adoptium JDK (LTS):** [Kostenloser Download hier](https://adoptium.net/de/)
* **Oracle Java:** [Offizieller Download hier](https://www.oracle.com/java/technologies/downloads/)

### Schritt 2: Java in LibreOffice aktivieren
Damit LibreOffice die Erweiterung ausführen kann, muss die Java-Laufzeitumgebung verknüpft werden:
1. Öffne **LibreOffice**.
2. Navigiere in der Menüleiste zu: `Extras` ▸ `Optionen` ▸ `LibreOffice` ▸ `Erweitert`. *(Unter macOS: `LibreOffice` ▸ `Einstellungen...`)*
3. Setze unter der Überschrift **„Java-Optionen“** ein Häkchen bei **„Eine Java-Laufzeitumgebung verwenden“**.
4. Wähle die installierte JRE aus der erscheinenden Liste aus.
   *(Sollte die Liste leer sein, klicke auf „Hinzufügen“ und wähle den Ordner deiner Java-Installation manuell aus.)*
5. Bestätige die Einstellungen mit `OK` und starte LibreOffice zur Sicherheit einmal neu.

### Schritt 3: Die Erweiterung installieren
1. Lade die aktuellste Version der Erweiterung (Dateiname: `PetanqueTurnierManager-vx.xx.oxt`) aus dem Bereich [Releases](https://github.com/michaelmassee/Petanque-Turnier-Manager/releases) herunter.
2. Doppelklicke auf die `.oxt`-Datei. LibreOffice öffnet automatisch den **Extension Manager** und fragt nach der Installation.
3. Bestätige die Installation und akzeptiere (falls nötig) die Lizenzbedingungen.
4. Schließe den Extension Manager. Die Installation ist nun abgeschlossen!

---

## 🚀 Erste Schritte & Verwendung

1. Starte **LibreOffice Calc** und öffne ein komplett leeres, neues Tabellenblatt.
2. Blicke in die obere Menüleiste von Calc. Dort findest du nun einen neuen Menüpunkt namens **„PétTurnMngr“**.
3. Klicke auf diesen Reiter. Es öffnet sich das Kontrollzentrum der Erweiterung, über das du Spieler anlegen, den Turniermodus auswählen und die Runden generieren kannst.
4. Das Programm führt dich ab hier menügesteuert durch das Turnier. Gib einfach die Ergebnisse der gespielten Partien in die generierten Felder ein, die Tabellen und nächsten Runden berechnen sich auf Knopfdruck von selbst.

---

## 📖 Dokumentation & Hilfe
Die vollständige Dokumentation, ausführliche Erklärungen zu den einzelnen Menüpunkten sowie Tipps zur Fehlerbehebung findest du in unserem offiziellen Wiki:
👉 **[Hier geht es zum Projekt-Wiki](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki)**

---

## 🤝 Mitwirken (Contributing)
Da dies ein Open-Source-Projekt ist, sind Beiträge aus der Community jederzeit willkommen! Egal ob es sich um das Melden von Fehlern (Issues), das Einreichen von Code-Verbesserungen (Pull Requests) oder das Erweitern der Dokumentation handelt – wir freuen uns über jede Unterstützung.

**Lizenz:** Dieses Projekt steht unter der [EUPL-1.2 Lizenz](LICENSE).
