package de.petanqueturniermanager.triptete.rangliste;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.awt.FontWeight;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;

/**
 * Trip-Tête-Rangliste – 3-zeiliger Summen-Header analog zur Liga-Rangliste.
 * <p>
 * Spaltenstruktur: Nr | Name | Platz | BegGew+ | BegVer- | PartGew+ | PartVer- | PartΔ |
 * SpPnkt+ | SpPnkt- | SpPnktΔ | Begegnungen.
 */
public class TripTeteRanglisteSheet extends SheetRunner implements ISheet {

	private static final int MARGIN = 120;
	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE;

	public static final int ERSTE_DATEN_ZEILE = 3;

	public static final int TEAM_NR_SPALTE   = 0;
	public static final int NAME_SPALTE      = 1;
	public static final int RANG_SPALTE      = 2;

	public static final int ERSTE_SUMMEN_SPALTE   = 3;
	public static final int BEG_GEW_SPALTE        = 3;  // Punkte+
	public static final int BEG_VER_SPALTE        = 4;  // Punkte-
	public static final int PART_GEW_SPALTE       = 5;  // Spiele+
	public static final int PART_VER_SPALTE       = 6;  // Spiele-
	public static final int PART_DIFF_SPALTE      = 7;  // SpieleΔ
	public static final int SP_PUNKTE_PLUS_SPALTE  = 8;  // Spielpunkte+
	public static final int SP_PUNKTE_MINUS_SPALTE = 9;  // Spielpunkte-
	public static final int SP_PUNKTE_DIFF_SPALTE  = 10; // SpielPunkteΔ
	public static final int BEGEGNUNGEN_SPALTE     = 11;
	public static final int LETZTE_SPALTE          = BEGEGNUNGEN_SPALTE;

	private final TripTeteKonfigurationSheet konfigurationSheet;
	private final TripTeteMeldeListeSheetUpdate meldeListe;

	public TripTeteRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.TRIPTETE, "Trip-Tête-RanglisteSheet");
		konfigurationSheet = new TripTeteKonfigurationSheet(workingSpreadsheet);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
	}

	@Override
	protected TripTeteKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@VisibleForTesting
	TripTeteMeldeListeSheetUpdate initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new TripTeteMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL,
				SheetNamen.LEGACY_RANGLISTE);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
	}

	public void upDateSheet() throws GenerateException {
		meldeListe.upDateSheet();

		getxCalculatable().enableAutomaticCalculation(false);
		try {
			TeamMeldungen meldungen = meldeListe.getAlleMeldungen();
			if (!meldungen.isValid()) {
				processBoxinfo("processbox.abbruch");
				MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
						.caption(I18n.get("msg.caption.triptete.rangliste"))
						.message(I18n.get("msg.text.ungueltige.anzahl.meldungen")).show();
				return;
			}

			if (!NewSheet.from(this, SheetNamen.rangliste(), METADATA_SCHLUESSEL)
					.pos(DefaultSheetPos.TRIPTETE_ENDRANGLISTE).setForceCreate(true).setActiv()
					.hideGrid().tabColor(konfigurationSheet.getRanglisteTabFarbe()).create().isDidCreate()) {
				ProcessBox.from().info("Abbruch vom Benutzer, Trip-Tête-Rangliste wurde nicht erstellt");
				return;
			}

			insertHeader();
			TripTeteRanglisteDatenSchreiber.from(this, meldeListe, getWorkingSpreadsheet()).schreibeDaten();
			insertFooter(meldungen.size());
			formatieren(meldungen.size());
			printBereichDefinieren(meldungen.size());
			SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).anzSpalten(3).doFreeze();
		} finally {
			getxCalculatable().enableAutomaticCalculation(true);
		}
	}

	private void insertHeader() throws GenerateException {
		int headerBackColor = konfigurationSheet.getRanglisteHeaderFarbe();

		BorderFactory borderFact = BorderFactory.from().allThin().boldLn().forBottom();
		TableBorder2 brdDaten = borderFact.toBorder();
		CellProperties headerProp = CellProperties.from().setAllThinBorder().margin(MARGIN).centerJustify()
				.setCellBackColor(headerBackColor).setShrinkToFit(true);
		ColumnProperties colSchmal = ColumnProperties.from().setWidth(900).centerJustify();

		// Nr – überspannt Header-Zeilen 1 und 2 (analog Liga-MeldungenSpalte)
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(getXSpreadSheet(), Position.from(TEAM_NR_SPALTE, 1))
						.setColumnProperties(colSchmal)
						.setValue(I18n.get("column.header.nr"))
						.setCellBackColor(headerBackColor)
						.setBorder(brdDaten)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setEndPosMergeZeilePlus(1)
						.setShrinkToFit(true));

		// Name – überspannt Header-Zeilen 1 und 2, identisch wie Nr
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(getXSpreadSheet(), Position.from(NAME_SPALTE, 1))
						.setColumnProperties(colSchmal)
						.setValue(I18n.get("column.header.name"))
						.setCellBackColor(headerBackColor)
						.setBorder(brdDaten)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setEndPosMergeZeilePlus(1)
						.setShrinkToFit(true));

		// Platz – überspannt alle 3 Header-Zeilen, hochkant
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(getXSpreadSheet(), Position.from(RANG_SPALTE, 0))
						.setColumnProperties(colSchmal)
						.setValue(I18n.get("column.header.platz"))
						.setRotate90()
						.setVertJustify(CellVertJustify2.CENTER)
						.setCellBackColor(headerBackColor)
						.setBorder(brdDaten)
						.setEndPosMergeZeilePlus(2)
						.setShrinkToFit(true)
						.setCharWeight(FontWeight.BOLD));

		// ---- Header-Zeile 3 (unterste): +/- Δ Labels ----
		RangeData zeile3Data = new RangeData();
		RowData zeile3 = zeile3Data.addNewRow();
		zeile3.newString("+"); zeile3.newString("-");                    // Punkte
		zeile3.newString("+"); zeile3.newString("-"); zeile3.newString("Δ"); // Spiele + Diff
		zeile3.newString("+"); zeile3.newString("-"); zeile3.newString("Δ"); // Spielpunkte + Diff

		RangeHelper.from(this, zeile3Data.getRangePosition(Position.from(ERSTE_SUMMEN_SPALTE, 2)))
				.setDataInRange(zeile3Data)
				.setRangeProperties(RangeProperties.from().centerJustify().setBorder(brdDaten)
						.setCellBackColor(headerBackColor).margin(MARGIN).setShrinkToFit(true));

		// ---- Header-Zeile 2: Punkte / Spiele / Spielpunkte ----
		StringCellValue header2val = StringCellValue.from(getXSpreadSheet())
				.setPos(Position.from(ERSTE_SUMMEN_SPALTE, 1))
				.setEndPosMergeSpaltePlus(1).setCellProperties(headerProp);

		header2val.setValue(I18n.get("column.header.punkte"));
		getSheetHelper().setStringValueInCell(header2val);
		header2val.spaltePlus(2);

		header2val.setValue(I18n.get("column.header.spiele")).setEndPosMergeSpaltePlus(2);
		getSheetHelper().setStringValueInCell(header2val);
		header2val.spaltePlus(3);

		header2val.setValue(I18n.get("column.header.spielpunkte")).setEndPosMergeSpaltePlus(2);
		getSheetHelper().setStringValueInCell(header2val);

		// ---- Header-Zeile 1 (oberste): Summen + Begegn. ----
		StringCellValue header1val = StringCellValue.from(getXSpreadSheet())
				.setPos(Position.from(ERSTE_SUMMEN_SPALTE, 0))
				.setEndPosMergeSpaltePlus(7).setCellProperties(headerProp);
		header1val.setValue(I18n.get("column.header.summen"));
		getSheetHelper().setStringValueInCell(header1val);

		// Begegn. – überspannt alle 3 Zeilen, hochkant
		TableBorder2 begegnungenBrd = BorderFactory.from(borderFact).doubleLn().forLeft().toBorder();
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(getXSpreadSheet()).setPos(Position.from(BEGEGNUNGEN_SPALTE, 0))
						.setValue(I18n.get("column.header.begegn"))
						.setRotate90()
						.setEndPosMergeZeilePlus(2)
						.centerJustify()
						.setBorder(begegnungenBrd)
						.setCellBackColor(headerBackColor)
						.setShrinkToFit(true)
						.setComment(I18n.get("comment.begegnungen")));
	}

	private void insertFooter(int anzTeams) throws GenerateException {
		int footerZeile = ERSTE_DATEN_ZEILE + anzTeams + 1;
		StringCellValue stVal = StringCellValue.from(this, Position.from(TEAM_NR_SPALTE, footerZeile))
				.setHoriJustify(CellHoriJustify.LEFT).setCharHeight(8);
		getSheetHelper().setStringValueInCell(
				stVal.setValue(I18n.get("triptete.rangliste.reihenfolge.platzierung")));
	}

	private void formatieren(int anzTeams) throws GenerateException {
		int letzteDatenZeile = ERSTE_DATEN_ZEILE + anzTeams - 1;
		RangePosition daten = RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, LETZTE_SPALTE, letzteDatenZeile);
		RangeHelper.from(this, daten).setRangeProperties(
				RangeProperties.from().setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder())
						.centerJustify().margin(MARGIN).setShrinkToFit(true));

		Integer farbeGerade = konfigurationSheet.getRanglisteHintergrundFarbeGerade();
		Integer farbeUngerade = konfigurationSheet.getRanglisteHintergrundFarbeUnGerade();
		RanglisteGeradeUngeradeFormatHelper.from(this, daten).geradeFarbe(farbeGerade)
				.ungeradeFarbe(farbeUngerade).apply();
	}

	private void printBereichDefinieren(int anzTeams) throws GenerateException {
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(
				RangePosition.from(0, 0, LETZTE_SPALTE, ERSTE_DATEN_ZEILE + anzTeams + 1));
	}

	public TripTeteMeldeListeSheetUpdate getMeldeListe() {
		return meldeListe;
	}
}
