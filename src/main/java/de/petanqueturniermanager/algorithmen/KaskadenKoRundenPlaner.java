/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Erzeugt den vollständigen Rundenplan für das Kaskaden-KO-System.<br>
 * <br>
 * Aufbauend auf {@link KaskadenKoFeldRechner} (Feldgrößen) berechnet dieser
 * Planer die konkreten Spielpaarungen für jede Kaskadenrunde.<br>
 * <br>
 * <b>Freilos-Konvention:</b> Bei ungerader Teamanzahl in einer Gruppe erhält
 * das Team an letzter Position (Position {@code m}) ein Freilos. Dieses Team
 * erscheint <em>nicht</em> in den Spielpaaren und wird durch die
 * Gruppengrößenberechnung automatisch der Verlierer-Gruppe zugeteilt:
 * <pre>
 *   Sieger-Gruppe   = floor(m / 2)   Positionen
 *   Verlierer-Gruppe = ceil(m / 2)   Positionen (inkl. Freilos)
 * </pre>
 * Diese Konvention ist identisch mit {@link KaskadenKoFeldRechner}.<br>
 * <br>
 * <b>Paarungsstrategie:</b> Standardmäßig wird sequenziell gepaart:
 * {@code (1,2), (3,4), …}. Über {@link #berechne(int, int, KaskadenKoPaarungsStrategie)}
 * kann eine eigene Strategie (z. B. Setzposition-basiert) übergeben werden.<br>
 * <br>
 * <b>Kaskadenrunden-Limit:</b> Dieser Planer hat kein hartes Limit für
 * {@code kaskadenStufen}. Der nachgelagerte Aufruf von {@link KaskadenKoFeldRechner}
 * begrenzt jedoch auf 1–3 (wegen der {@code FELDBEZEICHNER}-Liste A–H).
 *
 * @author Michael Massee
 * @see KaskadenKoFeldRechner
 * @see KaskadenKoPaarungsStrategie
 * @see KaskadenKoRundenPlan
 */
public class KaskadenKoRundenPlaner {

    /**
     * Sequenzielle Paarungsstrategie: Pos.&nbsp;1 vs 2, 3 vs 4, …<br>
     * Pfad und Rundennummer werden ignoriert.
     */
    public static final KaskadenKoPaarungsStrategie SEQUENZIELL = (pfad, rundenNr, anzTeams) -> {
        int anzPaare = anzTeams / 2;
        var paare = new ArrayList<KaskadenKoSpielPaar>(anzPaare);
        for (int i = 0; i < anzPaare; i++) {
            paare.add(new KaskadenKoSpielPaar(i + 1, 2 * i + 1, 2 * i + 2, null, null));
        }
        return Collections.unmodifiableList(paare);
    };

    private KaskadenKoRundenPlaner() {
        // Hilfsklasse – kein Instanz-Konstruktor
    }

    /**
     * Berechnet den Rundenplan mit sequenzieller Paarungsstrategie.
     *
     * @param gesamtTeams    Gesamtanzahl Teams (muss &ge; 2 sein)
     * @param kaskadenStufen Anzahl Kaskadenrunden (muss &ge; 1 sein)
     * @return vollständiger Rundenplan
     * @throws IllegalArgumentException wenn {@code gesamtTeams < 2} oder
     *                                  {@code kaskadenStufen < 1}
     */
    public static KaskadenKoRundenPlan berechne(int gesamtTeams, int kaskadenStufen) {
        return berechne(gesamtTeams, kaskadenStufen, SEQUENZIELL);
    }

    /**
     * Berechnet den Rundenplan mit der angegebenen Paarungsstrategie.
     *
     * @param gesamtTeams    Gesamtanzahl Teams (muss &ge; 2 sein)
     * @param kaskadenStufen Anzahl Kaskadenrunden (muss &ge; 1 sein)
     * @param strategie      Paarungsstrategie; darf nicht {@code null} sein
     * @return vollständiger Rundenplan
     * @throws IllegalArgumentException wenn {@code gesamtTeams < 2} oder
     *                                  {@code kaskadenStufen < 1}
     * @throws IllegalStateException    wenn die Strategie eine falsche Paaranzahl liefert
     */
    public static KaskadenKoRundenPlan berechne(
            int gesamtTeams, int kaskadenStufen, KaskadenKoPaarungsStrategie strategie) {

        checkArgument(gesamtTeams >= 2, "gesamtTeams muss mindestens 2 sein, war: %s", gesamtTeams);
        checkArgument(kaskadenStufen >= 1, "kaskadenStufen muss mindestens 1 sein, war: %s", kaskadenStufen);

        List<GruppeKnoten> aktuelleGruppen = new ArrayList<>();
        aktuelleGruppen.add(new GruppeKnoten("", gesamtTeams));

        var runden = new ArrayList<KaskadenKoRunde>(kaskadenStufen);

        for (int rundenNr = 1; rundenNr <= kaskadenStufen; rundenNr++) {
            var gruppenRunden = new ArrayList<KaskadenKoGruppenRunde>(aktuelleGruppen.size());
            var naechsteGruppen = new ArrayList<GruppeKnoten>(aktuelleGruppen.size() * 2);

            for (var gruppe : aktuelleGruppen) {
                var gruppenRunde = erstelleGruppenRunde(gruppe, rundenNr, strategie);
                gruppenRunden.add(gruppenRunde);

                int m = gruppe.anzTeams();
                naechsteGruppen.add(new GruppeKnoten(gruppe.pfad() + "S", m / 2));
                naechsteGruppen.add(new GruppeKnoten(gruppe.pfad() + "V", m - m / 2));
            }

            runden.add(new KaskadenKoRunde(rundenNr, Collections.unmodifiableList(gruppenRunden)));
            aktuelleGruppen = naechsteGruppen;
        }

        var felder = KaskadenKoFeldRechner.berechne(gesamtTeams, kaskadenStufen);
        return new KaskadenKoRundenPlan(
                gesamtTeams,
                kaskadenStufen,
                Collections.unmodifiableList(runden),
                felder);
    }

    private static KaskadenKoGruppenRunde erstelleGruppenRunde(
            GruppeKnoten gruppe, int rundenNr, KaskadenKoPaarungsStrategie strategie) {

        int m = gruppe.anzTeams();
        int erwarteteAnzPaare = m / 2;
        int freilose = m % 2;

        var spielPaare = strategie.generiere(gruppe.pfad(), rundenNr, m);

        checkState(spielPaare.size() == erwarteteAnzPaare,
                "Strategie liefert falsche Paaranzahl: erwartet %s, erhalten %s (Gruppe pfad='%s', anzTeams=%s)",
                erwarteteAnzPaare, spielPaare.size(), gruppe.pfad(), m);

        return new KaskadenKoGruppenRunde(
                gruppe.pfad(),
                m,
                spielPaare,
                freilose);
    }

    /** Interner Zwischenknoten: Pfad + Teamanzahl. */
    private record GruppeKnoten(String pfad, int anzTeams) {}
}
