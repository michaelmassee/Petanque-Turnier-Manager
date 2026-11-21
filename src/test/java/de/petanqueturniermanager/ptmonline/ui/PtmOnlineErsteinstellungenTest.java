/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJPropertiesSpalte;
import de.petanqueturniermanager.liga.konfiguration.LigaPropertiesSpalte;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterPropertiesSpalte;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerPropertiesSpalte;
import de.petanqueturniermanager.triptete.konfiguration.TripTetePropertiesSpalte;

/**
 * Ersteinstellungen beim ersten Verbinden: einmal je Dokument, nur für die Schalter des Turniersystems, vorbelegt mit
 * dem Stand des Dokuments (Default an).
 */
class PtmOnlineErsteinstellungenTest {

    private static final String LIVE = BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT;
    private static final String MELDUNG = BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG;
    private static final String DIREKT = BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISSE_DIREKT;

    @Test
    void systemeKennenAlleDreiSchalter() {
        assertThat(PtmOnlineErsteinstellungen.vorhandeneSchalter(SchweizerPropertiesSpalte.KONFIG_PROPERTIES))
                .containsExactly(LIVE, MELDUNG, DIREKT);
        assertThat(PtmOnlineErsteinstellungen.vorhandeneSchalter(JGJPropertiesSpalte.KONFIG_PROPERTIES))
                .containsExactly(LIVE, MELDUNG, DIREKT);
        assertThat(PtmOnlineErsteinstellungen.vorhandeneSchalter(MaastrichterPropertiesSpalte.KONFIG_PROPERTIES))
                .containsExactly(LIVE, MELDUNG, DIREKT);
    }

    @Test
    void tripTeteKenntNurDieLiveAnsichtUndLigaKeinenSchalter() {
        assertThat(PtmOnlineErsteinstellungen.vorhandeneSchalter(TripTetePropertiesSpalte.KONFIG_PROPERTIES))
                .containsExactly(LIVE);
        assertThat(PtmOnlineErsteinstellungen.vorhandeneSchalter(LigaPropertiesSpalte.KONFIG_PROPERTIES)).isEmpty();
        assertThat(PtmOnlineErsteinstellungen.vorhandeneSchalter(null)).isEmpty();
    }

    @Test
    void gefragtWirdNurBeimErstenMalUndNurMitSchaltern() {
        assertThat(PtmOnlineErsteinstellungen.istAbzufragen(false, Set.of(LIVE))).isTrue();
        assertThat(PtmOnlineErsteinstellungen.istAbzufragen(true, Set.of(LIVE))).isFalse();
        assertThat(PtmOnlineErsteinstellungen.istAbzufragen(false, Set.of())).isFalse();
    }

    @Test
    void ohneGespeichertenStandIstAllesAn() {
        assertThat(PtmOnlineErsteinstellungen.vorbelegung((schalter, defaultWert) -> defaultWert))
                .isEqualTo(new PtmOnlineErsteinstellungen.Auswahl(true, true, true));
    }

    @Test
    void vorbelegungFolgtDemDokument() {
        Map<String, Boolean> dokument = Map.of(LIVE, true, MELDUNG, false, DIREKT, false);

        assertThat(PtmOnlineErsteinstellungen.vorbelegung((schalter, defaultWert) -> dokument.get(schalter)))
                .isEqualTo(new PtmOnlineErsteinstellungen.Auswahl(true, false, false));
    }

    @Test
    void uebernimmtNurDieSchalterDesSystems() {
        Map<String, Boolean> geschrieben = new LinkedHashMap<>();

        PtmOnlineErsteinstellungen.uebernehmen(new PtmOnlineErsteinstellungen.Auswahl(false, true, true),
                Set.of(LIVE), geschrieben::put);

        assertThat(geschrieben).containsExactly(Map.entry(LIVE, false));
    }

    @Test
    void uebernimmtAlleDreiWerte() {
        Map<String, Boolean> geschrieben = new LinkedHashMap<>();

        PtmOnlineErsteinstellungen.uebernehmen(new PtmOnlineErsteinstellungen.Auswahl(true, true, false),
                Set.copyOf(List.of(LIVE, MELDUNG, DIREKT)), geschrieben::put);

        assertThat(geschrieben).containsExactly(Map.entry(LIVE, true), Map.entry(MELDUNG, true),
                Map.entry(DIREKT, false));
    }
}
