import SplitPane from 'react-split-pane';
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

  // Innerer Knoten: SplitPane
  // "H" = horizontale Teilung → SplitPane split="vertical" (senkrechter Trennbalken)
  // "V" = vertikale Teilung   → SplitPane split="horizontal" (waagrechter Trennbalken)
  const split = knoten.richtung === 'H' ? 'vertical' : 'horizontal';
  const defaultSize = `${knoten.groesse ?? 50}%`;

  return (
    <SplitPane split={split} defaultSize={defaultSize} style={{ position: 'relative' }}>
      <SplitPaneComposite
        knoten={knoten.links}
        panels={panels}
      />
      <SplitPaneComposite
        knoten={knoten.rechts}
        panels={panels}
      />
    </SplitPane>
  );
}
