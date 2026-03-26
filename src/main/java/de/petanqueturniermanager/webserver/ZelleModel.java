package de.petanqueturniermanager.webserver;

/**
 * Repräsentiert eine einzelne Tabellenzelle mit Inhalt und Stil.
 * <p>
 * Nur Master-Zellen (Anker einer Merge-Gruppe) werden als {@code ZelleModel} erstellt.
 * Slave-Positionen erscheinen als {@code null} im Gitter von {@link TabelleModel}
 * und erzeugen kein {@code <td>} im React-Frontend.
 */
public record ZelleModel(
        String id,      // "cell-{row}-{col}" (0-basiert, Druckbereich-relativ)
        String wert,
        StyleModel stil
) {}
