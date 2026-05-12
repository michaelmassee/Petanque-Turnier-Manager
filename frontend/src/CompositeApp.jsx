import SplitPaneComposite from './SplitPaneComposite';

/**
 * Composite-View mit Split-Layout-Baum aus mehreren Panels (BLATT/URL/TIMER).
 * Header/Footer werden global gerendert (aus dem ersten Panel mit nicht-leeren
 * Feldern), wenn `mitHeaderFooter` aktiv ist.
 */
export default function CompositeApp({ composite }) {
  if (!composite || !composite.layout) {
    return null;
  }
  const compositeMitHeaderFooter = composite.mitHeaderFooter !== false;
  // Globalen Header/Footer aus dem ERSTEN Panel mit nicht-leeren Kopf-/Fußzeilen wählen.
  // Panel 0 darf ein Timer/URL/fehlend-Panel sein – dort sind die Felder null und würden
  // sonst dazu führen, dass GAR KEIN Header/Footer angezeigt wird, obwohl andere Sheet-Panels
  // welche hätten. Kopfzeile und Fußzeile werden separat gesucht (können aus verschiedenen
  // Panels stammen).
  const panelsSortiert = compositeMitHeaderFooter
    ? Object.keys(composite.panels)
        .map((k) => Number(k))
        .sort((a, b) => a - b)
        .map((id) => composite.panels[id])
    : [];
  const kopfzeilenPanel = panelsSortiert.find((p) =>
    p && (p.kopfzeileLinks?.trim() || p.kopfzeileMitte?.trim() || p.kopfzeileRechts?.trim())
  );
  const fusszeilenPanel = panelsSortiert.find((p) =>
    p && (p.fusszeileLinks?.trim() || p.fusszeileMitte?.trim() || p.fusszeileRechts?.trim())
  );

  return (
    <div style={{ position: 'fixed', inset: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
      {kopfzeilenPanel && (
        <div className="seitenzeile">
          <span className="links">{kopfzeilenPanel.kopfzeileLinks}</span>
          <span className="mitte">{kopfzeilenPanel.kopfzeileMitte}</span>
          <span className="rechts">{kopfzeilenPanel.kopfzeileRechts}</span>
        </div>
      )}
      <div style={{ flex: 1, minHeight: 0 }}>
        <SplitPaneComposite
          knoten={composite.layout}
          panels={composite.panels}
          headerFooterUnterdruecken
        />
      </div>
      {fusszeilenPanel && (
        <div className="seitenzeile">
          <span className="links">{fusszeilenPanel.fusszeileLinks}</span>
          <span className="mitte">{fusszeilenPanel.fusszeileMitte}</span>
          <span className="rechts">{fusszeilenPanel.fusszeileRechts}</span>
        </div>
      )}
    </div>
  );
}
