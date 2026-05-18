/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.endrangliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.rangliste.RanglisteEingabeSignatur;
import de.petanqueturniermanager.helper.rangliste.SignaturErgebnis;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.RanglisteTestDaten;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

/**
 * End-to-End-UI-Test des Skip-Pfads für Supermelee: prüft pro Spieltag-Rangliste
 * <i>und</i> für die Endrangliste, dass {@link RanglisteEingabeSignatur} nur dann
 * einen geänderten Hash liefert, wenn sich semantisch relevante Eingaben tatsächlich
 * geändert haben.
 * <p>
 * Szenario: 20 Spieler, 3 Spieltage mit je 3 Spielrunden + generierten Ergebnissen
 * pro Spieltag. Pro Spieltag wird eine {@link SpieltagRanglisteSheet} erstellt,
 * abschließend die {@link EndranglisteSheet}.
 * <p>
 * Geprüfte Eigenschaften:
 * <ol>
 *   <li><b>Determinismus pro Spieltag</b>: zwei Hash-Berechnungen ohne Änderung
 *       liefern denselben Wert.</li>
 *   <li><b>Isolation</b>: Änderung an einer Ergebniszelle in Spieltag 1 verändert
 *       den Hash für Spieltag 1, lässt aber Spieltag 2 und 3 unverändert.</li>
 *   <li><b>End-Rangliste-Sensitivität</b>: dieselbe Änderung verändert auch den
 *       Endrangliste-Hash – erst nach Neuaufbau der betroffenen Spieltag-Rangliste,
 *       da der Endranglisten-Hash auf den Spieltag-Rangliste-Sheets basiert.</li>
 *   <li><b>Rückkehr-Stabilität</b>: nach Rücksetzen + Rebuild kehren alle Hashes
 *       wieder zu den ursprünglichen Werten zurück.</li>
 * </ol>
 */
@Tag("beispielturnier")
class SupermeleeSignaturSkipPfadUITest extends BaseCalcUITest {

    private static final int ANZ_MELDUNGEN = 20;
    private static final int ANZ_RUNDEN = 3;
    private static final int ANZ_SPIELTAGE = 3;

    private TestSuperMeleeMeldeListeErstellen testMeldeListeErstellen;
    private MeldeListeSheet_NeuerSpieltag meldeListeSheet_NeuerSpieltag;
    private SpieltagRanglisteSheet spieltagRanglisteAktiv;
    private RanglisteTestDaten<SupermeleeSignaturSkipPfadUITest> ranglisteTestDaten;
    private EndranglisteSheet endranglisteSheet;

    @BeforeEach
    void initialisiereSupermeleeUmgebung() throws GenerateException {
        testMeldeListeErstellen = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
        meldeListeSheet_NeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet);
        spieltagRanglisteAktiv = new SpieltagRanglisteSheet(wkingSpreadsheet);
        ranglisteTestDaten = new RanglisteTestDaten<>(wkingSpreadsheet, sheetHlp, this);
        endranglisteSheet = new EndranglisteSheet(wkingSpreadsheet);
    }

    @Test
    void mehrereSpieltage_skipPfadProSpieltagUndEndrangliste() throws GenerateException {
        baueDreiSpieltageMitRanglisten();
        endranglisteSheet.run();
        XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();

        // Initialer Hash-Stand
        String[] hashesInitial = new String[ANZ_SPIELTAGE + 1];
        for (int i = 1; i <= ANZ_SPIELTAGE; i++) {
            hashesInitial[i - 1] = berechneSpieltagHash(xDoc, i);
        }
        hashesInitial[ANZ_SPIELTAGE] = berechneEndranglisteHash(xDoc);

        // (1) Determinismus pro Spieltag + Endrangliste
        for (int i = 1; i <= ANZ_SPIELTAGE; i++) {
            assertThat(berechneSpieltagHash(xDoc, i))
                    .as("Determinismus: Spieltag %d zweimal aufgerufen muss denselben Hash liefern", i)
                    .isEqualTo(hashesInitial[i - 1]);
        }
        assertThat(berechneEndranglisteHash(xDoc))
                .as("Determinismus: Endrangliste zweimal aufgerufen muss denselben Hash liefern")
                .isEqualTo(hashesInitial[ANZ_SPIELTAGE]);

        // (2) Ergebnis in Spieltag 1, Runde 1 ändern
        int alterWert = leseErstesPlusErgebnis(xDoc, 1);
        int neuerWert = alterWert == 0 ? 7 : Math.max(0, alterWert - 5);
        schreibeErstesPlusErgebnis(xDoc, 1, neuerWert);

        // Spieltag-1-Hash MUSS sich ändern, Spieltage 2/3 NICHT
        String hashSpieltag1Nach = berechneSpieltagHash(xDoc, 1);
        assertThat(hashSpieltag1Nach)
                .as("Spieltag-1-Hash MUSS sich nach Ergebnisänderung in Spieltag 1 ändern")
                .isNotEqualTo(hashesInitial[0]);
        for (int i = 2; i <= ANZ_SPIELTAGE; i++) {
            assertThat(berechneSpieltagHash(xDoc, i))
                    .as("Spieltag-%d-Hash darf sich durch Änderung in Spieltag 1 NICHT ändern "
                            + "(Isolation der Spieltage)", i)
                    .isEqualTo(hashesInitial[i - 1]);
        }

        // (3) Endrangliste-Hash MUSS sich ändern, nachdem Spieltag-1-Rangliste neu gebaut wurde
        // (vorher liest sie noch die alten Daten aus der Spieltag-1-Rangliste).
        new SpieltagRanglisteSheet(wkingSpreadsheet, SpielTagNr.from(1)).run();
        String hashEndNach = berechneEndranglisteHash(xDoc);
        assertThat(hashEndNach)
                .as("Endrangliste-Hash MUSS sich ändern, sobald die Spieltag-1-Rangliste den "
                        + "geänderten Stand widerspiegelt")
                .isNotEqualTo(hashesInitial[ANZ_SPIELTAGE]);

        // (4) Rückkehr-Stabilität
        schreibeErstesPlusErgebnis(xDoc, 1, alterWert);
        new SpieltagRanglisteSheet(wkingSpreadsheet, SpielTagNr.from(1)).run();
        assertThat(berechneSpieltagHash(xDoc, 1))
                .as("Spieltag-1-Hash muss nach Rücksetzen + Rebuild wieder dem Initialwert entsprechen")
                .isEqualTo(hashesInitial[0]);
        assertThat(berechneEndranglisteHash(xDoc))
                .as("Endrangliste-Hash muss nach Rücksetzen + Rebuild wieder dem Initialwert entsprechen")
                .isEqualTo(hashesInitial[ANZ_SPIELTAGE]);
    }

    // -------------------------------------------------------------------------

    private void baueDreiSpieltageMitRanglisten() throws GenerateException {
        testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);
        for (int i = 1; i <= ANZ_SPIELTAGE; i++) {
            SpielTagNr spieltag = SpielTagNr.from(i);
            meldeListeSheet_NeuerSpieltag.setAktiveSpieltag(spieltag);
            meldeListeSheet_NeuerSpieltag.setAktiveSpielRunde(SpielRundeNr.from(1));
            testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(spieltag);
            if (i == 1) {
                // 2 Spieler in Spieltag 1 deaktivieren – passt zu den Paarungs-JSONs
                // im endrangliste-Ressourcenpaket (18 aktive Spieler statt 20).
                sheetHlp.setStringValueInCell(StringCellValue
                        .from(meldeListeSheet_NeuerSpieltag, Position.from(3, 10)).setValue(""));
                sheetHlp.setStringValueInCell(StringCellValue
                        .from(meldeListeSheet_NeuerSpieltag, Position.from(3, 11)).setValue(""));
            }
            ranglisteTestDaten.erstelleTestSpielrunden(ANZ_RUNDEN, false, spieltag);
            spieltagRanglisteAktiv.run();
        }
    }

    private static String berechneSpieltagHash(XSpreadsheetDocument xDoc, int spieltagNr) {
        RanglisteEingabeSignatur engine = new RanglisteEingabeSignatur(
                doc -> SignaturQuellen.fuerSupermeleeSpieltag(doc, spieltagNr));
        return berechneOk(engine, xDoc, "Spieltag " + spieltagNr);
    }

    private static String berechneEndranglisteHash(XSpreadsheetDocument xDoc) {
        RanglisteEingabeSignatur engine = new RanglisteEingabeSignatur(
                SignaturQuellen::fuerSupermeleeEnd);
        return berechneOk(engine, xDoc, "Endrangliste");
    }

    private static String berechneOk(RanglisteEingabeSignatur engine, XSpreadsheetDocument xDoc,
            String bez) {
        SignaturErgebnis ergebnis = engine.berechne(xDoc, 1);
        assertThat(ergebnis)
                .as("[%s] Engine muss Ok liefern (Turnier ist vollständig generiert)", bez)
                .isInstanceOf(SignaturErgebnis.Ok.class);
        return ((SignaturErgebnis.Ok) ergebnis).hash();
    }

    /** Liest den ersten +-Wert (ERSTE_SPALTE_ERGEBNISSE, ERSTE_DATEN_ZEILE) aus Spieltag/Runde-1-Sheet. */
    private int leseErstesPlusErgebnis(XSpreadsheetDocument xDoc, int spieltagNr) throws GenerateException {
        XSpreadsheet spielrunde = sheetHlp.findByName(SpielrundeSheetKonstanten.sheetName(spieltagNr, 1));
        assertThat(spielrunde).as("Spielrunde-Sheet Spieltag %d, Runde 1 muss existieren", spieltagNr)
                .isNotNull();
        RangePosition zelle = positionErstesPlusErgebnis();
        RangeData daten = RangeHelper.from(spielrunde, xDoc, zelle).getDataFromRange();
        return daten.get(0).get(0).getIntVal(0);
    }

    private void schreibeErstesPlusErgebnis(XSpreadsheetDocument xDoc, int spieltagNr, int wert)
            throws GenerateException {
        XSpreadsheet spielrunde = sheetHlp.findByName(SpielrundeSheetKonstanten.sheetName(spieltagNr, 1));
        assertThat(spielrunde).as("Spielrunde-Sheet Spieltag %d, Runde 1 muss existieren", spieltagNr)
                .isNotNull();
        RangeHelper.from(spielrunde, xDoc, positionErstesPlusErgebnis())
                .setDataInRange(new Object[][] { { wert } }, false);
    }

    private static RangePosition positionErstesPlusErgebnis() {
        Position start = Position.from(SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE,
                SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE);
        return RangePosition.from(start, start);
    }
}
