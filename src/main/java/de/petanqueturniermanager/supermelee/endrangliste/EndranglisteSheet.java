/**
 * Erstellung : 10.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.endrangliste;

import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.ANZAHL_SPALTEN_IN_SUMME;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_MINUS_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_DIV_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_MINUS_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_PLUS_OFFS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.TurnierModus;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuprMleEndranglisteSortMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class EndranglisteSheet extends SheetRunner implements IEndRangliste {

	public static final int ERSTE_DATEN_ZEILE = 3; // Zeile 4
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0, B=1
	public static final int RANGLISTE_SPALTE = 2; // Spalte C=2
	public static final int ERSTE_SPIELTAG_SPALTE = 3; // Spalte D=3

	public static final int ERSTE_SORTSPALTE_OFFSET = 3; // zur letzte spalte = anz Spieltage

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE;

	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final SpieltagRanglisteSheet spieltagRanglisteSheet;
	private final MeldungenSpalte<SpielerMeldungen, Spieler> spielerSpalte;
	private final EndRanglisteFormatter endRanglisteFormatter;
	private final RangListeSpalte rangListeSpalte;
	private final RangListeSorter rangListeSorter;

	public EndranglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, SheetNamen.endrangliste());
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
		spieltagRanglisteSheet = new SpieltagRanglisteSheet(workingSpreadsheet);
		spielerSpalte = MeldungenSpalte.builder().ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE)
				.anzZeilenInHeader(2).sheet(this).formation(Formation.MELEE)
				.spalteMeldungNameWidth(SuperMeleeKonfigurationSheet.SUPER_MELEE_MELDUNG_NAME_WIDTH).build();
		endRanglisteFormatter = new EndRanglisteFormatter(this, getAnzSpaltenInSpieltag(), spielerSpalte,
				ERSTE_SPIELTAG_SPALTE, getKonfigurationSheet());
		rangListeSpalte = new RangListeSpalte(RANGLISTE_SPALTE, this);
		rangListeSorter = new RangListeSorter(this);
	}

	@Override
	public SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected void doRun() throws GenerateException {
		SpielTagNr spieltagNr = getKonfigurationSheet().getAktiveSpieltag();
		if (NewSheet.from(this, SheetNamen.endrangliste(), METADATA_SCHLUESSEL)
				.pos(DefaultSheetPos.SUPERMELEE_ENDRANGLISTE).tabColor(konfigurationSheet.getRanglisteTabFarbe()).setActiv()
				.hideGrid().forceCreate().spielTagPageStyle(spieltagNr).create().isDidCreate()) {
			getxCalculatable().enableAutomaticCalculation(false); // speed up
			upDateSheet();
		}
	}

	private void upDateSheet() throws GenerateException {

		int anzahlSpieltage = getAnzahlSpieltage();
		if (anzahlSpieltage < 2) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.fehler"))
					.message(I18n.get("msg.text.ungueltige.anzahl.spieltage", anzahlSpieltage)).show();
			return;
		}

		Integer headerColor = getKonfigurationSheet().getRanglisteHeaderFarbe();

		spielerEinfuegen();
		spielerSpalte.insertHeaderInSheet(headerColor);
		spielerSpalte.formatSpielrNrUndNamenspalten(false);
		endRanglisteFormatter.updateHeader();

		spielTageUndSummenAlsWerteEinfuegen();
		anzSpielTageSpalteEinrichten();

		rangListeSorter.insertSortValidateSpalte(false);
		rangListeSorter.insertManuelsortSpalten(false);

		endRanglisteFormatter.formatDaten();
		rangListeSpalte.upDateRanglisteSpalte();
		rangListeSpalte.insertHeaderInSheet(headerColor);

		formatDatenGeradeUngeradeMitStreichSpieltag();
		formatSchlechtesteSpieltagSpalte();
		getxCalculatable().calculate(); // Sort-Spalten-Formeln auswerten
		rangListeSorter.doSort();
		Position footerPos = endRanglisteFormatter.addFooter().getPos();
		printBereichDefinieren(footerPos);
		processBoxinfo("processbox.header.festsetzen");
		SheetFreeze.from(getTurnierSheet()).anzZeilen(3).anzSpalten(3).doFreeze();
		blattschutzSchuetzen();
	}

	protected void blattschutzEntsprerren() {
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE)
					.ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
		}
	}

	protected void blattschutzSchuetzen() {
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE)
					.ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
		}
	}

	private void printBereichDefinieren(Position footerPos) throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		Position linksOben = Position.from(SPIELER_NR_SPALTE,
				AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position rechtsUnten = Position.from(getLetzteSpalte(), footerPos.getZeile());
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	protected void formatDatenGeradeUngeradeMitStreichSpieltag() throws GenerateException {

		processBoxinfo("processbox.endrangliste.formatieren");

		int spielerNrSpalte = spielerSpalte.getSpielerNrSpalte();
		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();

		Integer geradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeGerade();
		Integer unGeradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade();
		Integer streichGeradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeStreichSpieltagGerade();
		Integer streichUnGeradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeStreichSpieltagUnGerade();

		RangePosition datenRange = RangePosition.from(spielerNrSpalte, ersteDatenZeile, getLetzteSpalte(),
				letzteDatenZeile);

		RanglisteGeradeUngeradeFormatHelper.from(this, datenRange)
				.geradeFarbe(geradeColor)
				.ungeradeFarbe(unGeradeColor)
				.validateSpalte(rangListeSorter.validateSpalte())
				.streichSpieltagFormulaGerade(getFormulastreichSpieltag(true), streichGeradeColor)
				.streichSpieltagFormulaUnGerade(getFormulastreichSpieltag(false), streichUnGeradeColor)
				.apply();
	}

	private String getFormulastreichSpieltag(boolean iseven) throws GenerateException {
		// 13 = streichspieltag spalte
		// -1 = offset spalten links
		// /3 = anzahl spalten in spieltag
		// UND(INDIREKT(ADRESSE(ZEILE();13;4;))=AUFRUNDEN((SPALTE()-1)/3;0);ISTGERADE(ZEILE()))
		int schlechtesteSpielTageSpalte = getSchlechtesteSpielTageSpalte() + 1; // In Formula erste spalte ab 1
		int anzSpaltenInSpieltag = getAnzSpaltenInSpieltag();
		int anzahlSpieltage = getAnzahlSpieltage();

		String isCondition = "";
		if (iseven) {
			isCondition = "ISEVEN";
		} else {
			isCondition = "ISODD";
		}

		String verweisAufStreichSpalte = "INDIRECT(ADDRESS(ROW();" + schlechtesteSpielTageSpalte + ";4))";
		// 1. prüfen ob wert in steichspieltag spalte > 0
		String erstePruefung = verweisAufStreichSpalte + ">0";
		// 2. prüfen ob wert in steichspieltag spalte < anzahlSpieltage
		String zweitePruefung = verweisAufStreichSpalte + "<" + (anzahlSpieltage + 1);
		// 3. prüfen ob wert in steichspieltag spalte == aktuelle spalte block
		String drittePruefung = verweisAufStreichSpalte + "=ROUNDUP((COLUMN()-" + ERSTE_SPIELTAG_SPALTE + ")/"
				+ anzSpaltenInSpieltag + ";0)";
		// 4. prüfen ob gerade oder ungerade zeile
		String viertePruefung = isCondition + "(ROW())";
		return "AND(" + erstePruefung + ";" + zweitePruefung + ";" + drittePruefung + ";" + viertePruefung + ")";
	}

	protected void formatSchlechtesteSpieltagSpalte() throws GenerateException {

		processBoxinfo("processbox.endrangliste.sortieren");

		int schlechtesteSpielTageSpalte = getSchlechtesteSpielTageSpalte();
		NumberCellValue numberCellValueSchlechtesteSpielTag = NumberCellValue.from(getXSpreadSheet(),
				schlechtesteSpielTageSpalte, ERSTE_DATEN_ZEILE);

		// Header Streichspieltag
		Position startStreichspieltag = Position.from(getSchlechtesteSpielTageSpalte(),
				AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position endStreichspieltag = Position.from(startStreichspieltag).zeilePlus(2);

		ColumnProperties columnProperties = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerStreichspieltag = StringCellValue.from(getXSpreadSheet(), startStreichspieltag)
				.setEndPosMerge(endStreichspieltag).setCharWeight(FontWeight.LIGHT).setRotateAngle(27000)
				.setVertJustify(CellVertJustify2.CENTER).setValue("Streich")
				.setCellBackColor(endRanglisteFormatter.getHeaderFarbe())
				.setBorder(BorderFactory.from().allBold().toBorder()).setComment(I18n.get("supermelee.endrangliste.comment.streich.spieltag"))
				.setColumnProperties(columnProperties);
		getSheetHelper().setStringValueInCell(headerStreichspieltag);
		// Daten
		RangePosition rangPos = RangePosition.from(getSchlechtesteSpielTageSpalte(), ERSTE_DATEN_ZEILE,
				getSchlechtesteSpielTageSpalte(), getLetzteMitDatenZeileInSpielerNrSpalte());
		CellProperties celRangeProp = CellProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER);
		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), rangPos, celRangeProp);

		for (Integer spielerNr : spielerSpalte.getSpielerNrList()) {
			SheetRunner.testDoCancelTask();
			SpielTagNr spielTagNr = schlechtesteSpieltag(spielerNr);
			int spielerZeile = spielerSpalte.getSpielerZeileNr(spielerNr);
			if (spielTagNr != null && spielerZeile > 0) {
				getSheetHelper().setNumberValueInCell(
						numberCellValueSchlechtesteSpielTag.zeile(spielerZeile).setValue((double) spielTagNr.getNr()));
			}
		}
	}

	protected void spielerEinfuegen() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();

		HashSet<Integer> spielerNummer = new HashSet<>();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			List<Integer> spielerListe = spieltagRanglisteSheet.getSpielerNrList(SpielTagNr.from(spieltagCntr));
			spielerNummer.addAll(spielerListe);
		}
		spielerSpalte.alleSpielerNrEinfuegen(spielerNummer, new MeldeListeSheet_New(getWorkingSpreadsheet()));
	}

	/**
	 * Liest alle Spieltag-Daten per Block-Read aus den Spieltag-Ranglisten,
	 * berechnet EndSummen und AnzahlSpieltage in Java und schreibt alles als
	 * Werte (keine Formeln) per Block-Write in die Endrangliste.
	 */
	protected void spielTageUndSummenAlsWerteEinfuegen() throws GenerateException {

		processBoxinfo("processbox.endrangliste.spieltage");

		int anzSpieltage = getAnzahlSpieltage();
		List<Integer> spielerNrListe = spielerSpalte.getSpielerNrList();
		int anzSpalten = getAnzSpaltenInSpieltag();
		XSpreadsheetDocument xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();

		@SuppressWarnings("unchecked")
		Map<Integer, int[]>[] spieltagDaten = new Map[anzSpieltage];

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			SpielTagNr spielTagNr = SpielTagNr.from(spieltagCntr);
			spieltagRanglisteSheet.setSpieltagNr(spielTagNr);
			XSpreadsheet spieltagSheet = spieltagRanglisteSheet.getXSpreadSheet();
			Map<Integer, int[]> daten = new HashMap<>();
			spieltagDaten[spieltagCntr - 1] = daten;

			if (spieltagSheet == null) {
				continue;
			}

			int ersteSummeSpalte = spieltagRanglisteSheet.getErsteSummeSpalte(spielTagNr);
			RangePosition readRange = RangePosition.from(
					SpieltagRanglisteSheet.SPIELER_NR_SPALTE, SpieltagRanglisteSheet.ERSTE_DATEN_ZEILE,
					ersteSummeSpalte + anzSpalten - 1,
					SpieltagRanglisteSheet.ERSTE_DATEN_ZEILE + 999);
			RangeData rows = RangeHelper.from(spieltagSheet, xDoc, readRange).getDataFromRange();
			int summeOffset = ersteSummeSpalte - SpieltagRanglisteSheet.SPIELER_NR_SPALTE;

			for (RowData row : rows) {
				if (row.size() < summeOffset + anzSpalten) {
					break;
				}
				int spielerNr = row.get(0).getIntVal(-1);
				if (spielerNr < 1) {
					break;
				}
				int[] werte = new int[anzSpalten];
				for (int i = 0; i < anzSpalten; i++) {
					werte[i] = row.get(summeOffset + i).getIntVal(0);
				}
				daten.put(spielerNr, werte);
			}
		}

		RangeData spieltagBlock = new RangeData();
		RangeData endsummenBlock = new RangeData();
		RangeData anzTagBlock = new RangeData();

		for (int spielerNr : spielerNrListe) {
			SheetRunner.testDoCancelTask();

			List<SpielerSpieltagErgebnis> ergebnisseFuerStreich = new ArrayList<>();
			for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
				int[] werte = spieltagDaten[spieltagCntr - 1].get(spielerNr);
				SpielerSpieltagErgebnis erg = SpielerSpieltagErgebnis.from(SpielTagNr.from(spieltagCntr), spielerNr);
				if (werte != null) {
					erg.setSpielPlus(werte[SPIELE_PLUS_OFFS]).setSpielMinus(werte[SPIELE_MINUS_OFFS]);
					erg.setPunktePlus(werte[PUNKTE_PLUS_OFFS]).setPunkteMinus(werte[PUNKTE_MINUS_OFFS]);
				} else {
					erg.setSpielPlus(-1); // nicht gespielter Tag ist immer schlechtester
				}
				ergebnisseFuerStreich.add(erg);
			}
			ergebnisseFuerStreich.sort((o1, o2) -> o1.reversedCompareTo(o2));
			int schlechtesterSpieltagNr = ergebnisseFuerStreich.isEmpty()
					? -1 : ergebnisseFuerStreich.get(0).getSpielTagNr();

			RowData spieltagRow = spieltagBlock.addNewRow();
			int anzGespielt = 0;
			for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
				int[] werte = spieltagDaten[spieltagCntr - 1].get(spielerNr);
				if (werte != null) {
					anzGespielt++;
				}
				for (int i = 0; i < anzSpalten; i++) {
					if (werte != null) {
						spieltagRow.newInt(werte[i]);
					} else {
						spieltagRow.newString("");
					}
				}
			}

			int[] endsummen = new int[anzSpalten];
			for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
				if (spieltagCntr == schlechtesterSpieltagNr) {
					continue;
				}
				int[] werte = spieltagDaten[spieltagCntr - 1].get(spielerNr);
				if (werte != null) {
					for (int i = 0; i < anzSpalten; i++) {
						endsummen[i] += werte[i];
					}
				}
			}
			RowData endsummenRow = endsummenBlock.addNewRow();
			for (int w : endsummen) {
				endsummenRow.newInt(w);
			}

			anzTagBlock.addNewRow().newInt(anzGespielt);
		}

		if (!spielerNrListe.isEmpty()) {
			processBoxinfo("processbox.summenspalten.aktualisieren");
			RangeHelper.from(this,
					spieltagBlock.getRangePosition(Position.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE)))
					.setDataInRange(spieltagBlock);

			int ersteSpalteEndsumme = getErsteSummeSpalte();
			RangeHelper.from(this,
					endsummenBlock.getRangePosition(Position.from(ersteSpalteEndsumme, ERSTE_DATEN_ZEILE)))
					.setDataInRange(endsummenBlock);

			RangeHelper.from(this,
					anzTagBlock.getRangePosition(Position.from(anzSpielTageSpalte(), ERSTE_DATEN_ZEILE)))
					.setDataInRange(anzTagBlock);
		}
	}

	private void anzSpielTageSpalteEinrichten() throws GenerateException {

		processBoxinfo("processbox.endrangliste.aktualisieren");

		int letzteZeile = getLetzteMitDatenZeileInSpielerNrSpalte();
		int anzTageSpaltNr = anzSpielTageSpalte();

		Position start = Position.from(anzTageSpaltNr, AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position end = Position.from(start).zeilePlus(2);
		StringCellValue headerAnzTage = StringCellValue.from(getXSpreadSheet(), start)
				.setEndPosMerge(end).setCharWeight(FontWeight.LIGHT).setRotateAngle(27000)
				.setVertJustify(CellVertJustify2.CENTER).setValue("Tage")
				.setCellBackColor(endRanglisteFormatter.getHeaderFarbe())
				.setBorder(BorderFactory.from().allBold().toBorder())
				.setComment(I18n.get("supermelee.endrangliste.comment.gespielte.tage"))
				.addColumnProperties(ColumnProperties.from()
						.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
						.setHoriJustify(CellHoriJustify.CENTER));
		getSheetHelper().setStringValueInCell(headerAnzTage);

		RangePosition rangPos = RangePosition.from(anzTageSpaltNr, ERSTE_DATEN_ZEILE, anzTageSpaltNr, letzteZeile);
		CellProperties celRangeProp = CellProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER);
		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), rangPos, celRangeProp);
	}

	private int anzSpielTageSpalte() throws GenerateException {
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		return ersteSpalteEndsumme + ANZAHL_SPALTEN_IN_SUMME;
	}

	private int getSchlechtesteSpielTageSpalte() throws GenerateException {
		return anzSpielTageSpalte() + 1;
	}

	private SpielTagNr schlechtesteSpieltag(int spielrNr) throws GenerateException {

		int anzSpieltage = getAnzahlSpieltage();
		if (anzSpieltage < 2) {
			return null;
		}
		List<SpielerSpieltagErgebnis> spielerSpieltagErgebnisse = spielerErgebnisseEinlesen(spielrNr);
		spielerSpieltagErgebnisse.sort((o1, o2) -> o1.reversedCompareTo(o2));
		if (!spielerSpieltagErgebnisse.isEmpty()) {
			return spielerSpieltagErgebnisse.get(0).getSpielTag();
		}
		return null;
	}

	private List<SpielerSpieltagErgebnis> spielerErgebnisseEinlesen(int spielrNr) throws GenerateException {
		List<SpielerSpieltagErgebnis> spielerErgebnisse = new ArrayList<>();
		int anzSpieltage = getAnzahlSpieltage();

		int spielerZeile = spielerSpalte.getSpielerZeileNr(spielrNr);

		XSpreadsheet sheet = getXSpreadSheet();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();

			SpielTagNr spielTagNr = SpielTagNr.from(spieltagCntr);

			int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
			// summe vorhanden ?
			String spielPlus = getSheetHelper().getTextFromCell(sheet,
					Position.from(ersteSpieltagSummeSpalte, spielerZeile));
			if (StringUtils.isNotBlank(spielPlus)) {
				SpielerSpieltagErgebnis ergebnis = new SpielerSpieltagErgebnis(spielTagNr, spielrNr);
				ergebnis.setSpielPlus(NumberUtils.toInt(spielPlus));
				ergebnis.setSpielMinus(NumberUtils.toInt(getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + SPIELE_MINUS_OFFS, spielerZeile))));
				ergebnis.setPunktePlus(NumberUtils.toInt(getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_PLUS_OFFS, spielerZeile))));
				ergebnis.setPunkteMinus(NumberUtils.toInt(getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_MINUS_OFFS, spielerZeile))));
				spielerErgebnisse.add(ergebnis);
			} else {
				// nuller spieltag
				SpielerSpieltagErgebnis nullerSpielTag = new SpielerSpieltagErgebnis(spielTagNr, spielrNr);
				// nicht gespielten spieltage sind immer schlechter als gespielte spieltage
				nullerSpielTag.setSpielPlus(-1); // -1 Plus Punkte sind im normalen Spiel nicht möglich
				spielerErgebnisse.add(nullerSpielTag);
			}
		}
		return spielerErgebnisse;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SheetNamen.LEGACY_ENDRANGLISTE);
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public int getAnzahlSpieltage() throws GenerateException {
		return spieltagRanglisteSheet.countNumberOfRanglisten();
	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();
		return ERSTE_SPIELTAG_SPALTE + (anzSpieltage * ANZAHL_SPALTEN_IN_SUMME);
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		return getSchlechtesteSpielTageSpalte();
	}

	private int getAnzSpaltenInSpieltag() {
		return SuperMeleeSummenSpalten.ANZAHL_SPALTEN_IN_SUMME;
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return spielerSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
		return spielerSpalte.sucheLetzteZeileMitSpielerNummer();
	}

	@Override
	public int getErsteDatenZiele() throws GenerateException {
		return ERSTE_DATEN_ZEILE;
	}

	@Override
	public int getManuellSortSpalte() throws GenerateException {
		return getLetzteSpalte() + ERSTE_SORTSPALTE_OFFSET;
	}

	protected RangListeSpalte getRangListeSpalte() {
		return rangListeSpalte;
	}

	protected RangListeSorter getRangListeSorter() {
		return rangListeSorter;
	}

	@Override
	public int validateSpalte() throws GenerateException {
		return getManuellSortSpalte() + PUNKTE_DIV_OFFS;
	}

	@Override
	public int getErsteSpalte() throws GenerateException {
		return SPIELER_NR_SPALTE;
	}

	@Override
	public void calculateAll() {
		getxCalculatable().calculateAll();
	}

	@Override
	public List<Position> getRanglisteSpalten() throws GenerateException {
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		return getRanglisteSpalten(ersteSpalteEndsumme, ERSTE_DATEN_ZEILE);
	}

	/**
	 * get Ranglistenspalten to sort
	 */

	protected List<Position> getRanglisteSpalten(int ersteSpalteEndsumme, int ersteDatenZeile)
			throws GenerateException {

		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ersteDatenZeile);
		Position anzSpielTageZelle1 = Position.from(anzSpielTageSpalte(), ersteDatenZeile);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ersteDatenZeile);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ersteDatenZeile);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ersteDatenZeile);

		SuprMleEndranglisteSortMode suprMleEndranglisteSortMode = getKonfigurationSheet()
				.getSuprMleEndranglisteSortMode();

		Position[] arraylist = null;

		if (suprMleEndranglisteSortMode == SuprMleEndranglisteSortMode.ANZTAGE) { // Anzahl gespielte Tage mit sortieren
			arraylist = new Position[] { summeSpielGewonnenZelle1, anzSpielTageZelle1, summeSpielDiffZelle1,
					punkteDiffZelle1, punkteGewonnenZelle1 };
		} else {
			arraylist = new Position[] { summeSpielGewonnenZelle1, summeSpielDiffZelle1, punkteDiffZelle1,
					punkteGewonnenZelle1 };
		}

		return Arrays.asList(arraylist);
	}

}
