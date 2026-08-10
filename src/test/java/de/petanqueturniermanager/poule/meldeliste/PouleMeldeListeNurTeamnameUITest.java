/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;

/**
 * Regressionstest für Formation.NUR_TEAMNAME: die Poule-A/B-Meldeliste hat keine
 * Spieler-Namensspalten mehr, nur Nr + Teamname (+ Setzposition + Aktiv). Teamname-Anzeige
 * ist zwingend aktiv. Prüft insbesondere, dass die Zeilen-Erkennung (aktiv/befüllt)
 * korrekt auf die Teamname-Spalte statt auf eine nicht existierende Spieler-Spalte
 * zugreift (siehe {@code PouleListeDelegate.getZeilenKennungSpalte()}).
 */
class PouleMeldeListeNurTeamnameUITest extends BaseCalcUITest {

    @Test
    void meldelisteOhneSpielerspaltenWirdKorrektAufgebaut() throws Exception {
        PouleMeldeListeSheetNew meldeListeNew = new PouleMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.NUR_TEAMNAME, false, false);

        PouleKonfigurationSheet konfig = new PouleKonfigurationSheet(wkingSpreadsheet);
        assertThat(konfig.getMeldeListeFormation()).isEqualTo(Formation.NUR_TEAMNAME);
        assertThat(konfig.isMeldeListeTeamnameAnzeigen())
                .as("Teamname-Anzeige muss bei Nur Teamname zwingend aktiv sein, auch bei false-Parameter")
                .isTrue();

        // Layout ohne Spieler-Spalten: Nr=0, Teamname=1, Setzposition=2, Aktiv=3
        assertThat(meldeListeNew.getTeamnameSpalte()).isEqualTo(1);
        assertThat(meldeListeNew.getSetzPositionSpalte()).isEqualTo(2);
        assertThat(meldeListeNew.getAktivSpalte()).isEqualTo(3);

        int ersteDatenZeile = PouleListeDelegate.ERSTE_DATEN_ZEILE;

        RangeData data = new RangeData();
        for (int team = 1; team <= 4; team++) {
            RowData zeile = data.addNewRow();
            zeile.newInt(team);
            zeile.newString("Team " + team);
            zeile.newEmpty(); // Setzposition
            zeile.newInt(PouleListeDelegate.AKTIV_WERT_NIMMT_TEIL);
        }

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);
        meldeListeNew.upDateSheet();

        // Zeilen-Erkennung muss über die Teamname-Spalte laufen, nicht über eine
        // (bei NUR_TEAMNAME gar nicht existierende) Spieler-Spalte.
        assertThat(meldeListeNew.getAktiveMeldungen().getMeldungen()).hasSize(4);
        assertThat(meldeListeNew.getAlleMeldungen().getMeldungen()).hasSize(4);
    }

}
