import './Startseite.css';

/**
 * Turnier-Startseite – statische Begrüßungsseite für Beamer/Monitor.
 * Zeigt Turnierlogo (optional), Turniername und live-aktualisierte
 * Teilnehmerzahl (angemeldet/aktiv). PTM-Branding als Footer.
 */
export default function StartseiteApp({ startseite }) {
  const { turnierlogo, turnierbeschreibung, anzahlAngemeldet, anzahlAktiv } = startseite;
  return (
    <div className="startseite">
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
          <div className="zahl-label">angemeldet</div>
        </div>
        <div className="zahl-trenner" aria-hidden="true">/</div>
        <div className="zahl-block">
          <div className="zahl-wert">{anzahlAktiv}</div>
          <div className="zahl-label">aktiv</div>
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
      </div>
    </div>
  );
}
