package de.petanqueturniermanager.jedergegenjeden.spielplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.toolbar.TurnierModus;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.model.LigaSpielPlan;
import de.petanqueturniermanager.jedergegenjeden.JGJGruppenAufteiler;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public class JGJSpielPlanSheet extends SheetRunner implements ISheet {

	public static final String LEGACY_SHEET_NAMEN = SheetNamen.LEGACY_SPIELPLAN;

	public static String sheetName() {
		return SheetNamen.spielplan();
	}
	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN;

	private static final int ERSTE_SPIELTAG_HEADER_ZEILE = 0;
	public static final int ERSTE_SPIELTAG_DATEN_ZEILE = ERSTE_SPIELTAG_HEADER_ZEILE + 2; // Zeile 2
	public static final int SPIEL_NR_SPALTE = 0;
	private static final int NAME_A_SPALTE = SPIEL_NR_SPALTE + 1;
	private static final int NAME_B_SPALTE = NAME_A_SPALTE + 1;
	public static final int SPIELPNKT_A_SPALTE = NAME_B_SPALTE + 1;
	public static final int SPIELPNKT_B_SPALTE = SPIELPNKT_A_SPALTE + 1;
	public static final int EINGABE_VALIDIERUNG_SPALTE = SPIELPNKT_B_SPALTE + 1;

	private static final int PUNKTE_NR_WIDTH = AbstractSuperMeleeRanglisteFormatter.ENDSUMME_NUMBER_WIDTH;

	static final String NR_HINRUNDE_PREFIX = "HR-";
	static final String NR_RUECKRUNDE_PREFIX = "RR-";

	// Arbeitsspalten
	public static final int TEAM_A_NR_SPALTE = 14;
	public static final int TEAM_B_NR_SPALTE = TEAM_A_NR_SPALTE + 1;
	public static final int SPIELE_A_SPALTE = TEAM_A_NR_SPALTE - 2;
	public static final int SPIELE_B_SPALTE = SPIELE_A_SPALTE + 1;

	private final JGJKonfigurationSheet konfigurationSheet;
	private final JGJMeldeListeSheet_Update meldeListe;

	public JGJSpielPlanSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ);
		konfigurationSheet = new JGJKonfigurationSheet(workingSpreadsheet);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
	}

	@Override
	protected JGJKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@VisibleForTesting
	JGJMeldeListeSheet_Update initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new JGJMeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, LEGACY_SHEET_NAMEN);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.JGJ)
					.ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
		}
		meldeListe.vollstaendigAktualisieren();
		TeamMeldungen aktiveMeldungen = ladeAktiveMeldungen();
		if (aktiveMeldungen != null) {
			generate(aktiveMeldungen);
		}
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.JGJ)
					.ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
		}
	}

	private TeamMeldungen ladeAktiveMeldungen() throws GenerateException {
		TeamMeldungen aktiveMeldungen = meldeListe.getAktiveMeldungen();
		if (aktiveMeldungen.size() > 0) {
			return aktiveMeldungen;
		}
		TeamMeldungen alleMeldungen = meldeListe.getAlleMeldungen();
		if (alleMeldungen.size() == 0) {
			return aktiveMeldungen;
		}
		MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
				.caption(I18n.get("msg.caption.keine.aktiven.meldungen"))
				.message(I18n.get("msg.text.keine.aktiven.teams.aktivieren", alleMeldungen.size()))
				.show();
		if (result == MessageBoxResult.YES) {
			meldeListe.alleTeamsAktivieren();
			return meldeListe.getAktiveMeldungen();
		}
		return null;
	}

	public void generate(TeamMeldungen meldungen) throws GenerateException {
		int gruppengroesse = getKonfigurationSheet().getGruppengroesse();
		boolean gruppenModus = gruppengroesse > 0 && meldungen.size() > gruppengroesse;

		if (gruppenModus) {
			if (meldungen.size() < 2) {
				zeigeUngueltigeMeldungenFehler();
				return;
			}
		} else {
			if (!meldungen.isValid()) {
				zeigeUngueltigeMeldungenFehler();
				return;
			}
		}

		if (!NewSheet.from(this, sheetName(), METADATA_SCHLUESSEL)
				.pos(DefaultSheetPos.JGJ_WORK).setForceCreate(true).setActiv().hideGrid()
				.tabColor(getKonfigurationSheet().getSpielrundeTabFarbe()).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Jeder gegen Jeden SpielPlan wurde nicht erstellt");
			return;
		}

		insertDatenHeaderUndSpalteBreite();

		boolean mitRueckrunde = getKonfigurationSheet().isRueckrunde();

		if (gruppenModus) {
			generiereGruppenSpielplan(meldungen, gruppengroesse, mitRueckrunde);
		} else {
			var ligaSpielPlan = new LigaSpielPlan(meldungen);
			var spielPlanHRunde = ligaSpielPlan.schufflePlan().getSpielPlanClone();
			var spielPlanRRunde = mitRueckrunde
					? ligaSpielPlan.flipTeams().getSpielPlanClone()
					: List.<List<TeamPaarung>>of();
			insertSpieltageDaten(spielPlanHRunde, spielPlanRRunde, ERSTE_SPIELTAG_DATEN_ZEILE);
			insertArbeitsspalten(spielPlanHRunde, spielPlanRRunde, ERSTE_SPIELTAG_DATEN_ZEILE);
			insertFormulaPunkte();
			insertFormulaTeamNamen();
			insertFormulaValidierung();
			formatieren(spielPlanHRunde, spielPlanRRunde, ERSTE_SPIELTAG_DATEN_ZEILE);
		}

		printBereichDefinieren();
		SheetFreeze.from(getTurnierSheet()).anzZeilen(2).doFreeze();
	}

	private void zeigeUngueltigeMeldungenFehler() throws GenerateException {
		processBoxinfo("processbox.abbruch");
		MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
				.caption(I18n.get("msg.caption.jgj.spielplan"))
				.message(I18n.get("msg.text.ungueltige.anzahl.meldungen")).show();
	}

	private void generiereGruppenSpielplan(TeamMeldungen meldungen, int gruppengroesse, boolean mitRueckrunde) throws GenerateException {
		List<TeamMeldungen> gruppen = JGJGruppenAufteiler.teileInGruppen(meldungen, gruppengroesse);
		List<Integer> gruppenHeaderZeilen = new ArrayList<>();
		List<Integer> gruppenStartZeilen = new ArrayList<>();
		List<List<List<TeamPaarung>>> gruppenSpielplaeneH = new ArrayList<>();
		List<List<List<TeamPaarung>>> gruppenSpielplaeneR = new ArrayList<>();
		int aktuelleZeile = ERSTE_SPIELTAG_DATEN_ZEILE;

		for (int g = 0; g < gruppen.size(); g++) {
			TeamMeldungen gruppe = gruppen.get(g);
			gruppenHeaderZeilen.add(aktuelleZeile);
			schreibeGruppenNrZeile(aktuelleZeile);
			int gruppeStartZeile = aktuelleZeile + 1;
			gruppenStartZeilen.add(gruppeStartZeile);

			var ligaSpielPlan = new LigaSpielPlan(gruppe);
			var spielPlanHRunde = ligaSpielPlan.schufflePlan().getSpielPlanClone();
			var spielPlanRRunde = mitRueckrunde
					? ligaSpielPlan.flipTeams().getSpielPlanClone()
					: List.<List<TeamPaarung>>of();

			gruppenSpielplaeneH.add(spielPlanHRunde);
			gruppenSpielplaeneR.add(spielPlanRRunde);

			insertSpieltageDaten(spielPlanHRunde, spielPlanRRunde, gruppeStartZeile);
			insertArbeitsspalten(spielPlanHRunde, spielPlanRRunde, gruppeStartZeile);

			int anzSpiele = anzahlSpiele(spielPlanHRunde, spielPlanRRunde);
			aktuelleZeile = gruppeStartZeile + anzSpiele;
		}

		// Formeln erst nach allen Dateneintragungen einfügen, damit fillAuto die
		// danach folgende Zebra-Formatierung nicht überschreibt
		insertFormulaPunkte();
		insertFormulaTeamNamen();
		insertFormulaValidierung();

		for (int g = 0; g < gruppen.size(); g++) {
			formatieren(gruppenSpielplaeneH.get(g), gruppenSpielplaeneR.get(g), gruppenStartZeilen.get(g));
		}

		for (int g = 0; g < gruppen.size(); g++) {
			schreibeGruppenHeaderBeschriftung(gruppenHeaderZeilen.get(g), gruppenBuchstabe(g));
		}
	}

	private void schreibeGruppenNrZeile(int zeile) throws GenerateException {
		// Team-NR-Arbeitsspalten auf 0 setzen (damit Formeln leere Zellen erzeugen)
		RangeData data = new RangeData();
		RowData row = data.addNewRow();
		row.newInt(0);
		row.newInt(0);
		Position startPos = Position.from(TEAM_A_NR_SPALTE, zeile);
		RangeHelper.from(this, data.getRangePosition(startPos)).setDataInRange(data);
	}

	private void schreibeGruppenHeaderBeschriftung(int zeile, String buchstabe) throws GenerateException {
		String gruppenName = I18n.get("jgj.gruppe.name") + " " + buchstabe;
		StringCellValue headerVal = StringCellValue
				.from(getXSpreadSheet(), Position.from(SPIEL_NR_SPALTE, zeile), gruppenName)
				.setEndPosMergeSpalte(SPIELPNKT_B_SPALTE)
				.setCellBackColor(getKonfigurationSheet().getSpielPlanHeaderFarbe())
				.setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
		getSheetHelper().setStringValueInCell(headerVal);
	}

	private static int anzahlSpiele(List<List<TeamPaarung>> spielPlanHRunde, List<List<TeamPaarung>> spielPlanRRunde) {
		int anzRunden = spielPlanHRunde.size();
		int anzPaarungen = spielPlanHRunde.isEmpty() ? 0 : spielPlanHRunde.get(0).size();
		int multiplikator = spielPlanRRunde.isEmpty() ? 1 : 2;
		return anzRunden * anzPaarungen * multiplikator;
	}

	private static String gruppenBuchstabe(int index) {
		return String.valueOf((char) ('A' + index));
	}

	private void printBereichDefinieren() throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		XSpreadsheet xSheet = getXSpreadSheet();
		int letzteZeile;
		try {
			letzteZeile = letzteSpielZeile();
		} catch (GenerateException e) {
			letzteZeile = ERSTE_SPIELTAG_DATEN_ZEILE;
		}
		RangePosition bereich = RangePosition.from(
				Position.from(0, 0), Position.from(SPIELPNKT_B_SPALTE, letzteZeile));
		PrintArea.from(xSheet, getWorkingSpreadsheet()).setPrintArea(bereich);
	}

	public RangePosition printBereichRangePosition() throws GenerateException {
		LigaSpielPlan ligaSpielPlan = new LigaSpielPlan(meldeListe.getAlleMeldungen());
		int anzZeilen = (ligaSpielPlan.anzBegnungenProRunde() * ligaSpielPlan.anzRunden() * 2) - 1;

		Position rechtsUnten = Position.from(SPIELPNKT_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE + anzZeilen);
		Position linksOben = Position.from(0, 0);
		return RangePosition.from(linksOben, rechtsUnten);
	}

	private void insertDatenHeaderUndSpalteBreite() throws GenerateException {

		Position headerPos = Position.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE);
		ColumnProperties colPropErsteSpalten = ColumnProperties.from().setWidth(1500).centerJustify()
				.setShrinkToFit(true);
		StringCellValue stValHeader = StringCellValue.from(getXSpreadSheet(), headerPos)
				.setColumnProperties(colPropErsteSpalten);
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Nr.").setEndPosMergeZeilePlus(1));
		colPropErsteSpalten.setWidth(800);

		getSheetHelper().setStringValueInCell(stValHeader
				.setValue(I18n.get("column.header.mannschaft"))
				.spalte(NAME_A_SPALTE).setEndPosMergeSpaltePlus(1));

		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("column.header.ergebnis")).spalte(SPIELPNKT_A_SPALTE).setEndPosMergeSpaltePlus(1));

		ColumnProperties colProp = ColumnProperties.from().setWidth(JGJKonfigurationSheet.MELDUNG_NAME_WIDTH);
		stValHeader.setEndPosMerge(null).zeilePlusEins().setColumnProperties(colProp).centerJustify();
		getSheetHelper().setStringValueInCell(stValHeader.setValue("A").spalte(NAME_A_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("B").spalte(NAME_B_SPALTE));

		stValHeader.getColumnProperties().setWidth(PUNKTE_NR_WIDTH + 1000);

		getSheetHelper().setStringValueInCell(stValHeader.setValue("A").spalte(SPIELPNKT_A_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("B").spalte(SPIELPNKT_B_SPALTE));
	}

	private void insertArbeitsspalten(List<List<TeamPaarung>> spielPlanHRunde,
			List<List<TeamPaarung>> spielPlanRRunde, int startZeile) throws GenerateException {

		RangeData rangeData = new RangeData();

		List<List<TeamPaarung>> alleSpieltage = new ArrayList<>();
		alleSpieltage.addAll(spielPlanHRunde);
		alleSpieltage.addAll(spielPlanRRunde);

		for (List<TeamPaarung> spielTag : alleSpieltage) {
			for (TeamPaarung teamPaarung : spielTag) {
				SheetRunner.testDoCancelTask();
				RowData teamPaarungData = rangeData.addNewRow();
				teamPaarungData.newInt(teamPaarung.getA().getNr());
				teamPaarungData.newInt(teamPaarung.getOptionalB().isPresent() ? teamPaarung.getB().getNr() : 0);
			}
		}

		Position startPos = Position.from(TEAM_A_NR_SPALTE, startZeile);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData).setRangeProperties(
				RangeProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().toBorder()));

		ColumnProperties spalteBreite = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
				.isVisible(false);
		XSpreadsheet sheet = getXSpreadSheet();
		getSheetHelper().setColumnProperties(sheet, TEAM_A_NR_SPALTE, spalteBreite);
		getSheetHelper().setColumnProperties(sheet, TEAM_B_NR_SPALTE, spalteBreite);
		getSheetHelper().setColumnProperties(sheet, SPIELE_A_SPALTE, spalteBreite);
		getSheetHelper().setColumnProperties(sheet, SPIELE_B_SPALTE, spalteBreite);

		int freispielPlus = getKonfigurationSheet().getFreispielPunktePlus();
		int freispielMinus = getKonfigurationSheet().getFreispielPunkteMinus();
		int zeile = startZeile;
		for (List<TeamPaarung> spielTag : alleSpieltage) {
			for (TeamPaarung teamPaarung : spielTag) {
				if (!teamPaarung.getOptionalB().isPresent()) {
					getSheetHelper().setNumberValueInCell(
							NumberCellValue.from(sheet, SPIELPNKT_A_SPALTE, zeile, freispielPlus));
					getSheetHelper().setNumberValueInCell(
							NumberCellValue.from(sheet, SPIELPNKT_B_SPALTE, zeile, freispielMinus));
				}
				zeile++;
			}
		}
	}

	private void insertSpieltageDaten(List<List<TeamPaarung>> spielPlanHRunde,
			List<List<TeamPaarung>> spielPlanRRunde, int startZeile) throws GenerateException {
		RangeData rangeData = new RangeData();

		int anzSpieltage = spielPlanHRunde.size();
		int anzTeamPaarungen = spielPlanHRunde.get(0).size();

		for (int i = 1; i <= anzSpieltage * anzTeamPaarungen; i++) {
			rangeData.addNewRow().newString(NR_HINRUNDE_PREFIX + i);
		}
		if (!spielPlanRRunde.isEmpty()) {
			for (int i = 1; i <= anzSpieltage * anzTeamPaarungen; i++) {
				rangeData.addNewRow().newString(NR_RUECKRUNDE_PREFIX + i);
			}
		}

		Position startPos = Position.from(SPIEL_NR_SPALTE, startZeile);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	private void insertFormulaPunkte() throws GenerateException {
		int letzteSpielZeile = letzteSpielZeile();

		RangeProperties setBorder = RangeProperties.from().centerJustify()
				.setBorder(BorderFactory.from().allThin().toBorder());

		{
			String formulaHeimPunkteStr = "WENN("
					+ Position.from(SPIELPNKT_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress() + ">"
					+ Position.from(SPIELPNKT_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress() + ";1;0";

			StringCellValue formulaHeimPunkte = StringCellValue.from(getXSpreadSheet())
					.setValue(formulaHeimPunkteStr)
					.setPos(Position.from(SPIELE_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE))
					.setFillAutoDown(letzteSpielZeile);
			getSheetHelper().setFormulaInCell(formulaHeimPunkte);

			RangePosition rangePos = RangePosition.from(formulaHeimPunkte.getPos(), formulaHeimPunkte.getPos())
					.endeZeile(letzteSpielZeile);
			RangeHelper.from(this, rangePos).setRangeProperties(setBorder);
		}

		{
			String formulaGastPunkteStr = "WENN("
					+ Position.from(SPIELPNKT_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress() + ">"
					+ Position.from(SPIELPNKT_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress() + ";1;0";

			StringCellValue formulaGastPunkte = StringCellValue.from(getXSpreadSheet())
					.setValue(formulaGastPunkteStr)
					.setPos(Position.from(SPIELE_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE))
					.setFillAutoDown(letzteSpielZeile);
			getSheetHelper().setFormulaInCell(formulaGastPunkte);

			RangePosition rangePos = RangePosition.from(formulaGastPunkte.getPos(), formulaGastPunkte.getPos())
					.endeZeile(letzteSpielZeile);
			RangeHelper.from(this, rangePos).setRangeProperties(setBorder);
		}
	}

	private void insertFormulaTeamNamen() throws GenerateException {
		int letzteSpielZeile = letzteSpielZeile();
		boolean zeigeNr = konfigurationSheet.getSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NR;
		Map<Integer, String> teamNamen = zeigeNr ? Map.of() : meldeListe.leseTeamNamen();
		String freispielText = I18n.get("spielplan.freispiel.name");

		RangeData nrData = RangeHelper.from(this, RangePosition.from(
				TEAM_A_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE, TEAM_B_NR_SPALTE, letzteSpielZeile)).getDataFromRange();

		RangeData nameData = new RangeData();
		for (RowData row : nrData) {
			int nrA = row.get(0).getIntVal(0);
			int nrB = row.get(1).getIntVal(0);
			RowData nameRow = nameData.addNewRow();
			if (nrA <= 0) {
				nameRow.newString("");
				nameRow.newString("");
			} else {
				String nameA = zeigeNr ? String.valueOf(nrA) : teamNamen.getOrDefault(nrA, "");
				String nameB = nrB <= 0 ? freispielText : (zeigeNr ? String.valueOf(nrB) : teamNamen.getOrDefault(nrB, ""));
				nameRow.newString(nameA);
				nameRow.newString(nameB);
			}
		}
		RangeHelper.from(this, nameData.getRangePosition(Position.from(NAME_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE)))
				.setDataInRange(nameData);
	}

	private void insertFormulaValidierung() throws GenerateException {
		int letzteSpielZeile = letzteSpielZeile();
		XSpreadsheet sheet = getXSpreadSheet();

		getSheetHelper().setColumnProperties(sheet, EINGABE_VALIDIERUNG_SPALTE,
				ColumnProperties.from().setWidth(1800).centerJustify());

		String fehlerText = I18n.get("schweizer.spielrunde.fehler.formel");
		for (int zeile = ERSTE_SPIELTAG_DATEN_ZEILE; zeile <= letzteSpielZeile; zeile++) {
			String ergA = Position.from(SPIELPNKT_A_SPALTE, zeile).getAddress();
			String ergB = Position.from(SPIELPNKT_B_SPALTE, zeile).getAddress();
			String nrB = Position.from(TEAM_B_NR_SPALTE, zeile).getAddress();

			// @formatter:off
			String valFormula = "IF(" + nrB + "<=0;\"\";IF(OR("
					+ "AND(ISBLANK(" + ergA + ");ISBLANK(" + ergB + "));"
					+ "AND(" + ergA + "<14;" + ergB + "<14;" + ergA + ">-1;" + ergB + ">-1;" + ergA + "<>" + ergB + ")"
					+ ");\"\";\"" + fehlerText + "\"))";
			// @formatter:on

			StringCellValue valCellValue = StringCellValue
					.from(sheet, Position.from(EINGABE_VALIDIERUNG_SPALTE, zeile), valFormula)
					.setCharColor(ColorHelper.CHAR_COLOR_RED)
					.setCharWeight(FontWeight.BOLD)
					.setCharHeight(14)
					.setHoriJustify(CellHoriJustify.CENTER);
			getSheetHelper().setFormulaInCell(valCellValue);
		}
	}

	private int letzteSpielZeile() throws GenerateException {
		int zeile = RangeSearchHelper.from(this, RangePosition.from(SPIEL_NR_SPALTE, 0, SPIEL_NR_SPALTE, 999))
				.searchLastNotEmptyInSpalte().getZeile();
		if (zeile == 0) {
			throw new GenerateException(I18n.get("error.spielernummer.spalte.fehlt"));
		}
		return zeile;
	}

	private void formatieren(List<List<TeamPaarung>> spielPlanHRunde,
			List<List<TeamPaarung>> spielPlanRRunde, int startZeile) throws GenerateException {
		int letzteSpielZeile = startZeile + anzahlSpiele(spielPlanHRunde, spielPlanRRunde) - 1;

		RangePosition allDataMitHeader = RangePosition.from(SPIEL_NR_SPALTE, startZeile,
				SPIELPNKT_B_SPALTE, letzteSpielZeile);
		RangeProperties rangeProp = RangeProperties.from().setBorder(BorderFactory.from().allThin().toBorder())
				.centerJustify().setShrinkToFit(true).topMargin(110).bottomMargin(110).setCharHeight(12);
		RangeHelper.from(this, allDataMitHeader).setRangeProperties(rangeProp);

		RangePosition runden = RangePosition.from(SPIEL_NR_SPALTE, startZeile, SPIELPNKT_B_SPALTE,
				letzteSpielZeile);
		Integer spielPlanHintergrundFarbeGerade = getKonfigurationSheet().getSpielPlanHintergrundFarbeGerade();
		Integer spielPlanHintergrundFarbeUnGerade = getKonfigurationSheet().getSpielPlanHintergrundFarbeUnGerade();
		RanglisteGeradeUngeradeFormatHelper.from(this, runden).geradeFarbe(spielPlanHintergrundFarbeGerade)
				.ungeradeFarbe(spielPlanHintergrundFarbeUnGerade).apply();

		RangePosition ergRange = RangePosition.from(SPIELPNKT_A_SPALTE, startZeile, SPIELPNKT_B_SPALTE, letzteSpielZeile);
		EditierbaresZelleFormatHelper.anwenden(this, ergRange);
		formatiereErgebnisZellen(ergRange);

		RangeProperties horTrennerDouble = RangeProperties.from()
				.setBorder(BorderFactory.from().doubleLn().forBottom().toBorder());
		RangeProperties horTrennerBoldBottom = RangeProperties.from()
				.setBorder(BorderFactory.from().boldLn().forBottom().toBorder());
		RangeProperties horTrennerBoldTop = RangeProperties.from()
				.setBorder(BorderFactory.from().boldLn().forTop().toBorder());

		RangePosition headerHorTrenner = RangePosition.from(SPIEL_NR_SPALTE, startZeile,
				SPIELPNKT_B_SPALTE, startZeile);
		RangeHelper.from(this, headerHorTrenner).setRangeProperties(horTrennerBoldTop);

		RangePosition trennerPos = RangePosition.from(SPIEL_NR_SPALTE, startZeile, SPIELPNKT_B_SPALTE, startZeile);
		int anzRunden = spielPlanHRunde.size();
		int anzPaarungen = spielPlanHRunde.get(0).size();

		for (int i = 1; i < anzRunden; i++) {
			trennerPos.zeilePlus(anzPaarungen - 1);
			RangeHelper.from(this, trennerPos).setRangeProperties(horTrennerDouble);
			trennerPos.zeilePlusEins();
		}
		trennerPos.zeilePlus(anzPaarungen - 1);
		RangeHelper.from(this, trennerPos).setRangeProperties(horTrennerBoldBottom);
		trennerPos.zeilePlusEins();
		if (!spielPlanRRunde.isEmpty()) {
			for (int i = 1; i < anzRunden; i++) {
				trennerPos.zeilePlus(anzPaarungen - 1);
				RangeHelper.from(this, trennerPos).setRangeProperties(horTrennerDouble);
				trennerPos.zeilePlusEins();
			}
		}

		RangeProperties vertTrennerBoldLeft = RangeProperties.from()
				.setBorder(BorderFactory.from().boldLn().forLeft().toBorder());
		RangeProperties vertTrennerDoubleLeft = RangeProperties.from()
				.setBorder(BorderFactory.from().doubleLn().forLeft().toBorder());

		RangePosition vertikal = RangePosition.from(SPIEL_NR_SPALTE, startZeile, SPIEL_NR_SPALTE, letzteSpielZeile);
		RangeHelper.from(this, vertikal.spalte(SPIEL_NR_SPALTE)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(this, vertikal.spalte(NAME_B_SPALTE)).setRangeProperties(vertTrennerDoubleLeft);
		RangeHelper.from(this, vertikal.spalte(SPIELPNKT_A_SPALTE)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(this, vertikal.spalte(SPIELPNKT_B_SPALTE + 1)).setRangeProperties(vertTrennerBoldLeft);

		RangePosition headerRange = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE,
				SPIELPNKT_B_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE + 1);
		RangeHelper.from(this, headerRange).setRangeProperties(RangeProperties.from()
				.setCellBackColor(getKonfigurationSheet().getSpielPlanHeaderFarbe())
				.setBorder(BorderFactory.from().allThin().toBorder()));

		RangePosition headerVertikal = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE,
				SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE + 1);
		RangeHelper.from(this, headerVertikal.spalte(SPIEL_NR_SPALTE)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(this, headerVertikal.spalte(NAME_B_SPALTE)).setRangeProperties(vertTrennerDoubleLeft);
		RangeHelper.from(this, headerVertikal.spalte(SPIELPNKT_A_SPALTE)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(this, headerVertikal.spalte(SPIELPNKT_B_SPALTE + 1)).setRangeProperties(vertTrennerBoldLeft);
	}

	private void formatiereErgebnisZellen(RangePosition ergRange) throws GenerateException {
		String cellA = "INDIRECT(ADDRESS(ROW();" + (SPIELPNKT_A_SPALTE + 1) + "))";
		String cellB = "INDIRECT(ADDRESS(ROW();" + (SPIELPNKT_B_SPALTE + 1) + "))";
		String formulaGleicheWerte = "AND(NOT(ISBLANK(" + cellA + "));NOT(ISBLANK(" + cellB + "));"
				+ cellA + "=" + cellB + ")";
		ConditionalFormatHelper.from(this, ergRange).clear()
				.formula1("0").formula2("13").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
				.formula1("ISTEXT(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")")
				.operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset()
				.formula1(formulaGleicheWerte).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset();
	}

}
