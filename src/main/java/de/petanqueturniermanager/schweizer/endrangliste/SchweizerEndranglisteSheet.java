/*
* Erstellung : 18.08.2026 / Michael Massee
**/

package de.petanqueturniermanager.schweizer.endrangliste;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.turnierserie.TurnierserieAggregator;
import de.petanqueturniermanager.algorithmen.turnierserie.ergebnis.TeamEndranglisteErgebnis;
import de.petanqueturniermanager.algorithmen.turnierserie.ergebnis.TeamSpieltagErgebnis;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

/**
 * Serien-Endrangliste für das Schweizer System: aggregiert die archivierten Spieltag-Ranglisten
 * (siehe {@link de.petanqueturniermanager.schweizer.spieltagrangliste.SchweizerSpieltagRanglisteSheet})
 * mittels {@link TurnierserieAggregator}. Team-Nr bleibt über die Serie stabil; fehlt ein Team an
 * einem Spieltag, wird das als „nicht teilgenommen" behandelt statt die Berechnung abzubrechen.
 */
public class SchweizerEndranglisteSheet extends SheetRunner implements ISheet {

	public static final int HEADER_ZEILE = 0;
	public static final int ERSTE_DATEN_ZEILE = 1;

	public static final int TEAM_NR_SPALTE = 0;
	public static final int TEAM_NAME_SPALTE = 1;
	public static final int PLATZ_SPALTE = 2;
	public static final int SIEGE_SPALTE = 3;
	public static final int PUNKTE_PLUS_SPALTE = 4;
	public static final int PUNKTE_MINUS_SPALTE = 5;
	public static final int PUNKTE_DIFF_SPALTE = 6;
	public static final int ANZ_SPIELTAGE_SPALTE = 7;
	public static final int STREICH_SPIELTAG_SPALTE = 8;

	private final SchweizerKonfigurationSheet konfigurationSheet;

	public SchweizerEndranglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER, SheetNamen.endrangliste());
		konfigurationSheet = new SchweizerKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected SchweizerKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_ENDRANGLISTE, SheetNamen.endrangliste());
	}

	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public void doRun() throws GenerateException {
		int anzahlSpieltage = countAnzahlSpieltage();
		if (anzahlSpieltage < 2) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.fehler"))
					.message(I18n.get("msg.text.ungueltige.anzahl.spieltage", anzahlSpieltage)).show();
			return;
		}

		NewSheet.from(this, SheetNamen.endrangliste(), SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_ENDRANGLISTE)
				.pos(DefaultSheetPos.SCHWEIZER_ENDRANGLISTE).tabColor(konfigurationSheet.getRanglisteTabFarbe())
				.hideGrid().forceCreate().create();

		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			return;
		}

		Map<Integer, Map<Integer, TeamSpieltagErgebnis>> cache = new HashMap<>();
		Map<Integer, String> teamNamen = new HashMap<>();
		for (int spieltagNr = 1; spieltagNr <= anzahlSpieltage; spieltagNr++) {
			SpielTagNr spieltag = SpielTagNr.from(spieltagNr);
			cache.put(spieltagNr, leseSpieltagRangliste(spieltag, teamNamen));
		}

		Map<Integer, TeamEndranglisteErgebnis> endrangliste = TurnierserieAggregator.berechneEndrangliste(cache,
				anzahlSpieltage, false);

		List<TeamEndranglisteErgebnis> sortiert = new ArrayList<>(endrangliste.values());
		sortiert.sort(null); // natuerliche Ordnung: bestes Team zuerst (siehe TeamEndranglisteErgebnis.compareTo)

		insertHeader(sheet);
		insertDaten(sheet, sortiert, teamNamen);

		SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).anzSpalten(3).doFreeze();
		PrintArea.from(sheet, getWorkingSpreadsheet()).setPrintArea(RangePosition.from(TEAM_NR_SPALTE, HEADER_ZEILE,
				STREICH_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE + Math.max(0, sortiert.size() - 1)));
	}

	/** Zählt die vorhandenen archivierten Spieltag-Ranglisten (bricht bei der ersten Lücke ab). */
	private int countAnzahlSpieltage() throws GenerateException {
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		int anzahl = 0;
		for (int spieltagNr = 1; spieltagNr <= 90; spieltagNr++) {
			XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
					SheetMetadataHelper.schluesselSpieltagRangliste(spieltagNr), SheetNamen.spieltagRangliste(spieltagNr));
			if (sheet == null) {
				break;
			}
			anzahl++;
		}
		return anzahl;
	}

	/** Zählt die für einen Spieltag gespielten Runden (bricht bei der ersten Lücke ab). */
	private int countAnzahlRunden(SpielTagNr spieltag) throws GenerateException {
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		int anzahl = 0;
		for (int rundeNr = 1; rundeNr <= 90; rundeNr++) {
			String schluessel = spieltag.getNr() <= 1
					? SheetMetadataHelper.schluesselSchweizerSpielrunde(rundeNr)
					: SheetMetadataHelper.schluesselSchweizerSpielrundeSpieltag(spieltag.getNr(), rundeNr);
			String legacyName = spieltag.getNr() <= 1
					? SheetNamen.spielrunde(rundeNr)
					: SheetNamen.supermeleeSpielrunde(spieltag.getNr(), rundeNr);
			XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc, schluessel, legacyName);
			if (sheet == null) {
				break;
			}
			anzahl++;
		}
		return anzahl;
	}

	private Map<Integer, TeamSpieltagErgebnis> leseSpieltagRangliste(SpielTagNr spieltag,
			Map<Integer, String> teamNamenSammler) throws GenerateException {
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
				SheetMetadataHelper.schluesselSpieltagRangliste(spieltag.getNr()),
				SheetNamen.spieltagRangliste(spieltag.getNr()));

		Map<Integer, TeamSpieltagErgebnis> ergebnisse = new HashMap<>();
		if (sheet == null) {
			return ergebnisse;
		}

		int anzahlRunden = countAnzahlRunden(spieltag);
		RangePosition leseBereich = RangePosition.from(SchweizerRanglisteSheet.TEAM_NR_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE, SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + 999);
		RangeData daten = RangeHelper.from(sheet, xDoc, leseBereich).getDataFromRange();

		for (RowData row : daten) {
			if (row.size() <= SchweizerRanglisteSheet.PUNKTE_MINUS_SPALTE) {
				break;
			}
			int teamNr = row.get(SchweizerRanglisteSheet.TEAM_NR_SPALTE).getIntVal(-1);
			if (teamNr <= 0) {
				break;
			}
			String teamName = row.get(SchweizerRanglisteSheet.TEAM_NAME_SPALTE).getStringVal();
			if (teamName != null && !teamName.isEmpty()) {
				teamNamenSammler.putIfAbsent(teamNr, teamName);
			}
			int siege = row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(0);
			int punktePlus = row.get(SchweizerRanglisteSheet.PUNKTE_PLUS_SPALTE).getIntVal(0);
			int punkteMinus = row.get(SchweizerRanglisteSheet.PUNKTE_MINUS_SPALTE).getIntVal(0);

			TeamSpieltagErgebnis ergebnis = new TeamSpieltagErgebnis(spieltag, teamNr).setSpielPlus(siege)
					.setSpielMinus(Math.max(0, anzahlRunden - siege)).setPunktePlus(punktePlus)
					.setPunkteMinus(punkteMinus);
			ergebnisse.put(teamNr, ergebnis);
		}
		return ergebnisse;
	}

	private void insertHeader(XSpreadsheet sheet) throws GenerateException {
		Integer headerColor = konfigurationSheet.getMeldeListeHeaderFarbe();
		String[] texte = { I18n.get("column.header.nr"), I18n.get("column.header.teamname"),
				I18n.get("column.header.platz"), I18n.get("column.header.siege"),
				I18n.get("schweizer.rangliste.spalte.punkte.plus"), I18n.get("schweizer.rangliste.spalte.punkte.minus"),
				I18n.get("schweizer.rangliste.spalte.punkte.differenz"), I18n.get("schweizer.endrangliste.spalte.anz.spieltage"),
				I18n.get("schweizer.endrangliste.spalte.streich.spieltag") };
		for (int col = 0; col < texte.length; col++) {
			StringCellValue cv = StringCellValue.from(sheet, Position.from(col, HEADER_ZEILE), texte[col])
					.setCellBackColor(headerColor).setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
					.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
					.setCharWeight(FontWeight.BOLD).setShrinkToFit(true);
			getSheetHelper().setStringValueInCell(cv);
		}
	}

	private void insertDaten(XSpreadsheet sheet, List<TeamEndranglisteErgebnis> sortiert,
			Map<Integer, String> teamNamen) throws GenerateException {
		if (sortiert.isEmpty()) {
			return;
		}
		RangeData block = new RangeData();
		int platz = 1;
		for (TeamEndranglisteErgebnis erg : sortiert) {
			RowData row = block.addNewRow();
			row.newInt(erg.getTeamNr());
			row.newString(teamNamen.getOrDefault(erg.getTeamNr(), ""));
			row.newInt(platz++);
			row.newInt(erg.getSpielPlus());
			row.newInt(erg.getPunktePlus());
			row.newInt(erg.getPunkteMinus());
			row.newInt(erg.getPunkteDiv());
			row.newInt(erg.getAnzGespielteSpieltage());
			row.newInt(erg.getStreichSpieltag() != null ? erg.getStreichSpieltag().getNr() : 0);
		}
		RangeHelper.from(this, block.getRangePosition(Position.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE)))
				.setDataInRange(block);

		int letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() - 1;
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, STREICH_SPIELTAG_SPALTE, letzteZeile),
				CellProperties.from().centerJustify());

		RanglisteGeradeUngeradeFormatHelper.from(this,
				RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, STREICH_SPIELTAG_SPALTE, letzteZeile))
				.geradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeGerade())
				.ungeradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeUnGerade())
				.validateSpalte(-1)
				.apply();
	}

}
