import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { liveUrl } from './liveUrls';
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
          turniersystem, turnierStatus, sprueche, zoom } = startseite;
  const animation = beschreibungAnimation || 'keine';
  const beschreibungStil = beschreibungTextfarbe ? { color: beschreibungTextfarbe } : undefined;
  const angezeigtAngemeldet = useZahlAnimation(anzahlAngemeldet);
  const angezeigtAktiv = useZahlAnimation(anzahlAktiv);
  const z = zoom ?? 100;
  const benutzerZoom = z / 100;
  const wurzelRef = useRef(null);
  const inhaltRef = useRef(null);
  const footerRef = useRef(null);
  useAutoFit(wurzelRef, inhaltRef, footerRef, benutzerZoom);
  return (
    <>
    <div className="startseite-hintergrund" aria-hidden="true" />
    <div className="startseite" ref={wurzelRef}>
      <div className="startseite-inhalt" ref={inhaltRef}>
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
            <div className="zahl-wert">{angezeigtAngemeldet}</div>
            <ZahlLabel sichtbar={labelAngemeldet} anker={labelAktiv} />
            <span className="zahl-label-strich" aria-hidden="true" />
          </div>
          <div className="zahl-block">
            <div className="zahl-icon" aria-hidden="true">
              <IconAktiv />
            </div>
            <div className="zahl-wert">{angezeigtAktiv}</div>
            <ZahlLabel sichtbar={labelAktiv} anker={labelAngemeldet} />
            <span className="zahl-label-strich" aria-hidden="true" />
          </div>
        </div>
        <StatusLeiste
          turniersystem={turniersystem}
          turnierStatus={turnierStatus}
          sprueche={sprueche}
        />
      </div>
      <img
        ref={footerRef}
        className="startseite-footer-bild"
        src={liveUrl('images/start-background-footer.png')}
        alt=""
      />
    </div>
    </>
  );
}

/**
 * Misst den Inhalts-Wrapper bei scale(1) und rechnet ihn per transform: scale
 * auf die verfügbare Fläche zwischen oberem Rand und Footer-Bild herunter,
 * sodass nichts überlappt. Skaliert nur nach unten (max 1 × Benutzer-Zoom).
 */
function useAutoFit(wurzelRef, inhaltRef, footerRef, benutzerZoom) {
  useLayoutEffect(() => {
    const wurzel = wurzelRef.current;
    const inhalt = inhaltRef.current;
    if (!wurzel || !inhalt) return undefined;

    let rafId = null;
    const anwenden = () => {
      rafId = null;
      inhalt.style.transform = 'translateX(-50%)';
      const natuerlicheBreite = inhalt.offsetWidth;
      const natuerlicheHoehe = inhalt.offsetHeight;
      if (natuerlicheBreite === 0 || natuerlicheHoehe === 0) return;

      const wurzelHoehe = wurzel.clientHeight;
      const wurzelBreite = wurzel.clientWidth;
      const footerHoehe = footerRef.current ? footerRef.current.offsetHeight : 0;
      const minTop = 16;
      const sicherheitsabstand = 8;
      const verfuegbareHoehe = Math.max(0, wurzelHoehe - minTop - footerHoehe - sicherheitsabstand);
      const verfuegbareBreite = Math.max(0, wurzelBreite);

      const fitScale = Math.min(
        verfuegbareBreite / natuerlicheBreite,
        verfuegbareHoehe / natuerlicheHoehe,
        1,
      );
      const skala = Math.max(0.1, fitScale) * benutzerZoom;
      const skalierteHoehe = natuerlicheHoehe * skala;
      const zentrum = (minTop + (wurzelHoehe - footerHoehe)) / 2;
      const top = Math.max(minTop, zentrum - skalierteHoehe / 2);
      inhalt.style.top = `${top}px`;
      inhalt.style.transform = `translateX(-50%) scale(${skala})`;
    };

    const planen = () => {
      if (rafId !== null) return;
      rafId = requestAnimationFrame(anwenden);
    };

    planen();
    const ro = new ResizeObserver(planen);
    ro.observe(wurzel);
    ro.observe(inhalt);
    if (footerRef.current) ro.observe(footerRef.current);
    window.addEventListener('resize', planen);

    return () => {
      if (rafId !== null) cancelAnimationFrame(rafId);
      ro.disconnect();
      window.removeEventListener('resize', planen);
    };
  }, [wurzelRef, inhaltRef, footerRef, benutzerZoom]);
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

/**
 * Label-Komponente mit unsichtbarem Anker-Text: zwingt beide Boxen
 * (Angemeldet/Aktiv) auf identische Breite, indem in jeder Box beide
 * Label-Texte gestapelt gerendert werden — das längere bestimmt die Größe.
 */
function ZahlLabel({ sichtbar, anker }) {
  return (
    <div className="zahl-label">
      <span className="zahl-label-text">{sichtbar}</span>
      <span className="zahl-label-anker" aria-hidden="true">{anker}</span>
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

function useZahlAnimation(zielWert, dauerMs = 2000) {
  const istZahl = Number.isFinite(Number(zielWert));
  const ziel = istZahl ? Number(zielWert) : 0;
  const [angezeigt, setAngezeigt] = useState(0);
  const startWertRef = useRef(0);
  useEffect(() => {
    if (!istZahl) { setAngezeigt(0); startWertRef.current = 0; return undefined; }
    const start = startWertRef.current;
    if (start === ziel) { setAngezeigt(ziel); return undefined; }
    const reduce = typeof window !== 'undefined'
      && window.matchMedia
      && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduce) { setAngezeigt(ziel); startWertRef.current = ziel; return undefined; }
    let rafId = null;
    const startZeit = performance.now();
    const tick = (jetzt) => {
      const t = Math.min(1, (jetzt - startZeit) / dauerMs);
      const eased = 1 - Math.pow(1 - t, 3);
      setAngezeigt(Math.round(start + (ziel - start) * eased));
      if (t < 1) {
        rafId = requestAnimationFrame(tick);
      } else {
        startWertRef.current = ziel;
      }
    };
    rafId = requestAnimationFrame(tick);
    return () => {
      if (rafId) cancelAnimationFrame(rafId);
      startWertRef.current = ziel;
    };
  }, [ziel, istZahl, dauerMs]);
  return istZahl ? angezeigt : zielWert;
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
