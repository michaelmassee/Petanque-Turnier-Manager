/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.beispielturnier;

import java.util.List;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJTurnierTestDaten;
import de.petanqueturniermanager.ko.KoCadrageTurnierTestDaten;
import de.petanqueturniermanager.ko.Ko16TeamsTurnierTestDaten;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetTestDaten;
import de.petanqueturniermanager.ko.KoTurnierTestDaten;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetTestDaten;
import de.petanqueturniermanager.liga.spielplan.LigaMitFreispielTurnierTestDaten;
import de.petanqueturniermanager.liga.spielplan.LigaTurnierTestDaten;
import de.petanqueturniermanager.maastrichter.Maastrichter35TeamsTurnierTestDaten;
import de.petanqueturniermanager.maastrichter.Maastrichter57TeamsTurnierTestDaten;
import de.petanqueturniermanager.maastrichter.MaastrichterTurnierTestDaten;
import de.petanqueturniermanager.poule.Poule37TeamsTurnierTestDaten;
import de.petanqueturniermanager.poule.PouleTurnierTestDaten;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.spielrunde.Schweizer19TeamsTurnierTestDaten;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;
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
                        ws -> new LigaTurnierTestDaten(ws).erzeugeBeispielturnier(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE,
                                SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN,
                                SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE),
                        "LigaTurnierTestDaten.java"),

                new Eintrag(
                        "Liga – Spielplan mit Freispiel",
                        ws -> new LigaMitFreispielTurnierTestDaten(ws).erzeugeBeispielturnier(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE,
                                SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN,
                                SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE),
                        "LigaMitFreispielTurnierTestDaten.java"),

                new Eintrag(
                        "Liga – nur Meldeliste",
                        ws -> new LigaMeldeListeSheetTestDaten(ws, true).generate(),
                        List.of(SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE),
                        "LigaMeldeListeSheetTestDaten.java"),

                new Eintrag(
                        "Schweizer System – nur Meldeliste (16 Teams)",
                        ws -> new SchweizerMeldeListeSheetTestDaten(ws).doRun(),
                        List.of(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE),
                        "SchweizerMeldeListeSheetTestDaten.java"),

                new Eintrag(
                        "Schweizer System – Turnier (19 Teams, Freilos, Teamname, Bahn zufällig)",
                        ws -> new Schweizer19TeamsTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE,
                                SheetMetadataHelper.schluesselSchweizerSpielrunde(1),
                                SheetMetadataHelper.schluesselSchweizerSpielrunde(3),
                                SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE),
                        "Schweizer19TeamsTurnierTestDaten.java"),

                new Eintrag(
                        "K.-O. – nur Meldeliste (8 Teams)",
                        ws -> new KoMeldeListeSheetTestDaten(ws, 8).erstelleMeldelisteWithTestdaten(),
                        List.of(SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE),
                        "KoMeldeListeSheetTestDaten.java"),

                new Eintrag(
                        "K.-O. – 16 Teams (2 Gruppen)",
                        ws -> new Ko16TeamsTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE,
                                SheetMetadataHelper.schluesselKoTurnierbaum("A"),
                                SheetMetadataHelper.schluesselKoTurnierbaum("B")),
                        "Ko16TeamsTurnierTestDaten.java"),

                new Eintrag(
                        "K.-O. – 10 Teams (Cadrage)",
                        ws -> new KoCadrageTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE,
                                SheetMetadataHelper.schluesselKoTurnierbaum("")),
                        "KoCadrageTurnierTestDaten.java"),

                new Eintrag(
                        "Maastrichter – Turnier (57 Teams, 4 Vorrunden + 4 Finalgruppen)",
                        ws -> new Maastrichter57TeamsTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE,
                                SheetMetadataHelper.schluesselMaastrichterVorrunde(1),
                                SheetMetadataHelper.schluesselMaastrichterVorrunde(4),
                                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
                                SheetMetadataHelper.schluesselMaastrichterFinalrunde("A"),
                                SheetMetadataHelper.schluesselMaastrichterFinalrunde("D")),
                        "Maastrichter57TeamsTurnierTestDaten.java"),

                new Eintrag(
                        "Maastrichter – Turnier (35 Teams, 3 Vorrunden + 2 Finalgruppen)",
                        ws -> new Maastrichter35TeamsTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELDELISTE,
                                SheetMetadataHelper.schluesselMaastrichterVorrunde(1),
                                SheetMetadataHelper.schluesselMaastrichterVorrunde(3),
                                SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
                                SheetMetadataHelper.schluesselMaastrichterFinalrunde("A"),
                                SheetMetadataHelper.schluesselMaastrichterFinalrunde("B")),
                        "Maastrichter35TeamsTurnierTestDaten.java"),

                new Eintrag(
                        "Supermêlée – nur Meldeliste (100 Meldungen)",
                        ws -> new MeldeListeSheet_TestDaten(ws).generate(),
                        List.of(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE),
                        "MeldeListeSheet_TestDaten.java"),

                new Eintrag(
                        "Supermêlée – 1 Spieltag mit 4 Spielrunden",
                        ws -> new SpielrundeSheet_TestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE,
                                SheetMetadataHelper.schluesselSpieltagRangliste(1)),
                        "SpielrundeSheet_TestDaten.java"),

                new Eintrag(
                        "Poule A/B \u2013 nur Meldeliste (16 Teams)",
                        ws -> new PouleMeldeListeSheetTestDaten(ws).doRun(),
                        List.of(SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE),
                        "PouleMeldeListeSheetTestDaten.java"),

                new Eintrag(
                        "Poule A/B \u2013 Meldeliste + Vorrunde (16 Teams)",
                        ws -> new PouleTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE,
                                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE),
                        "PouleTurnierTestDaten.java"),

                new Eintrag(
                        "Poule A/B \u2013 Komplettes Turnier (37 Teams, Rangliste + KO)",
                        ws -> new Poule37TeamsTurnierTestDaten(ws).generate(),
                        List.of(
                                SheetMetadataHelper.SCHLUESSEL_POULE_MELDELISTE,
                                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE,
                                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE,
                                SheetMetadataHelper.schluesselPouleKo("A"),
                                SheetMetadataHelper.schluesselPouleKo("B")),
                        "Poule37TeamsTurnierTestDaten.java")
        );
    }
}
