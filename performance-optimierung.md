# Performance-Optimierung – nicht übernommene Maßnahmen

Sammlung von Optimierungen, die nach Abwägung **bewusst nicht** (oder noch nicht) übernommen wurden. Wer hier eine spätere Performance-Regression diagnostiziert, findet die Hintergründe und kann den Trade-off neu bewerten.

## CellStyleHelper – Scope-aware Lazy-Unprotect

**Quelle:** Commit `2a6b004a` auf Branch `lo-crash` (zusammen mit dem ControllerLock-Fix entstanden, in Commit `faed80ea` nur der ControllerLock-Teil übernommen).

### Was die Optimierung tut

`CellStyleHelper.apply()` macht bislang pro Aufruf einen vollen Protect/Unprotect-Roundtrip über **alle** Sheets im Dokument, die mit leerem Passwort geschützt sind:

1. Alle Sheets mit leerem Passwort merken und entsperren.
2. Zellstil schreiben (`setPropertyValue` auf der `XStyle`).
3. Alle gemerkten Sheets wieder schützen.

Innerhalb eines SheetRunner-Laufs wird `apply()` N-mal aufgerufen (jede Bedingte Formatierung, jeder Style-Set in einer Spielrunde/Rangliste). Damit laufen **N redundante Sweeps** über alle Sheets, obwohl der `SheetRunner` ohnehin schon einen Lazy-Unprotect-Scope via `BlattschutzManager.beginCommandScope(...)` öffnet — die Sheets sind im Scope sowieso entsperrt.

Die Optimierung führt zwei Code-Pfade in `CellStyleHelper.apply()` ein:

- **Im SheetRunner-Scope** (Standardfall): Neue API `BlattschutzManager.tryEnsureUnprotectedInScope()` entsperrt einmalig pro Scope und liefert `true` zurück. `apply()` verzichtet dann auf seinen eigenen Sweep.
- **Ohne Scope** (z. B. `TurnierModus.aktivieren()` → `BlattschutzManager.schuetzen()`): Legacy-Sweep bleibt erhalten.

### Geschätzter Nutzen

Tournament-Generierungen mit vielen Style-/CF-Anwendungen profitieren spürbar — vor allem auf Windows, wo jeder UNO-Roundtrip teurer ist als auf Linux. Eine harte Messung liegt nicht vor; der bisherige Engpass war der Renderpfad (jetzt durch ControllerLock entschärft), nicht der Protect/Unprotect-Overhead.

### Warum nicht übernommen

1. **ThreadLocal-Pfadwechsel ist subtil.** Die Logik hängt an `BlattschutzManager.SCOPE` (ThreadLocal). Wird `CellStyleHelper.apply()` aus einem anderen Thread aufgerufen, fällt es lautlos in den Legacy-Sweep zurück — Performance-Regression ohne Test-Failure. Refactors, die `apply()` versehentlich aus einem Helper-Thread ziehen, sind ein potentielles Footgun.
2. **Geändertes Error-Logging.** Der Commit hat im `catch (RuntimeException)`-Zweig `error` zu `warn` umgewertet (mit Exception). Vorteil: einheitliches Logging. Risiko: echte Style-Bugs (z. B. fehlerhafter Style-Name, kein Tab-Schutz-Problem) werden jetzt nur noch als `warn` geloggt — leichter zu übersehen.
3. **Blast-Radius im Kernfix nicht erwünscht.** Beim Cherry-Pick des ControllerLock-Fixes lag der Fokus auf der Crash-Bekämpfung; eine zweite, unabhängige Optimierung im selben Commit hätte die Diagnose bei Folge-Problemen erschwert.
4. **ControllerLock-Effekt mildert das Problem ohnehin.** Mit gesperrtem Controller fällt der visuelle Repaint pro `setPropertyValue` weg, der ein Großteil der UNO-Property-Latenz war. Der reine Protect/Unprotect-Roundtrip bleibt — schmerzt aber nur, wenn die Sweeps tatsächlich messbar dominieren.

### Voraussetzungen für späteres Nachziehen

- Reproduzierbare Messung des Sweep-Overheads (z. B. `System.nanoTime()` um eine Spielrunden-Generierung in Supermêlée, vor/nach Optimierung).
- Klärung: kann `CellStyleHelper.apply()` legitim aus einem Nicht-SheetRunner-Thread aufgerufen werden? Falls ja, dokumentieren und ggf. fallback explizit machen statt stillem ThreadLocal-Read.
- Entscheidung zum Error-Logging-Level separat treffen, nicht als Mit-Aufräumung.

### Referenzcode aus Commit 2a6b004a

`BlattschutzManager.tryEnsureUnprotectedInScope()`:

```java
public boolean tryEnsureUnprotectedInScope() {
    ScopeState state = SCOPE.get();
    if (state == null) {
        return false;
    }
    if (!state.wurdeEntsperrt) {
        doEntsperren(state.konfig, state.ws);
        state.wurdeEntsperrt = true;
    }
    return true;
}
```

`CellStyleHelper.apply()`-Umbau (Auszug):

```java
boolean scopeHandlesProtection = BlattschutzManager.get().tryEnsureUnprotectedInScope();
List<XProtectable> temporaerEntsperrt = scopeHandlesProtection
        ? List.of()
        : entsperreAlleSheetsMitLeeremPasswort(currentSpreadsheetDocument);
// ... Style schreiben ...
// finally: nur temporaerEntsperrt wieder schützen
```

Vollständiger Diff: `git show 2a6b004a -- src/main/java/de/petanqueturniermanager/helper/cellstyle/CellStyleHelper.java src/main/java/de/petanqueturniermanager/helper/sheet/blattschutz/BlattschutzManager.java`
