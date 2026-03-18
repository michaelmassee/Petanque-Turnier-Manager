/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoSpielbaumTeamAnzeige;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetUpdate;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt und aktualisiert den K.-O.-Turnierbaum als Spreadsheet.<br>
 * <br>
 * Spaltenstruktur pro Runde (ohne Bahn): Nr | [Name] | Pkt | Connector<br>
 * Spaltenstruktur pro Runde (mit Bahn):  Bahn | Nr | [Name] | Pkt | Connector<br>
 * <br>
 * Zeile 0: Rundentitel (merged über alle Spalten der Runde)<br>
 * Zeile 1: Spalten-Überschriften (Bahn, Nr, Name, Pkt)<br>
 * Runde 1 (ab Zeile 2): direkte Einträge nach Setzliste (aus RNG-Spalte der Meldeliste)<br>
 * Runden 2+: WENN-Formeln berechnen Gewinner aus Vorrundenscores<br>
 * Abschlusskolonne: Sieger-Anzeige
 */
public class KoTurnierbaumSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(KoTurnierbaumSheet.class);

	public static final String SHEETNAME = "KO Turnierbaum";
	private static final String SHEET_COLOR = "8b0000";

	/** Header-Zeilen */
	private static final int HEADER_ZEILE_TITEL = 0;
	private static final int HEADER_ZEILE_SPALTEN = 1;

	/** Erste Datenzeile (nach 2 Header-Zeilen). */
	static final int ERSTE_ZEILE = 2;

	private static final int NR_COL_WIDTH = 700;
	private static final int BAHN_COL_WIDTH = 900;
	private static final int NAME_COL_WIDTH = 3000;
	private static final int SCORE_COL_WIDTH = 900;
	private static final int CONNECTOR_COL_WIDTH = 400;
	private static final int SIEGER_NAME_COL_WIDTH = 3200;

	private static final int HEADER_COLOR = 0x2544DD;
	private static final int TEAM_A_COLOR = 0xDCEEFA;
	private static final int TEAM_B_COLOR = 0xF0F7FF;
	private static final int SCORE_COLOR = 0xFFFDE7;
	private static final int SIEGER_COLOR = 0xFFD700;
	private static final int BAHN_COLOR = 0xEEEEEE;

	/** Unicode-Zeichen für die Konnektorspalte */
	private static final String CHAR_TOP = "┐";
	private static final String CHAR_BOTTOM = "┘";
	private static final String CHAR_MITTE = "│";

	// Konfigurations-State für die aktuelle Turnierbaum-Erstellung
	private SpielrundeSpielbahn spielbahn = SpielrundeSpielbahn.X;
	private KoSpielbaumTeamAnzeige teamAnzeige = KoSpielbaumTeamAnzeige.NR;

	// Spalten-Offsets (dynamisch je nach spielbahn)
	private int nrOffset = 0;
	private int nameOffset = 1;
	private int scoreOffset = 2;
	private int connectorOffset = 3;
	private int colGroupSize = 4;

	private final KoMeldeListeSheetUpdate meldeliste;

	public KoTurnierbaumSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Turnierbaum");
		meldeliste = new KoMeldeListeSheetUpdate(workingSpreadsheet);
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
	protected KoKonfigurationSheet getKonfigurationSheet() {
		return new KoKonfigurationSheet(getWorkingSpreadsheet());
	}

	// ---------------------------------------------------------------
	// Zeilenberechnung
	// ---------------------------------------------------------------

	/**
	 * Zeile für Team A eines Matches (0-basierte Matches, 1-basierte Runden, ERSTE_ZEILE-basierte Zeilen).
	 */
	int teamAZeile(int runde, int match) {
		if (runde == 1) {
			return match * 3 + ERSTE_ZEILE;
		}
		return teamBZeile(runde - 1, 2 * match);
	}

	/**
	 * Zeile für Team B eines Matches.
	 */
	int teamBZeile(int runde, int match) {
		if (runde == 1) {
			return match * 3 + ERSTE_ZEILE + 1;
		}
		return teamAZeile(runde - 1, 2 * match + 1);
	}

	// ---------------------------------------------------------------
	// Spaltenberechnung (dynamisch je nach spielbahn)
	// ---------------------------------------------------------------

	/** Bahn-Spalte für Runde r – nur gültig wenn mitBahn(). */
	int bahnSpalte(int runde) {
		return (runde - 1) * colGroupSize;
	}

	int nrSpalte(int runde) {
		return (runde - 1) * colGroupSize + nrOffset;
	}

	int nameSpalte(int runde) {
		return (runde - 1) * colGroupSize + nameOffset;
	}

	int scoreSpalte(int runde) {
		return (runde - 1) * colGroupSize + scoreOffset;
	}

	int connectorSpalte(int runde) {
		return (runde - 1) * colGroupSize + connectorOffset;
	}

	int siegerNrSpalte(int numRunden) {
		return numRunden * colGroupSize;
	}

	int siegerNameSpalte(int numRunden) {
		return numRunden * colGroupSize + 1;
	}

	private boolean mitBahn() {
		return spielbahn != SpielrundeSpielbahn.X;
	}

	// ---------------------------------------------------------------
	// Setzliste
	// ---------------------------------------------------------------

	/**
	 * Berechnet die Standard-Einzeleliminierung-Setzliste für n Teams (n muss Zweierpotenz sein).<br>
	 * Für n=8: [1,8,4,5,2,7,3,6] → Matches [1v8, 4v5, 2v7, 3v6]<br>
	 * Garantiert: Seed 1 und Seed 2 treffen sich erst im Finale.
	 */
	int[] berechneSetzliste(int n) {
		if (n == 1) {
			return new int[] { 1 };
		}
		int[] prev = berechneSetzliste(n / 2);
		int[] result = new int[n];
		for (int i = 0; i < n / 2; i++) {
			result[2 * i] = prev[i];
			result[2 * i + 1] = n + 1 - prev[i];
		}
		return result;
	}

	// ---------------------------------------------------------------
	// Hauptlogik
	// ---------------------------------------------------------------

	/**
	 * Erstellt den Turnierbaum ohne Rückfrage-Dialog.<br>
	 * Wird von Testdaten-Klassen aufgerufen.
	 */
	public void erstelleTurnierbaumOhneDialog() throws GenerateException {
		TeamMeldungen meldungen = meldeliste.getMeldungenSortiertNachRangliste();
		int anzTeams = meldungen.size();
		if (anzTeams < 2) {
			return;
		}
		int bracketGroesse = Integer.highestOneBit(anzTeams);
		int numRunden = Integer.numberOfTrailingZeros(bracketGroesse);

		NewSheet.from(this, SHEETNAME)
				.pos(DefaultSheetPos.KO_TURNIERBAUM)
				.hideGrid()
				.tabColor(SHEET_COLOR)
				.setActiv()
				.create();

		XSpreadsheet xSheet = getXSpreadSheet();
		TurnierSheet.from(xSheet, getWorkingSpreadsheet()).setActiv();
		erstelleTurnierbaum(xSheet, meldungen, numRunden, bracketGroesse);
	}

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet meldelisteXSheet = meldeliste.getXSpreadSheet();
		if (meldelisteXSheet == null) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption("K.-O. Turnierbaum")
					.message("Meldeliste nicht gefunden. Bitte zuerst die Meldeliste erstellen.")
					.show();
			return;
		}

		String rangFehler = meldeliste.validiereRangSpalte();
		if (rangFehler != null) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption("K.-O. Turnierbaum – Rang-Fehler")
					.message(rangFehler)
					.show();
			return;
		}

		TeamMeldungen meldungen = meldeliste.getMeldungenSortiertNachRangliste();
		int anzTeams = meldungen.size();

		if (anzTeams < 2) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption("K.-O. Turnierbaum")
					.message("Mindestens 2 Teams erforderlich. Aktuell: " + anzTeams)
					.show();
			return;
		}

		int bracketGroesse = Integer.highestOneBit(anzTeams);
		if (bracketGroesse < anzTeams) {
			MessageBoxResult antwort = MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.WARN_OK_CANCEL)
					.caption("K.-O. Turnierbaum")
					.message(anzTeams + " Teams gefunden. Der Turnierbaum wird für " + bracketGroesse
							+ " Teams (nächste Zweierpotenz) erstellt.\n"
							+ (anzTeams - bracketGroesse) + " Teams werden NICHT berücksichtigt.\n\n"
							+ "Fortfahren?")
					.show();
			if (antwort != MessageBoxResult.OK) {
				return;
			}
		}

		int numRunden = Integer.numberOfTrailingZeros(bracketGroesse);

		NewSheet.from(this, SHEETNAME)
				.pos(DefaultSheetPos.KO_TURNIERBAUM)
				.hideGrid()
				.tabColor(SHEET_COLOR)
				.setActiv()
				.create();

		XSpreadsheet xSheet = getXSpreadSheet();
		TurnierSheet.from(xSheet, getWorkingSpreadsheet()).setActiv();

		erstelleTurnierbaum(xSheet, meldungen, numRunden, bracketGroesse);
	}

	private void erstelleTurnierbaum(XSpreadsheet xSheet, TeamMeldungen meldungen, int numRunden,
			int bracketGroesse) throws GenerateException {

		sheet().processBoxinfo("K.-O. Turnierbaum erstellen");

		// Konfiguration für diese Erstellung lesen
		KoKonfigurationSheet konfig = getKonfigurationSheet();
		this.spielbahn = konfig.getSpielbaumSpielbahn();
		this.teamAnzeige = konfig.getSpielbaumTeamAnzeige();

		if (mitBahn()) {
			this.nrOffset = 1;
			this.nameOffset = 2;
			this.scoreOffset = 3;
			this.connectorOffset = 4;
			this.colGroupSize = 5;
		} else {
			this.nrOffset = 0;
			this.nameOffset = 1;
			this.scoreOffset = 2;
			this.connectorOffset = 3;
			this.colGroupSize = 4;
		}

		int[] setzliste = berechneSetzliste(bracketGroesse);

		// Spaltenbreiten setzen
		formatiereKolumnen(xSheet, numRunden);

		// Header (2 Zeilen: Titel + Spaltenbeschriftung)
		schreibeHeader(xSheet, numRunden);

		// Runde 1: direkte Einträge aus Setzliste
		int anzMatchesR1 = bracketGroesse / 2;
		int[] bahnR1 = berechneBahnNummern(anzMatchesR1);
		for (int m = 0; m < anzMatchesR1; m++) {
			int seedA = setzliste[2 * m];
			int seedB = setzliste[2 * m + 1];
			int nrA = getTeamNrBySeedPosition(meldungen, seedA);
			int nrB = getTeamNrBySeedPosition(meldungen, seedB);

			int rowA = teamAZeile(1, m);
			int rowB = teamBZeile(1, m);

			if (mitBahn()) {
				schreibeBahnZelle(xSheet, 1, rowA, bahnR1[m]);
				schreibeBahnZelle(xSheet, 1, rowB, 0);
			}
			schreibeNrZelleR1(xSheet, rowA, nrA, true);
			schreibeNrZelleR1(xSheet, rowB, nrB, false);
			schreibeNameFormel(xSheet, 1, rowA);
			schreibeNameFormel(xSheet, 1, rowB);
			schreibeScoreZelle(xSheet, 1, rowA, true);
			schreibeScoreZelle(xSheet, 1, rowB, false);
			zeichneMatchConnector(xSheet, 1, rowA, rowB);
		}

		// Runden 2+: Gewinner-Formeln
		for (int r = 2; r <= numRunden; r++) {
			int anzMatches = bracketGroesse / (1 << r);
			int[] bahnRunde = berechneBahnNummern(anzMatches);
			for (int m = 0; m < anzMatches; m++) {
				int rowA = teamAZeile(r, m);
				int rowB = teamBZeile(r, m);

				if (mitBahn()) {
					schreibeBahnZelle(xSheet, r, rowA, bahnRunde[m]);
					schreibeBahnZelle(xSheet, r, rowB, 0);
				}
				schreibeGewinnerFormel(xSheet, r, m, true);
				schreibeGewinnerFormel(xSheet, r, m, false);
				schreibeNameFormel(xSheet, r, rowA);
				schreibeNameFormel(xSheet, r, rowB);
				schreibeScoreZelle(xSheet, r, rowA, true);
				schreibeScoreZelle(xSheet, r, rowB, false);
				zeichneMatchConnector(xSheet, r, rowA, rowB);
			}
		}

		// Sieger-Spalte
		schreibeSieger(xSheet, numRunden);
	}

	/**
	 * Berechnet Bahnnummern für anzMatches Matches entsprechend der aktuellen spielbahn-Einstellung.
	 * <ul>
	 * <li>X: alle 0 (wird nicht aufgerufen)</li>
	 * <li>L: alle 0 (leere Zellen, Benutzer füllt manuell)</li>
	 * <li>N: [1, 2, ..., anzMatches]</li>
	 * <li>R: [1..anzMatches] zufällig gemischt</li>
	 * </ul>
	 */
	private int[] berechneBahnNummern(int anzMatches) {
		int[] bahnen = new int[anzMatches];
		if (spielbahn == SpielrundeSpielbahn.N) {
			for (int i = 0; i < anzMatches; i++) {
				bahnen[i] = i + 1;
			}
		} else if (spielbahn == SpielrundeSpielbahn.R) {
			for (int i = 0; i < anzMatches; i++) {
				bahnen[i] = i + 1;
			}
			Random rng = new Random();
			for (int i = anzMatches - 1; i > 0; i--) {
				int j = rng.nextInt(i + 1);
				int tmp = bahnen[i];
				bahnen[i] = bahnen[j];
				bahnen[j] = tmp;
			}
		}
		// L und X → bahnen bleibt alle 0
		return bahnen;
	}

	// ---------------------------------------------------------------
	// Formatierung
	// ---------------------------------------------------------------

	private void formatiereKolumnen(XSpreadsheet xSheet, int numRunden) throws GenerateException {
		// Bei "keine Bahn": Nr-Spalte verstecken wenn Teamname angezeigt wird (nur eine Team-Spalte sichtbar)
		boolean nrVersteckt = !mitBahn() && teamAnzeige == KoSpielbaumTeamAnzeige.NAME;
		int nrColWidth = nrVersteckt ? 0 : NR_COL_WIDTH;
		int nameColWidth = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) ? NAME_COL_WIDTH : 0;

		for (int r = 1; r <= numRunden; r++) {
			if (mitBahn()) {
				getSheetHelper().setColumnProperties(xSheet, bahnSpalte(r),
						ColumnProperties.from().setWidth(BAHN_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
								.setVertJustify(CellVertJustify2.CENTER));
			}
			getSheetHelper().setColumnProperties(xSheet, nrSpalte(r),
					ColumnProperties.from().setWidth(nrColWidth).setHoriJustify(CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
			getSheetHelper().setColumnProperties(xSheet, nameSpalte(r),
					ColumnProperties.from().setWidth(nameColWidth).setHoriJustify(CellHoriJustify.LEFT)
							.setVertJustify(CellVertJustify2.CENTER));
			getSheetHelper().setColumnProperties(xSheet, scoreSpalte(r),
					ColumnProperties.from().setWidth(SCORE_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
			getSheetHelper().setColumnProperties(xSheet, connectorSpalte(r),
					ColumnProperties.from().setWidth(CONNECTOR_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
		}

		// Sieger-Spalten
		int siegerNrWidth = nrVersteckt ? 0 : NR_COL_WIDTH;
		getSheetHelper().setColumnProperties(xSheet, siegerNrSpalte(numRunden),
				ColumnProperties.from().setWidth(siegerNrWidth).setHoriJustify(CellHoriJustify.CENTER)
						.setVertJustify(CellVertJustify2.CENTER));
		int siegerNameWidth = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) ? SIEGER_NAME_COL_WIDTH : 0;
		getSheetHelper().setColumnProperties(xSheet, siegerNameSpalte(numRunden),
				ColumnProperties.from().setWidth(siegerNameWidth).setHoriJustify(CellHoriJustify.LEFT)
						.setVertJustify(CellVertJustify2.CENTER));
	}

	private void schreibeHeader(XSpreadsheet xSheet, int numRunden) throws GenerateException {
		for (int r = 1; r <= numRunden; r++) {
			String rundentitel = berechnRundenTitel(r, numRunden);

			// Zeile 0: Rundentitel über alle Spalten der Runde (merged)
			int titelStartSpalte = mitBahn() ? bahnSpalte(r) : nrSpalte(r);
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(titelStartSpalte, HEADER_ZEILE_TITEL), rundentitel)
							.setEndPosMergeSpaltePlus(colGroupSize - 1)
							.setCharWeight(FontWeight.BOLD)
							.setHoriJustify(CellHoriJustify.CENTER)
							.setCellBackColor(HEADER_COLOR)
							.setCharColor("FFFFFF")
							.setBorder(BorderFactory.from().allThin().toBorder()));

			// Zeile 1: Spalten-Überschriften
			if (mitBahn()) {
				schreibeSpaltenHeader(xSheet, bahnSpalte(r), "Bahn");
			}
			schreibeSpaltenHeader(xSheet, nrSpalte(r), "Nr");
			if (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) {
				schreibeSpaltenHeader(xSheet, nameSpalte(r), "Teamname");
			}
			schreibeSpaltenHeader(xSheet, scoreSpalte(r), "Pkt");
		}

		// Sieger-Header
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(siegerNrSpalte(numRunden), HEADER_ZEILE_TITEL), "Sieger")
						.setEndPosMergeSpaltePlus(1)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCellBackColor(SIEGER_COLOR)
						.setBorder(BorderFactory.from().allThin().toBorder()));
	}

	private String berechnRundenTitel(int r, int numRunden) {
		if (r == numRunden) {
			return "Finale";
		} else if (r == numRunden - 1) {
			return "Halbfinale";
		} else {
			int anzTeamsInRunde = 1 << (numRunden - r + 1);
			return "1/" + (anzTeamsInRunde / 2) + "-Finale";
		}
	}

	private void schreibeSpaltenHeader(XSpreadsheet xSheet, int spalte, String label) throws GenerateException {
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(spalte, HEADER_ZEILE_SPALTEN), label)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCellBackColor(HEADER_COLOR)
						.setCharColor("FFFFFF")
						.setBorder(BorderFactory.from().allThin().toBorder()));
	}

	// ---------------------------------------------------------------
	// Zellinhalte
	// ---------------------------------------------------------------

	private void schreibeNrZelleR1(XSpreadsheet xSheet, int zeile, int nr, boolean istTeamA)
			throws GenerateException {
		if (nr > 0) {
			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(xSheet, Position.from(nrSpalte(1), zeile))
							.setValue(nr)
							.setCellBackColor(istTeamA ? TEAM_A_COLOR : TEAM_B_COLOR)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
		} else {
			// Freilos
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(nrSpalte(1), zeile), "Freilos")
							.setCellBackColor(istTeamA ? TEAM_A_COLOR : TEAM_B_COLOR)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
		}
	}

	/**
	 * Schreibt eine Bahnnummer-Zelle. bahnNr=0 → leere editierbare Zelle (Modus L oder teamB-Zeile).
	 * Nur für Team-A-Zeile wird die eigentliche Nummer eingetragen (Modus N/R).
	 */
	private void schreibeBahnZelle(XSpreadsheet xSheet, int runde, int zeile, int bahnNr)
			throws GenerateException {
		if (bahnNr > 0) {
			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(xSheet, Position.from(bahnSpalte(runde), zeile))
							.setValue(bahnNr)
							.setCellBackColor(BAHN_COLOR)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
		} else {
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(bahnSpalte(runde), zeile), "")
							.setCellBackColor(BAHN_COLOR)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
		}
	}

	private void schreibeNameFormel(XSpreadsheet xSheet, int runde, int zeile) throws GenerateException {
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) {
			String nrAddr = Position.from(nrSpalte(runde), zeile).getAddressWith$();
			// ISTZAHL prüft ob die Nr-Zelle eine Zahl enthält (nicht "Freilos", "?" oder leer)
			String formel = "WENN(ISTZAHL(" + nrAddr + ")*(" + nrAddr + ">0);SVERWEIS(" + nrAddr
					+ ";" + MeldeListeKonstanten.SHEETNAME + ".$A:$B;2;0);\"\")";
			getSheetHelper().setFormulaInCell(
					StringCellValue.from(xSheet, Position.from(nameSpalte(runde), zeile), formel)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.LEFT));
		} else {
			// NR-Modus: Namenspalte leer (Breite = 0, gesetzt in formatiereKolumnen)
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(nameSpalte(runde), zeile), ""));
		}
	}

	private void schreibeScoreZelle(XSpreadsheet xSheet, int runde, int zeile, boolean istTeamA)
			throws GenerateException {
		// Score-Zelle ist editierbar (leer, Benutzer trägt Ergebnis ein)
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(scoreSpalte(runde), zeile), "")
						.setCellBackColor(SCORE_COLOR)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(CellHoriJustify.CENTER));
	}

	/**
	 * Schreibt die WENN-Gewinner-Formel für Runden 2+.<br>
	 * Gewinner = Team mit höherer Punktzahl. Bei Gleichstand: "?".
	 */
	private void schreibeGewinnerFormel(XSpreadsheet xSheet, int runde, int match, boolean istTeamA)
			throws GenerateException {
		int feederMatch = istTeamA ? (2 * match) : (2 * match + 1);
		int feederRunde = runde - 1;

		int rowFeederA = teamAZeile(feederRunde, feederMatch);
		int rowFeederB = teamBZeile(feederRunde, feederMatch);

		String scoreAAddr = Position.from(scoreSpalte(feederRunde), rowFeederA).getAddressWith$();
		String scoreBAddr = Position.from(scoreSpalte(feederRunde), rowFeederB).getAddressWith$();
		String nrAAddr = Position.from(nrSpalte(feederRunde), rowFeederA).getAddressWith$();
		String nrBAddr = Position.from(nrSpalte(feederRunde), rowFeederB).getAddressWith$();

		// ISTZAHL: Score muss eine Zahl sein (nicht leer) damit Gewinner berechnet wird
		String formel = "WENN(ISTZAHL(" + scoreAAddr + ")*ISTZAHL(" + scoreBAddr + ");"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + nrAAddr + ";"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + nrBAddr + ";\"?\"));"
				+ "\"\")";

		int targetRow = istTeamA ? teamAZeile(runde, match) : teamBZeile(runde, match);
		boolean istA = istTeamA;

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(nrSpalte(runde), targetRow), formel)
						.setCellBackColor(istA ? TEAM_A_COLOR : TEAM_B_COLOR)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(CellHoriJustify.CENTER));
	}

	/**
	 * Zeichnet den visuellen Konnektor (┐ │ ┘) für ein Match in die Konnektorspalte.
	 */
	private void zeichneMatchConnector(XSpreadsheet xSheet, int runde, int rowA, int rowB)
			throws GenerateException {
		int connSpalte = connectorSpalte(runde);

		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(connSpalte, rowA), CHAR_TOP)
						.setHoriJustify(CellHoriJustify.RIGHT));

		for (int z = rowA + 1; z < rowB; z++) {
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(connSpalte, z), CHAR_MITTE)
							.setHoriJustify(CellHoriJustify.RIGHT));
		}

		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(connSpalte, rowB), CHAR_BOTTOM)
						.setHoriJustify(CellHoriJustify.RIGHT));
	}

	/**
	 * Zeigt den Turniersieger nach dem Finale an.
	 */
	private void schreibeSieger(XSpreadsheet xSheet, int numRunden) throws GenerateException {
		int finaleMatch = 0;
		int rowFinaleA = teamAZeile(numRunden, finaleMatch);
		int rowFinaleB = teamBZeile(numRunden, finaleMatch);
		int siegerZeile = (rowFinaleA + rowFinaleB) / 2;

		int siegerNrSp = siegerNrSpalte(numRunden);
		int siegerNameSp = siegerNameSpalte(numRunden);

		String scoreAAddr = Position.from(scoreSpalte(numRunden), rowFinaleA).getAddressWith$();
		String scoreBAddr = Position.from(scoreSpalte(numRunden), rowFinaleB).getAddressWith$();
		String nrAAddr = Position.from(nrSpalte(numRunden), rowFinaleA).getAddressWith$();
		String nrBAddr = Position.from(nrSpalte(numRunden), rowFinaleB).getAddressWith$();

		// Sieger-Nr
		String siegerNrFormel = "WENN(ISTZAHL(" + scoreAAddr + ")*ISTZAHL(" + scoreBAddr + ");"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + nrAAddr + ";"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + nrBAddr + ";\"?\"));"
				+ "\"\")";

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(siegerNrSp, siegerZeile), siegerNrFormel)
						.setCellBackColor(SIEGER_COLOR)
						.setBorder(BorderFactory.from().allBold().toBorder())
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.CENTER));

		// Sieger-Name (nur im NAME-Modus)
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) {
			String siegerNrAddr = Position.from(siegerNrSp, siegerZeile).getAddressWith$();
			String siegerNameFormel = "WENN(ISTZAHL(" + siegerNrAddr + ")*(" + siegerNrAddr
					+ ">0);SVERWEIS(" + siegerNrAddr + ";" + MeldeListeKonstanten.SHEETNAME + ".$A:$B;2;0);\"\")";

			getSheetHelper().setFormulaInCell(
					StringCellValue.from(xSheet, Position.from(siegerNameSp, siegerZeile), siegerNameFormel)
							.setCellBackColor(SIEGER_COLOR)
							.setBorder(BorderFactory.from().allBold().toBorder())
							.setCharWeight(FontWeight.BOLD)
							.setHoriJustify(CellHoriJustify.LEFT));
		}
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden
	// ---------------------------------------------------------------

	/**
	 * Liefert die Team-Nr des Teams an Setzposition {@code seedPos} (1-basiert).
	 * Gibt 0 zurück wenn kein Team an dieser Position vorhanden ist (Freilos).
	 */
	private int getTeamNrBySeedPosition(TeamMeldungen meldungen, int seedPos) {
		var teams = meldungen.teams();
		if (seedPos < 1 || seedPos > teams.size()) {
			return 0; // Freilos
		}
		return teams.get(seedPos - 1).getNr();
	}

	private KoTurnierbaumSheet sheet() {
		return this;
	}

}
