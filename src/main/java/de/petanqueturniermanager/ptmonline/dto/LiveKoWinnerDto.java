/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

/**
 * Sieger eines KO-Felds für {@code PUT /api/sync/tournaments/{id}/ranking} ({@code koWinners}): zweiter Teil der
 * Maastrichter-Endrangliste neben der Vorrunden-Rangliste.
 *
 * @param label Bezeichnung des Felds (z.&nbsp;B. „A-Finale“)
 */
public record LiveKoWinnerDto(String label, List<String> registrationIds) {

    public LiveKoWinnerDto {
        registrationIds = List.copyOf(registrationIds);
    }
}
