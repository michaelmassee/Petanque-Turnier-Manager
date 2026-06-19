import { useCallback, useEffect, useRef, useState } from 'react';
import Cell from './Cell';
import StartseiteApp from './StartseiteApp';
import { transformOrigin } from './alignment';
import { useAutoFitScale } from './useAutoFit';

// 1/100 mm → Pixel (gerundet, verhindert Sub-Pixel-Layout-Drift)
const toPx = (v) => Math.round((v || 0) / 37.795) + 'px';

// Obergrenze für das automatische Hochskalieren eines Sheet-Panels, damit kleine Tabellen
// einen großen Bildschirm (Beamer/TV) ausfüllen, ohne ins Absurde zu wachsen.
const AUTO_FIT_MAX_SCALE = 8;
const PANEL_SCROLL_PX_PRO_SEKUNDE = 24;
const PANEL_SCROLL_PAUSE_MS = 1800;

function alignPosition(h, v) {
  let left = '0%';
  let translateX = '0%';
  if (h === 'mitte') {
    left = '50%';
    translateX = '-50%';
  } else if (h === 'rechts') {
    left = '100%';
    translateX = '-100%';
  }

  let top = '0%';
  let translateY = '0%';
  if (v === 'mitte') {
    top = '50%';
    translateY = '-50%';
  } else if (v === 'unten') {
    top = '100%';
    translateY = '-100%';
  }

  return { left, top, translateX, translateY };
}

/**
 * Rendert eine einzelne Tabellen-Ansicht (Sheet).
 *
 * @param {Object} props.table - Tabellenzustand (entspricht state.table in App.jsx)
 * @param {boolean} props.sheetnamenAnzeigen - ob Blattnamen in der Kopfzeile angezeigt werden
 * @param {boolean} [props.headerFooterUnterdruecken] - blendet die Panel-eigenen Kopf-/Fußzeilen aus
 *        (verwendet vom Composite-Raster, wenn ein globaler Header/Footer gerendert wird)
 */
export default function Panel({ table, sheetnamenAnzeigen, headerFooterUnterdruecken, timerAudio }) {
  const [iframeFehler, setIframeFehler] = useState(false);
  const containerRef = useRef(null);
  const contentRef = useRef(null);
  const zoomFaktor = (table?.zoom ?? 100) / 100;
  const sichtbarerTabellenAnteil = Math.min(100, Math.max(10, table?.sichtbarerTabellenAnteil ?? 100));
  const scrollAktiv = sichtbarerTabellenAnteil < 100;
  const verfuegbarePanelFlaeche = useCallback((container) => ({
    breite: container.clientWidth,
    hoehe: scrollAktiv
      ? container.clientHeight / (sichtbarerTabellenAnteil / 100)
      : container.clientHeight,
  }), [scrollAktiv, sichtbarerTabellenAnteil]);
  const autoFitScale = useAutoFitScale(
    containerRef, contentRef,
    {
      maxScale: AUTO_FIT_MAX_SCALE,
      zoom: zoomFaktor,
      verfuegbar: verfuegbarePanelFlaeche,
    },
    [table?.zeilen, table?.spalten, table?.zoom, table?.gitter, sichtbarerTabellenAnteil],
  );
  const position = alignPosition(table?.hAlign, table?.vAlign);
  const contentPosition = scrollAktiv
    ? { ...position, top: '0%', translateY: '0px' }
    : position;

  useEffect(() => {
    setIframeFehler(false);
  }, [table?.externeUrl]);

  useEffect(() => {
    const container = containerRef.current;
    const content = contentRef.current;
    if (!container || !content) return undefined;

    const setTransform = (yPx) => {
      content.style.transform = `translate(${contentPosition.translateX}, ${yPx}px) scale(${autoFitScale})`;
    };

    if (!scrollAktiv || window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      setTransform(contentPosition.translateY);
      return undefined;
    }

    const scrollStrecke = Math.max(0, (content.offsetHeight * autoFitScale) - container.clientHeight);
    if (scrollStrecke < 1) {
      setTransform(0);
      return undefined;
    }

    let rafId = null;
    const start = performance.now();
    const bewegungMs = Math.max(6000, (scrollStrecke / PANEL_SCROLL_PX_PRO_SEKUNDE) * 1000);
    const zyklusMs = (PANEL_SCROLL_PAUSE_MS * 2) + (bewegungMs * 2);

    const tick = (zeit) => {
      const t = (zeit - start) % zyklusMs;
      let y = 0;
      if (t < PANEL_SCROLL_PAUSE_MS) {
        y = 0;
      } else if (t < PANEL_SCROLL_PAUSE_MS + bewegungMs) {
        y = -scrollStrecke * ((t - PANEL_SCROLL_PAUSE_MS) / bewegungMs);
      } else if (t < (PANEL_SCROLL_PAUSE_MS * 2) + bewegungMs) {
        y = -scrollStrecke;
      } else {
        y = -scrollStrecke * (1 - ((t - (PANEL_SCROLL_PAUSE_MS * 2) - bewegungMs) / bewegungMs));
      }
      setTransform(y);
      rafId = requestAnimationFrame(tick);
    };

    rafId = requestAnimationFrame(tick);
    return () => {
      if (rafId !== null) cancelAnimationFrame(rafId);
    };
  }, [
    autoFitScale,
    contentPosition.translateX,
    contentPosition.translateY,
    scrollAktiv,
    table?.zeilen,
    table?.spalten,
    table?.gitter,
  ]);

  if (!table) {
    return null;
  }

  if (table.hinweisTitel || table.hinweisText) {
    return (
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        width: '100%', height: '100%', padding: '16px', boxSizing: 'border-box',
        flexDirection: 'column', gap: '8px', textAlign: 'center',
      }}>
        <div style={{ fontSize: '2em' }}>⏳</div>
        {table.hinweisTitel && (
          <div style={{ fontWeight: 'bold' }}>{table.hinweisTitel}</div>
        )}
        {table.hinweisText && (
          <div style={{ fontSize: '0.9em' }}>{table.hinweisText}</div>
        )}
      </div>
    );
  }

  if (table.timerAnzeige != null) {
    const zustand = (table.timerZustand ?? 'INAKTIV').toLowerCase();
    const GUELTIGE_ZUSTAENDE = ['inaktiv', 'laeuft', 'pausiert', 'beendet'];
    const cls = GUELTIGE_ZUSTAENDE.includes(zustand)
      ? `timer-zustand-${zustand}`
      : 'timer-zustand-inaktiv';
    const hintergrundStyle = table.timerHintergrundFarbe
      ? { backgroundColor: `${table.timerHintergrundFarbe}cc` }
      : {};
    const tonStatusAnzeigen = timerAudio?.vorhanden && !timerAudio?.aktiv;
    const tonAktivieren = () => {
      timerAudio?.aktivieren?.();
    };
    const tonPointerDown = (event) => {
      event.stopPropagation();
    };
    const tonKeyDown = (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        event.stopPropagation();
        tonAktivieren();
      }
    };
    return (
      <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
        <div className={`timer-panel ${cls}`} style={{
          ...hintergrundStyle,
          transform: table.zoom !== 100 ? `scale(${table.zoom / 100})` : undefined,
          transformOrigin: transformOrigin(table.hAlign, table.vAlign),
        }}>
          {table.timerBezeichnung && (
            <div className="timer-bezeichnung">{table.timerBezeichnung}</div>
          )}
          <div className="timer-anzeige">{table.timerAnzeige ?? '--:--'}</div>
          {tonStatusAnzeigen && (
            <div
              className={`timer-tonstatus${timerAudio?.fehler ? ' timer-tonstatus-fehler' : ''}`}
              role="button"
              tabIndex={0}
              onPointerDown={tonPointerDown}
              onClick={tonAktivieren}
              onKeyDown={tonKeyDown}
            >
              <div className="timer-tonstatus-titel">{timerAudio?.titel || 'Kein Ton aktiv'}</div>
              <div className="timer-tonstatus-hinweis">{timerAudio?.hinweis || 'OK drücken / antippen für Ton'}</div>
            </div>
          )}
        </div>
      </div>
    );
  }

  if (table.startseite) {
    return <StartseiteApp startseite={table.startseite} />;
  }

  if (table.externeUrl) {
    if (iframeFehler) {
      return (
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          width: '100%', height: '100%', padding: '16px', boxSizing: 'border-box',
          color: '#c0392b', flexDirection: 'column', gap: '8px',
        }}>
          <div style={{ fontWeight: 'bold' }}>Fehler beim Laden der URL</div>
          <div style={{ fontSize: '0.85em', wordBreak: 'break-all' }}>{table.externeUrl}</div>
        </div>
      );
    }
    return (
      <iframe
        src={table.externeUrl}
        style={{ width: '100%', height: '100%', border: 'none' }}
        sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
        title={table.externeUrl}
        onError={() => setIframeFehler(true)}
      />
    );
  }

  if (table.zeilen === 0) {
    return null;
  }

  const hatKopfzeile = !headerFooterUnterdruecken && (
    table.kopfzeileLinks?.trim()
    || table.kopfzeileMitte?.trim()
    || table.kopfzeileRechts?.trim()
  );

  const hatFusszeile = !headerFooterUnterdruecken && (
    table.fusszeileLinks?.trim()
    || table.fusszeileMitte?.trim()
    || table.fusszeileRechts?.trim()
  );

  return (
    <div ref={containerRef} style={{
      position: 'relative', width: '100%', height: '100%', overflow: 'hidden', boxSizing: 'border-box',
    }}>
      <div ref={contentRef} style={{
        position: 'absolute', left: contentPosition.left, top: contentPosition.top, width: 'fit-content',
        transform: `translate(${contentPosition.translateX}, ${contentPosition.translateY}) scale(${autoFitScale})`,
        transformOrigin: transformOrigin(table.hAlign, table.vAlign),
      }}>
        <div>
          {sheetnamenAnzeigen && table.seitenTitel && (
            <div className="seiten-titel">{table.seitenTitel}</div>
          )}
          {hatKopfzeile && (
            <div className="seitenzeile">
              <span className="links">{table.kopfzeileLinks}</span>
              <span className="mitte">{table.kopfzeileMitte}</span>
              <span className="rechts">{table.kopfzeileRechts}</span>
            </div>
          )}
          <table>
            <colgroup>
              {Array.from({ length: table.spalten }, (_, c) => {
                const breite = table.spaltenBreiten[c];
                const versteckt = breite === 0;
                return (
                  <col key={c} style={versteckt
                    ? { display: 'none' }
                    : { width: toPx(breite ?? 2000) }} />
                );
              })}
            </colgroup>
            <tbody>
              {table.gitter.map((row, r) => (
                <tr key={r}
                    className={r < table.kopfZeilenAnzahl ? 'zeile-kopf' : undefined}
                    style={{ height: toPx(table.zeilenHoehen[r] || 600) }}>
                  {row.map((id, c) =>
                    id && table.spaltenBreiten[c] !== 0
                      ? <Cell key={id} data={table.zellen[id]} />
                      : null
                  )}
                </tr>
              ))}
            </tbody>
          </table>
          {hatFusszeile && (
            <div className="seitenzeile">
              <span className="links">{table.fusszeileLinks}</span>
              <span className="mitte">{table.fusszeileMitte}</span>
              <span className="rechts">{table.fusszeileRechts}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
