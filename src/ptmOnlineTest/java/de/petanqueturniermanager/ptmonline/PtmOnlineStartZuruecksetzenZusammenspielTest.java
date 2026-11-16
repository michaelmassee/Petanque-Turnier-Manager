/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;

/**
 * Online zurückgesetzter Turnierstart (E-23) gegen den lokal gestarteten Worker. Das Dokument erfährt vom Zurücksetzen
 * erst über die abgelehnte Übertragung der Ergebnisse (Ablehnungen tragen keinen Status, der Online-Zustand wird
 * daraufhin neu abgerufen). Ein Abgleich danach darf das Turnier nicht wieder starten; erst die ausdrücklich neu
 * angelegte zweite Runde nimmt es wieder auf und überträgt den kompletten Stand samt der Ergebnisse von Runde 1 –
 * auch wenn sie angelegt wird, bevor das Dokument vom Zurücksetzen weiß.
 */
class PtmOnlineStartZuruecksetzenZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final String LAEUFT = "running";
    private static final String ANMELDUNG = "registration";
    /** So lange muss das Turnier nach dem Abgleich unverändert bleiben (mehrere Durchläufe des Beobachters). */
    private static final Duration RUHEZEIT = Duration.ofSeconds(5);

    @Test
    void abgleichNachZuruecksetzenStartetNichtUndZweiteRundeNimmtWiederAuf() throws Exception {
        int anzPaare = ersteRundeGestartetUndZurueckgesetztMitErgebnissen();
        warteBis("Zurücksetzen nach abgelehnter Übertragung im Sync-Blatt",
                () -> mapping.getOnlineStatus().equals(Optional.of(ANMELDUNG)));

        abgleichen();

        bleibtFuer(RUHEZEIT, "Turnier bleibt nach dem Abgleich zurückgesetzt", () -> ANMELDUNG.equals(onlineStatus())
                && !mapping.leseAuftragsBestand().hatOffenen(AuftragsArt.START));

        zweiteRundeAnlegen();

        pruefeWiederAufgenommen(anzPaare);
    }

    @Test
    void zweiteRundeVorBekanntemZuruecksetzenNimmtWiederAuf() throws Exception {
        int anzPaare = ersteRundeGestartetUndZurueckgesetztMitErgebnissen();

        // Sofort, ohne abzuwarten, dass das Dokument vom Zurücksetzen erfährt.
        zweiteRundeAnlegen();

        pruefeWiederAufgenommen(anzPaare);
    }

    /** Runde 1 aus dem Dokument gestartet und online, dann online zurückgesetzt; Ergebnisse 13:7 eingetragen. */
    private int ersteRundeGestartetUndZurueckgesetztMitErgebnissen() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        var teams = vollstaendigeTeams(6);
        verbinden();
        abgleichen();
        for (Person[] team : teams.values()) {
            einchecken(zeile(team[0]));
        }
        schweizerRundeAuslosen();
        warteBis("Turnier aus dem Dokument gestartet", () -> LAEUFT.equals(onlineStatus()));
        warteBis("Runde 1 online", () -> online.runden(turnierId).size() == 1);
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());
        warteBis("Start im Sync-Blatt angekommen", () -> mapping.getOnlineStatus().equals(Optional.of(LAEUFT)));

        online.startZuruecksetzen(turnierId);
        List<int[]> paare = paareImBlatt();
        for (int[] paar : paare) {
            ergebnisEintragen(paar[0], 13, 7);
        }
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());
        return paare.size();
    }

    /**
     * Wie über das Menü: Erst der SheetRunner schließt aus, dass die Erfassung des Beobachters (zieht die Rangliste
     * nach) gleichzeitig am Dokument arbeitet.
     */
    private void zweiteRundeAnlegen() throws Exception {
        var rundeZwei = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
        starteUndWarte(rundeZwei);
        assertThat(rundeZwei.isLetzterLaufFehlgeschlagen()).as("Runde 2 ohne Fehler angelegt").isFalse();
        assertThat(SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.schluesselSchweizerSpielrunde(2)))
                .as("Runde 2 im Dokument").isPresent();
    }

    private void pruefeWiederAufgenommen(int anzPaare) throws Exception {
        warteBis("zweite Runde nimmt das Turnier wieder auf", () -> LAEUFT.equals(onlineStatus()));
        warteBis("beide Runden online", () -> online.runden(turnierId).size() == 2);
        warteBis("Ergebnisse von Runde 1 nachübertragen", () -> ergebnisseRundeEins() == anzPaare);
    }

    private String onlineStatus() throws Exception {
        return online.turnier(turnierId).get("status").getAsString();
    }

    /** Partien der ersten Online-Runde mit Ergebnis 13:7. */
    private long ergebnisseRundeEins() throws Exception {
        List<JsonObject> runden = online.runden(turnierId);
        if (runden.isEmpty()) {
            return 0;
        }
        long anzahl = 0;
        for (JsonElement spiel : runden.getFirst().getAsJsonArray("matches")) {
            JsonObject partie = spiel.getAsJsonObject();
            if (hatWert(partie, "scoreA", 13) && hatWert(partie, "scoreB", 7)) {
                anzahl++;
            }
        }
        return anzahl;
    }

    private static boolean hatWert(JsonObject partie, String feld, int wert) {
        return partie.has(feld) && !partie.get(feld).isJsonNull() && partie.get(feld).getAsInt() == wert;
    }

    /** Negativ-Prüfung: Die Bedingung muss über die ganze Dauer gelten, nicht nur einmal. */
    private static void bleibtFuer(Duration dauer, String was, Callable<Boolean> bedingung) throws Exception {
        Instant ende = Instant.now().plus(dauer);
        while (Instant.now().isBefore(ende)) {
            assertThat(bedingung.call()).as(was).isTrue();
            Thread.sleep(500);
        }
    }
}
