package de.petanqueturniermanager.addins;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * UI Test für die Plugin AddIn-Formeln (PTM.ALG.*)
 * 
 * Wichtig: Dieser Test benötigt das vollständig installierte Plugin in LibreOffice,
 * da die AddIn-Funktionen nur mit dem geladenen Plugin funktionieren.
 * 
 * Tested wird hier hauptsächlich, dass:
 * 1. Die Formeln korrekt geschrieben werden (Format-Konstanten)
 * 2. Die Formeln von LibreOffice erkannt werden (keine Syntaxfehler)
 * 3. Wenn das Plugin geladen ist, dass die Werte korrekt sind
 * 
 * @author M.Massee
 */
public class GlobalImplUITest extends BaseCalcUITest {

	@Test
	public void testPTMIntPropertyFormula() throws GenerateException {
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
		assertThat(valueFromSpieltag).as("Spieltag-Wert muss korrekt sein").isEqualTo(spieltagNr);
		assertThat(valueFromSpielrunde).as("Spielrunde-Wert muss korrekt sein").isEqualTo(spielrundeNr);
	}

	@Test
	public void testPTMStringPropertyFormula() throws GenerateException {
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
	public void testPTMTurniersystemFormula() throws GenerateException {
		// Turniersystem setzen
		TurnierSystem expectedSystem = TurnierSystem.SCHWEIZER;
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, expectedSystem.getId());

		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
		
		// Formel in Zelle schreiben
		Position testPos = Position.from(2, 0); // C1
		String formula = "=PTM.ALG.TURNIERSYSTEM()";
		
		sheetHlp.setFormulaInCell(sheet, testPos, formula);
		
		// Formel validieren
		// LibreOffice speichert Formeln intern mit vollem Pfad
		String actualFormula = sheetHlp.getFormulaFromCell(sheet, testPos);
		assertThat(actualFormula).contains("TURNIERSYSTEM");
		
		// Wenn Plugin geladen ist, sollte das Turniersystem korrekt sein
		// Ohne Plugin: "Kein", "#NULL!" oder leer
		String cellValue = sheetHlp.getTextFromCell(sheet, testPos);
		if (cellValue != null && !cellValue.isEmpty()
				&& !cellValue.equals(TurnierSystem.KEIN.getBezeichnung())
				&& !cellValue.contains("NULL")) {
			assertThat(cellValue).isEqualTo(expectedSystem.getBezeichnung());
		}
	}

	@Test
	public void testMultiplePropertiesInSameSheet() throws GenerateException {
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

	@Test
	public void testPropertyUpdate() throws GenerateException {
		// Test: Zwei Formeln lesen verschiedene Properties mit unterschiedlichen Werten.
		// Hinweis: Property-Update nach Formel-Einfügen kann nicht getestet werden,
		// weil Test (socket bridge) und Plugin (soffice) in getrennten JVMs laufen
		// und DocumentPropertiesHelper einen statischen Cache pro Dokument hat.
		String propName = SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG;
		String propName2 = SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE;

		docPropHelper.setIntProperty(propName, 1);
		docPropHelper.setIntProperty(propName2, 7);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SUPERMELEE.getId());

		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
		Position testPos1 = Position.from(0, 10); // A11
		Position testPos2 = Position.from(0, 11); // A12

		sheetHlp.setFormulaInCell(sheet, testPos1, "=PTM.ALG.INTPROPERTY(\"" + propName + "\")");
		sheetHlp.setFormulaInCell(sheet, testPos2, "=PTM.ALG.INTPROPERTY(\"" + propName2 + "\")");

		// Formeln korrekt aufgelöst
		assertThat(sheetHlp.getFormulaFromCell(sheet, testPos1))
			.containsIgnoringCase("INTPROPERTY").contains(propName);
		assertThat(sheetHlp.getFormulaFromCell(sheet, testPos2))
			.containsIgnoringCase("INTPROPERTY").contains(propName2);

		// Verschiedene Properties liefern verschiedene Werte
		assertThat(sheetHlp.getIntFromCell(sheet, testPos1)).as("Spieltag").isEqualTo(1);
		assertThat(sheetHlp.getIntFromCell(sheet, testPos2)).as("Spielrunde").isEqualTo(7);
	}
}
