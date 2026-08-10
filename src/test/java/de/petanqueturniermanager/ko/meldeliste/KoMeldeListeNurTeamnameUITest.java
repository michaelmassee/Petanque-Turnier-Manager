/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Regressionstest für Formation.NUR_TEAMNAME: die K.-O.-Meldeliste hat keine
 * Spieler-Namensspalten mehr, nur Nr + Teamname (+ RNG + Aktiv). Teamname-Anzeige
 * ist zwingend aktiv. Prüft insbesondere, dass die Zeilen-Erkennung (aktiv/befüllt)
 * korrekt auf die Teamname-Spalte statt auf eine nicht existierende Spieler-Spalte
 * zugreift (siehe {@code KoListeDelegate.getZeilenKennungSpalte()}).
 */
class KoMeldeListeNurTeamnameUITest extends BaseCalcUITest {

    @Test
    void meldelisteOhneSpielerspaltenWirdKorrektAufgebaut() throws Exception {
        KoMeldeListeSheetNew meldeListeNew = new KoMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.getKonfigurationSheet().update();
        meldeListeNew.getKonfigurationSheet().setMeldeListeFormation(Formation.NUR_TEAMNAME);
        meldeListeNew.getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(false);
        meldeListeNew.getKonfigurationSheet().setMeldeListeVereinsnameAnzeigen(false);
        meldeListeNew.getKonfigurationSheet().update();
        meldeListeNew.createMeldelisteWithParams();

        assertThat(meldeListeNew.getKonfigurationSheet().getMeldeListeFormation()).isEqualTo(Formation.NUR_TEAMNAME);
        assertThat(meldeListeNew.getKonfigurationSheet().isMeldeListeTeamnameAnzeigen())
                .as("Teamname-Anzeige muss bei Nur Teamname zwingend aktiv sein, auch bei false-Parameter")
                .isTrue();

        // Layout ohne Spieler-Spalten: Nr=0, Teamname=1, RNG=2, Aktiv=3
        assertThat(meldeListeNew.getTeamnameSpalte()).isEqualTo(1);
        assertThat(meldeListeNew.getRanglisteSpalte()).isEqualTo(2);
        assertThat(meldeListeNew.getAktivSpalte()).isEqualTo(3);

        int ersteDatenZeile = KoListeDelegate.ERSTE_DATEN_ZEILE;

        RangeData data = new RangeData();
        for (int team = 1; team <= 4; team++) {
            RowData zeile = data.addNewRow();
            zeile.newInt(team);
            zeile.newString("Team " + team);
            zeile.newInt(team); // RNG (Setzreihenfolge)
            zeile.newInt(KoListeDelegate.AKTIV_WERT_NIMMT_TEIL);
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
