import { useReducer, useEffect, useRef, useState } from 'react';
import CompositeApp from './CompositeApp';
import EinzelApp from './EinzelApp';
import StartseiteApp from './StartseiteApp';
import { liveUrl } from './liveUrls';
import { viewKeyFromState } from './viewResolver';

// Renderer-Map: hier neue Top-Level-Views ergänzen, App.jsx bleibt sonst unverändert.
const VIEW_RENDERERS = {
  einzel:     ({ state }) => <EinzelApp     table={state.table} />,
  composite:  ({ state, timerAudio }) => <CompositeApp
    composite={state.composite}
    timerAudio={timerAudio}
  />,
  startseite: ({ state }) => <StartseiteApp startseite={state.startseite} />,
};

function clientId() {
  const key = 'ptm-live-client-id';
  let id = window.sessionStorage.getItem(key);
  if (!id) {
    id = (window.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`);
    window.sessionStorage.setItem(key, id);
  }
  return id;
}

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
  hAlign: 'kein',
  vAlign: 'kein',
  sheetnamenAnzeigen: false,
  kopfZeilenAnzahl: 0,
};

const GONG_INTERVAL_MS = 5000;

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
    timerSnoozed: msg.timerSnoozed ?? false,
    startseite: msg.startseite ?? null,
    hinweisTitel: msg.hinweisTitel ?? null,
    hinweisText: msg.hinweisText ?? null,
    zoom: msg.zoom ?? 100,
    sichtbarerTabellenAnteil: msg.sichtbarerTabellenAnteil ?? 100,
    hAlign: msg.horizontalAusrichtung ?? 'kein',
    vAlign: msg.vertikalAusrichtung ?? 'kein',
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
    timerSnoozed: msg.timerSnoozed ?? vorher?.timerSnoozed ?? false,
    startseite: msg.startseite ?? null,
    // Hinweis-Status NICHT mit vorher mergen: wenn das Backend eine reguläre Nachricht
    // (init/diff/url/timer) sendet, soll ein vorheriger "fehlend"-Zustand verschwinden.
    hinweisTitel: msg.hinweisTitel ?? null,
    hinweisText: msg.hinweisText ?? null,
    zoom: msg.zoom ?? vorher?.zoom ?? 100,
    sichtbarerTabellenAnteil: msg.sichtbarerTabellenAnteil ?? vorher?.sichtbarerTabellenAnteil ?? 100,
    hAlign: msg.horizontalAusrichtung ?? vorher?.hAlign ?? 'kein',
    vAlign: msg.vertikalAusrichtung ?? vorher?.vAlign ?? 'kein',
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
        startseite: null,
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
          hAlign: msg.horizontalAusrichtung ?? 'kein',
          vAlign: msg.vertikalAusrichtung ?? 'kein',
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
        startseite: null,
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
          hAlign: msg.horizontalAusrichtung ?? state.table.hAlign,
          vAlign: msg.vertikalAusrichtung ?? state.table.vAlign,
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
        startseite: null,
        composite: {
          layout: msg.layout,
          zoom: msg.zoom ?? 100,
          mitHeaderFooter: msg.mitHeaderFooter ?? true,
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
        startseite: null,
        composite: {
          layout: msg.layout ?? state.composite?.layout,
          zoom: msg.zoom ?? state.composite?.zoom ?? 100,
          mitHeaderFooter: msg.mitHeaderFooter ?? state.composite?.mitHeaderFooter ?? true,
          panels: neuerePanels,
        },
      };
    }
    case 'STARTSEITE_INIT': {
      const msg = action.payload;
      return {
        ...state,
        hinweis: null,
        table: leererZustand,
        composite: null,
        startseite: {
          turnierlogo: msg.turnierlogo ?? '',
          turnierbeschreibung: msg.turnierbeschreibung ?? '',
          beschreibungAnimation: msg.beschreibungAnimation ?? 'keine',
          beschreibungTextfarbe: msg.beschreibungTextfarbe ?? '',
          hintergrundfarbe: msg.hintergrundfarbe ?? '',
          anzahlAngemeldet: msg.anzahlAngemeldet ?? 0,
          anzahlAktiv: msg.anzahlAktiv ?? 0,
          labelAngemeldet: msg.labelAngemeldet ?? '',
          labelAktiv: msg.labelAktiv ?? '',
          tagline: msg.tagline ?? '',
          turniersystem: msg.turniersystem ?? '',
          turnierStatus: msg.turnierStatus ?? '',
          sprueche: Array.isArray(msg.sprueche) ? msg.sprueche : [],
          zoom: msg.zoom ?? 100,
        },
      };
    }
    case 'STARTSEITE_PATCH': {
      const msg = action.payload;
      const vorher = state.startseite ?? {};
      // Update-Nachricht trägt alle Felder; "" überschreibt vorhandene Werte
      // (z.B. wenn das Logo gelöscht wurde). Nur undefined bleibt vorher-Wert.
      const mergeStr = (n, v) => (n !== undefined && n !== null ? n : v ?? '');
      const mergeNum = (n, v) => (n !== undefined && n !== null ? n : v ?? 0);
      const mergeArr = (n, v) => (Array.isArray(n) ? n : (Array.isArray(v) ? v : []));
      return {
        ...state,
        hinweis: null,
        startseite: {
          turnierlogo:         mergeStr(msg.turnierlogo,         vorher.turnierlogo),
          turnierbeschreibung: mergeStr(msg.turnierbeschreibung, vorher.turnierbeschreibung),
          beschreibungAnimation: mergeStr(msg.beschreibungAnimation, vorher.beschreibungAnimation || 'keine'),
          beschreibungTextfarbe: mergeStr(msg.beschreibungTextfarbe, vorher.beschreibungTextfarbe),
          hintergrundfarbe:    mergeStr(msg.hintergrundfarbe,    vorher.hintergrundfarbe),
          anzahlAngemeldet:    mergeNum(msg.anzahlAngemeldet,    vorher.anzahlAngemeldet),
          anzahlAktiv:         mergeNum(msg.anzahlAktiv,         vorher.anzahlAktiv),
          labelAngemeldet:     mergeStr(msg.labelAngemeldet,     vorher.labelAngemeldet),
          labelAktiv:          mergeStr(msg.labelAktiv,          vorher.labelAktiv),
          tagline:             mergeStr(msg.tagline,             vorher.tagline),
          turniersystem:       mergeStr(msg.turniersystem,       vorher.turniersystem),
          turnierStatus:       mergeStr(msg.turnierStatus,       vorher.turnierStatus),
          sprueche:            mergeArr(msg.sprueche,            vorher.sprueche),
          zoom:                mergeNum(msg.zoom,                vorher.zoom ?? 100),
        },
      };
    }
    case 'HINWEIS':
      return { ...state, hinweis: action.payload };
    case 'VERBINDUNG_STATUS':
      return { ...state, verbunden: action.payload.verbunden };
    case 'I18N':
      return { ...state, i18n: { ...state.i18n, ...action.payload } };
    default:
      return state;
  }
}

function timerPanelsAusState(state) {
  if (!state.composite?.panels) {
    return state.table?.timerAnzeige != null ? [state.table] : [];
  }
  return Object.values(state.composite.panels)
    .filter((panel) => panel?.timerAnzeige != null);
}

function timerAlarmAktiv(panels) {
  return panels.some((panel) =>
    (panel.timerZustand ?? '').toUpperCase() === 'BEENDET' && !panel.timerSnoozed
  );
}

export default function App() {
  const [state, dispatch] = useReducer(reducer, {
    table: leererZustand,
    ui: {},
    hinweis: null,
    composite: null,
    startseite: null,
    verbunden: true,
    i18n: {},
  });

  const versionRef = useRef(0);
  const clientIdRef = useRef(clientId());
  const gongRef = useRef(null);
  const gongTimerRef = useRef(null);
  const letzterGongStartRef = useRef(0);
  const [timerAudioAktiv, setTimerAudioAktiv] = useState(false);
  const [timerAudioFehler, setTimerAudioFehler] = useState(false);

  const stopGongLoop = () => {
    if (gongTimerRef.current !== null) {
      window.clearInterval(gongTimerRef.current);
      gongTimerRef.current = null;
    }
  };

  const spieleGong = async () => {
    if (!gongRef.current) {
      gongRef.current = new Audio(liveUrl('gong.wav'));
      gongRef.current.preload = 'auto';
    }
    gongRef.current.currentTime = 0;
    await gongRef.current.play();
    letzterGongStartRef.current = Date.now();
  };

  const timerAudioAktivieren = async () => {
    try {
      await spieleGong();
      setTimerAudioAktiv(true);
      setTimerAudioFehler(false);
    } catch (e) {
      setTimerAudioAktiv(false);
      setTimerAudioFehler(true);
      console.log('Timer-Audio nicht verfügbar:', e);
    }
  };

  useEffect(() => {
    let src = null;
    let reconnectTimer = null;
    let abgebrochen = false;
    // Reconnect greift nur noch bei echten Abbrüchen (Netzwerk, Server-Neustart,
    // gestoppte Quelle) — ein gewollter View-Wechsel wird serverseitig live umgebunden
    // und trennt nicht mehr. Schneller Erstversuch, danach exponentieller Backoff.
    const RECONNECT_BASIS_MS = 300;
    const RECONNECT_MAX_MS = 5000;
    let reconnectDelay = RECONNECT_BASIS_MS;

    const verbinden = () => {
      const params = new URLSearchParams();
      params.set('clientId', clientIdRef.current);
      src = new EventSource(liveUrl(`events?${params.toString()}`));

      src.onopen = () => {
        reconnectDelay = RECONNECT_BASIS_MS;
        dispatch({ type: 'VERBINDUNG_STATUS', payload: { verbunden: true } });
      };

      src.onmessage = (e) => {
        const msg = JSON.parse(e.data);

        // Übersetzte Frontend-UI-Texte vom Backend (z.B. „Verbindung getrennt")
        // — sind ggf. in jeder Init-Nachricht enthalten und werden in state.i18n gemerged.
        if (msg.i18n && typeof msg.i18n === 'object') {
          dispatch({ type: 'I18N', payload: msg.i18n });
        }

        if (msg.typ === 'hinweis') {
          dispatch({ type: 'HINWEIS', payload: msg });
          return;
        }

        // Init-Nachrichten umgehen den Versions-Filter und setzen ihn hart auf den
        // neuen Wert. Damit funktioniert auch ein Reconnect nach Server-Restart, bei dem
        // die Server-Versions-Zähler wieder bei 1 starten und sonst alle Diffs verworfen
        // würden, weil sie scheinbar älter sind als der zuletzt gesehene Stand.
        if (msg.typ === 'init') {
          versionRef.current = msg.version ?? 0;
          dispatch({ type: 'INIT', payload: msg });
          return;
        }
        if (msg.typ === 'composite_init') {
          versionRef.current = msg.version ?? 0;
          dispatch({ type: 'COMPOSITE_INIT', payload: msg });
          return;
        }
        if (msg.typ === 'startseite_init') {
          versionRef.current = msg.version ?? 0;
          dispatch({ type: 'STARTSEITE_INIT', payload: msg });
          return;
        }

        if (msg.version <= versionRef.current) {
          return;
        }
        versionRef.current = msg.version;

        if (msg.typ === 'composite_diff') {
          dispatch({ type: 'COMPOSITE_PATCH', payload: msg });
        } else if (msg.typ === 'diff') {
          dispatch({ type: 'PATCH', payload: msg });
        } else if (msg.typ === 'startseite_update') {
          dispatch({ type: 'STARTSEITE_PATCH', payload: msg });
        }
      };

      src.onerror = () => {
        // Stream ist faktisch unterbrochen, egal ob Browser-Auto-Reconnect läuft
        // oder der Stream endgültig geschlossen wurde.
        dispatch({ type: 'VERBINDUNG_STATUS', payload: { verbunden: false } });
        // Falls Browser den Stream als endgültig geschlossen markiert,
        // selbst neu öffnen — der Auto-Reconnect greift sonst nicht mehr.
        if (src && src.readyState === EventSource.CLOSED && !abgebrochen) {
          src.close();
          src = null;
          reconnectTimer = setTimeout(verbinden, reconnectDelay);
          reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_MS);
        }
      };
    };

    verbinden();

    return () => {
      abgebrochen = true;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      if (src) src.close();
    };
  }, []);

  const { table, hinweis, composite, startseite, verbunden, i18n } = state;
  const timerPanels = timerPanelsAusState(state);
  const hatTimerPanel = timerPanels.length > 0;
  const alarmAktiv = timerAlarmAktiv(timerPanels);

  useEffect(() => {
    if (!alarmAktiv || !timerAudioAktiv) {
      stopGongLoop();
      return;
    }
    if (gongTimerRef.current !== null) {
      return;
    }
    if (Date.now() - letzterGongStartRef.current > 1000) {
      spieleGong().catch((e) => {
        setTimerAudioAktiv(false);
        setTimerAudioFehler(true);
        console.log('Timer-Gong konnte nicht gestartet werden:', e);
      });
    }
    gongTimerRef.current = window.setInterval(() => {
      spieleGong().catch((e) => {
        setTimerAudioAktiv(false);
        setTimerAudioFehler(true);
        stopGongLoop();
        console.log('Timer-Gong konnte nicht wiederholt werden:', e);
      });
    }, GONG_INTERVAL_MS);
  }, [alarmAktiv, timerAudioAktiv]);

  useEffect(() => () => stopGongLoop(), []);

  useEffect(() => {
    if (startseite) {
      // Erste Zeile der Beschreibung als Tab-Titel verwenden
      const ersteZeile = startseite.turnierbeschreibung?.split('\n', 1)[0]?.trim();
      document.title = ersteZeile
        ? `${ersteZeile} – PTM Live`
        : 'PTM Live';
    } else if (composite) {
      const erstesPanel = composite.panels[0];
      document.title = erstesPanel?.seitenTitel
        ? `${erstesPanel.seitenTitel} – PTM Live`
        : 'PTM Live';
    } else {
      document.title = table.seitenTitel
        ? `${table.seitenTitel} – PTM Live`
        : 'PTM Live';
    }
  }, [table.seitenTitel, composite, startseite]);

  if (hinweis) {
    return (
      <>
        <div className="hinweis">
          <div className="hinweis-symbol">⏳</div>
          <div className="hinweis-titel">{hinweis.hinweisTitel}</div>
          <div className="hinweis-text">{hinweis.hinweisText}</div>
        </div>
        <Signatur />
        <VerbindungsStatus verbunden={verbunden} i18n={i18n} />
      </>
    );
  }

  const viewKey = viewKeyFromState(state);
  const ViewComponent = VIEW_RENDERERS[viewKey];
  // StartseiteApp bringt eigene PTM-Signatur mit; sonst die globale verwenden.
  const mitSignatur = viewKey !== 'startseite';

  return (
    <>
      {ViewComponent ? <ViewComponent state={state} timerAudio={{
        vorhanden: hatTimerPanel,
        aktiv: timerAudioAktiv,
        fehler: timerAudioFehler,
        aktivieren: timerAudioAktivieren,
        titel: i18n?.tonNichtAktivTitel || 'Kein Ton aktiv',
        hinweis: i18n?.tonNichtAktivHinweis || 'OK drücken / antippen für Ton',
      }} /> : <LeereAnsicht />}
      {mitSignatur && <Signatur links={viewKey === 'composite'} />}
      <VerbindungsStatus verbunden={verbunden} i18n={i18n} />
    </>
  );
}

function LeereAnsicht() {
  return null;
}

function VerbindungsStatus({ verbunden, i18n }) {
  if (verbunden) {
    return null;
  }
  const text = i18n?.verbindungGetrennt || 'Verbindung getrennt';
  return (
    <div id="verbindungs-status" role="status" aria-live="polite">
      <span className="punkt" /> {text}
    </div>
  );
}

function Signatur({ links = false }) {
  return (
    <div id="signatur" className={links ? 'signatur--links' : undefined}>
      <a
        href="https://michaelmassee.github.io/Petanque-Turnier-Manager/"
        target="_blank"
        rel="noreferrer"
      >
        <img
          src={liveUrl('images/petanqueturniermanager-logo-256px.png')}
          alt="Pétanque-Turnier-Manager"
        />
      </a>
    </div>
  );
}
