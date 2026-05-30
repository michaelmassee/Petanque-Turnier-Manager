package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_MINUS_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_DIV_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_PLUS_OFFS;
import static de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
import static de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE;
import static de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE;
import static de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten.LETZTE_SPALTE;
import static de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten.PAARUNG_CNTR_SPALTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;

import com.google.common.base.Preconditions;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

/**
 * Test-Hilfsklasse: assertiert die drei Konsistenz-Invarianten eines Supermelee-Spieltags
 * direkt gegen die LibreOffice-Sheets (nicht gegen JSON-Snapshots).
 *
 * <p>Prüft:
 * <ol>
 *   <li>Mitspieler-Wiederholungen = 0 (Hard-Constraint des Algorithmus)</li>
 *   <li>Rangliste-Werte (Σ+, Σ−, Δ, Siege, Punkte) gegen nachgerechnete Spielrunden-Daten</li>
 *   <li>Rangliste-Sortierung: Siege ↓ → Punkte ↓ → Δ ↓ → Σ+ ↓</li>
 * </ol>
 *
 * <p>SYNC-CHECK: Prüflogik muss synchron mit {@code tools/analyse_supermelee_spieltag.py} bleiben.
 * Sync-Verifikation: {@code python3 tools/test_analyse_supermelee_sync.py}
 */
class SupermeleeSpieltagAnalyseAssert {

    private static final int MAX_DATEN_ZEILEN = 200;

    private final WorkingSpreadsheet ws;
    private final SheetHelper sheetHelper;
    private final XSpreadsheetDocument xDoc;

    private SupermeleeSpieltagAnalyseAssert(WorkingSpreadsheet ws) {
        this.ws = Preconditions.checkNotNull(ws);
        this.xDoc = ws.getWorkingSpreadsheetDocument();
        this.sheetHelper = new SheetHelper(ws.getxContext(), xDoc);
    }

    static SupermeleeSpieltagAnalyseAssert fuer(WorkingSpreadsheet ws) {
        return new SupermeleeSpieltagAnalyseAssert(ws);
    }

    void pruefe(int spieltagNr, int anzRunden) throws GenerateException {
        var spielrundenDaten = leseSpielrunden(spieltagNr, anzRunden);
        pruefeMitspielerWiederholungen(spieltagNr, spielrundenDaten.mitspielerZaehler());
        pruefeRanglisteKorrektheit(spieltagNr, anzRunden, spielrundenDaten.statsProSpieler());
    }

    // ----- Spielrunden lesen -----

    private SpielrundenDaten leseSpielrunden(int spieltagNr, int anzRunden) {
        var statsProSpieler = new HashMap<Integer, SpielerStats>();
        var mitspielerZaehler = new HashMap<SortiertesPaar, Integer>();

        for (int rundeNr = 1; rundeNr <= anzRunden; rundeNr++) {
            var sheetName = SheetNamen.supermeleeSpielrunde(spieltagNr, rundeNr);
            XSpreadsheet sheet = sheetHelper.findByName(sheetName);
            assertThat(sheet).as("Spielrunden-Sheet '%s' muss existieren", sheetName).isNotNull();
            verarbeiteSpielrundeSheet(sheet, statsProSpieler, mitspielerZaehler);
        }
        return new SpielrundenDaten(statsProSpieler, mitspielerZaehler);
    }

    private void verarbeiteSpielrundeSheet(XSpreadsheet sheet,
            Map<Integer, SpielerStats> statsProSpieler,
            Map<SortiertesPaar, Integer> mitspielerZaehler) {

        RangeData data = RangeHelper.from(sheet, xDoc,
                RangePosition.from(0, ERSTE_DATEN_ZEILE, LETZTE_SPALTE,
                        ERSTE_DATEN_ZEILE + MAX_DATEN_ZEILEN - 1))
                .getDataFromRange();

        for (RowData row : data) {
            int paarungCntr = row.get(PAARUNG_CNTR_SPALTE).getIntVal(-1);
            if (paarungCntr <= 0) {
                break;
            }
            var team1 = extrahiereTeam(row, ERSTE_SPIELERNR_SPALTE);
            var team2 = extrahiereTeam(row, ERSTE_SPIELERNR_SPALTE + 3);
            int score1 = row.get(ERSTE_SPALTE_ERGEBNISSE).getIntVal(0);
            int score2 = row.get(ERSTE_SPALTE_ERGEBNISSE + 1).getIntVal(0);

            akkumuliereTeamStats(team1, score1, score2, statsProSpieler);
            akkumuliereTeamStats(team2, score2, score1, statsProSpieler);
            zaehlerMitspielerPaare(team1, mitspielerZaehler);
            zaehlerMitspielerPaare(team2, mitspielerZaehler);
        }
    }

    private List<Integer> extrahiereTeam(RowData row, int ersteSpalte) {
        var team = new ArrayList<Integer>(3);
        for (int i = 0; i < 3; i++) {
            int nr = row.get(ersteSpalte + i).getIntVal(0);
            if (nr > 0) {
                team.add(nr);
            }
        }
        return team;
    }

    private void akkumuliereTeamStats(List<Integer> team, int plus, int minus,
            Map<Integer, SpielerStats> statsProSpieler) {
        for (int nr : team) {
            var alt = statsProSpieler.getOrDefault(nr, new SpielerStats(0, 0, 0, 0));
            int neuSiege = alt.siege() + (plus > minus ? 1 : 0);
            statsProSpieler.put(nr, new SpielerStats(
                    alt.plus() + plus, alt.minus() + minus, alt.spiele() + 1, neuSiege));
        }
    }

    private void zaehlerMitspielerPaare(List<Integer> team, Map<SortiertesPaar, Integer> zaehler) {
        for (int i = 0; i < team.size(); i++) {
            for (int j = i + 1; j < team.size(); j++) {
                var paar = SortiertesPaar.von(team.get(i), team.get(j));
                zaehler.merge(paar, 1, Integer::sum);
            }
        }
    }

    // ----- Mitspieler-Wiederholungen -----

    private void pruefeMitspielerWiederholungen(int spieltagNr,
            Map<SortiertesPaar, Integer> mitspielerZaehler) {
        var wiederholungen = mitspielerZaehler.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted(Map.Entry.<SortiertesPaar, Integer>comparingByValue().reversed())
                .toList();
        assertThat(wiederholungen)
                .as("Spieltag %d: %d Mitspieler-Wiederholungen (Spielerpaar > 1× im selben Team):%n%s",
                        spieltagNr, wiederholungen.size(), formatWiederholungen(wiederholungen))
                .isEmpty();
    }

    private String formatWiederholungen(List<Map.Entry<SortiertesPaar, Integer>> liste) {
        var sb = new StringBuilder();
        for (var e : liste) {
            sb.append("  Spieler ").append(e.getKey().a())
                    .append(" + ").append(e.getKey().b())
                    .append(" → ").append(e.getValue()).append("×\n");
        }
        return sb.toString();
    }

    // ----- Rangliste-Korrektheit -----

    private void pruefeRanglisteKorrektheit(int spieltagNr, int anzRunden,
            Map<Integer, SpielerStats> statsProSpieler) throws GenerateException {
        var konfigSheet = new SuperMeleeKonfigurationSheet(ws);
        int nichtGespieltPlus = konfigSheet.getNichtGespielteRundePlus();
        int nichtGespieltMinus = konfigSheet.getNichtGespielteRundeMinus();

        // Summen-Spalten-Layout: SpieltagRanglisteDelegate.ERSTE_SPIELRUNDE_SPALTE(3) + anzRunden * 2
        int ersteSummeSpalte = SpieltagRanglisteDelegate.ERSTE_SPIELRUNDE_SPALTE + anzRunden * 2;
        int letzteSpalte = ersteSummeSpalte + PUNKTE_DIV_OFFS;

        var sheetName = SheetNamen.spieltagRangliste(spieltagNr);
        XSpreadsheet sheet = sheetHelper.findByName(sheetName);
        assertThat(sheet).as("Spieltag-Rangliste-Sheet '%s' muss existieren", sheetName).isNotNull();

        RangeData data = RangeHelper.from(sheet, xDoc,
                RangePosition.from(0, SpieltagRanglisteDelegate.ERSTE_DATEN_ZEILE,
                        letzteSpalte,
                        SpieltagRanglisteDelegate.ERSTE_DATEN_ZEILE + MAX_DATEN_ZEILEN - 1))
                .getDataFromRange();

        var ranglisteEintraege = new ArrayList<RanglisteEintrag>();
        var softly = new SoftAssertions();

        for (RowData row : data) {
            int nr = row.get(SpieltagRanglisteDelegate.SPIELER_NR_SPALTE).getIntVal(0);
            if (nr <= 0) {
                break;
            }
            var stats = statsProSpieler.getOrDefault(nr, new SpielerStats(0, 0, 0, 0));
            int fehlt = anzRunden - stats.spiele();

            int erwPlus = stats.plus() + fehlt * nichtGespieltPlus;
            int erwMinus = stats.minus() + fehlt * nichtGespieltMinus;
            int erwDelta = erwPlus - erwMinus;
            int erwSiege = stats.siege();
            int erwNiederlagen = stats.spiele() + fehlt - erwSiege;
            int erwPunkte = erwSiege - erwNiederlagen;

            int istSiege = row.get(ersteSummeSpalte + SPIELE_PLUS_OFFS).getIntVal(Integer.MIN_VALUE);
            int istPunkte = row.get(ersteSummeSpalte + SPIELE_DIV_OFFS).getIntVal(Integer.MIN_VALUE);
            int istPlus = row.get(ersteSummeSpalte + PUNKTE_PLUS_OFFS).getIntVal(Integer.MIN_VALUE);
            int istMinus = row.get(ersteSummeSpalte + PUNKTE_MINUS_OFFS).getIntVal(Integer.MIN_VALUE);
            int istDelta = row.get(ersteSummeSpalte + PUNKTE_DIV_OFFS).getIntVal(Integer.MIN_VALUE);

            softly.assertThat(istSiege)
                    .as("Spieltag %d, Spieler #%d: Siege", spieltagNr, nr).isEqualTo(erwSiege);
            softly.assertThat(istPunkte)
                    .as("Spieltag %d, Spieler #%d: Punkte", spieltagNr, nr).isEqualTo(erwPunkte);
            softly.assertThat(istPlus)
                    .as("Spieltag %d, Spieler #%d: Σ+", spieltagNr, nr).isEqualTo(erwPlus);
            softly.assertThat(istMinus)
                    .as("Spieltag %d, Spieler #%d: Σ−", spieltagNr, nr).isEqualTo(erwMinus);
            softly.assertThat(istDelta)
                    .as("Spieltag %d, Spieler #%d: Δ", spieltagNr, nr).isEqualTo(erwDelta);

            ranglisteEintraege.add(new RanglisteEintrag(nr, istSiege, istPunkte, istDelta, istPlus));
        }

        softly.assertAll();
        pruefeRanglisteSortierung(spieltagNr, ranglisteEintraege);
    }

    // ----- Rangliste-Sortierung -----

    private void pruefeRanglisteSortierung(int spieltagNr, List<RanglisteEintrag> eintraege) {
        var erwartet = eintraege.stream()
                .sorted(Comparator.<RanglisteEintrag>comparingInt(e -> -e.siege())
                        .thenComparingInt(e -> -e.punkte())
                        .thenComparingInt(e -> -e.delta())
                        .thenComparingInt(e -> -e.sumPlus()))
                .map(RanglisteEintrag::nr)
                .toList();
        var aktuell = eintraege.stream().map(RanglisteEintrag::nr).toList();
        assertThat(aktuell)
                .as("Spieltag %d: Rangliste-Sortierung (Siege↓ → Punkte↓ → Δ↓ → Σ+↓)", spieltagNr)
                .isEqualTo(erwartet);
    }

    // ----- Hilfsdatenstrukturen -----

    record SpielerStats(int plus, int minus, int spiele, int siege) {
    }

    record SortiertesPaar(int a, int b) {
        static SortiertesPaar von(int x, int y) {
            return x <= y ? new SortiertesPaar(x, y) : new SortiertesPaar(y, x);
        }
    }

    record RanglisteEintrag(int nr, int siege, int punkte, int delta, int sumPlus) {
    }

    record SpielrundenDaten(
            Map<Integer, SpielerStats> statsProSpieler,
            Map<SortiertesPaar, Integer> mitspielerZaehler) {
    }
}
