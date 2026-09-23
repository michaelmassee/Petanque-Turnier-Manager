/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.NeueMeldungTeilnahme;

/**
 * Prüft den Anfangswert der Aktiv-Spalte beim Schreiben neuer Meldungen: Übernahmen aus der
 * Spieler-DB nehmen sofort teil, PTM-Online-Importe bleiben bis zum Check-in inaktiv (leer).
 */
class SheetMeldelisteAdapterTeilnahmeUITest extends BaseCalcUITest {

    private SchweizerMeldeListeSheetNew meldeListe;
    private MeldelisteZiel ziel;

    @BeforeEach
    void meldelisteAnlegen() throws Exception {
        meldeListe = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
        meldeListe.createMeldelisteWithParams(Formation.TETE, false, false);
        ziel = SheetMeldelisteAdapter.fuer(wkingSpreadsheet, SheetNamen.meldeliste(), TurnierSystem.SCHWEIZER,
                Formation.TETE, false, false).orElseThrow();
    }

    @Test
    void inaktiveMeldungLaesstAktivSpalteLeer() throws Exception {
        int zeile = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Anna", "Online")),
                NeueMeldungTeilnahme.INAKTIV);

        assertThat(aktivWert(zeile)).as("PTM-Online-Import darf nicht eingecheckt sein").isNullOrEmpty();
    }

    @Test
    void aktiveMeldungSetztNimmtTeil() throws Exception {
        int zeile = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Bernd", "Vorort")),
                NeueMeldungTeilnahme.AKTIV);

        assertThat(aktivWert(zeile)).isEqualTo("1");
    }

    @Test
    void schreibeBlockAusSpielerDbBleibtAktiv() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Clara", "Datenbank")));

        int zeile = ziel.findeZeileMitName("Clara Datenbank");
        assertThat(aktivWert(zeile)).isEqualTo("1");
    }

    private String aktivWert(int zeile1Basiert) throws GenerateException {
        XSpreadsheet xSheet = meldeListe.getXSpreadSheet();
        return meldeListe.getSheetHelper().getTextFromCell(xSheet,
                Position.from(meldeListe.getAktivSpalte(), zeile1Basiert - 1));
    }

    private static SpielerMitVerein spieler(String vorname, String nachname) {
        return new SpielerMitVerein(0, vorname, nachname, null, null, List.of(), List.of(), null);
    }
}
