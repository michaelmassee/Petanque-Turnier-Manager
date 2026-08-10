/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * Regressionstest für Formation.NUR_TEAMNAME: Maastrichter verwendet intern das gleiche
 * Meldeliste-Layout wie Schweizer ({@link MaastrichterMeldeListeSheetUpdate} erweitert
 * {@code SchweizerMeldeListeSheetUpdate}). Die Meldeliste hat bei NUR_TEAMNAME keine
 * Spieler-Namensspalten mehr, nur Nr + Teamname (+ Setzposition + Aktiv). Teamname-Anzeige
 * ist zwingend aktiv. Prüft insbesondere, dass die (von Schweizer geerbte) Zeilen-Erkennung
 * korrekt auf die Teamname-Spalte statt auf eine nicht existierende Spieler-Spalte zugreift.
 */
class MaastrichterMeldeListeNurTeamnameUITest extends BaseCalcUITest {

    @Test
    void meldelisteOhneSpielerspaltenWirdKorrektAufgebaut() throws Exception {
        MaastrichterMeldeListeSheetNew meldeListeNew = new MaastrichterMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.erstelleMeldeliste(Formation.NUR_TEAMNAME, false, false, SpielplanTeamAnzeige.NR);

        MaastrichterKonfigurationSheet konfig = new MaastrichterKonfigurationSheet(wkingSpreadsheet);
        assertThat(konfig.getMeldeListeFormation()).isEqualTo(Formation.NUR_TEAMNAME);
        assertThat(konfig.isMeldeListeTeamnameAnzeigen())
                .as("Teamname-Anzeige muss bei Nur Teamname zwingend aktiv sein, auch bei false-Parameter")
                .isTrue();

        MaastrichterMeldeListeSheetUpdate meldeListeUpdate = new MaastrichterMeldeListeSheetUpdate(wkingSpreadsheet);

        // Layout ohne Spieler-Spalten: Nr=0, Teamname=1, Setzposition=2, Aktiv=3
        assertThat(meldeListeUpdate.getTeamnameSpalte()).isEqualTo(1);
        assertThat(meldeListeUpdate.getSetzPositionSpalte()).isEqualTo(2);
        assertThat(meldeListeUpdate.getAktivSpalte()).isEqualTo(3);

        int ersteDatenZeile = meldeListeUpdate.getErsteDatenZiele();
        int aktivWertNimmtTeil = 1; // SchweizerListeDelegate.AKTIV_WERT_NIMMT_TEIL (package-private)

        RangeData data = new RangeData();
        for (int team = 1; team <= 4; team++) {
            RowData zeile = data.addNewRow();
            zeile.newInt(team);
            zeile.newString("Team " + team);
            zeile.newEmpty(); // Setzposition
            zeile.newInt(aktivWertNimmtTeil);
        }

        XSpreadsheet xSheet = meldeListeUpdate.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);
        meldeListeUpdate.upDateSheet();

        // Zeilen-Erkennung muss über die Teamname-Spalte laufen, nicht über eine
        // (bei NUR_TEAMNAME gar nicht existierende) Spieler-Spalte.
        assertThat(meldeListeUpdate.getAktiveMeldungen().getMeldungen()).hasSize(4);
        assertThat(meldeListeUpdate.getAlleMeldungen().getMeldungen()).hasSize(4);
    }

}
