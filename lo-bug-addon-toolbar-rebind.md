# LibreOffice Bug-Report
## Addon-Toolbars: StatusListener nicht re-registriert nach `ToolBarManager` dispose

**Filed as:** [tdf#172207](https://bugs.documentfoundation.org/show_bug.cgi?id=172207)

## Component
- Framework / UI Configuration
- Module: `framework/source/uielement/toolbarmanager.cxx`,
  `framework/source/layoutmanager/toolbarlayoutmanager.cxx`

## Tested With
- LibreOffice 26.2.x (Ubuntu 24.04 LTS / Wayland)
- Reproduzierbar mit jeder Extension, die `addon_*`-Toolbars per XCU
  registriert und einen eigenen `XDispatchProvider`/`XStatusListener` betreibt
  (Reproducer: `ptm:`-Protokoll des Petanque-Turnier-Manager-Plugins).

## Summary

Wenn LibreOffice die `ToolBarManager`-Instanz einer `addon_*`-Toolbar disposed
(z. B. nach einem internen View-Zyklus, der `OnPrepareViewClosing` +
`OnUnload` feuert **ohne** ein gepaartes `OnViewCreated`), bleibt das
UI-Element im LayoutManager-Cache stehen, aber **alle Toolbar-Item-Controller
werden zerstört** und `removeStatusListener` wird auf den zugehörigen
Dispatchen gerufen. **LibreOffice erzeugt die Controller nicht neu**, sodass
die Toolbar weiter sichtbar aber „eingefroren" ist auf ihrem letzten
Status-Snapshot — keine weiteren `addStatusListener`-Calls, und für die
Extension gibt es keinen Weg zukünftige State-Updates zu empfangen.

## Reproduzierbares Szenario

1. OXT-Extension installieren, die
   - eine Addon-Toolbar via `Addons.xcu` registriert,
   - einen `XDispatchProvider` (`ProtocolHandler`) für deren URLs liefert,
   - dynamisches Enabled/Disabled der Buttons via `XStatusListener` steuert.
2. 2 Calc-Dokumente öffnen (Addon-Toolbar existiert in 2 Frames).
3. Aus doc2 eine Extension-Aktion auslösen, die einen `XSpreadsheetDocument`-
   modifizierenden Hintergrund-Job startet und ein `storeToURL`
   (autoSave-Pfad) durchführt.
4. ~15-20 s nach Abschluss der Aktion warten.

## Beobachtung

Im Extension-Trace-Log:

```
13:03:22,856  GlobalEvent OnPrepareViewClosing
13:03:22,857  GlobalEvent OnPrepareUnload
13:03:22,863  removeStatusListener × 24 (alle Toolbar-URLs von doc1-Handler)
13:03:22,919  GlobalEvent OnViewClosed
13:03:22,921  GlobalEvent OnUnload    (doc1-View)
13:03:26,121  removeStatusListener × 24 (alle Toolbar-URLs von doc2-Handler)
13:03:26,121  GlobalEvent OnUnload    (doc2-View)
<keine weiteren addStatusListener-Calls — kein OnViewCreated für beide Docs>
```

Beide Calc-Dokumente bleiben sichtbar und editierbar. Die Addon-Toolbar ist in
beiden Frames sichtbar, reagiert aber auf keine State-Änderungen mehr —
Buttons bleiben enabled/disabled wie zum Zeitpunkt der Controller-Disposing.

## Erwartet

Nach `ToolBarManager::disposing()` für die Manager-Component einer
Addon-Toolbar entweder:

- (a) Der nächste `XLayoutManager.requestElement(addon_url)` aus der Extension
  triggert einen echten Toolbar-Re-Build mit neuen ToolbarItemControllers
  und frischen `addStatusListener`-Calls, ODER
- (b) LibreOffice clear-t das gecachte `m_xUIElement` für die Addon-Toolbar
  im `ToolbarLayoutManager`, wenn dessen Manager disposed wird, sodass der
  nächste `requestToolbar` in den `createToolbar`-Zweig läuft.

## Root-Cause-Analyse (LO-Source)

`framework/source/layoutmanager/toolbarlayoutmanager.cxx:570`
(`ToolbarLayoutManager::destroyToolbar`):
```cpp
bool bMustBeDestroyed( !o3tl::starts_with(rResourceURL,
                       u"private:resource/toolbar/addon_") );
if (bMustBeDestroyed)
    elem.m_xUIElement.clear();   // Standard-Toolbars: echtes Destroy
else
    elem.m_bVisible = false;     // Addon-Toolbars: nur als unsichtbar markieren
```

`framework/source/layoutmanager/toolbarlayoutmanager.cxx:404`
(`ToolbarLayoutManager::requestToolbar`):
```cpp
UIElement aRequestedToolbar = impl_findToolbar(rResourceURL);
if (aRequestedToolbar.m_aName != rResourceURL)
    bMustCallCreate = true;
xUIElement = aRequestedToolbar.m_xUIElement;
if (!xUIElement.is())
    bMustCallCreate = true;

if (bCreateOrShowToolbar)
    bNotify = bMustCallCreate ? createToolbar(...) : showToolbar(...);
```

Kombinierter Effekt: bei `addon_*`-Toolbars, deren `ToolBarManager` per
`framework/source/uielement/toolbarmanager.cxx:771`
(`ToolBarManager::disposing(EventObject)` → `RemoveControllers()` →
`xComponent->dispose()` pro Controller → die Controller rufen
`removeStatusListener`) disposed wurde, hält der LayoutManager-Cache weiterhin
die `m_xUIElement`-Referenz zum **disposed** ToolBarManager. Der nächste
`requestElement`-Call sieht `xUIElement.is() == true` und nimmt den
`showToolbar`-Zweig — die Controller werden nicht neu gebaut.

Extensions haben damit keinen LO-API-Weg, den kaputten Zustand zu reparieren.

## Vorgeschlagener Fix

In `ToolBarManager::disposing(EventObject)` (oder in der auslösenden Kette)
den übergeordneten `ToolbarLayoutManager` benachrichtigen, damit dieser die
`m_xUIElement`-Referenz für die Addon-Toolbar clear-t. Der nächste
`requestToolbar` würde dann den `createToolbar`-Zweig nehmen und die
Controller neu erstellen.

Alternative: die `bMustBeDestroyed`-Bedingung in `destroyToolbar` lockern,
sodass auch der Fall behandelt wird, wo `xUIElement` auf eine disposed
Component zeigt.

## Extension-Workaround (versucht — funktioniert nicht zuverlässig)

Die Extension erkennt eine leere `STATUS_LISTENERS`-Map bei `onFocus` und
versucht eine Best-Effort-Sequenz aus `hideElement` + `showElement` +
`requestElement` pro Addon-URL. **Das stellt die Toolbar nicht zuverlässig
wieder her** — `showElement` / `requestElement` für eine Addon-Toolbar mit
non-null gecachetem `xUIElement` returnen früh, ohne Controller neu zu bauen.

Der einzige bekannte Recovery-Pfad für User: das betroffene Doc schließen und
neu öffnen.

## Warum LO-Bug, kein Extension-Misuse

- Die Extension implementiert `XStatusListener`/`addStatusListener`/
  `removeStatusListener` per UNO-Contract korrekt.
- Für Nicht-Addon-Toolbars (z. B. Standard-Calc) funktioniert dasselbe
  Disposal-Pattern, weil `destroyToolbar` den Cache dort sehr wohl clear-t.
  Die unterschiedliche Behandlung von `addon_*`-URLs in `destroyToolbar`
  (Z. 570) ist die Ursache.
- Auf `XLayoutManager` ist keine API dokumentiert, mit der Extensions ein
  Recreate einer Addon-Toolbar erzwingen könnten.

## Reproduktions-Extension

[Petanque-Turnier-Manager](https://github.com/michaelmassee/Petanque-Turnier-Manager)
— Logs unter `~/.petanqueturniermanager/info.log` mit Prefix `[FOKUS-TRACE]`
nach Trigger einer Extension-Aktion mit 2 offenen Docs.
