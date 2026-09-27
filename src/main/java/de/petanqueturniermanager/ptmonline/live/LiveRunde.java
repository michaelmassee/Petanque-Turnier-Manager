/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.List;

/**
 * Eine Spielrunde für die Live-Ansicht. Die Rundennummer ist die fortlaufende Nummer im Online-Turnier; alle
 * Partien einer Runde laufen dort gleichzeitig („aktuelle Partie“ = Partie der letzten Runde).
 */
public record LiveRunde(int nr, List<LivePartie> partien) {

    public LiveRunde {
        partien = List.copyOf(partien);
    }
}
