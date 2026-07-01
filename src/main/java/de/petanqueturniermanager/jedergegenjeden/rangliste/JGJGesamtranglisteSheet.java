package de.petanqueturniermanager.jedergegenjeden.rangliste;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
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
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncSignaturStore;
import de.petanqueturniermanager.jedergegenjeden.JGJGruppenAufteiler;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJGesamtranglisteSortModus;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteRechner.TeamStats;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Gruppenübergreifende Gesamtrangliste für das JGJ-Turniersystem.
 * <p>
 * Nur sinnvoll (und nur erzeugt) bei mehr als einer Gruppe. Alle Teams werden in
 * einer einzigen Rangliste dargestellt; die Reihenfolge richtet sich nach dem
 * konfigurierten {@link JGJGesamtranglisteSortModus}:
 * <ul>
 *   <li>{@code GRUPPENPLATZ} – „Snake": erst alle Gruppensieger, dann alle Zweiten …</li>
 *   <li>{@code ABSOLUT} – alle Teams gemeinsam nach Siege↓ → Spielpunkte-Diff↓ → Spielpunkte+↓</li>
 * </ul>
 * Eine zusätzliche Spalte zeigt den Gruppenbuchstaben je Team.
 * <p>
 * Aufbau/Verdrahtung analog zur Supermelee-Endrangliste (eigener Menüpunkt,
 * eigener SheetSync-Listener, eigener Metadaten-Schlüssel).
 */
public class JGJGesamtranglisteSheet extends SheetRunner implements ISheet {

	public static final int HEADER_ZEILE = 0;
	public static final int ZWEITE_HEADER_ZEILE = 1;
	public static final int ERSTE_DATEN_ZEILE = 2;

	public static final int TEAM_NR_SPALTE = 0;
	public static final int TEAM_NAME_SPALTE = 1;
	public static final int GRUPPE_SPALTE = 2;
	public static final int PLATZ_SPALTE = 3;
	public static final int SPIELE_PLUS_SPALTE = 4;
	public static final int SPIELE_MINUS_SPALTE = 5;
	public static final int SPIELE_DIFF_SPALTE = 6;
	public static final int SPIELPUNKTE_PLUS_SPALTE = 7;
	public static final int SPIELPUNKTE_MINUS_SPALTE = 8;
	public static final int SPIELPUNKTE_DIFF_SPALTE = 9;

	private static final int COL_WIDTH_NR = 800;
	private static final int COL_WIDTH_NAME = 7000;
	private static final int COL_WIDTH_DATA = 1400;
	/** Beim Daten-Update großzügig geleerter Zeilenbereich (deckt jede Teamanzahl ab). */
	private static final int MAX_LEER_ZEILEN = 400;

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_JGJ_GESAMTRANGLISTE;

	private final JGJKonfigurationSheet konfigurationSheet;
	private final JGJRanglisteRechner rangListeRechner;

	public JGJGesamtranglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ, "JGJ-GesamtranglisteSheet");
		konfigurationSheet = new JGJKonfigurationSheet(workingSpreadsheet);
		rangListeRechner = new JGJRanglisteRechner(workingSpreadsheet);
	}

	@Override
	protected JGJKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				METADATA_SCHLUESSEL, SheetNamen.LEGACY_JGJ_GESAMTRANGLISTE);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
	}

	/**
	 * Vollständiger Neuaufbau (Menüpunkt „Gesamtrangliste"). Bricht mit Hinweis ab,
	 * wenn nur eine Gruppe existiert.
	 */
	public void upDateSheet() throws GenerateException {
		var meldeListe = new JGJMeldeListeSheet_Update(getWorkingSpreadsheet());
		meldeListe.upDateSheet();

		TeamMeldungen aktiveMeldungen = meldeListe.getAktiveMeldungen();
		if (aktiveMeldungen == null || aktiveMeldungen.size() == 0) {
			processBoxinfo("processbox.abbruch");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.jgj.gesamtrangliste"))
					.message(I18n.get("msg.text.ungueltige.anzahl.meldungen")).show();
			return;
		}

		List<TeamMeldungen> gruppen = ermittleGruppen(aktiveMeldungen);
		if (gruppen.size() < 2) {
			processBoxinfo("processbox.abbruch");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
					.caption(I18n.get("msg.caption.jgj.gesamtrangliste"))
					.message(I18n.get("msg.text.jgj.gesamtrangliste.nur.mehrere.gruppen")).show();
			return;
		}

		getxCalculatable().enableAutomaticCalculation(false);

		if (!NewSheet.from(this, SheetNamen.jgjGesamtrangliste(), METADATA_SCHLUESSEL)
				.pos(DefaultSheetPos.JGJ_GESAMTRANGLISTE).setForceCreate(true).setActiv()
				.hideGrid().tabColor(getKonfigurationSheet().getRanglisteTabFarbe())
				.create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer");
			return;
		}

		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			return;
		}

		insertHeader(sheet);
		berechnungUndSchreiben(sheet, meldeListe, aktiveMeldungen, gruppen);

		if (SheetRunner.isRunning()) {
			getSheetHelper().setActiveSheet(sheet);
			SheetRunner.unterdrückeNaechstesSelectionChange();
		}
		commitSignatur();
	}

	protected void commitSignatur() throws GenerateException {
		SheetSyncSignaturStore.commitVollaufbau(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				METADATA_SCHLUESSEL,
				new EingabeSignatur(SignaturQuellen::fuerJGJ));
	}

	protected List<TeamMeldungen> ermittleGruppen(TeamMeldungen aktiveMeldungen) throws GenerateException {
		int gruppengroesse = konfigurationSheet.getGruppengroesse();
		if (gruppengroesse <= 0 || aktiveMeldungen.size() <= gruppengroesse) {
			return List.of(aktiveMeldungen);
		}
		return JGJGruppenAufteiler.teileInGruppen(aktiveMeldungen, gruppengroesse);
	}

	/**
	 * Berechnet die kombinierte Reihenfolge (je Sortmodus) und schreibt den Datenbereich.
	 * Wird vom Vollaufbau und vom {@link JGJGesamtranglisteSheetUpdate} verwendet.
	 */
	protected void berechnungUndSchreiben(XSpreadsheet sheet, JGJMeldeListeSheet_Update meldeListe,
			TeamMeldungen aktiveMeldungen, List<TeamMeldungen> gruppen) throws GenerateException {
		processBoxinfo("processbox.rangliste.einfuegen");

		List<TeamStats> reihenfolge = berechneReihenfolge(gruppen);
		Map<Integer, String> gruppeJeTeam = gruppeJeTeam(gruppen);
		Map<Integer, String> teamNamen = meldeListe.leseTeamNamen();

		insertDaten(sheet, reihenfolge, teamNamen, gruppeJeTeam);

		int letzteZeile = ERSTE_DATEN_ZEILE + reihenfolge.size() - 1;
		if (!reihenfolge.isEmpty()) {
			formatiereZahlenSpalten(sheet, ERSTE_DATEN_ZEILE, reihenfolge.size());
			formatiereZebraStreifen(sheet, ERSTE_DATEN_ZEILE, reihenfolge.size());
		}
		addFooter(sheet, letzteZeile + 2);
		setzeDruckbereich(sheet, letzteZeile + 2);
		getSheetHelper().setOptimaleBreitePlusMarge(sheet, TEAM_NR_SPALTE, SheetHelper.OPTIMALE_BREITE_MARGE);
		SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).anzSpalten(PLATZ_SPALTE + 1).doFreeze();
		getxCalculatable().calculateAll();
	}

	/** Kombiniert die Teams gemäß konfiguriertem Sortmodus zu einer Gesamtreihenfolge. */
	private List<TeamStats> berechneReihenfolge(List<TeamMeldungen> gruppen) throws GenerateException {
		JGJGesamtranglisteSortModus modus = konfigurationSheet.getGesamtranglisteSortModus();
		if (modus == JGJGesamtranglisteSortModus.ABSOLUT) {
			TeamMeldungen alle = new TeamMeldungen();
			for (TeamMeldungen gruppe : gruppen) {
				gruppe.teams().forEach(alle::addTeamWennNichtVorhanden);
			}
			return rangListeRechner.berechneUndSortiere(alle);
		}
		List<List<TeamStats>> sortierteGruppen = new ArrayList<>();
		for (TeamMeldungen gruppe : gruppen) {
			sortierteGruppen.add(rangListeRechner.berechneUndSortiere(gruppe));
		}
		return JGJRanglisteRechner.snakeKombination(sortierteGruppen);
	}

	private static Map<Integer, String> gruppeJeTeam(List<TeamMeldungen> gruppen) {
		Map<Integer, String> map = new HashMap<>();
		for (int g = 0; g < gruppen.size(); g++) {
			String buchstabe = gruppenBuchstabe(g);
			gruppen.get(g).teams().forEach(team -> map.put(team.getNr(), buchstabe));
		}
		return map;
	}

	protected void insertHeader(XSpreadsheet sheet) throws GenerateException {
		Integer headerFarbe = konfigurationSheet.getRanglisteHeaderFarbe();

		int[][] spaltenBreiten = {
				{ TEAM_NAME_SPALTE, COL_WIDTH_NAME },
				{ GRUPPE_SPALTE, COL_WIDTH_NR },
				{ PLATZ_SPALTE, COL_WIDTH_NR },
				{ SPIELE_PLUS_SPALTE, COL_WIDTH_DATA },
				{ SPIELE_MINUS_SPALTE, COL_WIDTH_DATA },
				{ SPIELE_DIFF_SPALTE, COL_WIDTH_DATA },
				{ SPIELPUNKTE_PLUS_SPALTE, COL_WIDTH_DATA },
				{ SPIELPUNKTE_MINUS_SPALTE, COL_WIDTH_DATA },
				{ SPIELPUNKTE_DIFF_SPALTE, COL_WIDTH_DATA },
		};
		for (int[] sw : spaltenBreiten) {
			getSheetHelper().setColumnProperties(sheet, sw[0],
					ColumnProperties.from().setWidth(sw[1])
							.setHoriJustify(CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
		}

		int[] einzelSpalten = { TEAM_NR_SPALTE, TEAM_NAME_SPALTE, GRUPPE_SPALTE, PLATZ_SPALTE };
		String[] einzelTexte = {
				I18n.get("column.header.nr"),
				I18n.get("column.header.name"),
				I18n.get("column.header.jgj.gruppe"),
				I18n.get("column.header.platz"),
		};
		for (int i = 0; i < einzelSpalten.length; i++) {
			int col = einzelSpalten[i];
			var border = col == PLATZ_SPALTE
					? BorderFactory.from().allThin().boldLn().forBottom().forRight().toBorder()
					: BorderFactory.from().allThin().boldLn().forBottom().toBorder();
			var cv = StringCellValue
					.from(sheet, Position.from(col, HEADER_ZEILE), einzelTexte[i])
					.setCellBackColor(headerFarbe)
					.setBorder(border)
					.setHoriJustify(CellHoriJustify.CENTER)
					.setVertJustify(CellVertJustify2.CENTER)
					.setEndPosMergeZeilePlus(1)
					.setShrinkToFit(true);
			if (col == PLATZ_SPALTE) {
				cv.setRotate90().setCharWeight(FontWeight.BOLD);
			}
			getSheetHelper().setStringValueInCell(cv);
		}

		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(SPIELE_PLUS_SPALTE, HEADER_ZEILE), I18n.get("column.header.spiele"))
				.setCellBackColor(headerFarbe)
				.setBorder(BorderFactory.from().allThin().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER)
				.setEndPosMergeSpalte(SPIELE_DIFF_SPALTE)
				.setShrinkToFit(true));

		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(SPIELPUNKTE_PLUS_SPALTE, HEADER_ZEILE), I18n.get("column.header.punkte"))
				.setCellBackColor(headerFarbe)
				.setBorder(BorderFactory.from().allThin().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER)
				.setEndPosMergeSpalte(SPIELPUNKTE_DIFF_SPALTE)
				.setShrinkToFit(true));

		int[] subCols = {
				SPIELE_PLUS_SPALTE, SPIELE_MINUS_SPALTE, SPIELE_DIFF_SPALTE,
				SPIELPUNKTE_PLUS_SPALTE, SPIELPUNKTE_MINUS_SPALTE, SPIELPUNKTE_DIFF_SPALTE,
		};
		String[] subTexte = {
				I18n.get("schweizer.rangliste.spalte.punkte.plus"),
				I18n.get("schweizer.rangliste.spalte.punkte.minus"),
				I18n.get("schweizer.rangliste.spalte.punkte.differenz"),
				I18n.get("schweizer.rangliste.spalte.punkte.plus"),
				I18n.get("schweizer.rangliste.spalte.punkte.minus"),
				I18n.get("schweizer.rangliste.spalte.punkte.differenz"),
		};
		for (int i = 0; i < subCols.length; i++) {
			getSheetHelper().setStringValueInCell(StringCellValue
					.from(sheet, Position.from(subCols[i], ZWEITE_HEADER_ZEILE), subTexte[i])
					.setCellBackColor(headerFarbe)
					.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
					.setHoriJustify(CellHoriJustify.CENTER)
					.setShrinkToFit(true));
		}
	}

	private void insertDaten(XSpreadsheet sheet, List<TeamStats> reihenfolge,
			Map<Integer, String> teamNamen, Map<Integer, String> gruppeJeTeam) throws GenerateException {
		leereDatenBereich(sheet);
		if (reihenfolge.isEmpty()) {
			return;
		}
		int letzteZeile = ERSTE_DATEN_ZEILE + reihenfolge.size() - 1;

		// Block 1: Nr, Name, Gruppe, Platz
		RangeData block1 = new RangeData();
		for (int i = 0; i < reihenfolge.size(); i++) {
			TeamStats stats = reihenfolge.get(i);
			RowData row = block1.addNewRow();
			row.newInt(stats.teamNr());
			row.newString(teamNamen.getOrDefault(stats.teamNr(), ""));
			row.newString(gruppeJeTeam.getOrDefault(stats.teamNr(), ""));
			row.newInt(i + 1);
		}
		RangeHelper.from(this, RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, PLATZ_SPALTE, letzteZeile))
				.setDataInRange(block1);

		// Block 2: Spiele+, Spiele-, SpieleΔ, SpPunkte+, SpPunkte-, SpPunkteΔ
		RangeData block2 = new RangeData();
		for (TeamStats stats : reihenfolge) {
			RowData row = block2.addNewRow();
			row.newInt(stats.spielePlus());
			row.newInt(stats.spieleMinus());
			row.newInt(stats.spielDiff());
			row.newInt(stats.spielPunktePlus());
			row.newInt(stats.spielPunkteMinus());
			row.newInt(stats.spielPunkteDiff());
		}
		RangeHelper.from(this, block2.getRangePosition(Position.from(SPIELE_PLUS_SPALTE, ERSTE_DATEN_ZEILE)))
				.setDataInRange(block2);

		// Nr-Spalte: grau + doppelte rechte Linie + zentriert
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE, letzteZeile),
				CellProperties.from()
						.margin(MeldeListeKonstanten.CELL_MARGIN)
						.setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR)
						.setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder())
						.centerJustify());

		// Name-Spalte: linksbündig
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(TEAM_NAME_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NAME_SPALTE, letzteZeile),
				CellProperties.from().margin(MeldeListeKonstanten.CELL_MARGIN).setAllThinBorder()
						.setHoriJustify(CellHoriJustify.LEFT));

		// Gruppe-Spalte: zentriert
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(GRUPPE_SPALTE, ERSTE_DATEN_ZEILE, GRUPPE_SPALTE, letzteZeile),
				CellProperties.from().margin(MeldeListeKonstanten.CELL_MARGIN).setAllThinBorder()
						.setHoriJustify(CellHoriJustify.CENTER));
	}

	/** Leert den Datenbereich (Werte) für ein sauberes Daten-Update. */
	private void leereDatenBereich(XSpreadsheet sheet) throws GenerateException {
		RangeData leer = new RangeData();
		for (int i = 0; i < MAX_LEER_ZEILEN; i++) {
			RowData row = leer.addNewRow();
			for (int c = TEAM_NR_SPALTE; c <= SPIELPUNKTE_DIFF_SPALTE; c++) {
				row.newString("");
			}
		}
		RangeHelper.from(this, RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE,
				SPIELPUNKTE_DIFF_SPALTE, ERSTE_DATEN_ZEILE + MAX_LEER_ZEILEN - 1)).setDataInRange(leer);
	}

	private void formatiereZahlenSpalten(XSpreadsheet sheet, int startZeile, int anzTeams)
			throws GenerateException {
		int letzteZeile = startZeile + anzTeams - 1;
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(PLATZ_SPALTE, startZeile, SPIELPUNKTE_DIFF_SPALTE, letzteZeile),
				CellProperties.from()
						.margin(MeldeListeKonstanten.CELL_MARGIN)
						.setAllThinBorder()
						.setHoriJustify(CellHoriJustify.CENTER)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()));

		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(PLATZ_SPALTE, startZeile, PLATZ_SPALTE, letzteZeile),
				CellProperties.from()
						.margin(MeldeListeKonstanten.CELL_MARGIN)
						.setCharWeight(FontWeight.BOLD)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forRight().toBorder()));
	}

	private void formatiereZebraStreifen(XSpreadsheet sheet, int startZeile, int anzTeams)
			throws GenerateException {
		int letzteZeile = startZeile + anzTeams - 1;
		RangePosition datenRange = RangePosition.from(
				TEAM_NR_SPALTE, startZeile, SPIELPUNKTE_DIFF_SPALTE, letzteZeile);
		RanglisteGeradeUngeradeFormatHelper.from(this, datenRange)
				.geradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeGerade())
				.ungeradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeUnGerade())
				.apply();
	}

	private void addFooter(XSpreadsheet sheet, int fusszeile) throws GenerateException {
		processBoxinfo("processbox.fusszeile.einfuegen");
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(TEAM_NR_SPALTE, fusszeile), I18n.get("jgj.rangliste.fusszeile"))
				.setHoriJustify(CellHoriJustify.LEFT)
				.setCharHeight(8)
				.setEndPosMergeSpalte(SPIELPUNKTE_DIFF_SPALTE));
	}

	private void setzeDruckbereich(XSpreadsheet sheet, int letzteZeile) throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		PrintArea.from(sheet, getWorkingSpreadsheet()).setPrintArea(
				RangePosition.from(TEAM_NR_SPALTE, HEADER_ZEILE, SPIELPUNKTE_DIFF_SPALTE, letzteZeile));
	}

	private static String gruppenBuchstabe(int index) {
		return String.valueOf((char) ('A' + index));
	}
}
