/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
                MeldelisteLayout.team(Formation.TETE, false, false)).orElseThrow();
    }

    @Test
    void leereMeldelisteLiefertKeineUeberschriftAlsMeldung() {
        assertThat(ziel.leseAlleSpielerRoh()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void geloeschteZeileVerdecktKeineNachfolgendenMeldungen(int geloeschterIndex) throws Exception {
        int anna = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Anna", "Adler")), NeueMeldungTeilnahme.INAKTIV);
        int ben = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Ben", "Berg")), NeueMeldungTeilnahme.INAKTIV);
        int clara = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Clara", "Club")), NeueMeldungTeilnahme.INAKTIV);

        ziel.entferneMeldung(geloeschterIndex == 0 ? anna : ben);

        var gelesen = ziel.leseAlleSpielerRoh();
        assertThat(gelesen).extracting(MeldelisteSpielerDaten::vorname)
                .containsExactly(geloeschterIndex == 0 ? "Ben" : "Anna", "Clara");
        assertThat(gelesen).extracting(MeldelisteSpielerDaten::zeile1Basiert)
                .containsExactly(geloeschterIndex == 0 ? ben : anna, clara);
    }

    @Test
    void leererErsterSpielerslotVerdecktWederPartnerNochFolgendeTeams() throws Exception {
        meldeListe.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
        ziel = SheetMeldelisteAdapter.fuer(wkingSpreadsheet, SheetNamen.meldeliste(), TurnierSystem.SCHWEIZER,
                MeldelisteLayout.team(Formation.DOUBLETTE, false, false)).orElseThrow();
        int ersteZeile = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Anna", "Adler"), spieler("Ben", "Berg")),
                NeueMeldungTeilnahme.INAKTIV);
        int zweiteZeile = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Clara", "Club"), spieler("Dora", "Dorf")),
                NeueMeldungTeilnahme.INAKTIV);

        meldeListe.getXSpreadSheet().getCellByPosition(1, ersteZeile - 1).setFormula("");
        meldeListe.getXSpreadSheet().getCellByPosition(2, ersteZeile - 1).setFormula("");

        var gelesen = ziel.leseAlleSpielerRoh();
        assertThat(gelesen).extracting(MeldelisteSpielerDaten::vorname).containsExactly("Ben", "Clara", "Dora");
        assertThat(gelesen).extracting(MeldelisteSpielerDaten::zeile1Basiert)
                .containsExactly(ersteZeile, zweiteZeile, zweiteZeile);
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

    /** Spieler-DB-Übernahme: Team gilt als angemeldet, eingecheckt wird erst vor Ort. */
    @Test
    void schreibeBlockAusSpielerDbLaesstAktivLeer() throws Exception {
        ziel.schreibeBlock(List.of(spieler("Clara", "Datenbank")));

        int zeile = ziel.findeZeileMitName("Clara Datenbank");
        assertThat(aktivWert(zeile)).isEmpty();
    }

    @Test
    void aktivWertLiefertTeilnahmeDerZeile() throws Exception {
        int aktiv = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Dora", "Aktiv")), NeueMeldungTeilnahme.AKTIV);
        int inaktiv = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Emil", "Inaktiv")),
                NeueMeldungTeilnahme.INAKTIV);
        int abgemeldet = ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Fritz", "Weg")),
                NeueMeldungTeilnahme.AKTIV);
        ziel.markiereAlsAbgemeldet(abgemeldet);

        assertThat(ziel.getAktivWertAusZeile(aktiv)).isEqualTo(1);
        assertThat(ziel.getAktivWertAusZeile(inaktiv)).isEqualTo(-1);
        assertThat(ziel.getAktivWertAusZeile(abgemeldet)).isEqualTo(2);
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
