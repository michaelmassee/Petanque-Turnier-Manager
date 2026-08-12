package de.petanqueturniermanager.kaskade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;

/**
 * Regression: Dateien, die mit einer Kaskade-Version VOR Einführung der Plan-Fixierung
 * ({@code KaskadeKonfigurationSheet#getAnzahlTeamsRunde1()}, siehe {@link KaskadeAusstiegMittenImTurnierUITest})
 * gespeichert wurden, haben diese Property nicht gesetzt, obwohl Runde 1 bereits gespielt wurde
 * (Property-Default ist 0). Ohne Fallback würde die nächste Rundenerzeugung mit gesamtTeams=0 gegen
 * {@code KaskadenKoRundenPlaner.berechne()} laufen und dort mit {@code IllegalArgumentException}
 * abbrechen ({@code checkArgument(gesamtTeams >= 2, ...)}). Dieser Test simuliert diesen Altzustand
 * und prüft, dass {@code KaskadePlanGroesse} die Teamanzahl stattdessen aus dem bereits geschriebenen
 * Runde-1-Sheet rekonstruiert, statt abzubrechen.
 */
public class KaskadeAltdateiMigrationUITest extends BaseCalcUITest {

    private static final int ANZ_TEAMS = 12;
    private static final int ANZ_KASKADEN = 2;

    private KaskadeTurnierTestDaten testDaten;

    @BeforeEach
    public void setup() {
        testDaten = new KaskadeTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, ANZ_KASKADEN);
    }

    @Test
    public void altdateiOhneAnzahlTeamsRunde1Property_wirdAusRunde1SheetRekonstruiert() throws GenerateException {
        // Runde 1 normal spielen (setzt aktuell automatisch die Property - für den Test simulieren
        // wir gleich danach den Altzustand, in dem das noch nicht passierte).
        testDaten.generate(1, false);

        KaskadeKonfigurationSheet konfig = new KaskadeKonfigurationSheet(wkingSpreadsheet);
        assertThat(konfig.getAnzahlTeamsRunde1()).as("Vorbedingung: Property nach Runde 1 gesetzt")
                .isEqualTo(ANZ_TEAMS);

        // Altdatei simulieren: Property zurücksetzen, als wäre sie nie gesetzt worden (Datei aus
        // einer Version vor der Plan-Fixierung).
        konfig.setAnzahlTeamsRunde1(0);

        // Runde 2 erzeugen darf NICHT mit IllegalArgumentException abbrechen, sondern muss die
        // Teamanzahl aus dem bereits vorhandenen Runde-1-Sheet rekonstruieren.
        testDaten.spielrundeSheet.setForceOk(true);
        assertThatCode(() -> testDaten.spielrundeSheet.naechsteRunde()).doesNotThrowAnyException();

        XSpreadsheet runde2Sheet = sheetHlp.findByName(SheetNamen.kaskadenRunde(2));
        assertThat(runde2Sheet).as("Runde 2 muss trotz fehlender Property erzeugt werden").isNotNull();

        // Rekonstruktion muss die Property für Folgeaufrufe persistiert haben.
        assertThat(konfig.getAnzahlTeamsRunde1())
                .as("Rekonstruierte Teamanzahl muss für Folgerunden persistiert werden")
                .isEqualTo(ANZ_TEAMS);
    }
}
