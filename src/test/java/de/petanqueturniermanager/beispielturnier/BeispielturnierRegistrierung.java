/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.beispielturnier;

import java.util.List;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJTurnierTestDaten;
import de.petanqueturniermanager.ko.KoTurnierTestDaten;
import de.petanqueturniermanager.liga.spielplan.LigaTurnierTestDaten;
import de.petanqueturniermanager.maastrichter.MaastrichterTurnierTestDaten;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SupermeleeTurnierTestDaten;

/**
 * Zentrale Registrierung aller vollständigen Beispielturnier-Generatoren.
 * <p>
 * Jede Klasse, deren Dateiname auf {@code TurnierTestDaten.java} endet, muss hier einen
 * Eintrag besitzen. Der {@link BeispielturnierVollstaendigkeitTest} prüft dies automatisch:
 * Kommt eine neue {@code *TurnierTestDaten}-Klasse hinzu, ohne dass ein Eintrag ergänzt wird,
 * schlägt der Test fehl.
 * <p>
 * Jeder Eintrag definiert:
 * <ul>
 *   <li>eine {@link Generator}-Fabrik, die das Turnier auf einem {@link WorkingSpreadsheet} generiert</li>
 *   <li>die erwarteten Metadaten-Schlüssel der erzeugten Sheets (locale-unabhängig)</li>
 *   <li>den Dateinamen der zugehörigen Quellklasse für den Vollständigkeitstest</li>
 * </ul>
 */
public final class BeispielturnierRegistrierung {

    private BeispielturnierRegistrierung() {
    }

    /** Fabrik-Interface: erstellt und generiert ein Beispielturnier. */
    @FunctionalInterface
    public interface Generator {
        void generiere(WorkingSpreadsheet workingSpreadsheet) throws GenerateException;
    }

    /**
     * Beschreibt ein registriertes Beispielturnier.
     *
     * @param bezeichnung        Anzeigename (wird als Testname verwendet)
     * @param generator          Fabrik-Funktion, die das Turnier generiert
     * @param erwarteteSchluessel Metadaten-Schlüssel aller erwarteten Sheets
     * @param quelldateiName     Dateiname der Quellklasse (z.B. {@code SchweizerTurnierTestDaten.java})
     */
    public record Eintrag(
            String bezeichnung,
            Generator generator,
            List<String> erwarteteSchluessel,
            String quelldateiName) {

        @Override
        public String toString() {
            return bezeichnung;
        }
    }

    /**
     * Liefert alle registrierten Beispielturnier-Einträge.
     * <p>
     * Diese Liste ist die Quelle der Wahrheit für {@link BeispielturnierUITest}
     * und {@link BeispielturnierVollstaendigkeitTest}.
     */
    public static List<Eintrag> alleEintraege() {
        return List.of(

                new Eintrag(
                        "Schweizer System (16 Teams, 3 Runden)",
                        ws -> new SchweizerTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE,
                                SheetMetadataHelper.schluesselSchweizerSpielrunde(1),
                                SheetMetadataHelper.schluesselSchweizerSpielrunde(2),
                                SheetMetadataHelper.schluesselSchweizerSpielrunde(3),
                                SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE),
                        "SchweizerTurnierTestDaten.java"),

                new Eintrag(
                        "Maastrichter System (12 Teams, 3 Vorrunden)",
                        ws -> new MaastrichterTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE,
                                SheetMetadataHelper.schluesselMaastrichterVorrunde(1),
                                SheetMetadataHelper.schluesselMaastrichterVorrunde(2),
                                SheetMetadataHelper.schluesselMaastrichterVorrunde(3),
                                // Vorrunden-Rangliste nutzt PREFIX als Schlüssel
                                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
                                // A-Finale (alle 12 Teams in einer Gruppe)
                                SheetMetadataHelper.schluesselMaastrichterFinalrunde("A")),
                        "MaastrichterTurnierTestDaten.java"),

                new Eintrag(
                        "K.-O.-System (8 Teams)",
                        ws -> new KoTurnierTestDaten(ws, 8).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE,
                                // Einzelner Turnierbaum ohne Gruppenindex
                                SheetMetadataHelper.schluesselKoTurnierbaum("")),
                        "KoTurnierTestDaten.java"),

                new Eintrag(
                        "Jeder gegen Jeden (10 Teams)",
                        ws -> new JGJTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_JGJ_MELDELISTE,
                                SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN,
                                SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE),
                        "JGJTurnierTestDaten.java"),

                new Eintrag(
                        "Supermelee (5 Spieltage)",
                        ws -> new SupermeleeTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE,
                                SheetMetadataHelper.schluesselSpieltagRangliste(1),
                                SheetMetadataHelper.schluesselSpieltagRangliste(5)),
                        "SupermeleeTurnierTestDaten.java"),

                new Eintrag(
                        "Liga (6 Teams, Spielplan + Rangliste)",
                        ws -> new LigaTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE,
                                SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN,
                                SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE),
                        "LigaTurnierTestDaten.java")
        );
    }
}
