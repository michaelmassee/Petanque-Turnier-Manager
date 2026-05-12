// Bestimmt anhand des SSE-Nachrichten-Typs den passenden View-Key. Neue Views
// (overlay, kiosk, stream, …) werden hier ergänzt — App.jsx bleibt unverändert.
export function viewKeyFromTyp(typ) {
  if (!typ) return null;
  if (typ.startsWith('composite_')) return 'composite';
  if (typ.startsWith('startseite_')) return 'startseite';
  if (typ === 'init' || typ === 'diff') return 'einzel';
  return null;
}

// Liefert den aktiven View-Key aus dem Reducer-State. Die Reihenfolge spiegelt
// wider, welche Nachricht der Server zuletzt geschickt hat (genau ein Slice ist
// gesetzt).
export function viewKeyFromState(state) {
  if (state.startseite) return 'startseite';
  if (state.composite)  return 'composite';
  if (state.table && state.table.zeilen > 0) return 'einzel';
  return null;
}
