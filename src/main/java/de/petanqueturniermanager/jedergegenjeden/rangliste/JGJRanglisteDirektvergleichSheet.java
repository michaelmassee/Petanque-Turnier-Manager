/**
 * Erstellung 09.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.rangliste;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.algorithmen.DirektvergleichResult;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
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
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.model.IMeldung;
import de.petanqueturniermanager.model.LigaSpielPlan;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * @author Michael Massee
 */
public class JGJRanglisteDirektvergleichSheet extends JGJSheet implements ISheet {

	private static final Logger logger = LogManager.getLogger(JGJRanglisteDirektvergleichSheet.class);
	private static final String SHEETNAME = "Direktvergleich";
	private static final String SHEET_COLOR = "42d4f5";
	private static final int MARGIN = 120;

	private final JGJMeldeListeSheet_Update meldeListe;
	private final MeldungenSpalte<TeamMeldungen, Team> meldungenSpalte;
	private LigaSpielPlan spielPlan;

	public static final int ERSTE_DATEN_ZEILE = 1; // Zeile 2
	public static final int TEAM_NR_SPALTE = 0; // Spalte A=0
	public static final int TEAM_NR_HEADER_ZEILE = 0; // zeile 1
	public static final int ERSTE_SPALTE_DIREKTVERGLEICH = 2;

	/**
	 * @param workingSpreadsheet
	 */
	public JGJRanglisteDirektvergleichSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "JGJ-RanglisteSheet");
		meldungenSpalte = MeldungenSpalte.builder().spalteMeldungNameWidth(MELDUNG_NAME_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(TEAM_NR_SPALTE).sheet(this)
				.formation(Formation.TETE).anzZeilenInHeader(1).build();
		meldeListe = initMeldeListeSheet(workingSpreadsheet);

	}

	@VisibleForTesting
	JGJMeldeListeSheet_Update initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new JGJMeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
	}

	/**
	 * @throws GenerateException
	 */
	private void upDateSheet() throws GenerateException {
		meldeListe.upDateSheet();
		TeamMeldungen alleMeldungen = meldeListe.getAlleMeldungen();
		this.spielPlan = new LigaSpielPlan(alleMeldungen);

		getxCalculatable().enableAutomaticCalculation(false); // speed up
		if (!alleMeldungen.isValid()) {
			processBoxinfo("Abbruch, ungültige anzahl von Melungen.");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Neue JGJ-SpielPlan")
					.message("Ungültige anzahl von Melungen").show();
			return;
		}

		if (!NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.JGJ_DIREKTEVERGLEICH).setForceCreate(true).setActiv()
				.hideGrid().tabColor(SHEET_COLOR).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, JGJ Direktvergleich wurde nicht erstellt");
			return;
		}
		meldungenSpalte.alleAktiveUndAusgesetzteMeldungenAusmeldelisteEinfuegen(meldeListe);
		int headerBackColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		meldungenSpalte.insertHeaderInSheet(headerBackColor);

		dateneinfuegen(alleMeldungen);
		formatData();
		addConditionalFormuleForDirektVergleichReturnCode();
		StringCellValue lastPos = addFooter();
		printBereichDefinieren(lastPos.getPos());
	}

	public int anzTeams() throws GenerateException {
		TeamMeldungen alleMeldungen = meldeListe.getAlleMeldungen();
		return alleMeldungen.getMeldungen().size();
	}

	private void formatData() throws GenerateException {
		processBoxinfo("Formatieren Datenbereich");

		RangeProperties rangeProp = RangeProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()).centerJustify().margin(MARGIN);
		RangeHelper.from(this, allDatenRange()).setRangeProperties(rangeProp);

		RangePosition meldeListeRange = RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE + 1, // plus anz spalten Tmnr + Namen, minus 1
				ERSTE_DATEN_ZEILE + anzTeams() - 1);

		RanglisteGeradeUngeradeFormatHelper.from(this, meldeListeRange)
				.geradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeGerade())
				.ungeradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade()).apply();

		// teamnamen und data trennen mit bold
		RangeHelper.from(this, TEAM_NR_SPALTE + 1, TEAM_NR_HEADER_ZEILE, TEAM_NR_SPALTE + 1,
				TEAM_NR_HEADER_ZEILE + anzTeams()).setRangeProperties(
						RangeProperties.from().setBorder(BorderFactory.from().boldLn().forRight().toBorder()));
	}

	private RangePosition allDatenRange() throws GenerateException {
		int anzTeams = anzTeams();
		return RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE + anzTeams + 1, // plus anz spalten Tmnr + Namen, minus 1
				ERSTE_DATEN_ZEILE + anzTeams - 1);
	}

	private void dateneinfuegen(TeamMeldungen alleMeldungen) throws GenerateException {
		processBoxinfo("Daten einfuegen");

		Position startTeamNrPos = Position.from(ERSTE_SPALTE_DIREKTVERGLEICH, TEAM_NR_HEADER_ZEILE);
		int headerBackColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		StringCellValue htmNr = StringCellValue.from(getXSpreadSheet()).setPos(startTeamNrPos)
				.setCellBackColor(headerBackColor).centerJustify().setAllThinBorder();

		for (IMeldung<Team> mld : alleMeldungen.getMeldungenSortedByNr()) {
			htmNr.setValue(mld.getNr());
			getSheetHelper().setStringValueInCell(htmNr);
			htmNr.spaltePlusEins();
		}

		// formula einfuegen
		StringCellValue formula = StringCellValue.from(getXSpreadSheet())
				.setCharWeight(com.sun.star.awt.FontWeight.BOLD);
		StringCellValue xStr = StringCellValue.from(getXSpreadSheet()).setValue("x");
		String spielplanBegegnungenVerweis = ligaSpielPlanVerweis(JGJSpielPlanSheet.TEAM_A_NR_SPALTE);
		String spielplanSpieleVerweis = ligaSpielPlanVerweis(JGJSpielPlanSheet.SPIELE_A_SPALTE);
		String spielplanSpielPunkteVerweis = ligaSpielPlanVerweis(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE);
		for (IMeldung<Team> mldA : alleMeldungen.getMeldungenSortedByNr()) {
			for (IMeldung<Team> mldB : alleMeldungen.getMeldungenSortedByNr()) {
				if (mldA.getNr() != mldB.getNr()) {
					String formuleStr = direktVergleichFormula(mldA.getNr(), mldB.getNr(), spielplanBegegnungenVerweis,
							spielplanSpieleVerweis, spielplanSpielPunkteVerweis);
					formula.setPos(startTeamNrPos).zeilePlus(mldA.getNr()).spaltePlus(mldB.getNr() - 1)
							.setComment(mldA.getNr() + ":" + mldB.getNr()).setValue(formuleStr);
					getSheetHelper().setFormulaInCell(formula);
				} else {
					xStr.setPos(startTeamNrPos).zeilePlus(mldA.getNr()).spaltePlus(mldB.getNr() - 1);
					getSheetHelper().setStringValueInCell(xStr);
				}
			}
		}
	}

	private String ligaSpielPlanVerweis(int startSpalte) throws GenerateException {
		int anzZeilen = (spielPlan.anzRunden() * 2) * spielPlan.anzBegnungenProRunde();
		Position startBegegnungenPos = Position.from(startSpalte, JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE);
		return "$'" + JGJSpielPlanSheet.SHEET_NAMEN + "'." + startBegegnungenPos.getAddressWith$() + ":"
				+ startBegegnungenPos.spaltePlusEins().zeilePlus(anzZeilen - 1).getAddressWith$();
	}

	private String direktVergleichFormula(int tmA, int tmB, String spielplanBegegnungenVerweis,
			String spielplanSpieleVerweis, String spielplanSpielPunkteVerweis) {
		// =PTM.ALG.DIREKTVERGLEICH(1;2;$'Liga Spielplan'.O3:P32;$'Liga Spielplan'.I3:J32;$'Liga Spielplan'.K3:L32)
		return GlobalImpl.PTM_DIREKTVERGLEICH + "(" + tmA + ";" + tmB + ";" + spielplanBegegnungenVerweis + ";"
				+ spielplanSpieleVerweis + ";" + spielplanSpielPunkteVerweis + ")";
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	private void addConditionalFormuleForDirektVergleichReturnCode() throws GenerateException {
		processBoxinfo("Conditional Formule für Codes");

		RangePosition rangePosDirektCode = RangePosition.from(ERSTE_SPALTE_DIREKTVERGLEICH, ERSTE_DATEN_ZEILE,
				ERSTE_SPALTE_DIREKTVERGLEICH + anzTeams() - 1, ERSTE_DATEN_ZEILE + anzTeams() - 1);

		RanglisteGeradeUngeradeFormatHelper.from(this, rangePosDirektCode)
				.redCharEqualToValue(DirektvergleichResult.VERLOREN.getCode())
				.greenCharEqualToValue(DirektvergleichResult.GEWONNEN.getCode())
				.geradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeGerade())
				.ungeradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade()).apply();

	}

	private int footerStartLinePos() throws GenerateException {
		return ERSTE_DATEN_ZEILE + anzTeams();
	}

	/**
	 * 
	 * @return last pos
	 * @throws GenerateException
	 */
	private StringCellValue addFooter() throws GenerateException {
		processBoxinfo("Footer");
		Position startPos = Position.from(TEAM_NR_SPALTE, footerStartLinePos() + 1);
		StringCellValue direktvergleichResultCode = StringCellValue.from(getXSpreadSheet()).setPos(startPos)
				.setCharHeight(8);
		StringCellValue direktvergleichResultVal = StringCellValue.from(direktvergleichResultCode).spaltePlusEins()
				.setHoriJustify(com.sun.star.table.CellHoriJustify.LEFT);

		List<DirektvergleichResult> sortedList = DirektvergleichResult.stream()
				.sorted(Comparator.comparingInt(DirektvergleichResult::getCode)).collect(Collectors.toList());

		for (DirektvergleichResult drslt : sortedList) {
			direktvergleichResultCode.setValue(drslt.getCode());
			direktvergleichResultVal.setValue(drslt.getAnzeigeText());
			getSheetHelper().setStringValueInCell(direktvergleichResultCode);
			getSheetHelper().setStringValueInCell(direktvergleichResultVal);
			direktvergleichResultCode.zeilePlusEins();
			direktvergleichResultVal.zeilePlusEins();
		}
		return direktvergleichResultCode.zeilePlus(-1); // letzte zeile
	}

	private void printBereichDefinieren(Position footerPos) throws GenerateException {
		processBoxinfo("Print-Bereich");
		RangePosition allDatenRange = allDatenRange();
		Position rechtsUnten = Position.from(allDatenRange.getEnde().getSpalte(), footerPos.getZeile());
		Position linksOben = Position.from(0, 0);
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

}
