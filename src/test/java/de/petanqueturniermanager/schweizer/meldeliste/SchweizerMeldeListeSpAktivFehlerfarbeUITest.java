/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.ValidationType;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;

/**
 * Regressionstest: die Fehlerfarben-CF der Setzpositions- und Aktiv-Spalte darf nach dem
 * vollständigen Sheetaufbau nicht verschwunden sein.
 * <p>
 * {@link de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper#anwenden} liest/
 * schreibt danach noch die "editierbareFelderHervorheben"-Zebra-CF über den gesamten Bereich
 * Teamname..Aktiv als EIN mehrspaltiger UNO-Range-Zugriff. Da SP und Aktiv zu diesem Zeitpunkt
 * bereits eine eigene (andere) Fehlerprüfung tragen, ist dieser Bereich nicht homogen - LO liefert
 * dann beim Lesen von "ConditionalFormat" nur den (leeren) Zustand der ersten Spalte und
 * überschreibt beim Zurückschreiben alle Spalten gleich, wodurch die SP-/Aktiv-Fehlerprüfung
 * stillschweigend verloren ging (siehe Bugreport mit angehängter bug-red.ods).
 */
class SchweizerMeldeListeSpAktivFehlerfarbeUITest extends BaseCalcUITest {

	@Test
	void spSpalteBehaeltFehlerfarbeNachVollemSheetaufbau() throws Exception {
		SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.DOUBLETTE, true, false);

		int ersteDatenZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
		int spSpalte = meldeListeNew.getSetzPositionSpalte();
		XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();

		assertThat(alleConditionalFormatFormeln(xSheet, Position.from(spSpalte, ersteDatenZeile)))
				.as("SP-Spalte muss nach dem vollständigen Sheetaufbau weiterhin eine "
						+ "ISBLANK/ISNUMBER-Fehlerprüfung als bedingte Formatierung haben")
				.anySatisfy(formel -> assertThat(formel).containsIgnoringCase("ISBLANK"));
	}

	@Test
	void spSpalteHatNativeNichtnegativeGanzzahlDatengueltigkeit() throws Exception {
		SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.DOUBLETTE, true, false);

		XCell xCell = meldeListeNew.getXSpreadSheet().getCellByPosition(meldeListeNew.getSetzPositionSpalte(),
				SchweizerListeDelegate.ERSTE_DATEN_ZEILE);
		XPropertySet cellProperties = Lo.qi(XPropertySet.class, xCell);
		XPropertySet validation = Lo.qi(XPropertySet.class, cellProperties.getPropertyValue("Validation"));

		assertThat(validation.getPropertyValue("Type")).isEqualTo(ValidationType.WHOLE);
		assertThat(validation.getPropertyValue("IgnoreBlankCells")).isEqualTo(Boolean.TRUE);
		assertThat(Lo.qi(XSheetCondition.class, validation).getFormula1()).isEqualTo("0");
	}

	@Test
	void aktivSpalteBehaeltFehlerfarbeNachVollemSheetaufbau() throws Exception {
		SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.DOUBLETTE, true, false);

		int ersteDatenZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
		int aktivSpalte = meldeListeNew.getAktivSpalte();
		XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();

		assertThat(alleConditionalFormatFormeln(xSheet, Position.from(aktivSpalte, ersteDatenZeile)))
				.as("Aktiv-Spalte muss nach dem vollständigen Sheetaufbau weiterhin eine "
						+ "ISBLANK-Fehlerprüfung als bedingte Formatierung haben")
				.anySatisfy(formel -> assertThat(formel).containsIgnoringCase("ISBLANK"));
	}

	@Test
	void aktivSpalteHatNativeDatengueltigkeitMitTeilnahmeUndAusstieg() throws Exception {
		SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.DOUBLETTE, true, false);

		XSpreadsheet sheet = meldeListeNew.getXSpreadSheet();
		XCell xCell = sheet.getCellByPosition(meldeListeNew.getAktivSpalte(), SchweizerListeDelegate.ERSTE_DATEN_ZEILE);
		XPropertySet cellProperties = Lo.qi(XPropertySet.class, xCell);
		XPropertySet validation = Lo.qi(XPropertySet.class, cellProperties.getPropertyValue("Validation"));

		assertThat(validation.getPropertyValue("Type")).isEqualTo(ValidationType.LIST);
		assertThat(validation.getPropertyValue("IgnoreBlankCells")).isEqualTo(Boolean.TRUE);
		assertThat(validation.getPropertyValue("ShowInputMessage")).isEqualTo(Boolean.TRUE);
		assertThat((String) validation.getPropertyValue("InputMessage")).contains("1 = Nimmt teil", "2 =");
		assertThat(validation.getPropertyValue("ShowErrorMessage")).isEqualTo(Boolean.TRUE);
		assertThat((String) validation.getPropertyValue("ErrorMessage")).contains("1, 2");
		assertThat(Lo.qi(XSheetCondition.class, validation).getFormula1()).isEqualTo("{1;2}");
	}

	@Test
	void namensfelderBehaltenEditierfarbeNebenDerDuplikatPruefung() throws Exception {
		SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.DOUBLETTE, true, false);

		var formeln = alleConditionalFormatFormeln(meldeListeNew.getXSpreadSheet(),
				Position.from(meldeListeNew.getVornameSpalte(0), SchweizerListeDelegate.ERSTE_DATEN_ZEILE));

		assertThat(formeln.getFirst()).as("Die Duplikat-Prüfung muss vor der Editierfarbe Vorrang haben")
				.containsIgnoringCase("COUNTIFS");
		assertThat(formeln).as("Vor- und Nachname bleiben als editierbare Eingabefelder hervorgehoben")
				.anySatisfy(formel -> assertThat(formel).contains(EditierbaresZelleFormatHelper.PROPERTY_KEY));
	}

	private java.util.List<String> alleConditionalFormatFormeln(XSpreadsheet sheet, Position pos)
			throws GenerateException {
		try {
			XCell xCell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			XPropertySet xPropSet = Lo.qi(XPropertySet.class, xCell);
			XSheetConditionalEntries xEntries = Lo.qi(XSheetConditionalEntries.class,
					xPropSet.getPropertyValue("ConditionalFormat"));
			var formeln = new java.util.ArrayList<String>();
			for (int i = 0; i < xEntries.getCount(); i++) {
				var xCondition = Lo.qi(com.sun.star.sheet.XSheetCondition.class, xEntries.getByIndex(i));
				formeln.add(xCondition.getFormula1());
			}
			return formeln;
		} catch (Exception e) {
			throw new GenerateException(e.getMessage());
		}
	}
}
