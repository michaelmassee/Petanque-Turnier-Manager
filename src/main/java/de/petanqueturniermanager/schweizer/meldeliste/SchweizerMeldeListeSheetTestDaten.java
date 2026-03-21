package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erzeugt eine Schweizer Meldeliste mit Testdaten (ohne Dialog).
 * Formation: Triplette, Teamname und Vereinsname aktiv.
 */
public class SchweizerMeldeListeSheetTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

	public static final int ANZ_TEAMS_DEFAULT = 16;
	private static final Formation TEST_FORMATION = Formation.TRIPLETTE;

	protected static final int ERSTE_DATEN_ZEILE = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;

	private final SchweizerListeDelegate delegate;
	private final int anzTeams;
	private final SchweizerMeldeListeSheetNew meldeListe;
	private final TestnamenLoader testnamenLoader;

	public SchweizerMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, ANZ_TEAMS_DEFAULT);
	}

	public SchweizerMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER, "Schweizer-Meldeliste");
		delegate = new SchweizerListeDelegate(this);
		this.anzTeams = anzTeams;
		meldeListe = new SchweizerMeldeListeSheetNew(workingSpreadsheet);
		testnamenLoader = new TestnamenLoader();
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected SchweizerKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	// ---------------------------------------------------------------
	// Forwarding-Methoden → Delegate
	// ---------------------------------------------------------------

	public int getTeamNrSpalte() {
		return delegate.getTeamNrSpalte();
	}

	public int getTeamnameSpalte() throws GenerateException {
		return delegate.getTeamnameSpalte();
	}

	public int getVornameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getVornameSpalte(spielerIdx);
	}

	public int getNachnameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getNachnameSpalte(spielerIdx);
	}

	public int getVereinsnameSpalte(int spielerIdx) throws GenerateException {
		return delegate.getVereinsnameSpalte(spielerIdx);
	}

	public int getAktivSpalte() throws GenerateException {
		return delegate.getAktivSpalte();
	}

	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return delegate.getAktiveMeldungen();
	}

	public int getSpielerNameErsteSpalte() throws GenerateException {
		return delegate.getSpielerNameErsteSpalte();
	}

	public int getErsteDatenZiele() {
		return delegate.getErsteDatenZiele();
	}

	public int getLetzteDatenZeileUseMin() throws GenerateException {
		return delegate.getLetzteDatenZeileUseMin();
	}

	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		delegate.setAktiveSpielRunde(spielRundeNr);
	}

	// ---------------------------------------------------------------
	// Eigene Methoden
	// ---------------------------------------------------------------

	@Override
	public void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SCHWEIZER)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		getSheetHelper().removeAllSheetsExclude();
		setAktiveSpielRunde(SpielRundeNr.from(1));

		// Meldeliste mit Standardparametern anlegen (kein Dialog)
		meldeListe.createMeldelisteWithParams(TEST_FORMATION, true, true);

		testNamenEinfuegen();

		// Teamnummern vergeben und Meldeliste aktualisieren
		new SchweizerMeldeListeSheetUpdate(getWorkingSpreadsheet()).doRun();
	}

	private void testNamenEinfuegen() throws GenerateException {
		XSpreadsheet meldelisteSheet = meldeListe.getXSpreadSheet();
		getSheetHelper().setActiveSheet(meldelisteSheet);

		Formation formation = getKonfigurationSheet().getMeldeListeFormation();
		int anzSpielerProTeam = formation.getAnzSpieler();
		boolean teamnameAktiv = getKonfigurationSheet().isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAktiv = getKonfigurationSheet().isMeldeListeVereinsnameAnzeigen();

		// 2 Namen pro Spieler (Vorname + Nachname)
		List<String> testNamen = testnamenLoader.listeMitTestNamen(anzTeams * anzSpielerProTeam * 2);

		int nameIdx = 0;
		for (int team = 0; team < anzTeams && nameIdx < testNamen.size(); team++) {
			SheetRunner.testDoCancelTask();
			int zeile = ERSTE_DATEN_ZEILE + team;

			if (teamnameAktiv) {
				getSheetHelper().setStringValueInCell(StringCellValue.from(meldelisteSheet,
						Position.from(getTeamnameSpalte(), zeile), "Team " + (team + 1)));
			}

			for (int s = 0; s < anzSpielerProTeam && nameIdx + 1 < testNamen.size(); s++) {
				getSheetHelper().setStringValueInCell(StringCellValue.from(meldelisteSheet,
						Position.from(getVornameSpalte(s), zeile), testNamen.get(nameIdx++)));
				getSheetHelper().setStringValueInCell(StringCellValue.from(meldelisteSheet,
						Position.from(getNachnameSpalte(s), zeile), testNamen.get(nameIdx++)));
				if (vereinsnameAktiv) {
					getSheetHelper().setStringValueInCell(StringCellValue.from(meldelisteSheet,
							Position.from(getVereinsnameSpalte(s), zeile), "Verein " + ((team % 5) + 1)));
				}
			}

			// Aktiv-Spalte: alle Teams nehmen teil
			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(meldelisteSheet, Position.from(getAktivSpalte(), zeile),
							SchweizerListeDelegate.AKTIV_WERT_NIMMT_TEIL));
		}

		meldeListe.upDateSheet();
	}

}
