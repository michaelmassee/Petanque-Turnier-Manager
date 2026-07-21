/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.rangliste;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.maastrichter.MaastrichterGruppenFarbeHelper;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

/**
 * Liest, schreibt und stilisiert die "Gruppe"-Spalte der Maastrichter-Vorrunden-Rangliste.
 * <p>
 * Die Spalte wird beim Erstellen der KO-Gruppen mit den Gruppen-Buchstaben (A, B, C, …)
 * pro Team gefüllt. Beim Neuaufbau bzw. inkrementellen Refresh der Rangliste werden die
 * Werte vorher als TeamNr → Gruppe-Map ausgelesen und nach dem Schreiben der Standardspalten
 * wieder in die neuen Datenzeilen geschrieben.
 */
public final class MaastrichterGruppenSpalteHelper {

	public static final int GRUPPE_SPALTE = SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE + 1;
	private static final int COL_WIDTH = 1400;
	private static final Logger logger = LogManager.getLogger(MaastrichterGruppenSpalteHelper.class);

	private MaastrichterGruppenSpalteHelper() {
		// Utility-Klasse – keine Instanzen
	}

	/**
	 * Liest die bestehenden Gruppen-Zuweisungen (TeamNr → Gruppe) aus dem Rangliste-Sheet.
	 * Gibt eine leere Map zurück, wenn das Sheet noch nicht existiert oder die Spalte leer ist.
	 */
	public static Map<Integer, String> lesePreservedGruppen(SchweizerRanglisteSheet rangliste) {
		Map<Integer, String> result = new HashMap<>();
		try {
			XSpreadsheet sheet = rangliste.getXSpreadSheet();
			if (sheet == null) {
				return result;
			}
			RangePosition leseRange = RangePosition.from(
					SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
					GRUPPE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + 999);
			RangeData rowsData = RangeHelper
					.from(sheet, rangliste.getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), leseRange)
					.getDataFromRange();
			for (RowData row : rowsData) {
				if (row.size() <= GRUPPE_SPALTE) {
					break;
				}
				int teamNr = row.get(SchweizerRanglisteSheet.TEAM_NR_SPALTE).getIntVal(0);
				if (teamNr <= 0) {
					break;
				}
				String gruppe = row.get(GRUPPE_SPALTE).getStringVal();
				if (gruppe != null && !gruppe.isEmpty()) {
					result.put(teamNr, gruppe.trim());
				}
			}
		} catch (GenerateException e) {
			logger.debug("lesePreservedGruppen: Sheet nicht lesbar, leere Map verwendet", e);
		}
		return result;
	}

	/**
	 * Schreibt den zweizeiligen Header der Gruppe-Spalte analog zu den anderen Einzel-Spalten
	 * der Schweizer-Rangliste.
	 */
	public static void schreibeHeader(SchweizerRanglisteSheet rangliste, XSpreadsheet sheet,
			Integer headerColor) throws GenerateException {
		ColumnProperties props = ColumnProperties.from().setWidth(COL_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.isVisible(true);
		rangliste.getSheetHelper().setColumnProperties(sheet, GRUPPE_SPALTE, props);

		StringCellValue cv = StringCellValue
				.from(sheet, Position.from(GRUPPE_SPALTE, SchweizerRanglisteSheet.HEADER_ZEILE),
						I18n.get("column.header.gruppe.kurz"))
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER)
				.setVertJustify(CellVertJustify2.CENTER)
				.setEndPosMergeZeilePlus(1) // vertikal Row 0 + Row 1
				.setCharWeight(FontWeight.BOLD)
				.setShrinkToFit(true);
		rangliste.getSheetHelper().setStringValueInCell(cv);
	}

	/**
	 * Schreibt die Gruppe-Zuweisungen passend zur aktuellen TeamNr-Reihenfolge im Sheet.
	 * Liest dazu Spalte {@code TEAM_NR_SPALTE} aus und mapt jede gefundene TeamNr per
	 * übergebener Map auf den Gruppen-Buchstaben.
	 */
	public static void schreibeGruppenZuweisungen(SchweizerRanglisteSheet rangliste, XSpreadsheet sheet,
			Map<Integer, String> teamNrZuGruppe) throws GenerateException {
		RangePosition leseRange = RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + 999);
		RangeData rowsData = RangeHelper
				.from(sheet, rangliste.getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), leseRange)
				.getDataFromRange();

		RangeData schreibBlock = new RangeData();
		List<String> gruppenSpalte = new ArrayList<>();
		int anzZeilen = 0;
		for (RowData row : rowsData) {
			if (row.size() < 1) {
				break;
			}
			int teamNr = row.get(0).getIntVal(0);
			if (teamNr <= 0) {
				break;
			}
			String gruppe = teamNrZuGruppe.getOrDefault(teamNr, "");
			RowData neueRow = schreibBlock.addNewRow();
			neueRow.newString(gruppe);
			gruppenSpalte.add(gruppe);
			anzZeilen++;
		}
		if (anzZeilen == 0) {
			return;
		}
		RangeHelper.from(rangliste,
				schreibBlock.getRangePosition(Position.from(GRUPPE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE)))
				.setDataInRange(schreibBlock);
		faerbeGruppenBuchstaben(rangliste, sheet, gruppenSpalte);
	}

	/**
	 * Schreibt die Gruppe-Werte (Buchstaben) für die sortierten Teams in die Datenzeilen.
	 * Werte für nicht zugeordnete TeamNrn bleiben leer.
	 */
	public static void schreibeDaten(SchweizerRanglisteSheet rangliste, XSpreadsheet sheet,
			List<SchweizerTeamErgebnis> sortiert, int letzteZeile,
			Map<Integer, String> teamNrZuGruppe) throws GenerateException {
		if (sortiert.isEmpty()) {
			return;
		}
		RangeData block = new RangeData();
		List<String> gruppenSpalte = new ArrayList<>();
		for (SchweizerTeamErgebnis erg : sortiert) {
			String gruppe = teamNrZuGruppe.getOrDefault(erg.teamNr(), "");
			RowData row = block.addNewRow();
			row.newString(gruppe);
			gruppenSpalte.add(gruppe);
		}
		RangeHelper.from(rangliste,
				block.getRangePosition(Position.from(GRUPPE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE)))
				.setDataInRange(block);

		rangliste.getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(GRUPPE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
						GRUPPE_SPALTE, letzteZeile),
				CellProperties.from()
						.setAllThinBorder()
						.centerJustify()
						.setCharWeight(FontWeight.BOLD));
		faerbeGruppenBuchstaben(rangliste, sheet, gruppenSpalte);
	}

	/**
	 * Färbt die Schrift jedes Gruppenbuchstabens passend zur Buchstabe→Farbe-Zuordnung aus
	 * {@link MaastrichterGruppenFarbeHelper}, analog zur Maastrichter-Gruppen-Übersicht.
	 * Zusammenhängende Zeilen derselben Gruppe werden in einem Aufruf eingefärbt.
	 */
	private static void faerbeGruppenBuchstaben(SchweizerRanglisteSheet rangliste, XSpreadsheet sheet,
			List<String> gruppenSpalte) throws GenerateException {
		int blockStart = 0;
		for (int i = 1; i <= gruppenSpalte.size(); i++) {
			boolean blockEndeErreicht = i == gruppenSpalte.size()
					|| !gruppenSpalte.get(i).equals(gruppenSpalte.get(blockStart));
			if (blockEndeErreicht) {
				String gruppe = gruppenSpalte.get(blockStart);
				if (!gruppe.isEmpty()) {
					rangliste.getSheetHelper().setPropertiesInRange(sheet,
							RangePosition.from(GRUPPE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + blockStart,
									GRUPPE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + i - 1),
							CellProperties.from()
									.setCharColor(MaastrichterGruppenFarbeHelper.gruppenBuchstabeFarbe(gruppe)));
				}
				blockStart = i;
			}
		}
	}
}
