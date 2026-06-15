/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.sheets;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Sichert die Gruppen-Zuordnung der PTM-Identitäts-Schlüssel ab.
 * <p>
 * Regression: Die {@code __PTM_<system>_CHECKIN_LISTE__}-Schlüssel waren in keiner
 * {@link SheetGruppe}-Präfixliste enthalten und fielen daher über
 * {@link SheetGruppe#fuerSchluessel(String)} auf {@code ALLGEMEIN} zurück – die
 * Checkin-Liste erschien in der Sidebar als generischer Eintrag statt unter ihrem
 * Turniersystem. Dieser Test stellt sicher, dass jedes System seinen Checkin-Schlüssel
 * korrekt gruppiert und direkt hinter die Meldeliste sortiert.
 */
class SheetGruppeTest {

    @Test
    void checkinListeSchluesselWerdenKorrektemSystemZugeordnet() {
        assertThat(SheetGruppe.fuerSchluessel("__PTM_SCHWEIZER_CHECKIN_LISTE__")).contains(SheetGruppe.SCHWEIZER);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_JGJ_CHECKIN_LISTE__")).contains(SheetGruppe.JGJ);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_KO_CHECKIN_LISTE__")).contains(SheetGruppe.KO);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_KASKADE_CHECKIN_LISTE__")).contains(SheetGruppe.KASKADE);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_FORMULEX_CHECKIN_LISTE__")).contains(SheetGruppe.FORMULEX);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_MAASTRICHTER_CHECKIN_LISTE__")).contains(SheetGruppe.MAASTRICHTER);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_POULE_CHECKIN_LISTE__")).contains(SheetGruppe.POULE);
    }

    @Test
    void checkinListeWirdDirektHinterDieMeldelisteSortiert() {
        assertCheckinDirektHinterMeldeliste(SheetGruppe.SCHWEIZER,
                "__PTM_SCHWEIZER_MELDELISTE__", "__PTM_SCHWEIZER_CHECKIN_LISTE__");
        assertCheckinDirektHinterMeldeliste(SheetGruppe.MAASTRICHTER,
                "__PTM_MAASTRICHTER_MELDELISTE__", "__PTM_MAASTRICHTER_CHECKIN_LISTE__");
        assertCheckinDirektHinterMeldeliste(SheetGruppe.POULE,
                "__PTM_POULE_MELDELISTE__", "__PTM_POULE_CHECKIN_LISTE__");
    }

    @Test
    void teilnehmerBleibtAllgemein() {
        assertThat(SheetGruppe.fuerSchluessel("__PTM_TEILNEHMER__")).contains(SheetGruppe.ALLGEMEIN);
    }

    @Test
    void ligaTermineProTeilnehmerBleibenAufLigaEbene() {
        String termine = "__PTM_LIGA_TERMINE_PRO_TEILNEHMER_1__";

        assertThat(SheetGruppe.fuerSchluessel(termine)).contains(SheetGruppe.LIGA);
        assertThat(SheetGruppe.LIGA.reihenfolgeDesSchluessels(termine))
                .as("Liga-Terminlisten direkt hinter Spielplan einsortieren")
                .isEqualTo(SheetGruppe.LIGA.reihenfolgeDesSchluessels("__PTM_LIGA_SPIELPLAN__") + 1);
    }

    private static void assertCheckinDirektHinterMeldeliste(SheetGruppe gruppe,
            String meldelisteSchluessel, String checkinSchluessel) {
        assertThat(gruppe.reihenfolgeDesSchluessels(checkinSchluessel))
                .as("Checkin-Liste direkt hinter Meldeliste in %s", gruppe)
                .isEqualTo(gruppe.reihenfolgeDesSchluessels(meldelisteSchluessel) + 1);
    }
}
