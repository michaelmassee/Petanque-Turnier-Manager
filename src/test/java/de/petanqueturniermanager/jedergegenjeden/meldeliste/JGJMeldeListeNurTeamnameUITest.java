/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.jedergegenjeden.blattschutz.JGJBlattschutzKonfiguration;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Regressionstest für Formation.NUR_TEAMNAME: die JGJ-Meldeliste hat keine
 * Spieler-Namensspalten mehr, nur Nr + Teamname (+ Setzposition + Aktiv). Teamname-Anzeige
 * ist zwingend aktiv. Prüft insbesondere, dass die Zeilen-Erkennung (aktiv/befüllt)
 * korrekt auf die Teamname-Spalte statt auf eine nicht existierende Spieler-Spalte
 * zugreift (siehe {@code JGJMeldeListeDelegate.getZeilenKennungSpalte()}).
 */
class JGJMeldeListeNurTeamnameUITest extends BaseCalcUITest {

    @Test
    void meldelisteOhneSpielerspaltenWirdKorrektAufgebaut() throws Exception {
        JGJMeldeListeSheet_New meldeListeNew = new JGJMeldeListeSheet_New(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.NUR_TEAMNAME, false, false, SpielplanTeamAnzeige.NR);

        JGJKonfigurationSheet konfig = new JGJKonfigurationSheet(wkingSpreadsheet);
        assertThat(konfig.getMeldeListeFormation()).isEqualTo(Formation.NUR_TEAMNAME);
        assertThat(konfig.isMeldeListeTeamnameAnzeigen())
                .as("Teamname-Anzeige muss bei Nur Teamname zwingend aktiv sein, auch bei false-Parameter")
                .isTrue();

        // Spalten-Layout über die (package-private) Delegate-Klasse prüfen.
        JGJMeldeListeDelegate spaltenSonde = new JGJMeldeListeDelegate(meldeListeNew, wkingSpreadsheet, TurnierSystem.JGJ);
        // Layout ohne Spieler-Spalten: Nr=0, Teamname=1, Setzposition=2, Aktiv=3
        assertThat(spaltenSonde.getTeamnameSpalte()).isEqualTo(1);
        assertThat(spaltenSonde.getSetzPositionSpalte()).isEqualTo(2);
        assertThat(spaltenSonde.getAktivSpalte()).isEqualTo(3);

        int ersteDatenZeile = JGJMeldeListeDelegate.ERSTE_DATEN_ZEILE;

        RangeData data = new RangeData();
        for (int team = 1; team <= 4; team++) {
            RowData zeile = data.addNewRow();
            zeile.newInt(team);
            zeile.newString("Team " + team);
            zeile.newEmpty(); // Setzposition
            zeile.newInt(JGJMeldeListeDelegate.AKTIV_WERT_NIMMT_TEIL);
        }

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);
        meldeListeNew.upDateSheet();

        // Zeilen-Erkennung muss über die Teamname-Spalte laufen, nicht über eine
        // (bei NUR_TEAMNAME gar nicht existierende) Spieler-Spalte.
        assertThat(meldeListeNew.getAktiveMeldungen().getMeldungen()).hasSize(4);
        assertThat(meldeListeNew.getAlleMeldungen().getMeldungen()).hasSize(4);
    }

    /**
     * Die Blattschutz-Konfiguration darf nicht nur die erste Namensspalte freigeben.
     * Mit Teamname, Doublette und Vereinsnamen umfasst die Eingabe Spalten B bis J;
     * allein die automatisch vergebene Nr in Spalte A bleibt gesperrt.
     */
    @Test
    void kioskModus_gibtDenVollstaendigenKonfiguriertenMeldelistenEingabebereichFrei() throws Exception {
        JGJMeldeListeSheet_New meldeListeNew = new JGJMeldeListeSheet_New(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.DOUBLETTE, true, true, SpielplanTeamAnzeige.NR);
        XSpreadsheet sheet = meldeListeNew.getXSpreadSheet();

        TurnierModus.get().setAktivForTest(true);
        try {
            BlattschutzManager.get().schuetzen(JGJBlattschutzKonfiguration.get(), wkingSpreadsheet);

            assertThat(Lo.qi(XProtectable.class, sheet).isProtected()).isTrue();
            assertThat(istGesperrt(sheet, 0)).as("automatische Nr bleibt gesperrt").isTrue();
            for (int spalte = 1; spalte <= 9; spalte++) {
                assertThat(istGesperrt(sheet, spalte))
                        .as("JGJ-Eingabespalte %d muss trotz Blattschutz editierbar bleiben", spalte)
                        .isFalse();
            }
        } finally {
            BlattschutzManager.get().entsperren(JGJBlattschutzKonfiguration.get(), wkingSpreadsheet);
            TurnierModus.get().setAktivForTest(false);
        }
    }

    private boolean istGesperrt(XSpreadsheet sheet, int spalte) throws Exception {
        XPropertySet props = Lo.qi(XPropertySet.class,
                sheet.getCellByPosition(spalte, JGJMeldeListeDelegate.ERSTE_DATEN_ZEILE));
        return ((CellProtection) props.getPropertyValue("CellProtection")).IsLocked;
    }

}
