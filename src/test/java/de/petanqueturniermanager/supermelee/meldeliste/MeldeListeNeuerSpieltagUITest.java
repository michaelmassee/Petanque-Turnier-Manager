package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Update;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * UI-Tests für MeldeListeSheet_NeuerSpieltag.naechsteSpieltag() –
 * prüft Spieltag-Inkrementierung, Spielrunde-Reset und korrekte Sheet-Erstellung über Spieltage hinweg.
 */
public class MeldeListeNeuerSpieltagUITest extends BaseCalcUITest {

    private MeldeListeSheet_NeuerSpieltag meldeListeNeuerSpieltag;
    private TestSuperMeleeMeldeListeErstellen testMeldeListeErstellen;

    @BeforeEach
    public void setUp() throws GenerateException {
        testMeldeListeErstellen = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
        testMeldeListeErstellen.initMitAlleDieSpielen(25);
        meldeListeNeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet);
    }

    /**
     * naechsteSpieltag() muss den Spieltag auf 2 setzen, die Spielrunde auf 1 zurücksetzen
     * und eine neue Spieltag-Spalte in der Meldeliste anlegen.
     */
    @Test
    public void testNaechsterSpieltagInkrementiert() throws GenerateException {
        meldeListeNeuerSpieltag.naechsteSpieltag();

        assertThat(docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 999))
                .as("Spieltag-Konfig muss auf 2 gesetzt sein")
                .isEqualTo(2);
        assertThat(docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, 999))
                .as("Spielrunde-Konfig muss auf 1 zurückgesetzt sein")
                .isEqualTo(1);
        assertThat(meldeListeNeuerSpieltag.countAnzSpieltageInMeldeliste())
                .as("Meldeliste muss 2 Spieltage enthalten")
                .isEqualTo(2);
        assertThat(meldeListeNeuerSpieltag.getSpielTag().getNr())
                .as("Aktiver Spieltag muss 2 sein")
                .isEqualTo(2);
    }

    /**
     * Nach einem Spieltag-Wechsel muss die Spielrunde im neuen Spieltag korrekt erstellt werden,
     * während die Spielrunden des ersten Spieltags erhalten bleiben.
     */
    @Test
    public void testSpielrundeInZweitemSpieltag() throws GenerateException {
        new SpielrundeSheet_Naechste(wkingSpreadsheet).run();
        meldeListeNeuerSpieltag.naechsteSpieltag();
        testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(SpielTagNr.from(2));
        new SpielrundeSheet_Naechste(wkingSpreadsheet).run();

        assertThat(sheetHlp.findByName("1.1. Spielrunde"))
                .as("Spielrunde 1.1 muss erhalten bleiben")
                .isNotNull();
        assertThat(sheetHlp.findByName("2.1. Spielrunde"))
                .as("Spielrunde 2.1 muss für Spieltag 2 erstellt werden")
                .isNotNull();

        int anzRundenSpieltag1 = new SpielrundeSheet_Update(wkingSpreadsheet)
                .countNumberOfSpielRundenSheets(SpielTagNr.from(1));
        assertThat(anzRundenSpieltag1)
                .as("Spieltag 1 muss genau 1 Spielrunde haben")
                .isEqualTo(1);

        int anzRundenSpieltag2 = new SpielrundeSheet_Update(wkingSpreadsheet)
                .countNumberOfSpielRundenSheets(SpielTagNr.from(2));
        assertThat(anzRundenSpieltag2)
                .as("Spieltag 2 muss genau 1 Spielrunde haben")
                .isEqualTo(1);
    }

    /**
     * Spieltag 1 mit 2 Runden, Spieltag 2 mit 1 Runde:
     * prüft Rundenanzahl pro Spieltag und Existenz der Sheet-Namen.
     */
    @Test
    public void testMehrereRundenInZweitemSpieltag() throws GenerateException {
        SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
        spielrundeSheetNaechste.run(); // 1.1. Spielrunde
        spielrundeSheetNaechste.run(); // 1.2. Spielrunde

        meldeListeNeuerSpieltag.naechsteSpieltag();
        testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(SpielTagNr.from(2));
        spielrundeSheetNaechste.run(); // 2.1. Spielrunde

        int anzRundenSpieltag1 = new SpielrundeSheet_Update(wkingSpreadsheet)
                .countNumberOfSpielRundenSheets(SpielTagNr.from(1));
        assertThat(anzRundenSpieltag1)
                .as("Spieltag 1 muss 2 Spielrunden haben")
                .isEqualTo(2);

        int anzRundenSpieltag2 = new SpielrundeSheet_Update(wkingSpreadsheet)
                .countNumberOfSpielRundenSheets(SpielTagNr.from(2));
        assertThat(anzRundenSpieltag2)
                .as("Spieltag 2 muss 1 Spielrunde haben")
                .isEqualTo(1);

        assertThat(sheetHlp.findByName("1.2. Spielrunde"))
                .as("Spielrunde 1.2 muss existieren")
                .isNotNull();
        assertThat(sheetHlp.findByName("2.1. Spielrunde"))
                .as("Spielrunde 2.1 muss existieren")
                .isNotNull();
        assertThat(sheetHlp.findByName("2.2. Spielrunde"))
                .as("Spielrunde 2.2 darf nicht existieren")
                .isNull();
    }
}
