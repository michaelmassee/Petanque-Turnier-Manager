/**
 * Erstellung 09.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.algorithmen.JederGegenJeden;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_Update;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * @author Michael Massee
 *
 */
public class LigaRanglisteSheet extends LigaSheet implements ISheet {

	private static final Logger logger = LogManager.getLogger(LigaRanglisteSheet.class);
	private static final String SHEETNAME = "Rangliste";
	private static final String SHEET_COLOR = "d637e8";
	private static final int ERSTE_DATEN_ZEILE = 2; // Zeile 3
	private static final int TEAM_NR_SPALTE = 0; // Spalte A=0

	private static final int ERSTE_SPIELTAG_SPALTE = TEAM_NR_SPALTE + 3; // nr + name + rangliste
	private static final int PUNKTE_NR_WIDTH = MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH;
	private static final int ANZ_SUMMEN_SPALTEN = 8; // Punkte +/- Spiele +/-/Diff SpielPunkte +/-/Diff

	private final MeldungenSpalte<TeamMeldungen> meldungenSpalte;
	private final LigaMeldeListeSheet_Update meldeListe;

	/**
	 * @param workingSpreadsheet
	 * @param logPrefix
	 */
	public LigaRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Liga-RanglisteSheet");
		meldungenSpalte = MeldungenSpalte.Builder().spalteMeldungNameWidth(LIGA_MELDUNG_NAME_WIDTH).ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(TEAM_NR_SPALTE).sheet(this)
				.formation(Formation.TETE).build();
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
	 *
	 */
	private void upDateSheet() throws GenerateException {
		getxCalculatable().enableAutomaticCalculation(false); // speed up
		TeamMeldungen alleMeldungen = meldeListe.getAlleMeldungen();
		if (!alleMeldungen.isValid()) {
			processBoxinfo("Abbruch, ungültige anzahl von Melungen.");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Neue Liga-SpielPlan").message("Ungültige anzahl von Melungen").show();
			return;
		}

		if (!NewSheet.from(getWorkingSpreadsheet(), SHEETNAME).pos(DefaultSheetPos.LIGA_ENDRANGLISTE).setForceCreate(true).setActiv().hideGrid().tabColor(SHEET_COLOR).create()
				.isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Liga SpielPlan wurde nicht erstellt");
			return;
		}

		meldungenSpalte.alleAktiveUndAusgesetzteMeldungenAusmeldelisteEinfuegen(meldeListe);
		int headerBackColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		meldungenSpalte.insertHeaderInSheet(headerBackColor);
		JederGegenJeden jederGegenJeden = new JederGegenJeden(alleMeldungen);
		spieltageFormulaEinfuegen(jederGegenJeden);
		summenSpaltenEinfuegen(jederGegenJeden);
		format(jederGegenJeden);
		doSort(jederGegenJeden);
	}

	/**
	 * @param jederGegenJeden
	 * @throws GenerateException
	 */
	private void format(JederGegenJeden jederGegenJeden) throws GenerateException {
		RangeProperties rangeProp = RangeProperties.from().setBorder(BorderFactory.from().allThin().toBorder()).centerJustify();
		RangeHelper.from(getXSpreadSheet(), allDatenRange(jederGegenJeden)).setRangeProperties(rangeProp);
	}

	/**
	 * Sortier reihenfolge:<br>
	 * (Begegnung)Punkte +<b>
	 *
	 * @param jederGegenJeden
	 * @throws GenerateException
	 */
	private void doSort(JederGegenJeden jederGegenJeden) throws GenerateException {
		getxCalculatable().calculateAll(); // zum sortieren werte kalkulieren
		RangePosition allDatenRange = allDatenRange(jederGegenJeden);
		int punktepalte = (allDatenRange.getAnzahlSpalten() - 8);
		int spielePluspalte = (allDatenRange.getAnzahlSpalten() - 6);
		int spielpunkteDiffSpalte = (allDatenRange.getAnzahlSpalten() - 1);
		int sortSpalten[] = new int[] { punktepalte, spielePluspalte, spielpunkteDiffSpalte }; // 0 = erste Spalte in Range
		SortHelper.from(getXSpreadSheet(), allDatenRange(jederGegenJeden)).abSteigendSortieren().spaltenToSort(sortSpalten).doSort();
	}

	private RangePosition allDatenRange(JederGegenJeden jederGegenJeden) {
		int ersteSummeSpalte = ersteSummeSpalte(jederGegenJeden);
		return RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, ersteSummeSpalte + (ANZ_SUMMEN_SPALTEN - 1), anzZeilen(jederGegenJeden) + 1);
	}

	private int anzZeilen(JederGegenJeden jederGegenJeden) {
		return (jederGegenJeden.anzPaarungen() * 2);
	}

	private int ersteSummeSpalte(JederGegenJeden jederGegenJeden) {
		return ERSTE_SPIELTAG_SPALTE + (anzGesamtRunden(jederGegenJeden) * 6); // 6 = anzahl summen spalte pro spieltag
	}

	private int anzGesamtRunden(JederGegenJeden jederGegenJeden) {
		return jederGegenJeden.anzRunden() * 2; // *2 weil hin und rückrunde
	}

	private void summenSpaltenEinfuegen(JederGegenJeden jederGegenJeden) throws GenerateException {
		int anzRunden = anzGesamtRunden(jederGegenJeden);
		int anzPaarungen = jederGegenJeden.anzPaarungen();
		int ersteSummeSpalte = ersteSummeSpalte(jederGegenJeden);
		int autoFillDownZeilePlus = (anzPaarungen * 2) - 1;
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(PUNKTE_NR_WIDTH);

		int endSummeSpalteOffs = 0;
		for (int summeSpalte = 0; summeSpalte < 6; summeSpalte++) {
			Position summePos = Position.from(ersteSummeSpalte + endSummeSpalteOffs, ERSTE_DATEN_ZEILE);
			String summenFormulaSummeStr = "";
			for (int rndCnt = 0; rndCnt < anzRunden; rndCnt++) {
				Position punktePlusPos = Position.from(ERSTE_SPIELTAG_SPALTE + summeSpalte + (rndCnt * 6), ERSTE_DATEN_ZEILE);
				summenFormulaSummeStr += punktePlusPos.getAddress() + ((rndCnt + 1 < anzRunden) ? "+" : "");
			}
			StringCellValue summenFormulaSumme = StringCellValue.from(getXSpreadSheet()).setPos(summePos).setFillAutoDownZeilePlus(autoFillDownZeilePlus)
					.setValue(summenFormulaSummeStr).setColumnProperties(columnProperties);
			getSheetHelper().setFormulaInCell(summenFormulaSumme);
			endSummeSpalteOffs++;

			// Spiele (Siege) Diff einfuegen ?
			if (endSummeSpalteOffs == 4) {
				Position summeSpielePlusPos = Position.from((ersteSummeSpalte + endSummeSpalteOffs) - 2, ERSTE_DATEN_ZEILE);
				Position summeSpieleMinusPos = Position.from((ersteSummeSpalte + endSummeSpalteOffs) - 1, ERSTE_DATEN_ZEILE);
				String summenFormulaDiffSpieleStr = summeSpielePlusPos.getAddress() + "-" + summeSpieleMinusPos.getAddress();
				StringCellValue summenFormulaDiffSpiele = StringCellValue.from(getXSpreadSheet()).setPos(summePos).spaltePlusEins().setFillAutoDownZeilePlus(autoFillDownZeilePlus)
						.setValue(summenFormulaDiffSpieleStr).setColumnProperties(columnProperties);
				getSheetHelper().setFormulaInCell(summenFormulaDiffSpiele);
				endSummeSpalteOffs++;
			}
		}
		// Spielpunkte Diff
		Position summeSpielPnktPlusPos = Position.from((ersteSummeSpalte + endSummeSpalteOffs) - 2, ERSTE_DATEN_ZEILE);
		Position summeSpielPnktMinusPos = Position.from((ersteSummeSpalte + endSummeSpalteOffs) - 1, ERSTE_DATEN_ZEILE);
		String summenFormulaDiffSpielPnktStr = summeSpielPnktPlusPos.getAddress() + "-" + summeSpielPnktMinusPos.getAddress();
		StringCellValue summenFormulaDiffSpielPnkt = StringCellValue.from(getXSpreadSheet()).setPos(summeSpielPnktMinusPos.spaltePlusEins())
				.setFillAutoDownZeilePlus(autoFillDownZeilePlus).setValue(summenFormulaDiffSpielPnktStr).setColumnProperties(columnProperties);
		getSheetHelper().setFormulaInCell(summenFormulaDiffSpielPnkt);
	}

	private void spieltageFormulaEinfuegen(JederGegenJeden jederGegenJeden) throws GenerateException {

		// =WENNNV(INDEX($'Liga Spielplan'.F3:F5;VERGLEICH(A3;$'Liga Spielplan'.O3:O5;0)); INDEX($'Liga Spielplan'.G3:G5;VERGLEICH(A3;$'Liga Spielplan'.P3:P5;0)))

		int erstePaarungSpieltagZeileInSpielplan = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		Position spielTagFormulaPos = Position.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE);

		int anzRunden = jederGegenJeden.anzRunden();
		int anzPaarungen = jederGegenJeden.anzPaarungen();

		for (int hinruckrunde = 0; hinruckrunde < 2; hinruckrunde++) {
			for (int rndCnt = 0; rndCnt < anzRunden; rndCnt++) {
				spieltagEinfuegen(erstePaarungSpieltagZeileInSpielplan, Position.from(spielTagFormulaPos), anzPaarungen);
				// 6 Spalten pro Spieltag
				spielTagFormulaPos.spaltePlus(6);
				erstePaarungSpieltagZeileInSpielplan += anzPaarungen;
			}
		}
	}

	private void spieltagEinfuegen(int erstePaarungSpieltagZeileInSpielplan, Position spielTagFormulaPos, int anzPaarungen) throws GenerateException {
		int[] spalteA = new int[] { LigaSpielPlanSheet.PUNKTE_A_SPALTE, LigaSpielPlanSheet.SPIELE_A_SPALTE, LigaSpielPlanSheet.SPIELPNKT_A_SPALTE };
		int[] spalteB = new int[] { LigaSpielPlanSheet.PUNKTE_B_SPALTE, LigaSpielPlanSheet.SPIELE_B_SPALTE, LigaSpielPlanSheet.SPIELPNKT_B_SPALTE };

		for (int idx = 0; idx < spalteA.length; idx++) {
			verweisAufErgbnisseEinfuegen(spalteA[idx], spalteB[idx], erstePaarungSpieltagZeileInSpielplan, anzPaarungen, Position.from(spielTagFormulaPos));
			spielTagFormulaPos.spaltePlus(2);
		}
	}

	private void verweisAufErgbnisseEinfuegen(int spalteA, int spalteB, int startZeile, int anzPaarungen, Position startFormulaPos) throws GenerateException {
		Position ersteTeamNrPos = Position.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE);

		Position punkteAStartPos = Position.from(spalteA, startZeile);
		Position punkteAEndePos = Position.from(punkteAStartPos).zeilePlus(anzPaarungen - 1);
		Position punkteBStartPos = Position.from(spalteB, startZeile);
		Position punkteBEndePos = Position.from(punkteBStartPos).zeilePlus(anzPaarungen - 1);

		String rangeStrAPlus = punkteAStartPos.getAddressWith$() + ":" + punkteAEndePos.getAddressWith$();
		String rangeStrBPlus = punkteBStartPos.getAddressWith$() + ":" + punkteBEndePos.getAddressWith$();

		Position teamNrAStartPos = Position.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE, startZeile);
		Position teamNrAEndePos = Position.from(teamNrAStartPos).zeilePlus(anzPaarungen - 1);
		Position teamNrBStartPos = Position.from(LigaSpielPlanSheet.TEAM_B_NR_SPALTE, startZeile);
		Position teamNrBEndePos = Position.from(teamNrBStartPos).zeilePlus(anzPaarungen - 1);

		String rangeStrATeamNr = teamNrAStartPos.getAddressWith$() + ":" + teamNrAEndePos.getAddressWith$();
		String rangeStrBTeamNr = teamNrBStartPos.getAddressWith$() + ":" + teamNrBEndePos.getAddressWith$();

		// @formatter:off
		String formulaPunktePlus = "IFNA("+
		                    "INDEX($'Liga Spielplan'." + rangeStrAPlus +
									";MATCH("+ ersteTeamNrPos.getAddress() + ";$'Liga Spielplan'." + rangeStrATeamNr + ";0)"+
		                    	");" +
		                    "INDEX($'Liga Spielplan'." + rangeStrBPlus +
								";MATCH("+ ersteTeamNrPos.getAddress() + ";$'Liga Spielplan'."+ rangeStrBTeamNr +";0)"+
							")" +
                    	")";

		String formulaPunkteMinus = "IFNA("+
                "INDEX($'Liga Spielplan'." + rangeStrBPlus +
						";MATCH("+ ersteTeamNrPos.getAddress() + ";$'Liga Spielplan'." + rangeStrATeamNr + ";0)"+
                	");" +
                "INDEX($'Liga Spielplan'." + rangeStrAPlus +
					";MATCH("+ ersteTeamNrPos.getAddress() + ";$'Liga Spielplan'."+ rangeStrBTeamNr +";0)"+
				")" +
        	")";
		// @formatter:on
		// RangeHelper;

		boolean isvisable = getKonfigurationSheet().zeigeArbeitsSpalten();
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(PUNKTE_NR_WIDTH).isVisible(isvisable);

		StringCellValue spielTagFormula = StringCellValue.from(getXSpreadSheet()).setPos(startFormulaPos).setValue(formulaPunktePlus)
				.setFillAutoDownZeilePlus((anzPaarungen * 2) - 1).addColumnProperties(columnProperties);
		getSheetHelper().setFormulaInCell(spielTagFormula);

		spielTagFormula.spaltePlusEins().setFillAutoDownZeilePlus((anzPaarungen * 2) - 1).setValue(formulaPunkteMinus);
		getSheetHelper().setFormulaInCell(spielTagFormula);

	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

}
