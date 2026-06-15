/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.forme.export.KoExportInVerzeichnis;
import de.petanqueturniermanager.formulex.export.FormuleXExportInVerzeichnis;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXTurnierTestDaten;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.jedergegenjeden.export.JGJExportInVerzeichnis;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJTurnierTestDaten;
import de.petanqueturniermanager.kaskade.KaskadeTurnierTestDaten;
import de.petanqueturniermanager.kaskade.export.KaskadeExportInVerzeichnis;
import de.petanqueturniermanager.ko.KoTurnierTestDaten;
import de.petanqueturniermanager.liga.meldeliste.LigaExportInVerzeichnis;
import de.petanqueturniermanager.liga.spielplan.LigaTurnierTestDaten;
import de.petanqueturniermanager.maastrichter.MaastrichterTurnierTestDaten;
import de.petanqueturniermanager.maastrichter.export.MaastrichterExportInVerzeichnis;
import de.petanqueturniermanager.poule.Poule37TeamsTurnierTestDaten;
import de.petanqueturniermanager.poule.export.PouleExportInVerzeichnis;
import de.petanqueturniermanager.schweizer.export.SchweizerExportInVerzeichnis;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;
import de.petanqueturniermanager.supermelee.export.SupermeleeExportInVerzeichnis;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SupermeleeTurnierTestDaten;
import de.petanqueturniermanager.triptete.export.TripTeteExportInVerzeichnis;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheetTestDaten;

class ExportInVerzeichnisSmokeUITest extends BaseCalcUITest {

    private static final long SEED_FUER_TESTS = 42L;

    @AfterEach
    void resetRandom() {
        RandomSource.reset();
    }

    static Stream<ExportFall> exportFaelle() {
        return Stream.of(
                fall(TurnierSystem.SUPERMELEE,
                        ws -> {
                            new SupermeleeTurnierTestDaten(ws).generate();
                            new EndranglisteSheet(ws).run();
                        },
                        (test, ziel) -> new SupermeleeExportInVerzeichnis(test.wkingSpreadsheet, ziel)),
                fall(TurnierSystem.LIGA,
                        ws -> new LigaTurnierTestDaten(ws).erzeugeBeispielturnier(),
                        (test, ziel) -> new LigaExportInVerzeichnis(test.wkingSpreadsheet, ziel)),
                fall(TurnierSystem.MAASTRICHTER,
                        ws -> new MaastrichterTurnierTestDaten(ws).generate(),
                        (test, ziel) -> new MaastrichterExportInVerzeichnis(test.wkingSpreadsheet, ziel)),
                fall(TurnierSystem.SCHWEIZER,
                        ws -> new SchweizerTurnierTestDaten(ws).generate(),
                        (test, ziel) -> new SchweizerExportInVerzeichnis(test.wkingSpreadsheet, ziel)),
                fall(TurnierSystem.JGJ,
                        ws -> new JGJTurnierTestDaten(ws).generate(),
                        (test, ziel) -> new JGJExportInVerzeichnis(test.wkingSpreadsheet, ziel)),
                fall(TurnierSystem.KO,
                        ws -> new KoTurnierTestDaten(ws, 8).generate(),
                        (test, ziel) -> new KoExportInVerzeichnis(test.wkingSpreadsheet, ziel)),
                fall(TurnierSystem.POULE,
                        ws -> new Poule37TeamsTurnierTestDaten(ws).generate(),
                        (test, ziel) -> new PouleExportInVerzeichnis(test.wkingSpreadsheet, ziel)),
                fall(TurnierSystem.KASKADE,
                        ws -> new KaskadeTurnierTestDaten(ws).generate(),
                        (test, ziel) -> new KaskadeExportInVerzeichnis(test.wkingSpreadsheet, ziel)),
                fall(TurnierSystem.FORMULEX,
                        ws -> new FormuleXTurnierTestDaten(ws).generate(),
                        (test, ziel) -> new FormuleXExportInVerzeichnis(test.wkingSpreadsheet, ziel)),
                fall(TurnierSystem.TRIPTETE,
                        ws -> new TripTeteSpielPlanSheetTestDaten(ws).generate(),
                        (test, ziel) -> new TripTeteExportInVerzeichnis(test.wkingSpreadsheet, ziel)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exportFaelle")
    void exportInVerzeichnis_erstelltHtmlUndPdf(ExportFall fall, @TempDir Path zielVerzeichnis)
            throws Exception {
        RandomSource.setSeed(SEED_FUER_TESTS);

        fall.generator().generiere(wkingSpreadsheet);
        new DocumentPropertiesHelper(wkingSpreadsheet)
                .setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, fall.system().getId());
        assertThat(new DocumentPropertiesHelper(wkingSpreadsheet).getTurnierSystemAusDocument())
                .as("%s muss als aktives Turniersystem im Dokument stehen", fall.system())
                .isEqualTo(fall.system());

        fall.exporter().erzeuge(this, zielVerzeichnis)
                .testTurnierSystem(fall.system())
                .run();

        var ergebnis = ExportErgebnis.laden(wkingSpreadsheet);
        assertThat(ergebnis)
                .as("%s ExportErgebnis muss gespeichert werden", fall.system())
                .isPresent();

        List<Path> dateien = ergebnis.orElseThrow().exportierteDateien();
        assertThat(dateien)
                .as("%s muss Exportdateien melden", fall.system())
                .isNotEmpty()
                .allSatisfy(datei -> assertThat(datei)
                        .as("%s liegt im Exportverzeichnis", datei)
                        .startsWith(zielVerzeichnis));

        assertThat(dateien)
                .as("%s alle gemeldeten Dateien muessen existieren", fall.system())
                .allSatisfy(datei -> assertThat(datei).exists().isRegularFile());

        List<Path> htmlDateien = dateien.stream()
                .filter(datei -> datei.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".html"))
                .toList();
        List<Path> pdfDateien = dateien.stream()
                .filter(datei -> datei.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                .toList();

        assertThat(htmlDateien)
                .as("%s muss genau eine HTML-Datei erzeugen", fall.system())
                .hasSize(1);
        assertThat(pdfDateien)
                .as("%s muss mindestens eine PDF-Datei erzeugen", fall.system())
                .isNotEmpty();

        pruefeHtml(fall.system(), htmlDateien.getFirst());
        for (Path pdf : pdfDateien) {
            pruefePdf(fall.system(), pdf);
        }
    }

    @Test
    void supermeleeExport_endranglisteEnthaeltStreichspieltagFarbe(@TempDir Path zielVerzeichnis)
            throws Exception {
        RandomSource.setSeed(SEED_FUER_TESTS);

        new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
        new EndranglisteSheet(wkingSpreadsheet).run();
        new DocumentPropertiesHelper(wkingSpreadsheet)
                .setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SUPERMELEE.getId());

        new SupermeleeExportInVerzeichnis(wkingSpreadsheet, zielVerzeichnis)
                .testTurnierSystem(TurnierSystem.SUPERMELEE)
                .run();

        var htmlDatei = ExportErgebnis.laden(wkingSpreadsheet)
                .orElseThrow()
                .exportierteDateien()
                .stream()
                .filter(datei -> datei.getFileName().toString().equals("SuperMelee.html"))
                .findFirst()
                .orElseThrow();

        pruefeHtml(TurnierSystem.SUPERMELEE, htmlDatei);
    }

    private void pruefeHtml(TurnierSystem system, Path htmlDatei) throws IOException {
        String html = Files.readString(htmlDatei, StandardCharsets.UTF_8);
        assertThat(html)
                .as("%s HTML darf nicht leer sein", system)
                .isNotBlank()
                .contains("<html")
                .contains("export-section")
                .contains(".pdf");
        if (system == TurnierSystem.SUPERMELEE) {
            assertThat(html)
                    .as("Supermelee HTML muss die direkt gesetzte Streich-Spieltag-Farbe exportieren")
                    .contains("background-color:#DDDDDD");
        }
    }

    private void pruefePdf(TurnierSystem system, Path pdfDatei) throws IOException {
        byte[] bytes = Files.readAllBytes(pdfDatei);
        assertThat(bytes)
                .as("%s PDF %s darf nicht leer sein", system, pdfDatei.getFileName())
                .hasSizeGreaterThan(5);
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII))
                .as("%s PDF %s muss mit PDF-Signatur beginnen", system, pdfDatei.getFileName())
                .isEqualTo("%PDF-");
    }

    private static ExportFall fall(TurnierSystem system, ExportGenerator generator, ExporterFactory exporter) {
        return new ExportFall(system, generator, exporter);
    }

    @FunctionalInterface
    private interface ExportGenerator {
        void generiere(de.petanqueturniermanager.comp.WorkingSpreadsheet ws) throws GenerateException;
    }

    @FunctionalInterface
    private interface ExporterFactory {
        SheetRunner erzeuge(ExportInVerzeichnisSmokeUITest test, Path zielVerzeichnis);
    }

    private record ExportFall(
            TurnierSystem system,
            ExportGenerator generator,
            ExporterFactory exporter) {

        @Override
        public String toString() {
            return system.name();
        }
    }
}
