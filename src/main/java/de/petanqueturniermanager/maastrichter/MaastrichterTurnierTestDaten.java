/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.maastrichter.finalrunde.MaastrichterFinalrundeSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterGruppenModus;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetTestDaten;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterTeilnehmerSheet;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Generiert ein vollständiges Maastrichter Beispielturnier ohne Dialoge:
 * <ol>
 *   <li>Meldeliste (konfigurierbare Anzahl Teams, Doublette)</li>
 *   <li>Vorrunden (Schweizer System) mit Zufallsergebnissen</li>
 *   <li>Vorrunden-Rangliste</li>
 *   <li>Finalrunden (A/B/C/…-Bracket nach Gruppengröße)</li>
 * </ol>
 */
public class MaastrichterTurnierTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

	/** Standard-Konfiguration: 12 Teams, 3 Vorrunden, Gruppen à 16 */
	private static final int DEFAULT_ANZ_TEAMS      = 12;
	private static final int DEFAULT_ANZ_VORRUNDEN  = 3;
	private static final int DEFAULT_GRUPPEN_GROESSE = 16;

	private final int anzVorrunden;
	private final int gruppenGroesse;
	private TeamAnzeige spielplanTeamAnzeige = TeamAnzeige.NR;

	private final MaastrichterMeldeListeSheetTestDaten meldelisteTestDaten;
	final MaastrichterSpielrundeSheetNaechste naechsteVorrunde;
	final MaastrichterVorrundenRanglisteSheet ranglisteSheet;
	final MaastrichterFinalrundeSheet finalrundeSheet;
	private final MaastrichterTeilnehmerSheet teilnehmerSheet;

	/** Standard-Konstruktor: 12 Teams, 3 Vorrunden, gruppenGroesse=16 */
	public MaastrichterTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, DEFAULT_ANZ_TEAMS, DEFAULT_ANZ_VORRUNDEN, DEFAULT_GRUPPEN_GROESSE);
	}

	/**
	 * Parametrisierter Konstruktor für beliebige Szenarien.
	 *
	 * @param anzTeams        Anzahl zu generierender Teams
	 * @param anzVorrunden    Anzahl Schweizer Vorrunden
	 * @param gruppenGroesse  Maximale Teams pro KO-Finalgruppe (Zweierpotenz)
	 */
	public MaastrichterTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet,
			int anzTeams, int anzVorrunden, int gruppenGroesse) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Turnier-Testdaten");
		this.anzVorrunden   = anzVorrunden;
		this.gruppenGroesse = gruppenGroesse;
		meldelisteTestDaten = new MaastrichterMeldeListeSheetTestDaten(workingSpreadsheet, anzTeams);
		naechsteVorrunde = new MaastrichterSpielrundeSheetNaechste(workingSpreadsheet);
		ranglisteSheet = new MaastrichterVorrundenRanglisteSheet(workingSpreadsheet);
		finalrundeSheet = new MaastrichterFinalrundeSheet(workingSpreadsheet);
		teilnehmerSheet = new MaastrichterTeilnehmerSheet(workingSpreadsheet);
	}

	/** Sichtbare Teamkennung in den Vorrunden (Standard: Nummer). */
	public MaastrichterTurnierTestDaten mitSpielplanTeamAnzeige(TeamAnzeige anzeige) {
		spielplanTeamAnzeige = anzeige;
		return this;
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

		generate(anzVorrunden, true);

		// Teilnehmerliste erstellen
		SheetRunner.testDoCancelTask();
		processBoxinfo("processbox.erstelle.teilnehmerliste");
		teilnehmerSheet.generate();

		// Kopfzeile und Werbefußzeile setzen
		MaastrichterKonfigurationSheet konfigSheet = new MaastrichterKonfigurationSheet(getWorkingSpreadsheet());
		konfigSheet.setKopfZeileMitte(getTurnierSystem().getBezeichnung());
		konfigSheet.seitenstileAktualisieren();
	}

	/**
	 * Generiert nur die ersten {@code anzVorrundenZuSpielen} Vorrunden (mit Ergebnissen), optional
	 * gefolgt von Vorrunden-Rangliste + Finalrunden. Für Tests, die zwischen zwei Vorrunden
	 * eingreifen wollen (z.B. Team auf "ausgestiegen" setzen).
	 */
	public void generate(int anzVorrundenZuSpielen, boolean mitRanglisteUndFinale) throws GenerateException {
		// 1. Meldeliste erstellen (löscht alle vorhandenen Sheets)
		meldelisteTestDaten.erstelleTestdaten();

		// Konfiguration setzen (überschreibt ggf. die Defaults der Testdaten-Klasse)
		MaastrichterKonfigurationSheet konfigSheet = new MaastrichterKonfigurationSheet(getWorkingSpreadsheet());
		konfigSheet.setSpielrundeSpielbahn(SpielrundeSpielbahn.R);
		konfigSheet.setSpielplanTeamAnzeige(spielplanTeamAnzeige);
		konfigSheet.setAnzVorrunden(anzVorrunden);
		konfigSheet.setGruppenGroesse(gruppenGroesse);
		konfigSheet.setMaastrichterGruppenModus(MaastrichterGruppenModus.NACH_GROESSE);

		// 2. Vorrunden erstellen und mit Zufallsergebnissen füllen
		for (int runde = 1; runde <= anzVorrundenZuSpielen; runde++) {
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

		if (mitRanglisteUndFinale) {
			// 3. Vorrunden-Rangliste erstellen
			SheetRunner.testDoCancelTask();
			processBoxinfo("processbox.erstelle.rangliste");
			ranglisteSheet.doRun();

			// 4. Finalrunden erstellen
			SheetRunner.testDoCancelTask();
			processBoxinfo("processbox.erstelle.finalrunde");
			finalrundeSheet.doRun();
		}
	}

	/**
	 * Füllt alle Paarungen des Vorrunden-Sheets mit Zufallsergebnissen (13:x). Paarungen werden
	 * über den sichtbaren Zellinhalt erkannt, damit Nummern- und Namensanzeige gleichermaßen
	 * funktionieren; vorbelegte Freilos-Ergebnisse bleiben unverändert.
	 */
	void ergebnisseEinfuegen(XSpreadsheet sheet) throws GenerateException {
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		RangeData paarungen = RangeHelper.from(sheet, xDoc, RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 100)).getDataFromRange();

		RangeData ergebnisse = new RangeData();
		for (RowData paarung : paarungen) {
			if (paarung.size() < 4 || istLeer(paarung.get(0))) {
				break;
			}
			if (istLeer(paarung.get(1))) {
				// Freilos – vorbelegte Zellinhalte unverändert zurückschreiben (leer bleibt leer)
				RowData freilos = ergebnisse.addNewRow();
				freilos.add(paarung.get(2));
				freilos.add(paarung.get(3));
				continue;
			}
			int winner = RandomSource.nextInt(2);
			int loserPts = RandomSource.nextInt(0, 13);
			ergebnisse.addNewRow(winner == 0 ? 13 : loserPts, winner == 0 ? loserPts : 13);
		}
		if (ergebnisse.isEmpty()) {
			return;
		}
		RangeHelper.from(sheet, xDoc, ergebnisse.getRangePosition(Position.from(
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE)))
				.setDataInRange(ergebnisse);
	}

	private static boolean istLeer(CellData zelle) {
		String inhalt = zelle.getStringVal();
		return inhalt == null || inhalt.isEmpty();
	}

}
