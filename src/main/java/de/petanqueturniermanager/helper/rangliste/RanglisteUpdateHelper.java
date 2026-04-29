package de.petanqueturniermanager.helper.rangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;

/**
 * Gemeinsame Hilfsmethoden für {@code *RanglisteSheetUpdate}-Klassen.
 * Verwendet ausschließlich Methoden aus {@link IRangliste} und {@link de.petanqueturniermanager.helper.ISheet},
 * um Duplikation zwischen den einzelnen Turniersystem-Update-Klassen zu vermeiden.
 */
public final class RanglisteUpdateHelper {

    private RanglisteUpdateHelper() {}

    /**
     * Löscht veraltete Datenzeilen wenn sich die Teamanzahl verringert hat.
     * Bei gleichbleibender oder größerer Teamanzahl passiert nichts, da
     * {@code insertDatenAlsWerte} die vorhandenen Zellen überschreibt.
     *
     * @param rangliste     Die Ranglisten-Sheet-Instanz (muss {@link IRangliste} implementieren)
     * @param sheet         Das aktive Spreadsheet
     * @param neueTeamAnzahl Neue Anzahl der Teams/Spieler
     */
    public static void loescheDatenzeilen(IRangliste rangliste, XSpreadsheet sheet, int neueTeamAnzahl)
            throws GenerateException {
        int bisherigeLetzte = rangliste.sucheLetzteZeileMitSpielerNummer();
        int neueLetzte = rangliste.getErsteDatenZiele() + neueTeamAnzahl - 1;
        if (bisherigeLetzte > neueLetzte) {
            RangeHelper.from(sheet,
                    rangliste.getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                    RangePosition.from(rangliste.getErsteSpalte(), neueLetzte + 1,
                            rangliste.validateSpalte(), bisherigeLetzte))
                    .clearRange();
        }
    }
}
