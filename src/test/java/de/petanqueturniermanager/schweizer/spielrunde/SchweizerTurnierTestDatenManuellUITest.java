package de.petanqueturniermanager.schweizer.spielrunde;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Manueller UITest für schrittweise Inspektion der Schweizer Turnierdaten.<br>
 * Nicht headless ausführen – LibreOffice-Fenster wird sichtbar.<br>
 * Nach der Generierung den Dialog schließen, um den Test zu beenden.
 */
@Disabled("Manueller Test – nur bei Bedarf starten, nicht headless ausführen")
public class SchweizerTurnierTestDatenManuellUITest extends BaseCalcUITest {

	private SchweizerTurnierTestDaten testDaten;

	@BeforeEach
	public void setup() {
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet);
	}

	@Test
	public void testStufe1NurMeldeliste() throws GenerateException {
		testDaten.generate(0, false);
		warten("Stufe 1: Nur Meldeliste");
	}

	@Test
	public void testStufe2MeldelisteUndEineSpielrunde() throws GenerateException {
		testDaten.generate(1, false);
		warten("Stufe 2: Meldeliste + 1 Spielrunde");
	}

	@Test
	public void testStufe3VollstaendigesTurnier() throws GenerateException {
		testDaten.generate(); // 3 Runden + Rangliste
		warten("Stufe 3: Vollständiges Turnier");
	}
}
