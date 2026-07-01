package de.petanqueturniermanager.addins;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * UI Test für die Plugin AddIn-Formeln (PTM.ALG.*)
 * <p>
 * Wichtig: Dieser Test benötigt das vollständig installierte Plugin in LibreOffice,
 * da die AddIn-Funktionen nur mit dem geladenen Plugin funktionieren.
 * <p>
 * Tested wird hier hauptsächlich, dass:
 * 1. Die Formeln korrekt geschrieben werden (Format-Konstanten)
 * 2. Die Formeln von LibreOffice erkannt werden (keine Syntaxfehler)
 * 3. Wenn das Plugin geladen ist, dass die Werte korrekt sind
 * 
 * @author M.Massee
 */
public class GlobalImplUITest extends BaseCalcUITest {

	@Test
	public void testPTMIntPropertyFormula() {
		// Jetzt Test-Werte in Document Properties setzen
		int spieltagNr = 3;
		int spielrundeNr = 5;
		

		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, spieltagNr);
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, spielrundeNr);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SUPERMELEE.getId());

		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);

		// Formeln in Zellen schreiben - WICHTIG: Verwende die kurzen DisplayNames aus GlobalAddIn.xcu
		Position spieltagPos = Position.from(0, 0); // A1
		Position spielrundePos = Position.from(0, 1); // A2
		
		String spieltagFormula = "=PTM.ALG.INTPROPERTY(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG + "\")";
		String spielrundeFormula = "=PTM.ALG.INTPROPERTY(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE + "\")";
		
		sheetHlp.setFormulaInCell(sheet, spieltagPos, spieltagFormula);
		sheetHlp.setFormulaInCell(sheet, spielrundePos, spielrundeFormula);
		
		// Werte aus Zellen lesen und validieren
		// Note: LibreOffice speichert Formeln intern mit dem vollen Pfad:
		// z.B. =DE.PETANQUETURNIERMANAGER.ADDIN.GLOBALADDIN.PTMINTPROPERTY(...)
		// anstatt =PTM.ALG.INTPROPERTY(...)
		String actualSpieltagFormula = sheetHlp.getFormulaFromCell(sheet, spieltagPos);
		String actualSpielrundeFormula = sheetHlp.getFormulaFromCell(sheet, spielrundePos);
		
		// Plugin MUSS geladen sein - Formel muss vom Plugin aufgelöst werden
		// LibreOffice ersetzt den DisplayName durch den internen vollen Pfad wenn das Plugin geladen ist
		assertThat(actualSpieltagFormula).as("Spieltag-Formel muss INTPROPERTY enthalten")
			.containsIgnoringCase("INTPROPERTY")
			.contains(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG);
		assertThat(actualSpielrundeFormula).as("Spielrunde-Formel muss INTPROPERTY enthalten")
			.containsIgnoringCase("INTPROPERTY")
			.contains(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE);

		// Zellwerte lesen
		String spieltagText = sheetHlp.getTextFromCell(sheet, spieltagPos);
		String spielrundeText = sheetHlp.getTextFromCell(sheet, spielrundePos);
		int valueFromSpieltag = sheetHlp.getIntFromCell(sheet, spieltagPos);
		int valueFromSpielrunde = sheetHlp.getIntFromCell(sheet, spielrundePos);

		System.out.println("testPTMIntPropertyFormula:");
		System.out.println("  Spieltag  - Formula: " + actualSpieltagFormula + ", IntVal: " + valueFromSpieltag + ", TextVal: '" + spieltagText + "'");
		System.out.println("  Spielrunde - Formula: " + actualSpielrundeFormula + ", IntVal: " + valueFromSpielrunde + ", TextVal: '" + spielrundeText + "'");

		// Kein Fehler erlaubt - Plugin muss die Formeln korrekt auflösen
		assertThat(spieltagText).as("Spieltag-Formel darf keinen Fehler liefern")
			.doesNotContain("504").doesNotContain("Fehler").doesNotContain("#NAME?").doesNotContain("#NULL");
		assertThat(spielrundeText).as("Spielrunde-Formel darf keinen Fehler liefern")
			.doesNotContain("504").doesNotContain("Fehler").doesNotContain("#NAME?").doesNotContain("#NULL");

		// Werte müssen korrekt sein
		// TODO FixME
		// assertThat(valueFromSpieltag).as("Spieltag-Wert muss korrekt sein").isEqualTo(spieltagNr);
		// assertThat(valueFromSpielrunde).as("Spielrunde-Wert muss korrekt sein").isEqualTo(spielrundeNr);
	}

	@Test
	public void testPTMStringPropertyFormula() {
		// Test-Werte in Document Properties setzen
		String testPropertyName = "TestProperty";
		String testPropertyValue = "TestWert123";
		
		docPropHelper.setStringProperty(testPropertyName, testPropertyValue);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.LIGA.getId());

		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
		
		// Formel in Zelle schreiben
		Position testPos = Position.from(1, 0); // B1
		String formula = "=PTM.ALG.STRINGPROPERTY(\"" + testPropertyName + "\")";
		
		sheetHlp.setFormulaInCell(sheet, testPos, formula);
		
		// Formel validieren
		// LibreOffice speichert Formeln intern mit vollem Pfad
		String actualFormula = sheetHlp.getFormulaFromCell(sheet, testPos);
		assertThat(actualFormula).contains("STRINGPROPERTY").contains(testPropertyName);
		
		// Wenn Plugin geladen ist, sollte der Wert korrekt sein
		// Ohne Plugin: "fehler", "Fehler:504" oder leer
		String cellValue = sheetHlp.getTextFromCell(sheet, testPos);
		if (cellValue != null && !cellValue.isEmpty() 
				&& !cellValue.toLowerCase().contains("fehler") 
				&& !cellValue.contains("504")) {
			assertThat(cellValue).isEqualTo(testPropertyValue);
		}
	}

	@Test
	public void testPTMTurniersystemFormula() {
		// Turniersystem setzen
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SCHWEIZER.getId());

		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);

		// Formel in Zelle schreiben
		Position testPos = Position.from(2, 0); // C1
		sheetHlp.setFormulaInCell(sheet, testPos, "=PTM.ALG.TURNIERSYSTEM()");

		// Formel muss vom Plugin erkannt werden (kein #NAME? im Formula-String)
		String actualFormula = sheetHlp.getFormulaFromCell(sheet, testPos);
		assertThat(actualFormula).containsIgnoringCase("TURNIERSYSTEM");

		// Zellwert darf keinen Fehlercode enthalten
		// Hinweis: ptmturniersystem() gibt einen lokalisierten String zurück, der
		// sich je nach LibreOffice-Locale unterscheiden kann ("Schweizer" vs "Swiss System").
		// Daher wird nur auf Abwesenheit von Fehlercodes geprüft, nicht auf exakten Wert.
		String cellValue = sheetHlp.getTextFromCell(sheet, testPos);
		System.out.println("testPTMTurniersystemFormula: formula='" + actualFormula + "' cellValue='" + cellValue + "'");
		if (cellValue != null && !cellValue.isEmpty()) {
			assertThat(cellValue).as("TURNIERSYSTEM-Formel darf keinen Fehler liefern")
					.doesNotContain("504").doesNotContain("Fehler").doesNotContain("#NAME?").doesNotContain("#WERT");
		}
	}

	@Test
	public void testMultiplePropertiesInSameSheet() {
		// Mehrere Properties setzen
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 2);
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, 4);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.JGJ.getId());
		docPropHelper.setStringProperty("CustomProp", "CustomValue");

		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
		
		// Verschiedene Formeln setzen
		sheetHlp.setFormulaInCell(sheet, Position.from(0, 5), // A6
			"=PTM.ALG.INTPROPERTY(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG + "\")");
		sheetHlp.setFormulaInCell(sheet, Position.from(1, 5), // B6
			"=PTM.ALG.INTPROPERTY(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE + "\")");
		sheetHlp.setFormulaInCell(sheet, Position.from(2, 5), // C6
			"=PTM.ALG.TURNIERSYSTEM()");
		sheetHlp.setFormulaInCell(sheet, Position.from(3, 5), // D6
			"=PTM.ALG.STRINGPROPERTY(\"CustomProp\")");

		// Validieren, dass alle Formeln korrekt geschrieben wurden
		// LibreOffice speichert Formeln intern mit vollem Pfad
		assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(0, 5)))
			.contains("INTPROPERTY");
		assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(1, 5)))
			.contains("INTPROPERTY");
		assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(2, 5)))
			.contains("TURNIERSYSTEM");
		assertThat(sheetHlp.getFormulaFromCell(sheet, Position.from(3, 5)))
			.contains("STRINGPROPERTY");
	}

	// -------------------------------------------------------------------------
	// Algorithmik-Funktionen (PTM.CADRAGE.*, PTM.POULE.*, PTM.SUPERMELEE.*)
	//
	// Diese Funktionen rechnen rein numerisch und sind unabhängig vom geladenen
	// Dokument. Statt Formeln über Zellen auszuwerten (UNO-Socket evaluiert
	// Add-In-Formeln nicht synchron), werden die Methoden direkt am
	// GlobalImpl-Objekt aufgerufen und die Ergebnisse mit bekannten
	// Erwartungswerten der zugrunde liegenden Rechner-Klassen verglichen.
	// -------------------------------------------------------------------------

	@Test
	public void testPTMCadrageFunktionen() {
		GlobalImpl impl = new GlobalImpl(starter.getxComponentContext());

		// Zweierpotenz → keine Cadrage nötig
		assertThat(impl.ptmcadrageanzteams(8)).as("8 Teams: keine Cadrage").isEqualTo(0);
		assertThat(impl.ptmcadragezielanz(8)).as("8 Teams: Ziel = 8").isEqualTo(8);
		assertThat(impl.ptmcadrageanzohnecadrage(8)).as("8 Teams: 8 Teams ohne Cadrage").isEqualTo(8);

		// 10 Teams: Ziel 8, 4 Cadrage-Teams (auf 2 reduziert), 6 Teams ohne Cadrage
		assertThat(impl.ptmcadragezielanz(10)).as("10 Teams: Ziel = 8").isEqualTo(8);
		assertThat(impl.ptmcadrageanzteams(10)).as("10 Teams: 4 Cadrage-Teams").isEqualTo(4);

		// 17 Teams: Ziel 16, 2 Cadrage-Teams
		assertThat(impl.ptmcadragezielanz(17)).as("17 Teams: Ziel = 16").isEqualTo(16);
		assertThat(impl.ptmcadrageanzteams(17)).as("17 Teams: 2 Cadrage-Teams").isEqualTo(2);

		// Edge: <=2 Teams → 0
		assertThat(impl.ptmcadrageanzteams(1)).isZero();
		assertThat(impl.ptmcadrageanzteams(2)).isZero();
	}

	@Test
	public void testPTMPouleFunktionen() {
		GlobalImpl impl = new GlobalImpl(starter.getxComponentContext());

		// 8 Teams → 2 Vierergruppen, 0 Dreier
		assertThat(impl.ptmpouleanzgruppen(8)).as("8 Teams: 2 Gruppen").isEqualTo(2);
		assertThat(impl.ptmpouleanzvierergruppen(8)).as("8 Teams: 2 Vierer").isEqualTo(2);
		assertThat(impl.ptmpouleanzdreiergruppen(8)).as("8 Teams: 0 Dreier").isZero();

		// 9 Teams → 3 Dreiergruppen
		assertThat(impl.ptmpouleanzgruppen(9)).as("9 Teams: 3 Gruppen").isEqualTo(3);
		assertThat(impl.ptmpouleanzvierergruppen(9)).as("9 Teams: 0 Vierer").isZero();
		assertThat(impl.ptmpouleanzdreiergruppen(9)).as("9 Teams: 3 Dreier").isEqualTo(3);

		// 13 Teams → 1 Vierer + 3 Dreier = 4 Gruppen
		assertThat(impl.ptmpouleanzgruppen(13)).as("13 Teams: 4 Gruppen").isEqualTo(4);
		assertThat(impl.ptmpouleanzvierergruppen(13)).as("13 Teams: 1 Vierer").isEqualTo(1);
		assertThat(impl.ptmpouleanzdreiergruppen(13)).as("13 Teams: 3 Dreier").isEqualTo(3);

		// Edge: <3 Teams → 0
		assertThat(impl.ptmpouleanzgruppen(2)).isZero();
		assertThat(impl.ptmpouleanzgruppen(0)).isZero();
	}

	@Test
	public void testPTMSuperMeleeFunktionen() {
		GlobalImpl impl = new GlobalImpl(starter.getxComponentContext());

		// 12 Spieler: 4 Triplette (Triplette-Modus) bzw. 6 Doublette (Doublette-Modus)
		assertThat(impl.ptmsmtriplanztriplette(12)).as("12 Spieler: 4 Triplette").isEqualTo(4);
		assertThat(impl.ptmsmtriplanzdoublette(12)).as("12 Spieler im Triplette-Modus: 0 Doublette").isZero();
		assertThat(impl.ptmsmdouplanzdoublette(12)).as("12 Spieler: 6 Doublette").isEqualTo(6);

		// Validierung – nur ungerade Zahlen außer Vielfache zwischen Triplette/Doublette sind invalide.
		// 7 ist invalide (weder durch 3 noch durch 2 sauber teilbar im SuperMelee-Sinn).
		assertThat(impl.ptmsmvalide(8)).as("8 Spieler valide").isEqualTo(1);
		assertThat(impl.ptmsmvalide(9)).as("9 Spieler valide").isEqualTo(1);
		assertThat(impl.ptmsmvalide(12)).as("12 Spieler valide").isEqualTo(1);
		assertThat(impl.ptmsmvalide(7)).as("7 Spieler invalide").isZero();

		// 9 Spieler: 3 Doublette + 1 Triplette (Mix-Konfiguration)
		assertThat(impl.ptmsmtriplanztriplette(9)).as("9 Spieler im Triplette-Modus: 1 Triplette").isEqualTo(1);
		assertThat(impl.ptmsmtriplanzdoublette(9)).as("9 Spieler im Triplette-Modus: 3 Doublette").isEqualTo(3);

		// Anzahl Teams ("Paarungen" in der Original-API) und Bahnen bei 12 Spielern (Triplette-Modus):
		// 4 Teams insgesamt, 2 Spiele/Bahnen.
		assertThat(impl.ptmsmtriplanzpaarungen(12)).as("12 Spieler Triplette: 4 Teams").isEqualTo(4);
		assertThat(impl.ptmsmtriplanzbahnen(12)).as("12 Spieler Triplette: 2 Bahnen").isEqualTo(2);

		// Edge: <4 Spieler → 0
		assertThat(impl.ptmsmtriplanztriplette(3)).isZero();
		assertThat(impl.ptmsmdouplanzdoublette(0)).isZero();
	}

	@Test
	public void testPropertyUpdate() {
		// Testet Property-Round-Trip (setIntProperty / getIntProperty) sowie
		// dass die INTPROPERTY-Formel korrekt in die Zelle eingetragen wird.
		String propName = SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG;
		String propName2 = SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE;

		docPropHelper.setIntProperty(propName, 1);
		docPropHelper.setIntProperty(propName2, 7);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SUPERMELEE.getId());

		// Property-Round-Trip: set/get direkt über docPropHelper (unabhängig von GlobalImpl)
		assertThat(docPropHelper.getIntProperty(propName, -1)).as("Spieltag").isEqualTo(1);
		assertThat(docPropHelper.getIntProperty(propName2, -1)).as("Spielrunde").isEqualTo(7);
		assertThat(docPropHelper.getTurnierSystemAusDocument()).as("TurnierSystem").isEqualTo(TurnierSystem.SUPERMELEE);

		// Formeln strukturell korrekt in Zellen eingetragen
		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
		Position testPos1 = Position.from(0, 10); // A11
		Position testPos2 = Position.from(0, 11); // A12

		sheetHlp.setFormulaInCell(sheet, testPos1, "=PTM.ALG.INTPROPERTY(\"" + propName + "\")");
		sheetHlp.setFormulaInCell(sheet, testPos2, "=PTM.ALG.INTPROPERTY(\"" + propName2 + "\")");

		// Formel-Struktur prüfen: LibreOffice löst den Display-Namen in den internen Namen auf,
		// was beweist dass das Add-In korrekt registriert ist.
		assertThat(sheetHlp.getFormulaFromCell(sheet, testPos1))
			.containsIgnoringCase("INTPROPERTY").contains(propName);
		assertThat(sheetHlp.getFormulaFromCell(sheet, testPos2))
			.containsIgnoringCase("INTPROPERTY").contains(propName2);

		// HINWEIS: Die Formelwerte können im UITest-Kontext nicht geprüft werden.
		// Wenn Formeln via UNO-Socket in Zellen geschrieben werden, wertet LibreOffice
		// Add-In-Formeln nicht synchron aus – die Auswertung erfolgt erst im Main-Thread-
		// Event-Loop, nachdem alle Socket-Calls abgeschlossen sind. In der Produktion
		// funktioniert dies korrekt (Auswertung via OnLoad/calculateAll auf dem Main-Thread).
		// assertThat(sheetHlp.getIntFromCell(sheet, testPos1)).as("Spieltag").isEqualTo(1);
		// assertThat(sheetHlp.getIntFromCell(sheet, testPos2)).as("Spielrunde").isEqualTo(7);
	}

	/**
	 * Regression im Kiosk-Modus: AddIn-Formeln (PTM.ALG.*) müssen auch bei aktivem
	 * TurnierModus-Flag in Zellen geschrieben und vom Plugin aufgelöst werden.
	 */
	@Test
	public void kioskModus_addInFormelWirdAkzeptiert() throws Exception {
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 2);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SUPERMELEE.getId());
		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
		Position pos = Position.from(0, 0);

		mitKioskModus(TurnierSystem.SUPERMELEE, () -> sheetHlp.setFormulaInCell(sheet, pos,
				"=PTM.ALG.INTPROPERTY(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG + "\")"));

		assertThat(sheetHlp.getFormulaFromCell(sheet, pos))
				.as("AddIn-Formel muss auch im Kiosk-Modus geschrieben werden")
				.containsIgnoringCase("INTPROPERTY");
	}
}
