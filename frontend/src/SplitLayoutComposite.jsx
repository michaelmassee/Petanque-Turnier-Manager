import Panel from './Panel';

/**
 * Rendert den zentral konfigurierten Split-Baum als FESTES, komplett zusammengesetztes
 * Layout (keine ziehbaren Split-Panes). Innere Knoten werden als geschachtelte Flex-Boxen
 * mit festen Größenverhältnissen dargestellt; Blätter rendern ihr Panel.
 *
 * Wichtig: Blätter werden bei jedem Render frisch aus der `panels`-Map (per panelId) gelesen,
 * damit die per SSE-Diff live aktualisierten Panel-Inhalte durchfließen, während die
 * Layout-Struktur stabil bleibt.
 *
 * Baum-Format (siehe SplitKnotenAdapter):
 *   Blatt:   { panel: <number> }
 *   Teilung: { richtung: "H"|"V", groesse: <0-100>, links: <node>, rechts: <node> }
 */
export default function SplitLayoutComposite({ knoten, panels, timerAudio }) {
  return renderKnoten(knoten, panels, timerAudio);
}

function renderKnoten(knoten, panels, timerAudio) {
  if (!knoten) {
    return null;
  }
  // Blatt: hat eine panelId, keine Richtung.
  if (knoten.panel !== undefined && knoten.richtung === undefined) {
    return (
      <div style={{ width: '100%', height: '100%', minWidth: 0, minHeight: 0, overflow: 'hidden' }}>
        <Panel table={panels[knoten.panel]} headerFooterUnterdruecken timerAudio={timerAudio} />
      </div>
    );
  }

  const horizontal = knoten.richtung === 'H';
  const linksGroesse = knoten.groesse ?? 50;
  const rechtsGroesse = 100 - linksGroesse;
  const erstesPanelStyle = horizontal
    ? { left: 0, top: 0, width: `${linksGroesse}%`, height: '100%' }
    : { left: 0, top: 0, width: '100%', height: `${linksGroesse}%` };
  const zweitesPanelStyle = horizontal
    ? { left: `${linksGroesse}%`, top: 0, width: `${rechtsGroesse}%`, height: '100%' }
    : { left: 0, top: `${linksGroesse}%`, width: '100%', height: `${rechtsGroesse}%` };
  return (
    <div style={{
      position: 'relative', width: '100%', height: '100%', minWidth: 0, minHeight: 0,
    }}>
      <div style={{
        position: 'absolute',
        boxSizing: 'border-box',
        paddingRight: horizontal ? '1.5px' : 0,
        paddingBottom: horizontal ? 0 : '1.5px',
        minWidth: 0,
        minHeight: 0,
        overflow: 'hidden',
        ...erstesPanelStyle,
      }}>
        {renderKnoten(knoten.links, panels, timerAudio)}
      </div>
      <div style={{
        position: 'absolute',
        boxSizing: 'border-box',
        paddingLeft: horizontal ? '1.5px' : 0,
        paddingTop: horizontal ? 0 : '1.5px',
        minWidth: 0,
        minHeight: 0,
        overflow: 'hidden',
        ...zweitesPanelStyle,
      }}>
        {renderKnoten(knoten.rechts, panels, timerAudio)}
      </div>
    </div>
  );
}
