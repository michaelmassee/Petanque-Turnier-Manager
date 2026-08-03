/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XPrintAreas;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellRangeAddress;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;

/**
 * Schreibt den nutzerdefinierten, systemübergreifenden Spielrunde-Footer
 * ({@link BasePropertiesSpalte#KONFIG_PROP_SPIELRUNDE_FOOTER}) unterhalb der
 * jeweils bereits gesetzten Druckbereichs-Tabelle eines Turniersystems und
 * erweitert den Druckbereich entsprechend.
 *
 * <p>Wird von jedem Turniersystem direkt NACH dessen eigener
 * Druckbereichs-Setzung (z.B. {@code printBereichDefinieren()}) aufgerufen.
 * Da sowohl der allgemeine Export als auch die Live-Webserver-Ansicht
 * ({@code TabellenMapper.ermittleDruckbereich()}) den Druckbereich als
 * Datenquelle nutzen, erscheint der Footer dadurch automatisch überall,
 * ohne dass Export- oder Web-Code angepasst werden muss.
 *
 * <p>Ist der Footer-Text leer (Standardfall), ist dies ein No-Op — keine
 * Verhaltensänderung für Dokumente, die die Option nicht nutzen.
 */
public final class SpielrundeFooterHelper {

	private static final Logger logger = LogManager.getLogger(SpielrundeFooterHelper.class);

	/** Abstand zwischen letzter Datenzeile und erster Footer-Zeile (1 Leerzeile). */
	private static final int ZEILEN_ABSTAND = 2;

	private SpielrundeFooterHelper() {
	}

	public static void schreibeFooterUndErweitereDruckbereich(ISheet iSheet, XSpreadsheet xsheet, WorkingSpreadsheet ws)
			throws GenerateException {
		String footerText = new DocumentPropertiesHelper(ws)
				.getStringProperty(BasePropertiesSpalte.KONFIG_PROP_SPIELRUNDE_FOOTER, "");
		if (StringUtils.isBlank(footerText)) {
			return;
		}

		CellRangeAddress bereich = ermittleAktuellenDruckbereich(xsheet);
		if (bereich == null) {
			logger.debug("Spielrunde-Footer übersprungen: kein Druckbereich gesetzt");
			return;
		}

		String[] zeilen = footerText.split("\n");
		int footerStartZeile = bereich.EndRow + ZEILEN_ABSTAND;
		for (int i = 0; i < zeilen.length; i++) {
			BorderFactory borderFactory = BorderFactory.from().thinLn().forLeft().forRight();
			if (i == 0) {
				borderFactory.forTop();
			}
			if (i == zeilen.length - 1) {
				borderFactory.forBottom();
			}
			StringCellValue footerZelle = StringCellValue
					.from(xsheet, Position.from(bereich.StartColumn, footerStartZeile + i))
					.setEndPosMergeSpalte(bereich.EndColumn)
					.setBorder(borderFactory.toBorder());
			iSheet.getSheetHelper().setStringValueInCell(footerZelle.setValue(zeilen[i]));
		}

		int footerEndZeile = footerStartZeile + zeilen.length - 1;
		PrintArea.from(xsheet, ws).setPrintArea(RangePosition.from(
				bereich.StartColumn, bereich.StartRow, bereich.EndColumn, footerEndZeile));
	}

	private static CellRangeAddress ermittleAktuellenDruckbereich(XSpreadsheet xsheet) {
		XPrintAreas printAreas = Lo.qi(XPrintAreas.class, xsheet);
		if (printAreas == null) {
			return null;
		}
		CellRangeAddress[] bereiche = printAreas.getPrintAreas();
		if (bereiche == null || bereiche.length == 0) {
			return null;
		}
		return begrenzungsrahmen(bereiche);
	}

	private static CellRangeAddress begrenzungsrahmen(CellRangeAddress[] bereiche) {
		var box = new CellRangeAddress();
		box.Sheet = bereiche[0].Sheet;
		box.StartColumn = bereiche[0].StartColumn;
		box.StartRow = bereiche[0].StartRow;
		box.EndColumn = bereiche[0].EndColumn;
		box.EndRow = bereiche[0].EndRow;
		for (int i = 1; i < bereiche.length; i++) {
			box.StartColumn = Math.min(box.StartColumn, bereiche[i].StartColumn);
			box.StartRow = Math.min(box.StartRow, bereiche[i].StartRow);
			box.EndColumn = Math.max(box.EndColumn, bereiche[i].EndColumn);
			box.EndRow = Math.max(box.EndRow, bereiche[i].EndRow);
		}
		return box;
	}
}
