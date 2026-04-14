import { useReducer, useEffect, useRef } from 'react';
import Cell from './Cell';
import Panel from './Panel';
import SplitPaneComposite from './SplitPaneComposite';

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

function panelAusNachricht(msg) {
  const zellen = {};
  if (msg.zellen) {
    msg.zellen.forEach((z) => { zellen[z.id] = z; });
  }
  return {
    panelId: msg.panelId,
    externeUrl: msg.externeUrl ?? null,
    timerAnzeige: msg.timerAnzeige ?? null,
    timerZustand: msg.timerZustand ?? null,
    timerBezeichnung: msg.timerBezeichnung ?? null,
    timerHintergrundFarbe: msg.timerHintergrundFarbe ?? null,
    zoom: msg.zoom ?? 100,
    zentrieren: msg.zentriert ?? false,
    blattnameAnzeigen: msg.blattnameAnzeigen ?? false,
    seitenTitel: msg.seitenTitel ?? null,
    zeilen: msg.zeilen || 0,
    spalten: msg.spalten || 0,
    gitter: msg.gitter || [],
    zellen,
    spaltenBreiten: msg.spaltenBreiten || {},
    zeilenHoehen: msg.zeilenHoehen || {},
    kopfzeileLinks: msg.kopfzeileLinks ?? null,
    kopfzeileMitte: msg.kopfzeileMitte ?? null,
    kopfzeileRechts: msg.kopfzeileRechts ?? null,
    fusszeileLinks: msg.fusszeileLinks ?? null,
    fusszeileMitte: msg.fusszeileMitte ?? null,
    fusszeileRechts: msg.fusszeileRechts ?? null,
    kopfZeilenAnzahl: msg.kopfZeilenAnzahl ?? 0,
  };
}

function panelDiffAusNachricht(msg, vorher) {
  const neueZellen = { ...(vorher?.zellen || {}) };
  if (msg.zellen) {
    msg.zellen.forEach((z) => { neueZellen[z.id] = z; });
  }
  return {
    panelId: msg.panelId,
    externeUrl: msg.externeUrl ?? vorher?.externeUrl ?? null,
    timerAnzeige: msg.timerAnzeige ?? vorher?.timerAnzeige ?? null,
    timerZustand: msg.timerZustand ?? vorher?.timerZustand ?? null,
    timerBezeichnung: msg.timerBezeichnung ?? vorher?.timerBezeichnung ?? null,
    timerHintergrundFarbe: msg.timerHintergrundFarbe ?? vorher?.timerHintergrundFarbe ?? null,
    zoom: msg.zoom ?? vorher?.zoom ?? 100,
    zentrieren: msg.zentriert ?? vorher?.zentrieren ?? false,
    blattnameAnzeigen: msg.blattnameAnzeigen ?? vorher?.blattnameAnzeigen ?? false,
    seitenTitel: msg.seitenTitel ?? vorher?.seitenTitel ?? null,
    zeilen: msg.zeilen || vorher?.zeilen || 0,
    spalten: msg.spalten || vorher?.spalten || 0,
    gitter: msg.gitter || vorher?.gitter || [],
    zellen: neueZellen,
    spaltenBreiten: msg.spaltenBreiten || vorher?.spaltenBreiten || {},
    zeilenHoehen: msg.zeilenHoehen || vorher?.zeilenHoehen || {},
    kopfzeileLinks: msg.kopfzeileLinks ?? vorher?.kopfzeileLinks ?? null,
    kopfzeileMitte: msg.kopfzeileMitte ?? vorher?.kopfzeileMitte ?? null,
    kopfzeileRechts: msg.kopfzeileRechts ?? vorher?.kopfzeileRechts ?? null,
    fusszeileLinks: msg.fusszeileLinks ?? vorher?.fusszeileLinks ?? null,
    fusszeileMitte: msg.fusszeileMitte ?? vorher?.fusszeileMitte ?? null,
    fusszeileRechts: msg.fusszeileRechts ?? vorher?.fusszeileRechts ?? null,
    kopfZeilenAnzahl: msg.kopfZeilenAnzahl ?? vorher?.kopfZeilenAnzahl ?? 0,
  };
}

function reducer(state, action) {
  switch (action.type) {
    case 'INIT': {
      const msg = action.payload;
      const zellen = {};
      if (msg.zellen) {
        msg.zellen.forEach((z) => { zellen[z.id] = z; });
      }
      return {
        ...state,
        hinweis: null,
        composite: null,
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
      const msg = action.payload;
      const neueZellen = { ...state.table.zellen };
      if (msg.zellen) {
        msg.zellen.forEach((z) => { neueZellen[z.id] = z; });
      }
      return {
        ...state,
        hinweis: null,
        composite: null,
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
    case 'COMPOSITE_INIT': {
      const msg = action.payload;
      const panels = {};
      if (msg.panels) {
        msg.panels.forEach((p) => { panels[p.panelId] = panelAusNachricht(p); });
      }
      return {
        ...state,
        hinweis: null,
        table: leererZustand,
        composite: {
          layout: msg.layout,
          zoom: msg.zoom ?? 100,
          panels,
        },
      };
    }
    case 'COMPOSITE_PATCH': {
      const msg = action.payload;
      const neuerePanels = { ...(state.composite?.panels || {}) };
      if (msg.panels) {
        msg.panels.forEach((p) => {
          neuerePanels[p.panelId] = panelDiffAusNachricht(p, neuerePanels[p.panelId]);
        });
      }
      return {
        ...state,
        hinweis: null,
        composite: {
          layout: msg.layout ?? state.composite?.layout,
          zoom: msg.zoom ?? state.composite?.zoom ?? 100,
          panels: neuerePanels,
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
    ui: {},
    hinweis: null,
    composite: null,
  });

  const versionRef = useRef(0);

  useEffect(() => {
    const src = new EventSource('/events');

    src.onmessage = (e) => {
      const msg = JSON.parse(e.data);

      if (msg.typ === 'hinweis') {
        dispatch({ type: 'HINWEIS', payload: msg });
        return;
      }

      if (msg.version <= versionRef.current) {
        return;
      }
      versionRef.current = msg.version;

      if (msg.typ === 'composite_init') {
        dispatch({ type: 'COMPOSITE_INIT', payload: msg });
      } else if (msg.typ === 'composite_diff') {
        dispatch({ type: 'COMPOSITE_PATCH', payload: msg });
      } else if (msg.typ === 'init') {
        dispatch({ type: 'INIT', payload: msg });
      } else if (msg.typ === 'diff') {
        dispatch({ type: 'PATCH', payload: msg });
      }
    };

    src.onerror = () => {
      // EventSource reconnectet automatisch; nach Reconnect sendet der Server init
    };

    return () => src.close();
  }, []);

  const { table, hinweis, composite } = state;

  useEffect(() => {
    if (composite) {
      // Titel aus erstem Panel nehmen
      const erstesPanel = composite.panels[0];
      document.title = erstesPanel?.seitenTitel
        ? `${erstesPanel.seitenTitel} – PTM Live`
        : 'PTM Live';
    } else {
      document.title = table.seitenTitel
        ? `${table.seitenTitel} – PTM Live`
        : 'PTM Live';
    }
  }, [table.seitenTitel, composite]);

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

  // Composite View
  if (composite && composite.layout) {
    return (
      <div style={{ width: '100vw', height: '100vh', overflow: 'hidden' }}>
        <SplitPaneComposite
          knoten={composite.layout}
          panels={composite.panels}
        />
        <Signatur />
      </div>
    );
  }

  // Einzel-Ansicht (bestehend, unverändert)
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
      <Signatur />
    </>
  );
}

function Signatur() {
  return (
    <div id="signatur">
      <a
        href="https://michaelmassee.github.io/Petanque-Turnier-Manager/"
        target="_blank"
        rel="noreferrer"
      >
        <img
          src="/images/petanqueturniermanager-logo-256px.png"
          alt="Pétanque-Turnier-Manager"
        />
      </a>
    </div>
  );
}
