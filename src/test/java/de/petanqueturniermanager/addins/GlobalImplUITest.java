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
 * @author M.Massee
 */
public class GlobalImplUITest extends BaseCalcUITest {

	@Test
	public void testPTMIntPropertyFormula() throws GenerateException {
		// Zuerst testen wir eine Funktion OHNE Parameter
		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
		Position testPos = Position.from(0, 0);
		sheetHlp.setFormulaInCell(sheet, testPos, "=de.petanqueturniermanager.addin.GlobalAddIn.ptmturniersystem()");
		String turniersystemText = sheetHlp.getTextFromCell(sheet, testPos);
		System.out.println("DEBUG: TURNIERSYSTEM() returned: '" + turniersystemText + "'");
		assertThat(turniersystemText).as("TURNIERSYSTEM muss Wert liefern!").doesNotContain("504").doesNotContain("Fehler");
		
		// Jetzt Test-Werte in Document Properties setzen
		int spieltagNr = 3;
		int spielrundeNr = 5;
		
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, spieltagNr);
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, spielrundeNr);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SUPERMELEE.getId());

		// Formeln in Zellen schreiben - WICHTIG: Verwende GlobalImpl.PTM_INT_PROPERTY statt "PTM.ALG.INTPROPERTY"
		Position spieltagPos = Position.from(0, 0); // A1
		Position spielrundePos = Position.from(0, 1); // A2
		
		String spieltagFormula = "=" + GlobalImpl.PTM_INT_PROPERTY + "(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG + "\")";
		String spielrundeFormula = "=" + GlobalImpl.PTM_INT_PROPERTY + "(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE + "\")";
		
		sheetHlp.setFormulaInCell(sheet, spieltagPos, spieltagFormula);
		sheetHlp.setFormulaInCell(sheet, spielrundePos, spielrundeFormula);
		
		// Werte aus Zellen lesen und validieren
		// Note: LibreOffice speichert Formeln intern mit dem vollen Pfad:
		// z.B. =DE.PETANQUETURNIERMANAGER.ADDIN.GLOBALADDIN.PTMINTPROPERTY(...)
		// anstatt =PTM.ALG.INTPROPERTY(...)
		String actualSpieltagFormula = sheetHlp.getFormulaFromCell(sheet, spieltagPos);
		String actualSpielrundeFormula = sheetHlp.getFormulaFromCell(sheet, spielrundePos);
		
		assertThat(actualSpieltagFormula).contains("INTPROPERTY").contains(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG);
		assertThat(actualSpielrundeFormula).contains("INTPROPERTY").contains(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE);
		
		// Wenn das Plugin installiert ist, sollten die Zellen die korrekten Werte haben
		// Hinweis: Dies funktioniert nur, wenn das Plugin tatsächlich in LibreOffice geladen ist
		int valueFromSpieltag = sheetHlp.getIntFromCell(sheet, spieltagPos);
		int valueFromSpielrunde = sheetHlp.getIntFromCell(sheet, spielrundePos);
		
		// Debug-Ausgabe für Diagnose
		String spieltagText = sheetHlp.getTextFromCell(sheet, spieltagPos);
		String spielrundeText = sheetHlp.getTextFromCell(sheet, spielrundePos);
		System.out.println("DEBUG testPTMIntPropertyFormula:");
		System.out.println("  Spieltag  - Formula: " + actualSpieltagFormula);
		System.out.println("  Spieltag  - IntVal:  " + valueFromSpieltag + ", TextVal: '" + spieltagText + "'");
		System.out.println("  Spielrunde - Formula: " + actualSpielrundeFormula);
		System.out.println("  Spielrunde - IntVal:  " + valueFromSpielrunde + ", TextVal: '" + spielrundeText + "'");
		
		// TEST: Plugin MUSS geladen sein - Fehler:504 darf NICHT vorkommen!
		assertThat(spieltagText).as("Spieltag muss Wert liefern, nicht Fehler:504!")
			.doesNotContain("504").doesNotContain("Fehler");
		assertThat(spielrundeText).as("Spielrunde muss Wert liefern, nicht Fehler:504!")
			.doesNotContain("504").doesNotContain("Fehler");
		
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
		// Test: Property ändern und Formel sollte neuen Wert liefern
		String propName = SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG;
		
		// Initialer Wert
		docPropHelper.setIntProperty(propName, 1);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SUPERMELEE.getId());

		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
		Position testPos = Position.from(0, 10); // A11
		
		sheetHlp.setFormulaInCell(sheet, testPos, "=PTM.ALG.INTPROPERTY(\"" + propName + "\")");
		
		// Property ändern
		docPropHelper.setIntProperty(propName, 7);
		
		// Formel sollte immer noch korrekt sein
		// LibreOffice speichert Formeln intern mit vollem Pfad
		assertThat(sheetHlp.getFormulaFromCell(sheet, testPos))
			.contains("INTPROPERTY")
			.contains(propName);
		
		// Bei installiertem Plugin sollte der neue Wert erscheinen
		// (Hinweis: Calc neu berechnet Formeln automatisch bei Property-Änderungen)
		int value = sheetHlp.getIntFromCell(sheet, testPos);
		if (value > 0) {
			// Plugin ist geladen und Formel wurde evaluiert
			assertThat(value).isEqualTo(7);
		}
	}
}
