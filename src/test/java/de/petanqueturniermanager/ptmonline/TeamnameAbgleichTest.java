/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.RegistrationImportTask.BesetzungsRichtung;

/** Richtung des Teamnamen-Abgleichs und Ablage des zuletzt abgeglichenen Teamnamens im Sync-Blatt. */
class TeamnameAbgleichTest {

    @Test
    void gleicheTeamnamenSindGleich() {
        assertThat(RegistrationImportTask.teamnameRichtung("A", "A", Optional.of("X")))
                .isEqualTo(BesetzungsRichtung.GLEICH);
    }

    @Test
    void nurLokalGeaendertGehtOnline() {
        assertThat(RegistrationImportTask.teamnameRichtung("Neu", "Alt", Optional.of("Alt")))
                .isEqualTo(BesetzungsRichtung.NACH_ONLINE);
    }

    @Test
    void nurOnlineGeaendertWirdLokalUebernommen() {
        assertThat(RegistrationImportTask.teamnameRichtung("Alt", "Neu", Optional.of("Alt")))
                .isEqualTo(BesetzungsRichtung.NACH_LOKAL);
    }

    @Test
    void onlineNeuEingetragenerTeamnameWirdLokalUebernommen() {
        assertThat(RegistrationImportTask.teamnameRichtung("", "Neu", Optional.of("")))
                .isEqualTo(BesetzungsRichtung.NACH_LOKAL);
    }

    @Test
    void beidseitigGeaendertGehtDasDokumentVor() {
        assertThat(RegistrationImportTask.teamnameRichtung("Lokal", "Online", Optional.of("Alt")))
                .isEqualTo(BesetzungsRichtung.BEIDSEITIG);
    }

    @Test
    void lokalOderBeidseitigGeaendertGehtOnlineSonstNicht() {
        assertThat(RegistrationImportTask.gehtOnline(BesetzungsRichtung.NACH_ONLINE)).isTrue();
        assertThat(RegistrationImportTask.gehtOnline(BesetzungsRichtung.BEIDSEITIG)).as("das Dokument ist Master")
                .isTrue();
        assertThat(RegistrationImportTask.gehtOnline(BesetzungsRichtung.NACH_LOKAL)).isFalse();
        assertThat(RegistrationImportTask.gehtOnline(BesetzungsRichtung.GLEICH)).isFalse();
    }

    @Test
    void ohneGespeichertenStandUebernimmtLeererLokalerTeamnameDenOnlineNamen() {
        assertThat(RegistrationImportTask.teamnameRichtung("", "Online", Optional.empty()))
                .isEqualTo(BesetzungsRichtung.NACH_LOKAL);
    }

    @Test
    void ohneGespeichertenStandGehtEinLokalerTeamnameOnline() {
        assertThat(RegistrationImportTask.teamnameRichtung("Lokal", "Online", Optional.empty()))
                .isEqualTo(BesetzungsRichtung.NACH_ONLINE);
    }

    @Test
    void ohneTeamnamenAbgeglichenIstVonNieAbgeglichenUnterscheidbar() {
        assertThat(AbgeglichenerTeamname.lese(AbgeglichenerTeamname.alsText(null))).contains("");
        assertThat(AbgeglichenerTeamname.lese("")).isEmpty();
    }

    @Test
    void teamnameUeberstehtDieAblage() {
        assertThat(AbgeglichenerTeamname.lese(AbgeglichenerTeamname.alsText("  Les \"Boulistes\" "))).contains(
                "Les \"Boulistes\"");
    }

    @Test
    void unlesbarerZellinhaltGiltAlsNieAbgeglichen() {
        assertThat(AbgeglichenerTeamname.lese("{kaputt")).isEmpty();
    }
}
