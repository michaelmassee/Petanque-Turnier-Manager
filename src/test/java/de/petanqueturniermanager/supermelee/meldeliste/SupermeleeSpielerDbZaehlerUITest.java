/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.MeldelisteStatus;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SupermeleeTurnierTestDaten;

/**
 * Regressionstest: Supermelee hat je Spieltag eine eigene Aktiv-Spalte. Der Checkin-Zähler im
 * Spieler-DB-Dialog zählte ab Spieltag 2 trotzdem die Teilnahme am ersten Spieltag.
 */
class SupermeleeSpielerDbZaehlerUITest extends BaseCalcUITest {

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
    void checkinZaehltDenAktivenSpieltag() throws Exception {
        new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SUPERMELEE.getId());
        int aktiverSpieltag = new SuperMeleeKonfigurationSheet(wkingSpreadsheet).getAktiveSpieltag().getNr();
        assertThat(aktiverSpieltag).as("Vorbedingung: Testdaten mit mehreren Spieltagen").isGreaterThan(1);

        var meldeliste = new MeldeListeSheet_Update(wkingSpreadsheet);
        int ersteZeile = meldeliste.getErsteDatenZiele();
        int letzteZeile = meldeliste.getLetzteMitDatenZeileInSpielerNrSpalte();
        int checkinAktiverSpieltag = anzahlBelegt(meldeliste, aktiverSpieltag, ersteZeile, letzteZeile);
        assertThat(anzahlBelegt(meldeliste, 1, ersteZeile, letzteZeile))
                .as("Vorbedingung: Spieltag 1 hat eine andere Teilnahme als der aktive Spieltag")
                .isNotEqualTo(checkinAktiverSpieltag);

        int gesamt = letzteZeile - ersteZeile + 1;
        assertThat(MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow().getMeldelisteStatus())
                .isEqualTo(new MeldelisteStatus(gesamt - checkinAktiverSpieltag, checkinAktiverSpieltag, gesamt));
    }

    private int anzahlBelegt(MeldeListeSheet_Update meldeliste, int spieltag, int ersteZeile, int letzteZeile)
            throws Exception {
        int spalte = meldeliste.spieltagSpalte(SpielTagNr.from(spieltag));
        int anzahl = 0;
        for (RowData zeile : RangeHelper
                .from(meldeliste.getXSpreadSheet(), doc, RangePosition.from(spalte, ersteZeile, spalte, letzteZeile))
                .getDataFromRange()) {
            if (!zeile.get(0).getStringVal().isBlank()) {
                anzahl++;
            }
        }
        return anzahl;
    }
}
