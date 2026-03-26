import { memo } from 'react';

/**
 * Rendert eine einzelne Tabellenzelle.
 *
 * React.memo verhindert Re-Render, wenn die `data`-Referenz gleich bleibt –
 * massiver Performance-Gewinn bei großen Tabellen mit vielen unveränderten Zellen.
 */
const Cell = memo(function Cell({ data }) {
  if (!data) {
    return <td />;
  }

  const s = data.stil;
  const rot = s.rotationGrad || 0;

  return (
    <td
      colSpan={s.colspan || 1}
      rowSpan={s.rowspan || 1}
      style={{
        backgroundColor: s.hintergrundfarbe || undefined,
        color: s.schriftfarbe || undefined,
        fontWeight: s.fett ? 'bold' : undefined,
        fontStyle: s.kursiv ? 'italic' : undefined,
        fontFamily: s.schriftart || undefined,
        fontSize: s.schriftgroesse > 0 ? s.schriftgroesse + 'pt' : undefined,
        textAlign: s.ausrichtung || undefined,
        verticalAlign: s.vertikaleAusrichtung || 'top',
        whiteSpace: s.zeilenumbruch ? 'normal' : 'nowrap',
        overflow: 'hidden',
        // 90° und 270° über writing-mode (kein Layout-Bruch in Tabellen)
        writingMode: (rot === 90 || rot === 270) ? 'vertical-rl' : undefined,
        // Andere Winkel über transform
        transform: (rot && rot !== 90 && rot !== 270) ? `rotate(${rot}deg)` : undefined,
        borderTop:    s.linienOben    || undefined,
        borderBottom: s.linienUnten   || undefined,
        borderLeft:   s.linienLinks   || undefined,
        borderRight:  s.linienRechts  || undefined,
      }}
    >
      {data.wert}
    </td>
  );
});

export default Cell;
