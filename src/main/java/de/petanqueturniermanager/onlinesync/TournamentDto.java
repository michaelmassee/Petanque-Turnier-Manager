/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

/**
 * Turnier-Datentransferobjekt für die PTM-Online-REST-API (Tabelle {@code tournaments}).
 * Feldnamen entsprechen 1:1 den JSON-Schlüsseln der API (camelCase).
 */
public class TournamentDto {

	public String id;
	public String name;
	public String date;
	public String startTime;
	public String location;
	public String description;
	public String type;
	public String formation;
	public String status;
	public Integer maxRegistrations;
	public String registrationDeadline;
	public Integer entryFeeCents;
	public String contactName;
	public String contactEmail;
	public String contactPhone;
	public String visibility;
	public String region;
}
