import Cell from './Cell';

// 1/100 mm → Pixel (gerundet, verhindert Sub-Pixel-Layout-Drift)
const toPx = (v) => Math.round((v || 0) / 37.795) + 'px';

/**
 * Einzel-Ansicht (genau ein Sheet pro Port). Erhält die durch den Reducer
 * gepflegte `table`-Struktur aus dem State.
 */
export default function EinzelApp({ table }) {
  const hatKopfzeile = table.kopfzeileLinks?.trim()
    || table.kopfzeileMitte?.trim()
    || table.kopfzeileRechts?.trim();

  const hatFusszeile = table.fusszeileLinks?.trim()
    || table.fusszeileMitte?.trim()
    || table.fusszeileRechts?.trim();

  return (
    <div style={{ paddingBottom: '24px' }}>
      <div style={{
        width: 'fit-content',
        margin: table.zentrieren ? '0 auto' : undefined,
      }}>
        <div style={{
          transform: table.zoom !== 100 ? `scale(${table.zoom / 100})` : undefined,
          transformOrigin: table.zentrieren ? 'top center' : 'top left',
        }}>
          {table.sheetnamenAnzeigen && table.seitenTitel && (
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
