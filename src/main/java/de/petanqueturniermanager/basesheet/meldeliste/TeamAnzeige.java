package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.Arrays;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Steuert, welche Kennung eines Teams in Spielrunden, Ranglisten und K.-o.-Bäumen angezeigt wird.
 * <p>
 * Die Enum-Namen werden als Property-Werte im Dokument gespeichert und dürfen daher nicht
 * umbenannt werden.
 */
public enum TeamAnzeige {
	/** Teamnummer (Standard). */
	NR("dialog.teamanzeige.nr"),
	/** Vor- und Nachnamen aller Teammitglieder, mit " / " verbunden. */
	SPIELERNAMEN("dialog.teamanzeige.spielernamen"),
	/** Teamname aus der Meldeliste. */
	NAME("dialog.teamanzeige.teamname");

	private final String dialogTextKey;

	TeamAnzeige(String dialogTextKey) {
		this.dialogTextKey = dialogTextKey;
	}

	public boolean istNummer() {
		return this == NR;
	}

	/**
	 * Liefert die tatsächlich darstellbare Anzeige: Ohne Teamname-Spalte in der Meldeliste gibt es
	 * keinen Teamnamen, {@link #NAME} fällt dann auf {@link #SPIELERNAMEN} zurück (z.B. ältere
	 * Dateien, in denen „Teamname" gewählt, die Teamname-Spalte aber deaktiviert ist).
	 */
	public TeamAnzeige effektiv(boolean teamnameAktiv) {
		return this == NAME && !teamnameAktiv ? SPIELERNAMEN : this;
	}

	/** Index in den Auswahllisten der Turnier-Parameterdialoge (Nr, Spieler, Teamname). */
	public short dialogIndex() {
		return (short) ordinal();
	}

	/** Einträge der Auswahllisten in {@link #dialogIndex()}-Reihenfolge. */
	public static String[] dialogEintraege() {
		return Arrays.stream(values()).map(anzeige -> I18n.get(anzeige.dialogTextKey)).toArray(String[]::new);
	}

	/** Gegenstück zu {@link #dialogIndex()}; unbekannte Indizes ergeben {@link #NR}. */
	public static TeamAnzeige ausDialogIndex(short index) {
		TeamAnzeige[] werte = values();
		return index >= 0 && index < werte.length ? werte[index] : NR;
	}
}
