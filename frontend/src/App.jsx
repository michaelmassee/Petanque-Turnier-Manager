import { useReducer, useEffect, useRef } from 'react';
import Cell from './Cell';

// 1/100 mm → Pixel (gerundet, verhindert Sub-Pixel-Layout-Drift)
const toPx = (v) => Math.round((v || 0) / 37.795) + 'px';

const leererZustand = {
  zeilen: 0,
  spalten: 0,
  gitter: [],
  zellen: {},
  spaltenBreiten: {},
  zeilenHoehen: {},
  seitenTitel: null,
  kopfzeileLinks: null,
  kopfzeileMitte: null,
  kopfzeileRechts: null,
  fusszeileLinks: null,
  fusszeileMitte: null,
  fusszeileRechts: null,
  zoom: 100,
  zentrieren: false,
  sheetnamenAnzeigen: false,
  kopfZeilenAnzahl: 0,
};

function reducer(state, action) {
  switch (action.type) {
    case 'INIT': {
      // Vollständiger Tabellenzustand – ersetzt state.table, state.ui bleibt erhalten
      const msg = action.payload;
      const zellen = {};
      if (msg.zellen) {
        msg.zellen.forEach((z) => { zellen[z.id] = z; });
      }
      return {
        ...state,
        hinweis: null,
        table: {
          zeilen: msg.zeilen || 0,
          spalten: msg.spalten || 0,
          gitter: msg.gitter || [],
          zellen,
          spaltenBreiten: msg.spaltenBreiten || {},
          zeilenHoehen: msg.zeilenHoehen || {},
          seitenTitel: msg.seitenTitel ?? null,
          kopfzeileLinks: msg.kopfzeileLinks ?? null,
          kopfzeileMitte: msg.kopfzeileMitte ?? null,
          kopfzeileRechts: msg.kopfzeileRechts ?? null,
          fusszeileLinks: msg.fusszeileLinks ?? null,
          fusszeileMitte: msg.fusszeileMitte ?? null,
          fusszeileRechts: msg.fusszeileRechts ?? null,
          zoom: msg.zoom,
          zentrieren: msg.zentrieren,
          sheetnamenAnzeigen: msg.sheetnamenAnzeigen ?? false,
          kopfZeilenAnzahl: msg.kopfZeilenAnzahl ?? 0,
        },
      };
    }
    case 'PATCH': {
      // Nur geänderte Zellen mergen; Gitter, Dimensionen und Kopf-/Fußzeile aus dem neuen Modell
      const msg = action.payload;
      const neueZellen = { ...state.table.zellen };
      if (msg.zellen) {
        msg.zellen.forEach((z) => { neueZellen[z.id] = z; });
      }
      return {
        ...state,
        hinweis: null,
        table: {
          zeilen: msg.zeilen || state.table.zeilen,
          spalten: msg.spalten || state.table.spalten,
          gitter: msg.gitter || state.table.gitter,
          zellen: neueZellen,
          spaltenBreiten: msg.spaltenBreiten || state.table.spaltenBreiten,
          zeilenHoehen: msg.zeilenHoehen || state.table.zeilenHoehen,
          seitenTitel: msg.seitenTitel ?? state.table.seitenTitel,
          kopfzeileLinks: msg.kopfzeileLinks ?? state.table.kopfzeileLinks,
          kopfzeileMitte: msg.kopfzeileMitte ?? state.table.kopfzeileMitte,
          kopfzeileRechts: msg.kopfzeileRechts ?? state.table.kopfzeileRechts,
          fusszeileLinks: msg.fusszeileLinks ?? state.table.fusszeileLinks,
          fusszeileMitte: msg.fusszeileMitte ?? state.table.fusszeileMitte,
          fusszeileRechts: msg.fusszeileRechts ?? state.table.fusszeileRechts,
          zoom: msg.zoom ?? state.table.zoom,
          zentrieren: msg.zentrieren ?? state.table.zentrieren,
          sheetnamenAnzeigen: msg.sheetnamenAnzeigen ?? state.table.sheetnamenAnzeigen,
          kopfZeilenAnzahl: msg.kopfZeilenAnzahl ?? state.table.kopfZeilenAnzahl,
        },
      };
    }
    case 'HINWEIS':
      return { ...state, hinweis: action.payload };
    default:
      return state;
  }
}

export default function App() {
  const [state, dispatch] = useReducer(reducer, {
    table: leererZustand,
    ui: {},          // lokaler UI-Zustand (Scroll, Zoom …) – wird von INIT nicht berührt
    hinweis: null,   // { titel, text } oder null
  });

  // Version-Guard: veraltete oder doppelte Events ignorieren
  const versionRef = useRef(0);

  useEffect(() => {
    const src = new EventSource('/events');

    src.onmessage = (e) => {
      const msg = JSON.parse(e.data);

      // Hinweis-Nachrichten haben keine Version
      if (msg.typ === 'hinweis') {
        dispatch({ type: 'HINWEIS', payload: msg });
        return;
      }

      if (msg.version <= versionRef.current) {
        return; // Out-of-order oder Duplikat ignorieren
      }
      versionRef.current = msg.version;
      dispatch({ type: msg.typ === 'init' ? 'INIT' : 'PATCH', payload: msg });
    };

    src.onerror = () => {
      // EventSource reconnectet automatisch; nach Reconnect sendet der Server init
    };

    return () => src.close();
  }, []);

  const { table, hinweis } = state;

  useEffect(() => {
    document.title = table.seitenTitel
      ? `${table.seitenTitel} – PTM Live`
      : 'PTM Live';
  }, [table.seitenTitel]);

  if (hinweis) {
    return (
      <>
        <div className="hinweis">
          <div className="hinweis-symbol">⏳</div>
          <div className="hinweis-titel">{hinweis.hinweisTitel}</div>
          <div className="hinweis-text">{hinweis.hinweisText}</div>
        </div>
        <Signatur />
      </>
    );
  }

  if (table.zeilen === 0) {
    return (
      <>
        <div className="hinweis">
          <div className="hinweis-symbol">🏆</div>
          <div className="hinweis-titel">Petanque-Turnier-Manager</div>
          <div className="hinweis-text">Warte auf Turnierdaten…</div>
        </div>
        <Signatur />
      </>
    );
  }

  const hatKopfzeile = table.kopfzeileLinks?.trim()
    || table.kopfzeileMitte?.trim()
    || table.kopfzeileRechts?.trim();

  const hatFusszeile = table.fusszeileLinks?.trim()
    || table.fusszeileMitte?.trim()
    || table.fusszeileRechts?.trim();

  return (
    <>
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
                {Array.from({ length: table.spalten }, (_, c) => (
                  <col key={c} style={{ width: toPx(table.spaltenBreiten[c] || 2000) }} />
                ))}
              </colgroup>
              <tbody>
                {table.gitter.map((row, r) => (
                  <tr key={r}
                      className={r < table.kopfZeilenAnzahl ? 'zeile-kopf' : (r % 2 === 0 ? 'zeile-gerade' : 'zeile-ungerade')}
                      style={{ height: toPx(table.zeilenHoehen[r] || 600) }}>
                    {row.map((id, c) =>
                      id
                        ? <Cell key={id} data={table.zellen[id]} />
                        : null  // Merge-Slave → kein <td>
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
      <Signatur />
    </>
  );
}

function Signatur() {
  return (
    <div id="signatur">
      * Pétanque-Turnier-Manager *<br />
      michael@massee.de
    </div>
  );
}
