/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.Optional;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Art der Team-Bildung beim Übernehmen der Mêlée-Anmeldung in die Meldeliste.
 * <ul>
 * <li>{@link #EINFACH}: feste Team-Größe aus der Meldeliste-Formation (Doublette oder Triplette);
 * überzählige Spieler bleiben offen, eine ungerade Teamzahl (Freilos) ist möglich.</li>
 * <li>{@link #DOUBLETTE_OHNE_FREILOS} / {@link #TRIPLETTE_OHNE_FREILOS}: gemischte 2er- und 3er-Teams
 * nach derselben Rechenregel wie im Supermêlée ({@code SuperMeleeTeamRechner}) – es entsteht immer
 * eine gerade Teamzahl. Gemischte Teams brauchen drei Spielerspalten, daher setzen diese Modi die
 * Meldeliste-Formation {@link Formation#TRIPLETTE} voraus (bei Doubletten bleibt Spieler 3 leer).</li>
 * </ul>
 * Der Enum-Name wird als Wert der Auswahl-Property in den Document-Properties gespeichert.
 */
public enum MeleeAnmeldungModus {

	EINFACH("enum.melee.anmeldung.modus.einfach", null),
	DOUBLETTE_OHNE_FREILOS("enum.melee.anmeldung.modus.doublette", SuperMeleeMode.Doublette),
	TRIPLETTE_OHNE_FREILOS("enum.melee.anmeldung.modus.triplette", SuperMeleeMode.Triplette);

	private final String i18nKey;
	private final SuperMeleeMode superMeleeMode;

	MeleeAnmeldungModus(String i18nKey, SuperMeleeMode superMeleeMode) {
		this.i18nKey = i18nKey;
		this.superMeleeMode = superMeleeMode;
	}

	/**
	 * @return Schlüssel für die Speicherung als Document-Property (Enum-Name)
	 */
	public String getKey() {
		return name();
	}

	public String getBezeichnung() {
		return I18n.get(i18nKey);
	}

	/**
	 * @return {@code true} wenn gemischte 2er- und 3er-Teams gebildet werden
	 */
	public boolean istGemischt() {
		return superMeleeMode != null;
	}

	/**
	 * @return Supermêlée-Rechenmodus für die Aufteilung in Doubletten und Triplettes; leer bei
	 *         {@link #EINFACH}
	 */
	public Optional<SuperMeleeMode> superMeleeMode() {
		return Optional.ofNullable(superMeleeMode);
	}

	/**
	 * @param formation Meldeliste-Formation
	 * @return {@code true} wenn dieser Modus mit der Formation verwendbar ist
	 */
	public boolean passtZu(Formation formation) {
		return istGemischt() ? formation == Formation.TRIPLETTE : formation.erlaubtMeleeAnmeldung();
	}

	@Override
	public String toString() {
		return getBezeichnung();
	}
}
