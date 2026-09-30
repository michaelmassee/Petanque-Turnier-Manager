/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Ordnet das lokale {@link TurnierSystem} den passenden {@code type}-/{@code registrationType}-
 * Werten der PTM-Online-API zu (Tabelle {@code tournaments}). Wird sowohl beim Filtern der
 * Online-Turnier-Auswahlliste als auch für spätere Metadaten-Abgleiche verwendet.
 * <p>
 * Supermelee ist serverseitig <b>kein eigener {@code type}-Wert mehr</b> (Migration
 * {@code 0031_registration_type_supermelee.sql}): dort gilt {@code type="rangliste"} <b>und</b>
 * {@code registrationType="supermelee"}. Für alle anderen Turniersysteme unterscheidet
 * {@code registrationType} Einzel-Anmeldung ({@code "melee"}, lokal die Mêlée-Anmeldeliste) von
 * Team-Anmeldung ({@code "forme"}, lokal die Meldeliste).
 */
public final class TurnierSystemOnlineTypMapping {

	private TurnierSystemOnlineTypMapping() {}

	/** {@code type}-Wert der PTM-Online-API für das gegebene {@link TurnierSystem}, falls unterstützt. */
	public static Optional<String> onlineTyp(TurnierSystem ts) {
		String typ = switch (ts) {
			case SUPERMELEE -> "rangliste";
			case LIGA -> "liga";
			case MAASTRICHTER -> "maastrichter";
			case SCHWEIZER -> "schweizer";
			case JGJ -> "jeder_gegen_jeden";
			case KO -> "ko";
			case POULE -> "poule_ab";
			case KASKADE -> "kaskaden";
			case FORMULEX -> "formule_x";
			case TRIPTETE -> "trip_tete";
			case KEIN -> null;
		};
		return Optional.ofNullable(typ);
	}

	/**
	 * {@code registrationType}-Wert, der zusätzlich zum {@code type} passen muss: Supermêlée immer
	 * {@code supermelee}; sonst entscheidet die Anmeldeart des Dokuments – eine Mêlée-Anmeldeliste passt nur
	 * zu {@code melee}, eine Team-Meldeliste nur zu {@code forme}.
	 */
	public static String onlineRegistrationTyp(TurnierSystem ts, boolean meleeAnmeldung) {
		if (ts == TurnierSystem.SUPERMELEE) {
			return "supermelee";
		}
		return meleeAnmeldung ? "melee" : "forme";
	}

	/**
	 * Ob das online gemeldete Turnier ({@code type}/{@code registrationType}) zum lokalen {@link TurnierSystem}
	 * und zur Anmeldeart des Dokuments passt.
	 */
	public static boolean passtZu(TurnierSystem ts, boolean meleeAnmeldung, OnlineTournamentDto online) {
		Optional<String> erwarteterTyp = onlineTyp(ts);
		if (erwarteterTyp.isEmpty() || !erwarteterTyp.get().equals(online.type)) {
			return false;
		}
		return onlineRegistrationTyp(ts, meleeAnmeldung).equals(online.registrationType);
	}
}
