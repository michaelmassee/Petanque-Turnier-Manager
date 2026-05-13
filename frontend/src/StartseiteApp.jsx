import { useEffect, useRef, useState } from 'react';
import './Startseite.css';

/**
 * Turnier-Startseite – statische Begrüßungsseite für Beamer/Monitor.
 * Zeigt Turnierlogo (optional), Turniername und live-aktualisierte
 * Teilnehmerzahl (angemeldet/aktiv). PTM-Branding als Footer.
 */
export default function StartseiteApp({ startseite }) {
  const { turnierlogo, turnierbeschreibung, beschreibungAnimation, hintergrundfarbe,
          anzahlAngemeldet, anzahlAktiv,
          labelAngemeldet, labelAktiv } = startseite;
  const stil = hintergrundfarbe ? { backgroundColor: hintergrundfarbe } : undefined;
  const animation = beschreibungAnimation || 'keine';
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
          <Beschreibung text={turnierbeschreibung} animation={animation} />
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
      <img
        className="startseite-footer-bild"
        src="/images/start-background-footer.png"
        alt=""
      />
    </div>
  );
}

function Beschreibung({ text, animation }) {
  // `key` erzwingt Remount bei Text- oder Animations-Wechsel — sonst würden
  // einmalige CSS-Keyframes (fade/slide) bei Live-Updates nicht erneut starten.
  const key = `${animation}::${text}`;
  if (animation === 'typewriter') {
    return <TypewriterText key={key} text={text} />;
  }
  if (animation === 'marquee') {
    return (
      <div key={key} className="startseite-turnierbeschreibung anim-marquee">
        <span>{text}</span>
      </div>
    );
  }
  return (
    <div key={key} className={`startseite-turnierbeschreibung anim-${animation}`}>
      {text}
    </div>
  );
}

function TypewriterText({ text }) {
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
    <div className="startseite-turnierbeschreibung anim-typewriter">
      {angezeigt}
      <span className="anim-typewriter-caret" aria-hidden="true">|</span>
    </div>
  );
}
