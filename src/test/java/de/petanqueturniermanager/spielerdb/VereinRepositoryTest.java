package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.spielerdb.VereinRepository.DuplikatException;
import de.petanqueturniermanager.spielerdb.VereinRepository.InBenutzungException;

class VereinRepositoryTest {

    private SpielerDbConnection conn;
    private VereinRepository repo;
    private SpielerRepository spielerRepo;

    @BeforeEach
    void setUp() throws Exception {
        // Jeder DriverManager.getConnection auf jdbc:sqlite::memory: liefert
        // eine eigene, isolierte In-Memory-DB.
        conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:");
        repo = new VereinRepository(conn);
        spielerRepo = new SpielerRepository(conn);
    }

    @AfterEach
    void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void insertUndFindByName_caseInsensitiv() throws Exception {
        VereinDatensatz angelegt = repo.insert("BC Linden");
        assertThat(angelegt.nr()).isNotNull();
        assertThat(angelegt.name()).isEqualTo("BC Linden");

        assertThat(repo.findByName("bc linden")).isPresent()
                .get().extracting(VereinDatensatz::nr).isEqualTo(angelegt.nr());
        assertThat(repo.findByName("  BC LINDEN  ")).isPresent();
    }

    @Test
    void insert_trimmtNamen() throws Exception {
        VereinDatensatz v = repo.insert("  BC Linden  ");
        assertThat(v.name()).isEqualTo("BC Linden");
    }

    @Test
    void insert_duplikatWirftDuplikatException() throws Exception {
        repo.insert("BC Linden");
        assertThatThrownBy(() -> repo.insert("BC Linden"))
                .isInstanceOf(DuplikatException.class);
    }

    @Test
    void insert_leererName_wirft() {
        assertThatThrownBy(() -> repo.insert("   "))
                .isInstanceOf(SpielerDbException.class);
    }

    @Test
    void update_aendertNamen() throws Exception {
        VereinDatensatz v = repo.insert("Alt");
        repo.update(v.nr(), "Neu");
        assertThat(repo.findById(v.nr())).get().extracting(VereinDatensatz::name).isEqualTo("Neu");
    }

    @Test
    void delete_ohneSpieler_funktioniert() throws Exception {
        VereinDatensatz v = repo.insert("Temp");
        repo.delete(v.nr());
        assertThat(repo.findById(v.nr())).isEmpty();
    }

    @Test
    void delete_mitSpielern_wirftInBenutzungException() throws Exception {
        VereinDatensatz v = repo.insert("BC Linden");
        spielerRepo.insert(SpielerDatensatz.neu("Max", "Mustermann", v.nr(), null));

        assertThatThrownBy(() -> repo.delete(v.nr()))
                .isInstanceOf(InBenutzungException.class);
        assertThat(repo.findById(v.nr())).isPresent();
    }

    @Test
    void countSpieler_zaehltZuordnungen() throws Exception {
        VereinDatensatz v = repo.insert("BC Linden");
        assertThat(repo.countSpieler(v.nr())).isZero();
        spielerRepo.insert(SpielerDatensatz.neu("A", "X", v.nr(), null));
        spielerRepo.insert(SpielerDatensatz.neu("B", "Y", v.nr(), null));
        assertThat(repo.countSpieler(v.nr())).isEqualTo(2);
    }

    @Test
    void findAll_alphabetisch() throws Exception {
        repo.insert("zeta");
        repo.insert("alpha");
        repo.insert("Mike");
        assertThat(repo.findAll()).extracting(VereinDatensatz::name)
                .containsExactly("alpha", "Mike", "zeta");
    }
}
