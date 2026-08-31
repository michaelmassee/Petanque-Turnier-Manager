/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

/**
 * Anmeldungs-Datentransferobjekt für die PTM-Online-REST-API (Tabelle {@code registrations}).
 * Feldnamen entsprechen 1:1 den JSON-Schlüsseln der API (camelCase).
 */
public class RegistrationDto {

	public String id;
	public String tournamentId;
	public String firstName;
	public String lastName;
	public String email;
	public String club;
	public String licenseNr;
	public String partnerFirstName;
	public String partnerLastName;
	public String partnerEmail;
	public String teamName;
	public Integer seedingPosition;
	public String status;
	public String registeredAt;
	public String updatedAt;
}
