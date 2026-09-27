/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

/**
 * Markiert {@link de.petanqueturniermanager.SheetRunner}, deren Lauf Spielrunden, Ergebnisse oder die Rangliste
 * verändern kann. Nach einem solchen Lauf stößt der SheetRunner die Übertragung an die PTM-Online-Live-Ansicht an
 * ({@link PtmOnlineLiveBeobachter}) – einmal je Kommando, ohne auf das Netz zu warten.
 */
public interface PtmOnlineLiveAusloeser {
}
