/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;

/**
 * End-to-End-UI-Test des Skip-Pfads für den Rangliste-Refresh.
 * <p>
 * Verifiziert anhand eines vollständig generierten Schweizer-Turniers, dass die
 * {@link RanglisteEingabeSignatur} die zentrale Anforderung erfüllt: <i>Rebuild nur
 * wenn sich relevante Eingaben tatsächlich geändert haben.</i>
 * <p>
 * Geprüfte Eigenschaften:
 * <ol>
 *   <li><b>Determinismus</b>: zweimal aufgerufen ohne Datenänderung liefert die
 *       Engine denselben Hash → der Listener würde den Rebuild überspringen.</li>
 *   <li><b>Sensitivität</b>: Änderung an einer Ergebniszelle (in einer Spielrunde)
 *       liefert einen anderen Hash → der Listener würde rebuilden.</li>
 *   <li><b>Rückkehr-Stabilität</b>: nach Rücksetzen der Änderung ist der ursprüngliche
 *       Hash wieder identisch.</li>
 *   <li><b>Whitelist</b>: Schreiben in eine nicht-relevante Hilfsspalte verändert
 *       den Hash <em>nicht</em>.</li>
 *   <li><b>Store-Roundtrip</b>: gespeicherter Hash ist gleich dem soeben berechneten
 *       und wird durch {@code aktualisiereVerifyZeit} nicht überschrieben.</li>
 *   <li><b>Safety-Revalidation</b>: ohne gespeicherten Verify-Zeitstempel meldet
 *       {@code verifyVeraltet} {@code true}; nach Schreiben des Hashes {@code false}.</li>
 * </ol>
 */
@Tag("beispielturnier")
class RanglisteSignaturSkipPfadUITest extends BaseCalcUITest {

    /** Hilfsspalte rechts vom Schweizer-Datenbereich (ERG_TEAM_B = 4, FEHLER = 5). */
    private static final int HILFSSPALTE_AUSSERHALB_WHITELIST = 12;

    private static final String SCHLUESSEL_SCHWEIZER = "ranking-test-schweizer";

    @Test
    void skipPfad_zweimalGleicheEingabe_liefertGleichenHash() throws GenerateException {
        new SchweizerTurnierTestDaten(wkingSpreadsheet).generate();
        XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();

        RanglisteEingabeSignatur engine = new RanglisteEingabeSignatur(SignaturQuellen::fuerSchweizer);

        String hash1 = berechneOk(engine, xDoc);
        String hash2 = berechneOk(engine, xDoc);

        assertThat(hash2)
                .as("Zwei Hash-Berechnungen ohne Datenänderung müssen identisch sein "
                        + "(Skip-Pfad – Listener würde Rebuild auslassen)")
                .isEqualTo(hash1);
    }

    @Test
    void aenderungAnErgebnis_aendertHash() throws GenerateException {
        new SchweizerTurnierTestDaten(wkingSpreadsheet).generate();
        XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();

        RanglisteEingabeSignatur engine = new RanglisteEingabeSignatur(SignaturQuellen::fuerSchweizer);
        String hashVorher = berechneOk(engine, xDoc);

        XSpreadsheet runde1 = SheetMetadataHelper.findeSheetUndHeile(xDoc,
                SheetMetadataHelper.schluesselSchweizerSpielrunde(1), null);
        assertThat(runde1).as("Spielrunde 1 muss existieren").isNotNull();
        int alterWert = leseErgebnisA(xDoc, runde1);
        schreibeErgebnisA(xDoc, runde1, alterWert == 13 ? 7 : 13);

        String hashNachher = berechneOk(engine, xDoc);
        assertThat(hashNachher)
                .as("Hash MUSS sich ändern, wenn ein Spielergebnis editiert wurde")
                .isNotEqualTo(hashVorher);

        // Rückkehr-Stabilität
        schreibeErgebnisA(xDoc, runde1, alterWert);
        String hashZurueck = berechneOk(engine, xDoc);
        assertThat(hashZurueck)
                .as("Nach Rücksetzen der Änderung muss wieder der ursprüngliche Hash entstehen")
                .isEqualTo(hashVorher);
    }

    @Test
    void aenderungAnHilfsspalteAusserhalbWhitelist_aendertHashNicht() throws GenerateException {
        new SchweizerTurnierTestDaten(wkingSpreadsheet).generate();
        XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();

        RanglisteEingabeSignatur engine = new RanglisteEingabeSignatur(SignaturQuellen::fuerSchweizer);
        String hashVorher = berechneOk(engine, xDoc);

        XSpreadsheet runde1 = SheetMetadataHelper.findeSheetUndHeile(xDoc,
                SheetMetadataHelper.schluesselSchweizerSpielrunde(1), null);
        // Block-Write in einer Zelle weit außerhalb der Whitelist (TEAM_A..ERG_TEAM_B = 1..4).
        var daten = new RangeData(1, 999);
        RangeHelper.from(runde1, xDoc,
                RangePosition.from(HILFSSPALTE_AUSSERHALB_WHITELIST,
                        SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
                        HILFSSPALTE_AUSSERHALB_WHITELIST,
                        SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE))
                .setDataInRange(daten);

        String hashNachher = berechneOk(engine, xDoc);
        assertThat(hashNachher)
                .as("Schreiben in nicht-whitelistete Spalte %d DARF den Hash nicht verändern",
                        HILFSSPALTE_AUSSERHALB_WHITELIST)
                .isEqualTo(hashVorher);
    }

    @Test
    void store_speichertHashUndAktualisiertVerifyZeit() throws GenerateException {
        new SchweizerTurnierTestDaten(wkingSpreadsheet).generate();
        XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();

        RanglisteEingabeSignatur engine = new RanglisteEingabeSignatur(SignaturQuellen::fuerSchweizer);

        assertThat(RanglisteSignaturStore.ladeHash(xDoc, SCHLUESSEL_SCHWEIZER))
                .as("Anfangs darf kein Hash gespeichert sein")
                .isEmpty();
        assertThat(RanglisteSignaturStore.verifyVeraltet(xDoc, SCHLUESSEL_SCHWEIZER,
                Duration.ofMinutes(10)))
                .as("Ohne gespeicherten Verify-Zeitstempel muss verifyVeraltet=true gelten")
                .isTrue();

        String hash = berechneOk(engine, xDoc);
        RanglisteSignaturStore.speichereNachRebuild(xDoc, SCHLUESSEL_SCHWEIZER, hash, "test-rebuild");

        assertThat(RanglisteSignaturStore.ladeHash(xDoc, SCHLUESSEL_SCHWEIZER))
                .as("Gespeicherter Hash muss dem berechneten entsprechen")
                .hasValue(hash);
        assertThat(RanglisteSignaturStore.verifyVeraltet(xDoc, SCHLUESSEL_SCHWEIZER,
                Duration.ofMinutes(10)))
                .as("Direkt nach Speichern darf der Verify-Zeitstempel nicht veraltet sein")
                .isFalse();

        // Reine Verify-Aktualisierung darf den Hash unverändert lassen
        RanglisteSignaturStore.aktualisiereVerifyZeit(xDoc, SCHLUESSEL_SCHWEIZER);
        assertThat(RanglisteSignaturStore.ladeHash(xDoc, SCHLUESSEL_SCHWEIZER))
                .as("aktualisiereVerifyZeit DARF den Hash nicht überschreiben")
                .hasValue(hash);
    }

    // -------------------------------------------------------------------------

    private static String berechneOk(RanglisteEingabeSignatur engine, XSpreadsheetDocument xDoc) {
        SignaturErgebnis ergebnis = engine.berechne(xDoc, 1);
        assertThat(ergebnis)
                .as("Engine muss Ok liefern (Schweizer-Turnier ist vollständig generiert)")
                .isInstanceOf(SignaturErgebnis.Ok.class);
        return ((SignaturErgebnis.Ok) ergebnis).hash();
    }

    private static int leseErgebnisA(XSpreadsheetDocument xDoc, XSpreadsheet runde) throws GenerateException {
        RangeData daten = RangeHelper.from(runde, xDoc,
                RangePosition.from(
                        SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                        SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
                        SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                        SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE))
                .getDataFromRange();
        return daten.get(0).get(0).getIntVal(0);
    }

    private static void schreibeErgebnisA(XSpreadsheetDocument xDoc, XSpreadsheet runde, int wert)
            throws GenerateException {
        var daten = new RangeData(1, wert);
        RangeHelper.from(runde, xDoc,
                RangePosition.from(
                        SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                        SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
                        SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                        SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE))
                .setDataInRange(daten);
    }
}
