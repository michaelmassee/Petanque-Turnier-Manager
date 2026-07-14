/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.common.CadrageRechner;
import de.petanqueturniermanager.algorithmen.common.GruppenAufteilungRechner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import com.sun.star.sheet.ConditionOperator;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.random.RandomSource;

import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.ko.konfiguration.IKoBracketKonfiguration;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoSpielbaumTeamAnzeige;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetUpdate;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

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

	/**
	 * Header-Zeilen.
	 * <p>
	 * Bei mehreren Gruppen wird zusätzlich Zeile 0 als „Gruppe X"-Banner verwendet,
	 * dann sind alle nachfolgenden Header- und Datenzeilen um {@link #gruppenZeileOffset}
	 * nach unten verschoben. Reads erfolgen daher immer über die Hilfsmethoden
	 * {@link #headerZeileTitel()}, {@link #headerZeileSpalten()}, {@link #ersteZeile()}
	 * und {@link #scoreDataZeile()}, niemals direkt über die Konstanten.
	 */
	private static final int HEADER_ZEILE_TITEL_BASE = 0;
	private static final int HEADER_ZEILE_SPALTEN_BASE = 1;
	private static final int ERSTE_ZEILE_BASE = 2;

	/** Offset für alle Zeilen, wenn ein „Gruppe X"-Banner über dem Bracket steht (sonst 0). */
	private volatile int gruppenZeileOffset = 0;

	/** Optionaler „Gruppe X"-Label-Buchstabe (z.B. „A"); {@code null} = kein Banner. */
	private volatile String gruppenHeaderLabel = null;

	private int headerZeileTitel()    { return HEADER_ZEILE_TITEL_BASE   + gruppenZeileOffset; }
	private int headerZeileSpalten()  { return HEADER_ZEILE_SPALTEN_BASE + gruppenZeileOffset; }
	private int ersteZeile()          { return ERSTE_ZEILE_BASE          + gruppenZeileOffset; }
	private int scoreDataZeile()      { return headerZeileSpalten(); }

	/** Präfix in der Score-Daten-Arbeitszelle – macht den Inhalt beim Debuggen erkennbar. */
	static final String SCORE_DATA_PREFIX = "PTM_EDIT:";

	private static final int NR_COL_WIDTH = 700;
	private static final int BAHN_COL_WIDTH = NR_COL_WIDTH;
	private static final int NAME_COL_WIDTH = 3000;
	private static final int SCORE_COL_WIDTH = 900;
	private static final int CONNECTOR_COL_WIDTH = 400;
	private static final String BAHN_HEADER_KURZ = "Bn";

	/** Breite der Sieger-Name-Spalte (letzte sichtbare Spalte im Turnierbaum). */
	static final int SIEGER_NAME_COL_WIDTH = 5000;

	// Farben – aus Konfiguration gelesen (Standardwerte als Fallback)
	private volatile int headerFarbe       = 0x2544DD;
	private volatile int teamAFarbe        = 0xDCEEFA;
	private volatile int teamBFarbe        = 0xF0F7FF;
	private volatile int siegerFarbe       = 0xFFD700;
	private volatile int bahnFarbe         = 0xEEEEEE;
	private volatile int drittePlatzFarbe  = 0xCD7F32;

	/** Unicode-Zeichen für die Konnektorspalte */
	private static final String CHAR_TOP = "┐";
	private static final String CHAR_BOTTOM = "┘";
	private static final String CHAR_MITTE = "│";

	// Konfigurations-State für die aktuelle Turnierbaum-Erstellung
	private volatile SpielrundeSpielbahn spielbahn = SpielrundeSpielbahn.X;
	private volatile KoSpielbaumTeamAnzeige teamAnzeige = KoSpielbaumTeamAnzeige.NR;
	private volatile boolean spielUmPlatz3 = false;
	private volatile boolean bahnNurRunde1 = true;

	// Meldeliste-Struktur (für die VLOOKUP-Formeln auf Team-/Sieger-Zellen)
	private volatile boolean meldeListeTeamnameAnzeigen = true;
	private volatile boolean meldeListeVereinsnameAnzeigen = false;
	private volatile Formation meldeListeFormation = Formation.DOUBLETTE;

	// Aktuell in Erstellung befindlicher Gruppen-Sheet-Name (für getXSpreadSheet())
	private volatile String aktuellerGruppenSheetName = null;

	// Cadrage-State
	private volatile boolean mitCadrage = false;
	private volatile int cadrageSpaltOffset = 0; // = colGroupSize wenn Cadrage vorhanden, sonst 0
	private int[] cadrageBahnNummern = new int[0];
	private List<int[]> vorgegebeneBahnNummernProRunde = List.of();
	private int[] vorgegebeneCadrageBahnNummern = null;
	// Anzahl der bestgesetzten Teams, die keine Cadrage spielen und direkt ins Hauptfeld starten
	// (kein Freilos – sie spielen die Hauptrunde ganz normal, nur ohne Cadrage-Vorrunde).
	private volatile int anzOhneCadrage = 0;
	private volatile int gesanzTeamsIntern = 0;
	// Zeilenabstand zwischen zwei aufeinanderfolgenden Runde-1-Matches.
	private volatile int runde1MatchZeilenAbstand = 3;
	// Zeilenabstand zwischen Team A und Team B innerhalb eines Runde-1-Matches. Die Slots werden
	// nur gespreizt, wenn eine Cadrage-Partie am oberen Slot sonst in die untere Team-Zeile läuft.
	private volatile int runde1SlotAbstand = 1;

	// Spalten-Offsets (dynamisch je nach spielbahn)
	// Mit Bahn:    Bahn(0) | Team(1) | Score(2) | Connector(3)  → colGroupSize = 4
	// Ohne Bahn:   Team(0) | Score(1) | Connector(2)             → colGroupSize = 3
	private volatile int teamOffset = 0;
	private volatile int scoreOffset = 1;
	private volatile int connectorOffset = 2;
	private volatile int colGroupSize = 3;

	/** Sammelt Score-Zell-Positionen während einer Bracket-Erstellung für den Blattschutz.
	 *  Nur die Referenz ist {@code volatile} (Sichtbarkeit von Neuanlage/Nullen je Lauf);
	 *  die Liste selbst wird ausschließlich vom laufenden Worker-Thread befüllt. */
	private volatile List<Position> aktuelleScorePositionen = null;

	/** Bracketgröße der zuletzt erzeugten Gruppe – wird von Testhilfen wie
	 *  {@link #schreibeRunde1TestErgebnisse(String)} ausgelesen. */
	private volatile int aktuelleBracketGroesse = 0;

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
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		// Metadaten-Suche: zuerst Einzelgruppe, dann Gruppe A
		var found = SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.schluesselKoTurnierbaum(""));
		if (found.isPresent()) return found.get();
		found = SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.schluesselKoTurnierbaum("A"));
		if (found.isPresent()) return found.get();
		// Fallback per Name: zuerst lokalisierter Name, dann Legacy (alte deutsche Dokumente)
		var praefix = SheetNamen.koTurnierbaumEinzel();
		XSpreadsheet sheet = getSheetHelper().findByName(praefix);
		if (sheet != null) {
			return sheet;
		}
		sheet = getSheetHelper().findByName(praefix + " A");
		if (sheet != null) {
			return sheet;
		}
		sheet = getSheetHelper().findByName(SheetNamen.LEGACY_KO_TURNIERBAUM_EINZEL);
		if (sheet != null) {
			return sheet;
		}
		return getSheetHelper().findByName(SheetNamen.LEGACY_KO_TURNIERBAUM_EINZEL + " A");
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
			return match * runde1MatchZeilenAbstand + ersteZeile();
		}
		return teamBZeile(runde - 1, 2 * match);
	}

	/**
	 * Zeile für Team B eines Matches.
	 */
	int teamBZeile(int runde, int match) {
		if (runde == 1) {
			return match * runde1MatchZeilenAbstand + ersteZeile() + runde1SlotAbstand;
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
	 * Berechnet die Bracket-Größe (Teilnehmer in Runde 1) für eine Gruppe mit
	 * {@code teamCount} Teams.
	 *
	 * Liefert die Größe des Hauptfelds nach einer ggf. nötigen Cadrage. Nicht-Zweierpotenzen
	 * werden auf die nächstkleinere Zweierpotenz reduziert; die überzähligen Teams spielen
	 * vorher Cadrage.
	 */
	static int berechneBracketGroesse(int teamCount) {
		return Integer.highestOneBit(teamCount);
	}

	/**
	 * Zeilenabstand zwischen Team A und Team B innerhalb eines Runde-1-Matches.
	 *
	 * <p>Ohne überlappende Cadrage stehen die beiden Teams direkt untereinander (Abstand 1). Eine
	 * Spreizung ist nur nötig, wenn der obere Runde-1-Slot selbst aus einer zweizeiligen
	 * Cadrage-Partie kommt; sonst würde diese Partie die untere Team-Zeile des Matches belegen.
	 */
	static int berechneRunde1SlotAbstand(int teamCount, int bracketGroesse) {
		return brauchtGespreizteCadrageSlots(teamCount, bracketGroesse) ? 3 : 1;
	}

	/**
	 * Zeilenabstand zwischen zwei aufeinanderfolgenden Runde-1-Matches.
	 *
	 * <p>Muss so groß sein, dass die (ggf. gespreizten) Slots samt Cadrage-Feeder eines Matches
	 * nicht in das nächste Match hineinragen. Kompakt: 3 (Slot A, Slot B, Leerzeile). Gespreizt:
	 * 6 (Slot A + Feeder, Leerzeile, Slot B + Feeder, Leerzeile).
	 */
	static int berechneRunde1MatchZeilenAbstand(int teamCount, int bracketGroesse) {
		return brauchtGespreizteCadrageSlots(teamCount, bracketGroesse) ? 6 : 3;
	}

	static boolean brauchtGespreizteCadrageSlots(int teamCount, int bracketGroesse) {
		if (teamCount <= bracketGroesse) {
			return false;
		}
		int anzOhneCadrage = new CadrageRechner(teamCount).anzOhneCadrage();
		int[] setzliste = berechneSetzliste(bracketGroesse);
		for (int m = 0; m < setzliste.length / 2; m++) {
			if (setzliste[2 * m] > anzOhneCadrage) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Berechnet die Standard-Einzeleliminierung-Setzliste für n Teams (n muss Zweierpotenz sein).<br>
	 * Für n=8: [1,8,4,5,2,7,3,6] → Matches [1v8, 4v5, 2v7, 3v6]<br>
	 * Garantiert: Seed 1 und Seed 2 treffen sich erst im Finale.
	 */
	static int[] berechneSetzliste(int n) {
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
	 * Erstellt einen einzelnen Turnierbaum-Sheet für die übergebenen Teams.
	 * Wird von externen Klassen (z.B. MaastrichterFinalrundeSheet) für A/B/C/D-Gruppen aufgerufen.
	 *
	 * @param gruppeTeams Teams für diese Gruppe (müssen mindestens 2 sein)
	 * @param sheetName   Name des zu erstellenden Sheets
	 * @param sheetPos    Position des Sheets in der Tabelle
	 */
	public void erstelleGruppeBracket(TeamMeldungen gruppeTeams, String sheetName, short sheetPos)
			throws GenerateException {
		erstelleGruppeBracket(gruppeTeams, sheetName, sheetPos, getKonfigurationSheet());
	}

	/**
	 * Erstellt einen KO-Bracket-Sheet mit externer Konfiguration.
	 * Für Aufrufer ohne eigenes "KO Konfiguration"-Blatt (z.B. Maastrichter).
	 *
	 * @param gruppeTeams Teams für diese Gruppe (müssen mindestens 2 sein)
	 * @param sheetName   Name des zu erstellenden Sheets
	 * @param sheetPos    Position des Sheets in der Tabelle
	 * @param konfig      Konfiguration (z.B. aus MaastrichterKonfigurationSheet)
	 */
	public void erstelleGruppeBracket(TeamMeldungen gruppeTeams, String sheetName, short sheetPos,
			IKoBracketKonfiguration konfig) throws GenerateException {
		erstelleGruppeBracket(gruppeTeams, sheetName, sheetPos, konfig, schluesselAusSheetName(sheetName));
	}

	/**
	 * Erstellt einen KO-Bracket-Sheet mit externer Konfiguration und explizitem Metadaten-Schlüssel.
	 * Für Aufrufer, deren Sheet-Name nicht dem "KO Turnierbaum"-Muster folgt (z.B. Maastrichter Finale).
	 *
	 * @param gruppeTeams       Teams für diese Gruppe (müssen mindestens 2 sein)
	 * @param sheetName         Name des zu erstellenden Sheets
	 * @param sheetPos          Position des Sheets in der Tabelle
	 * @param konfig            Konfiguration (z.B. aus MaastrichterKonfigurationSheet)
	 * @param metadatenSchluessel expliziter Metadaten-Schlüssel für den Named-Range-Eintrag
	 */
	public void erstelleGruppeBracket(TeamMeldungen gruppeTeams, String sheetName, short sheetPos,
			IKoBracketKonfiguration konfig, String metadatenSchluessel) throws GenerateException {
		erstelleGruppeBracket(gruppeTeams, sheetName, sheetPos, konfig, metadatenSchluessel, null);
	}

	/**
	 * Erstellt einen KO-Bracket-Sheet mit explizitem „Gruppe X"-Banner über dem Turnierbaum.
	 * Wird z.B. von {@code MaastrichterFinalrundeSheet} verwendet, um die A/B/C/D-Finalrunden
	 * zu beschriften.
	 *
	 * @param gruppenLabel    Buchstabe/Bezeichner für das Gruppen-Banner (z.B. „A"); {@code null} = ohne Banner
	 */
	public void erstelleGruppeBracket(TeamMeldungen gruppeTeams, String sheetName, short sheetPos,
			IKoBracketKonfiguration konfig, String metadatenSchluessel, String gruppenLabel)
			throws GenerateException {
		if (gruppeTeams.size() < 2) {
			return;
		}
		int bracketGroesse = berechneBracketGroesse(gruppeTeams.size());
		int numRunden = Integer.numberOfTrailingZeros(bracketGroesse);
		this.aktuellerGruppenSheetName = sheetName;
		try {
			NewSheet.from(this, sheetName, metadatenSchluessel)
					.pos(sheetPos)
					.hideGrid()
					.tabColor(konfig.getKoTurnierbaumTabFarbe())
					.setActiv()
					.create();
			XSpreadsheet xSheet = getSheetHelper().findByName(sheetName);
			TurnierSheet.from(xSheet, getWorkingSpreadsheet()).setActiv();
			erstelleTurnierbaum(xSheet, gruppeTeams, numRunden, bracketGroesse, konfig, metadatenSchluessel,
					gruppenLabel);
		} finally {
			this.aktuellerGruppenSheetName = null;
		}
	}

	/**
	 * Leitet den Metadaten-Schlüssel aus dem Sheet-Namen ab.
	 * Einzelgruppe ("KO Turnierbaum"): leerer Suffix.
	 * Gruppe mit Buchstabe ("KO Turnierbaum A"): Suffix "A".
	 * Unterstützt sowohl lokalisierte Namen als auch Legacy-Namen (alte deutsche Dokumente).
	 */
	static String schluesselAusSheetName(String sheetName) {
		var praefix = SheetNamen.koTurnierbaumEinzel();
		if (praefix.equals(sheetName)) {
			return SheetMetadataHelper.schluesselKoTurnierbaum("");
		}
		if (sheetName.startsWith(praefix)) {
			return SheetMetadataHelper.schluesselKoTurnierbaum(
					sheetName.substring(praefix.length()).replace(" ", "_"));
		}
		// Legacy: ältere deutsche Dokumente ("KO Turnierbaum")
		var legacy = SheetNamen.LEGACY_KO_TURNIERBAUM_EINZEL;
		if (legacy.equals(sheetName)) {
			return SheetMetadataHelper.schluesselKoTurnierbaum("");
		}
		return SheetMetadataHelper.schluesselKoTurnierbaum(
				sheetName.substring(legacy.length()).replace(" ", "_"));
	}

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
		int minLetzteGruppe = getKonfigurationSheet().getMinLetzteGruppeGroesse();
		erstelleAlleGruppenBaeume(alleMeldungen, gruppenGroesse, minLetzteGruppe);
	}

	/**
	 * Schreibt deterministische Test-Ergebnisse in Runde 1 (und ggf. Cadrage) eines bereits
	 * erstellten Turnierbaum-Sheets. Pro Match wird ein Sieger via {@link RandomSource} gewählt,
	 * 13 in dessen Score-Zelle geschrieben und {@code RandomSource.nextInt(13)} in die Score-Zelle
	 * des Verlierers. Die {@code WENN}-Sieger-Formeln in Runden 2+ lösen die Folgerunden danach
	 * automatisch auf, sobald LO neu rechnet.
	 *
	 * <p>Voraussetzung: Der Layout-State (bracketGroesse, mitCadrage, anzOhneCadrage, Offsets) muss
	 * zur Übergabe-Sheet passen — typischer Aufruf direkt nach {@link #erstelleTurnierbaumOhneDialog()},
	 * Iteration über alle erzeugten Gruppen-Sheets bei gleicher Gruppengröße.
	 */
	public void schreibeRunde1TestErgebnisse(String sheetName) throws GenerateException {
		XSpreadsheet xSheet = getSheetHelper().findByName(sheetName);
		if (xSheet == null) {
			return;
		}
		int anzMatchesR1 = aktuelleBracketGroesse / 2;
		int[] setzliste = berechneSetzliste(aktuelleBracketGroesse);

		RangeData scoresR1 = new RangeData();
		RangeData scoresCadrage = mitCadrage ? new RangeData() : null;

		for (int m = 0; m < anzMatchesR1; m++) {
			int seedA = setzliste[2 * m];
			int seedB = setzliste[2 * m + 1];
			if (scoresCadrage != null) {
				fuegeCadrageTestergebnisHinzu(scoresCadrage, seedA);
				fuegeCadrageTestergebnisHinzu(scoresCadrage, seedB);
			}

			setzeTestergebnis(scoresR1, teamAZeile(1, m), teamBZeile(1, m));
		}

		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		Position startR1 = Position.from(scoreSpalte(1), ersteZeile());
		RangeHelper.from(xSheet, xDoc, scoresR1.getRangePosition(startR1)).setDataInRange(scoresR1);

		if (scoresCadrage != null) {
			Position startCad = Position.from(cadrageScoreSpalte(), ersteZeile());
			RangeHelper.from(xSheet, xDoc, scoresCadrage.getRangePosition(startCad)).setDataInRange(scoresCadrage);
		}
	}

	private void fuegeCadrageTestergebnisHinzu(RangeData scoresCadrage, int seed) {
		if (seed <= anzOhneCadrage) {
			return;
		}
		setzeTestergebnis(scoresCadrage, cadrageTeamAZeile(seed), cadrageTeamBZeile(seed));
	}

	private void setzeTestergebnis(RangeData scores, int rowA, int rowB) {
		int relA = rowA - ersteZeile();
		int relB = rowB - ersteZeile();
		int siegerSeite = RandomSource.nextInt(2);
		int verliererPunkte = RandomSource.nextInt(13);
		while (scores.size() <= relB) {
			scores.addNewRow().newEmpty();
		}
		scores.set(relA, new RowData(siegerSeite == 0 ? 13 : verliererPunkte));
		scores.set(relB, new RowData(siegerSeite == 0 ? verliererPunkte : 13));
	}

	/**
	 * Schreibt deterministische Test-Ergebnisse in alle Folgerunden (ab Runde 2 bis zum Finale).
	 * Voraussetzung: {@link #schreibeRunde1TestErgebnisse(String)} wurde bereits aufgerufen und
	 * eine globale Neuberechnung ist erfolgt, sodass die Team-Namen über die Sieger-Formeln in
	 * den Folgerunden propagiert sind. Score-Zellen sind bisher leer und werden hier pro Match
	 * mit einer 13:x-Paarung gefüllt (Sieger und Verlierer-Punkte via {@link RandomSource}).
	 */
	public void schreibeFolgerundenTestErgebnisse(String sheetName) throws GenerateException {
		XSpreadsheet xSheet = getSheetHelper().findByName(sheetName);
		if (xSheet == null) {
			return;
		}
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		int numRunden = Integer.numberOfTrailingZeros(aktuelleBracketGroesse);
		for (int runde = 2; runde <= numRunden; runde++) {
			int anzMatches = aktuelleBracketGroesse / (1 << runde);
			int spalte = scoreSpalte(runde);
			for (int m = 0; m < anzMatches; m++) {
				int rowA = teamAZeile(runde, m);
				int rowB = teamBZeile(runde, m);
				int insideStride = rowB - rowA;
				int siegerSeite = RandomSource.nextInt(2);
				int verliererPunkte = RandomSource.nextInt(13);
				int scoreA = siegerSeite == 0 ? 13 : verliererPunkte;
				int scoreB = siegerSeite == 0 ? verliererPunkte : 13;

				RangeData data = new RangeData();
				for (int offset = 0; offset <= insideStride; offset++) {
					if (offset == 0) {
						data.addNewRow().newInt(scoreA);
					} else if (offset == insideStride) {
						data.addNewRow().newInt(scoreB);
					} else {
						data.addNewRow().newEmpty();
					}
				}
				Position start = Position.from(spalte, rowA);
				RangeHelper.from(xSheet, xDoc, data.getRangePosition(start)).setDataInRange(data);
			}
		}
	}

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet meldelisteXSheet = meldeliste.getXSpreadSheet();
		if (meldelisteXSheet == null) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.ko.turnierbaum"))
					.message(I18n.get("msg.text.meldeliste.nicht.gefunden"))
					.show();
			return;
		}

		meldeliste.aktualisiereMeldeliste();
		pruefeUndFragObAlleAktivieren();

		String rangFehler = meldeliste.validiereRangSpalte();
		if (rangFehler != null) {
			MessageBoxResult result = MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption(I18n.get("msg.caption.ko.rang.fehler"))
					.message(I18n.get("msg.text.ko.rang.durchnummerieren", rangFehler))
					.show();
			if (result != MessageBoxResult.YES) {
				return;
			}
			meldeliste.rangSpalteDurchnummerieren();
		}

		TeamMeldungen alleMeldungen = meldeliste.getMeldungenSortiertNachRangliste();
		if (alleMeldungen.size() < 2) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.ko.turnierbaum"))
					.message(I18n.get("msg.text.ko.mindestens.2.teams", alleMeldungen.size()))
					.show();
			return;
		}

		int gruppenGroesse = getKonfigurationSheet().getGruppenGroesse();
		int minLetzteGruppe = getKonfigurationSheet().getMinLetzteGruppeGroesse();
		erstelleAlleGruppenBaeume(alleMeldungen, gruppenGroesse, minLetzteGruppe);
	}

	private void pruefeUndFragObAlleAktivieren() throws GenerateException {
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen.size() > 0) {
			return;
		}
		TeamMeldungen alleMeldungen = meldeliste.getAlleMeldungen();
		if (alleMeldungen.size() == 0) {
			return;
		}
		MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
				.caption(I18n.get("msg.caption.keine.aktiven.meldungen"))
				.message(I18n.get("msg.text.keine.aktiven.teams.aktivieren", alleMeldungen.size()))
				.show();
		if (result == MessageBoxResult.YES) {
			meldeliste.alleTeamsAktivieren();
		}
	}

	// ---------------------------------------------------------------
	// Gruppen-Logik
	// ---------------------------------------------------------------

	/**
	 * Teilt alle Meldungen in Gruppen auf und erstellt pro Gruppe einen eigenen Turnierbaum-Sheet.
	 * Die Aufteilung erfolgt gemäß {@link GruppenAufteilungRechner}: kleine letzte Gruppen werden
	 * in die vorherige Gruppe gefaltet; Cadrage übernimmt dort den Ausgleich.
	 */
	private void erstelleAlleGruppenBaeume(TeamMeldungen alleMeldungen, int gruppenGroesse,
			int minLetzteGruppe) throws GenerateException {
		List<Integer> gruppenGroessen = GruppenAufteilungRechner.berechne(
				alleMeldungen.size(), gruppenGroesse, minLetzteGruppe);
		int anzGruppen = gruppenGroessen.size();

		alleGruppenSheetNamenLoeschen();

		var alleTeams = alleMeldungen.teams();
		int startIndex = 0;
		List<GruppenTurnierbaumDaten> gruppenDaten = new ArrayList<>();

		for (int g = 0; g < anzGruppen; g++) {
			int groesse = gruppenGroessen.get(g);
			var gruppenMeldungen = new TeamMeldungen();
			for (int i = startIndex; i < startIndex + groesse; i++) {
				gruppenMeldungen.addTeamWennNichtVorhanden(alleTeams.get(i));
			}
			startIndex += groesse;

			int bracketGroesse = berechneBracketGroesse(gruppenMeldungen.size());
			int numRunden = Integer.numberOfTrailingZeros(bracketGroesse);
			gruppenDaten.add(new GruppenTurnierbaumDaten(gruppenMeldungen, bracketGroesse, numRunden));
		}

		this.spielbahn = getKonfigurationSheet().getSpielbaumSpielbahn();
		List<GruppenBahnNummern> bahnNummern = berechneGruppenUebergreifendeBahnNummern(gruppenDaten);

		for (int g = 0; g < anzGruppen; g++) {
			GruppenTurnierbaumDaten gruppenDatum = gruppenDaten.get(g);
			String sheetName = sheetNameFuerGruppe(g, anzGruppen);

			// aktuellerGruppenSheetName setzen BEVOR NewSheet.create() aufgerufen wird,
			// damit PageStyleHelper.applytoSheet() via getXSpreadSheet() das richtige Sheet trifft
			this.aktuellerGruppenSheetName = sheetName;
			this.vorgegebeneBahnNummernProRunde = bahnNummern.get(g).runden();
			this.vorgegebeneCadrageBahnNummern = bahnNummern.get(g).cadrage();
			try {
				NewSheet.from(this, sheetName, schluesselFuerGruppe(g, anzGruppen))
						.pos((short) (DefaultSheetPos.KO_TURNIERBAUM + g))
						.hideGrid()
						.tabColor(getKonfigurationSheet().getKoTurnierbaumTabFarbe())
						.setActiv()
						.create();

				XSpreadsheet xSheet = getSheetHelper().findByName(sheetName);
				TurnierSheet.from(xSheet, getWorkingSpreadsheet()).setActiv();
				String gruppenLabel = (anzGruppen > 1) ? String.valueOf((char) ('A' + g)) : null;
				erstelleTurnierbaum(xSheet, gruppenDatum.meldungen(), gruppenDatum.numRunden(),
						gruppenDatum.bracketGroesse(),
						getKonfigurationSheet(), schluesselFuerGruppe(g, anzGruppen), gruppenLabel);
			} finally {
				this.aktuellerGruppenSheetName = null;
				this.vorgegebeneBahnNummernProRunde = List.of();
				this.vorgegebeneCadrageBahnNummern = null;
			}
		}
	}

	private record GruppenTurnierbaumDaten(TeamMeldungen meldungen, int bracketGroesse, int numRunden) {}

	private record GruppenBahnNummern(List<int[]> runden, int[] cadrage) {}

	private List<GruppenBahnNummern> berechneGruppenUebergreifendeBahnNummern(
			List<GruppenTurnierbaumDaten> gruppenDaten) {
		List<List<int[]>> rundenJeGruppe = new ArrayList<>();
		List<int[]> cadrageJeGruppe = new ArrayList<>();
		for (int g = 0; g < gruppenDaten.size(); g++) {
			rundenJeGruppe.add(new ArrayList<>());
			cadrageJeGruppe.add(new int[0]);
		}

		int maxRunden = gruppenDaten.stream()
				.mapToInt(GruppenTurnierbaumDaten::numRunden)
				.max()
				.orElse(0);
		for (int runde = 1; runde <= maxRunden; runde++) {
			int[] matchesProGruppe = new int[gruppenDaten.size()];
			int alleMatches = 0;
			for (int g = 0; g < gruppenDaten.size(); g++) {
				GruppenTurnierbaumDaten daten = gruppenDaten.get(g);
				if (runde <= daten.numRunden()) {
					matchesProGruppe[g] = daten.bracketGroesse() / (1 << runde);
					alleMatches += matchesProGruppe[g];
				}
			}
			int[] alleBahnNummern = berechneBahnNummern(alleMatches);
			int offset = 0;
			for (int g = 0; g < gruppenDaten.size(); g++) {
				int anzMatches = matchesProGruppe[g];
				rundenJeGruppe.get(g).add(kopiereBahnNummern(alleBahnNummern, offset, anzMatches));
				offset += anzMatches;
			}
		}

		int[] cadrageMatchesProGruppe = new int[gruppenDaten.size()];
		int alleCadrageMatches = 0;
		for (int g = 0; g < gruppenDaten.size(); g++) {
			GruppenTurnierbaumDaten daten = gruppenDaten.get(g);
			if (daten.meldungen().size() > daten.bracketGroesse()) {
				cadrageMatchesProGruppe[g] = new CadrageRechner(daten.meldungen().size()).anzTeams() / 2;
				alleCadrageMatches += cadrageMatchesProGruppe[g];
			}
		}
		int[] alleCadrageBahnNummern = berechneBahnNummern(alleCadrageMatches);
		int offset = 0;
		for (int g = 0; g < gruppenDaten.size(); g++) {
			int anzMatches = cadrageMatchesProGruppe[g];
			cadrageJeGruppe.set(g, kopiereBahnNummern(alleCadrageBahnNummern, offset, anzMatches));
			offset += anzMatches;
		}

		List<GruppenBahnNummern> ergebnis = new ArrayList<>();
		for (int g = 0; g < gruppenDaten.size(); g++) {
			ergebnis.add(new GruppenBahnNummern(List.copyOf(rundenJeGruppe.get(g)), cadrageJeGruppe.get(g)));
		}
		return ergebnis;
	}

	private int[] kopiereBahnNummern(int[] quelle, int offset, int laenge) {
		int[] ziel = new int[laenge];
		System.arraycopy(quelle, offset, ziel, 0, laenge);
		return ziel;
	}

	/**
	 * Blattname für eine Gruppe: bei einer Gruppe ohne Buchstabe, sonst mit A, B, C …
	 */
	static String sheetNameFuerGruppe(int gruppenIndex, int anzGruppen) {
		if (anzGruppen == 1) {
			return SheetNamen.koTurnierbaumEinzel();
		}
		return SheetNamen.koTurnierbaumGruppe(String.valueOf((char) ('A' + gruppenIndex)));
	}

	/**
	 * Liefert den Metadaten-Schlüssel für einen Gruppen-Turnierbaum.
	 * Einzelgruppe: {@code ""}-Suffix, Mehrgruppen: {@code "A"}, {@code "B"}, …
	 */
	static String schluesselFuerGruppe(int gruppenIndex, int anzGruppen) {
		if (anzGruppen == 1) {
			return SheetMetadataHelper.schluesselKoTurnierbaum("");
		}
		return SheetMetadataHelper.schluesselKoTurnierbaum(String.valueOf((char) ('A' + gruppenIndex)));
	}

	/**
	 * Löscht alle vorhandenen Turnierbaum-Sheets (lokalisierte und Legacy-Namen werden erkannt).
	 */
	private void alleGruppenSheetNamenLoeschen() throws GenerateException {
		var praefix = SheetNamen.koTurnierbaumEinzel();
		var legacyPraefix = SheetNamen.LEGACY_KO_TURNIERBAUM_EINZEL;
		for (String name : getSheetHelper().getSheets().getElementNames()) {
			if (name.startsWith(praefix) || name.startsWith(legacyPraefix)) {
				getSheetHelper().removeSheet(name);
			}
		}
	}

	private void erstelleTurnierbaum(XSpreadsheet xSheet, TeamMeldungen meldungen, int numRunden,
			int bracketGroesse, IKoBracketKonfiguration konfig, String metadatenSchluessel,
			String gruppenLabel) throws GenerateException {

		sheet().processBoxinfo("processbox.ko.turnierbaum.erstellen");

		this.aktuelleScorePositionen = new ArrayList<>();
		this.gruppenHeaderLabel = gruppenLabel;
		this.gruppenZeileOffset = (gruppenLabel != null) ? 1 : 0;

		this.spielbahn = konfig.getSpielbaumSpielbahn();
		this.teamAnzeige = konfig.getSpielbaumTeamAnzeige();
		this.spielUmPlatz3 = konfig.isSpielbaumSpielUmPlatz3();
		this.bahnNurRunde1 = konfig.isSpielbaumBahnNurRunde1();
		this.headerFarbe      = konfig.getTurnierbaumHeaderFarbe();
		this.teamAFarbe       = konfig.getTurnierbaumTeamAFarbe();
		this.teamBFarbe       = konfig.getTurnierbaumTeamBFarbe();
		this.siegerFarbe      = konfig.getTurnierbaumSiegerFarbe();
		this.bahnFarbe        = konfig.getTurnierbaumBahnFarbe();
		this.drittePlatzFarbe = konfig.getTurnierbaumDrittePlatzFarbe();
		this.meldeListeTeamnameAnzeigen    = konfig.isMeldeListeTeamnameAnzeigen();
		this.meldeListeVereinsnameAnzeigen = konfig.isMeldeListeVereinsnameAnzeigen();
		this.meldeListeFormation           = konfig.getMeldeListeFormation();

		this.aktuelleBracketGroesse = bracketGroesse;
		this.runde1MatchZeilenAbstand = berechneRunde1MatchZeilenAbstand(meldungen.size(), bracketGroesse);
		this.runde1SlotAbstand = berechneRunde1SlotAbstand(meldungen.size(), bracketGroesse);

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
			this.anzOhneCadrage = rechner.anzOhneCadrage();
			this.cadrageSpaltOffset = colGroupSize;
			this.cadrageBahnNummern = vorgegebeneCadrageBahnNummern != null
					? vorgegebeneCadrageBahnNummern
					: berechneBahnNummern(rechner.anzTeams() / 2);
		} else {
			this.mitCadrage = false;
			this.anzOhneCadrage = bracketGroesse;
			this.cadrageSpaltOffset = 0;
			this.cadrageBahnNummern = new int[0];
		}

		int[] setzliste = berechneSetzliste(bracketGroesse);
		int anzMatchesR1 = bracketGroesse / 2;
		int letzteZeile = berechneLetzteZeile(anzMatchesR1);
		int letzteSpalte = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME)
				? siegerSpalte(numRunden)
				: siegerNameSpalte(numRunden);

		// Spaltenbreiten setzen
		formatiereKolumnen(xSheet, numRunden);
		setzeBracketLeerflaecheZurueck(xSheet, letzteZeile, letzteSpalte);

		// Header (2 Zeilen: Titel + Spaltenbeschriftung)
		schreibeHeader(xSheet, numRunden);

		// Runde 1: direkte Einträge aus Setzliste; Cadrage-Slots via Vorrundenformel
		int[] bahnR1 = bahnNummernFuerRunde(1, anzMatchesR1);
		for (int m = 0; m < anzMatchesR1; m++) {
			int seedA = setzliste[2 * m];
			int seedB = setzliste[2 * m + 1];

			int rowA = teamAZeile(1, m);
			int rowB = teamBZeile(1, m);

			if (mitBahn()) {
				schreibeBahnZelle(xSheet, 1, rowA, rowB, bahnR1[m]);
			}

			if (mitCadrage && seedA > anzOhneCadrage) {
				schreibeCadrageMatch(xSheet, meldungen, seedA);
				schreibeCadrageGewinnerFormel(xSheet, seedA, rowA, true);
			} else {
				schreibeTeamZelleR1(xSheet, rowA, getTeamNrBySeedPosition(meldungen, seedA), true);
			}

			if (mitCadrage && seedB > anzOhneCadrage) {
				schreibeCadrageMatch(xSheet, meldungen, seedB);
				schreibeCadrageGewinnerFormel(xSheet, seedB, rowB, false);
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
			int[] bahnRunde = bahnNummernFuerRunde(r, anzMatches);
			for (int m = 0; m < anzMatches; m++) {
				int rowA = teamAZeile(r, m);
				int rowB = teamBZeile(r, m);

				if (mitBahnInRunde(r)) {
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

		// Optimale Spaltenbreite und Zeilenhöhe setzen
		// Im NAME-Modus ist siegerNameSpalte versteckt (Breite 0) – nicht anfassen
		getSheetHelper().setOptimaleBreiteUndHoeheAlles(xSheet, 0, letzteZeile, 0, letzteSpalte);
		formatieresSiegerSpalten(xSheet, numRunden);

		// "Gruppe X"-Banner über dem Bracket (nur bei Mehrgruppen)
		if (gruppenHeaderLabel != null) {
			schreibeGruppenHeader(xSheet, letzteSpalte);
		}

		// Druckbereich auf den sichtbaren Bracket-Bereich begrenzen.
		// Score-Daten-Arbeitszelle (siegerNameSpalte + 2) liegt bewusst außerhalb,
		// damit sie weder gedruckt noch im Web-Renderer (Used-Area-Fallback) auftaucht.
		PrintArea.from(xSheet, getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(0, 0, letzteSpalte, letzteZeile));

		speichereScoreBereiche(xSheet, numRunden, metadatenSchluessel, aktuelleScorePositionen);
		aktuelleScorePositionen = null;
		gruppenHeaderLabel = null;
		gruppenZeileOffset = 0;
	}

	private int berechneLetzteZeile(int anzMatchesR1) {
		int letzteZeile = teamBZeile(1, anzMatchesR1 - 1);
		if (spielUmPlatz3 && aktuelleBracketGroesse >= 4) {
			// platz3TeamBZeile = letzteZeileHauptbaum + 3 (Header) + 2 (TeamB-Zeile)
			letzteZeile += 5;
		}
		if (mitCadrage) {
			letzteZeile = Math.max(letzteZeile, cadrageLetzteZeile());
		}
		return letzteZeile;
	}

	/**
	 * Setzt die gesamte Bracket-Fläche vor dem Neuaufbau auf „keine Hintergrundfarbe" zurück, damit
	 * Altfarben eines vorherigen Aufbaus verschwinden. Bewusst wird {@code -1} (transparent) aktiv
	 * geschrieben statt die Property nur zu entfernen: Nur so werden bestehende Füllungen tatsächlich
	 * überschrieben. Zugleich vermeidet {@code -1} deckendes Weiß, das der HTML-Web-Export sonst als
	 * {@code background-color:#FFFFFF} rendern würde ({@code CellBackColor == -1} gilt dort als „keine
	 * Farbe").
	 */
	private void setzeBracketLeerflaecheZurueck(XSpreadsheet xSheet, int letzteZeile, int letzteSpalte)
			throws GenerateException {
		getSheetHelper().setPropertiesInRange(xSheet,
				RangePosition.from(0, ersteZeile(), letzteSpalte, letzteZeile),
				CellProperties.from().setCellBackColor(-1));
	}

	/**
	 * Schreibt das „Gruppe X"-Banner in Zeile 0, gemerged über die volle Breite des Brackets.
	 */
	private void schreibeGruppenHeader(XSpreadsheet xSheet, int letzteSpalte) throws GenerateException {
		String text = I18n.get("ko.turnierbaum.gruppe.header", gruppenHeaderLabel);
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(0, 0), text)
						.setEndPosMergeSpaltePlus(letzteSpalte)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCellBackColor(headerFarbe)
						.setCharColor("FFFFFF")
						.setShrinkToFit(true)
						.setBorder(BorderFactory.from().allThin().toBorder()));
	}

	/**
	 * Kodiert die gesammelten Score-Positionen als kompakten String, schreibt ihn in eine
	 * versteckte Arbeitszelle ({@code siegerNameSpalte + 2}, {@link #scoreDataZeile()}) und
	 * legt einen Named Range ({@code __PTM_SCORE_…}) darauf an.
	 */
	private void speichereScoreBereiche(XSpreadsheet xSheet, int numRunden,
			String metadatenSchluessel, List<Position> positionen) throws GenerateException {
		var encoded = SCORE_DATA_PREFIX + positionen.stream()
				.map(p -> p.getSpalte() + "," + p.getZeile())
				.collect(Collectors.joining("|"));
		int dataSpalte = siegerNameSpalte(numRunden) + 2;
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(dataSpalte, scoreDataZeile()), encoded));
		var scoreKey = SheetMetadataHelper.scoreSchluessel(metadatenSchluessel);
		SheetMetadataHelper.schreibeScoreZellenMetadaten(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), xSheet, scoreKey, dataSpalte, scoreDataZeile());
		formatiereScoreZellen(positionen);
	}

	/**
	 * Wendet bedingte Formatierung auf alle Score-Zellen-Paare an.
	 * <p>
	 * Pro Paar (Team-A-Zeile, Team-B-Zeile):
	 * <ol>
	 *   <li>Fehler-Style bei Wert außerhalb 0–13</li>
	 *   <li>Fehler-Style bei Texteingabe</li>
	 *   <li>Fehler-Style bei Gleichstand (beide Zellen eines Paars erhalten denselben Wert)</li>
	 *   <li>Orange-Zebra-Hervorhebung editierbarer Felder (togglebar per BOOLEANPROPERTY)</li>
	 * </ol>
	 * Die Positionen stammen aus denselben Daten, die via
	 * {@link SheetMetadataHelper#schreibeScoreZellenMetadaten} gespeichert wurden.
	 */
	private void formatiereScoreZellen(List<Position> positionen) throws GenerateException {
		for (int i = 0; i + 1 < positionen.size(); i += 2) {
			var posA = positionen.get(i);
			var posB = positionen.get(i + 1);

			String addrA = posA.getAddressWith$();
			String addrB = posB.getAddressWith$();
			String gleichstandFormel = "AND(ISNUMBER(" + addrA + ");" + addrA + "<>\"\";"
					+ "ISNUMBER(" + addrB + ");" + addrB + "<>\"\";" + addrA + "=" + addrB + ")";

			var rangeA = RangePosition.from(posA);
			var rangeB = RangePosition.from(posB);

			// Fehlerprüfungen einzeln pro Zelle – vermeidet Einfärben von Lückenzeilen zwischen den Paaren
			ConditionalFormatHelper.from(this, rangeA).clear()
					.formula1("0").formula2("13").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
					.formulaIsText().styleIsFehler().applyAndDoReset()
					.formula1(gleichstandFormel).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset();
			ConditionalFormatHelper.from(this, rangeB).clear()
					.formula1("0").formula2("13").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
					.formulaIsText().styleIsFehler().applyAndDoReset()
					.formula1(gleichstandFormel).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset();

			// Editierbar-Hervorhebung pro Zelle, basierend auf Team-A/B-Position (nicht Zeilen-Parität)
			EditierbaresZelleFormatHelper.anwendenFuerTeam(this, rangeA, true);
			EditierbaresZelleFormatHelper.anwendenFuerTeam(this, rangeB, false);
		}
	}

	/**
	 * Dekodiert gespeicherte Score-Positionen zurück zu {@link RangePosition}-Objekten.
	 * Aufeinanderfolgende Zeilen in derselben Spalte werden zu einem Bereich zusammengefasst.
	 * Gibt eine leere Liste zurück wenn {@code encoded} leer oder {@code null} ist.
	 */
	public static List<RangePosition> decodeScoreBereiche(String encoded) {
		if (encoded == null || encoded.isBlank()) {
			return List.of();
		}
		String data = encoded.startsWith(SCORE_DATA_PREFIX)
				? encoded.substring(SCORE_DATA_PREFIX.length())
				: encoded; // Rückwärtskompatibilität: alte Daten ohne Präfix
		if (data.isBlank()) {
			return List.of();
		}
		var spalteZuZeilen = new TreeMap<Integer, TreeSet<Integer>>();
		for (var token : data.split("\\|")) {
			var parts = token.split(",");
			if (parts.length != 2) continue;
			try {
				int spalte = Integer.parseInt(parts[0].trim());
				int zeile = Integer.parseInt(parts[1].trim());
				spalteZuZeilen.computeIfAbsent(spalte, _ -> new TreeSet<>()).add(zeile);
			} catch (NumberFormatException ignoriert) {
				// Fehlerhafte Tokens überspringen
			}
		}
		var bereiche = new ArrayList<RangePosition>();
		for (var eintrag : spalteZuZeilen.entrySet()) {
			int spalte = eintrag.getKey();
			int startZeile = -1;
			int letzteZeile = -1;
			for (int zeile : eintrag.getValue()) {
				if (startZeile == -1) {
					startZeile = zeile;
					letzteZeile = zeile;
				} else if (zeile == letzteZeile + 1) {
					letzteZeile = zeile;
				} else {
					bereiche.add(RangePosition.from(spalte, startZeile, spalte, letzteZeile));
					startZeile = zeile;
					letzteZeile = zeile;
				}
			}
			if (startZeile != -1) {
				bereiche.add(RangePosition.from(spalte, startZeile, spalte, letzteZeile));
			}
		}
		return bereiche;
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
			Random rng = RandomSource.asJavaRandom();
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

	private int[] bahnNummernFuerRunde(int runde, int anzMatches) {
		int index = runde - 1;
		if (index >= 0 && index < vorgegebeneBahnNummernProRunde.size()) {
			return vorgegebeneBahnNummernProRunde.get(index);
		}
		return berechneBahnNummern(anzMatches);
	}

	private boolean mitBahnInRunde(int runde) {
		return mitBahn() && (!bahnNurRunde1 || runde == 1);
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

		formatieresSiegerSpalten(xSheet, numRunden);

		// Versteckte Arbeitsspalte für Score-Positions-Daten (Blattschutz)
		getSheetHelper().setColumnProperties(xSheet, siegerNameSpalte(numRunden) + 2,
				ColumnProperties.from().isVisible(false));
	}

	/**
	 * Setzt die Breiten beider Sieger-Spalten einheitlich für alle KO-Turnierbaum-Sheets.
	 * <p>
	 * NR-Modus: siegerSpalte = {@link MeldungenSpalte#DEFAULT_SPALTE_NUMBER_WIDTH}, siegerNameSpalte = {@link #SIEGER_NAME_COL_WIDTH}.<br>
	 * NAME-Modus: siegerSpalte = {@link #SIEGER_NAME_COL_WIDTH}, siegerNameSpalte = versteckt (Breite 0).
	 */
	void formatieresSiegerSpalten(XSpreadsheet xSheet, int numRunden) throws GenerateException {
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NR) {
			getSheetHelper().setColumnProperties(xSheet, siegerSpalte(numRunden),
					ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
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
					StringCellValue.from(xSheet, Position.from(titelStartSpalte, headerZeileTitel()), I18n.get("column.header.cadrage"))
							.setEndPosMergeSpaltePlus(colGroupSize - 1)
							.setCharWeight(FontWeight.BOLD)
							.setHoriJustify(CellHoriJustify.CENTER)
							.setCellBackColor(headerFarbe)
							.setCharColor("FFFFFF")
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setShrinkToFit(true));
			if (mitBahn()) {
				schreibeSpaltenHeader(xSheet, cadrageBahnSpalte(), BAHN_HEADER_KURZ);
			}
			schreibeSpaltenHeader(xSheet, cadrageTeamSpalte(), teamHeader);
			schreibeSpaltenHeader(xSheet, cadrageScoreSpalte(), "Pkt");
		}

		for (int r = 1; r <= numRunden; r++) {
			String rundentitel = berechnRundenTitel(r, numRunden);

			// Zeile 0: Rundentitel über alle Spalten der Runde (merged)
			int titelStartSpalte = mitBahn() ? bahnSpalte(r) : teamSpalte(r);
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(titelStartSpalte, headerZeileTitel()), rundentitel)
							.setEndPosMergeSpaltePlus(colGroupSize - 1)
							.setCharWeight(FontWeight.BOLD)
							.setHoriJustify(CellHoriJustify.CENTER)
							.setCellBackColor(headerFarbe)
							.setCharColor("FFFFFF")
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setShrinkToFit(true));

			// Zeile 1: Spalten-Überschriften
			if (mitBahn()) {
				schreibeSpaltenHeader(xSheet, bahnSpalte(r), BAHN_HEADER_KURZ);
			}
			schreibeSpaltenHeader(xSheet, teamSpalte(r), teamHeader);
			schreibeSpaltenHeader(xSheet, scoreSpalte(r), "Pkt");
		}

		// Sieger-Header (merged über siegerSpalte + siegerNameSpalte)
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(siegerSpalte(numRunden), headerZeileTitel()), I18n.get("column.header.sieger"))
						.setEndPosMergeSpaltePlus(1)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCellBackColor(siegerFarbe)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setShrinkToFit(true));
	}

	private String berechnRundenTitel(int r, int numRunden) {
		if (r == numRunden) {
			return I18n.get("ko.runde.titel.finale");
		} else if (r == numRunden - 1) {
			return I18n.get("ko.runde.titel.halbfinale");
		} else {
			int anzTeamsInRunde = 1 << (numRunden - r + 1);
			return I18n.get("ko.runde.titel.ntel.finale", anzTeamsInRunde / 2);
		}
	}

	private void schreibeSpaltenHeader(XSpreadsheet xSheet, int spalte, String label) throws GenerateException {
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(spalte, headerZeileSpalten()), label)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCellBackColor(headerFarbe)
						.setCharColor("FFFFFF")
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setShrinkToFit(true));
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
		int farbe = istTeamA ? teamBFarbe : teamAFarbe;
		if (nr <= 0) {
			// Defensiver Fallback: im Cadrage-Modell ist jeder Hauptfeld-Slot mit einem echten
			// Team belegt (kein Freilos), daher sollte dieser Zweig nicht erreicht werden. Falls
			// doch, bleibt die Zelle leer statt einen erfundenen Platzhalter anzuzeigen.
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(teamSpalte(1), zeile), "")
							.setCellBackColor(farbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
			return;
		}
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) {
			// Teamname via SVERWEIS
			String formel = MeldeListeHelper.teamNameFormel(String.valueOf(nr),
					meldeListeTeamnameAnzeigen, meldeListeFormation, meldeListeVereinsnameAnzeigen);
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
		int spalte = scoreSpalte(runde);
		if (aktuelleScorePositionen != null) {
			aktuelleScorePositionen.add(Position.from(spalte, zeile));
		}
		int hintergrundFarbe = istTeamA
				? EditierbaresZelleFormatHelper.EDITIERBAR_GERADE_FARBE
				: EditierbaresZelleFormatHelper.EDITIERBAR_UNGERADE_FARBE;
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(spalte, zeile), "")
						.setCellBackColor(hintergrundFarbe)
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

		// Scores müssen Zahlen und explizit nicht leer sein, damit Calc leere Zellen nicht als 0 wertet.
		String formel = "WENN(" + beideScoresVorhandenFormel(scoreAAddr, scoreBAddr) + ";"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + teamAAddr + ";"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + teamBAddr + ";\"?\"));"
				+ "\"\")";

		int targetRow = istTeamA ? teamAZeile(runde, match) : teamBZeile(runde, match);
		CellHoriJustify justify = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME)
				? CellHoriJustify.LEFT : CellHoriJustify.CENTER;

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(teamSpalte(runde), targetRow), formel)
						.setCellBackColor(istTeamA ? teamBFarbe : teamAFarbe)
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

		String siegerFormel = "WENN(" + beideScoresVorhandenFormel(scoreAAddr, scoreBAddr) + ";"
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
						.setShrinkToFit(true)
						.setHoriJustify(justify));

		// Im NR-Modus: zusätzlich Teamname via SVERWEIS in der Nebenspalte
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NR) {
			String siegerNrAddr = Position.from(siegerSp, siegerZeile).getAddressWith$();
			String siegerNameFormel = "WENN(UND(ISTZAHL(" + siegerNrAddr + ");" + siegerNrAddr
					+ "<>\"\";" + siegerNrAddr + ">0);" + MeldeListeHelper.teamNameFormel(siegerNrAddr,
							meldeListeTeamnameAnzeigen, meldeListeFormation, meldeListeVereinsnameAnzeigen) + ";\"\")";

			getSheetHelper().setFormulaInCell(
					StringCellValue.from(xSheet, Position.from(siegerNameSpalte(numRunden), siegerZeile),
							siegerNameFormel)
							.setCellBackColor(siegerFarbe)
							.setBorder(BorderFactory.from().allBold().toBorder())
							.setCharWeight(FontWeight.BOLD)
							.setShrinkToFit(true)
							.setHoriJustify(CellHoriJustify.LEFT));
		}
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden
	// ---------------------------------------------------------------


	/**
	 * Liefert die Team-Nr des Teams an Setzposition {@code seedPos} (1-basiert).
	 * Gibt 0 zurück wenn an dieser Position kein Team vorhanden ist (im Cadrage-Modell
	 * regulär nicht der Fall – jeder Slot ist belegt).
	 */
	private int getTeamNrBySeedPosition(TeamMeldungen meldungen, int seedPos) {
		var teams = meldungen.teams();
		if (seedPos < 1 || seedPos > teams.size()) {
			return 0; // kein Team an dieser Setzposition
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
	 * {@code gesanzTeamsIntern - cadrageIdx}. Jede Cadrage-Partie bekommt eigene Zeilen in
	 * der Cadrage-Spalte, damit auch 13-15 Teams ohne künstliches 16er-Feld darstellbar sind.
	 *
	 * @param slotSeed Bracket-Slot-Seed &gt; {@link #anzOhneCadrage} (z.B. 7 oder 8)
	 */
	private void schreibeCadrageMatch(XSpreadsheet xSheet, TeamMeldungen meldungen, int slotSeed)
			throws GenerateException {
		int cadrageIdx = slotSeed - anzOhneCadrage - 1;
		int opponentSeed = gesanzTeamsIntern - cadrageIdx;
		int rowA = cadrageTeamAZeile(slotSeed);
		int rowB = cadrageTeamBZeile(slotSeed);

		int nrA = getTeamNrBySeedPosition(meldungen, slotSeed);
		int nrB = getTeamNrBySeedPosition(meldungen, opponentSeed);

		if (mitBahn()) {
			// Eine zusammengeführte Bahn-Zelle für die Cadrage-Paarung
			int bahnNr = cadrageIdx < cadrageBahnNummern.length ? cadrageBahnNummern[cadrageIdx] : 0;
			schreibeBahnZelleAnSpalte(xSheet, cadrageBahnSpalte(), rowA, rowB, bahnNr);
		}

		schreibeTeamZelleInSpalte(xSheet, cadrageTeamSpalte(), rowA, nrA, true);
		schreibeTeamZelleInSpalte(xSheet, cadrageTeamSpalte(), rowB, nrB, false);

		// Score-Zellen (editierbar) – Positionen für Blattschutz registrieren
		int cadrageScore = cadrageScoreSpalte();
		if (aktuelleScorePositionen != null) {
			aktuelleScorePositionen.add(Position.from(cadrageScore, rowA));
			aktuelleScorePositionen.add(Position.from(cadrageScore, rowB));
		}
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(cadrageScore, rowA), "")
						.setCellBackColor(EditierbaresZelleFormatHelper.EDITIERBAR_GERADE_FARBE)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(CellHoriJustify.CENTER));
		getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(cadrageScore, rowB), "")
						.setCellBackColor(EditierbaresZelleFormatHelper.EDITIERBAR_UNGERADE_FARBE)
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
		int farbe = istTeamA ? teamBFarbe : teamAFarbe;
		if (nr <= 0) {
			// Defensiver Fallback: jede Cadrage-Paarung besteht aus zwei echten Teams, dieser
			// Zweig sollte nicht erreicht werden. Falls doch, Zelle leer statt Platzhalter.
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(spalte, zeile), "")
							.setCellBackColor(farbe)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
			return;
		}
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NAME) {
			String formel = MeldeListeHelper.teamNameFormel(String.valueOf(nr),
					meldeListeTeamnameAnzeigen, meldeListeFormation, meldeListeVereinsnameAnzeigen);
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
	 * @param slotSeed  Bracket-Slot-Seed der Cadrage-Partie
	 * @param targetRow Zielzeile im Runde-1-Hauptfeld
	 * @param istSlotA  true wenn der Cadrage-Slot in der Team-A-Position von Runde 1 sitzt
	 */
	private void schreibeCadrageGewinnerFormel(XSpreadsheet xSheet, int slotSeed, int targetRow,
			boolean istSlotA) throws GenerateException {
		int rowA = cadrageTeamAZeile(slotSeed);
		int rowB = cadrageTeamBZeile(slotSeed);
		String scoreAAddr = Position.from(cadrageScoreSpalte(), rowA).getAddressWith$();
		String scoreBAddr = Position.from(cadrageScoreSpalte(), rowB).getAddressWith$();
		String teamAAddr = Position.from(cadrageTeamSpalte(), rowA).getAddressWith$();
		String teamBAddr = Position.from(cadrageTeamSpalte(), rowB).getAddressWith$();

		String formel = "WENN(" + beideScoresVorhandenFormel(scoreAAddr, scoreBAddr) + ";"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + teamAAddr + ";"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + teamBAddr + ";\"?\"));"
				+ "\"\")";

		int farbe = istSlotA ? teamBFarbe : teamAFarbe;
		CellHoriJustify justify = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME)
				? CellHoriJustify.LEFT : CellHoriJustify.CENTER;

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(teamSpalte(1), targetRow), formel)
						.setCellBackColor(farbe)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(justify));
	}

	/**
	 * Zeile der oberen Team-Zelle der Cadrage-Partie für einen Bracket-Slot.
	 *
	 * <p>Die Cadrage-Partie fluchtet mit ihrem Ziel-Slot in Runde 1: sie belegt die beiden Zeilen
	 * {@code [zielZeile, zielZeile+1]}, wobei {@code zielZeile} genau die Runde-1-Zeile ist, in der
	 * die Sieger-Formel dieses Slots steht. Dadurch fließt jede Cadrage direkt horizontal in ihren
	 * Runde-1-Slot, statt weit entfernt gestapelt zu werden.
	 */
	private int cadrageTeamAZeile(int slotSeed) {
		return runde1ZeileFuerSeed(slotSeed);
	}

	private int cadrageTeamBZeile(int slotSeed) {
		return cadrageTeamAZeile(slotSeed) + 1;
	}

	/**
	 * Liefert die Runde-1-Zeile, in der der Bracket-Slot mit dem gegebenen Seed sitzt (Team-A-Zeile
	 * bei ungerader, Team-B-Zeile bei gerader Setzlisten-Position).
	 */
	private int runde1ZeileFuerSeed(int slotSeed) {
		int[] setzliste = berechneSetzliste(aktuelleBracketGroesse);
		for (int m = 0; m < setzliste.length / 2; m++) {
			if (setzliste[2 * m] == slotSeed) {
				return teamAZeile(1, m);
			}
			if (setzliste[2 * m + 1] == slotSeed) {
				return teamBZeile(1, m);
			}
		}
		// Sollte nie auftreten: Setzliste ist eine Permutation von 1..bracketGroesse
		throw new IllegalStateException("Seed " + slotSeed + " nicht in Setzliste gefunden");
	}

	private int cadrageLetzteZeile() {
		int letzterMatch = aktuelleBracketGroesse / 2 - 1;
		// Slot B des letzten Matches samt zweizeiligem Cadrage-Feeder darunter.
		return teamBZeile(1, letzterMatch) + 1;
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
						.setShrinkToFit(true)
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

		// Verlierer = Team mit niedrigere Punktzahl (umgekehrt zur Gewinner-Formel)
		String formel = "WENN(" + beideScoresVorhandenFormel(scoreAAddr, scoreBAddr) + ";"
				+ "WENN(" + scoreAAddr + "<" + scoreBAddr + ";" + teamAAddr + ";"
				+ "WENN(" + scoreAAddr + ">" + scoreBAddr + ";" + teamBAddr + ";\"?\"));"
				+ "\"\")";

		CellHoriJustify justify = (teamAnzeige == KoSpielbaumTeamAnzeige.NAME)
				? CellHoriJustify.LEFT : CellHoriJustify.CENTER;

		getSheetHelper().setFormulaInCell(
				StringCellValue.from(xSheet, Position.from(teamSpalte(runde), targetRow), formel)
						.setCellBackColor(istTeamA ? teamBFarbe : teamAFarbe)
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

		String drittePlatzFormel = "WENN(" + beideScoresVorhandenFormel(scoreAAddr, scoreBAddr) + ";"
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
						.setShrinkToFit(true)
						.setHoriJustify(justify));

		// Im NR-Modus: Teamname via SVERWEIS in der Nebenspalte
		if (teamAnzeige == KoSpielbaumTeamAnzeige.NR) {
			String drittePlatzNrAddr = Position.from(siegerSp, siegerZeile).getAddressWith$();
			String drittePlatzNameFormel = "WENN(UND(ISTZAHL(" + drittePlatzNrAddr + ");" + drittePlatzNrAddr
					+ "<>\"\";" + drittePlatzNrAddr + ">0);" + MeldeListeHelper.teamNameFormel(drittePlatzNrAddr,
						meldeListeTeamnameAnzeigen, meldeListeFormation, meldeListeVereinsnameAnzeigen) + ";\"\")";

			getSheetHelper().setFormulaInCell(
					StringCellValue.from(xSheet, Position.from(siegerNameSpalte(numRunden), siegerZeile),
							drittePlatzNameFormel)
							.setCellBackColor(drittePlatzFarbe)
							.setBorder(BorderFactory.from().allBold().toBorder())
							.setCharWeight(FontWeight.BOLD)
							.setShrinkToFit(true)
							.setHoriJustify(CellHoriJustify.LEFT));
		}
	}

	private String beideScoresVorhandenFormel(String scoreAAddr, String scoreBAddr) {
		return "UND(ISTZAHL(" + scoreAAddr + ");" + scoreAAddr + "<>\"\";"
				+ "ISTZAHL(" + scoreBAddr + ");" + scoreBAddr + "<>\"\")";
	}

}
