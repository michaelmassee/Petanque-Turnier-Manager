package de.petanqueturniermanager.addins;

import org.junit.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Debug Test für AddIn-Formeln mit Parametern
 */
public class GlobalImplDebugUITest extends BaseCalcUITest {

	@Test
	public void testCompareIntAndStringProperty() throws GenerateException {
		// Properties setzen
		String testPropName = "TestProp";
		String testStringValue = "TestString";
		int testIntValue = 42;
		
		docPropHelper.setStringProperty(testPropName, testStringValue);
		docPropHelper.setIntProperty(testPropName + "Int", testIntValue);
		docPropHelper.setIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, testIntValue);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SUPERMELEE.getId());

		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
		
		// Test 1: TURNIERSYSTEM (keine Parameter) - sollte funktionieren
		Position pos1 = Position.from(0, 0); // A1
		sheetHlp.setFormulaInCell(sheet, pos1, "=PTM.ALG.TURNIERSYSTEM()");
		
		// Test 2: STRINGPROPERTY (String parameter) - sollte funktionieren
		Position pos2 = Position.from(0, 1); // A2
		sheetHlp.setFormulaInCell(sheet, pos2, "=PTM.ALG.STRINGPROPERTY(\"" + testPropName + "\")");
		
		// Test 3: INTPROPERTY (String parameter) - FEHLT mit 504!
		Position pos3 = Position.from(0, 2); // A3
		sheetHlp.setFormulaInCell(sheet, pos3, "=PTM.ALG.INTPROPERTY(\"" + SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG + "\")");
		
		// Results lesen
		String result1 = sheetHlp.getTextFromCell(sheet, pos1);
		String result2 = sheetHlp.getTextFromCell(sheet, pos2);
		String result3 = sheetHlp.getTextFromCell(sheet, pos3);
		
		System.out.println("\n========== ADDIN DEBUG COMPARISON ==========");
		System.out.println("1. TURNIERSYSTEM() -> '" + result1 + "'");
		System.out.println("2. STRINGPROPERTY(" + testPropName + ") -> '" + result2 + "'");
		System.out.println("3. INTPROPERTY(Spieltag) -> '" + result3 + "'");
		System.out.println("==========================================\n");
		
		// Formeln lesen
		String formula1 = sheetHlp.getFormulaFromCell(sheet, pos1);
		String formula2 = sheetHlp.getFormulaFromCell(sheet, pos2);
		String formula3 = sheetHlp.getFormulaFromCell(sheet, pos3);
		
		System.out.println("EXPANDED FORMULAS:");
		System.out.println("1. " + formula1);
		System.out.println("2. " + formula2);
		System.out.println("3. " + formula3);
		System.out.println("==========================================\n");
	}
}
