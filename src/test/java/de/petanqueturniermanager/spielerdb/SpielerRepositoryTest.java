package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.spielerdb.SpielerRepository.LizenzDuplikatException;
import de.petanqueturniermanager.spielerdb.SpielerRepository.NameDuplikatException;

class SpielerRepositoryTest {

    private SpielerDbConnection conn;
    private SpielerRepository repo;
    private VereinRepository vereinRepo;

    @BeforeEach
    void setUp() throws Exception {
        // Jeder DriverManager.getConnection auf jdbc:sqlite::memory: liefert
        // eine eigene, isolierte In-Memory-DB.
        conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:");
        repo = new SpielerRepository(conn);
        vereinRepo = new VereinRepository(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void insert_minimal() throws Exception {
        SpielerDatensatz d = repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, null));
        assertThat(d.nr()).isNotNull();
    }

    @Test
    void insert_pflichtfelderLeer_wirft() {
        assertThatThrownBy(() -> repo.insert(SpielerDatensatz.neu("", "Y", null, null, null)))
                .isInstanceOf(SpielerDbException.class);
        assertThatThrownBy(() -> repo.insert(SpielerDatensatz.neu("X", "  ", null, null, null)))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void insert_lizenzDoppelt_wirft() throws Exception {
        repo.insert(SpielerDatensatz.neu("A", "X", null, null, "L-1"));
        assertThatThrownBy(() -> repo.insert(SpielerDatensatz.neu("B", "Y", null, null, "L-1")))
                .isInstanceOf(LizenzDuplikatException.class);
    }

    @Test
    void insert_lizenzMehrfachNullErlaubt() throws Exception {
        repo.insert(SpielerDatensatz.neu("A", "X", null, null, null));
        repo.insert(SpielerDatensatz.neu("B", "Y", null, null, null));
        repo.insert(SpielerDatensatz.neu("C", "Z", null, null, "  "));
        assertThat(repo.findAll()).hasSize(3);
    }

    @Test
    void insert_mitVerein_findetVereinName() throws Exception {
        VereinDatensatz v = vereinRepo.insert("BC Linden");
        repo.insert(SpielerDatensatz.neu("Max", "Mustermann", v.nr(), null, null));

        SpielerMitVerein gefunden = repo.findAll().getFirst();
        assertThat(gefunden.vereinNr()).isEqualTo(v.nr());
        assertThat(gefunden.vereinName()).isEqualTo("BC Linden");
    }

    @Test
    void insert_ungueltigeVereinReferenz_wirft() {
        assertThatThrownBy(() -> repo.insert(SpielerDatensatz.neu("Max", "Mustermann", 99999, null, null)))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void findByNamePart_prefixCaseInsensitiv() throws Exception {
        repo.insert(SpielerDatensatz.neu("Anna", "Schmidt", null, null, null));
        repo.insert(SpielerDatensatz.neu("Bernd", "Schubert", null, null, null));
        repo.insert(SpielerDatensatz.neu("Carla", "Meier", null, null, null));

        assertThat(repo.findByNamePart("schu", 50)).extracting(SpielerMitVerein::nachname)
                .containsExactly("Schubert");
        // Vorname-Match
        assertThat(repo.findByNamePart("ann", 50)).extracting(SpielerMitVerein::vorname)
                .containsExactly("Anna");
        // beide Namen-Anfänge mit "S"
        assertThat(repo.findByNamePart("S", 50)).hasSize(2);
    }

    @Test
    void findByNamePart_kontainsKeinFalseMatch() throws Exception {
        repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, null));
        // kein contains: "uster" darf NICHT matchen, weil wir Prefix-Suche machen
        assertThat(repo.findByNamePart("uster", 50)).isEmpty();
    }

    @Test
    void findByNamePart_limitWirktSich() throws Exception {
        for (int i = 0; i < 10; i++) {
            repo.insert(SpielerDatensatz.neu("Vor" + i, "Nach" + i, null, null, null));
        }
        assertThat(repo.findByNamePart("", 5)).hasSize(5);
    }

    @Test
    void findByLizenz_findetGenauenTreffer() throws Exception {
        repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, "L-42"));
        assertThat(repo.findByLizenz("L-42")).isPresent();
        assertThat(repo.findByLizenz("l-42")).isEmpty(); // Lizenz case-sensitiv
        assertThat(repo.findByLizenz("")).isEmpty();
    }

    @Test
    void update_aendertFelder() throws Exception {
        VereinDatensatz v = vereinRepo.insert("Verein");
        SpielerDatensatz d = repo.insert(SpielerDatensatz.neu("Max", "Alt", null, null, null));

        repo.update(new SpielerDatensatz(d.nr(), "Max", "Neu", v.nr(), null, "L-1"));
        SpielerMitVerein nach = repo.findById(d.nr()).orElseThrow();
        assertThat(nach.nachname()).isEqualTo("Neu");
        assertThat(nach.vereinNr()).isEqualTo(v.nr());
        assertThat(nach.lizenznr()).isEqualTo("L-1");
    }

    @Test
    void delete_entferntDatensatz() throws Exception {
        SpielerDatensatz d = repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, null));
        repo.delete(d.nr());
        assertThat(repo.findById(d.nr())).isEmpty();
    }

    @Test
    void anzahl_zaehltDatensaetze() throws Exception {
        assertThat(repo.anzahl()).isZero();
        repo.insert(SpielerDatensatz.neu("Anna", "Schmidt", null, null, null));
        repo.insert(SpielerDatensatz.neu("Bert", "Müller", null, null, null));
        assertThat(repo.anzahl()).isEqualTo(2);
    }

    @Test
    void findeMitWildcard_prefixOhneWildcard() throws Exception {
        repo.insert(SpielerDatensatz.neu("Anna", "Schmidt", null, null, null));
        repo.insert(SpielerDatensatz.neu("Bert", "Schneider", null, null, null));
        repo.insert(SpielerDatensatz.neu("Carl", "Müller", null, null, null));

        assertThat(repo.findeMitWildcard("schm", false, 10))
                .extracting(SpielerMitVerein::nachname)
                .containsExactly("Schmidt");
    }

    @Test
    void findeMitWildcard_substringMitWildcard() throws Exception {
        repo.insert(SpielerDatensatz.neu("Anna", "Schmidt", null, null, null));
        repo.insert(SpielerDatensatz.neu("Bert", "Goldschmidt", null, null, null));
        repo.insert(SpielerDatensatz.neu("Carl", "Müller", null, null, null));

        assertThat(repo.findeMitWildcard("schmidt", true, 10))
                .extracting(SpielerMitVerein::nachname)
                .containsExactlyInAnyOrder("Schmidt", "Goldschmidt");
    }

    @Test
    void findeMitWildcard_findetAuchVorname() throws Exception {
        repo.insert(SpielerDatensatz.neu("Müller", "Anders", null, null, null));
        repo.insert(SpielerDatensatz.neu("Hans", "Müller", null, null, null));

        assertThat(repo.findeMitWildcard("mü", false, 10)).hasSize(2);
    }

    @Test
    void findeMitWildcard_caseInsensitivUndGetrimmt() throws Exception {
        repo.insert(SpielerDatensatz.neu("Max", "MÜLLER", null, null, null));

        assertThat(repo.findeMitWildcard("  müller  ", false, 10)).hasSize(1);
        assertThat(repo.findeMitWildcard("MÜLLER",     false, 10)).hasSize(1);
    }

    @Test
    void findeMitWildcard_leereSucheLiefertAlleBisLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            repo.insert(SpielerDatensatz.neu("V" + i, "N" + i, null, null, null));
        }
        assertThat(repo.findeMitWildcard("", false, 3)).hasSize(3);
    }

    @Test
    void findeMitWildcard_nullSucheBehandeltWieLeer() throws Exception {
        repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, null));
        assertThat(repo.findeMitWildcard(null, false, 10)).hasSize(1);
    }

    @Test
    void insert_nameDuplikatMitGleichemVerein_wirft() throws Exception {
        VereinDatensatz v = vereinRepo.insert("BC Linden");
        repo.insert(SpielerDatensatz.neu("Max", "Mustermann", v.nr(), null, null));

        assertThatThrownBy(() -> repo.insert(SpielerDatensatz.neu("Max", "Mustermann", v.nr(), null, null)))
                .isInstanceOf(NameDuplikatException.class);
    }

    @Test
    void insert_nameDuplikatOhneVerein_wirft() throws Exception {
        // UQ_SPIELER_NAME_VEREIN nutzt COALESCE(VEREIN_NR, -1) -> auch ohne Verein
        // gilt derselbe Name zweimal als Duplikat.
        repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, null));
        assertThatThrownBy(() -> repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, null)))
                .isInstanceOf(NameDuplikatException.class);
    }

    @Test
    void update_nrNull_wirft() {
        assertThatThrownBy(() -> repo.update(SpielerDatensatz.neu("Max", "Mustermann", null, null, null)))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void update_pflichtfelderLeer_wirft() throws Exception {
        SpielerDatensatz d = repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, null));
        assertThatThrownBy(() -> repo.update(new SpielerDatensatz(d.nr(), "  ", "Mustermann", null, null, null)))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void update_unbekannteNr_wirft() {
        assertThatThrownBy(() -> repo.update(new SpielerDatensatz(999, "Max", "Mustermann", null, null, null)))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void update_lizenzDoppelt_wirft() throws Exception {
        repo.insert(SpielerDatensatz.neu("A", "X", null, null, "L-1"));
        SpielerDatensatz d = repo.insert(SpielerDatensatz.neu("B", "Y", null, null, null));

        assertThatThrownBy(() -> repo.update(new SpielerDatensatz(d.nr(), "B", "Y", null, null, "L-1")))
                .isInstanceOf(LizenzDuplikatException.class);
    }

    @Test
    void update_ungueltigeVereinReferenz_wirft() throws Exception {
        SpielerDatensatz d = repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, null));
        assertThatThrownBy(() -> repo.update(new SpielerDatensatz(d.nr(), "Max", "Mustermann", 99999, null, null)))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void delete_unbekannteNr_wirft() {
        assertThatThrownBy(() -> repo.delete(999))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void findeDuplikat_findetGleichenNamenUndVerein() throws Exception {
        VereinDatensatz v = vereinRepo.insert("BC Linden");
        SpielerDatensatz d = repo.insert(SpielerDatensatz.neu("Max", "Mustermann", v.nr(), null, null));

        assertThat(repo.findeDuplikat("max", "mustermann", v.nr(), null))
                .isPresent().get().extracting(SpielerMitVerein::nr).isEqualTo(d.nr());
    }

    @Test
    void findeDuplikat_ohneVereinMatchtNurOhneVerein() throws Exception {
        VereinDatensatz v = vereinRepo.insert("BC Linden");
        repo.insert(SpielerDatensatz.neu("Max", "Mustermann", v.nr(), null, null));

        assertThat(repo.findeDuplikat("Max", "Mustermann", null, null)).isEmpty();
    }

    @Test
    void findeDuplikat_ausserNrSchliesstEigenenDatensatzAus() throws Exception {
        SpielerDatensatz d = repo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, null, null));

        assertThat(repo.findeDuplikat("Max", "Mustermann", null, d.nr())).isEmpty();
    }

    @Test
    void findeDuplikat_leererNameLiefertEmpty() throws Exception {
        assertThat(repo.findeDuplikat("  ", "Mustermann", null, null)).isEmpty();
        assertThat(repo.findeDuplikat("Max", "", null, null)).isEmpty();
    }
}
