/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.NeueMeldungTeilnahme;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SupermeleeTurnierTestDaten;

/**
 * Supermelee hat je Spieltag eine Aktiv-Spalte: Check-in beim Übernehmen, Abmeldung und deren Aufhebung wirken auf
 * die Spalte des aktiven Spieltags, nicht auf die des ersten.
 */
class SheetMeldelisteAdapterSupermeleeUITest extends BaseCalcUITest {

    private MeldelisteZiel ziel;
    private MeldeListeSheet_Update meldeliste;
    private int spieltag;

    @BeforeEach
    void spieltageAnlegen() throws Exception {
        RandomSource.setSeed(42L);
        new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SUPERMELEE.getId());
        spieltag = new SuperMeleeKonfigurationSheet(wkingSpreadsheet).getAktiveSpieltag().getNr();
        meldeliste = new MeldeListeSheet_Update(wkingSpreadsheet);
        ziel = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
    }

    @AfterEach
    void aufraeumen() {
        RandomSource.reset();
    }

    @Test
    void checkinUndAbmeldungWirkenAufDenAktivenSpieltag() throws Exception {
        assertThat(spieltag).as("Testdaten mit mehreren Spieltagen").isGreaterThan(1);
        int zeile = ziel.schreibeBlockUndLiefereZeile(List.of(spieler()), NeueMeldungTeilnahme.AKTIV);

        assertThat(wert(spieltag, zeile)).as("eingecheckt am aktiven Spieltag").isEqualTo("1");
        assertThat(ziel.getAktivWertAusZeile(zeile)).isEqualTo(1);

        ziel.markiereAlsAbgemeldet(zeile);
        assertThat(wert(spieltag, zeile)).isEqualTo("2");
        assertThat(ziel.getAktivWertAusZeile(zeile)).isEqualTo(2);

        ziel.hebeAbmeldungAuf(zeile);
        assertThat(wert(spieltag, zeile)).isNullOrEmpty();
        assertThat(wert(1, zeile)).as("Spalte des ersten Spieltags bleibt unberührt").isNullOrEmpty();
    }

    private String wert(int spieltagNr, int zeile1Basiert) throws Exception {
        return meldeliste.getSheetHelper().getTextFromCell(meldeliste.getXSpreadSheet(),
                Position.from(meldeliste.spieltagSpalte(SpielTagNr.from(spieltagNr)), zeile1Basiert - 1));
    }

    private static SpielerMitVerein spieler() {
        return new SpielerMitVerein(0, "Zora", "Spieltag", null, null, List.of(), List.of(), null);
    }
}
