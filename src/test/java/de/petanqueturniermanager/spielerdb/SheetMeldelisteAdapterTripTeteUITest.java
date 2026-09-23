/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.NeueMeldungTeilnahme;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetNew;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;

/**
 * Regression: Die Trip-Tête-Meldeliste hat keine Setzpositionsspalte, die Aktiv-Spalte folgt direkt auf
 * die Spielerdaten. Der Adapter muss dieselbe Spalte beschreiben und lesen wie die Meldeliste selbst.
 */
class SheetMeldelisteAdapterTripTeteUITest extends BaseCalcUITest {

    private MeldelisteZiel ziel;
    private int zeile;

    @BeforeEach
    void meldelisteMitTeamAnlegen() throws Exception {
        new TripTeteMeldeListeSheetNew(wkingSpreadsheet).createMeldeliste();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.TRIPTETE.getId());
        ziel = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
        zeile = ziel.schreibeBlockUndLiefereZeile(
                List.of(spieler("Anna", "Eins"), spieler("Bernd", "Zwei"), spieler("Clara", "Drei")),
                NeueMeldungTeilnahme.AKTIV);
    }

    @Test
    void aktivWirdInDieAktivSpalteDerMeldelisteGeschrieben() throws Exception {
        assertThat(aktivZelleDerMeldeliste()).isEqualTo("1");
        assertThat(ziel.getAktivWertAusZeile(zeile)).isEqualTo(1);
    }

    @Test
    void abmeldungLandetInDerAktivSpalteDerMeldeliste() throws Exception {
        ziel.markiereAlsAbgemeldet(zeile);

        assertThat(aktivZelleDerMeldeliste()).isEqualTo("2");
        assertThat(ziel.getAktivWertAusZeile(zeile)).isEqualTo(2);
    }

    private String aktivZelleDerMeldeliste() throws GenerateException {
        TripTeteMeldeListeSheetUpdate meldeliste = new TripTeteMeldeListeSheetUpdate(wkingSpreadsheet);
        return meldeliste.getSheetHelper().getTextFromCell(meldeliste.getXSpreadSheet(),
                Position.from(meldeliste.getAktivSpalte(), zeile - 1));
    }

    private static SpielerMitVerein spieler(String vorname, String nachname) {
        return new SpielerMitVerein(0, vorname, nachname, null, null, List.of(), List.of(), null);
    }
}
