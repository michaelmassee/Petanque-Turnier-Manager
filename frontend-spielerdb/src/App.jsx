import { useEffect, useState } from 'react';
import { SpielerListe } from './SpielerListe.jsx';
import { VereineListe } from './VereineListe.jsx';
import { LabelsListe } from './LabelsListe.jsx';
import { Toast } from './Toast.jsx';

const TABS = [
  { key: 'spieler', label: 'Spieler' },
  { key: 'vereine', label: 'Vereine' },
  { key: 'labels', label: 'Labels' },
];

export function App() {
  const [tab, setTab] = useState('spieler');
  const [stats, setStats] = useState(null);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    fetch('/api/stats')
      .then((r) => r.json())
      .then(setStats)
      .catch(() => setStats({ error: true }));
  }, []);

  const showToast = (text) => {
    setToast({ text, key: Date.now() });
  };

  return (
    <div className="app">
      <header className="header">
        <h1>Spieler-DB Viewer</h1>
        <span style={{ fontSize: 12, opacity: 0.85 }}>read-only</span>
      </header>
      <div className="tabs">
        {TABS.map((t) => (
          <button
            key={t.key}
            className={`tab ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'spieler' && <SpielerListe onToast={showToast} />}
      {tab === 'vereine' && <VereineListe onToast={showToast} />}
      {tab === 'labels' && <LabelsListe onToast={showToast} />}
      <footer className="footer">
        <span>
          {stats && !stats.error
            ? `${stats.spielerCount} Spieler · ${stats.vereineCount} Vereine · ${stats.labelsCount} Labels`
            : 'Statistiken werden geladen…'}
        </span>
        <span className="db-path">
          {stats && !stats.error ? stats.dbPath : ''}
        </span>
      </footer>
      {toast && <Toast key={toast.key} text={toast.text} />}
    </div>
  );
}
