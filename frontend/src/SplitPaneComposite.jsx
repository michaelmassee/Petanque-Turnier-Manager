import { PanelGroup, Panel as ResizablePanel, PanelResizeHandle } from 'react-resizable-panels';
import Panel from './Panel';

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
 */
export default function SplitPaneComposite({ knoten, panels }) {
  if (!knoten) return null;

  // Blattknoten
  if (knoten.panel !== undefined) {
    const table = panels[knoten.panel];
    return (
      <div style={{ height: '100%', width: '100%', overflow: 'auto' }}>
        <Panel table={table} sheetnamenAnzeigen={table?.blattnameAnzeigen ?? false} />
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
    <PanelGroup direction={direction} style={{ height: '100%' }}>
      <ResizablePanel defaultSize={linksGroesse}>
        <SplitPaneComposite
          knoten={knoten.links}
          panels={panels}
        />
      </ResizablePanel>
      <PanelResizeHandle style={{
        background: '#ccc',
        width: direction === 'horizontal' ? '4px' : undefined,
        height: direction === 'vertical' ? '4px' : undefined,
        cursor: direction === 'horizontal' ? 'col-resize' : 'row-resize',
      }} />
      <ResizablePanel defaultSize={rechtsGroesse}>
        <SplitPaneComposite
          knoten={knoten.rechts}
          panels={panels}
        />
      </ResizablePanel>
    </PanelGroup>
  );
}
