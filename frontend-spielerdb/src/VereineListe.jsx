import { useEffect, useMemo, useState } from 'react';
import { buildCsv, copyText, downloadCsv } from './csv.js';

export function VereineListe({ onToast }) {
  const [items, setItems] = useState([]);
  const [filter, setFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [sortAsc, setSortAsc] = useState(true);

  useEffect(() => {
    setLoading(true);
    fetch('/api/vereine')
      .then((r) => r.json())
      .then(setItems)
      .catch(() => onToast?.('Laden fehlgeschlagen'))
      .finally(() => setLoading(false));
  }, [onToast]);

  const angezeigt = useMemo(() => {
    const f = filter.trim().toLowerCase();
    const liste = f
      ? items.filter((v) => v.name.toLowerCase().includes(f))
      : items;
    return [...liste].sort((a, b) => {
      const v = a.name.localeCompare(b.name, 'de', { sensitivity: 'base' });
      return sortAsc ? v : -v;
    });
  }, [items, filter, sortAsc]);

  const exportCsv = () => {
    downloadCsv('vereine.csv',
      buildCsv(['Nr', 'Name'], angezeigt.map((v) => [v.nr, v.name])));
    onToast?.(`${angezeigt.length} Zeilen exportiert`);
  };

  return (
    <>
      <div className="toolbar">
        <input
          type="search"
          placeholder="Vereinsname filtern…"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <button className="btn" onClick={exportCsv}>CSV exportieren</button>
        <span style={{ marginLeft: 'auto', color: '#6b7280' }}>
          {loading ? <span className="spinner" /> : `${angezeigt.length} Vereine`}
        </span>
      </div>
      <div className="table-wrap">
        {loading ? (
          <div className="empty"><span className="spinner" /></div>
        ) : (
          <table>
            <thead>
              <tr>
                <th style={{ width: 80 }}>Nr</th>
                <th className="sortable" onClick={() => setSortAsc(!sortAsc)}>
                  Name <span className="arrow">{sortAsc ? '▲' : '▼'}</span>
                </th>
                <th className="col-actions">Aktionen</th>
              </tr>
            </thead>
            <tbody>
              {angezeigt.map((v) => (
                <tr key={v.nr}>
                  <td>{v.nr}</td>
                  <td>{v.name}</td>
                  <td className="col-actions">
                    <button className="icon-btn" title="Name kopieren"
                            onClick={() => copyText(v.name, onToast)}>📋</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
