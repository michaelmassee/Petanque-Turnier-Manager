/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.SpieleinstellungenDto;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

class PtmOnlineSpieleinstellungenTest {

    private static final String TURNIER = "spieleinstellungen-test";
    private static final SpieleinstellungenDto SCHWEIZER = new SpieleinstellungenDto("triplette", "mit_buchholz", null,
            null);

    @AfterEach
    void gedaechtnisLeeren() {
        PtmOnlineSpieleinstellungen.vergessen(TURNIER);
    }

    @Test
    void erfasstNurBeiAenderung() {
        AuftragsBestand bestand = AuftragsBestand.leer();

        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER)).isTrue();
        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER)).as("unverändert").isFalse();
        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER,
                new SpieleinstellungenDto("triplette", "ohne_buchholz", null, null))).as("Modus geändert").isTrue();

        assertThat(bestand.eintraege()).as("ein ungesendeter älterer Stand entfällt").singleElement()
                .satisfies(eintrag -> {
                    assertThat(eintrag.auftrag().art()).isEqualTo(AuftragsArt.SPIELEINSTELLUNGEN);
                    assertThat(eintrag.auftrag().body()).contains("ohne_buchholz");
                });
    }

    @Test
    void nachDemVergessenWirdErneutErfasst() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER);

        PtmOnlineSpieleinstellungen.vergessen(TURNIER);

        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER)).isTrue();
    }

    @Test
    void ohneEinstellungenEntstehtKeinAuftrag() {
        AuftragsBestand bestand = AuftragsBestand.leer();

        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SpieleinstellungenDto.KEINE)).isFalse();
        assertThat(bestand.hatOffene()).isFalse();
    }

    @Test
    void ordnetFormationUndModusDenOnlineWertenZu() {
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.TETE)).isEqualTo("tete");
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.DOUBLETTE)).isEqualTo("doublette");
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.TRIPLETTE)).isEqualTo("triplette");
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.MELEE)).isNull();
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.NUR_TEAMNAME)).isNull();
        assertThat(PtmOnlineSpieleinstellungen.supermeleeFormation(SuperMeleeMode.Doublette)).isEqualTo("doublette");
        assertThat(PtmOnlineSpieleinstellungen.onlineRankingModus(SchweizerRankingModus.OHNE_BUCHHOLZ))
                .isEqualTo("ohne_buchholz");
    }
}
