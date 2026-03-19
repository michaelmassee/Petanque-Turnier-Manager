/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.CadrageRechner;
import de.petanqueturniermanager.algorithmen.GruppenAufteilungRechner;
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
 * Spaltenstruktur pro Runde (ohne Bahn): Team | Pkt | Connector<br>
 * Spaltenstruktur pro Runde (mit Bahn):  Bahn | Team | Pkt | Connector<br>
 * <br>
 * Die Team-Spalte enthält im NR-Modus die Teamnummer, im NAME-Modus den Teamnamen (SVERWEIS).<br>
 * <br>
 * Zeile 0: Rundentitel (merged über alle Spalten der Runde)<br>
 * Zeile 1: Spalten-Überschriften (Bahn, Nr/Teamname, Pkt)<br>
 * Runde 1 (ab Zeile 2): direkte Einträge nach Setzliste (aus RNG-Spalte der Meldeliste)<br>
 * Runden 2+: WENN-Formeln berechnen Gewinner aus Vorrundenscores<br>
 * Abschlusskolonne: Sieger-Anzeige
 */
public class KoTurnierbaumSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(KoTurnierbaumSheet.class);

	public static final String SHEETNAME_PREFIX = "KO Turnierbaum";
	/** Blattname für eine einzelne Gruppe (kein Buchstabe-Suffix). */
	public static final String SHEETNAME = SHEETNAME_PREFIX;
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

	// Farben – aus Konfiguration gelesen (Standardwerte als Fallback)
	private int headerFarbe       = 0x2544DD;
	private int teamAFarbe        = 0xDCEEFA;
	private int teamBFarbe        = 0xF0F7FF;
	private int scoreFarbe        = 0xFFFDE7;
	private int siegerFarbe       = 0xFFD700;
	private int bahnFarbe         = 0xEEEEEE;
	private int drittePlatzFarbe  = 0xCD7F32;

	/** Unicode-Zeichen für die Konnektorspalte */
	private static final String CHAR_TOP = "┐";
	private static final String CHAR_BOTTOM = "┘";
	private static final String CHAR_MITTE = "│";

	// Konfigurations-State für die aktuelle Turnierbaum-Erstellung
	private SpielrundeSpielbahn spielbahn = SpielrundeSpielbahn.X;
	private KoSpielbaumTeamAnzeige teamAnzeige = KoSpielbaumTeamAnzeige.NR;
	private boolean spielUmPlatz3 = false;

	// Aktuell in Erstellung befindlicher Gruppen-Sheet-Name (für getXSpreadSheet())
	private String aktuellerGruppenSheetName = null;

	// Cadrage-State
	private boolean mitCadrage = false;
	private int cadrageSpaltOffset = 0; // = colGroupSize wenn Cadrage vorhanden, sonst 0
	private int anzCadrageMatches = 0;
	private int anzFreilose = 0;
	private int gesanzTeamsIntern = 0;

	// Spalten-Offsets (dynamisch je nach spielbahn)
	// Mit Bahn:    Bahn(0) | Team(1) | Score(2) | Connector(3)  → colGroupSize = 4
	// Ohne Bahn:   Team(0) | Score(1) | Connector(2)             → colGroupSize = 3
	private int teamOffset = 0;
	private int scoreOffset = 1;
	private int connectorOffset = 2;
	private int colGroupSize = 3;

	private final KoMeldeListeSheetUpdate meldeliste;

	public KoTurnierbaumSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Turnierbaum");
		meldeliste = new KoMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		// Während einer Gruppen-Erstellung: den aktuellen Gruppen-Sheet zurückgeben
		// (wird von NewSheet/PageStyleHelper.applytoSheet() benötigt)
		if (aktuellerGruppenSheetName != null) {
			XSpreadsheet sheet = getSheetHelper().findByName(aktuellerGruppenSheetName);
			if (sheet != null) {
				return sheet;
			}
		}
		// Fallback: erste vorhandene Gruppe (Einzelgruppe ohne Buchstabe, dann A)
		XSpreadsheet sheet = getSheetHelper().findByName(SHEETNAME_PREFIX);
		if (sheet != null) {
			return sheet;
		}
		return getSheetHelper().findByName(SHEETNAME_PREFIX + " A");
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
		return (runde - 1) * colGroupSize + cadrageSpaltOffset;
	}

	/** Team-Spalte für Runde r (enthält Nr oder Teamname je nach teamAnzeige). */
	int teamSpalte(int runde) {
		return (runde - 1) * colGroupSize + teamOffset + cadrageSpaltOffset;
	}

	int scoreSpalte(int runde) {
		return (runde - 1) * colGroupSize + scoreOffset + cadrageSpaltOffset;
	}

	int connectorSpalte(int runde) {
		return (runde - 1) * colGroupSize + connectorOffset + cadrageSpaltOffset;
	}

	int siegerSpalte(int numRunden) {
		return numRunden * colGroupSize + cadrageSpaltOffset;
	}

	int siegerNameSpalte(int numRunden) {
		return numRunden * colGroupSize + 1 + cadrageSpaltOffset;
	}

	private boolean mitBahn() {
		return spielbahn != SpielrundeSpielbahn.X;
	}

	// Cadrage-Spalten (immer am linken Rand, kein Offset)
	int cadrageTeamSpalte() {
		return teamOffset;
	}

	int cadrageScoreSpalte() {
		return scoreOffset;
	}

	int cadrageConnectorSpalte() {
		return connectorOffset;
	}

	int cadrageBahnSpalte() {
		return 0;
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
	 * Erstellt alle Gruppen-Turnierbäume ohne Rückfrage-Dialog.<br>
	 * Wird von Testdaten-Klassen aufgerufen.
	 */
	public void erstelleTurnierbaumOhneDialog() throws GenerateException {
		TeamMeldungen alleMeldungen = meldeliste.getMeldungenSortiertNachRangliste();
		if (alleMeldungen.size() < 2) {
			return;
		}
		int gruppenGroesse = getKonfigurationSheet().getGruppenGroesse();
		erstelleAlleGruppenBaeume(alleMeldungen, gruppenGroesse);
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

		TeamMeldungen alleMeldungen = meldeliste.getMeldungenSortiertNachRangliste();
		if (alleMeldungen.size() < 2) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption("K.-O. Turnierbaum")
					.message("Mindestens 2 Teams erforderlich. Aktuell: " + alleMeldungen.size())
					.show();
			return;
		}

		int gruppenGroesse = getKonfigurationSheet().getGruppenGroesse();
		erstelleAlleGruppenBaeume(alleMeldungen, gruppenGroesse);
	}

	// ---------------------------------------------------------------
	// Gruppen-Logik
	// ---------------------------------------------------------------

	/**
	 * Teilt alle Meldungen in Gruppen auf und erstellt pro Gruppe einen eigenen Turnierbaum-Sheet.
	 * Die Aufteilung berücksichtigt Szenario 1/2 gemäß {@link GruppenAufteilungRechner}.
	 */
	private void erstelleAlleGruppenBaeume(TeamMeldungen alleMeldungen, int gruppenGroesse)
			throws GenerateException {
		int minRestGroesse = getKonfigurationSheet().getMinRestGroesse();
		List<Integer> gruppenGroessen = GruppenAufteilungRechner.berechne(
				alleMeldungen.size(), gruppenGroesse, minRestGroesse);
		int anzGruppen = gruppenGroessen.size();

		alleGruppenSheetNamenLoeschen();

		var alleTeams = alleMeldungen.teams();
		int startIndex = 0;

		for (int g = 0; g < anzGruppen; g++) {
			int groesse = gruppenGroessen.get(g);
			var gruppenMeldungen = new TeamMeldungen();
			for (int i = startIndex; i < startIndex + groesse; i++) {
				gruppenMeldungen.addTeamWennNichtVorhanden(alleTeams.get(i));
			}
			startIndex += groesse;

			int bracketGroesse = Integer.highestOneBit(gruppenMeldungen.size());
			int numRunden = Integer.numberOfTrailingZeros(bracketGroesse);
			String sheetName = sheetNameFuerGruppe(g, anzGruppen);

			// aktuellerGruppenSheetName setzen BEVOR NewSheet.create() aufgerufen wird,
			// damit PageStyleHelper.applytoSheet() via getXSpreadSheet() das richtige Sheet trifft
			this.aktuellerGruppenSheetName = sheetName;
			try {
				NewSheet.from(this, sheetName)
						.pos((short) (DefaultSheetPos.KO_TURNIERBAUM + g))
						.hideGrid()
						.tabColor(SHEET_COLOR)
						.setActiv()
						.create();

				XSpreadsheet xSheet = getSheetHelper().findByName(sheetName);
				TurnierSheet.from(xSheet, getWorkingSpreadsheet()).setActiv();
				erstelleTurnierbaum(xSheet, gruppenMeldungen, numRunden, bracketGroesse);
			} finally {
				this.aktuellerGruppenSheetName = null;
			}
		}
	}

	/**
	 * Blattname für eine Gruppe: bei einer Gruppe ohne Buchstabe, sonst mit A, B, C …
	 */
	static String sheetNameFuerGruppe(int gruppenIndex, int anzGruppen) {
		if (anzGruppen == 1) {
			return SHEETNAME_PREFIX;
		}
		return SHEETNAME_PREFIX + " " + (char) ('A' + gruppenIndex);
	}

	/**
	 * Löscht alle vorhandenen Turnierbaum-Sheets (Namen beginnen mit {@link #SHEETNAME_PREFIX}).
	 */
	private void alleGruppenSheetNamenLoeschen() throws GenerateException {
		for (String name : getSheetHelper().getSheets().getElementNames()) {
			if (name.startsWith(SHEETNAME_PREFIX)) {
				getSheetHelper().removeSheet(name);
			}
		}
	}

	private void erstelleTurnierbaum(XSpreadsheet xSheet, TeamMeldungen meldungen, int numRunden,
			int bracketGroesse) throws GenerateException {

		sheet().processBoxinfo("K.-O. Turnierbaum erstellen");

		// Konfiguration für diese Erstellung lesen
		KoKonfigurationSheet konfig = getKonfigurationSheet();
		this.spielbahn = konfig.getSpielbaumSpielbahn();
		this.teamAnzeige = konfig.getSpielbaumTeamAnzeige();
		this.spielUmPlatz3 = konfig.isSpielbaumSpielUmPlatz3();
		this.headerFarbe      = konfig.getTurnierbaumHeaderFarbe();
		this.teamAFarbe       = konfig.getTurnierbaumTeamAFarbe();
		this.teamBFarbe       = konfig.getTurnierbaumTeamBFarbe();
		this.scoreFarbe       = konfig.getTurnierbaumScoreFarbe();
		this.siegerFarbe      = konfig.getTurnierbaumSiegerFarbe();
		this.bahnFarbe        = konfig.getTurnierbaumBahnFarbe();
		this.drittePlatzFarbe = konfig.getTurnierbaumDrittePlatzFarbe();

		// Spalten-Offsets je nach Bahn-Einstellung:
		// Mit Bahn:    Bahn(0) | Team(1) | Score(2) | Connector(3)  → colGroupSize = 4
		// Ohne Bahn:   Team(0) | Score(1) | Connector(2)             → colGroupSize = 3
		if (mitBahn()) {
			this.teamOffset = 1;
			this.scoreOffset = 2;
			this.connectorOffset = 3;
			this.colGroupSize = 4;
		} else {
			this.teamOffset = 0;
			this.scoreOffset = 1;
			this.connectorOffset = 2;
			this.colGroupSize = 3;
		}

		// Cadrage-State initialisieren
		this.gesanzTeamsIntern = meldungen.size();
		if (gesanzTeamsIntern > bracketGroesse) {
			var rechner = new CadrageRechner(gesanzTeamsIntern);
			this.mitCadrage = true;
			this.anzCadrageMatches = rechner.anzTeams() / 2;
			this.anzFreilose = rechner.anzFreilose();
			this.cadrageSpaltOffset = colGroupSize;
		} else {
			this.mitCadrage = false;
			this.anzCadrageMatches = 0;
			this.anzFreilose = bracketGroesse;
			this.cadrageSpaltOffset = 0;
		}

		int[] setzliste = berechneSetzliste(bracketGroesse);

		// Spaltenbreiten setzen
		formatiereKolumnen(xSheet, numRunden);

		// Header (2 Zeilen: Titel + Spaltenbeschriftung)
		schreibeHeader(xSheet, numRunden);

		// Runde 1: direkte Einträge aus Setzliste; Cadrage-Slots via Vorrundenformel
		int anzMatchesR1 = bracketGroesse / 2;
		int[] bahnR1 = berechneBahnNummern(anzMatchesR1);
		for (int m = 0; m < anzMatchesR1; m++) {
			int seedA = setzliste[2 * m];
			int seedB = setzliste[2 * m + 1];

			int rowA = teamAZeile(1, m);
			int rowB = teamBZeile(1, m);

			if (mitBahn()) {
				schreibeBahnZelle(xSheet, 1, rowA, rowB, bahnR1[m]);
			}

			if (mitCadrage && seedA > anzFreilose) {
				schreibeCadrageMatch(xSheet, meldungen, seedA, rowA, rowB);
				schreibeCadrageGewinnerFormel(xSheet, rowA, rowB, true);
			} else {
				schreibeTeamZelleR1(xSheet, rowA, getTeamNrBySeedPosition(meldungen, seedA), true);
			}

			if (mitCadrage && seedB > anzFreilose) {
				schreibeCadrageMatch(xSheet, meldungen, seedB, rowA, rowB);
				schreibeCadrageGewinnerFormel(xSheet, rowA, rowB, false);
			} else {
				schreibeTeamZelleR1(xSheet, rowB, getTeamNrBySeedPosition(meldungen, seedB), false);
			}

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
					schreibeBahnZelle(xSheet, r, rowA, rowB, bahnRunde[m]);
				}
				schreibeGewinnerFormel(xSheet, r, m, true);
				schreibeGewinnerFormel(xSheet, r, m, false);
				schreibeScoreZelle(xSheet, r, rowA, true);
				schreibeScoreZelle(xSheet, r, rowB, false);
				zeichneMatchConnector(xSheet, r, rowA, rowB);
			}
		}

		// Sieger-Spalte
		schreibeSieger(xSheet, numRunden);

		// Spiel um Platz 3/4 (nur wenn Option aktiv und mindestens Halbfinale vorhanden)
		if (spielUmPlatz3 && numRunden >= 2) {
			schreibePlatz3Match(xSheet, numRunden, bracketGroesse);
		}
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
		int teamColWidth = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) ? NAME_COL_WIDTH : NR_COL_WIDTH;

		if (mitCadrage) {
			if (mitBahn()) {
				getSheetHelper().setColumnProperties(xSheet, cadrageBahnSpalte(),
						ColumnProperties.from().setWidth(BAHN_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
								.setVertJustify(CellVertJustify2.CENTER));
			}
			getSheetHelper().setColumnProperties(xSheet, cadrageTeamSpalte(),
					ColumnProperties.from().setWidth(teamColWidth)
							.setHoriJustify(teamAnzeige == KoSpielbaumTeamAnzeige.NAME
									? CellHoriJustify.LEFT : CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
			getSheetHelper().setColumnProperties(xSheet, cadrageScoreSpalte(),
					ColumnProperties.from().setWidth(SCORE_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
			getSheetHelper().setColumnProperties(xSheet, cadrageConnectorSpalte(),
					ColumnProperties.from().setWidth(CONNECTOR_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
		}

		for (int r = 1; r <= numRunden; r++) {
			if (mitBahn()) {
				getSheetHelper().setColumnProperties(xSheet, bahnSpalte(r),
						ColumnProperties.from().setWidth(BAHN_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
								.setVertJustify(CellVertJustify2.CENTER));
			}
			getSheetHelper().setColumnProperties(xSheet, teamSpalte(r),
					ColumnProperties.from().setWidth(teamColWidth)
							.setHoriJustify(teamAnzeige == KoSpielbaumTeamAnzeige.NAME
									? CellHoriJustify.LEFT : CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
			getSheetHelper().setColumnProperties(xSheet, scoreSpalte(r),
					ColumnProperties.from().setWidth(SCORE_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
			getSheetHelper().setColumnProperties(xSheet, connectorSpalte(r),
					ColumnProperties.from().setWidth(CONNECTOR_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
		}

		// Sieger-Spalten:
		// NR-Modus:   siegerSpalte = Nr (schmal), siegerNameSpalte = Name via SVERWEIS (breit)
		// NAME-Modus: siegerSpalte = Name (breit), siegerNameSpalte = versteckt
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NR) {
			getSheetHelper().setColumnProperties(xSheet, siegerSpalte(numRunden),
					ColumnProperties.from().setWidth(NR_COL_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
							.setVertJustify(CellVertJustify2.CENTER));
			getSheetHelper().setColumnProperties(xSheet, siegerNameSpalte(numRunden),
					ColumnProperties.from().setWidth(SIEGER_NAME_COL_WIDTH).setHoriJustify(CellHoriJustify.LEFT)
							.setVertJustify(CellVertJustify2.CENTER));
		} else {
			getSheetHelper().setColumnProperties(xSheet, siegerSpalte(numRunden),
					ColumnProperties.from().setWidth(SIEGER_NAME_COL_WIDTH).setHoriJustify(CellHoriJustify.LEFT)
							.setVertJustify(CellVertJustify2.CENTER));
			getSheetHelper().setColumnProperties(xSheet, siegerNameSpalte(numRunden),
					ColumnProperties.from().setWidth(0));
		}
	}

	private void schreibeHeader(XSpreadsheet xSheet, int numRunden) throws GenerateException {
		String teamHeader = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) ? "Teamname" : "Nr";

		if (mitCadrage) {
			int titelStartSpalte = mitBahn() ? cadrageBahnSpalte() : cadrageTeamSpalte();
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(titelStartSpalte, HEADER_ZEILE_TITEL), "Cadrage")
							.setEndPosMergeSpaltePlus(colGroupSize - 1)
							.setCharWeight(FontWeight.BOLD)
							.setHoriJustify(CellHoriJustify.CENTER)
							.setCellBackColor(headerFarbe)
							.setCharColor("FFFFFF")
							.setBorder(BorderFactory.from().allThin().toBorder()));
			if (mitBahn()) {
				schreibeSpaltenHeader(xSheet, cadrageBahnSpalte(), "Bahn");
			}
			schreibeSpaltenHeader(xSheet, cadrageTeamSpalte(), teamHeader);
			schreibeSpaltenHeader(xSheet, cadrageScoreSpalte(), "Pkt");
		}

		for (int r = 1; r <= numRunden; r++) {
			String rundentitel = berechnRundenTitel(r, numRunden);

			// Zeile 0: Rundentitel über alle Spalten der Runde (merged)
			int titelStartSpalte = mitBahn() ? bahnSpalte(r) : teamSpalte(r);
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(titelStartSpalte, HEADER_ZEILE_TITEL), rundentitel)
							.setEndPosMergeSpaltePlus(colGroupSize - 1)
							.setCharWeight(FontWeight.BOLD)
							.setHoriJustify(CellHoriJustify.CENTER)
							.setCellBackColor(headerFarbe)
							.setCharColor("FFFFFF")
							.setBorder(BorderFactory.from().allThin().toBorder()));

			// Zeile 1: Spalten-Überschriften
			if (mitBahn()) {
				schreibeSpaltenHeader(xSheet, bahnSpalte(r), "Bahn");
			}
			schreibeSpaltenHeader(xSheet, teamSpalte(r), teamHeader);
			schreibeSpaltenHeader(xSheet, scoreSpalte(r), "Pkt");
		}

		// Sieger-Header (merged über siegerSpalte + siegerNameSpalte)
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(siegerSpalte(numRunden), HEADER_ZEILE_TITEL), "Sieger")
						.setEndPosMergeSpaltePlus(1)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCellBackColor(siegerFarbe)
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
						.setCellBackColor(headerFarbe)
						.setCharColor("FFFFFF")
						.setBorder(BorderFactory.from().allThin().toBorder()));
	}

	// ---------------------------------------------------------------
	// Zellinhalte
	// ---------------------------------------------------------------

	/**
	 * Schreibt die Team-Zelle für Runde 1 (direkte Einträge aus der Setzliste).<br>
	 * NR-Modus: schreibt die Teamnummer direkt.<br>
	 * NAME-Modus: schreibt eine SVERWEIS-Formel, die den Teamnamen aus der Meldeliste liest.
	 */
	private void schreibeTeamZelleR1(XSpreadsheet xSheet, int zeile, int nr, boolean istTeamA)
			throws GenerateException {
		int farbe = istTeamA ? teamAFarbe : teamBFarbe;
		if (nr <= 0) {
			// Freilos – immer als Text
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(teamSpalte(1), zeile), "Freilos")
							.setCellBackColor(farbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
			return;
		}
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) {
			// Teamname via SVERWEIS
			String formel = "SVERWEIS(" + nr + ";" + MeldeListeKonstanten.SHEETNAME + ".$A:$B;2;0)";
			getSheetHelper().setFormulaInCell(
					StringCellValue.from(xSheet, Position.from(teamSpalte(1), zeile), formel)
							.setCellBackColor(farbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.LEFT));
		} else {
			// Teamnummer direkt
			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(xSheet, Position.from(teamSpalte(1), zeile))
							.setValue(nr)
							.setCellBackColor(farbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
		}
	}

	/**
	 * Schreibt eine Bahnnummer als vertikal zusammengeführte Zelle für eine Paarung (rowA..rowB).<br>
	 * bahnNr=0 → leere editierbare Zelle (Modus L); bahnNr&gt;0 → Nummer zentriert.
	 */
	private void schreibeBahnZelle(XSpreadsheet xSheet, int runde, int rowA, int rowB, int bahnNr)
			throws GenerateException {
		schreibeBahnZelleAnSpalte(xSheet, bahnSpalte(runde), rowA, rowB, bahnNr);
	}

	private void schreibeBahnZelleAnSpalte(XSpreadsheet xSheet, int spalte, int rowA, int rowB, int bahnNr)
			throws GenerateException {
		if (bahnNr > 0) {
			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(xSheet, Position.from(spalte, rowA))
							.setValue(bahnNr)
							.setEndPosMergeZeile(rowB)
							.setCellBackColor(bahnFarbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER)
							.centerVertJustify());
		} else {
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(spalte, rowA), "")
							.setEndPosMergeZeile(rowB)
							.setCellBackColor(bahnFarbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER)
							.centerVertJustify());
		}
	}

	private void schreibeScoreZelle(XSpreadsheet xSheet, int runde, int zeile, boolean istTeamA)
			throws GenerateException {
		// Score-Zelle ist editierbar (leer, Benutzer trägt Ergebnis ein)
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(scoreSpalte(runde), zeile), "")
						.setCellBackColor(scoreFarbe)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(CellHoriJustify.CENTER));
	}

	/**
	 * Schreibt die WENN-Gewinner-Formel für Runden 2+.<br>
	 * Gewinner = Team mit höherer Punktzahl. Bei Gleichstand: "?".<br>
	 * Im NR-Modus wird die Teamnummer propagiert, im NAME-Modus der Teamname.
	 */
	private void schreibeGewinnerFormel(XSpreadsheet xSheet, int runde, int match, boolean istTeamA)
			throws GenerateException {
		int feederMatch = istTeamA ? (2 * match) : (2 * match + 1);
		int feederRunde = runde - 1;

		int rowFeederA = teamAZeile(feederRunde, feederMatch);
		int rowFeederB = teamBZeile(feederRunde, feederMatch);

		String scoreAAddr = Position.from(scoreSpalte(feederRunde), rowFeederA).getAddressWith$();
		String scoreBAddr = Position.from(scoreSpalte(feederRunde), rowFeederB).getAddressWith$();
		String teamAAddr = Position.from(teamSpalte(feederRunde), rowFeederA).getAddressWith$();
		String teamBAddr = Position.from(teamSpalte(feederRunde), rowFeederB).getAddressWith$();

		// ISTZAHL: Score muss eine Zahl sein (nicht leer) damit Gewinner berechnet wird
		String formel = "WENN(ISTZAHL(" + scoreAAddr + ")*ISTZAHL(" + scoreBAddr + ");"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + teamAAddr + ";"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + teamBAddr + ";\"?\"));"
				+ "\"\")";

		int targetRow = istTeamA ? teamAZeile(runde, match) : teamBZeile(runde, match);
		CellHoriJustify justify = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME)
				? CellHoriJustify.LEFT : CellHoriJustify.CENTER;

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(teamSpalte(runde), targetRow), formel)
						.setCellBackColor(istTeamA ? teamAFarbe : teamBFarbe)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(justify));
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
	 * Zeigt den Turniersieger nach dem Finale an.<br>
	 * NR-Modus: siegerSpalte = Nr, siegerNameSpalte = Teamname via SVERWEIS.<br>
	 * NAME-Modus: siegerSpalte = Teamname (direkt propagiert), siegerNameSpalte = leer.
	 */
	private void schreibeSieger(XSpreadsheet xSheet, int numRunden) throws GenerateException {
		int finaleMatch = 0;
		int rowFinaleA = teamAZeile(numRunden, finaleMatch);
		int rowFinaleB = teamBZeile(numRunden, finaleMatch);
		int siegerZeile = (rowFinaleA + rowFinaleB) / 2;

		int siegerSp = siegerSpalte(numRunden);

		String scoreAAddr = Position.from(scoreSpalte(numRunden), rowFinaleA).getAddressWith$();
		String scoreBAddr = Position.from(scoreSpalte(numRunden), rowFinaleB).getAddressWith$();
		String teamAAddr = Position.from(teamSpalte(numRunden), rowFinaleA).getAddressWith$();
		String teamBAddr = Position.from(teamSpalte(numRunden), rowFinaleB).getAddressWith$();

		String siegerFormel = "WENN(ISTZAHL(" + scoreAAddr + ")*ISTZAHL(" + scoreBAddr + ");"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + teamAAddr + ";"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + teamBAddr + ";\"?\"));"
				+ "\"\")";

		CellHoriJustify justify = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME)
				? CellHoriJustify.LEFT : CellHoriJustify.CENTER;

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(siegerSp, siegerZeile), siegerFormel)
						.setCellBackColor(siegerFarbe)
						.setBorder(BorderFactory.from().allBold().toBorder())
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(justify));

		// Im NR-Modus: zusätzlich Teamname via SVERWEIS in der Nebenspalte
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NR) {
			String siegerNrAddr = Position.from(siegerSp, siegerZeile).getAddressWith$();
			String siegerNameFormel = "WENN(ISTZAHL(" + siegerNrAddr + ")*(" + siegerNrAddr
					+ ">0);SVERWEIS(" + siegerNrAddr + ";" + MeldeListeKonstanten.SHEETNAME + ".$A:$B;2;0);\"\")";

			getSheetHelper().setFormulaInCell(
					StringCellValue.from(xSheet, Position.from(siegerNameSpalte(numRunden), siegerZeile),
							siegerNameFormel)
							.setCellBackColor(siegerFarbe)
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

	// ---------------------------------------------------------------
	// Cadrage
	// ---------------------------------------------------------------

	/**
	 * Schreibt ein Cadrage-Match in die Cadrage-Spalte.<br>
	 * Der Bracket-Slot {@code slotSeed} (z.B. 7 oder 8) spielt gegen das gespiegelte Seed
	 * {@code gesanzTeamsIntern - cadrageIdx}. Beide Teams werden direkt in die Cadrage-Spalte
	 * an denselben Zeilen wie das zugehörige Runde-1-Match eingetragen.
	 *
	 * @param slotSeed Bracket-Slot-Seed &gt; {@link #anzFreilose} (z.B. 7 oder 8)
	 * @param rowA     Zeile Team A des zugehörigen Runde-1-Matches
	 * @param rowB     Zeile Team B des zugehörigen Runde-1-Matches
	 */
	private void schreibeCadrageMatch(XSpreadsheet xSheet, TeamMeldungen meldungen, int slotSeed,
			int rowA, int rowB) throws GenerateException {
		int cadrageIdx = slotSeed - anzFreilose - 1;
		int opponentSeed = gesanzTeamsIntern - cadrageIdx;

		int nrA = getTeamNrBySeedPosition(meldungen, slotSeed);
		int nrB = getTeamNrBySeedPosition(meldungen, opponentSeed);

		if (mitBahn()) {
			// Eine zusammengeführte Bahn-Zelle für die Cadrage-Paarung
			schreibeBahnZelleAnSpalte(xSheet, cadrageBahnSpalte(), rowA, rowB, 0);
		}

		schreibeTeamZelleInSpalte(xSheet, cadrageTeamSpalte(), rowA, nrA, true);
		schreibeTeamZelleInSpalte(xSheet, cadrageTeamSpalte(), rowB, nrB, false);

		// Score-Zellen (editierbar)
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(cadrageScoreSpalte(), rowA), "")
						.setCellBackColor(scoreFarbe)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(CellHoriJustify.CENTER));
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(cadrageScoreSpalte(), rowB), "")
						.setCellBackColor(scoreFarbe)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(CellHoriJustify.CENTER));

		// Konnektor
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(cadrageConnectorSpalte(), rowA), CHAR_TOP)
						.setHoriJustify(CellHoriJustify.RIGHT));
		for (int z = rowA + 1; z < rowB; z++) {
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(cadrageConnectorSpalte(), z), CHAR_MITTE)
							.setHoriJustify(CellHoriJustify.RIGHT));
		}
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(cadrageConnectorSpalte(), rowB), CHAR_BOTTOM)
						.setHoriJustify(CellHoriJustify.RIGHT));
	}

	/**
	 * Schreibt eine Team-Zelle in eine explizit angegebene Spalte.<br>
	 * NR-Modus: Teamnummer direkt; NAME-Modus: SVERWEIS-Formel.
	 */
	private void schreibeTeamZelleInSpalte(XSpreadsheet xSheet, int spalte, int zeile, int nr,
			boolean istTeamA) throws GenerateException {
		int farbe = istTeamA ? teamAFarbe : teamBFarbe;
		if (nr <= 0) {
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(spalte, zeile), "Freilos")
							.setCellBackColor(farbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
			return;
		}
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) {
			String formel = "SVERWEIS(" + nr + ";" + MeldeListeKonstanten.SHEETNAME + ".$A:$B;2;0)";
			getSheetHelper().setFormulaInCell(
					StringCellValue.from(xSheet, Position.from(spalte, zeile), formel)
							.setCellBackColor(farbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.LEFT));
		} else {
			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(xSheet, Position.from(spalte, zeile))
							.setValue(nr)
							.setCellBackColor(farbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
		}
	}

	/**
	 * Schreibt die Gewinner-Formel für einen Cadrage-Slot in die Runde-1-Teamzelle.<br>
	 * Referenziert die Score-Zellen der Cadrage-Spalte.
	 *
	 * @param rowA      Zeile des Runde-1 Team-A (= Cadrage Match Team-A-Zeile)
	 * @param rowB      Zeile des Runde-1 Team-B (= Cadrage Match Team-B-Zeile)
	 * @param istSlotA  true wenn der Cadrage-Slot in der Team-A-Position von Runde 1 sitzt
	 */
	private void schreibeCadrageGewinnerFormel(XSpreadsheet xSheet, int rowA, int rowB,
			boolean istSlotA) throws GenerateException {
		String scoreAAddr = Position.from(cadrageScoreSpalte(), rowA).getAddressWith$();
		String scoreBAddr = Position.from(cadrageScoreSpalte(), rowB).getAddressWith$();
		String teamAAddr = Position.from(cadrageTeamSpalte(), rowA).getAddressWith$();
		String teamBAddr = Position.from(cadrageTeamSpalte(), rowB).getAddressWith$();

		String formel = "WENN(ISTZAHL(" + scoreAAddr + ")*ISTZAHL(" + scoreBAddr + ");"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + teamAAddr + ";"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + teamBAddr + ";\"?\"));"
				+ "\"\")";

		int targetRow = istSlotA ? rowA : rowB;
		boolean istTeamA = istSlotA;
		int farbe = istTeamA ? teamAFarbe : teamBFarbe;
		CellHoriJustify justify = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME)
				? CellHoriJustify.LEFT : CellHoriJustify.CENTER;

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(teamSpalte(1), targetRow), formel)
						.setCellBackColor(farbe)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(justify));
	}

	// ---------------------------------------------------------------
	// Spiel um Platz 3/4
	// ---------------------------------------------------------------

	/**
	 * Erstellt den Bereich "Spiel um Platz 3/4" unterhalb des Hauptbaums.<br>
	 * Die beiden Verlierer des Halbfinales (Runde {@code numRunden-1}) treten gegeneinander an.
	 */
	private void schreibePlatz3Match(XSpreadsheet xSheet, int numRunden, int bracketGroesse)
			throws GenerateException {
		int halbfinaleRunde = numRunden - 1;

		// Letzte verwendete Zeile im Hauptbaum
		int anzMatchesR1 = bracketGroesse / 2;
		int letzteZeile = teamBZeile(1, anzMatchesR1 - 1);
		int platz3HeaderZeile = letzteZeile + 3;
		int platz3TeamAZeile = platz3HeaderZeile + 1;
		int platz3TeamBZeile = platz3HeaderZeile + 2;

		// Bereichs-Header "Spiel um Platz 3/4" in den Finale-Spalten
		int headerStartSpalte = mitBahn() ? bahnSpalte(numRunden) : teamSpalte(numRunden);
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(headerStartSpalte, platz3HeaderZeile), "Spiel um Platz 3/4")
						.setEndPosMergeSpaltePlus(colGroupSize - 1)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCellBackColor(headerFarbe)
						.setCharColor("FFFFFF")
						.setBorder(BorderFactory.from().allThin().toBorder()));

		// "3. Platz" Kopf-Label in der Sieger-Spalte
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(siegerSpalte(numRunden), platz3HeaderZeile), "3. Platz")
						.setEndPosMergeSpaltePlus(1)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCellBackColor(drittePlatzFarbe)
						.setBorder(BorderFactory.from().allThin().toBorder()));

		// Bahn-Zelle (falls Spielbahn aktiv) – eine zusammengeführte Zelle pro Paarung
		if (mitBahn()) {
			schreibeBahnZelle(xSheet, numRunden, platz3TeamAZeile, platz3TeamBZeile, 0);
		}

		// Verlierer-Formeln: Verlierer Halbfinale-Match 0 → TeamA, Match 1 → TeamB
		schreibeVerliererFormel(xSheet, numRunden, halbfinaleRunde, 0, platz3TeamAZeile, true);
		schreibeVerliererFormel(xSheet, numRunden, halbfinaleRunde, 1, platz3TeamBZeile, false);

		// Score-Zellen
		schreibeScoreZelle(xSheet, numRunden, platz3TeamAZeile, true);
		schreibeScoreZelle(xSheet, numRunden, platz3TeamBZeile, false);

		// Connector
		zeichneMatchConnector(xSheet, numRunden, platz3TeamAZeile, platz3TeamBZeile);

		// Gewinner des Spiels um Platz 3
		schreibeDrittePlatzSieger(xSheet, numRunden, platz3TeamAZeile, platz3TeamBZeile);
	}

	/**
	 * Schreibt die Verlierer-Formel für den Einzug ins Spiel um Platz 3/4.<br>
	 * Verlierer = Team mit der niedrigeren Punktzahl im Quell-Match.
	 */
	private void schreibeVerliererFormel(XSpreadsheet xSheet, int runde, int feederRunde, int feederMatch,
			int targetRow, boolean istTeamA) throws GenerateException {
		int rowFeederA = teamAZeile(feederRunde, feederMatch);
		int rowFeederB = teamBZeile(feederRunde, feederMatch);

		String scoreAAddr = Position.from(scoreSpalte(feederRunde), rowFeederA).getAddressWith$();
		String scoreBAddr = Position.from(scoreSpalte(feederRunde), rowFeederB).getAddressWith$();
		String teamAAddr = Position.from(teamSpalte(feederRunde), rowFeederA).getAddressWith$();
		String teamBAddr = Position.from(teamSpalte(feederRunde), rowFeederB).getAddressWith$();

		// Verlierer = Team mit niedrigerer Punktzahl (umgekehrt zur Gewinner-Formel)
		String formel = "WENN(ISTZAHL(" + scoreAAddr + ")*ISTZAHL(" + scoreBAddr + ");"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + teamAAddr + ";"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + teamBAddr + ";\"?\"));"
				+ "\"\")";

		CellHoriJustify justify = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME)
				? CellHoriJustify.LEFT : CellHoriJustify.CENTER;

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(teamSpalte(runde), targetRow), formel)
						.setCellBackColor(istTeamA ? teamAFarbe : teamBFarbe)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(justify));
	}

	/**
	 * Zeigt den Gewinner des Spiels um Platz 3 an (analog zu {@link #schreibeSieger}).
	 */
	private void schreibeDrittePlatzSieger(XSpreadsheet xSheet, int numRunden, int teamAZeile,
			int teamBZeile) throws GenerateException {
		int siegerZeile = (teamAZeile + teamBZeile) / 2;
		int siegerSp = siegerSpalte(numRunden);

		String scoreAAddr = Position.from(scoreSpalte(numRunden), teamAZeile).getAddressWith$();
		String scoreBAddr = Position.from(scoreSpalte(numRunden), teamBZeile).getAddressWith$();
		String teamAAddr = Position.from(teamSpalte(numRunden), teamAZeile).getAddressWith$();
		String teamBAddr = Position.from(teamSpalte(numRunden), teamBZeile).getAddressWith$();

		String drittePlatzFormel = "WENN(ISTZAHL(" + scoreAAddr + ")*ISTZAHL(" + scoreBAddr + ");"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + teamAAddr + ";"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + teamBAddr + ";\"?\"));"
				+ "\"\")";

		CellHoriJustify justify = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME)
				? CellHoriJustify.LEFT : CellHoriJustify.CENTER;

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(siegerSp, siegerZeile), drittePlatzFormel)
						.setCellBackColor(drittePlatzFarbe)
						.setBorder(BorderFactory.from().allBold().toBorder())
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(justify));

		// Im NR-Modus: Teamname via SVERWEIS in der Nebenspalte
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NR) {
			String drittePlatzNrAddr = Position.from(siegerSp, siegerZeile).getAddressWith$();
			String drittePlatzNameFormel = "WENN(ISTZAHL(" + drittePlatzNrAddr + ")*(" + drittePlatzNrAddr
					+ ">0);SVERWEIS(" + drittePlatzNrAddr + ";" + MeldeListeKonstanten.SHEETNAME
					+ ".$A:$B;2;0);\"\")";

			getSheetHelper().setFormulaInCell(
					StringCellValue.from(xSheet, Position.from(siegerNameSpalte(numRunden), siegerZeile),
							drittePlatzNameFormel)
							.setCellBackColor(drittePlatzFarbe)
							.setBorder(BorderFactory.from().allBold().toBorder())
							.setCharWeight(FontWeight.BOLD)
							.setHoriJustify(CellHoriJustify.LEFT));
		}
	}

}
