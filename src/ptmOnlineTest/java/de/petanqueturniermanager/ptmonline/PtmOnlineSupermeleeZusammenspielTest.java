/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;

/**
 * Zusammenspiel PTM ↔ PTM Online bei Supermêlée (E-25): Die Meldeliste ist der Spielerpool aller Spieltage, jeder
 * Spieltag ist online ein eigenes Turnier mit Einzelanmeldungen. Geprüft gegen den lokal gestarteten Worker – ein
 * Spieltag vom Import bis zum Start, und der Wechsel auf einen zweiten Spieltag mit Stammspielern aus dem Pool.
 */
class PtmOnlineSupermeleeZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final Person VOR_ORT = Person.gast("Vera", "VorOrt");

    @BeforeEach
    void zufallFestlegen() {
        RandomSource.setSeed(42L);
    }

    @AfterEach
    void zufallZuruecksetzen() {
        RandomSource.reset();
    }

    @Test
    void einSpieltagVomImportBisZumStart() throws Exception {
        supermeleeMeldelisteAnlegen();
        supermeleeSpieltagOnlineAnlegen(IN_EINEM_MONAT);
        Map<String, Person> onlineSpieler = onlineAnmelden(spieler("Tag1", 7));
        lokaleMeldung(VOR_ORT);
        verbinden();

        abgleichen();

        assertThat(lokaleMeldungen()).as("Pool: 7 online + 1 vor Ort, je eine Person").hasSize(8)
                .allSatisfy((zeile, namen) -> assertThat(namen).hasSize(1));
        assertThat(onlineSpieler.keySet()).allSatisfy(id -> assertThat(mapping.istBereitsImportiert(id)).isTrue());
        assertThat(online.anmeldungen(turnierId)).as("ohne Eintrag am Spieltag nicht online angelegt (E-25)")
                .hasSize(7);

        for (int zeile : lokaleMeldungen().keySet()) {
            einchecken(zeile);
        }
        abgleichen();

        assertThat(online.anmeldungen(turnierId)).as("am Spieltag gemeldet → online angelegt").hasSize(8)
                .filteredOn(a -> "document".equals(a.get("origin").getAsString())).singleElement()
                .satisfies(a -> assertThat(name(a)).isEqualTo(VOR_ORT.name()));

        ersteRundeAuslosen();

        warteBis("Spieltag online gestartet",
                () -> "running".equals(online.turnier(turnierId).get("status").getAsString()));
        warteBis("alle eingecheckten Spieler online aktiv", () -> online.anmeldungen(turnierId).stream()
                .allMatch(a -> "active".equals(a.get("participation").getAsString())));
    }

    @Test
    void zweiterSpieltagVerknuepftStammspielerUndLegtNurSpieltagsMeldungenAn() throws Exception {
        supermeleeMeldelisteAnlegen();
        supermeleeSpieltagOnlineAnlegen(IN_EINEM_MONAT);
        String spieltag1 = turnierId;
        List<Person> pool = spieler("Pool", 8);
        onlineAnmelden(pool);
        verbinden();
        abgleichen();
        for (int zeile : lokaleMeldungen().keySet()) {
            einchecken(zeile);
        }
        ersteRundeAuslosen();
        warteBis("Spieltag 1 online gestartet",
                () -> "running".equals(online.turnier(spieltag1).get("status").getAsString()));

        new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet).naechsteSpieltag();
        aktivenSpieltagUebernehmen();

        assertThat(spieltagNr).isEqualTo(2);
        assertThat(online.turnier(spieltag1).get("documentManaged").getAsBoolean())
                .as("neuer Spieltag trennt Spieltag 1 online").isFalse();
        assertThat(new PtmOnlineRegistrationMapping(wkingSpreadsheet, system, 1).getTournamentId())
                .as("Sync-Blatt von Spieltag 1 archiviert").isEmpty();

        supermeleeSpieltagOnlineAnlegen(IN_EINEM_MONAT.plusWeeks(1));
        Person stammspieler = pool.getFirst();
        Person neu = Person.gast("Nina", "Neu");
        String idStamm = online.anmelden(turnierId, stammspieler);
        String idNeu = online.anmelden(turnierId, neu);
        verbinden();

        abgleichen();

        assertThat(lokaleMeldungen()).as("Stammspieler nicht doppelt, Neue im Pool").hasSize(9);
        assertThat(mapping.getLokaleUuid(idStamm)).as("Stammspieler ohne Rückfrage mit seiner Pool-Zeile verknüpft")
                .contains(ziel.getOderErzeugeLokaleUuid(zeile(stammspieler)));
        assertThat(mapping.istBereitsImportiert(idNeu)).isTrue();
        assertThat(konfliktArten()).doesNotContain(KonfliktArt.MOEGLICH_IDENTISCH);
        assertThat(online.anmeldungen(turnierId)).as("übriger Pool bleibt lokal").hasSize(2);

        // Am Spieltag 2 spielen: Stammspieler, die Neue und vier weitere aus dem Pool; drei aus dem Pool fehlen.
        List<Person> heute = new ArrayList<>(List.of(stammspieler, neu));
        heute.addAll(pool.subList(1, 5));
        for (Person person : heute) {
            einchecken(zeile(person));
        }
        abgleichen();

        assertThat(online.anmeldungen(turnierId)).extracting(PtmOnlineSupermeleeZusammenspielTest::name)
                .as("online nur, wer an Spieltag 2 gemeldet ist (E-25)")
                .containsExactlyInAnyOrderElementsOf(heute.stream().map(Person::name).toList());
        assertThat(online.anmeldungen(spieltag1)).as("Spieltag 1 bleibt unverändert").hasSize(pool.size());

        ersteRundeAuslosen();

        warteBis("Spieltag 2 online gestartet",
                () -> "running".equals(online.turnier(turnierId).get("status").getAsString()));
    }

    /** Einzelanmeldungen ohne Konto; liefert Online-ID → Person. */
    private Map<String, Person> onlineAnmelden(List<Person> personen) throws Exception {
        Map<String, Person> angemeldet = new LinkedHashMap<>();
        for (Person person : personen) {
            angemeldet.put(online.anmelden(turnierId, person), person);
        }
        return angemeldet;
    }

    private static List<Person> spieler(String praefix, int anzahl) {
        List<Person> personen = new ArrayList<>();
        for (int i = 1; i <= anzahl; i++) {
            personen.add(Person.gast(praefix + i, "Spieler"));
        }
        return personen;
    }

    /** Spielrunde 1 des aktiven Spieltags wie über das Menü; die Spielrunde muss danach existieren. */
    private void ersteRundeAuslosen() throws Exception {
        SpielrundeSheet_Naechste runde = new SpielrundeSheet_Naechste(wkingSpreadsheet);
        runde.run();
        assertThat(sheetHlp.findByName(runde.getSheetName(SpielTagNr.from(spieltagNr), SpielRundeNr.from(1))))
                .as("Spielrunde 1 von Spieltag %s angelegt", spieltagNr).isNotNull();
    }

    private static String name(JsonObject anmeldung) {
        return anmeldung.get("firstName").getAsString() + " " + anmeldung.get("lastName").getAsString();
    }
}
