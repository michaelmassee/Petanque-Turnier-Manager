package de.petanqueturniermanager.helper.rangliste;

import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;

/**
 * Gemeinsame Hilfsmethoden für {@code *RanglisteSheetUpdate}-Klassen.
 * Verwendet ausschließlich Methoden aus {@link IRangliste} und {@link de.petanqueturniermanager.helper.ISheet},
 * um Duplikation zwischen den einzelnen Turniersystem-Update-Klassen zu vermeiden.
 */
public final class RanglisteUpdateHelper {

    /** Abstand der Fußzeile zur letzten Datenzeile (eine Leerzeile dazwischen). */
    public static final int FUSSZEILEN_ABSTAND = 2;

    private RanglisteUpdateHelper() {}

    /**
     * Löscht veraltete Zeilen unterhalb der neuen Datentabelle vor dem Neuschreiben.
     * Die erste Leerzeile und die Fußzeile werden ebenfalls geleert, weil sie keine
     * Datenzeilen sind und beim anschließenden Schreiben neu aufgebaut werden.
     * Kommen Teams hinzu, wird ab der bisherigen letzten Datenzeile geleert. Das Löschen
     * der Attribute hebt dabei die Verbindung der alten Fußzeile auf.
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
        int bisherigeLetzteDatenZeile = rangliste.sucheLetzteZeileMitSpielerNummer();
        int bisherigeLetzte = Math.max(bisherigeLetzteDatenZeile,
                rangliste.getLetzteMitDatenZeileInSpielerNrSpalte());
        int neueLetzte = rangliste.getErsteDatenZiele() + neueTeamAnzahl - 1;
        // Bei mehr Teams ab der bisherigen letzten Datenzeile leeren: die alte, verbundene Fußzeile
        // läge sonst mitten in den neuen Datenzeilen.
        int ersteZuLeerndeZeile = Math.min(bisherigeLetzteDatenZeile, neueLetzte) + 1;
        // Die alte Fußzeile steht FUSSZEILEN_ABSTAND unter der alten letzten Datenzeile. Nicht jede
        // getLetzteMitDatenZeileInSpielerNrSpalte()-Implementierung erfasst sie (z.B. Liga).
        int letzteZuLeerndeZeile = Math.max(bisherigeLetzte,
                Math.max(bisherigeLetzteDatenZeile, neueLetzte) + FUSSZEILEN_ABSTAND);
        if (letzteZuLeerndeZeile >= ersteZuLeerndeZeile) {
            RangePosition alteDatenzeilen = RangePosition.from(rangliste.getErsteSpalte(), ersteZuLeerndeZeile,
                    rangliste.validateSpalte(), letzteZuLeerndeZeile);
            RangeHelper.from(sheet,
                    rangliste.getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                    alteDatenzeilen)
                    .clearRange();
            rangliste.getSheetHelper().setPropertiesInRange(sheet, alteDatenzeilen,
                    CellProperties.from().setCellBackColor(-1));
        }
    }

    /**
     * Löscht die alten Daten- und Fußzeilen, wenn sich die Anzahl der Teams geändert hat. Bei gleicher Anzahl
     * passiert nichts, weil das Neuschreiben jede Zeile überschreibt.
     * <p>
     * Weniger Teams: Die Zeilen unterhalb der neuen Daten und die alte Fußzeile blieben sonst stehen.
     * Mehr Teams: Die alte, über die ganze Breite verbundene Fußzeile läge mitten in den neuen Datenzeilen. Das
     * Löschen der Attribute hebt die Verbindung auf. Danach muss der Aufrufer Daten, Formatierung und Fußzeile
     * neu schreiben.
     * <p>
     * Erwartet eine Zahl in jeder Datenzeile der {@code nrSpalte} und eine Fußzeile, die mit Text beginnt und
     * {@value #FUSSZEILEN_ABSTAND} Zeilen unter der letzten Datenzeile steht.
     *
     * @param rangliste       Ranglisten-Sheet
     * @param nrSpalte        Spalte mit der Team-Nr bzw. dem Platz in jeder Datenzeile
     * @param ersteDatenZeile erste Datenzeile
     * @param letzteSpalte    letzte Spalte der Tabelle inklusive Fußzeile
     * @param neueAnzahl      Anzahl der Datenzeilen nach dem Update
     */
    public static void loescheAlteZeilenBeiGeaenderterAnzahl(ISheet rangliste, int nrSpalte, int ersteDatenZeile,
            int letzteSpalte, int neueAnzahl) throws GenerateException {
        Position letzteMitNr = RangeSearchHelper
                .from(rangliste, RangePosition.from(nrSpalte, ersteDatenZeile, nrSpalte, ersteDatenZeile + 999))
                .searchNachRegExprInSpalte("^\\d", Map.<String, Object>of(RangeSearchHelper.SEARCH_BACKWARDS, true));
        if (letzteMitNr == null) {
            return;
        }
        int bisherigeLetzte = letzteMitNr.getZeile();
        int neueLetzte = ersteDatenZeile + neueAnzahl - 1;
        if (bisherigeLetzte == neueLetzte) {
            return;
        }
        RangePosition alteZeilen = RangePosition.from(nrSpalte, Math.min(bisherigeLetzte, neueLetzte) + 1,
                letzteSpalte, Math.max(bisherigeLetzte, neueLetzte) + FUSSZEILEN_ABSTAND);
        RangeHelper.from(rangliste, alteZeilen).clearRange();
    }
}
