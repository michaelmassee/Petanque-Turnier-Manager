/**
 * Erstellung 09.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.addins.GlobalImpl;
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
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_Update;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.model.IMeldung;
import de.petanqueturniermanager.model.LigaSpielPlan;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * @author Michael Massee
 */
public class LigaRanglisteDirektvergleichSheet extends LigaSheet implements ISheet {

	private static final Logger logger = LogManager.getLogger(LigaRanglisteDirektvergleichSheet.class);
	private static final String SHEETNAME = "Direktvergleich";
	private static final String SHEET_COLOR = "42d4f5";
	private static final int MARGIN = 120;

	private final LigaMeldeListeSheet_Update meldeListe;
	private final MeldungenSpalte<TeamMeldungen, Team> meldungenSpalte;
	private LigaSpielPlan ligaSpielPlan;

	private static final int ERSTE_DATEN_ZEILE = 1; // Zeile 2
	private static final int TEAM_NR_SPALTE = 0; // Spalte A=0
	public static final int TEAM_NR_HEADER_ZEILE = 0; // zeile 1
	public static final int ERSTE_SPALTE_DIREKTVERGLEICH = 2;

	/**
	 * @param workingSpreadsheet
	 * @param logPrefix
	 * @throws GenerateException
	 */
	public LigaRanglisteDirektvergleichSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Liga-RanglisteSheet");
		meldungenSpalte = MeldungenSpalte.Builder().spalteMeldungNameWidth(LIGA_MELDUNG_NAME_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(TEAM_NR_SPALTE).sheet(this)
				.formation(Formation.TETE).anzZeilenInHeader(1).build();
		meldeListe = initMeldeListeSheet(workingSpreadsheet);

	}

	@VisibleForTesting
	LigaMeldeListeSheet_Update initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new LigaMeldeListeSheet_Update(workingSpreadsheet);
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
		TeamMeldungen alleMeldungen = meldeListe.getAlleMeldungen();
		this.ligaSpielPlan = new LigaSpielPlan(alleMeldungen);

		getxCalculatable().enableAutomaticCalculation(false); // speed up
		if (!alleMeldungen.isValid()) {
			processBoxinfo("Abbruch, ungültige anzahl von Melungen.");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Neue Liga-SpielPlan")
					.message("Ungültige anzahl von Melungen").show();
			return;
		}

		if (!NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.LIGA_DIREKTEVERGLEICH).setForceCreate(true).setActiv()
				.hideGrid().tabColor(SHEET_COLOR).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Liga Direktvergleich wurde nicht erstellt");
			return;
		}
		meldungenSpalte.alleAktiveUndAusgesetzteMeldungenAusmeldelisteEinfuegen(meldeListe);
		int headerBackColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		meldungenSpalte.insertHeaderInSheet(headerBackColor);

		dateneinfuegen(alleMeldungen);
		formatData();
		addConditionalFormuleForDirektVergleichReturnCode();
	}

	private int anzTeams() throws GenerateException {
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

		for (IMeldung<Team> mld : alleMeldungen.getMeldungen()) {
			htmNr.setValue(mld.getNr());
			getSheetHelper().setStringValueInCell(htmNr);
			htmNr.spaltePlusEins();
		}

		// formula einfuegen
		StringCellValue formula = StringCellValue.from(getXSpreadSheet());
		StringCellValue xStr = StringCellValue.from(getXSpreadSheet()).setValue("X");
		String spielplanBegegnungenVerweis = ligaSpielPlanVerweis();
		String spielplanSpieleVerweis = ligaSpielPlanVerweis();
		String spielplanSpielPunkteVerweis = ligaSpielPlanVerweis();
		for (IMeldung<Team> mldA : alleMeldungen.getMeldungen()) {
			for (IMeldung<Team> mldB : alleMeldungen.getMeldungen()) {
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

	private String ligaSpielPlanVerweis() throws GenerateException {
		Position startBegegnungenPos = Position.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE);
		return "$'" + LigaSpielPlanSheet.SHEET_NAMEN + "'." + startBegegnungenPos.getAddress() + ":"
				+ startBegegnungenPos.spaltePlusEins().zeilePlus(anzTeams() - 1).getAddress();
	}

	private String direktVergleichFormula(int tmA, int tmB, String spielplanBegegnungenVerweis,
			String spielplanSpieleVerweis, String spielplanSpielPunkteVerweis) {
		// =PTM.ALG.DIREKTVERGLEICH(1;2;$'Liga Spielplan'.O3:P32;$'Liga Spielplan'.I3:J32;$'Liga Spielplan'.K3:L32)
		return GlobalImpl.PTMDIREKTVERGLEICH + "(" + tmA + ";" + tmB + ";" + spielplanBegegnungenVerweis + ";"
				+ spielplanSpieleVerweis + ";" + spielplanSpielPunkteVerweis + ")";
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	private void addConditionalFormuleForDirektVergleichReturnCode() throws GenerateException {

		RangePosition rangePosDirektCode = RangePosition.from(ERSTE_SPALTE_DIREKTVERGLEICH, ERSTE_DATEN_ZEILE,
				ERSTE_SPALTE_DIREKTVERGLEICH + anzTeams() - 1, ERSTE_DATEN_ZEILE + anzTeams() - 1);

		RanglisteGeradeUngeradeFormatHelper.from(this, rangePosDirektCode)
				.geradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeGerade())
				.ungeradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade()).apply();

	}

}
