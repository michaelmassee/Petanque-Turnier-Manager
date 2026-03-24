/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter;

import java.util.concurrent.ThreadLocalRandom;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.maastrichter.finalrunde.MaastrichterFinalrundeSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetTestDaten;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Generiert ein vollständiges Maastrichter Beispielturnier ohne Dialoge:
 * <ol>
 *   <li>Meldeliste (konfigurierbare Anzahl Teams, Doublette)</li>
 *   <li>Vorrunden (Schweizer System) mit Zufallsergebnissen</li>
 *   <li>Vorrunden-Rangliste</li>
 *   <li>Finalrunden (A/B/C/…-Bracket nach GruppenAufteilungRechner)</li>
 * </ol>
 */
public class MaastrichterTurnierTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

	/** Standard-Konfiguration: 12 Teams, 3 Vorrunden, Gruppen à 16, Min-Rest 16 */
	private static final int DEFAULT_ANZ_TEAMS      = 12;
	private static final int DEFAULT_ANZ_VORRUNDEN  = 3;
	private static final int DEFAULT_GRUPPEN_GROESSE = 16;
	private static final int DEFAULT_MIN_REST_GROESSE = 16;

	private final int anzVorrunden;
	private final int gruppenGroesse;
	private final int minRestGroesse;

	private final MaastrichterMeldeListeSheetTestDaten meldelisteTestDaten;
	private final MaastrichterSpielrundeSheetNaechste naechsteVorrunde;
	private final MaastrichterVorrundenRanglisteSheet ranglisteSheet;
	private final MaastrichterFinalrundeSheet finalrundeSheet;

	/** Standard-Konstruktor: 12 Teams, 3 Vorrunden, gruppenGroesse=16, minRestGroesse=16 */
	public MaastrichterTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, DEFAULT_ANZ_TEAMS, DEFAULT_ANZ_VORRUNDEN,
				DEFAULT_GRUPPEN_GROESSE, DEFAULT_MIN_REST_GROESSE);
	}

	/**
	 * Parametrisierter Konstruktor für beliebige Szenarien.
	 *
	 * @param anzTeams        Anzahl zu generierender Teams
	 * @param anzVorrunden    Anzahl Schweizer Vorrunden
	 * @param gruppenGroesse  Maximale Teams pro KO-Finalgruppe (Zweierpotenz)
	 * @param minRestGroesse  Mindestzahl für eigene Restgruppe (Zweierpotenz)
	 */
	public MaastrichterTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet,
			int anzTeams, int anzVorrunden, int gruppenGroesse, int minRestGroesse) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Turnier-Testdaten");
		this.anzVorrunden   = anzVorrunden;
		this.gruppenGroesse = gruppenGroesse;
		this.minRestGroesse = minRestGroesse;
		meldelisteTestDaten = new MaastrichterMeldeListeSheetTestDaten(workingSpreadsheet, anzTeams);
		naechsteVorrunde = new MaastrichterSpielrundeSheetNaechste(workingSpreadsheet);
		ranglisteSheet = new MaastrichterVorrundenRanglisteSheet(workingSpreadsheet);
		finalrundeSheet = new MaastrichterFinalrundeSheet(workingSpreadsheet);
	}

	@Override
	protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
		return new MaastrichterKonfigurationSheet(getWorkingSpreadsheet());
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SheetNamen.meldeliste());
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	/**
	 * Öffentlicher Einstiegspunkt für Tests: generiert das vollständige Maastrichter
	 * Beispielturnier ohne Dialoge direkt auf dem aktuellen Dokument.
	 */
	public void generate() throws GenerateException {
		doRun();
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.MAASTRICHTER)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// 1. Meldeliste erstellen (löscht alle vorhandenen Sheets)
		meldelisteTestDaten.erstelleTestdaten();

		// Konfiguration setzen (überschreibt ggf. die Defaults der Testdaten-Klasse)
		MaastrichterKonfigurationSheet konfigSheet = new MaastrichterKonfigurationSheet(getWorkingSpreadsheet());
		konfigSheet.setSpielrundeSpielbahn(SpielrundeSpielbahn.R);
		konfigSheet.setAnzVorrunden(anzVorrunden);
		konfigSheet.setGruppenGroesse(gruppenGroesse);
		konfigSheet.setMinRestGroesse(minRestGroesse);

		// 2. Vorrunden erstellen und mit Zufallsergebnissen füllen
		for (int runde = 1; runde <= anzVorrunden; runde++) {
			SheetRunner.testDoCancelTask();
			processBoxinfo("processbox.erstelle.vorrunde", runde, anzVorrunden);
			naechsteVorrunde.erstelleNaechsteVorrunde();

			String legacyName = runde + ". " + SheetNamen.LEGACY_MAASTRICHTER_VORRUNDE_PRAEFIX;
			XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(
					getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
					SheetMetadataHelper.schluesselMaastrichterVorrunde(runde), legacyName);
			if (sheet != null) {
				ergebnisseEinfuegen(sheet);
			}
		}

		// 3. Vorrunden-Rangliste erstellen
		SheetRunner.testDoCancelTask();
		processBoxinfo("processbox.erstelle.rangliste");
		ranglisteSheet.doRun();

		// 4. Finalrunden erstellen
		SheetRunner.testDoCancelTask();
		processBoxinfo("processbox.erstelle.finalrunde");
		finalrundeSheet.doRun();
	}

	/**
	 * Füllt alle Paarungen des Vorrunden-Sheets mit Zufallsergebnissen (13:x).
	 */
	private void ergebnisseEinfuegen(XSpreadsheet sheet) throws GenerateException {
		RangePosition readRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 100);

		RangeData data = RangeHelper
				.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
				.getDataFromRange();

		for (int i = 0; i < data.size(); i++) {
			RowData row = data.get(i);
			if (row.size() < 2) break;

			int nrA = row.get(0).getIntVal(0);
			if (nrA <= 0) break;
			int nrB = row.get(1).getIntVal(0);
			if (nrB <= 0) continue; // Freilos – kein Ergebnis nötig

			int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + i;
			int winner = ThreadLocalRandom.current().nextInt(2);
			int loserPts = ThreadLocalRandom.current().nextInt(0, 13);
			int ergA = (winner == 0) ? 13 : loserPts;
			int ergB = (winner == 0) ? loserPts : 13;

			getSheetHelper().setNumberValueInCell(NumberCellValue
					.from(sheet, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile))
					.setValue(ergA));
			getSheetHelper().setNumberValueInCell(NumberCellValue
					.from(sheet, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, zeile))
					.setValue(ergB));
		}
	}

}
