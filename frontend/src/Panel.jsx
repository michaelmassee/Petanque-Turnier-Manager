import { useState } from 'react';
import Cell from './Cell';

// 1/100 mm → Pixel (gerundet, verhindert Sub-Pixel-Layout-Drift)
const toPx = (v) => Math.round((v || 0) / 37.795) + 'px';

/**
 * Rendert eine einzelne Tabellen-Ansicht (Sheet).
 *
 * @param {Object} props.table - Tabellenzustand (entspricht state.table in App.jsx)
 * @param {boolean} props.sheetnamenAnzeigen - ob Blattnamen in der Kopfzeile angezeigt werden
 */
export default function Panel({ table, sheetnamenAnzeigen }) {
  const [iframeFehler, setIframeFehler] = useState(false);

  if (!table) {
    return null;
  }

  if (table.timerAnzeige != null) {
    const zustand = (table.timerZustand ?? 'INAKTIV').toLowerCase();
    const GUELTIGE_ZUSTAENDE = ['inaktiv', 'laeuft', 'pausiert', 'beendet'];
    const cls = GUELTIGE_ZUSTAENDE.includes(zustand)
      ? `timer-zustand-${zustand}`
      : 'timer-zustand-inaktiv';
    const hintergrundStyle = table.timerHintergrundFarbe
      ? { backgroundColor: table.timerHintergrundFarbe }
      : {};
    return (
      <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
        <div className={`timer-panel ${cls}`} style={{
          ...hintergrundStyle,
          transform: table.zoom !== 100 ? `scale(${table.zoom / 100})` : undefined,
          transformOrigin: table.zentrieren ? 'top center' : 'top left',
        }}>
          {table.timerBezeichnung && (
            <div className="timer-bezeichnung">{table.timerBezeichnung}</div>
          )}
          <div className="timer-anzeige">{table.timerAnzeige ?? '--:--'}</div>
        </div>
      </div>
    );
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

  const hatKopfzeile = table.kopfzeileLinks?.trim()
    || table.kopfzeileMitte?.trim()
    || table.kopfzeileRechts?.trim();

  const hatFusszeile = table.fusszeileLinks?.trim()
    || table.fusszeileMitte?.trim()
    || table.fusszeileRechts?.trim();

  return (
    <div style={{ paddingBottom: '8px', overflow: 'auto', height: '100%', boxSizing: 'border-box' }}>
      <div style={{
        width: 'fit-content',
        margin: table.zentrieren ? '0 auto' : undefined,
      }}>
        <div style={{
          transform: table.zoom !== 100 ? `scale(${table.zoom / 100})` : undefined,
          transformOrigin: table.zentrieren ? 'top center' : 'top left',
        }}>
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
              {Array.from({ length: table.spalten }, (_, c) => (
                <col key={c} style={{ width: toPx(table.spaltenBreiten[c] || 2000) }} />
              ))}
            </colgroup>
            <tbody>
              {table.gitter.map((row, r) => (
                <tr key={r}
                    className={r < table.kopfZeilenAnzahl ? 'zeile-kopf' : undefined}
                    style={{ height: toPx(table.zeilenHoehen[r] || 600) }}>
                  {row.map((id, c) =>
                    id
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
