import { useEffect, useRef } from 'react';
import SplitLayoutComposite from './SplitLayoutComposite';

/**
 * Composite-View mit mehreren Panels (BLATT/URL/TIMER) in einem FESTEN, zentral
 * konfigurierten Split-Layout (keine ziehbaren Split-Panes im Browser). Die Anordnung
 * kommt aus dem Split-Baum (`composite.layout`); jedes Panel skaliert seinen Inhalt per
 * Auto-Fit und aktualisiert ihn live über SSE-Diffs. Header/Footer werden global gerendert
 * (aus dem ersten Panel mit nicht-leeren Feldern), wenn `mitHeaderFooter` aktiv ist.
 */
export default function CompositeApp({ composite, timerAudio }) {
  const rootRef = useRef(null);

  useEffect(() => {
    if (timerAudio?.vorhanden && !timerAudio?.aktiv) {
      rootRef.current?.focus();
    }
  }, [timerAudio?.vorhanden, timerAudio?.aktiv]);

  if (!composite || !composite.layout) {
    return null;
  }
  const tonAktivieren = () => {
    if (timerAudio?.vorhanden && !timerAudio?.aktiv) {
      timerAudio.aktivieren?.();
    }
  };
  const keyDown = (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      tonAktivieren();
    }
  };
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
  const zoomFaktor = (composite.zoom ?? 100) / 100;
  const seitenStyle = {
    width: '100%',
    height: '100%',
    display: 'flex',
    flexDirection: 'column',
    transform: zoomFaktor !== 1 ? `scale(${zoomFaktor})` : undefined,
    transformOrigin: 'top left',
  };

  // Gesamtrahmen um die komplette View: rein clientseitig gezeichnet (CSS border + Keyframe-Animation),
  // unabhängig von der Live-Aktualisierung der Panels via SSE. "Laufende Ameisen" wird über ein
  // SVG-Overlay mit stroke-dashoffset-Animation realisiert (echte Wanderbewegung, per CSS-border
  // nicht möglich); dafür entfällt in dem Fall der statische CSS-Rahmen (sonst Doppel-Linie).
  const rand = composite.rand;
  const istAmeisenAnimation = rand?.animation === 'ameisen';
  const randKlasse = rand?.animation && rand.animation !== 'keine' && !istAmeisenAnimation
    ? `ptm-rand--${rand.animation}`
    : undefined;
  const rootStyle = {
    position: 'fixed',
    inset: 0,
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column',
    outline: 'none',
    ...(rand ? {
      boxSizing: 'border-box',
      border: istAmeisenAnimation ? 'none' : `${rand.dicke}px ${rand.art} ${rand.farbe}`,
      '--rand-farbe': rand.farbe,
    } : null),
  };

  return (
    <div
      ref={rootRef}
      tabIndex={0}
      onPointerDown={tonAktivieren}
      onKeyDown={keyDown}
      className={randKlasse}
      style={rootStyle}
    >
      {istAmeisenAnimation && (
        <svg className="ptm-rand-ameisen-svg" preserveAspectRatio="none" viewBox="0 0 100 100" aria-hidden="true">
          <rect
            x="0.5" y="0.5" width="99" height="99"
            fill="none"
            stroke={rand.farbe}
            strokeWidth={rand.dicke}
            strokeDasharray="6 4"
            vectorEffect="non-scaling-stroke"
          />
        </svg>
      )}
      <div style={seitenStyle}>
        {kopfzeilenPanel && (
          <div className="seitenzeile">
            <span className="links">{kopfzeilenPanel.kopfzeileLinks}</span>
            <span className="mitte">{kopfzeilenPanel.kopfzeileMitte}</span>
            <span className="rechts">{kopfzeilenPanel.kopfzeileRechts}</span>
          </div>
        )}
        <div style={{ flex: 1, minHeight: 0 }}>
          <SplitLayoutComposite
            knoten={composite.layout}
            panels={composite.panels}
            timerAudio={timerAudio}
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
    </div>
  );
}
