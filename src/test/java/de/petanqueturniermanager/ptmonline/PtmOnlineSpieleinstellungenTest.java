/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnameModus;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.SpieleinstellungenDto;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

class PtmOnlineSpieleinstellungenTest {

    private static final String TURNIER = "spieleinstellungen-test";
    private static final SpieleinstellungenDto SCHWEIZER = new SpieleinstellungenDto("triplette", "mit_buchholz", null,
            null);

    @Test
    void erfasstEineGeaenderteLiveFreigabe() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER.mitLive(true, true))).isTrue();
        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER.mitLive(false, true))).isTrue();
        assertThat(bestand.eintraege()).singleElement()
                .satisfies(eintrag -> assertThat(eintrag.auftrag().body()).contains("\"liveViewEnabled\":false"));
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

        PtmOnlineSpieleinstellungen.vergessen(bestand, TURNIER);

        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER)).isTrue();
    }

    @Test
    void kopieDesselbenTurniersUnterdruecktDieEinstellungenDesOriginalsNicht() {
        AuftragsBestand original = AuftragsBestand.leer();
        AuftragsBestand kopie = AuftragsBestand.leer();
        PtmOnlineSpieleinstellungen.erfasse(kopie, TURNIER, SCHWEIZER);

        assertThat(PtmOnlineSpieleinstellungen.erfasse(original, TURNIER, SCHWEIZER)).isTrue();
    }

    @Test
    void ohneEinstellungenEntstehtKeinAuftrag() {
        AuftragsBestand bestand = AuftragsBestand.leer();

        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SpieleinstellungenDto.KEINE)).isFalse();
        assertThat(bestand.hatOffene()).isFalse();
    }

    @Test
    void nurTeamnameGehtAlsFormationAndereMitTeamnamenHinaus() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        SpieleinstellungenDto nurTeamname = new SpieleinstellungenDto(
                PtmOnlineSpieleinstellungen.onlineFormation(Formation.NUR_TEAMNAME), null, null, null)
                .mitTeamname(TeamnameModus.NUR_TEAMNAME.teamnameSpalte());

        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, nurTeamname.mitLive(true, true))).isTrue();
        assertThat(bestand.eintraege()).singleElement().satisfies(eintrag -> assertThat(eintrag.auftrag().body())
                .contains("\"formation\":\"andere\"", "\"teamNameEnabled\":true", "\"liveViewEnabled\":true"));
    }

    @Test
    void geaenderterTeamnameModusWirdErneutErfasst() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER.mitTeamname(false))).isTrue();
        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER.mitTeamname(false))).isFalse();
        assertThat(PtmOnlineSpieleinstellungen.erfasse(bestand, TURNIER, SCHWEIZER.mitTeamname(true)))
                .as("Teamname-Spalte eingeschaltet").isTrue();
    }

    @Test
    void nurSystemeMitOnlineGegenstueckUebertragenEinstellungen() {
        assertThat(PtmOnlineSpieleinstellungen.hatOnlineGegenstueck(TurnierSystem.FORMULEX)).isTrue();
        assertThat(PtmOnlineSpieleinstellungen.hatOnlineGegenstueck(TurnierSystem.JGJ)).isTrue();
        assertThat(PtmOnlineSpieleinstellungen.hatOnlineGegenstueck(TurnierSystem.LIGA)).isFalse();
        assertThat(PtmOnlineSpieleinstellungen.hatOnlineGegenstueck(TurnierSystem.KEIN)).isFalse();
    }

    @Test
    void ordnetFormationUndModusDenOnlineWertenZu() {
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.TETE)).isEqualTo("tete");
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.DOUBLETTE)).isEqualTo("doublette");
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.TRIPLETTE)).isEqualTo("triplette");
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.MELEE)).isNull();
        assertThat(PtmOnlineSpieleinstellungen.onlineFormation(Formation.NUR_TEAMNAME)).isEqualTo("andere");
        assertThat(PtmOnlineSpieleinstellungen.onlineRankingModus(SchweizerRankingModus.OHNE_BUCHHOLZ))
                .isEqualTo("ohne_buchholz");
    }

    @Test
    void supermeleeModusEntsprichtDerFormation() {
        assertThat(PtmOnlineFormation.ausSupermeleeModus(SuperMeleeMode.Triplette)).isEqualTo(Formation.TRIPLETTE);
        assertThat(PtmOnlineFormation.ausSupermeleeModus(SuperMeleeMode.Doublette)).isEqualTo(Formation.DOUBLETTE);
    }
}
