package de.petanqueturniermanager.jedergegenjeden.spielplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.model.TeamPaarung;

/**
 * Erstellt für jede JGJ-Gruppe ein eigenes minimales Spielplan-Sheet (Aushang).
 * <p>
 * Reine Anzeige zum Drucken oder als Quelle für die Webseiten-Ausgabe.
 * Die Ergebnis-Spalten verweisen per Formel auf die zentralen Ergebnis-Zellen
 * im "Spielplan"-Tab, sodass Eingaben automatisch hier sichtbar werden.
 * <p>
 * Wird ausschließlich aus {@link JGJSpielPlanSheet} im Gruppen-Modus aufgerufen –
 * die Reihenfolge der Paarungen muss exakt der Reihenfolge im zentralen
 * Spielplan entsprechen, damit die Zeilen-Referenzen passen.
 */
public class JGJGruppenSpielplaeneSheet implements ISheet {

	private static final int HEADER_TITEL_ZEILE = 0;
	private static final int HEADER_SPALTEN_ZEILE = 1;
	private static final int ERSTE_DATEN_ZEILE = HEADER_SPALTEN_ZEILE + 2; // 2 Header-Zeilen: Gruppen-Labels + A/B Sub-Labels

	private static final int SPALTE_NR = 0;
	private static final int SPALTE_TEAM_A = 1;
	private static final int SPALTE_TEAM_B = 2;
	private static final int SPALTE_ERGEBNIS_A = 3;
	private static final int SPALTE_ERGEBNIS_B = 4;
	private static final int LETZTE_SPALTE = SPALTE_ERGEBNIS_B;

	private static final int SPALTE_BREITE_NR = 1000;        // 1 cm
	private static final int SPALTE_BREITE_ERGEBNIS = 1000;  // 1 cm

	private final ISheet parent;
	private final JGJKonfigurationSheet konfigurationSheet;

	private XSpreadsheet aktuellesSheet;

	public JGJGruppenSpielplaeneSheet(ISheet parent, JGJKonfigurationSheet konfigurationSheet) {
		this.parent = parent;
		this.konfigurationSheet = konfigurationSheet;
	}

	@Override
	public SheetHelper getSheetHelper() throws GenerateException {
		return parent.getSheetHelper();
	}

	@Override
	public XSpreadsheet getXSpreadSheet() {
		return aktuellesSheet;
	}

	@Override
	public Logger getLogger() {
		return LogManager.getLogger(this.getClass());
	}

	@Override
	public XComponentContext getxContext() {
		return parent.getxContext();
	}

	@Override
	public WorkingSpreadsheet getWorkingSpreadsheet() {
		return parent.getWorkingSpreadsheet();
	}

	@Override
	public void processBoxinfo(String i18nKey, Object... args) {
		parent.processBoxinfo(i18nKey, args);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	/**
	 * Erstellt das Aushang-Sheet einer einzelnen Gruppe und verdrahtet die
	 * Ergebnis-Spalten per Formel mit dem zentralen Spielplan.
	 *
	 * @param buchstabe              Gruppen-Buchstabe (A, B, C, ...)
	 * @param zentraleStartZeile     Zeile (0-basiert) im zentralen Spielplan, ab der
	 *                               die Spiele dieser Gruppe stehen.
	 * @param hRunde                 Hinrunde (Reihenfolge wie im zentralen Spielplan).
	 * @param rRunde                 Rückrunde oder leere Liste.
	 * @param zeigeNr                {@code true}: Team-Nr statt Name anzeigen.
	 * @param teamNamen              Mapping Team-Nr → Name (wenn {@code !zeigeNr}).
	 * @param freispielText          Anzeigetext für Freispiel-Paarungen.
	 */
	public void erstelle(String buchstabe, int zentraleStartZeile,
			List<List<TeamPaarung>> hRunde, List<List<TeamPaarung>> rRunde,
			boolean zeigeNr, Map<Integer, String> teamNamen, String freispielText) throws GenerateException {

		String sheetName = SheetNamen.jgjGruppeSpielplan(buchstabe);
		String metaKey = SheetMetadataHelper.schluesselJgjGruppeSpielplan(buchstabe);

		NewSheet.from(this, sheetName, metaKey)
				.tabColor(konfigurationSheet.getSpielrundeTabFarbe())
				.pos(DefaultSheetPos.JGJ_WORK)
				.forceCreate().hideGrid().create();

		aktuellesSheet = SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), metaKey, sheetName);
		try {
			schreibeSpaltenBreiten(zeigeNr);
			schreibeTitelZeile(buchstabe);
			schreibeSpaltenHeader();

			int letzteDatenZeile = schreibeSpieleDaten(hRunde, rRunde, zeigeNr, teamNamen, freispielText);
			schreibeErgebnisFormeln(letzteDatenZeile, zentraleStartZeile);

			formatiereSheet(letzteDatenZeile, hRunde, rRunde);
			definierePrintBereich(letzteDatenZeile);

			if (SheetRunner.isRunning()) {
				SheetFreeze.from(aktuellesSheet, getWorkingSpreadsheet())
						.anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();
			}
		} finally {
			aktuellesSheet = null;
		}
	}

	private void schreibeSpaltenBreiten(boolean zeigeNr) throws GenerateException {
		int teamSpalteBreite = zeigeNr ? SPALTE_BREITE_NR : JGJKonfigurationSheet.MELDUNG_NAME_WIDTH;
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_NR,
				ColumnProperties.from().setWidth(SPALTE_BREITE_NR).centerJustify());
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_TEAM_A,
				ColumnProperties.from().setWidth(teamSpalteBreite));
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_TEAM_B,
				ColumnProperties.from().setWidth(teamSpalteBreite));
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_ERGEBNIS_A,
				ColumnProperties.from().setWidth(SPALTE_BREITE_ERGEBNIS).centerJustify());
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_ERGEBNIS_B,
				ColumnProperties.from().setWidth(SPALTE_BREITE_ERGEBNIS).centerJustify());
	}

	private void schreibeTitelZeile(String buchstabe) throws GenerateException {
		String titel = I18n.get("jgj.gruppe.name") + " " + buchstabe;
		StringCellValue titelVal = StringCellValue
				.from(aktuellesSheet, Position.from(SPALTE_NR, HEADER_TITEL_ZEILE), titel)
				.setEndPosMergeSpalte(LETZTE_SPALTE)
				.setCellBackColor(konfigurationSheet.getSpielPlanHeaderFarbe())
				.setHoriJustify(CellHoriJustify.CENTER)
				.setCharHeight(14)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
		getSheetHelper().setStringValueInCell(titelVal);
	}

	private void schreibeSpaltenHeader() throws GenerateException {
		int farbe = konfigurationSheet.getSpielPlanHeaderFarbe();
		var border = BorderFactory.from().allThin().toBorder();

		StringCellValue val = StringCellValue
				.from(aktuellesSheet, Position.from(SPALTE_NR, HEADER_SPALTEN_ZEILE))
				.setCellBackColor(farbe)
				.setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(border);

		// Nr. über beide Header-Zeilen zusammenführen
		getSheetHelper().setStringValueInCell(
				val.setValue(I18n.get("column.header.nr")).setEndPosMergeZeilePlus(1));

		// "Mannschaft" über Spalten A+B zusammenführen
		getSheetHelper().setStringValueInCell(
				val.setValue(I18n.get("column.header.mannschaft"))
						.spalte(SPALTE_TEAM_A).setEndPosMergeSpaltePlus(1));

		// "Ergebnis" über Spalten A+B zusammenführen
		getSheetHelper().setStringValueInCell(
				val.setValue(I18n.get("column.header.ergebnis"))
						.spalte(SPALTE_ERGEBNIS_A).setEndPosMergeSpaltePlus(1));

		// Unterzeile: A / B Sub-Labels
		val.setEndPosMerge(null).zeilePlusEins();
		getSheetHelper().setStringValueInCell(val.setValue("A").spalte(SPALTE_TEAM_A));
		getSheetHelper().setStringValueInCell(val.setValue("B").spalte(SPALTE_TEAM_B));
		getSheetHelper().setStringValueInCell(val.setValue("A").spalte(SPALTE_ERGEBNIS_A));
		getSheetHelper().setStringValueInCell(val.setValue("B").spalte(SPALTE_ERGEBNIS_B));
	}

	private int schreibeSpieleDaten(List<List<TeamPaarung>> hRunde, List<List<TeamPaarung>> rRunde,
			boolean zeigeNr, Map<Integer, String> teamNamen, String freispielText) throws GenerateException {

		List<List<TeamPaarung>> alleSpieltage = new ArrayList<>();
		alleSpieltage.addAll(hRunde);
		alleSpieltage.addAll(rRunde);

		RangeData daten = new RangeData();
		int hIndex = 1;
		int rIndex = 1;
		boolean inHinrunde = true;
		int hAnzahl = anzPaarungen(hRunde);

		for (List<TeamPaarung> spieltag : alleSpieltage) {
			for (TeamPaarung paarung : spieltag) {
				SheetRunner.testDoCancelTask();
				RowData row = daten.addNewRow();
				if (inHinrunde) {
					row.newString(JGJSpielPlanSheet.NR_HINRUNDE_PREFIX + hIndex++);
				} else {
					row.newString(JGJSpielPlanSheet.NR_RUECKRUNDE_PREFIX + rIndex++);
				}
				int nrA = paarung.getA().getNr();
				boolean hatB = paarung.getOptionalB().isPresent();
				int nrB = hatB ? paarung.getB().getNr() : 0;

				row.newString(zeigeNr ? String.valueOf(nrA) : teamNamen.getOrDefault(nrA, ""));
				row.newString(hatB
						? (zeigeNr ? String.valueOf(nrB) : teamNamen.getOrDefault(nrB, ""))
						: freispielText);
			}
			if (inHinrunde && hIndex > hAnzahl) {
				inHinrunde = false;
			}
		}

		Position startPos = Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE);
		int letzteDatenZeile = ERSTE_DATEN_ZEILE + daten.size() - 1;

		RangeHelper.from(this, daten.getRangePosition(startPos)).setDataInRange(daten);

		// Border/Formatierung über die komplette Datenzeile (inkl. der per Formel
		// befüllten Ergebnis-Spalten) anwenden.
		RangeHelper.from(this,
				RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, letzteDatenZeile))
				.setRangeProperties(RangeProperties.from()
						.setBorder(BorderFactory.from().allThin().toBorder())
						.centerJustify().setShrinkToFit(true)
						.topMargin(110).bottomMargin(110).setCharHeight(12));

		return letzteDatenZeile;
	}

	/**
	 * Schreibt für die Ergebnis-Spalten Formeln, die direkt auf die Ergebnis-Zellen
	 * im zentralen Spielplan-Tab verweisen. Verwendet relative Zeilen-Referenzen,
	 * damit {@code fillAutoDown} die Zeilen entsprechend hochzählt.
	 */
	private void schreibeErgebnisFormeln(int letzteDatenZeile, int zentraleStartZeile) throws GenerateException {
		String spielplanRef = "$'" + SheetNamen.spielplan() + "'.";

		String ergAStart = Position.from(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE, zentraleStartZeile).getAddress();
		String ergBStart = Position.from(JGJSpielPlanSheet.SPIELPNKT_B_SPALTE, zentraleStartZeile).getAddress();

		StringCellValue formulaA = StringCellValue.from(aktuellesSheet)
				.setValue(spielplanRef + ergAStart)
				.setPos(Position.from(SPALTE_ERGEBNIS_A, ERSTE_DATEN_ZEILE))
				.setFillAutoDown(letzteDatenZeile);
		getSheetHelper().setFormulaInCell(formulaA);

		StringCellValue formulaB = StringCellValue.from(aktuellesSheet)
				.setValue(spielplanRef + ergBStart)
				.setPos(Position.from(SPALTE_ERGEBNIS_B, ERSTE_DATEN_ZEILE))
				.setFillAutoDown(letzteDatenZeile);
		getSheetHelper().setFormulaInCell(formulaB);
	}

	private static int anzPaarungen(List<List<TeamPaarung>> spielplan) {
		int anz = 0;
		for (List<TeamPaarung> spieltag : spielplan) {
			anz += spieltag.size();
		}
		return anz;
	}

	private void formatiereSheet(int letzteDatenZeile, List<List<TeamPaarung>> hRunde,
			List<List<TeamPaarung>> rRunde) throws GenerateException {
		RangePosition zebraRange = RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE,
				LETZTE_SPALTE, letzteDatenZeile);
		RanglisteGeradeUngeradeFormatHelper.from(this, zebraRange)
				.geradeFarbe(konfigurationSheet.getSpielPlanHintergrundFarbeGerade())
				.ungeradeFarbe(konfigurationSheet.getSpielPlanHintergrundFarbeUnGerade())
				.apply();

		RangeProperties horTrennerDouble = RangeProperties.from()
				.setBorder(BorderFactory.from().doubleLn().forBottom().toBorder());
		RangeProperties horTrennerBoldBottom = RangeProperties.from()
				.setBorder(BorderFactory.from().boldLn().forBottom().toBorder());

		int zeile = ERSTE_DATEN_ZEILE;
		int anzRunden = hRunde.size();
		int anzPaarungenProRunde = hRunde.isEmpty() ? 0 : hRunde.get(0).size();

		for (int i = 1; i < anzRunden; i++) {
			zeile += anzPaarungenProRunde - 1;
			RangeHelper.from(this,
					RangePosition.from(SPALTE_NR, zeile, LETZTE_SPALTE, zeile))
					.setRangeProperties(horTrennerDouble);
			zeile++;
		}
		zeile += anzPaarungenProRunde - 1;
		RangeHelper.from(this,
				RangePosition.from(SPALTE_NR, zeile, LETZTE_SPALTE, zeile))
				.setRangeProperties(horTrennerBoldBottom);
		zeile++;

		if (!rRunde.isEmpty()) {
			for (int i = 1; i < anzRunden; i++) {
				zeile += anzPaarungenProRunde - 1;
				RangeHelper.from(this,
						RangePosition.from(SPALTE_NR, zeile, LETZTE_SPALTE, zeile))
						.setRangeProperties(horTrennerDouble);
				zeile++;
			}
		}

		RangeProperties vertTrennerBoldLeft = RangeProperties.from()
				.setBorder(BorderFactory.from().boldLn().forLeft().toBorder());
		RangeProperties vertTrennerDoubleLeft = RangeProperties.from()
				.setBorder(BorderFactory.from().doubleLn().forLeft().toBorder());

		RangePosition vertikal = RangePosition.from(SPALTE_NR, HEADER_SPALTEN_ZEILE,
				SPALTE_NR, letzteDatenZeile);
		RangeHelper.from(this, vertikal.spalte(SPALTE_NR)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(this, vertikal.spalte(SPALTE_TEAM_B)).setRangeProperties(vertTrennerDoubleLeft);
		RangeHelper.from(this, vertikal.spalte(SPALTE_ERGEBNIS_A)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(this, vertikal.spalte(LETZTE_SPALTE + 1)).setRangeProperties(vertTrennerBoldLeft);
	}

	private void definierePrintBereich(int letzteDatenZeile) throws GenerateException {
		RangePosition bereich = RangePosition.from(
				Position.from(SPALTE_NR, HEADER_TITEL_ZEILE),
				Position.from(LETZTE_SPALTE, letzteDatenZeile));
		PrintArea.from(aktuellesSheet, getWorkingSpreadsheet()).setPrintArea(bereich);
	}
}
