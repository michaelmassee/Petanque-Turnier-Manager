function escapeCsv(wert) {
  if (wert == null) return '';
  const s = String(wert);
  if (/[",;\r\n]/.test(s)) return '"' + s.replace(/"/g, '""') + '"';
  return s;
}

export function buildCsv(header, rows) {
  const lines = [header.map(escapeCsv).join(';')];
  for (const row of rows) lines.push(row.map(escapeCsv).join(';'));
  return '﻿' + lines.join('\r\n');
}

export function downloadCsv(filename, csv) {
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export async function copyText(text, onToast) {
  try {
    await navigator.clipboard.writeText(text);
    onToast?.('Kopiert');
  } catch {
    onToast?.('Kopieren fehlgeschlagen');
  }
}
