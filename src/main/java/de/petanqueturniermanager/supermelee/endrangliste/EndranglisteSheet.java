/*
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

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
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
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
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
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
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

		rangListeSorter.insertSortValidateSpalte(false);
		rangListeSorter.insertManuelsortSpalten(false);

		streichspieltagSpalteHeader();
		anzSpieltageSpalteHeader();

		berechnungUndSchreiben(getXSpreadSheet());

		endRanglisteFormatter.formatDaten();
		rangListeSpalte.insertHeaderInSheet(headerColor);

		formatDatenGeradeUngeradeMitStreichSpieltag();
		formatSchlechtesteSpieltagSpaltenRahmen();
		anzSpieltageSpaltenRahmen();
		getxCalculatable().calculate();
		rangListeSorter.doSort();
		rangListeSpalte.upDateRanglisteSpalte();
		Position footerPos = endRanglisteFormatter.addFooter().getPos();
		printBereichDefinieren(footerPos);
		processBoxinfo("processbox.header.festsetzen");
		SheetFreeze.from(getTurnierSheet()).anzZeilen(3).anzSpalten(3).doFreeze();
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE).ifPresent(
					k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
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

	private void formatDatenGeradeUngeradeMitStreichSpieltag() throws GenerateException {

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

	/**
	 * Streichspieltag-Spalte: Header + Spalteneigenschaften.
	 * Daten werden in {@link #berechnungUndSchreiben(XSpreadsheet)} per Block-Write geschrieben.
	 */
	private void streichspieltagSpalteHeader() throws GenerateException {
		Position startStreichspieltag = Position.from(getSchlechtesteSpielTageSpalte(),
				AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position endStreichspieltag = Position.from(startStreichspieltag).zeilePlus(2);

		ColumnProperties columnProperties = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerStreichspieltag = StringCellValue.from(getXSpreadSheet(), startStreichspieltag)
				.setEndPosMerge(endStreichspieltag).setCharWeight(FontWeight.LIGHT).setRotateAngle(27000)
				.setVertJustify(CellVertJustify2.CENTER).setValue("Streich")
				.setCellBackColor(endRanglisteFormatter.getHeaderFarbe())
				.setBorder(BorderFactory.from().allBold().toBorder())
				.setComment(I18n.get("supermelee.endrangliste.comment.streich.spieltag"))
				.setColumnProperties(columnProperties);
		getSheetHelper().setStringValueInCell(headerStreichspieltag);
	}

	private void formatSchlechtesteSpieltagSpaltenRahmen() throws GenerateException {
		int letzteZeile = getLetzteMitDatenZeileInSpielerNrSpalte();
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		RangePosition rangPos = RangePosition.from(getSchlechtesteSpielTageSpalte(), ERSTE_DATEN_ZEILE,
				getSchlechtesteSpielTageSpalte(), letzteZeile);
		CellProperties celRangeProp = CellProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER);
		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), rangPos, celRangeProp);
	}

	private void spielerEinfuegen() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();

		HashSet<Integer> spielerNummer = new HashSet<>();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			List<Integer> spielerListe = spieltagRanglisteSheet.getSpielerNrList(SpielTagNr.from(spieltagCntr));
			spielerNummer.addAll(spielerListe);
		}
		spielerSpalte.alleSpielerNrEinfuegen(spielerNummer, new MeldeListeSheet_New(getWorkingSpreadsheet()));
	}

	private int anzSpielTageSpalte() throws GenerateException {
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		return ersteSpalteEndsumme + ANZAHL_SPALTEN_IN_SUMME;
	}

	private int getSchlechtesteSpielTageSpalte() throws GenerateException {
		return anzSpielTageSpalte() + 1;
	}

	private void anzSpieltageSpalteHeader() throws GenerateException {
		Position start = Position.from(anzSpielTageSpalte(),
				AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position end = Position.from(start).zeilePlus(2);
		ColumnProperties celColumProp = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerAnzTage = StringCellValue.from(getXSpreadSheet(), start).setEndPosMerge(end)
				.setCharWeight(FontWeight.LIGHT).setRotateAngle(27000).setVertJustify(CellVertJustify2.CENTER)
				.setValue("Tage").setCellBackColor(endRanglisteFormatter.getHeaderFarbe())
				.setBorder(BorderFactory.from().allBold().toBorder())
				.setComment(I18n.get("supermelee.endrangliste.comment.gespielte.tage"))
				.setColumnProperties(celColumProp);
		getSheetHelper().setStringValueInCell(headerAnzTage);
	}

	private void anzSpieltageSpaltenRahmen() throws GenerateException {
		int letzteZeile = getLetzteMitDatenZeileInSpielerNrSpalte();
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		RangePosition rangPos = RangePosition.from(anzSpielTageSpalte(), ERSTE_DATEN_ZEILE,
				anzSpielTageSpalte(), letzteZeile);
		CellProperties celRangeProp = CellProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder());
		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), rangPos, celRangeProp);
	}

	/**
	 * Liest die Summenblöcke aller Spieltag-Rangliste-Sheets einmalig per Block-Read
	 * in einen Cache. Anschließend werden je Spieler die End-Summen, der Streichspieltag
	 * und die Anzahl gespielter Spieltage rein in Java berechnet und als
	 * <em>ein</em> {@link RangeData}-Block in den Spalten
	 * {@link #ERSTE_SPIELTAG_SPALTE}..{@link #getSchlechtesteSpielTageSpalte()}
	 * geschrieben. Keine Sheet-Formeln.
	 * <p>
	 * Gemeinsame Methode für Vollaufbau und inkrementelles Update.
	 */
	protected void berechnungUndSchreiben(XSpreadsheet sheet) throws GenerateException {
		processBoxinfo("processbox.endrangliste.spieltage");

		int anzSpieltage = getAnzahlSpieltage();
		if (anzSpieltage < 2) {
			return;
		}

		List<Integer> spielerNrList = leseSpielerNrInSheetOrdnung();
		if (spielerNrList.isEmpty()) {
			return;
		}

		Map<Integer, Map<Integer, SpielerSpieltagErgebnis>> cache = leseSpieltagRanglisteCache(anzSpieltage);

		int letzteDatenzeile = ERSTE_DATEN_ZEILE + spielerNrList.size() - 1;
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		int anzSpielTageSpalte = anzSpielTageSpalte();
		int schlechtesterSpielTageSpalte = getSchlechtesteSpielTageSpalte();

		RangeData block = new RangeData();
		for (Integer spielerNr : spielerNrList) {
			SheetRunner.testDoCancelTask();
			List<SpielerSpieltagErgebnis> ergebnisseDesSpielers = ergebnisseDesSpielers(cache, anzSpieltage, spielerNr);
			SpielTagNr streich = ermittleStreichSpieltag(ergebnisseDesSpielers);

			RowData row = block.addNewRow();
			int endSpielePlus = 0;
			int endSpieleMinus = 0;
			int endSpieleDiff = 0;
			int endPunktePlus = 0;
			int endPunkteMinus = 0;
			int endPunkteDiff = 0;
			int anzGespielt = 0;

			for (SpielerSpieltagErgebnis erg : ergebnisseDesSpielers) {
				if (erg.getSpielPlus() < 0) {
					// nuller Spieltag – komplette Zellgruppe leer lassen
					for (int i = 0; i < ANZAHL_SPALTEN_IN_SUMME; i++) {
						row.newEmpty();
					}
				} else {
					int sPlus = erg.getSpielPlus();
					int sMinus = erg.getSpielMinus();
					int pPlus = erg.getPunktePlus();
					int pMinus = erg.getPunkteMinus();
					row.newInt(sPlus);
					row.newInt(sMinus);
					row.newInt(sPlus - sMinus);
					row.newInt(pPlus);
					row.newInt(pMinus);
					row.newInt(pPlus - pMinus);
					anzGespielt++;
					if (streich == null || streich.getNr() != erg.getSpielTag().getNr()) {
						endSpielePlus += sPlus;
						endSpieleMinus += sMinus;
						endSpieleDiff += sPlus - sMinus;
						endPunktePlus += pPlus;
						endPunkteMinus += pMinus;
						endPunkteDiff += pPlus - pMinus;
					}
				}
			}
			row.newInt(endSpielePlus);
			row.newInt(endSpieleMinus);
			row.newInt(endSpieleDiff);
			row.newInt(endPunktePlus);
			row.newInt(endPunkteMinus);
			row.newInt(endPunkteDiff);

			row.newInt(anzGespielt);
			if (streich != null) {
				row.newInt(streich.getNr());
			} else {
				row.newEmpty();
			}
		}

		// Sanity check: Layout muss zusammenhängend sein.
		int endBlockSpalte = ersteSpalteEndsumme + ANZAHL_SPALTEN_IN_SUMME - 1;
		int expectedAnzTageSpalte = endBlockSpalte + 1;
		int expectedSchlechtSpalte = expectedAnzTageSpalte + 1;
		if (anzSpielTageSpalte != expectedAnzTageSpalte || schlechtesterSpielTageSpalte != expectedSchlechtSpalte) {
			throw new GenerateException("EndranglisteSheet: unerwartetes Spaltenlayout für Block-Write");
		}

		RangeHelper.from(this,
				RangePosition.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE,
						schlechtesterSpielTageSpalte, letzteDatenzeile))
				.setDataInRange(block);
	}

	private List<Integer> leseSpielerNrInSheetOrdnung() throws GenerateException {
		int letzteDatenzeile = spielerSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();
		List<Integer> result = new ArrayList<>();
		if (letzteDatenzeile < ERSTE_DATEN_ZEILE) {
			return result;
		}
		RangeData data = RangeHelper.from(this,
				RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, letzteDatenzeile))
				.getDataFromRange();
		for (RowData row : data) {
			int nr = row.get(0).getIntVal(-1);
			if (nr < 1) {
				break;
			}
			result.add(nr);
		}
		return result;
	}

	/**
	 * Liest pro Spieltag-Rangliste-Sheet einmalig den Summenblock (Spielernr +
	 * Summenspalten) als RangeData und baut daraus
	 * {@code Map<spielTagNr, Map<spielerNr, SpielerSpieltagErgebnis>>}.
	 */
	private Map<Integer, Map<Integer, SpielerSpieltagErgebnis>> leseSpieltagRanglisteCache(int anzSpieltage)
			throws GenerateException {
		Map<Integer, Map<Integer, SpielerSpieltagErgebnis>> cache = new HashMap<>();
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			SpielTagNr spielTag = SpielTagNr.from(spieltagCntr);
			Map<Integer, SpielerSpieltagErgebnis> proSpieler = new HashMap<>();
			cache.put(spieltagCntr, proSpieler);

			XSpreadsheet spieltagSheet = spieltagRanglisteSheet.getSheet(spielTag);
			if (spieltagSheet == null) {
				continue;
			}
			int ersteSummeSpalte = spieltagRanglisteSheet.getErsteSummeSpalte(spielTag);
			int letzteSummeSpalte = ersteSummeSpalte + ANZAHL_SPALTEN_IN_SUMME - 1;
			int spielerNrSpalteRangliste = SpieltagRanglisteSheet.SPIELER_NR_SPALTE;
			int ersteDatenZeile = SpieltagRanglisteSheet.ERSTE_DATEN_ZEILE;
			int letzteDatenZeile = ersteDatenZeile + 999;

			// Block 1: Spielernr-Spalte
			RangeData spielerNrBlock = RangeHelper
					.from(spieltagSheet, xDoc,
							RangePosition.from(spielerNrSpalteRangliste, ersteDatenZeile,
									spielerNrSpalteRangliste, letzteDatenZeile))
					.getDataFromRange();
			// Block 2: Summenspalten
			RangeData summenBlock = RangeHelper
					.from(spieltagSheet, xDoc,
							RangePosition.from(ersteSummeSpalte, ersteDatenZeile,
									letzteSummeSpalte, letzteDatenZeile))
					.getDataFromRange();

			int rowCount = Math.min(spielerNrBlock.size(), summenBlock.size());
			for (int r = 0; r < rowCount; r++) {
				int spielerNr = spielerNrBlock.get(r).get(0).getIntVal(-1);
				if (spielerNr < 1) {
					break;
				}
				RowData summenRow = summenBlock.get(r);
				int sPlus = summenRow.size() > SPIELE_PLUS_OFFS ? summenRow.get(SPIELE_PLUS_OFFS).getIntVal(-1) : -1;
				if (sPlus < 0) {
					// nuller Spieltag für diesen Spieler in dieser Spieltag-Rangliste
					continue;
				}
				int sMinus = summenRow.size() > SPIELE_MINUS_OFFS ? summenRow.get(SPIELE_MINUS_OFFS).getIntVal(0) : 0;
				int pPlus = summenRow.size() > PUNKTE_PLUS_OFFS ? summenRow.get(PUNKTE_PLUS_OFFS).getIntVal(0) : 0;
				int pMinus = summenRow.size() > PUNKTE_MINUS_OFFS ? summenRow.get(PUNKTE_MINUS_OFFS).getIntVal(0) : 0;

				SpielerSpieltagErgebnis erg = new SpielerSpieltagErgebnis(spielTag, spielerNr);
				erg.setSpielPlus(sPlus).setSpielMinus(sMinus).setPunktePlus(pPlus).setPunkteMinus(pMinus);
				proSpieler.put(spielerNr, erg);
			}
		}
		return cache;
	}

	private List<SpielerSpieltagErgebnis> ergebnisseDesSpielers(
			Map<Integer, Map<Integer, SpielerSpieltagErgebnis>> cache, int anzSpieltage, int spielerNr) {
		List<SpielerSpieltagErgebnis> list = new ArrayList<>(anzSpieltage);
		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			Map<Integer, SpielerSpieltagErgebnis> proSpieler = cache.get(spieltagCntr);
			SpielerSpieltagErgebnis erg = proSpieler != null ? proSpieler.get(spielerNr) : null;
			if (erg == null) {
				// nuller Spieltag – schlechter als jedes gespielte Ergebnis
				SpielerSpieltagErgebnis nuller = new SpielerSpieltagErgebnis(SpielTagNr.from(spieltagCntr), spielerNr);
				nuller.setSpielPlus(-1);
				erg = nuller;
			}
			list.add(erg);
		}
		return list;
	}

	private SpielTagNr ermittleStreichSpieltag(List<SpielerSpieltagErgebnis> ergebnisseDesSpielers) {
		if (ergebnisseDesSpielers.size() < 2) {
			return null;
		}
		List<SpielerSpieltagErgebnis> kopie = new ArrayList<>(ergebnisseDesSpielers);
		kopie.sort(SpielerSpieltagErgebnis::reversedCompareTo);
		return kopie.get(0).getSpielTag();
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

	protected RangListeSorter getRangListeSorter() {
		return rangListeSorter;
	}

	protected RangListeSpalte getRangListeSpalte() {
		return rangListeSpalte;
	}

	protected MeldungenSpalte<SpielerMeldungen, Spieler> getSpielerSpalte() {
		return spielerSpalte;
	}

	protected SpieltagRanglisteSheet getSpieltagRanglisteSheet() {
		return spieltagRanglisteSheet;
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
