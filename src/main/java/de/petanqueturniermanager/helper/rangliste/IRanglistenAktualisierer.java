/*
 * Erstellung: 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.helper.rangliste;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Strategie zur Aktualisierung aller relevanten Ranglisten-Sheets eines
 * Turniersystems unmittelbar vor dem Anlegen der nächsten Spielrunde.
 *
 * <p>Implementierungen kapseln (a) <em>welche</em> Ranglisten zum jeweiligen
 * Turniersystem gehören (Spieltag-, End-, Vorrunden-, Gruppen-Rangliste …)
 * und (b) <em>unter welchen Bedingungen</em> sie aktualisiert werden – etwa
 * indem ein nicht vorhandenes Sheet bewusst übersprungen wird, statt es durch
 * die Hintertür anzulegen.</p>
 *
 * <p>Aufgerufen wird der Aktualisierer aus dem {@code vorNaechsterRunde()}-Hook
 * der Spielrunde-Generatoren (z.&nbsp;B. {@code SchweizerSpielrundeSheetNaechste},
 * {@code SpielrundeSheet_Naechste} im Supermelee).</p>
 */
@FunctionalInterface
public interface IRanglistenAktualisierer {

	/**
	 * Aktualisiert alle relevanten Ranglisten. Implementierungen sind dafür
	 * verantwortlich, Blattschutz korrekt mit {@code try/finally} zu handhaben –
	 * die {@code *SheetUpdate}-Klassen erledigen das bereits intern.
	 */
	void aktualisiereRanglisten() throws GenerateException;
}
