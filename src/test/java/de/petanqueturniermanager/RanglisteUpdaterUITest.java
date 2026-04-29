/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXAbstractSpielrundeSheet;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXTurnierTestDaten;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.maastrichter.MaastrichterTurnierTestDaten;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.poule.PouleTurnierTestDaten;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;

/**
 * Systemübergreifender UI-Test für alle {@code *RanglisteSheetUpdate}-Klassen.
 * <p>
 * Prüft für jedes der vier Turniersysteme (Schweizer, Maastrichter, Poule, FormuleX):
 * <ol>
 *   <li><b>Initialer Stand</b>: Nach Turniergenerierung enthält die Rangliste vollständig
 *       korrekte Daten – Teamanzahl, Gesamtsiege, mathematische Invarianten (z. B.
 *       PUNKTE_DIFF = PUNKTE_PLUS − PUNKTE_MINUS), gültige Wertebereiche und
 *       korrekte Sortierreihenfolge.</li>
 *   <li><b>Aktualisierung nach Änderung</b>: Nach Nullen der Ergebniszellen produziert
 *       {@code doRun()} eine geänderte Rangliste (reduzierte Siegesumme).</li>
 * </ol>
 * <p>
 * Das Paket {@code de.petanqueturniermanager} erlaubt direkten Zugriff auf
 * {@code SheetRunner.doRun()} (protected, gleiches Paket wie die Basisklasse).
 */
@Tag("beispielturnier")
class RanglisteUpdaterUITest extends BaseCalcUITest {

    private static final int ERGEBNIS_ERSTE_DATEN_ZEILE = 2;
    private static final int NULL_MATRIX_ZEILEN = 300;

    @FunctionalInterface
    private interface TurnierGenerator {
        void generiere(WorkingSpreadsheet ws) throws GenerateException;
    }

    @FunctionalInterface
    private interface UpdaterAktion {
        void ausfuehren(WorkingSpreadsheet ws) throws GenerateException;
    }

    @FunctionalInterface
    private interface PrüferAktion {
        void prüfe(RangeData daten, SoftAssertions soft, String bezeichnung);
    }

    /**
     * Konfiguration eines Turniersystem-Szenarios für den parametrisierten Test.
     *
     * @param bezeichnung             Anzeigename im Testbericht
     * @param generator               Erzeugt ein vollständiges Beispielturnier
     * @param updaterAktion           Ruft {@code doRun()} des zugehörigen {@code *RanglisteSheetUpdate} auf
     * @param ranglisteSchluessel     Named-Range-Schlüssel des Rangliste-Sheets
     * @param teamNrSpalte            Absolute Spalte für TeamNr in der Rangliste (ab Spalte 0)
     * @param siegeSpalte             Absolute Spalte für Siege in der Rangliste (ab Spalte 0)
     * @param letzteDatenSpalte       Rechteste relevante Datenspalte (Lesebereich endet hier)
     * @param ersteDatenZeile         Erste Datenzeile der Rangliste (0-basiert)
     * @param ergebnisSheetSchluessel Named-Range-Schlüssel des Sheets mit Spielergebnissen
     * @param ergASpalte              Absolute Spalte für ERG_A im Ergebnis-Sheet
     * @param ergBSpalte              Absolute Spalte für ERG_B im Ergebnis-Sheet
     * @param erwarteteSiegesumme     Erwartete Gesamtanzahl Siege nach Turniergenerierung
     * @param erwartetAnzahlTeams     Erwartete Anzahl Teams in der Rangliste
     * @param prüfer                  System-spezifische Vollprüfung aller Invarianten
     */
    private record Szenario(
            String bezeichnung,
            TurnierGenerator generator,
            UpdaterAktion updaterAktion,
            String ranglisteSchluessel,
            int teamNrSpalte,
            int siegeSpalte,
            int letzteDatenSpalte,
            int ersteDatenZeile,
            String ergebnisSheetSchluessel,
            int ergASpalte,
            int ergBSpalte,
            int erwarteteSiegesumme,
            int erwartetAnzahlTeams,
            PrüferAktion prüfer) {

        @Override
        public String toString() {
            return bezeichnung;
        }
    }

    static Stream<Szenario> szenarienProvider() {
        return Stream.of(
                schweizer(),
                maastrichter(),
                poule(),
                formuleX());
    }

    private static Szenario schweizer() {
        // 16 Teams, 3 Runden → 8 Spiele × 3 = 24 Siege
        return new Szenario(
                "Schweizer",
                ws -> new SchweizerTurnierTestDaten(ws).generate(),
                ws -> new SchweizerRanglisteSheetUpdate(ws).doRun(),
                SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE,
                SchweizerRanglisteSheet.TEAM_NR_SPALTE,
                SchweizerRanglisteSheet.SIEGE_SPALTE,
                SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
                SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
                SheetMetadataHelper.schluesselSchweizerSpielrunde(1),
                SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
                24,
                16,
                (daten, soft, bez) -> prüfeSchweizerArtig(daten, soft, bez, 3));
    }

    private static Szenario maastrichter() {
        // 12 Teams, 3 Vorrunden → 6 Spiele × 3 = 18 Siege
        return new Szenario(
                "Maastrichter",
                ws -> new MaastrichterTurnierTestDaten(ws).generate(),
                ws -> new MaastrichterVorrundenRanglisteSheetUpdate(ws).doRun(),
                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
                SchweizerRanglisteSheet.TEAM_NR_SPALTE,
                SchweizerRanglisteSheet.SIEGE_SPALTE,
                SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
                SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
                SheetMetadataHelper.schluesselMaastrichterVorrunde(1),
                SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
                18,
                12,
                (daten, soft, bez) -> prüfeSchweizerArtig(daten, soft, bez, 3));
    }

    private static Szenario poule() {
        // 16 Teams, 4 Gruppen à 4 Teams → 5 Spiele × 4 Gruppen = 20 Siege
        return new Szenario(
                "Poule",
                ws -> new PouleTurnierTestDaten(ws).generate(),
                ws -> new PouleVorrundenRanglisteSheetUpdate(ws).doRun(),
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE,
                PouleVorrundenRanglisteSheet.SPALTE_NR,
                PouleVorrundenRanglisteSheet.SPALTE_SIEGE,
                PouleVorrundenRanglisteSheet.SPALTE_TURNIER,
                PouleVorrundenRanglisteSheet.HEADER_ZEILEN,
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE,
                AbstractPouleVorrundeSheet.SPALTE_ERG_A,
                AbstractPouleVorrundeSheet.SPALTE_ERG_B,
                20,
                16,
                (daten, soft, bez) -> prüfePoule(daten, soft, bez));
    }

    private static Szenario formuleX() {
        // 39 Teams, 5 Runden → (19 reguläre + 1 Freilos) × 5 = 100 Siege
        return new Szenario(
                "FormuleX",
                ws -> new FormuleXTurnierTestDaten(ws).generate(),
                ws -> new FormuleXRanglisteSheetUpdate(ws).doRun(),
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE,
                FormuleXRanglisteSheet.TEAM_NR_SPALTE,
                FormuleXRanglisteSheet.SIEGE_SPALTE,
                FormuleXRanglisteSheet.PUNKTE_DIFF_SPALTE,
                FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE,
                SheetMetadataHelper.schluesselFormuleXSpielrunde(1),
                FormuleXAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
                FormuleXAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
                100,
                FormuleXTurnierTestDaten.ANZ_TEAMS,
                (daten, soft, bez) -> prüfeFormuleX(daten, soft, bez));
    }

    /**
     * Vollständige Prüfung der Rangliste nach Turniergenerierung:
     * korrekte Teamanzahl, Gesamtsiege und alle system-spezifischen Invarianten.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("szenarienProvider")
    void initialStandNachGenerierung_RanglisteIstVollständigKorrekt(Szenario s) throws GenerateException {
        s.generator().generiere(wkingSpreadsheet);

        var xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
        XSpreadsheet rangliste = SheetMetadataHelper.findeSheetUndHeile(xDoc, s.ranglisteSchluessel(), null);
        assertThat(rangliste)
                .as("[%s] Rangliste-Sheet muss vorhanden sein", s.bezeichnung())
                .isNotNull();

        RangeData daten = ladeVollständigeDaten(rangliste, s);
        assertThat(daten)
                .as("[%s] Rangliste muss Einträge enthalten", s.bezeichnung())
                .isNotEmpty();

        long anzahlTeams = anzahlTeams(daten, s);
        assertThat(anzahlTeams)
                .as("[%s] Rangliste muss genau %d Teams enthalten", s.bezeichnung(), s.erwartetAnzahlTeams())
                .isEqualTo(s.erwartetAnzahlTeams());

        int gesamtSiege = siegesumme(daten, s);
        assertThat(gesamtSiege)
                .as("[%s] Gesamtsiege muss %d betragen", s.bezeichnung(), s.erwarteteSiegesumme())
                .isEqualTo(s.erwarteteSiegesumme());

        var soft = new SoftAssertions();
        s.prüfer().prüfe(daten, soft, s.bezeichnung());
        soft.assertAll();
    }

    /**
     * Prüft, dass {@code doRun()} nach dem Nullen aller Ergebniszellen eine geänderte
     * Rangliste produziert: die Siegesumme muss kleiner sein als vor der Änderung.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("szenarienProvider")
    void aktualisierungNachAenderung_SiegesummeAendertSich(Szenario s) throws GenerateException {
        s.generator().generiere(wkingSpreadsheet);

        var xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
        XSpreadsheet rangliste = SheetMetadataHelper.findeSheetUndHeile(xDoc, s.ranglisteSchluessel(), null);
        assertThat(rangliste)
                .as("[%s] Rangliste-Sheet muss vorhanden sein", s.bezeichnung())
                .isNotNull();

        int siegeVorher = siegesumme(ladeVollständigeDaten(rangliste, s), s);
        assertThat(siegeVorher)
                .as("[%s] Ausgangszustand: Siegesumme muss > 0 sein", s.bezeichnung())
                .isGreaterThan(0);

        XSpreadsheet ergebnisSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc, s.ergebnisSheetSchluessel(), null);
        assertThat(ergebnisSheet)
                .as("[%s] Ergebnis-Sheet (Schlüssel '%s') muss vorhanden sein",
                        s.bezeichnung(), s.ergebnisSheetSchluessel())
                .isNotNull();
        nulleErgebniszellen(ergebnisSheet, s);

        s.updaterAktion().ausfuehren(wkingSpreadsheet);

        int siegeNachher = siegesumme(ladeVollständigeDaten(rangliste, s), s);
        assertThat(siegeNachher)
                .as("[%s] Siegesumme nach Update (%d) muss kleiner sein als vorher (%d)",
                        s.bezeichnung(), siegeNachher, siegeVorher)
                .isLessThan(siegeVorher);
    }

    // -------------------------------------------------------------------------
    // System-spezifische Vollprüfer
    // -------------------------------------------------------------------------

    /**
     * Validiert alle Invarianten einer Schweizer- oder Maastrichter-Rangliste:
     * <ul>
     *   <li>PLATZ monoton nicht-fallend, ≥ 1</li>
     *   <li>SIEGE ∈ [0, anzRunden], nicht-steigende Sortierung</li>
     *   <li>BHZ ≥ 0, FBHZ ≥ 0</li>
     *   <li>PUNKTE_PLUS ≥ 0, PUNKTE_MINUS ≥ 0</li>
     *   <li>PUNKTE_DIFF = PUNKTE_PLUS − PUNKTE_MINUS</li>
     * </ul>
     */
    private static void prüfeSchweizerArtig(RangeData daten, SoftAssertions soft, String bezeichnung, int anzRunden) {
        final int TEAM_NR = SchweizerRanglisteSheet.TEAM_NR_SPALTE;
        final int PLATZ   = SchweizerRanglisteSheet.PLATZ_SPALTE;
        final int SIEGE   = SchweizerRanglisteSheet.SIEGE_SPALTE;
        final int BHZ     = SchweizerRanglisteSheet.BHZ_SPALTE;
        final int FBHZ    = SchweizerRanglisteSheet.FBHZ_SPALTE;
        final int PPLUS   = SchweizerRanglisteSheet.PUNKTE_PLUS_SPALTE;
        final int PMINUS  = SchweizerRanglisteSheet.PUNKTE_MINUS_SPALTE;
        final int PDIFF   = SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE;

        int letztePlatz = 0;
        int letzteSiege = Integer.MAX_VALUE;

        for (int i = 0; i < daten.size(); i++) {
            var row = daten.get(i);
            if (row.get(TEAM_NR).getIntVal(0) <= 0) break;
            int zeile = i + 1;

            int platz  = row.get(PLATZ).getIntVal(-1);
            int siege  = row.get(SIEGE).getIntVal(-1);
            int bhz    = row.get(BHZ).getIntVal(-1);
            int fbhz   = row.get(FBHZ).getIntVal(-1);
            int pplus  = row.get(PPLUS).getIntVal(-1);
            int pminus = row.get(PMINUS).getIntVal(-1);
            int pdiff  = row.get(PDIFF).getIntVal(-1);

            soft.assertThat(platz)
                    .as("[%s] Zeile %d: PLATZ muss ≥ vorherigem PLATZ (%d) sein", bezeichnung, zeile, letztePlatz)
                    .isGreaterThanOrEqualTo(letztePlatz);
            soft.assertThat(platz)
                    .as("[%s] Zeile %d: PLATZ muss ≥ 1 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(1);
            soft.assertThat(siege)
                    .as("[%s] Zeile %d: SIEGE muss in [0, %d] liegen", bezeichnung, zeile, anzRunden)
                    .isBetween(0, anzRunden);
            soft.assertThat(siege)
                    .as("[%s] Zeile %d: SIEGE muss ≤ vorherigen SIEGE (%d) (Sortierreihenfolge)",
                            bezeichnung, zeile, letzteSiege)
                    .isLessThanOrEqualTo(letzteSiege);
            soft.assertThat(bhz)
                    .as("[%s] Zeile %d: BHZ muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);
            soft.assertThat(fbhz)
                    .as("[%s] Zeile %d: FBHZ muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);
            soft.assertThat(pplus)
                    .as("[%s] Zeile %d: PUNKTE_PLUS muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);
            soft.assertThat(pminus)
                    .as("[%s] Zeile %d: PUNKTE_MINUS muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);
            soft.assertThat(pdiff)
                    .as("[%s] Zeile %d: PUNKTE_DIFF muss PUNKTE_PLUS(%d) − PUNKTE_MINUS(%d) = %d sein",
                            bezeichnung, zeile, pplus, pminus, pplus - pminus)
                    .isEqualTo(pplus - pminus);

            letztePlatz = platz;
            letzteSiege = siege;
        }
    }

    /**
     * Validiert alle Invarianten der Poule-Vorrunden-Rangliste:
     * <ul>
     *   <li>Pro Gruppe: PLATZ läuft sequenziell von 1 aufsteigend</li>
     *   <li>SIEGE ist innerhalb jeder Gruppe nicht-steigend</li>
     *   <li>NDLG ≥ 0, PKT_PLUS ≥ 0, PKT_MINUS ≥ 0</li>
     *   <li>DIFF = PKT_PLUS − PKT_MINUS</li>
     *   <li>TURNIER = "A" genau dann wenn PLATZ ≤ 2</li>
     * </ul>
     */
    private static void prüfePoule(RangeData daten, SoftAssertions soft, String bezeichnung) {
        final int NR        = PouleVorrundenRanglisteSheet.SPALTE_NR;
        final int PLATZ     = PouleVorrundenRanglisteSheet.SPALTE_PLATZ;
        final int GRUPPE    = PouleVorrundenRanglisteSheet.SPALTE_GRUPPE;
        final int SIEGE     = PouleVorrundenRanglisteSheet.SPALTE_SIEGE;
        final int NDLG      = PouleVorrundenRanglisteSheet.SPALTE_NDLG;
        final int PKT_PLUS  = PouleVorrundenRanglisteSheet.SPALTE_PKT_PLUS;
        final int PKT_MINUS = PouleVorrundenRanglisteSheet.SPALTE_PKT_MINUS;
        final int DIFF      = PouleVorrundenRanglisteSheet.SPALTE_DIFF;
        final int TURNIER   = PouleVorrundenRanglisteSheet.SPALTE_TURNIER;

        int aktuellGruppe   = -1;
        int erwarteterPlatz = 1;
        int letzteSiege     = Integer.MAX_VALUE;

        for (int i = 0; i < daten.size(); i++) {
            var row = daten.get(i);
            if (row.get(NR).getIntVal(0) <= 0) break;
            int zeile = i + 1;

            int platz    = row.get(PLATZ).getIntVal(-1);
            int gruppe   = row.get(GRUPPE).getIntVal(-1);
            int siege    = row.get(SIEGE).getIntVal(-1);
            int ndlg     = row.get(NDLG).getIntVal(-1);
            int pktPlus  = row.get(PKT_PLUS).getIntVal(-1);
            int pktMinus = row.get(PKT_MINUS).getIntVal(-1);
            int diff     = row.get(DIFF).getIntVal(-1);
            String turnier = row.get(TURNIER).getStringVal();

            if (gruppe != aktuellGruppe) {
                aktuellGruppe   = gruppe;
                erwarteterPlatz = 1;
                letzteSiege     = Integer.MAX_VALUE;
            }

            soft.assertThat(platz)
                    .as("[%s] Zeile %d: PLATZ muss %d sein (Gruppe %d)", bezeichnung, zeile, erwarteterPlatz, gruppe)
                    .isEqualTo(erwarteterPlatz);
            soft.assertThat(siege)
                    .as("[%s] Zeile %d: SIEGE muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);
            soft.assertThat(siege)
                    .as("[%s] Zeile %d: SIEGE muss ≤ vorherigen SIEGE (%d) in Gruppe %d (Sortierreihenfolge)",
                            bezeichnung, zeile, letzteSiege, gruppe)
                    .isLessThanOrEqualTo(letzteSiege);
            soft.assertThat(ndlg)
                    .as("[%s] Zeile %d: NDLG muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);
            soft.assertThat(pktPlus)
                    .as("[%s] Zeile %d: PKT_PLUS muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);
            soft.assertThat(pktMinus)
                    .as("[%s] Zeile %d: PKT_MINUS muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);
            soft.assertThat(diff)
                    .as("[%s] Zeile %d: DIFF muss PKT_PLUS(%d) − PKT_MINUS(%d) = %d sein",
                            bezeichnung, zeile, pktPlus, pktMinus, pktPlus - pktMinus)
                    .isEqualTo(pktPlus - pktMinus);

            String erwartetTurnier = (platz <= 2) ? "A" : "B";
            soft.assertThat(turnier)
                    .as("[%s] Zeile %d: TURNIER muss '%s' sein (PLATZ=%d)", bezeichnung, zeile, erwartetTurnier, platz)
                    .isEqualTo(erwartetTurnier);

            erwarteterPlatz++;
            letzteSiege = siege;
        }
    }

    /**
     * Validiert alle Invarianten der Formule-X-Rangliste:
     * <ul>
     *   <li>PLATZ monoton nicht-fallend, ≥ 1</li>
     *   <li>WERTUNG ≥ 0, nicht-steigende Sortierung</li>
     *   <li>SIEGE ∈ [0, ANZ_RUNDEN]</li>
     *   <li>PUNKTE_PLUS ≥ 0</li>
     * </ul>
     */
    private static void prüfeFormuleX(RangeData daten, SoftAssertions soft, String bezeichnung) {
        final int TEAM_NR = FormuleXRanglisteSheet.TEAM_NR_SPALTE;
        final int PLATZ   = FormuleXRanglisteSheet.PLATZ_SPALTE;
        final int WERTUNG = FormuleXRanglisteSheet.WERTUNG_SPALTE;
        final int SIEGE   = FormuleXRanglisteSheet.SIEGE_SPALTE;
        final int PPLUS   = FormuleXRanglisteSheet.PUNKTE_PLUS_SPALTE;

        int letztePlatz   = 0;
        int letzteWertung = Integer.MAX_VALUE;

        for (int i = 0; i < daten.size(); i++) {
            var row = daten.get(i);
            if (row.get(TEAM_NR).getIntVal(0) <= 0) break;
            int zeile = i + 1;

            int platz   = row.get(PLATZ).getIntVal(-1);
            int wertung = row.get(WERTUNG).getIntVal(-1);
            int siege   = row.get(SIEGE).getIntVal(-1);
            int pplus   = row.get(PPLUS).getIntVal(-1);

            soft.assertThat(platz)
                    .as("[%s] Zeile %d: PLATZ muss ≥ vorherigem PLATZ (%d) sein", bezeichnung, zeile, letztePlatz)
                    .isGreaterThanOrEqualTo(letztePlatz);
            soft.assertThat(platz)
                    .as("[%s] Zeile %d: PLATZ muss ≥ 1 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(1);
            soft.assertThat(wertung)
                    .as("[%s] Zeile %d: WERTUNG muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);
            soft.assertThat(wertung)
                    .as("[%s] Zeile %d: WERTUNG muss ≤ vorherige WERTUNG (%d) (Sortierreihenfolge)",
                            bezeichnung, zeile, letzteWertung)
                    .isLessThanOrEqualTo(letzteWertung);
            soft.assertThat(siege)
                    .as("[%s] Zeile %d: SIEGE muss in [0, %d] liegen",
                            bezeichnung, zeile, FormuleXTurnierTestDaten.ANZ_RUNDEN)
                    .isBetween(0, FormuleXTurnierTestDaten.ANZ_RUNDEN);
            soft.assertThat(pplus)
                    .as("[%s] Zeile %d: PUNKTE_PLUS muss ≥ 0 sein", bezeichnung, zeile)
                    .isGreaterThanOrEqualTo(0);

            letztePlatz   = platz;
            letzteWertung = wertung;
        }
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    /**
     * Liest den gesamten Datenbereich ab Spalte 0 bis {@code letzteDatenSpalte}.
     * Alle Spaltenindizes im {@link RangeData}-Ergebnis sind damit absolut (ab 0).
     */
    private RangeData ladeVollständigeDaten(XSpreadsheet rangliste, Szenario s) throws GenerateException {
        return RangeHelper
                .from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                        RangePosition.from(
                                0, s.ersteDatenZeile(),
                                s.letzteDatenSpalte(), s.ersteDatenZeile() + 9999))
                .getDataFromRange();
    }

    /**
     * Zählt gültige Datenzeilen (TeamNr > 0). Bricht bei der ersten Zeile mit TeamNr ≤ 0 ab.
     */
    private long anzahlTeams(RangeData daten, Szenario s) {
        long count = 0;
        for (var row : daten) {
            if (row.get(s.teamNrSpalte()).getIntVal(0) <= 0) break;
            count++;
        }
        return count;
    }

    /**
     * Summiert alle Siege-Werte. Bricht bei der ersten Zeile mit TeamNr ≤ 0 ab.
     */
    private int siegesumme(RangeData daten, Szenario s) {
        int summe = 0;
        for (var row : daten) {
            if (row.get(s.teamNrSpalte()).getIntVal(0) <= 0) break;
            summe += row.get(s.siegeSpalte()).getIntVal(0);
        }
        return summe;
    }

    /**
     * Schreibt 0 in alle ERG_A- und ERG_B-Zellen des Ergebnis-Sheets als Block-Operation.
     */
    private void nulleErgebniszellen(XSpreadsheet ergebnisSheet, Szenario s) throws GenerateException {
        var nullDaten = new RangeData(NULL_MATRIX_ZEILEN, 0, 0);
        RangeHelper
                .from(ergebnisSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                        RangePosition.from(
                                s.ergASpalte(), ERGEBNIS_ERSTE_DATEN_ZEILE,
                                s.ergBSpalte(), ERGEBNIS_ERSTE_DATEN_ZEILE + NULL_MATRIX_ZEILEN - 1))
                .setDataInRange(nullDaten);
    }
}
