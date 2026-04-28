# Sidebar Bug-Analyse: 2 defekte Panels beim ersten Start

## Status: BEHOBEN (2026-04-25)

---

## Root Cause

`createUIElement` wurde beim LO-Start **2× innerhalb von 47ms** aufgerufen (LO feuert 2 Kontext-Wechsel-Events schnell hintereinander). Beide Male war noch kein Calc-Dokument geladen.

`DocumentHelper.getCurrentSpreadsheetView()` warf explizit `NullPointerException("xModel == null")`, wenn kein Dokument verfügbar war. Das wurde von `WorkingSpreadsheet(xContext)` in `createUIElement` aufgerufen.

Factory fing die Exception, gab `null` zurück → LO zeigte 2 defekte Panel-Platzhalter.

7 Sekunden nach Start wurde `createUIElement` ein **3. Mal** aufgerufen (nach Dokument-Laden) → Erfolg → korrektes Panel mit "5.7.40".

### Beweis aus info.log (vor Fix):
```
10:38:20 - createUIElement → ERROR: xModel == null   ← 1. Aufruf
10:38:20 - createUIElement → ERROR: xModel == null   ← 2. Aufruf (47ms später)
10:38:27 - createUIElement → SUCCESS: "Neues InfoSidebarPanel"  ← 3. Aufruf
```

### Beweis aus info.log (nach Fix):
```
20:32:27 - createUIElement → SUCCESS: "Neues InfoSidebarPanel"  ← sofort, 1× aufgerufen
20:32:28 - OnNew (Dokument erst DANACH verfügbar)
```

---

## Fix (4 Dateien geändert)

### 1. `DocumentHelper.java` – `getCurrentSpreadsheetView()`
Statt `throw new NullPointerException("xModel == null")` → `return null` wenn kein Modell verfügbar.

### 2. `WorkingSpreadsheet.java` – Konstruktor `(XComponentContext)`
`xController` null-safe: `xController = xModel != null ? xModel.getCurrentController() : null;`

### 3. `BaseSidebarContent.java` – `onPropertiesChanged()`
Null-Check für `getWorkingSpreadsheetDocument()` hinzugefügt (kann null sein wenn Sidebar vor Dokument erstellt wird).

### 4. `BaseSidebarContent.java` – `aktualisiereSpreadsheetUndFelder()`
Deprecated 3-Arg-Konstruktor durch `WorkingSpreadsheet(context, document)` ersetzt.

---

## Vorherige Untersuchungsergebnisse

### 1. Doppelregistrierung via `.components` — WIRKUNGSLOS

`PetanqueTurnierManager.components` (eingebettet im JAR unter `META-INF/`) referenziert:
```xml
<component loader="com.sun.star.loader.Java2" uri="PetanqueTurnierManager-1.0.0.jar">
```

Der JAR im OXT heißt jedoch `PetanqueTurnierManager.jar`. Die `.components`-Registrierung findet den JAR nicht und ist damit **wirkungslos**. Alle Registrierungen laufen über `RegistrationHandler.classes`.

**Konsequenz:** Es gibt keine echte Doppelregistrierung der Factory.

### 2. Nur EIN Panel in `Sidebar.xcu`

`oor:op="replace"` schließt doppelte XCU-Einträge aus.

### 3. Stale Eintrag in `registrymodifications.xcu`

Fügt `com.sun.star.sheet.SpreadsheetDocument, any, visible` als Duplikat per `fuse` in die ContextList ein. Da `ReadContextList()` "Calc" ebenfalls auf `"com.sun.star.sheet.SpreadsheetDocument"` mappt, ist der Inhalt identisch → kein Duplikat-Panel, aber ein überflüssiger Eintrag.

Aufräumen: LO Sidebar-Einstellungen zurücksetzen oder `~/.config/libreoffice/4/user/registrymodifications.xcu` bereinigen.

### 4. `Factories.xcu` korrekt registriert

Passt zum LO-internen Pfad `/org.openoffice.Office.UI.Factories/Registered/UIElementFactories`. Die alte `UIElementFactoryManager.xcu` verwendete einen falschen Pfad und wurde gelöscht.

---

## Relevante Dateien

| Datei | Zustand | Bedeutung |
|-------|---------|-----------|
| `META-INF/manifest.xml` | M | Sidebar.xcu + Factories.xcu aktiviert |
| `registry/.../Sidebar.xcu` | unverändert | 1 Deck, 1 Panel |
| `registry/.../Factories.xcu` | neu | Factory-Registrierung (korrekt) |
| `registry/.../UIElementFactoryManager.xcu` | gelöscht | Alter falscher Pfad |
| `PetanqueTurnierManager.components` | M | Factory-Eintrag aktiviert, aber URI falsch (noch offen) |
| `RegistrationHandler.classes` | M | Factory aktiviert |
| `PetanqueTurnierManagerPanelFactory.java` | M | `__getComponentFactory` / `__writeRegistryServiceInfo` |
| `DocumentHelper.java` | M | **FIX**: `getCurrentSpreadsheetView` wirft nicht mehr bei fehlendem Dokument |
| `WorkingSpreadsheet.java` | M | **FIX**: Null-sicherer Konstruktor |
| `BaseSidebarContent.java` | M | **FIX**: Null-Check + deprecated Konstruktor entfernt |
