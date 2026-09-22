package de.petanqueturniermanager.helper.rangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
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
     * Löscht veraltete Zeilen unterhalb der neuen Datentabelle vor dem Neuschreiben.
     * Die erste Leerzeile und die Fußzeile werden ebenfalls geleert, weil sie keine
     * Datenzeilen sind und beim anschließenden Schreiben neu aufgebaut werden.
     * <p>
     * {@code sucheLetzteZeileMitSpielerNummer()} findet nur Zeilen mit einer echten
     * Spieler-/Team-Nummer (Regex {@code ^\d}); eine mehrzeilige Fußzeile (z.B. bei
     * Supermelee: 3 Textzeilen ohne Leerzeile davor) wird davon nicht erfasst und bliebe
     * sonst nach dem Update stehen. {@code getLetzteMitDatenZeileInSpielerNrSpalte()}
     * findet dagegen die letzte NICHT LEERE Zelle unabhängig vom Inhalt und deckt damit
     * auch eine bestehende Fußzeile vollständig ab.
     *
     * @param rangliste     Die Ranglisten-Sheet-Instanz (muss {@link IRangliste} implementieren)
     * @param sheet         Das aktive Spreadsheet
     * @param neueTeamAnzahl Neue Anzahl der Teams/Spieler
     */
    public static void loescheDatenzeilen(IRangliste rangliste, XSpreadsheet sheet, int neueTeamAnzahl)
            throws GenerateException {
        int bisherigeLetzte = Math.max(rangliste.sucheLetzteZeileMitSpielerNummer(),
                rangliste.getLetzteMitDatenZeileInSpielerNrSpalte());
        int neueLetzte = rangliste.getErsteDatenZiele() + neueTeamAnzahl - 1;
        int ersteNichtDatenZeile = neueLetzte + 1;
        int letzteZuLeerndeZeile = Math.max(bisherigeLetzte, neueLetzte + 2);
        if (letzteZuLeerndeZeile >= ersteNichtDatenZeile) {
            RangePosition alteDatenzeilen = RangePosition.from(rangliste.getErsteSpalte(), ersteNichtDatenZeile,
                    rangliste.validateSpalte(), letzteZuLeerndeZeile);
            RangeHelper.from(sheet,
                    rangliste.getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                    alteDatenzeilen)
                    .clearRange();
            rangliste.getSheetHelper().setPropertiesInRange(sheet, alteDatenzeilen,
                    CellProperties.from().setCellBackColor(-1));
        }
    }
}
