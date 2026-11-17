/**
 * Ankündigung der Turnierleitung als Banner am unteren Bildrand über jeder Ansicht.
 * Ein leerer Text blendet das Banner aus.
 */
export default function AnkuendigungBanner({ ankuendigung }) {
  const text = ankuendigung?.text?.trim();
  if (!text) {
    return null;
  }
  return (
    <div id="ankuendigung-banner" role="status" aria-live="polite">
      <span className="ankuendigung-symbol" aria-hidden="true">📢</span>
      <div className="ankuendigung-inhalt">
        {ankuendigung.titel && <div className="ankuendigung-titel">{ankuendigung.titel}</div>}
        <div className="ankuendigung-text">{text}</div>
      </div>
    </div>
  );
}
