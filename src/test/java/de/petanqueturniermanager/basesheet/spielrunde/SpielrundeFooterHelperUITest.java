package de.petanqueturniermanager.basesheet.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XPrintAreas;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.TableBorder2;
import com.sun.star.table.XCell;
import com.sun.star.text.XText;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;

/**
 * Verifiziert end-to-end (echtes LibreOffice), dass der Spielrunde-Footer
 * ({@link BasePropertiesSpalte#KONFIG_PROP_SPIELRUNDE_FOOTER}) unterhalb der Tabelle
 * geschrieben wird, den Rahmen bekommt und der Druckbereich entsprechend erweitert wird.
 * Nutzt Supermelee als Beispielsystem, da {@link SpielrundeFooterHelper} aber
 * systemübergreifend (identisch) in allen Turniersystemen eingehängt ist.
 */
public class SpielrundeFooterHelperUITest extends BaseCalcUITest {

	@Test
	void footerWirdGeschriebenUndDruckbereichErweitert() throws Exception {
		docPropHelper.setStringProperty(BasePropertiesSpalte.KONFIG_PROP_SPIELRUNDE_FOOTER, "Erste Zeile\nZweite Zeile");

		TestSuperMeleeMeldeListeErstellen testMeldeListe = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListe.run();

		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		spielrundeSheetNaechste.run();

		Position letztePositionRechtsUnten = spielrundeSheetNaechste.letztePositionRechtsUnten();

		XSpreadsheet spielrunde = sheetHlp.findByName("1.1. Spielrunde");
		assertThat(spielrunde).isNotNull();

		XPrintAreas printAreas = Lo.qi(XPrintAreas.class, spielrunde);
		CellRangeAddress[] bereiche = printAreas.getPrintAreas();
		assertThat(bereiche).as("genau ein Druckbereich erwartet").hasSize(1);

		int erwarteteErsteFooterZeile = letztePositionRechtsUnten.getZeile() + 2;
		int erwarteteLetzteFooterZeile = erwarteteErsteFooterZeile + 1; // 2 Footer-Zeilen

		assertThat(bereiche[0].EndRow)
				.as("Druckbereich muss bis zur letzten Footer-Zeile erweitert sein")
				.isEqualTo(erwarteteLetzteFooterZeile);

		XCell zelle1 = spielrunde.getCellByPosition(bereiche[0].StartColumn, erwarteteErsteFooterZeile);
		XCell zelle2 = spielrunde.getCellByPosition(bereiche[0].StartColumn, erwarteteLetzteFooterZeile);

		assertThat(Lo.qi(XText.class, zelle1).getString()).isEqualTo("Erste Zeile");
		assertThat(Lo.qi(XText.class, zelle2).getString()).isEqualTo("Zweite Zeile");

		XPropertySet zelle1Props = Lo.qi(XPropertySet.class, zelle1);
		TableBorder2 border = (TableBorder2) zelle1Props.getPropertyValue("TableBorder2");
		assertThat(border.TopLine.LineWidth).as("Footer-Zelle muss einen (einfachen) Rahmen haben").isGreaterThan(0);
	}

	@Test
	void keinEingriffWennFooterTextLeer() throws GenerateException {
		docPropHelper.setStringProperty(BasePropertiesSpalte.KONFIG_PROP_SPIELRUNDE_FOOTER, "");

		TestSuperMeleeMeldeListeErstellen testMeldeListe = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListe.run();

		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		spielrundeSheetNaechste.run();

		Position letztePositionRechtsUnten = spielrundeSheetNaechste.letztePositionRechtsUnten();

		XSpreadsheet spielrunde = sheetHlp.findByName("1.1. Spielrunde");
		XPrintAreas printAreas = Lo.qi(XPrintAreas.class, spielrunde);
		CellRangeAddress[] bereiche = printAreas.getPrintAreas();

		assertThat(bereiche[0].EndRow)
				.as("Ohne Footer-Text darf der Druckbereich nicht erweitert werden")
				.isEqualTo(letztePositionRechtsUnten.getZeile());
	}
}
