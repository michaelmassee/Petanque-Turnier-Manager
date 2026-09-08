/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

/**
 * Gemeinsame Sicht auf Turniersysteme mit einer <b>konfigurierbaren</b> Meldeliste-Formation
 * (Schweizer, JGJ, KO, Kaskade, Poule, Formule&nbsp;X).
 * <p>
 * Erlaubt formationsabhängige Entscheidungen (z.B. ob die Melee-Anmeldung nutzbar ist), ohne dass
 * die Aufrufer über ein {@code switch} auf {@link TurnierSystem} das jeweilige
 * {@code *KonfigurationSheet} auflösen müssen.
 * <p>
 * Turniersysteme mit fester Formation (Liga, Maastrichter, Supermelee, Trip-Tête) implementieren
 * dieses Interface bewusst <b>nicht</b>.
 */
public interface IFormationKonfiguration {

	/**
	 * @return die im Turnier eingestellte Meldeliste-Formation
	 */
	Formation getMeldeListeFormation();
}
