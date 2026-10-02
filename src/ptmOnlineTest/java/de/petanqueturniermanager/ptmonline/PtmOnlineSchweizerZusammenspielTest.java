/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Zusammenspiel PTM ↔ PTM Online an einem Schweizer Triplette-Turnier, gegen den lokal gestarteten Worker: Import,
 * Online-Anlage vor Ort erfasster Meldungen, Entscheidungen in der Konfliktliste, Ausschluss gesperrter Meldungen von
 * der Auslosung, Turnierstart, Online-Storno und Schließen der Online-Anmeldung.
 */
class PtmOnlineSchweizerZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);

    private static final Person[] TEAM_A = { Person.gast("Anna", "Adler"), Person.gast("Arno", "Amsel"),
            Person.gast("Ada", "Auer") };
    private static final Person[] TEAM_B = { Person.gast("Bert", "Berg"), Person.gast("Bea", "Busch"),
            Person.gast("Bodo", "Brand") };
    /** Triplette zu zweit: zulässig, aber bis zur Auslosung unvollständig (E-20). */
    private static final Person[] TEAM_ZU_ZWEIT = { Person.gast("Dora", "Dahl"), Person.gast("Dirk", "Decker") };
    private static final Person[] TEAM_VOR_ORT = { Person.gast("Lena", "Lokal"), Person.gast("Lars", "Lind"),
            Person.gast("Lea", "Lutz") };

    @Test
    void abgleichUebernimmtOnlineAnmeldungenUndLegtVorOrtErfassteOnlineAn() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        String idA = online.anmelden(turnierId, TEAM_A);
        String idZuZweit = online.anmelden(turnierId, TEAM_ZU_ZWEIT);
        lokaleMeldung(TEAM_VOR_ORT);
        verbinden();

        abgleichen();

        assertThat(lokaleMeldungen().values()).as("Meldeliste: vor Ort erfasst + beide Online-Anmeldungen")
                .containsExactlyInAnyOrder(namen(TEAM_VOR_ORT), namen(TEAM_A), namen(TEAM_ZU_ZWEIT));
        assertThat(mapping.istBereitsImportiert(idA)).isTrue();
        assertThat(mapping.istBereitsImportiert(idZuZweit)).isTrue();
        List<JsonObject> anmeldungen = online.anmeldungen(turnierId);
        assertThat(anmeldungen).as("vor Ort erfasste Meldung online angelegt").hasSize(3)
                .extracting(PtmOnlineSchweizerZusammenspielTest::personen)
                .containsExactlyInAnyOrder(namen(TEAM_VOR_ORT), namen(TEAM_A), namen(TEAM_ZU_ZWEIT));
        String uuidVorOrt = ziel.getOderErzeugeLokaleUuid(zeile(TEAM_VOR_ORT[0]));
        assertThat(anmeldungen).as("Online-Anlage mit der lokalen UUID als Idempotenzschlüssel (T-24)")
                .filteredOn(a -> "document".equals(a.get("origin").getAsString())).singleElement()
                .satisfies(a -> assertThat(a.get("localRegistrationUuid").getAsString()).isEqualTo(uuidVorOrt));
        assertThat(mapping.getLokaleUuid(idA)).as("Zuordnung im Dokument (T-04)")
                .contains(ziel.getOderErzeugeLokaleUuid(zeile(TEAM_A[0])));
        assertThat(anmeldung(anmeldungen, idZuZweit).get("incomplete").getAsBoolean()).isTrue();

        abgleichen();

        assertThat(lokaleMeldungen()).as("zweiter Abgleich ändert nichts").hasSize(3);
        assertThat(online.anmeldungen(turnierId)).as("keine doppelte Online-Anlage").hasSize(3);
    }

    @Test
    void namensgleicheVorOrtMeldungWirdErstNachEntscheidungVerknuepft() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        String idA = online.anmelden(turnierId, TEAM_A);
        int zeileVorOrt = lokaleMeldung(TEAM_A);
        verbinden();

        abgleichen();

        assertThat(mapping.istBereitsImportiert(idA)).as("nicht automatisch verknüpft (KP-06 a2)").isFalse();
        assertThat(konfliktArten()).contains(KonfliktArt.MOEGLICH_IDENTISCH);
        assertThat(lokaleMeldungen()).as("kein Duplikat in der Meldeliste").hasSize(1);
        String uuid = ziel.getOderErzeugeLokaleUuid(zeileVorOrt);
        mapping.konfliktListe().setzeEntscheidung(
                KonfliktFall.schluessel(KonfliktArt.MOEGLICH_IDENTISCH, uuid, List.of(idA)),
                Entscheidung.VERKNUEPFEN.anzeige());

        abgleichen();

        assertThat(mapping.istBereitsImportiert(idA)).as("nach „verknüpfen“").isTrue();
        assertThat(konfliktArten()).doesNotContain(KonfliktArt.MOEGLICH_IDENTISCH);
        assertThat(mapping.getLokaleUuid(idA)).as("mit der vor Ort erfassten Zeile verknüpft").contains(uuid);
        assertThat(online.anmeldungen(turnierId)).as("keine zusätzliche Online-Anlage").hasSize(1);
    }

    @Test
    void kontoInZweiAnmeldungenSperrtBeideBisVerschiedenePersonen() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        Person[] mitKonto = { Person.konto("Kai", "Kunz", 1), Person.gast("Kim", "Kurz"), Person.gast("Kurt", "Kolb") };
        Person[] auchMitKonto = { Person.gast("Mia", "Moll"), Person.konto("Kai", "Kunz", 1), Person.gast("Max", "Mohr") };
        String id1 = online.anmelden(turnierId, mitKonto);
        String id2 = online.anmelden(turnierId, auchMitKonto);
        assertThat(online.anmeldungen(turnierId)).as("Server markiert die Doppelbelegung (KP-06 b)")
                .allSatisfy(a -> assertThat(a.get("accountConflict").getAsBoolean()).isTrue());
        verbinden();

        abgleichen();

        assertThat(konfliktArten()).contains(KonfliktArt.KONTO_KONFLIKT);
        assertThat(RegistrationImportTask.gesperrteZeilen(mapping, ziel)).as("beide Meldungen gesperrt")
                .containsOnlyKeys(zeile(mitKonto[1]), zeile(auchMitKonto[0]));
        mapping.konfliktListe().setzeEntscheidung(
                KonfliktFall.schluessel(KonfliktArt.KONTO_KONFLIKT, null, List.of(id1, id2)),
                Entscheidung.VERSCHIEDENE_PERSONEN.anzeige());

        abgleichen();

        assertThat(RegistrationImportTask.gesperrteZeilen(mapping, ziel)).as("als verschiedene Personen freigegeben")
                .isEmpty();
        assertThat(konfliktArten()).doesNotContain(KonfliktArt.KONTO_KONFLIKT);
    }

    @Test
    void ersteRundeLostUnvollstaendigeNichtAusUndStartetDasTurnierOnline() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        Map<String, Person[]> vollstaendig = vollstaendigeTeams(7);
        String idZuZweit = online.anmelden(turnierId, TEAM_ZU_ZWEIT);
        verbinden();
        abgleichen();
        for (Person[] team : vollstaendig.values()) {
            einchecken(zeile(team[0]));
        }
        einchecken(zeile(TEAM_ZU_ZWEIT[0]));

        Set<Integer> ausgelost = schweizerRundeAuslosen();

        assertThat(ausgelost).as("eingecheckt und vollständig ausgelost, unvollständig nicht (P-42)")
                .containsExactlyInAnyOrderElementsOf(vollstaendig.values().stream().map(this::teamNr).toList());
        warteBis("Turnier online gestartet", () -> "running".equals(online.turnier(turnierId).get("status")
                .getAsString()));
        warteBis("Teilnahme der ausgelosten Teams online", () -> {
            Map<String, String> teilnahme = online.anmeldungen(turnierId).stream().collect(Collectors.toMap(
                    a -> a.get("id").getAsString(), a -> a.get("participation").getAsString()));
            return vollstaendig.keySet().stream().allMatch(id -> "active".equals(teilnahme.get(id)));
        });
        assertThat(anmeldung(online.anmeldungen(turnierId), idZuZweit).get("participation").getAsString())
                .as("nicht ausgelost, bleibt aber eingecheckt (P-42)").isEqualTo("active");
    }

    @Test
    void spielrundeUndErgebnisseErscheinenOnlineInDerLiveAnsicht() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        Map<String, Person[]> teams = vollstaendigeTeams(6);
        verbinden();
        abgleichen();
        for (Person[] team : teams.values()) {
            einchecken(zeile(team[0]));
        }
        Map<Integer, String> idProTeamNr = new HashMap<>();
        teams.forEach((id, team) -> idProTeamNr.put(teamNr(team), id));

        schweizerRundeAuslosen();

        Set<Set<String>> paareImBlatt = new HashSet<>();
        for (int[] paar : paareImBlatt()) {
            paareImBlatt.add(Set.of(idProTeamNr.get(paar[1]), idProTeamNr.get(paar[2])));
        }
        assertThat(paareImBlatt).hasSize(3);
        warteBis("Runde 1 mit den Paarungen des Dokuments online", () -> paareOnline().equals(paareImBlatt));

        for (int[] paar : paareImBlatt()) {
            ergebnisEintragen(paar[0], 13, 7);
        }
        // Im Betrieb meldet LibreOffice die Änderung; der Beobachter überträgt sie mit kurzer Verzögerung.
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());

        warteBis("Ergebnisse online", () -> spieleOnline().stream().allMatch(
                spiel -> istZahl(spiel, "scoreA", 13) && istZahl(spiel, "scoreB", 7)));
    }

    @Test
    void onlineStornoNachDemImportMeldetDieZeileLokalAb() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        String idA = online.anmelden(turnierId, TEAM_A);
        online.anmelden(turnierId, TEAM_B);
        verbinden();
        abgleichen();
        einchecken(zeile(TEAM_A[0]));
        einchecken(zeile(TEAM_B[0]));

        online.stornieren(turnierId, idA);
        abgleichen();

        assertThat(lokaleMeldungen().values()).as("Abgleich löscht nichts (E-14)").contains(namen(TEAM_A));
        assertThat(ziel.getAktivWertAusZeile(zeile(TEAM_A[0]))).as("online storniert → abgemeldet").isEqualTo(2);
        assertThat(ziel.getAktivWertAusZeile(zeile(TEAM_B[0]))).as("unverändert eingecheckt").isEqualTo(1);
        assertThat(konfliktArten()).contains(KonfliktArt.ONLINE_AUSGESCHLOSSEN);
    }

    @Test
    void vorDemTurniertagSchliesstDerAbgleichDieOnlineAnmeldungErstNachDemErstenCheckIn() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        online.anmelden(turnierId, TEAM_A);
        verbinden();
        abgleichen();
        assertThat(online.turnier(turnierId).get("registrationClosed").getAsBoolean()).as("ohne Check-in offen")
                .isFalse();
        einchecken(zeile(TEAM_A[0]));

        abgleichen();

        warteBis("Online-Anmeldung geschlossen (KP-05)",
                () -> online.turnier(turnierId).get("registrationClosed").getAsBoolean());
    }

    @Test
    void amTurniertagSchliesstDerAbgleichDieOnlineAnmeldungSchonVorDemCheckIn() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, LocalDate.now());
        online.anmelden(turnierId, TEAM_A);
        verbinden();

        abgleichen();

        warteBis("Online-Anmeldung geschlossen (KP-05)",
                () -> online.turnier(turnierId).get("registrationClosed").getAsBoolean());
        assertThat(lokaleMeldungen().values()).as("vorher noch übernommen").containsExactly(namen(TEAM_A));
    }

    /** Vollständige Triplette-Teams, online angemeldet; Online-ID → Team. */
    private Map<String, Person[]> vollstaendigeTeams(int anzahl) throws Exception {
        Map<String, Person[]> teams = new LinkedHashMap<>();
        for (int i = 1; i <= anzahl; i++) {
            Person[] team = { Person.gast("Voll" + i, "Erster"), Person.gast("Voll" + i, "Zweiter"),
                    Person.gast("Voll" + i, "Dritter") };
            teams.put(online.anmelden(turnierId, team), team);
        }
        return teams;
    }

    /** Paarungen der zuletzt ausgelosten Runde: {Blattzeile, Team A, Team B}. */
    private List<int[]> paareImBlatt() throws Exception {
        int erste = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
        List<int[]> paare = new ArrayList<>();
        int zeile = erste;
        for (RowData daten : RangeHelper.from(letzteRunde.getXSpreadSheet(),
                wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                RangePosition.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, erste,
                        SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, erste + 10))
                .getDataFromRange()) {
            int teamA = daten.get(0).getIntVal(0);
            int teamB = daten.get(1).getIntVal(0);
            if (teamA > 0 && teamB > 0) {
                paare.add(new int[] { zeile, teamA, teamB });
            }
            zeile++;
        }
        return paare;
    }

    private void ergebnisEintragen(int zeile, int punkteA, int punkteB) throws Exception {
        var blatt = letzteRunde.getXSpreadSheet();
        sheetHlp.setNumberValueInCell(NumberCellValue.from(blatt,
                Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile)).setValue(punkteA));
        sheetHlp.setNumberValueInCell(NumberCellValue.from(blatt,
                Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, zeile)).setValue(punkteB));
    }

    /** Spiele der ersten Online-Runde (leer, solange sie nicht übertragen ist). */
    private List<JsonObject> spieleOnline() throws Exception {
        List<JsonObject> runden = online.runden(turnierId);
        List<JsonObject> spiele = new ArrayList<>();
        if (!runden.isEmpty()) {
            runden.getFirst().getAsJsonArray("matches").forEach(spiel -> spiele.add(spiel.getAsJsonObject()));
        }
        return spiele;
    }

    /** Paarungen der ersten Online-Runde als Mengen von Online-IDs (Freilos ohne Gegner nicht mitgezählt). */
    private Set<Set<String>> paareOnline() throws Exception {
        Set<Set<String>> paare = new HashSet<>();
        for (JsonObject spiel : spieleOnline()) {
            if (spiel.getAsJsonArray("teamB").isEmpty()) {
                continue;
            }
            paare.add(Set.of(spiel.getAsJsonArray("teamA").get(0).getAsJsonObject().get("id").getAsString(),
                    spiel.getAsJsonArray("teamB").get(0).getAsJsonObject().get("id").getAsString()));
        }
        return paare;
    }

    private static boolean istZahl(JsonObject spiel, String feld, int wert) {
        return spiel.has(feld) && !spiel.get(feld).isJsonNull() && spiel.get(feld).getAsInt() == wert;
    }

    private int teamNr(Person[] team) {
        return ziel.getTeamNrAusZeile(zeile(team[0]));
    }

    private static JsonObject anmeldung(List<JsonObject> anmeldungen, String id) {
        return anmeldungen.stream().filter(a -> id.equals(a.get("id").getAsString())).findFirst().orElseThrow();
    }

    private static Set<String> personen(JsonObject anmeldung) {
        Set<String> namen = new java.util.TreeSet<>();
        for (String praefix : List.of("", "partner", "partner2")) {
            String vorname = praefix.isEmpty() ? "firstName" : praefix + "FirstName";
            String nachname = praefix.isEmpty() ? "lastName" : praefix + "LastName";
            if (anmeldung.has(vorname) && !anmeldung.get(vorname).isJsonNull()) {
                namen.add(anmeldung.get(vorname).getAsString() + " " + anmeldung.get(nachname).getAsString());
            }
        }
        return namen;
    }
}
