/**
 * Erstellung 09.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
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

	private final LigaMeldeListeSheet_Update meldeListe;

	public static final int TEAM_NR_HEADER_ZEILE = 1;
	public static final int TEAM_NR_HEADER_SPALTE = 1;

	/**
	 * @param workingSpreadsheet
	 * @param logPrefix
	 * @throws GenerateException
	 */
	public LigaRanglisteDirektvergleichSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Liga-RanglisteSheet");
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

		getxCalculatable().enableAutomaticCalculation(false); // speed up
		if (!alleMeldungen.isValid()) {
			processBoxinfo("Abbruch, ungültige anzahl von Melungen.");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Neue Liga-SpielPlan")
					.message("Ungültige anzahl von Melungen").show();
			return;
		}

		if (!NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.LIGA_DIREKTEVERGLEICH).setForceCreate(true).setActiv()
				.hideGrid().tabColor(SHEET_COLOR).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Liga Diektvergleich wurde nicht erstellt");
			return;
		}
		dateneinfuegen(alleMeldungen);
	}

	private void dateneinfuegen(TeamMeldungen alleMeldungen) throws GenerateException {
		LigaSpielPlan ligaSpielPlan = new LigaSpielPlan(alleMeldungen);

		Position startTeamNrPos = Position.from(TEAM_NR_HEADER_SPALTE, TEAM_NR_HEADER_ZEILE);
		StringCellValue htmNr = StringCellValue.from(getXSpreadSheet()).setPos(startTeamNrPos);
		StringCellValue vtmNr = StringCellValue.from(getXSpreadSheet()).setPos(startTeamNrPos);

		for (IMeldung<Team> mld : alleMeldungen.getMeldungen()) {
			htmNr.setValue(mld.getNr()).spaltePlusEins();
			vtmNr.setValue(mld.getNr()).zeilePlusEins();
			getSheetHelper().setStringValueInCell(htmNr);
			getSheetHelper().setStringValueInCell(vtmNr);
		}

		// formula einfuegen
		StringCellValue formula = StringCellValue.from(getXSpreadSheet());
		StringCellValue xStr = StringCellValue.from(getXSpreadSheet()).setValue("X");
		String spielplanBegegnungenVerweis = ligaSpielPlanVerweis(ligaSpielPlan, LigaSpielPlanSheet.TEAM_A_NR_SPALTE);
		String spielplanSpieleVerweis = ligaSpielPlanVerweis(ligaSpielPlan, LigaSpielPlanSheet.SPIELE_A_SPALTE);
		String spielplanSpielPunkteVerweis = ligaSpielPlanVerweis(ligaSpielPlan, LigaSpielPlanSheet.SPIELPNKT_A_SPALTE);
		for (IMeldung<Team> mldA : alleMeldungen.getMeldungen()) {
			for (IMeldung<Team> mldB : alleMeldungen.getMeldungen()) {
				if (mldA.getNr() != mldB.getNr()) {
					String formuleStr = direktVergleichFormula(mldA.getNr(), mldB.getNr(), spielplanBegegnungenVerweis,
							spielplanSpieleVerweis, spielplanSpielPunkteVerweis);
					formula.setPos(startTeamNrPos).zeilePlus(mldA.getNr()).spaltePlus(mldB.getNr())
							.setComment(mldA.getNr() + ":" + mldB.getNr()).setValue(formuleStr);
					getSheetHelper().setFormulaInCell(formula);
				} else {
					xStr.setPos(startTeamNrPos).zeilePlus(mldA.getNr()).spaltePlus(mldB.getNr());
					getSheetHelper().setStringValueInCell(xStr);
				}
			}
		}
	}

	private String ligaSpielPlanVerweis(LigaSpielPlan ligaSpielPlan, int startSpalte) {
		int anzZeilen = (ligaSpielPlan.anzRunden() * 2) * ligaSpielPlan.anzBegnungenProRunde();
		Position startBegegnungenPos = Position.from(startSpalte, LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE);
		return "$'" + LigaSpielPlanSheet.SHEET_NAMEN + "'." + startBegegnungenPos.getAddress() + ":"
				+ startBegegnungenPos.spaltePlusEins().zeilePlus(anzZeilen - 1).getAddress();
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

}
