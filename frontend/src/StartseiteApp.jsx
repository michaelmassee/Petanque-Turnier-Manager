import './Startseite.css';

/**
 * Turnier-Startseite – statische Begrüßungsseite für Beamer/Monitor.
 * Zeigt Turnierlogo (optional), Turniername und live-aktualisierte
 * Teilnehmerzahl (angemeldet/aktiv). PTM-Branding als Footer.
 */
export default function StartseiteApp({ startseite }) {
  const { turnierlogo, turnierbeschreibung, hintergrundfarbe,
          anzahlAngemeldet, anzahlAktiv,
          labelAngemeldet, labelAktiv, tagline } = startseite;
  const stil = hintergrundfarbe ? { background: hintergrundfarbe } : undefined;
  return (
    <div className="startseite" style={stil}>
      <div className="startseite-kopf">
        {turnierlogo && (
          <img
            className="startseite-turnierlogo"
            src={turnierlogo}
            alt=""
            onError={(e) => { e.currentTarget.style.display = 'none'; }}
          />
        )}
        {turnierbeschreibung && (
          <div className="startseite-turnierbeschreibung">{turnierbeschreibung}</div>
        )}
      </div>
      <div className="startseite-zahlen">
        <div className="zahl-block">
          <div className="zahl-wert">{anzahlAngemeldet}</div>
          <div className="zahl-label">{labelAngemeldet}</div>
        </div>
        <div className="zahl-trenner" aria-hidden="true">/</div>
        <div className="zahl-block">
          <div className="zahl-wert">{anzahlAktiv}</div>
          <div className="zahl-label">{labelAktiv}</div>
        </div>
      </div>
      <div className="startseite-fuss">
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
        {tagline && <div className="startseite-tagline">{tagline}</div>}
      </div>
    </div>
  );
}
