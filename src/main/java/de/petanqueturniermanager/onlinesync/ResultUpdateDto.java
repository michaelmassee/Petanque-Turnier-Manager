/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

/**
 * Ein einzelner Ergebnis-/Status-Update-Eintrag für {@code POST /api/sync/tournaments/{id}/results}.
 */
public class ResultUpdateDto {

	public String id;
	public String status;
	public Integer seedingPosition;

	public ResultUpdateDto(String id, String status, Integer seedingPosition) {
		this.id = id;
		this.status = status;
		this.seedingPosition = seedingPosition;
	}
}
