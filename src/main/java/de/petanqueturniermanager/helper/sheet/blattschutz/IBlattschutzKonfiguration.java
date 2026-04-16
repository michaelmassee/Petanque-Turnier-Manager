/**
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet.blattschutz;

import java.util.List;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Strategie-Interface für die Blattschutz-Konfiguration eines Turniersystems.
 * <p>
 * Jedes Turniersystem implementiert dieses Interface, um festzulegen, welche
 * Sheets geschützt werden und welche Zellbereiche trotz Schutz editierbar bleiben.
 */
public interface IBlattschutzKonfiguration {

    /**
     * Liefert die Liste aller zu schützenden Sheets mit ihren editierbaren Bereichen.
     * <p>
     * Sheets ohne editierbare Bereiche werden vollständig gesperrt.
     * Nicht vorhandene Sheets werden übersprungen.
     * <p>
     * Diese Methode wird einmalig aufgerufen – das Ergebnis wird intern durchgereicht,
     * um unnötige Neuberechnungen zu vermeiden.
     *
     * @param ws aktuelles Spreadsheet
     * @return Liste der Schutz-Infos (nie null, ggf. leer)
     */
    List<SheetSchutzInfo> berechneSchutzInfos(WorkingSpreadsheet ws);

    /**
     * Erzeugt oder aktualisiert die CellStyles im Dokument, die von editierbaren Zellen
     * (bedingte Formatierung) benötigt werden.
     * <p>
     * <b>MUSS VOR</b> {@code XProtectable.protect()} aufgerufen werden, da LibreOffice
     * bei tab-geschützten Sheets keine Style-Änderungen erlaubt
     * (Einschränkung in {@code sc/source/ui/unoobj/styleuno.cxx}).
     * Arbeitet direkt auf Dokumentebene, ohne Sheet-Kontext.
     *
     * @param ws aktuelles Spreadsheet
     */
    void zelleStylesAktualisieren(WorkingSpreadsheet ws);
}
