/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungKonstanten;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeleeAnmeldungSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeleeAnmeldungUebernehmenSheet;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/**
 * Zusammenspiel PTM ↔ PTM Online bei Mêlée-Anmeldung (Schweizer Doublette): Online melden sich Einzelne an, PTM bildet
 * die Teams beim Übernehmen in die Meldeliste. Geprüft gegen den lokal gestarteten Worker: Import in die
 * Mêlée-Anmeldeliste, Online-Anlage vor Ort Erfasster, Turnierstart und die erste Runde online mit den in PTM
 * gebildeten Teams.
 */
class PtmOnlineMeleeZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);

    @BeforeEach
    void zufallFestlegen() {
        RandomSource.setSeed(42L);
    }

    @AfterEach
    void zufallZuruecksetzen() {
        RandomSource.reset();
    }

    @Test
    void einzelneWerdenInPtmZuTeamsUndSpielenOnlineInDiesenTeams() throws Exception {
        meleeTurnierAnlegen(Formation.DOUBLETTE, IN_EINEM_MONAT);
        Map<String, Person> angemeldet = new LinkedHashMap<>();
        for (int i = 1; i <= 13; i++) {
            Person person = Person.gast("Einzel" + i, "Melee");
            angemeldet.put(online.anmelden(turnierId, person), person);
        }
        Person vorOrt = Person.gast("Paul", "VorOrt");
        lokaleMeldung(vorOrt);
        verbinden();

        abgleichen();

        assertThat(lokaleMeldungen()).as("Mêlée-Anmeldeliste: 13 online + 1 vor Ort, je eine Person").hasSize(14)
                .allSatisfy((zeile, namen) -> assertThat(namen).hasSize(1));
        assertThat(angemeldet.keySet()).allSatisfy(id -> assertThat(mapping.istBereitsImportiert(id)).isTrue());

        for (int zeile : lokaleMeldungen().keySet()) {
            meleeEinchecken(zeile);
        }
        new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();
        MeldelisteZiel meldeliste = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
        assertThat(meldeliste.leseAlleSpielerRoh()).as("alle 14 in Doublette-Teams").hasSize(14);

        Set<Integer> ausgelost = schweizerRundeAuslosen();

        assertThat(ausgelost).as("7 Teams ausgelost").hasSize(7);
        warteBis("Turnier online gestartet",
                () -> "running".equals(online.turnier(turnierId).get("status").getAsString()));
        warteBis("vor Ort Erfasster online angelegt, alle aktiv", () -> {
            List<JsonObject> anmeldungen = online.anmeldungen(turnierId);
            return anmeldungen.size() == 14
                    && anmeldungen.stream().allMatch(a -> "active".equals(a.get("participation").getAsString()));
        });
        warteBis("Runde 1 online mit den in PTM gebildeten Doublette-Teams", () -> {
            List<List<String>> teams = teamsDerErstenRunde();
            Set<String> alle = new HashSet<>();
            teams.forEach(alle::addAll);
            return teams.size() == 7 && teams.stream().allMatch(team -> team.size() == 2) && alle.size() == 14;
        });
    }

    /** Eingecheckt-Markierung in der Mêlée-Anmeldeliste. */
    private void meleeEinchecken(int zeile1Basiert) throws Exception {
        sheetHlp.setStringValueInCell(StringCellValue.from(new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet)
                .getXSpreadSheet(), Position.from(MeleeAnmeldungKonstanten.SPALTE_EINGECHECKT, zeile1Basiert - 1))
                .setValue(MeleeAnmeldungKonstanten.MARKIERUNG));
    }

    /** Teams der ersten Online-Runde als Listen von Online-IDs (je Spieler eine Einzelanmeldung). */
    private List<List<String>> teamsDerErstenRunde() throws Exception {
        List<JsonObject> runden = online.runden(turnierId);
        List<List<String>> teams = new ArrayList<>();
        if (runden.isEmpty()) {
            return teams;
        }
        for (JsonElement element : runden.getFirst().getAsJsonArray("matches")) {
            JsonObject spiel = element.getAsJsonObject();
            for (String seite : List.of("teamA", "teamB")) {
                List<String> team = new ArrayList<>();
                spiel.getAsJsonArray(seite).forEach(spieler -> team.add(spieler.getAsJsonObject().get("id").getAsString()));
                if (!team.isEmpty()) {
                    teams.add(team);
                }
            }
        }
        return teams;
    }
}
