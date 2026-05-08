package de.petanqueturniermanager.spielerdb.webview;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.spielerdb.LabelRepository;
import de.petanqueturniermanager.spielerdb.SpielerDatensatz;
import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.spielerdb.VereinRepository;

class SpielerDbApiHandlerTest {

    private SpielerDbConnection conn;
    private SpielerDbApiHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:");
        SpielerRepository spielerRepo = new SpielerRepository(conn);
        VereinRepository vereinRepo = new VereinRepository(conn);
        LabelRepository labelRepo = new LabelRepository(conn);

        var clubA = vereinRepo.insert("Boule Aachen");
        vereinRepo.insert("Petanque Bonn");
        labelRepo.insert("Lizenz");
        spielerRepo.insert(SpielerDatensatz.neu("Anna", "Müller", clubA.nr(), null, "L-100"));
        spielerRepo.insert(SpielerDatensatz.neu("Bert", "Schulz", null, null, null));

        handler = new SpielerDbApiHandler(spielerRepo, vereinRepo, labelRepo, conn.getJdbcUrl());
    }

    @AfterEach
    void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void stats_enthaeltCounts() throws Exception {
        String json = handler.renderStatsJson();
        assertThat(json).contains("\"spielerCount\":2");
        assertThat(json).contains("\"vereineCount\":2");
        assertThat(json).contains("\"labelsCount\":1");
        assertThat(json).contains("\"dbPath\":\"jdbc:sqlite::memory:\"");
    }

    @Test
    void spielerListe_paginiert() throws Exception {
        String json = handler.renderSpielerJson("limit=1&offset=0");
        assertThat(json).contains("\"total\":2");
        assertThat(json).contains("\"limit\":1");
        assertThat(json).contains("\"offset\":0");
    }

    @Test
    void spielerListe_filtert_via_q() throws Exception {
        String json = handler.renderSpielerJson("q=schul");
        assertThat(json).contains("Schulz");
        assertThat(json).doesNotContain("Müller");
    }

    @Test
    void spielerListe_invalidLimit_faelltAufDefault() throws Exception {
        String json = handler.renderSpielerJson("limit=abc");
        assertThat(json).contains("\"limit\":200");
    }
}
