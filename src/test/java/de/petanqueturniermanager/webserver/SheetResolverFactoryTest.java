/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Sichert die generische, system-agnostische Sheet-Typ-Konfiguration der Webserver-Panels ab.
 * <p>
 * Hintergrund: Statt ~33 system-spezifischer Schlüssel gibt es generische Rollen
 * ({@code RANGLISTE}, {@code AKTUELLE_SPIELRUNDE}, {@code MELDELISTE}, {@code CHECKIN},
 * {@code TEILNEHMER}), die über {@link AktivesSystemSheetResolver} auf das aktive System
 * auflösen. Echte Parallel-Sub-Blätter bleiben system-gefilterte Spezial-Einträge. Alte
 * Schlüssel persistierter Konfigurationen werden per Alias migriert.
 */
class SheetResolverFactoryTest {

    @Test
    void generischeRolleErzeugtAktivesSystemResolver() {
        assertThat(SheetResolverFactory.erstellen("RANGLISTE"))
                .isInstanceOf(AktivesSystemSheetResolver.class);
        assertThat(SheetResolverFactory.erstellen("AKTUELLE_SPIELRUNDE"))
                .isInstanceOf(AktivesSystemSheetResolver.class);
        assertThat(SheetResolverFactory.erstellen("MELDELISTE"))
                .isInstanceOf(AktivesSystemSheetResolver.class);
        assertThat(SheetResolverFactory.erstellen("CHECKIN"))
                .isInstanceOf(AktivesSystemSheetResolver.class);
        assertThat(SheetResolverFactory.erstellen("TEILNEHMER"))
                .isInstanceOf(TeilnehmerSheetResolver.class);
    }

    @Test
    void alteSystemspezifischeSchluesselWerdenAufGenerischeRolleMigriert() {
        assertThat(SheetResolverFactory.erstellen("SCHWEIZER_RANGLISTE"))
                .as("Legacy-Alias → generische RANGLISTE")
                .isInstanceOf(AktivesSystemSheetResolver.class);
        assertThat(SheetResolverFactory.erstellen("KO_TURNIERBAUM"))
                .as("Legacy-Alias → generische AKTUELLE_SPIELRUNDE")
                .isInstanceOf(AktivesSystemSheetResolver.class);
        assertThat(SheetResolverFactory.erstellen("SPIELTAG_ANMELDUNGEN"))
                .as("Legacy-Alias → generische CHECKIN")
                .isInstanceOf(AktivesSystemSheetResolver.class);
    }

    @Test
    void unbekannterWertWirdZuStatischemSheetNamen() {
        assertThat(SheetResolverFactory.erstellen("Mein eigenes Blatt"))
                .isInstanceOf(StaticSheetResolver.class);
    }

    @Test
    void comboBoxFiltertSpezialEintraegeFremderSysteme() {
        var schweizer = Arrays.asList(SheetResolverFactory.sheetTypenFuer(TurnierSystem.SCHWEIZER));
        assertThat(schweizer)
                .contains("RANGLISTE", "AKTUELLE_SPIELRUNDE", "MELDELISTE", "CHECKIN", "TEILNEHMER")
                .doesNotContain("MAASTRICHTER_FINALRUNDE_A", "KASKADE_FELD_A", "POULE_KO_A",
                        "SUPERMELEE_ENDRANGLISTE", "FORME_CADRAGE");
    }

    @Test
    void comboBoxZeigtSystemeigeneSpezialEintraege() {
        assertThat(SheetResolverFactory.sheetTypenFuer(TurnierSystem.SUPERMELEE))
                .contains("SUPERMELEE_ENDRANGLISTE", "RANGLISTE");
        assertThat(SheetResolverFactory.sheetTypenFuer(TurnierSystem.MAASTRICHTER))
                .contains("MAASTRICHTER_FINALRUNDE_A", "MAASTRICHTER_FINALRUNDE_D");
    }

    @Test
    void ohneAktivesSystemVolleListe() {
        assertThat(SheetResolverFactory.sheetTypenFuer(null))
                .isEqualTo(SheetResolverFactory.SHEET_TYPEN);
        assertThat(SheetResolverFactory.sheetTypenFuer(TurnierSystem.KEIN))
                .isEqualTo(SheetResolverFactory.SHEET_TYPEN);
    }
}
