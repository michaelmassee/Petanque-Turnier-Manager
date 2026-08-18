package de.petanqueturniermanager.schweizer.spielrunde;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.schweizer.endrangliste.SchweizerEndranglisteSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.spieltagrangliste.SchweizerSpieltagRanglisteSheet;
import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

/**
 * Generiert ein vollständiges Schweizer-Beispielturnier mit mehreren Spieltagen und
 * Serien-Endrangliste – Beispielturnier-Pendant zu {@link SchweizerTurnierTestDaten}
 * (analog {@code SupermeleeTurnierTestDaten}).
 * <p>
 * Standard: 16 Teams, 2 Spieltage à 3 Runden.
 */
public class SchweizerSerienTurnierTestDaten extends SchweizerAbstractSpielrundeSheet {

	private static final int ANZ_SPIELTAGE_DEFAULT = 2;
	private static final int ANZ_RUNDEN_PRO_SPIELTAG_DEFAULT = 3;

	private final int anzSpieltage;
	private final int anzRundenProSpieltag;

	private final SchweizerMeldeListeSheetTestDaten meldelisteTestDaten;
	private final SchweizerMeldeListeSheetNew meldeListeSheetNew;
	public final SchweizerSpielrundeSheetNaechste naechsteSpielrunde;
	private final SchweizerTurnierTestDaten ergebnisHelfer;

	/** Standard-Konstruktor: 16 Teams, 2 Spieltage à 3 Runden. */
	public SchweizerSerienTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT, ANZ_SPIELTAGE_DEFAULT,
				ANZ_RUNDEN_PRO_SPIELTAG_DEFAULT);
	}

	/**
	 * Parametrisierter Konstruktor.
	 *
	 * @param anzTeams             Anzahl zu generierender Teams
	 * @param anzSpieltage         Anzahl Spieltage der Serie (mind. 2, damit die Endrangliste ein Streichresultat kennt)
	 * @param anzRundenProSpieltag Anzahl Spielrunden je Spieltag
	 */
	public SchweizerSerienTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams, int anzSpieltage,
			int anzRundenProSpieltag) {
		super(workingSpreadsheet);
		this.anzSpieltage = anzSpieltage;
		this.anzRundenProSpieltag = anzRundenProSpieltag;
		meldelisteTestDaten = new SchweizerMeldeListeSheetTestDaten(workingSpreadsheet, anzTeams);
		meldeListeSheetNew = new SchweizerMeldeListeSheetNew(workingSpreadsheet);
		naechsteSpielrunde = new SchweizerSpielrundeSheetNaechste(workingSpreadsheet);
		ergebnisHelfer = new SchweizerTurnierTestDaten(workingSpreadsheet, anzTeams, SpielplanTeamAnzeige.NR);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SCHWEIZER)
				.prefix(getLogPrefix()).validate()) {
			return;
		}
		getSheetHelper().removeAllSheetsExclude();
		generate();
	}

	public void generate() throws GenerateException {
		meldelisteTestDaten.doRun();
		naechsteSpielrunde.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R);

		for (int spieltag = 1; spieltag <= anzSpieltage; spieltag++) {
			SheetRunner.testDoCancelTask();
			processBoxinfo("processbox.schweizer.serie.spieltag", spieltag, anzSpieltage);

			for (int runde = 1; runde <= anzRundenProSpieltag; runde++) {
				SheetRunner.testDoCancelTask();
				naechsteSpielrunde.doRun();
				XSpreadsheet sheet = naechsteSpielrunde.getXSpreadSheet();
				if (sheet != null) {
					ergebnisHelfer.ergebnisseEinfuegen(sheet);
				}
			}

			if (spieltag < anzSpieltage) {
				// Archiviert die Rangliste dieses Spieltags und wechselt auf den nächsten.
				meldeListeSheetNew.naechsteSpieltag();
			} else {
				// Letzter Spieltag: nur archivieren, kein Wechsel mehr nötig.
				new SchweizerSpieltagRanglisteSheet(getWorkingSpreadsheet(), SpielTagNr.from(spieltag)).doRun();
			}
		}

		new SchweizerEndranglisteSheet(getWorkingSpreadsheet()).doRun();

		var konfig = naechsteSpielrunde.getKonfigurationSheet();
		konfig.setKopfZeileMitte(getTurnierSystem().getBezeichnung());
		konfig.seitenstileAktualisieren();
	}

}
