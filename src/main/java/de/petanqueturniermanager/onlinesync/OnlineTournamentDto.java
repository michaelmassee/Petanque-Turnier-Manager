/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import com.google.gson.annotations.SerializedName;

/**
 * Turnier-Datentransferobjekt für die Liste der zum aktiven API-Key gehörenden PTM-Online-Turniere
 * ({@code GET /api/sync/tournaments}). Feldnamen entsprechen 1:1 den JSON-Schlüsseln der API
 * (camelCase, siehe {@code toPublicTournament()} im PTM-Online-Worker).
 */
public class OnlineTournamentDto {

	/** Vergleichswert für die online frei gewählte Formation ohne feste Teamgröße. */
	public static final String FORMATION_ANDERE = "andere";

	public String id;
	public String name;
	public String date;
	public String type;

	@SerializedName("registrationType")
	public String registrationType;

	/** {@code tete}, {@code doublette} oder {@code triplette}; bei {@link #formationOther} gespeichert als {@code tete}. */
	public String formation;

	@SerializedName("formationOther")
	public boolean formationOther;

	public String status;

	@SerializedName("documentManaged")
	public boolean documentManaged;

	/**
	 * Abgeschlossene Turniere nehmen kein Turnierdokument mehr an (PTM-Online lehnt das Verbinden mit
	 * {@code tournament_finished} ab).
	 */
	public boolean istAbgeschlossen() {
		return "finished".equals(status);
	}

	/** Formation, wie sie mit der des Dokuments verglichen wird: {@code andere} statt des gespeicherten {@code tete}. */
	public String formationsWert() {
		return formationOther ? FORMATION_ANDERE : formation;
	}
}
