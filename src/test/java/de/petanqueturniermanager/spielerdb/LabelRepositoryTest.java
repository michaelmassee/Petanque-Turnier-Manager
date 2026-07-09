package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.spielerdb.LabelRepository.DuplikatException;
import de.petanqueturniermanager.spielerdb.LabelRepository.InBenutzungException;

class LabelRepositoryTest {

    private SpielerDbConnection conn;
    private LabelRepository repo;
    private SpielerRepository spielerRepo;

    @BeforeEach
    void setUp() throws Exception {
        // Jeder DriverManager.getConnection auf jdbc:sqlite::memory: liefert
        // eine eigene, isolierte In-Memory-DB.
        conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:");
        repo = new LabelRepository(conn);
        spielerRepo = new SpielerRepository(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void insertUndFindByName_caseInsensitiv() throws Exception {
        LabelDatensatz angelegt = repo.insert("Anfänger");
        assertThat(angelegt.nr()).isNotNull();
        assertThat(angelegt.name()).isEqualTo("Anfänger");

        assertThat(repo.findByName("anfänger")).isPresent()
                .get().extracting(LabelDatensatz::nr).isEqualTo(angelegt.nr());
        assertThat(repo.findByName("  ANFÄNGER  ")).isPresent();
    }

    @Test
    void insert_trimmtNamen() throws Exception {
        LabelDatensatz l = repo.insert("  Anfänger  ");
        assertThat(l.name()).isEqualTo("Anfänger");
    }

    @Test
    void insert_duplikatWirftDuplikatException() throws Exception {
        repo.insert("Anfänger");
        assertThatThrownBy(() -> repo.insert("Anfänger"))
                .isInstanceOf(DuplikatException.class);
    }

    @Test
    void insert_leererName_wirft() {
        assertThatThrownBy(() -> repo.insert("   "))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void update_aendertNamen() throws Exception {
        LabelDatensatz l = repo.insert("Alt");
        repo.update(l.nr(), "Neu");
        assertThat(repo.findById(l.nr())).get().extracting(LabelDatensatz::name).isEqualTo("Neu");
    }

    @Test
    void update_leererName_wirft() throws Exception {
        LabelDatensatz l = repo.insert("Alt");
        assertThatThrownBy(() -> repo.update(l.nr(), "   "))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void update_unbekannteNr_wirft() {
        assertThatThrownBy(() -> repo.update(999, "Neu"))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void update_aufBereitsVorhandenenNamen_wirftDuplikatException() throws Exception {
        repo.insert("Anfänger");
        LabelDatensatz zweites = repo.insert("Fortgeschritten");
        assertThatThrownBy(() -> repo.update(zweites.nr(), "Anfänger"))
                .isInstanceOf(DuplikatException.class);
    }

    @Test
    void delete_ohneSpieler_funktioniert() throws Exception {
        LabelDatensatz l = repo.insert("Temp");
        repo.delete(l.nr());
        assertThat(repo.findById(l.nr())).isEmpty();
    }

    @Test
    void delete_unbekannteNr_wirft() {
        assertThatThrownBy(() -> repo.delete(999))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void delete_mitSpielern_wirftInBenutzungException() throws Exception {
        LabelDatensatz l = repo.insert("Anfänger");
        spielerRepo.insert(SpielerDatensatz.neu("Max", "Mustermann", null, List.of(l.nr()), null));

        assertThatThrownBy(() -> repo.delete(l.nr()))
                .isInstanceOf(InBenutzungException.class);
        assertThat(repo.findById(l.nr())).isPresent();
    }

    @Test
    void countSpieler_zaehltZuordnungen() throws Exception {
        LabelDatensatz l = repo.insert("Anfänger");
        assertThat(repo.countSpieler(l.nr())).isZero();
        spielerRepo.insert(SpielerDatensatz.neu("A", "X", null, List.of(l.nr()), null));
        spielerRepo.insert(SpielerDatensatz.neu("B", "Y", null, List.of(l.nr()), null));
        assertThat(repo.countSpieler(l.nr())).isEqualTo(2);
    }

    @Test
    void findAll_alphabetisch() throws Exception {
        repo.insert("zeta");
        repo.insert("alpha");
        repo.insert("Mike");
        assertThat(repo.findAll()).extracting(LabelDatensatz::name)
                .containsExactly("alpha", "Mike", "zeta");
    }

    @Test
    void findById_unbekannt_liefertEmpty() throws Exception {
        assertThat(repo.findById(999)).isEmpty();
    }
}
