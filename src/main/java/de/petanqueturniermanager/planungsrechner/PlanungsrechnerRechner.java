/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.planungsrechner;

import static com.google.common.base.Preconditions.checkArgument;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.algorithmen.common.DurchgangAufteilungRechner;

/**
 * Reine Rechenlogik für den eigenständigen Planungsrechner (unabhängig vom Turniersystem).
 * <p>
 * Zwei Richtungen: aus Turnier-Start/-Ende das Zeitlimit pro Runde bzw. Durchgang ableiten
 * ({@link #berechneZeitlimit}), oder aus einem vorgegebenen Zeitlimit den kompletten Zeitplan
 * über alle Runden inkl. Turnier-Ende ableiten ({@link #berechneZeitplan}).
 * <p>
 * Wenn wegen zu weniger Bahnen mehr Paarungen als Bahnen anstehen, wird eine Runde – analog zum
 * pro-Runde-Zeitplan-Feature (siehe {@code turniersysteme/ZEITPLAN.md}) – in mehrere Durchgänge
 * aufgeteilt; die Blockgrößen kommen aus {@link DurchgangAufteilungRechner}.
 */
public final class PlanungsrechnerRechner {

    /**
     * Obergrenze für die Zeilenzahl der {@code PTM.PLANUNG.ZEITPLAN}-Array-Formel: die Formel
     * wird einmalig über eine fest vorgegebene Zellzahl eingegeben (LibreOffice-Add-Ins kennen
     * kein dynamisches Spilling), überzählige Zeilen werden leer gelassen statt {@code #NV} zu
     * zeigen. Großzügig genug für alle realistischen Turniergrößen (z.B. 40 Runden × 5 Durchgänge).
     */
    public static final int MAX_ZEITPLAN_ZEILEN = 200;

    private PlanungsrechnerRechner() {
        // Hilfsklasse – kein Instanz-Konstruktor
    }

    /**
     * @param durchgaengeProRunde         Anzahl Durchgänge, in die jede Runde aufgeteilt wird (≥ 1)
     * @param zeitlimitProDurchgangMinuten errechnetes Zeitlimit je Durchgang in Minuten
     */
    public record ZeitlimitErgebnis(int durchgaengeProRunde, int zeitlimitProDurchgangMinuten) {
    }

    /**
     * Ein Zeitfenster (Durchgang) innerhalb einer Runde des Gesamt-Zeitplans.
     */
    public record ZeitplanEintrag(int rundeNr, int durchgangNr, LocalTime start, LocalTime ende) {
    }

    /**
     * @param eintraege   alle Durchgänge über alle Runden, chronologisch
     * @param turnierEnde Ende des letzten Durchgangs der letzten Runde
     */
    public record ZeitplanErgebnis(List<ZeitplanEintrag> eintraege, LocalTime turnierEnde) {
    }

    /**
     * Leitet aus Turnier-Start und -Ende das Zeitlimit pro Runde (bzw. pro Durchgang, wenn die
     * Bahnenzahl nicht für alle Paarungen einer Runde ausreicht) ab.
     *
     * @throws IllegalArgumentException wenn Vorbedingungen verletzt sind, insbesondere wenn die
     *         Zeit zwischen Start und Ende nicht einmal für die Pausen ausreicht
     */
    public static ZeitlimitErgebnis berechneZeitlimit(LocalTime start, LocalTime ende, int anzahlTeams,
            int anzahlBahnen, int anzahlRunden, int durchgangPauseMin, int rundenPauseMin) {
        pruefeGemeinsameParameter(anzahlTeams, anzahlBahnen, anzahlRunden, durchgangPauseMin, rundenPauseMin);
        checkArgument(start != null, "start darf nicht null sein");
        checkArgument(ende != null, "ende darf nicht null sein");
        checkArgument(ende.isAfter(start), "ende muss nach start liegen");

        int durchgaengeProRunde = durchgaengeProRunde(anzahlTeams, anzahlBahnen);
        long gesamtMinuten = Duration.between(start, ende).toMinutes();
        long pausenMinuten = (long) (anzahlRunden - 1) * rundenPauseMin
                + (long) anzahlRunden * (durchgaengeProRunde - 1) * durchgangPauseMin;
        long verfuegbareSpielzeit = gesamtMinuten - pausenMinuten;
        checkArgument(verfuegbareSpielzeit > 0,
                "Zeit zwischen Start und Ende reicht nicht einmal fuer die Pausen aus");

        long gesamtDurchgaenge = (long) anzahlRunden * durchgaengeProRunde;
        int zeitlimitProDurchgang = (int) Math.floorDiv(verfuegbareSpielzeit, gesamtDurchgaenge);
        checkArgument(zeitlimitProDurchgang > 0, "errechnetes Zeitlimit pro Durchgang ist 0");

        return new ZeitlimitErgebnis(durchgaengeProRunde, zeitlimitProDurchgang);
    }

    /**
     * Leitet aus einem vorgegebenen Zeitlimit pro Durchgang den kompletten Zeitplan über alle
     * Runden sowie das Turnier-Ende ab.
     */
    public static ZeitplanErgebnis berechneZeitplan(LocalTime start, int anzahlTeams, int anzahlBahnen,
            int anzahlRunden, int durchgangPauseMin, int rundenPauseMin, int zeitlimitProDurchgangMinuten) {
        pruefeGemeinsameParameter(anzahlTeams, anzahlBahnen, anzahlRunden, durchgangPauseMin, rundenPauseMin);
        checkArgument(start != null, "start darf nicht null sein");
        checkArgument(zeitlimitProDurchgangMinuten > 0, "zeitlimitProDurchgangMinuten muss groesser als 0 sein");

        int durchgaengeProRunde = durchgaengeProRunde(anzahlTeams, anzahlBahnen);
        List<ZeitplanEintrag> eintraege = new ArrayList<>();
        LocalTime zeiger = start;
        for (int rundeNr = 1; rundeNr <= anzahlRunden; rundeNr++) {
            for (int durchgangNr = 1; durchgangNr <= durchgaengeProRunde; durchgangNr++) {
                LocalTime durchgangStart = zeiger;
                LocalTime durchgangEnde = durchgangStart.plusMinutes(zeitlimitProDurchgangMinuten);
                eintraege.add(new ZeitplanEintrag(rundeNr, durchgangNr, durchgangStart, durchgangEnde));
                zeiger = durchgangEnde;
                if (durchgangNr < durchgaengeProRunde) {
                    zeiger = zeiger.plusMinutes(durchgangPauseMin);
                }
            }
            if (rundeNr < anzahlRunden) {
                zeiger = zeiger.plusMinutes(rundenPauseMin);
            }
        }
        return new ZeitplanErgebnis(eintraege, zeiger);
    }

    /**
     * Anzahl Durchgänge, in die eine Runde mit {@code anzahlTeams} Teams bei {@code anzahlBahnen}
     * verfügbaren Bahnen aufgeteilt wird. Öffentlich, damit {@code GlobalImpl} sie direkt für
     * {@code PTM.PLANUNG.DURCHGAENGE_PRO_RUNDE} wiederverwenden kann, ohne die Chunk-Logik zu
     * duplizieren.
     *
     * @throws IllegalArgumentException wenn {@code anzahlTeams < 2} oder {@code anzahlBahnen <= 0}
     */
    public static int durchgaengeProRunde(int anzahlTeams, int anzahlBahnen) {
        checkArgument(anzahlTeams >= 2, "anzahlTeams muss mindestens 2 sein");
        checkArgument(anzahlBahnen > 0, "anzahlBahnen muss groesser als 0 sein");
        int anzahlPaarungen = anzahlTeams / 2;
        return DurchgangAufteilungRechner.berechne(anzahlPaarungen, anzahlBahnen).size();
    }

    private static void pruefeGemeinsameParameter(int anzahlTeams, int anzahlBahnen, int anzahlRunden,
            int durchgangPauseMin, int rundenPauseMin) {
        checkArgument(anzahlTeams >= 2, "anzahlTeams muss mindestens 2 sein");
        checkArgument(anzahlBahnen > 0, "anzahlBahnen muss groesser als 0 sein");
        checkArgument(anzahlRunden > 0, "anzahlRunden muss groesser als 0 sein");
        checkArgument(durchgangPauseMin >= 0, "durchgangPauseMin darf nicht negativ sein");
        checkArgument(rundenPauseMin >= 0, "rundenPauseMin darf nicht negativ sein");
    }
}
