import { useEffect, useState } from 'react';
import './Startseite.css';

/**
 * Turnier-Startseite – statische Begrüßungsseite für Beamer/Monitor.
 * Zeigt Turnierlogo (optional), Turniername und live-aktualisierte
 * Teilnehmerzahl (angemeldet/aktiv). PTM-Branding als Footer.
 */
export default function StartseiteApp({ startseite }) {
  const { turnierlogo, turnierbeschreibung, beschreibungAnimation, beschreibungTextfarbe,
          anzahlAngemeldet, anzahlAktiv,
          labelAngemeldet, labelAktiv,
          turniersystem, turnierStatus, sprueche } = startseite;
  const animation = beschreibungAnimation || 'keine';
  const beschreibungStil = beschreibungTextfarbe ? { color: beschreibungTextfarbe } : undefined;
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
          <Beschreibung text={turnierbeschreibung} animation={animation} stil={beschreibungStil} />
        )}
      </div>
      <div className="startseite-zahlen">
        <div className="zahl-block">
          <div className="zahl-icon" aria-hidden="true">
            <IconAngemeldet />
          </div>
          <div className="zahl-wert">{anzahlAngemeldet}</div>
          <div className="zahl-label">{labelAngemeldet}</div>
          <span className="zahl-label-strich" aria-hidden="true" />
        </div>
        <div className="zahl-block">
          <div className="zahl-icon" aria-hidden="true">
            <IconAktiv />
          </div>
          <div className="zahl-wert">{anzahlAktiv}</div>
          <div className="zahl-label">{labelAktiv}</div>
          <span className="zahl-label-strich" aria-hidden="true" />
        </div>
      </div>
      <StatusLeiste
        turniersystem={turniersystem}
        turnierStatus={turnierStatus}
        sprueche={sprueche}
      />
      <img
        className="startseite-footer-bild"
        src="/images/start-background-footer.png"
        alt=""
      />
    </div>
  );
}

function StatusLeiste({ turniersystem, turnierStatus, sprueche }) {
  const liste = Array.isArray(sprueche) ? sprueche : [];
  const [spruchIndex, setSpruchIndex] = useState(() =>
    liste.length > 0 ? Math.floor(Math.random() * liste.length) : 0,
  );

  useEffect(() => {
    if (liste.length <= 1) return undefined;
    const id = setInterval(() => {
      setSpruchIndex((i) => (i + 1) % liste.length);
    }, 30000);
    return () => clearInterval(id);
  }, [liste.length]);

  if (!turniersystem && !turnierStatus && liste.length === 0) {
    return null;
  }
  const aktuellerSpruch = liste.length > 0 ? liste[spruchIndex % liste.length] : '';
  return (
    <div className="startseite-status">
      {turniersystem && (
        <span className="status-segment status-system">{turniersystem}</span>
      )}
      {turniersystem && turnierStatus && (
        <span className="status-trenner" aria-hidden="true" />
      )}
      {turnierStatus && (
        <span className="status-segment status-fortschritt">{turnierStatus}</span>
      )}
      {(turniersystem || turnierStatus) && aktuellerSpruch && (
        <span className="status-trenner" aria-hidden="true" />
      )}
      {aktuellerSpruch && (
        <span key={spruchIndex} className="status-segment status-spruch">{aktuellerSpruch}</span>
      )}
    </div>
  );
}

function Beschreibung({ text, animation, stil }) {
  // `key` erzwingt Remount bei Text- oder Animations-Wechsel — sonst würden
  // einmalige CSS-Keyframes (fade/slide) bei Live-Updates nicht erneut starten.
  const key = `${animation}::${text}`;
  if (animation === 'typewriter') {
    return <TypewriterText key={key} text={text} stil={stil} />;
  }
  if (animation === 'marquee') {
    return (
      <div key={key} className="startseite-turnierbeschreibung anim-marquee" style={stil}>
        <span>{text}</span>
      </div>
    );
  }
  return (
    <div key={key} className={`startseite-turnierbeschreibung anim-${animation}`} style={stil}>
      {text}
    </div>
  );
}

function IconAngemeldet() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
      <path d="M16 11a3 3 0 1 0-3-3" />
      <circle cx="9" cy="8" r="3" />
      <path d="M2 20c0-3 3-5 7-5s7 2 7 5" />
      <path d="M16 14c3 0 6 2 6 5" />
    </svg>
  );
}

function IconAktiv() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
      <circle cx="12" cy="12" r="9" />
      <path d="M8 12.5l2.8 2.8L16.5 9.5" />
    </svg>
  );
}

function TypewriterText({ text, stil }) {
  const [angezeigt, setAngezeigt] = useState('');
  useEffect(() => {
    let index = 0;
    let intervalId = null;
    let pauseTimeoutId = null;
    const tippen = () => {
      intervalId = setInterval(() => {
        index += 1;
        if (index > text.length) {
          clearInterval(intervalId);
          intervalId = null;
          pauseTimeoutId = setTimeout(() => {
            index = 0;
            setAngezeigt('');
            tippen();
          }, 2500);
          return;
        }
        setAngezeigt(text.slice(0, index));
      }, 60);
    };
    setAngezeigt('');
    tippen();
    return () => {
      if (intervalId) clearInterval(intervalId);
      if (pauseTimeoutId) clearTimeout(pauseTimeoutId);
    };
  }, [text]);
  return (
    <div className="startseite-turnierbeschreibung anim-typewriter" style={stil}>
      {angezeigt}
      <span className="anim-typewriter-caret" aria-hidden="true">|</span>
    </div>
  );
}
