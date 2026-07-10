/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.io.IOException;
import java.util.List;

public interface KiClient {

    String erstelleAntwort(String prompt) throws IOException, InterruptedException;

    /** Ruft die beim Anbieter für diesen API-Key verfügbaren Modell-IDs ab. */
    List<String> listeModelle() throws IOException, InterruptedException;
}
