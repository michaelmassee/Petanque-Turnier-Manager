package de.petanqueturniermanager.webserver;

import java.util.Optional;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Löst den zu einem Webserver-Port konfigurierten Sheet-Namen dynamisch auf.
 * <p>
 * Alle Implementierungen müssen UNO-sicher im SheetRunner-Thread aufgerufen werden.
 */
public interface SheetResolver {

    /**
     * Liefert das Sheet, das für diesen Port angezeigt werden soll.
     *
     * @param ws aktuelles Arbeits-Spreadsheet
     * @return gefundenes Sheet, oder leer wenn nicht vorhanden
     */
    Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws);

    /**
     * Anzeigename für den HTML-Titel, z.B. "Spielrunde" oder "Rangliste".
     */
    String getAnzeigeName();

    /**
     * Optionale laufende Nummer des Sheets, z.B. 3 für "Spielrunde 3".
     * Wird für den Seitentitel verwendet: "{anzeigeName} {nummer}".
     *
     * @param sheet das aufgelöste Sheet
     * @return Nummer, falls ermittelbar
     */
    Optional<Integer> getNummer(XSpreadsheet sheet);
}
