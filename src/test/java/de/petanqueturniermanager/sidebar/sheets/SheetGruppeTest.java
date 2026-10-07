/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.sheets;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

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

    /**
     * Regression: dasselbe Muster wie bei der Checkin-Liste ({@link #checkinListeSchluesselWerdenKorrektemSystemZugeordnet()}),
     * diesmal für die {@code __PTM_<system>_MELEE_ANMELDUNG__}-Schlüssel – fielen ebenfalls auf
     * {@code ALLGEMEIN} zurück, statt vor der Meldeliste ihres Systems zu erscheinen.
     */
    @Test
    void meleeAnmeldungSchluesselWerdenKorrektemSystemZugeordnet() {
        assertThat(SheetGruppe.fuerSchluessel("__PTM_SCHWEIZER_MELEE_ANMELDUNG__")).contains(SheetGruppe.SCHWEIZER);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_JGJ_MELEE_ANMELDUNG__")).contains(SheetGruppe.JGJ);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_KO_MELEE_ANMELDUNG__")).contains(SheetGruppe.KO);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_KASKADE_MELEE_ANMELDUNG__")).contains(SheetGruppe.KASKADE);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_FORMULEX_MELEE_ANMELDUNG__")).contains(SheetGruppe.FORMULEX);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_MAASTRICHTER_MELEE_ANMELDUNG__")).contains(SheetGruppe.MAASTRICHTER);
        assertThat(SheetGruppe.fuerSchluessel("__PTM_POULE_MELEE_ANMELDUNG__")).contains(SheetGruppe.POULE);
    }

    @Test
    void meleeAnmeldungWirdVorDerMeldelisteSortiert() {
        assertMeleeVorMeldeliste(SheetGruppe.SCHWEIZER,
                "__PTM_SCHWEIZER_MELEE_ANMELDUNG__", "__PTM_SCHWEIZER_MELDELISTE__");
        assertMeleeVorMeldeliste(SheetGruppe.JGJ,
                "__PTM_JGJ_MELEE_ANMELDUNG__", "__PTM_JGJ_MELDELISTE__");
        assertMeleeVorMeldeliste(SheetGruppe.KO,
                "__PTM_KO_MELEE_ANMELDUNG__", "__PTM_KO_MELDELISTE__");
        assertMeleeVorMeldeliste(SheetGruppe.KASKADE,
                "__PTM_KASKADE_MELEE_ANMELDUNG__", "__PTM_KASKADE_MELDELISTE__");
        assertMeleeVorMeldeliste(SheetGruppe.FORMULEX,
                "__PTM_FORMULEX_MELEE_ANMELDUNG__", "__PTM_FORMULEX_MELDELISTE__");
        assertMeleeVorMeldeliste(SheetGruppe.MAASTRICHTER,
                "__PTM_MAASTRICHTER_MELEE_ANMELDUNG__", "__PTM_MAASTRICHTER_MELDELISTE__");
        assertMeleeVorMeldeliste(SheetGruppe.POULE,
                "__PTM_POULE_MELEE_ANMELDUNG__", "__PTM_POULE_MELDELISTE__");
    }

    /** Turniersystem-übergreifende Blätter, die bewusst in {@link SheetGruppe#ALLGEMEIN} stehen. */
    private static final Set<String> BEWUSST_ALLGEMEIN = Set.of(
            SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
            SheetMetadataHelper.SCHLUESSEL_SIEGERGELD,
            SheetMetadataHelper.SCHLUESSEL_PLANUNGSRECHNER);

    /**
     * Gate: Jeder Blatt-Schlüssel aus {@link SheetMetadataHelper} muss seinem Turniersystem
     * zugeordnet sein. Fehlt er in {@link SheetGruppe}, fällt das Blatt auf ALLGEMEIN zurück und
     * wird alphabetisch einsortiert (Regression: Trip-Tête-Rangliste stand vor dem Spielplan;
     * Maastrichter-Vorrunden-Rangliste und JGJ-Gesamtrangliste/-Gruppenspielpläne ebenso).
     */
    @Test
    void jederSystemSchluesselHatEineGruppe() throws IllegalAccessException {
        List<String> ohneGruppe = new ArrayList<>();
        for (Field feld : SheetMetadataHelper.class.getFields()) {
            if (!Modifier.isStatic(feld.getModifiers()) || feld.getType() != String.class
                    || !feld.getName().startsWith("SCHLUESSEL_")) {
                continue;
            }
            String schluessel = (String) feld.get(null);
            if (!schluessel.startsWith("__PTM_") || BEWUSST_ALLGEMEIN.contains(schluessel)) {
                continue;
            }
            var gruppe = SheetGruppe.fuerSchluessel(schluessel);
            if (gruppe.isEmpty() || gruppe.get() == SheetGruppe.ALLGEMEIN) {
                ohneGruppe.add(feld.getName() + " = " + schluessel);
            }
        }
        assertThat(ohneGruppe).as("Schlüssel ohne Turniersystem-Gruppe in SheetGruppe").isEmpty();
    }

    @Test
    void tripTeteRanglisteWirdHinterDemSpielplanSortiert() {
        assertThat(SheetGruppe.TRIPTETE.reihenfolgeDesSchluessels(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE))
                .isGreaterThan(SheetGruppe.TRIPTETE
                        .reihenfolgeDesSchluessels(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN));
        assertCheckinDirektHinterMeldeliste(SheetGruppe.TRIPTETE,
                SheetMetadataHelper.SCHLUESSEL_TRIPTETE_MELDELISTE, SheetMetadataHelper.SCHLUESSEL_TRIPTETE_CHECKIN_LISTE);
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

    private static void assertMeleeVorMeldeliste(SheetGruppe gruppe,
            String meleeSchluessel, String meldelisteSchluessel) {
        assertThat(gruppe.reihenfolgeDesSchluessels(meleeSchluessel))
                .as("Mêlée-Anmeldung vor Meldeliste in %s", gruppe)
                .isLessThan(gruppe.reihenfolgeDesSchluessels(meldelisteSchluessel));
    }
}
