/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Berechnet die Feldaufteilung für das Kaskaden-KO-System.<br>
 * <br>
 * Das System unterteilt das Teilnehmerfeld schrittweise durch Kaskaden-Runden.
 * Nach {@code k} Kaskaden entstehen 2^k Felder, die anschließend jeweils als
 * eigenständige KO-Turniere ausgetragen werden (ggf. mit Cadrage-Vorrunde).<br>
 * <br>
 * <b>Splitregel:</b> Bei {@code n} Teams in einem Feld gilt:<br>
 * <ul>
 *   <li>Sieger-Gruppe (besseres Feld) = {@code floor(n / 2)}</li>
 *   <li>Verlierer-Gruppe (schlechteres Feld) = {@code n - floor(n / 2)} = {@code ceil(n / 2)}</li>
 * </ul>
 * <br>
 * <b>Pfad-basierte Feldbenennung:</b> Jedes Feld erhält einen Pfad aus
 * {@code 'S'} (Sieger) und {@code 'V'} (Verlierer), der den Entstehungsweg
 * im Kaskadenbaum beschreibt. Der alphabetische Buchstabe (A, B, C, …) ergibt
 * sich aus der lexikografischen Position des Pfades (S &lt; V):
 * <pre>
 *   k=1: S→A,  V→B
 *   k=2: SS→A, SV→B, VS→C, VV→D
 *   k=3: SSS→A, SSV→B, SVS→C, SVV→D, VSS→E, VSV→F, VVS→G, VVV→H
 * </pre>
 * <b>Hinweis zur Terminologie:</b> Die Spezifikation bezeichnet die
 * Zwischenstände nach Runde 1 als „A und C" (traditionelles ABCD-System).
 * Dieser Rechner liefert bei {@code kaskadenStufen=1} die <em>Endfelder</em>
 * A (Pfad "S") und B (Pfad "V") – das entspricht dem Ergebnis nach abgeschlossener
 * Kaskadierung, nicht einem Zwischenzustand.<br>
 * <br>
 * Anwendungsbeispiele (2 Kaskaden):
 * <pre>
 *   30 Teams: A=7, B=8, C=7, D=8
 *   34 Teams: A=8, B=9, C=8, D=9
 *   50 Teams: A=12, B=13, C=12, D=13
 * </pre>
 *
 * @author Michael Massee
 * @see KaskadenKoFeldInfo
 * @see CadrageRechner
 */
public class KaskadenKoFeldRechner {

    /**
     * Alphabetische Feldbezeichner in Leistungsreihenfolge.<br>
     * Position im Array entspricht der lexikografischen Position des S/V-Pfades.
     * Erweiterung auf k&gt;3: diese Liste verlängern und die Validierung anpassen.
     */
    static final List<String> FELDBEZEICHNER = List.of("A", "B", "C", "D", "E", "F", "G", "H");

    private KaskadenKoFeldRechner() {
        // Hilfsklasse – kein Instanz-Konstruktor
    }

    /**
     * Berechnet die Feldaufteilung für das Kaskaden-KO-System.
     * Freilos-Teams gehen in die Verlierer-Gruppe (klassisches Verhalten).
     *
     * @param gesamtTeams    Gesamtanzahl Teams (muss &gt;= 2 sein)
     * @param kaskadenStufen Anzahl Kaskaden-Runden (1 bis 3)
     * @return unveränderliche Liste der Felder in alphabetischer Reihenfolge
     *         (Index 0 = bestes Feld A, letzter Index = schwächstes Feld)
     * @throws IllegalArgumentException wenn {@code gesamtTeams < 2} oder
     *                                  {@code kaskadenStufen} außerhalb von 1..3
     */
    public static List<KaskadenKoFeldInfo> berechne(int gesamtTeams, int kaskadenStufen) {
        return berechne(gesamtTeams, kaskadenStufen, false);
    }

    /**
     * Berechnet die Feldaufteilung für das Kaskaden-KO-System.
     *
     * @param gesamtTeams      Gesamtanzahl Teams (muss &gt;= 2 sein)
     * @param kaskadenStufen   Anzahl Kaskaden-Runden (1 bis 3)
     * @param freispielGewonnen {@code true} wenn das Freilos als Sieg gewertet wird
     *                         (Freilos-Team → Sieger-Gruppe); {@code false} für Verlierer-Gruppe
     * @return unveränderliche Liste der Felder in alphabetischer Reihenfolge
     *         (Index 0 = bestes Feld A, letzter Index = schwächstes Feld)
     * @throws IllegalArgumentException wenn {@code gesamtTeams < 2} oder
     *                                  {@code kaskadenStufen} außerhalb von 1..3
     */
    public static List<KaskadenKoFeldInfo> berechne(int gesamtTeams, int kaskadenStufen, boolean freispielGewonnen) {
        checkArgument(gesamtTeams >= 2, "gesamtTeams muss mindestens 2 sein, war: %s", gesamtTeams);
        checkArgument(kaskadenStufen >= 1 && kaskadenStufen <= 3,
                "kaskadenStufen muss zwischen 1 und 3 liegen, war: %s", kaskadenStufen);

        List<FeldKnoten> feldListe = new ArrayList<>();
        feldListe.add(new FeldKnoten("", gesamtTeams));

        for (int stufe = 0; stufe < kaskadenStufen; stufe++) {
            feldListe = spalteFelderAuf(feldListe, freispielGewonnen);
        }

        return erstelleFeldInfos(feldListe);
    }

    /**
     * Spaltet alle aktuellen Felder auf: jedes Feld erzeugt einen Sieger- und
     * einen Verlierer-Knoten direkt hintereinander (Interleaved-Reihenfolge).<br>
     * Da die Pfade lexikografisch aufgebaut werden (S &lt; V), ist das Ergebnis
     * automatisch in der gewünschten Leistungsreihenfolge sortiert.
     * <p>
     * Bei ungerader Teamanzahl gilt:<br>
     * – {@code freispielGewonnen=true}: Sieger bekommt {@code ceil(n/2)}, Verlierer {@code floor(n/2)}<br>
     * – {@code freispielGewonnen=false}: Sieger bekommt {@code floor(n/2)}, Verlierer {@code ceil(n/2)}
     */
    private static List<FeldKnoten> spalteFelderAuf(List<FeldKnoten> felder, boolean freispielGewonnen) {
        var neueFelder = new ArrayList<FeldKnoten>(felder.size() * 2);
        for (var feld : felder) {
            int n         = feld.anzTeams();
            int sieger    = freispielGewonnen ? (n + 1) / 2 : n / 2;
            int verlierer = n - sieger;
            neueFelder.add(new FeldKnoten(feld.pfad() + "S", sieger));
            neueFelder.add(new FeldKnoten(feld.pfad() + "V", verlierer));
        }
        return neueFelder;
    }

    /**
     * Wandelt die berechneten Feldknoten in {@link KaskadenKoFeldInfo}-Objekte um
     * und weist die Buchstaben-Bezeichner sequenziell zu.
     */
    private static List<KaskadenKoFeldInfo> erstelleFeldInfos(List<FeldKnoten> feldListe) {
        var ergebnis = new ArrayList<KaskadenKoFeldInfo>(feldListe.size());
        for (int i = 0; i < feldListe.size(); i++) {
            var knoten = feldListe.get(i);
            ergebnis.add(new KaskadenKoFeldInfo(
                    FELDBEZEICHNER.get(i),
                    knoten.pfad(),
                    knoten.anzTeams(),
                    KaskadenKoFeldInfo.cadrageRechnerFuer(knoten.anzTeams())));
        }
        return Collections.unmodifiableList(ergebnis);
    }

    /** Interner Zwischenknoten im Kaskadenbaum: Pfad + Teamanzahl. */
    private record FeldKnoten(String pfad, int anzTeams) {}
}
