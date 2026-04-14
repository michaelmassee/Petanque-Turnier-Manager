/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.algorithmen.KaskadenFeldBelegung;
import de.petanqueturniermanager.algorithmen.KaskadenKoGruppenRunde;
import de.petanqueturniermanager.algorithmen.KaskadenKoRundenPlan;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Liest aus den gespeicherten Kaskadenrunden-Sheets die endgültige Teamverteilung
 * und liefert pro Endfeld (A, B, C, D …) eine {@link KaskadenFeldBelegung}.<br>
 * <br>
 * Die Logik verfolgt für jede Gruppe den Sieger- und Verlierer-Pfad über alle
 * abgeschlossenen Kaskadenrunden und baut daraus eine Setzliste auf.
 * Freilos-Schutz: Kein Team erhält in zwei Runden hintereinander ein Freilos.
 *
 * @author Michael Massee
 * @see KaskadenFeldBelegung
 */
public class KaskadeRundenErgebnisLeser {

    private static final Logger LOGGER = LogManager.getLogger(KaskadeRundenErgebnisLeser.class);

    /**
     * Spaltenoffsets im Kaskadenrunden-Sheet (entsprechen den Konstanten in
     * {@link KaskadeSpielrundeSheet}).
     */
    private static final int TEAM_A_SPALTE     = KaskadeSpielrundeSheet.TEAM_A_SPALTE;
    private static final int ERG_TEAM_B_SPALTE = KaskadeSpielrundeSheet.ERG_TEAM_B_SPALTE;
    private static final int ERSTE_DATEN_ZEILE = KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE;

    private final WorkingSpreadsheet workingSpreadsheet;

    public KaskadeRundenErgebnisLeser(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    /**
     * Berechnet aus den Kaskadenrunden-Sheets die endgültige Teamverteilung auf
     * alle Endfelder des Plans.<br>
     * <br>
     * Gibt für jedes Endfeld (in Leistungsreihenfolge: A zuerst) eine
     * {@link KaskadenFeldBelegung} zurück, die sowohl die Feldinformation als auch
     * die geordneten Teamnummern enthält.
     *
     * @param plan vollständiger Kaskadenplan (muss mindestens 1 Kaskadenstufe haben)
     * @return unveränderliche Liste der Feldbelegungen in Leistungsreihenfolge
     * @throws GenerateException bei Lesefehler aus einem Sheet
     */
    public List<KaskadenFeldBelegung> ladeFeldBelegungen(KaskadenKoRundenPlan plan) throws GenerateException {
        // naechsteRundeNr = kaskadenStufen + 1: "die Runde nach allen Kaskaden"
        var pfadMap = teamPositionenBerechnen(plan.kaskadenStufen() + 1, plan);

        var ergebnis = new ArrayList<KaskadenFeldBelegung>(plan.felder().size());
        for (var feld : plan.felder()) {
            var teams = pfadMap.getOrDefault(feld.pfad(), List.of());
            ergebnis.add(new KaskadenFeldBelegung(feld, Collections.unmodifiableList(teams)));
        }
        return Collections.unmodifiableList(ergebnis);
    }

    /**
     * Berechnet die Teamverteilung auf Gruppen für die angegebene Folgerunde.<br>
     * Wird von {@link KaskadeSpielrundeSheet} verwendet, um die Teamnummern für
     * Kaskadenrunden &gt; 1 zu ermitteln (nicht für die finalen KO-Felder).
     *
     * @param naechsteRundeNr Nummer der nächsten Kaskadenrunde (≥ 2)
     * @param plan            vollständiger Kaskadenplan
     * @return Map {@code pfad → geordnete Teamnummern-Liste}
     * @throws GenerateException bei Lesefehler
     */
    Map<String, List<Integer>> ladeZwischenPositionen(int naechsteRundeNr, KaskadenKoRundenPlan plan)
            throws GenerateException {
        return teamPositionenBerechnen(naechsteRundeNr, plan);
    }

    // ---------------------------------------------------------------
    // Interne Berechnungslogik (aus KaskadeSpielrundeSheet extrahiert)
    // ---------------------------------------------------------------

    /**
     * Berechnet die Teampositionen für Runde {@code naechsteRundeNr} aus den
     * Ergebnissen aller vorherigen Runden.
     * <p>
     * Iteriert von Runde 1 bis {@code naechsteRundeNr - 1}: liest jeweils das
     * Sheet der Runde, ermittelt Sieger (→ S-Gruppe) und Verlierer (→ V-Gruppe)
     * und baut so die Positions-Map der nächsten Runde auf.
     * <p>
     * <b>Freilos-Schutz:</b> Ein Team, das in einer früheren Runde ein Freilos
     * erhalten hat, wird niemals an die letzte Position einer Gruppe gesetzt.
     * Die letzte Position bestimmt, wer in der nächsten Runde das Freilos bekommt.
     *
     * @return Map {@code pfad → geordnete Teamnummern-Liste (0-basierter Index = Position − 1)}
     */
    private Map<String, List<Integer>> teamPositionenBerechnen(int naechsteRundeNr, KaskadenKoRundenPlan plan)
            throws GenerateException {
        Map<String, List<Integer>> aktuelleMap = new HashMap<>();
        Set<Integer> bereitsFreilosTeams = new HashSet<>();

        for (int rundenNr = 1; rundenNr < naechsteRundeNr; rundenNr++) {
            var rundenInfo  = plan.kaskadeRunden().get(rundenNr - 1);
            var sheetDaten  = leseRundenSheetDaten(rundenNr);
            var naechsteMap = new HashMap<String, List<Integer>>();
            int zeilenOffset = 0;

            for (var gruppe : rundenInfo.gruppenRunden()) {
                var pfad          = gruppe.pfad();
                var teamsInGruppe = (rundenNr == 1)
                        ? leseTeamNrsAusGruppe(sheetDaten, zeilenOffset, gruppe)
                        : aktuelleMap.getOrDefault(pfad, Collections.emptyList());

                List<Integer> sieger    = new ArrayList<>();
                List<Integer> verlierer = new ArrayList<>();

                for (int i = 0; i < gruppe.spielPaare().size(); i++) {
                    var spiel = gruppe.spielPaare().get(i);
                    int teamA = spiel.positionA() <= teamsInGruppe.size()
                            ? teamsInGruppe.get(spiel.positionA() - 1) : 0;
                    int teamB = spiel.positionB() <= teamsInGruppe.size()
                            ? teamsInGruppe.get(spiel.positionB() - 1) : 0;

                    var row  = sheetDaten.get(zeilenOffset + i);
                    int ergA = row.size() > 2 ? row.get(2).getIntVal(0) : 0;
                    int ergB = row.size() > 3 ? row.get(3).getIntVal(0) : 0;

                    if (ergA >= ergB) {
                        sieger.add(teamA);
                        verlierer.add(teamB);
                    } else {
                        sieger.add(teamB);
                        verlierer.add(teamA);
                    }
                }

                if (gruppe.anzFreilose() > 0) {
                    int freilosPos  = gruppe.anzTeams() - 1;
                    int freilosTeam = freilosPos < teamsInGruppe.size()
                            ? teamsInGruppe.get(freilosPos) : 0;

                    var freilosRow  = sheetDaten.get(zeilenOffset + gruppe.spielPaare().size());
                    int freilosErgA = freilosRow.size() > 2 ? freilosRow.get(2).getIntVal(0) : 0;
                    int freilosErgB = freilosRow.size() > 3 ? freilosRow.get(3).getIntVal(0) : 0;

                    if (freilosTeam > 0) {
                        bereitsFreilosTeams.add(freilosTeam);
                    }
                    if (freilosErgA > freilosErgB) {
                        sieger.add(freilosTeam);
                    } else {
                        verlierer.add(freilosTeam);
                    }
                }

                sieger    = verhindereFreilosDuplikat(sieger,    bereitsFreilosTeams);
                verlierer = verhindereFreilosDuplikat(verlierer, bereitsFreilosTeams);

                naechsteMap.put(pfad + "S", Collections.unmodifiableList(sieger));
                naechsteMap.put(pfad + "V", Collections.unmodifiableList(verlierer));
                zeilenOffset += gruppe.spielPaare().size() + gruppe.anzFreilose();
            }

            aktuelleMap = naechsteMap;
        }

        return aktuelleMap;
    }

    /**
     * Liest das Daten-Array (Team A bis Erg. B) aus dem Sheet der angegebenen
     * Kaskadenrunde.
     * <p>
     * Spalten-Indizes im Ergebnis: 0 = Team A, 1 = Team B, 2 = Erg. A, 3 = Erg. B
     */
    private RangeData leseRundenSheetDaten(int rundenNr) throws GenerateException {
        var xDoc  = workingSpreadsheet.getWorkingSpreadsheetDocument();
        var sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
                SheetMetadataHelper.schluesselKaskadenRunde(rundenNr),
                SheetNamen.kaskadenRunde(rundenNr));
        if (sheet == null) {
            return new RangeData();
        }
        var readRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE,
                ERG_TEAM_B_SPALTE, ERSTE_DATEN_ZEILE + 999);
        return RangeHelper.from(sheet, xDoc, readRange).getDataFromRange();
    }

    /**
     * Liest aus dem Sheet-Daten-Array die Teamnummern einer Gruppe in Runde 1.
     * <p>
     * Gibt eine Liste zurück, bei der Index {@code position − 1} die Teamnummer
     * enthält. Sequenzielles Paarungsschema: Zeile i enthält Team A (pos 2i+1)
     * und Team B (pos 2i+2); bei Freilos enthält die letzte Zeile nur Team A.
     */
    private static List<Integer> leseTeamNrsAusGruppe(RangeData sheetDaten, int zeilenOffset,
            KaskadenKoGruppenRunde gruppe) {
        var teams = new ArrayList<Integer>(gruppe.anzTeams());
        for (int i = 0; i < gruppe.spielPaare().size(); i++) {
            var row = sheetDaten.get(zeilenOffset + i);
            teams.add(row.size() > 0 ? row.get(0).getIntVal(0) : 0);  // Team A
            teams.add(row.size() > 1 ? row.get(1).getIntVal(0) : 0);  // Team B
        }
        if (gruppe.anzFreilose() > 0) {
            var freilosRow = sheetDaten.get(zeilenOffset + gruppe.spielPaare().size());
            teams.add(freilosRow.size() > 0 ? freilosRow.get(0).getIntVal(0) : 0);
        }
        return teams;
    }

    /**
     * Verhindert, dass ein Team ein zweites Freilos erhält.
     * <p>
     * Bei ungerader Listenlänge bekommt das letzte Element in der nächsten Runde
     * ein Freilos. Wenn dieses Team bereits ein Freilos hatte, wird es mit dem
     * letzten Team ohne Freilos getauscht.
     *
     * @param teams               geordnete Teamnummern-Liste
     * @param bereitsFreilosTeams Menge der Teams mit bereits erhaltenem Freilos
     * @return ggf. neu geordnete Liste ohne Freilos-Duplikat an letzter Stelle
     */
    private static List<Integer> verhindereFreilosDuplikat(
            List<Integer> teams, Set<Integer> bereitsFreilosTeams) {
        if (teams.size() % 2 == 0) {
            return teams;
        }
        int letzteIdx = teams.size() - 1;
        if (!bereitsFreilosTeams.contains(teams.get(letzteIdx))) {
            return teams;
        }
        for (int i = letzteIdx - 1; i >= 0; i--) {
            if (!bereitsFreilosTeams.contains(teams.get(i))) {
                var ergebnis = new ArrayList<>(teams);
                Collections.swap(ergebnis, i, letzteIdx);
                return ergebnis;
            }
        }
        LOGGER.warn("Alle Teams in Gruppe hatten bereits ein Freilos. Kein Tausch möglich.");
        return teams;
    }
}
