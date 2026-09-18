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

	public String id;
	public String name;
	public String date;
	public String type;

	@SerializedName("registrationType")
	public String registrationType;

	public String status;

	@SerializedName("documentManaged")
	public boolean documentManaged;
}
