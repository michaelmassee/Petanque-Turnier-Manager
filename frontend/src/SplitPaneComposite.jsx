import { useEffect, useRef } from 'react';
import { PanelGroup, Panel as ResizablePanel, PanelResizeHandle } from 'react-resizable-panels';
import Panel from './Panel';
import { liveUrl } from './liveUrls';

const SEND_DELAY_MS = 100;
let bekannteGruppen = {};
let sendTimer = null;

function rundeGroessen(sizes) {
  return sizes.map((size) => Math.round(size * 100) / 100);
}

function meldeSplit(pfad, sizes) {
  bekannteGruppen = {
    ...bekannteGruppen,
    [pfad]: rundeGroessen(sizes),
  };
  if (sendTimer) {
    return;
  }
  sendTimer = window.setTimeout(() => {
    sendTimer = null;
    fetch(liveUrl('steuerung/split'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ gruppen: bekannteGruppen }),
    }).catch(() => {
      // Netzwerkfehler sind für die Anzeige nicht fatal; der nächste Drag sendet erneut.
    });
  }, SEND_DELAY_MS);
}

/**
 * Rendert einen Composite View rekursiv als verschachtelten Split-Baum.
 *
 * Der Baum-Knoten hat entweder:
 *   - { panel: number }  → Blatt: rendert Panel mit ID `panel`
 *   - { richtung: "H"|"V", groesse: number, links: Knoten, rechts: Knoten } → Teilung
 *
 * Richtung "H" = horizontale Teilung (links | rechts),
 * Richtung "V" = vertikale Teilung (oben / unten).
 *
 * @param {Object} props.knoten - aktueller Baumknoten
 * @param {Object[]} props.panels - Array aller Panel-Tabellenzustände (Index = Panel-ID)
 * @param {boolean} [props.headerFooterUnterdruecken] - wenn true, blendet alle Panel-eigenen
 *        Kopf-/Fußzeilen aus (globaler Header/Footer wird in App.jsx gerendert)
 */
export default function SplitPaneComposite({
  knoten,
  panels,
  headerFooterUnterdruecken,
  splitGroessen = {},
  syncRolle = '',
  timerAudio,
  pfad = 'R',
}) {
  const groupRef = useRef(null);
  const slaveGroessen = splitGroessen?.[pfad];

  useEffect(() => {
    if (syncRolle !== 'slave' || !Array.isArray(slaveGroessen) || slaveGroessen.length !== 2) {
      return;
    }
    groupRef.current?.setLayout(slaveGroessen);
  }, [syncRolle, slaveGroessen]);

  if (!knoten) return null;

  // Blattknoten
  if (knoten.panel !== undefined) {
    const table = panels[knoten.panel];
    return (
      <div style={{ height: '100%', width: '100%', overflow: 'auto' }}>
        <Panel
          table={table}
          sheetnamenAnzeigen={table?.blattnameAnzeigen ?? false}
          headerFooterUnterdruecken={headerFooterUnterdruecken}
          timerAudio={timerAudio}
        />
      </div>
    );
  }

  // Innerer Knoten: PanelGroup
  // "H" = horizontale Teilung (links | rechts) → direction="horizontal"
  // "V" = vertikale Teilung (oben / unten)     → direction="vertical"
  const direction = knoten.richtung === 'H' ? 'horizontal' : 'vertical';
  const linksGroesse = knoten.groesse ?? 50;
  const rechtsGroesse = 100 - linksGroesse;

  return (
    <PanelGroup
      ref={groupRef}
      id={pfad}
      direction={direction}
      onLayout={syncRolle === 'master' ? (sizes) => meldeSplit(pfad, sizes) : undefined}
      style={{ height: '100%' }}
    >
      <ResizablePanel id={`${pfad}/L-panel`} order={1} defaultSize={linksGroesse}>
        <SplitPaneComposite
          knoten={knoten.links}
          panels={panels}
          splitGroessen={splitGroessen}
          syncRolle={syncRolle}
          timerAudio={timerAudio}
          pfad={`${pfad}/L`}
          headerFooterUnterdruecken={headerFooterUnterdruecken}
        />
      </ResizablePanel>
      <PanelResizeHandle style={{
        background: '#ccc',
        width: direction === 'horizontal' ? '4px' : undefined,
        height: direction === 'vertical' ? '4px' : undefined,
        cursor: direction === 'horizontal' ? 'col-resize' : 'row-resize',
      }} />
      <ResizablePanel id={`${pfad}/R-panel`} order={2} defaultSize={rechtsGroesse}>
        <SplitPaneComposite
          knoten={knoten.rechts}
          panels={panels}
          splitGroessen={splitGroessen}
          syncRolle={syncRolle}
          timerAudio={timerAudio}
          pfad={`${pfad}/R`}
          headerFooterUnterdruecken={headerFooterUnterdruecken}
        />
      </ResizablePanel>
    </PanelGroup>
  );
}
