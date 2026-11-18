/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.maastrichter.MaastrichterTurnierTestDaten;

/**
 * Die Maastrichter-Endrangliste in PTM-Online hat zwei Teile: die Vorrunden-Rangliste und je Finalfeld den Sieger.
 * Der Sieger steht erst fest, wenn das Finale entschieden ist; er kommt über die Sieger-Formeln des Turnierbaums ins
 * Finale.
 */
class MaastrichterKoSiegerUITest extends BaseCalcUITest {

    private static final int GEWINNER_PUNKTE = 13;
    private static final int VERLIERER_PUNKTE = 5;

    @BeforeEach
    @Override
    public void beforeTest() {
        super.beforeTest();
        RandomSource.setSeed(42L);
    }

    @AfterEach
    public void resetRandom() {
        RandomSource.reset();
    }

    @Test
    void entschiedeneFinalfelderLiefernIhrenSiegerNebenDerVorrundenRangliste() throws Exception {
        new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate();
        LiveStandQuelle quelle = LiveStandQuellen.fuer(wkingSpreadsheet, TurnierSystem.MAASTRICHTER, null)
                .orElseThrow();
        LiveTurnierStand vorDemFinale = quelle.lese();
        assertThat(vorDemFinale.koSieger()).as("ohne KO-Ergebnisse noch kein Sieger").isEmpty();
        assertThat(vorDemFinale.rangliste()).as("Vorrunden-Rangliste vollständig").hasSize(12)
                .extracting(LiveRanglistenEintrag::platz).startsWith(1, 2);

        LiveTurnierStand stand = spieleAlleKoPartienAus(quelle);

        LivePartie finale = finaleVonFeld(stand, SheetNamen.koFinaleGruppe("A"));
        assertThat(stand.koSieger()).containsExactly(new LiveKoSieger(SheetNamen.koFinaleGruppe("A"), finale.teamA()));
        assertThat(stand.rangliste()).as("die Vorrunden-Rangliste bleibt der erste Teil").hasSize(12);
    }

    /** Trägt Runde für Runde 13:5 für Seite A ein, bis keine KO-Partie mehr offen ist. */
    private LiveTurnierStand spieleAlleKoPartienAus(LiveStandQuelle quelle) throws Exception {
        LiveTurnierStand stand = quelle.lese();
        for (List<LivePartie> offen = offeneKoPartien(stand); !offen.isEmpty(); offen = offeneKoPartien(stand)) {
            for (LivePartie partie : offen) {
                assertThat(ErgebnisSchreiber.schreibe(wkingSpreadsheet, partie.ergebnisOrt(), GEWINNER_PUNKTE,
                        VERLIERER_PUNKTE)).isTrue();
            }
            stand = quelle.lese();
        }
        return stand;
    }

    private static List<LivePartie> offeneKoPartien(LiveTurnierStand stand) {
        return stand.runden().stream().flatMap(runde -> runde.partien().stream())
                .filter(partie -> partie.ergebnisOrt() != null && partie.punkteA() == null && !partie.istFreilos())
                .filter(partie -> partie.stufe() != null && partie.stufe().startsWith(SheetNamen.koFinaleGruppe("A")))
                .toList();
    }

    private static LivePartie finaleVonFeld(LiveTurnierStand stand, String feld) {
        List<LivePartie> partien = stand.runden().getLast().partien().stream()
                .filter(partie -> partie.stufe() != null && partie.stufe().startsWith(feld)).toList();
        return partien.getFirst();
    }
}
