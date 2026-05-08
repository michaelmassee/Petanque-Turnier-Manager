import { useEffect, useMemo, useState } from 'react';
import { buildCsv, copyText, downloadCsv } from './csv.js';

const SPALTEN = [
  { key: 'nr', label: 'Nr', sortable: true, num: true },
  { key: 'vorname', label: 'Vorname', sortable: true },
  { key: 'nachname', label: 'Nachname', sortable: true },
  { key: 'vereinName', label: 'Verein', sortable: true },
  { key: 'lizenznr', label: 'Lizenznr', sortable: true },
  { key: 'labelNamen', label: 'Labels', sortable: false },
];

function vergleiche(a, b, key, num) {
  const va = a[key];
  const vb = b[key];
  if (va == null && vb == null) return 0;
  if (va == null) return 1;
  if (vb == null) return -1;
  if (num) return va - vb;
  return String(va).localeCompare(String(vb), 'de', { sensitivity: 'base' });
}

export function SpielerListe({ onToast }) {
  const [items, setItems] = useState([]);
  const [vereine, setVereine] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');
  const [vereinFilter, setVereinFilter] = useState('');
  const [sortKey, setSortKey] = useState('nachname');
  const [sortAsc, setSortAsc] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      fetch('/api/spieler?limit=1000').then((r) => r.json()),
      fetch('/api/vereine').then((r) => r.json()),
    ])
      .then(([spielerRes, vereineRes]) => {
        setItems(spielerRes.items || []);
        setTotal(spielerRes.total || 0);
        setVereine(vereineRes || []);
      })
      .catch(() => onToast?.('Laden fehlgeschlagen'))
      .finally(() => setLoading(false));
  }, [onToast]);

  const angezeigt = useMemo(() => {
    const f = filter.trim().toLowerCase();
    let liste = items;
    if (f) {
      liste = liste.filter((s) =>
        [s.vorname, s.nachname, s.vereinName, s.lizenznr]
          .filter(Boolean)
          .some((v) => v.toLowerCase().includes(f))
      );
    }
    if (vereinFilter) {
      liste = liste.filter((s) => String(s.vereinNr ?? '') === vereinFilter);
    }
    const sortiert = [...liste].sort((a, b) => {
      const spalte = SPALTEN.find((c) => c.key === sortKey);
      const v = vergleiche(a, b, sortKey, !!spalte?.num);
      return sortAsc ? v : -v;
    });
    return sortiert;
  }, [items, filter, vereinFilter, sortKey, sortAsc]);

  const klickSort = (key) => {
    if (sortKey === key) setSortAsc(!sortAsc);
    else { setSortKey(key); setSortAsc(true); }
  };

  const exportCsv = () => {
    const header = ['Nr', 'Vorname', 'Nachname', 'Verein', 'Lizenznr', 'Labels'];
    const rows = angezeigt.map((s) => [
      s.nr, s.vorname, s.nachname, s.vereinName ?? '',
      s.lizenznr ?? '', (s.labelNamen ?? []).join(', '),
    ]);
    downloadCsv('spieler.csv', buildCsv(header, rows));
    onToast?.(`${rows.length} Zeilen exportiert`);
  };

  const zeileText = (s) => [
    s.nr, s.vorname, s.nachname, s.vereinName ?? '',
    s.lizenznr ?? '', (s.labelNamen ?? []).join(', '),
  ].join('\t');

  return (
    <>
      <div className="toolbar">
        <input
          type="search"
          placeholder="Suche (Vor-, Nachname, Verein, Lizenz)…"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <select value={vereinFilter} onChange={(e) => setVereinFilter(e.target.value)}>
          <option value="">Alle Vereine</option>
          {vereine.map((v) => (
            <option key={v.nr} value={String(v.nr)}>{v.name}</option>
          ))}
        </select>
        <button className="btn" onClick={exportCsv}>CSV exportieren</button>
        <span style={{ marginLeft: 'auto', color: '#6b7280' }}>
          {loading ? <span className="spinner" /> : `${angezeigt.length} von ${total} angezeigt`}
        </span>
      </div>
      <div className="table-wrap">
        {loading ? (
          <div className="empty"><span className="spinner" /></div>
        ) : angezeigt.length === 0 ? (
          <div className="empty">Keine Treffer</div>
        ) : (
          <table>
            <thead>
              <tr>
                {SPALTEN.map((c) => (
                  <th
                    key={c.key}
                    className={c.sortable ? 'sortable' : ''}
                    onClick={c.sortable ? () => klickSort(c.key) : undefined}
                  >
                    {c.label}
                    {c.sortable && sortKey === c.key && (
                      <span className="arrow">{sortAsc ? '▲' : '▼'}</span>
                    )}
                  </th>
                ))}
                <th className="col-actions">Aktionen</th>
              </tr>
            </thead>
            <tbody>
              {angezeigt.map((s) => (
                <tr key={s.nr}>
                  <td>{s.nr}</td>
                  <td>{s.vorname}</td>
                  <td>{s.nachname}</td>
                  <td>{s.vereinName ?? ''}</td>
                  <td>{s.lizenznr ?? ''}</td>
                  <td>
                    {(s.labelNamen ?? []).map((n, i) => (
                      <span key={i} className="badge">{n}</span>
                    ))}
                  </td>
                  <td className="col-actions">
                    <button className="icon-btn" title="Zeile kopieren"
                            onClick={() => copyText(zeileText(s), onToast)}>📋</button>
                    <button className="icon-btn" title="Name kopieren"
                            onClick={() => copyText(`${s.vorname} ${s.nachname}`, onToast)}>👤</button>
                    {s.lizenznr && (
                      <button className="icon-btn" title="Lizenznr kopieren"
                              onClick={() => copyText(s.lizenznr, onToast)}>#</button>
                    )}
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
