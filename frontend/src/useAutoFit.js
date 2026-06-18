import { useLayoutEffect, useState } from 'react';

/**
 * Generischer Auto-Fit-Kern: misst den Inhalt (`contentRef`) in seiner natürlichen Größe
 * und berechnet den Skalierungsfaktor, mit dem er in die verfügbare Fläche des Containers
 * (`containerRef`) passt. Gibt den reinen Zahlen-Skalierungsfaktor zurück – das Anwenden
 * (transform/Positionierung) übernimmt der Aufrufer, sodass sowohl zentrierte Panel-Zellen
 * als auch die Startseite (mit eigener Positionierung) denselben Kern nutzen können.
 *
 * @param {{current: HTMLElement|null}} containerRef sichtbarer Rahmen (begrenzt die Fläche)
 * @param {{current: HTMLElement|null}} contentRef   zu skalierender Inhalt (natürliche Größe)
 * @param {Object} [optionen]
 * @param {number} [optionen.maxScale=1]  obere Schranke des Skalierungsfaktors (Hochskalieren begrenzen)
 * @param {number} [optionen.zoom=1]      zusätzlicher Benutzer-Zoom-Multiplikator
 * @param {(container: HTMLElement) => {breite: number, hoehe: number}} [optionen.verfuegbar]
 *        optionale Berechnung der verfügbaren Fläche (Default: clientWidth/clientHeight)
 * @param {Array} [deps=[]] zusätzliche Abhängigkeiten, die eine Neumessung auslösen
 * @returns {number} Skalierungsfaktor (> 0)
 */
export function useAutoFitScale(containerRef, contentRef, optionen = {}, deps = []) {
  const { maxScale = 1, zoom = 1, verfuegbar } = optionen;
  const [scale, setScale] = useState(1);

  useLayoutEffect(() => {
    const container = containerRef.current;
    const content = contentRef.current;
    if (!container || !content) return undefined;

    let rafId = null;
    const anwenden = () => {
      rafId = null;
      const natuerlicheBreite = content.offsetWidth;
      const natuerlicheHoehe = content.offsetHeight;
      if (natuerlicheBreite === 0 || natuerlicheHoehe === 0) return;
      const flaeche = verfuegbar
        ? verfuegbar(container)
        : { breite: container.clientWidth, hoehe: container.clientHeight };
      if (flaeche.breite <= 0 || flaeche.hoehe <= 0) return;
      const fit = Math.min(
        flaeche.breite / natuerlicheBreite,
        flaeche.hoehe / natuerlicheHoehe,
        maxScale,
      );
      setScale(Math.max(0.05, fit) * zoom);
    };

    const planen = () => {
      if (rafId === null) rafId = requestAnimationFrame(anwenden);
    };

    planen();
    const ro = new ResizeObserver(planen);
    ro.observe(container);
    ro.observe(content);
    window.addEventListener('resize', planen);

    return () => {
      if (rafId !== null) cancelAnimationFrame(rafId);
      ro.disconnect();
      window.removeEventListener('resize', planen);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [containerRef, contentRef, maxScale, zoom, verfuegbar, ...deps]);

  return scale;
}
