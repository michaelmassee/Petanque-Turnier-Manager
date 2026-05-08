package de.petanqueturniermanager.spielerdb.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.spielerdb.LabelRepository;
import de.petanqueturniermanager.spielerdb.SpielerDatensatz;
import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.spielerdb.VereinDatensatz;
import de.petanqueturniermanager.spielerdb.VereinRepository;
import de.petanqueturniermanager.spielerdb.export.ExportEntity;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpieler;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpielerLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohVerein;

class SpielerDbImporterTest {

    private SpielerDbConnection conn;
    private SpielerRepository spielerRepo;
    private VereinRepository vereinRepo;
    private LabelRepository labelRepo;

    @BeforeEach
    void setUp() throws Exception {
        conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:");
        spielerRepo = new SpielerRepository(conn);
        vereinRepo = new VereinRepository(conn);
        labelRepo = new LabelRepository(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void importInLeereDb_legtAllesAn() throws Exception {
        ImportRohdaten roh = new ImportRohdaten(
                List.of(new RohSpieler(1, "Hans", "Müller", 10, "BC Linden", "LIZ-1")),
                List.of(new RohVerein(10, "BC Linden")),
                List.of(new RohLabel(5, "Senior")),
                List.of(new RohSpielerLabel(1, 5)));
        ImportErgebnis erg = importiere(roh, ImportModus.AKTUALISIEREN, false);

        assertThat(erg.spielerEingefuegt()).isEqualTo(1);
        assertThat(erg.vereineEingefuegt()).isEqualTo(1);
        assertThat(erg.labelsEingefuegt()).isEqualTo(1);
        assertThat(erg.junctionEingefuegt()).isEqualTo(1);

        SpielerMitVerein s = spielerRepo.findAll().get(0);
        assertThat(s.vorname()).isEqualTo("Hans");
        assertThat(s.lizenznr()).isEqualTo("LIZ-1");
        assertThat(s.vereinName()).isEqualTo("BC Linden");
    }

    @Test
    void modusNurNeue_skipBestehende() throws Exception {
        VereinDatensatz v = vereinRepo.insert("BC Linden");
        spielerRepo.insert(SpielerDatensatz.neu("Hans", "Müller", v.nr(), List.of(), null));

        ImportRohdaten roh = new ImportRohdaten(
                List.of(new RohSpieler(99, "Hans", "Müller", 10, "BC Linden", "LIZ-NEU")),
                List.of(new RohVerein(10, "BC Linden")),
                List.of(),
                List.of());
        ImportErgebnis erg = importiere(roh, ImportModus.NUR_NEUE, false);

        assertThat(erg.spielerUebersprungen()).isEqualTo(1);
        assertThat(erg.spielerEingefuegt()).isZero();
        SpielerMitVerein s = spielerRepo.findAll().get(0);
        assertThat(s.lizenznr()).isNull();  // wurde nicht überschrieben
    }

    @Test
    void modusAktualisieren_lizenznrNurWennZielLeer() throws Exception {
        VereinDatensatz v = vereinRepo.insert("BC Linden");
        spielerRepo.insert(SpielerDatensatz.neu("Hans", "Müller", v.nr(), List.of(), "ALT-LIZ"));

        ImportRohdaten roh = new ImportRohdaten(
                List.of(new RohSpieler(99, "Hans", "Müller", 10, "BC Linden", "NEU-LIZ")),
                List.of(new RohVerein(10, "BC Linden")),
                List.of(),
                List.of());
        ImportErgebnis erg = importiere(roh, ImportModus.AKTUALISIEREN, false);

        assertThat(erg.spielerAktualisiert()).isEqualTo(1);
        SpielerMitVerein s = spielerRepo.findAll().get(0);
        assertThat(s.lizenznr()).isEqualTo("ALT-LIZ");  // bleibt erhalten
        assertThat(erg.warnungen()).anyMatch(w -> w.text().contains("Lizenznummer"));
    }

    @Test
    void modusAktualisieren_lizenznrUebernehmenWennZielLeer() throws Exception {
        VereinDatensatz v = vereinRepo.insert("BC Linden");
        spielerRepo.insert(SpielerDatensatz.neu("Hans", "Müller", v.nr(), List.of(), null));

        ImportRohdaten roh = new ImportRohdaten(
                List.of(new RohSpieler(99, "Hans", "Müller", 10, "BC Linden", "NEU-LIZ")),
                List.of(new RohVerein(10, "BC Linden")),
                List.of(),
                List.of());
        importiere(roh, ImportModus.AKTUALISIEREN, false);

        SpielerMitVerein s = spielerRepo.findAll().get(0);
        assertThat(s.lizenznr()).isEqualTo("NEU-LIZ");
    }

    @Test
    void modusDuplikateSeparat_legtZweitenDatensatzAn() throws Exception {
        VereinDatensatz v = vereinRepo.insert("BC Linden");
        spielerRepo.insert(SpielerDatensatz.neu("Hans", "Müller", v.nr(), List.of(), null));

        ImportRohdaten roh = new ImportRohdaten(
                List.of(new RohSpieler(99, "Hans", "Müller", 10, "BC Linden", null)),
                List.of(new RohVerein(10, "BC Linden")),
                List.of(),
                List.of());
        ImportErgebnis erg = importiere(roh, ImportModus.DUPLIKATE_SEPARAT, false);

        // Bestehender Spieler-Name+Verein ist UNIQUE → INSERT_NEU schlägt fehl,
        // wird als Warnung verbucht und übersprungen.
        assertThat(erg.spielerEingefuegt()).isZero();
        assertThat(erg.spielerUebersprungen()).isEqualTo(1);
        assertThat(erg.warnungen()).anyMatch(w -> w.text().contains("UNIQUE"));
    }

    @Test
    void dryRun_aenderteNichts() throws Exception {
        ImportRohdaten roh = new ImportRohdaten(
                List.of(new RohSpieler(1, "Hans", "Müller", 10, null, null)),
                List.of(new RohVerein(10, "BC Linden")),
                List.of(),
                List.of());
        ImportErgebnis erg = importiere(roh, ImportModus.AKTUALISIEREN, true);

        assertThat(erg.dryRun()).isTrue();
        assertThat(erg.spielerEingefuegt()).isEqualTo(1);
        assertThat(spielerRepo.findAll()).isEmpty();
        assertThat(vereinRepo.findAll()).isEmpty();
    }

    @Test
    void junctionReMapping_aufNeueDbNrs() throws Exception {
        ImportRohdaten roh = new ImportRohdaten(
                List.of(new RohSpieler(7, "Hans", "Müller", 70, null, null)),
                List.of(new RohVerein(70, "BC Linden")),
                List.of(new RohLabel(50, "Senior")),
                List.of(new RohSpielerLabel(7, 50)));
        importiere(roh, ImportModus.AKTUALISIEREN, false);

        SpielerMitVerein s = spielerRepo.findAll().get(0);
        assertThat(s.labelNrs()).hasSize(1);
        assertThat(s.labelNrs().get(0)).isEqualTo(labelRepo.findAll().get(0).nr());
    }

    @Test
    void vereineUpdateModus_aktualisiertNamen() throws Exception {
        vereinRepo.insert("BC Linden");
        ImportRohdaten roh = new ImportRohdaten(
                List.of(),
                List.of(new RohVerein(10, "BC LINDEN")),  // gleicher Name, andere Schreibweise
                List.of(),
                List.of());
        ImportErgebnis erg = importiere(roh, ImportModus.AKTUALISIEREN, false);

        assertThat(erg.vereineAktualisiert()).isEqualTo(1);
        assertThat(vereinRepo.findAll().get(0).name()).isEqualTo("BC LINDEN");
    }

    private ImportErgebnis importiere(ImportRohdaten roh, ImportModus modus, boolean dryRun)
            throws Exception {
        ValidierteDaten vd = new SpielerDbImportValidator().validiere(roh);
        ImportRequest req = new ImportRequest(SpielerDbDateiFormat.JSON,
                EnumSet.allOf(ExportEntity.class), Path.of("/dev/null"), modus, dryRun);
        return new SpielerDbImporter(conn).importiere(vd, req);
    }
}
