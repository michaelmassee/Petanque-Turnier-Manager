// Panel-Ausrichtung: Mapping von Backend-Keys (kein/links/mitte/rechts bzw.
// kein/oben/mitte/unten) auf CSS-Flex-Werte. Der Tabellen-Container ist ein
// Flex-Container mit `flex-direction: column`; daraus folgt:
//   horizontal (Cross-Achse) → alignItems
//   vertikal   (Main-Achse)  → justifyContent

export const H_KEIN   = 'kein';
export const H_LINKS  = 'links';
export const H_MITTE  = 'mitte';
export const H_RECHTS = 'rechts';

export const V_KEIN  = 'kein';
export const V_OBEN  = 'oben';
export const V_MITTE = 'mitte';
export const V_UNTEN = 'unten';

/** `align-items` für die horizontale Ausrichtung in einem column-Flex-Container. */
export function horizontalAlignItems(h) {
  switch (h) {
    case H_MITTE:  return 'center';
    case H_RECHTS: return 'flex-end';
    default:       return 'flex-start';
  }
}

/** `justify-content` für die vertikale Ausrichtung in einem column-Flex-Container. */
export function vertikalJustifyContent(v) {
  switch (v) {
    case V_MITTE: return 'center';
    case V_UNTEN: return 'flex-end';
    default:      return 'flex-start';
  }
}

/** `transform-origin` für die Zoom-Skalierung, aus beiden Achsen abgeleitet. */
export function transformOrigin(h, v) {
  let xWort;
  switch (h) {
    case H_MITTE:  xWort = 'center'; break;
    case H_RECHTS: xWort = 'right';  break;
    default:       xWort = 'left';
  }
  let yWort;
  switch (v) {
    case V_MITTE: yWort = 'center'; break;
    case V_UNTEN: yWort = 'bottom'; break;
    default:      yWort = 'top';
  }
  return `${yWort} ${xWort}`;
}
