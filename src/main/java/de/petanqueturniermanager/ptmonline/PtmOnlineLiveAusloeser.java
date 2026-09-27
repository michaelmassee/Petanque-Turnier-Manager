/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

/**
 * Markiert {@link de.petanqueturniermanager.SheetRunner}, deren Lauf Spielrunden, Ergebnisse oder die Rangliste
 * verändern kann. Nach einem solchen Lauf überträgt der SheetRunner den Stand an die PTM-Online-Live-Ansicht
 * ({@link PtmOnlineLiveSync}) – einmal je Kommando, auch wenn der Runner intern weitere Runner synchron aufruft.
 */
public interface PtmOnlineLiveAusloeser {
}
