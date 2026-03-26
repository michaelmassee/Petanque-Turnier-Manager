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
        },
      };
    }
    case 'PATCH': {
      // Nur geänderte Zellen mergen; Gitter und Dimensionen aus dem neuen Modell
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

  return (
    <>
      <div style={{ paddingBottom: '24px' }}>
        <table>
          <colgroup>
            {Array.from({ length: table.spalten }, (_, c) => (
              <col key={c} style={{ width: toPx(table.spaltenBreiten[c] || 2000) }} />
            ))}
          </colgroup>
          <tbody>
            {table.gitter.map((row, r) => (
              <tr key={r} style={{ height: toPx(table.zeilenHoehen[r] || 600) }}>
                {row.map((id, c) =>
                  id
                    ? <Cell key={id} data={table.zellen[id]} />
                    : null  // Merge-Slave → kein <td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
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
