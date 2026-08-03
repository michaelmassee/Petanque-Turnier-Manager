package de.petanqueturniermanager.schweizer.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_WEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HORI_JUSTIFY;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.SHRINK_TO_FIT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.VERT_JUSTIFY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.algorithmen.common.DurchgangAufteilungRechner;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerSystem;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeFooterHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.sheet.numberformat.UserNumberFormat;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerPropertiesSpalte;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Erstellung 27.03.2024 / Michael Massee
 */

public abstract class SchweizerAbstractSpielrundeSheet extends SheetRunner implements ISheet {

	private static final Logger LOGGER = LogManager.getLogger(SchweizerAbstractSpielrundeSheet.class);

	public static final int SHEET_COLOR = SheetTabFarben.SPIELRUNDE;
	public static final String SHEET_NAMEN = "Spielrunde";

	public static final int ERSTE_HEADER_ZEILE = 0;
	public static final int ZWEITE_HEADER_ZEILE = ERSTE_HEADER_ZEILE + 1;
	public static final int ERSTE_DATEN_ZEILE = ZWEITE_HEADER_ZEILE + 1;

	public static final int NR_CHARHEIGHT = 18;
	public static final int BAHN_NR_SPALTE = 0;
	public static final int TEAM_A_SPALTE = BAHN_NR_SPALTE + 1;
	public static final int TEAM_B_SPALTE = TEAM_A_SPALTE + 1;
	public static final int ERG_TEAM_A_SPALTE = TEAM_B_SPALTE + 1;
	public static final int ERG_TEAM_B_SPALTE = ERG_TEAM_A_SPALTE + 1;
	public static final int FEHLER_SPALTE = ERG_TEAM_B_SPALTE + 1;
	/** Nur befuellt bei aktiver Rundenzeitplanung (isDurchgangAufteilungWirksam), sonst leer. */
	public static final int DURCHGANG_LABEL_SPALTE = FEHLER_SPALTE + 1;
	/** Nur befuellt bei aktiver Rundenzeitplanung (isDurchgangAufteilungWirksam), sonst leer. */
	public static final int DURCHGANG_STARTZEIT_SPALTE = FEHLER_SPALTE + 2;

	private final SchweizerKonfigurationSheet konfigurationSheet;
	private final SchweizerMeldeListeSheetUpdate meldeListe;
	private final SpielrundeHelper spielrundeHelper;
	private final String sheetBaseName;
	private SpielRundeNr spielRundeNrInSheet = null;
	private volatile boolean forceOk = false; // wird fuer Test verwendet

	//	private SpielRundeNr sheetSpielRundeNr = null; // muss nicht der Aktive sein

	protected SchweizerAbstractSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, TurnierSystem.SCHWEIZER, SHEET_NAMEN);
	}

	protected SchweizerAbstractSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem ts,
			String sheetBaseName) {
		super(workingSpreadsheet, ts, sheetBaseName);
		this.sheetBaseName = sheetBaseName;
		konfigurationSheet = initKonfigurationSheet(workingSpreadsheet);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
		spielrundeHelper = new SpielrundeHelper(this, NR_CHARHEIGHT, NR_CHARHEIGHT, true,
				konfigurationSheet.getSpielRundeHintergrundFarbeGeradeStyle(),
				konfigurationSheet.getSpielRundeHintergrundFarbeUnGeradeStyle());
	}

	protected SchweizerKonfigurationSheet initKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	public SchweizerKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	protected final boolean canStart(TeamMeldungen meldungen) throws GenerateException {
		if (getSpielRundeNr().getNr() < 1) {
			getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
					.message(I18n.get("schweizer.spielrunde.fehler.ungueltige.spielrunde", getSpielRundeNr().getNr()))
					.show();
			return false;
		}

		if (meldungen.size() < 6) {
			getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
					.message(I18n.get("schweizer.spielrunde.fehler.zu.wenige.meldungen", meldungen.size()))
					.show();
			return false;
		}
		return true;
	}

	@VisibleForTesting
	protected SchweizerMeldeListeSheetUpdate initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerMeldeListeSheetUpdate(workingSpreadsheet);
	}

	/**
	 * Liefert den Metadaten-Schlüssel für die Spielrunde mit der angegebenen Nummer.
	 * Subklassen (z.B. Maastrichter) können dies überschreiben um einen anderen Schlüssel zu verwenden.
	 */
	protected String getSpielrundeSchluessel(int rundeNr) {
		return SheetMetadataHelper.schluesselSchweizerSpielrunde(rundeNr);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		var rundeNr = getSpielRundeNr();
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				getSpielrundeSchluessel(rundeNr.getNr()), getLegacySheetName(rundeNr));
	}

	/**
	 * Gibt den lokalisierten Tabellennamen für eine Spielrunde zurück (für die Sheet-Erstellung).
	 * Kann von Subklassen überschrieben werden (z.B. für Maastrichter Vorrunden).
	 */
	protected String getSheetName(SpielRundeNr nr) {
		return SheetNamen.spielrunde(nr.getNr());
	}

	/**
	 * Gibt den unveränderlichen deutschen Legacy-Namen zurück (für {@code findeSheetUndHeile}-Fallback
	 * bei älteren Dokumenten ohne Metadaten).
	 */
	protected final String getLegacySheetName(SpielRundeNr nr) {
		return nr.getNr() + ". " + sheetBaseName;
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	public final SpielRundeNr getSpielRundeNr() throws GenerateException {
		return getKonfigurationSheet().getAktiveSpielRunde();
	}

	public SpielRundeNr getSpielRundeNrInSheet() {
		return spielRundeNrInSheet;
	}

	public void setSpielRundeNrInSheet(SpielRundeNr spielRundeNrInSheet) {
		this.spielRundeNrInSheet = spielRundeNrInSheet;
	}

	public SchweizerMeldeListeSheetUpdate getMeldeListe() {
		return meldeListe;
	}

	/**
	 * Liest alle gespielten Runden ein und befüllt:
	 * <ul>
	 *   <li>Team-Gegner-Beziehungen (für Auslosung der nächsten Runde)</li>
	 *   <li>Freilos-Flags</li>
	 * </ul>
	 *
	 * @param aktiveMeldungen aktive Teams
	 * @param abSpielrunde    erste einzulesende Runde (inkl.)
	 * @param bisSpielrunde   letzte einzulesende Runde (inkl.)
	 * @return Auswertungsdaten je Team (Siege, Punktedifferenz, Gegnerliste)
	 */
	protected List<SchweizerTeamErgebnis> gespieltenRundenEinlesen(TeamMeldungen aktiveMeldungen, int abSpielrunde,
			int bisSpielrunde) throws GenerateException {

		Map<Integer, int[]> statsMap = new HashMap<>(); // teamNr → [0]=siege, [1]=punktediff, [2]=punkte+
		Map<Integer, List<Integer>> gegnerMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new int[3]);
			gegnerMap.put(team.getNr(), new ArrayList<>());
		}

		if (bisSpielrunde >= abSpielrunde && bisSpielrunde >= 1) {
			int spielrunde = (abSpielrunde > 1) ? abSpielrunde : 1;
			processBoxinfo("processbox.gespielte.runden.einlesen", spielrunde, bisSpielrunde);
			var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();

			for (; spielrunde <= bisSpielrunde; spielrunde++) {
				SheetRunner.testDoCancelTask();
				// Iterations-Lookup: Metadaten-first (überlebt Umbenennung), Fallback auf Namen
				XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
						getSpielrundeSchluessel(spielrunde), getSheetName(SpielRundeNr.from(spielrunde)));
				if (sheet == null) {
					continue;
				}
				leseRundeEin(sheet, aktiveMeldungen, statsMap, gegnerMap);
			}
		}

		List<SchweizerTeamErgebnis> ergebnisse = new ArrayList<>();
		for (Team team : aktiveMeldungen.teams()) {
			int[] stats = statsMap.getOrDefault(team.getNr(), new int[3]);
			List<Integer> gegnerNrn = gegnerMap.getOrDefault(team.getNr(), new ArrayList<>());
			ergebnisse.add(new SchweizerTeamErgebnis(team.getNr(), stats[0], stats[1], stats[2], gegnerNrn));
		}
		return ergebnisse;
	}

	private void leseRundeEin(XSpreadsheet sheet, TeamMeldungen aktiveMeldungen, Map<Integer, int[]> statsMap,
			Map<Integer, List<Integer>> gegnerMap) throws GenerateException {
		RangePosition readRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE,
				ERSTE_DATEN_ZEILE + 999);
		RangeData rowsData = RangeHelper
				.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange).getDataFromRange();

		for (RowData row : rowsData) {
			if (row.size() < 2) {
				break;
			}
			int nrA = resolveTeamNr(row.get(0)); // TEAM_A_SPALTE (relativ: 0)
			if (nrA <= 0) {
				break; // Ende der Daten
			}
			Team teamA = aktiveMeldungen.getTeam(nrA);
			if (teamA == null) {
				continue; // Team inaktiv, überspringen
			}

			int nrB = resolveTeamNr(row.get(1)); // TEAM_B_SPALTE (relativ: 1)
			if (nrB <= 0) {
				// Freilos für Team A
				teamA.setHatteFreilos(true);
				statsMap.computeIfAbsent(nrA, k -> new int[3])[0]++;
				continue;
			}
			Team teamB = aktiveMeldungen.getTeam(nrB);
			if (teamB == null) {
				continue;
			}

			teamA.addGegner(teamB); // registriert gegenseitig

			gegnerMap.computeIfAbsent(nrA, k -> new ArrayList<>()).add(nrB);
			gegnerMap.computeIfAbsent(nrB, k -> new ArrayList<>()).add(nrA);

			int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0; // ERG_TEAM_A_SPALTE (relativ: 2)
			int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0; // ERG_TEAM_B_SPALTE (relativ: 3)

			// Punkte+ für beide Teams
			statsMap.computeIfAbsent(nrA, k -> new int[3])[2] += ergA;
			statsMap.computeIfAbsent(nrB, k -> new int[3])[2] += ergB;

			if (ergA > ergB) {
				statsMap.computeIfAbsent(nrA, k -> new int[3])[0]++;
				statsMap.computeIfAbsent(nrA, k -> new int[3])[1] += ergA - ergB;
				statsMap.computeIfAbsent(nrB, k -> new int[3])[1] -= ergA - ergB;
			} else if (ergB > ergA) {
				statsMap.computeIfAbsent(nrB, k -> new int[3])[0]++;
				statsMap.computeIfAbsent(nrB, k -> new int[3])[1] += ergB - ergA;
				statsMap.computeIfAbsent(nrA, k -> new int[3])[1] -= ergB - ergA;
			}
		}
	}

	/**
	 * Löst eine Team-Nr aus einer Zelle auf.
	 * Versucht zunächst den Integer-Wert, dann Name-Lookup über die Meldeliste.
	 */
	private int resolveTeamNr(CellData cell) throws GenerateException {
		int nr = cell.getIntVal(0);
		if (nr > 0) {
			return nr;
		}
		String name = cell.getStringVal();
		if (name != null && !name.isEmpty()) {
			return getMeldeListe().getTeamNrByTeamname(name);
		}
		return 0;
	}

	/**
	 * Sortiert die aktiven Teams nach Ranglisten-Kriterien (Schweizer System)
	 * und gibt eine neue TeamMeldungen-Liste in dieser Reihenfolge zurück.
	 */
	protected TeamMeldungen sortierteTeamMeldungen(TeamMeldungen aktiveMeldungen,
			List<SchweizerTeamErgebnis> ergebnisse) throws GenerateException {
		SchweizerRankingModus modus = getKonfigurationSheet().getRankingModus();
		SchweizerSystem sortierer = new SchweizerSystem();
		List<SchweizerTeamErgebnis> sortiert = sortierer.sortiereNachAuswertungskriterien(ergebnisse, modus);

		TeamMeldungen sortierteMeldungen = new TeamMeldungen();
		for (SchweizerTeamErgebnis erg : sortiert) {
			Team team = aktiveMeldungen.getTeam(erg.teamNr());
			if (team != null) {
				sortierteMeldungen.addTeamWennNichtVorhanden(team);
			}
		}
		return sortierteMeldungen;
	}

	/**
	 * enweder einfach ein laufende nummer, oder jenachdem was in der konfig steht die Spielbahnnummer<br>
	 * property getSpielrundeSpielbahn<br>
	 * X = nur ein laufende paarungen nummer<br>
	 * L = Spielbahn -> leere Spalte<br>
	 * N = Spielbahn -> durchnumeriert<br>
	 * R = Spielbahn -> random<br>
	 *
	 * @throws GenerateException
	 */
	private void datenErsteSpalte() throws GenerateException {
		Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();
		Integer letzteZeile = letztePositionRechtsUnten().getZeile();
		SpielrundeSpielbahn spielrundeSpielbahn = getKonfigurationSheet().getSpielrundeSpielbahn();

		spielrundeHelper.datenErsteSpalte(spielrundeSpielbahn, ERSTE_DATEN_ZEILE, letzteZeile, BAHN_NR_SPALTE,
				ERSTE_HEADER_ZEILE, ZWEITE_HEADER_ZEILE, headerColor);
	}

	private void header() throws GenerateException {
		processBoxinfo("processbox.formatiere.header");
		Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();
		boolean nameMode = getKonfigurationSheet().getSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NAME;

		Position headerStart = Position.from(TEAM_A_SPALTE, ERSTE_HEADER_ZEILE);

		StringCellValue headerValue = StringCellValue.from(getXSpreadSheet(), headerStart)
				.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor)
				.setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true).setEndPosMergeSpaltePlus(3)
				.setValue(I18n.get("schweizer.spielrunde.header.spielrunde", getSpielRundeNr().getNr()));
		getSheetHelper().setStringValueInCell(headerValue);

		int zeile2CharHeight = nameMode ? 12 : NR_CHARHEIGHT;
		String labelA = nameMode ? I18n.get("schweizer.spielrunde.spalte.mannschaft.a") : "A";
		String labelB = nameMode ? I18n.get("schweizer.spielrunde.spalte.mannschaft.b") : "B";

		StringCellValue headerValueZeile2 = StringCellValue
				.from(getXSpreadSheet(), headerStart.zeile(ZWEITE_HEADER_ZEILE)).setVertJustify(CellVertJustify2.CENTER)
				.setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder()).setCellBackColor(headerColor)
				.setCharHeight(zeile2CharHeight).setShrinkToFit(true);

		headerValueZeile2.setValue(labelA);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		headerValueZeile2.setValue(labelB).spaltePlus(1);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		headerValueZeile2.setValue(I18n.get("schweizer.spielrunde.spalte.ergebnis")).spaltePlus(1).setEndPosMergeSpaltePlus(1);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		rundenStartzeitFeld();
	}

	/**
	 * Schreibt (nur wenn {@code isZeitplanAktiv()}) das einzige haendisch editierbare
	 * Zeit-Eingabefeld dieses Features in die freien Header-Zellen von {@link #FEHLER_SPALTE}
	 * (dort steht sonst nichts, siehe {@link #fehlerSpalteFormatieren()}). Runde 1 nutzt die
	 * zentrale Turnier-Startzeit als Default, alle Folgerunden verketten sich per
	 * Cross-Sheet-Zellbezug auf die Startzeit der Vorrunde plus deren Gesamtdauer plus
	 * Rundenpause. Alle Werte werden live per {@code PTM.ALG.INTPROPERTY}/{@code STRINGPROPERTY}
	 * referenziert, damit Aenderungen an der Konfiguration ohne Sheet-Neuaufbau wirken.
	 */
	private void rundenStartzeitFeld() throws GenerateException {
		if (!getKonfigurationSheet().isZeitplanAktiv()) {
			return;
		}
		Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();

		StringCellValue labelValue = StringCellValue.from(getXSpreadSheet(), Position.from(FEHLER_SPALTE, ERSTE_HEADER_ZEILE))
				.setVertJustify(CellVertJustify2.BOTTOM).setHoriJustify(CellHoriJustify.CENTER)
				.setCellBackColor(headerColor).setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
				.setValue(I18n.get("schweizer.spielrunde.start.label"));
		getSheetHelper().setStringValueInCell(labelValue);

		Position startzeitPos = Position.from(FEHLER_SPALTE, ZWEITE_HEADER_ZEILE);
		if (getSpielRundeNr().getNr() <= 1) {
			// Runde 1: einmaliger literaler Default aus der Turnier-Startzeit (kein Formelbezug) —
			// konsistent mit der Persistenz-Regel (das Sheet-Feld selbst ist die fuehrende Quelle,
			// keine retroaktive Verschiebung wenn die zentrale Turnier-Startzeit spaeter geaendert wird).
			double bruchteilDesTages = zeitStringZuTagesBruchteil(getKonfigurationSheet().getZeitplanTurnierStartzeit());
			NumberCellValue startzeitValue = NumberCellValue.from(getXSpreadSheet(), startzeitPos)
					.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
					.setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true)
					.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
					.setValue(bruchteilDesTages);
			getSheetHelper().setNumberValueInCell(startzeitValue);
		} else {
			StringCellValue startzeitValue = StringCellValue.from(getXSpreadSheet(), startzeitPos, rundenStartzeitFormel())
					.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
					.setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true)
					.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
			getSheetHelper().setFormulaInCell(startzeitValue);
		}

		RangePosition startzeitRange = RangePosition.from(startzeitPos, startzeitPos);
		RangeHelper.from(this, startzeitRange).setRangeProperties(RangeProperties.from().numberFormat(UserNumberFormat.TIME));
		EditierbaresZelleFormatHelper.anwenden(this, startzeitRange);
	}

	/** "09:00" -&gt; 0.375 (Bruchteil des Tages, Calc-interne Zeit-Repraesentation). Ungueltige Eingabe -&gt; 0.0 (Mitternacht). */
	private static double zeitStringZuTagesBruchteil(String hhMm) {
		try {
			String[] teile = hhMm.split(":");
			int stunden = Integer.parseInt(teile[0].trim());
			int minuten = teile.length > 1 ? Integer.parseInt(teile[1].trim()) : 0;
			return (stunden * 60 + minuten) / 1440.0;
		} catch (RuntimeException e) {
			return 0.0;
		}
	}

	/**
	 * Nur fuer Runde N&gt;1: Cross-Sheet-Bezug auf die Rundenstartzeit-Zelle der Vorrunde plus deren
	 * Gesamtdauer (Anzahl Durchgaenge der Vorrunde ist ein struktureller Wert, ausgelesen aus der
	 * bereits geschriebenen Vorrunden-Struktur, siehe {@link #ermittleAnzahlDurchgaengeVorrunde}),
	 * plus Rundenpause.
	 * <p>
	 * Der Sheet-Name im Formel-Bezug wird ueber {@link SheetMetadataHelper#findeSheetUndHeile}
	 * aufgeloest (Metadaten-first, ueberlebt Umbenennung), nicht ueber den lokalisierten
	 * Default-Namen {@link #getSheetName} — sonst entsteht ein {@code #REF!}, sobald die Vorrunde
	 * umbenannt wurde, obwohl die Metadaten-Suche das richtige Sheet findet.
	 */
	private String rundenStartzeitFormel() throws GenerateException {
		SpielRundeNr aktuelleRunde = getSpielRundeNr();
		SpielRundeNr vorherigeRunde = SpielRundeNr.from(aktuelleRunde.getNr() - 1);
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		XSpreadsheet vorSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
				getSpielrundeSchluessel(vorherigeRunde.getNr()), getSheetName(vorherigeRunde));
		// Vorrunde muesste eigentlich existieren (Runde N>1 setzt Runde N-1 voraus); defensiv
		// trotzdem auf den Default-Namen zurueckfallen statt eine NPE zu riskieren.
		String vorSheetName = vorSheet != null ? Lo.qi(XNamed.class, vorSheet).getName() : getSheetName(vorherigeRunde);
		int anzDurchgaengeVorrunde = ermittleAnzahlDurchgaengeVorrunde(vorSheet, xDoc);

		String vorZelle = "$'" + vorSheetName + "'." + Position.from(FEHLER_SPALTE, ZWEITE_HEADER_ZEILE).getAddressWith$();
		String zeitlimit = GlobalImpl.FORMAT_PTM_INT_PROPERTY(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN);
		String durchgangPause = GlobalImpl
				.FORMAT_PTM_INT_PROPERTY(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN);
		String rundenPause = GlobalImpl.FORMAT_PTM_INT_PROPERTY(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN);

		String dauerMinuten = anzDurchgaengeVorrunde + "*" + zeitlimit + "+" + (anzDurchgaengeVorrunde - 1) + "*"
				+ durchgangPause + "+" + rundenPause;
		return vorZelle + "+TIME(0;" + dauerMinuten + ";0)";
	}

	/**
	 * Liest die Anzahl der Durchgaenge der Vorrunde aus deren bereits geschriebener
	 * {@link #DURCHGANG_LABEL_SPALTE} aus (struktureller Wert zum Zeitpunkt der Vorrunden-Erzeugung,
	 * nicht aus der aktuellen Konfiguration neu hergeleitet — vermeidet Drift bei zwischenzeitlich
	 * geaenderter Bahnenzahl). 1, wenn die Vorrunde nicht existiert oder nicht aufgeteilt war.
	 */
	private int ermittleAnzahlDurchgaengeVorrunde(XSpreadsheet vorSheet, XSpreadsheetDocument xDoc) throws GenerateException {
		if (vorSheet == null) {
			return 1;
		}
		RangePosition labelRange = RangePosition.from(DURCHGANG_LABEL_SPALTE, ERSTE_DATEN_ZEILE, DURCHGANG_LABEL_SPALTE,
				ERSTE_DATEN_ZEILE + 999);
		RangeData labelDaten = RangeHelper.from(vorSheet, xDoc, labelRange).getDataFromRange();
		int anzahl = 0;
		for (RowData row : labelDaten) {
			String val = row.get(0).getStringVal();
			if (val != null && !val.isEmpty()) {
				anzahl++;
			}
		}
		return anzahl > 0 ? anzahl : 1;
	}

	/**
	 * spalten Teampaarungen + Ergebnis
	 * 
	 * @throws GenerateException
	 */

	private void datenformatieren() throws GenerateException {
		processBoxinfo("processbox.formatiere.daten");

		XSpreadsheet sheet = getXSpreadSheet();
		Position datenStart = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position datenEnd = letztePositionRechtsUnten();

		// komplett mit normal gitter
		RangePosition datenRangeInclErg = RangePosition.from(datenStart, datenEnd);
		TableBorder2 border = BorderFactory.from().allThin().toBorder();
		getSheetHelper().setPropertyInRange(sheet, datenRangeInclErg, TABLE_BORDER2, border);

		SpielrundeHintergrundFarbeGeradeStyle geradeColor = getKonfigurationSheet()
				.getSpielRundeHintergrundFarbeGeradeStyle();
		SpielrundeHintergrundFarbeUnGeradeStyle unGeradeColor = getKonfigurationSheet()
				.getSpielRundeHintergrundFarbeUnGeradeStyle();

		// Zebra-Farbe: Team-A- und Team-B-Spalten
		RangePosition datenRangeSpielpaarungen = RangePosition.from(datenRangeInclErg).endeSpalte(TEAM_B_SPALTE);
		spielrundeHelper.formatiereGeradeUngradeSpielpaarungen(this, datenRangeSpielpaarungen, geradeColor,
				unGeradeColor);

		// Alle Spalten (A, B, Erg) zentrieren
		getSheetHelper().setPropertyInRange(sheet, datenRangeInclErg, HORI_JUSTIFY, CellHoriJustify.CENTER);
		getSheetHelper().setPropertyInRange(sheet, datenRangeInclErg, VERT_JUSTIFY, CellVertJustify2.CENTER);

		RangePosition abSpalten = RangePosition.from(datenRangeInclErg).endeSpalte(TEAM_B_SPALTE);
		if (getKonfigurationSheet().getSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NAME) {
			// Teamname-Modus: Font 12, an Zellgröße anpassen, Spaltenbreite 6 cm
			getSheetHelper().setPropertyInRange(sheet, abSpalten, CHAR_HEIGHT, 12);
			getSheetHelper().setPropertyInRange(sheet, abSpalten, SHRINK_TO_FIT, Boolean.TRUE);
			getSheetHelper().setColumnWidth(sheet, TEAM_A_SPALTE, 6000); // 6 cm = 6000 (1/100 mm)
			getSheetHelper().setColumnWidth(sheet, TEAM_B_SPALTE, 6000);
		} else {
			// Teamnummer-Modus: große Schrift für A/B-Spalten
			getSheetHelper().setPropertyInRange(sheet, abSpalten, CHAR_HEIGHT, 32);
			getSheetHelper().setPropertyInRange(sheet, abSpalten, CHAR_WEIGHT, FontWeight.BOLD);
		}

		// Ergebnis-Spalten: Zebra + Validierung 0–13 (wie Supermelee)
		datenEnd = letztePositionRechtsUnten(); // neu einlesen
		RangePosition ergebnisRange = RangePosition.from(
				Position.from(ERG_TEAM_A_SPALTE, ERSTE_DATEN_ZEILE), Position.from(datenEnd));

		getSheetHelper().setPropertyInRange(sheet, ergebnisRange, CHAR_HEIGHT, 32);
		getSheetHelper().setPropertyInRange(sheet, ergebnisRange, CHAR_WEIGHT, FontWeight.BOLD);

		spielrundeHelper.formatiereErgebnissRange(this, ergebnisRange, ERG_TEAM_A_SPALTE);

		// Editierbare Felder hervorheben: Ergebnis-Spalten A und B
		if (datenEnd != null) {
			RangePosition ergebnisEditierbarRange = RangePosition.from(
					ERG_TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE, datenEnd.getZeile());
			EditierbaresZelleFormatHelper.anwenden(this, ergebnisEditierbarRange);
		}
	}

	/**
	 * Setzt den Druckbereich über die gesamte Tabelle
	 * (von BAHN_NR_SPALTE/ERSTE_HEADER_ZEILE bis ERG_TEAM_B_SPALTE/letzte Datenzeile, ohne Fehler-Spalte).
	 */
	private void druckBereichSetzen() throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		Position letztePos = letztePositionRechtsUnten();
		if (letztePos == null) {
			return;
		}
		// Bei aktiver Zeitplanung wird der Druckbereich bis DURCHGANG_STARTZEIT_SPALTE erweitert
		// (schliesst dann bewusst auch FEHLER_SPALTE mit ein — akzeptabler Trade-off, siehe Plan;
		// TabellenMapper bildet nur die Bounding-Box mehrerer Druckbereiche, eine Luecke waere wirkungslos).
		int letzteDruckSpalte = getKonfigurationSheet().isZeitplanAktiv() ? DURCHGANG_STARTZEIT_SPALTE : ERG_TEAM_B_SPALTE;
		RangePosition druckBereich = RangePosition.from(BAHN_NR_SPALTE, ERSTE_HEADER_ZEILE,
				Position.from(letzteDruckSpalte, letztePos.getZeile()));
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(druckBereich);
		SpielrundeFooterHelper.schreibeFooterUndErweitereDruckbereich(this, getXSpreadSheet(), getWorkingSpreadsheet());
	}

	/**
	 * Setzt Trennlinien:
	 * <ul>
	 *   <li>Doppelte Linie rechts von BAHN_NR_SPALTE (erste Spalte)</li>
	 *   <li>Doppelte Linie rechts von TEAM_B_SPALTE (Trennlinie B/Ergebnis)</li>
	 * </ul>
	 * Muss nach allen anderen Formatierungen aufgerufen werden.
	 */
	private void trennlinienSetzen() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		Position letztePos = letztePositionRechtsUnten();
		if (letztePos == null) {
			return;
		}
		int letzteZeile = letztePos.getZeile();

		// Doppelte Linie rechts von BAHN_NR_SPALTE
		RangePosition bahnNrRange = RangePosition.from(BAHN_NR_SPALTE, ERSTE_HEADER_ZEILE,
				BAHN_NR_SPALTE, letzteZeile);
		getSheetHelper().setPropertyInRange(sheet, bahnNrRange, TABLE_BORDER2,
				BorderFactory.from().allThin().doubleLn().forRight().toBorder());

		// Doppelte Trennlinie rechts von TEAM_B_SPALTE (zwischen B und Ergebnis)
		RangePosition teamBRange = RangePosition.from(TEAM_B_SPALTE, ERSTE_HEADER_ZEILE,
				TEAM_B_SPALTE, letzteZeile);
		getSheetHelper().setPropertyInRange(sheet, teamBRange, TABLE_BORDER2,
				BorderFactory.from().allThin().doubleLn().forRight().toBorder());

		// Dicke untere Linie für die gesamte zweite Header-Zeile (ohne Fehler-Spalte)
		// Nur BottomLine setzen (IsBottomLineValid=true), andere Borders bleiben erhalten
		RangePosition headerUntenRange = RangePosition.from(BAHN_NR_SPALTE, ZWEITE_HEADER_ZEILE,
				ERG_TEAM_B_SPALTE, ZWEITE_HEADER_ZEILE);
		getSheetHelper().setPropertyInRange(sheet, headerUntenRange, TABLE_BORDER2,
				BorderFactory.from().boldLn().forBottom().toBorder());
	}

	/**
	 * Fügt rechts neben den Ergebnisspalten eine Fehler-Spalte ein.
	 * <p>
	 * Pro Datenzeile wird eine Formel eingetragen, die „FEHLER" anzeigt, wenn:
	 * <ul>
	 *   <li>ein Ergebnis außerhalb 0–13 liegt</li>
	 *   <li>beide Ergebnisse gleich sind (Unentschieden nicht erlaubt)</li>
	 *   <li>genau eine der beiden Zellen leer ist</li>
	 * </ul>
	 */
	private void fehlerSpalteFormatieren() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		Position letztePos = letztePositionRechtsUnten();
		if (letztePos == null) {
			return;
		}
		int letzteZeile = letztePos.getZeile();

		getSheetHelper().setColumnWidth(sheet, Position.from(FEHLER_SPALTE, ERSTE_HEADER_ZEILE), 1800);

		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String ergA = Position.from(ERG_TEAM_A_SPALTE, zeile).getAddress();
			String ergB = Position.from(ERG_TEAM_B_SPALTE, zeile).getAddress();

			// @formatter:off
			String formel = "IF(OR(" +
					"AND(ISBLANK(" + ergA + ");ISBLANK(" + ergB + "));" +
					"AND(" + ergA + "<14;" + ergB + "<14;" + ergA + ">-1;" + ergB + ">-1;" + ergA + "<>" + ergB + ")" +
					");\"\";\"" + I18n.get("schweizer.spielrunde.fehler.formel") + "\")";
			// @formatter:on

			StringCellValue cv = StringCellValue
					.from(sheet, Position.from(FEHLER_SPALTE, zeile), formel)
					.setCharColor(ColorHelper.CHAR_COLOR_RED)
					.setCharWeight(FontWeight.BOLD)
					.setCharHeight(14)
					.setHoriJustify(CellHoriJustify.CENTER);
			getSheetHelper().setFormulaInCell(cv);
		}
	}

	/**
	 * Spalte SpielerNR A verwenden um die letzte zeile zu ermitteln<br>
	 * Spalte ist dann ergebniss Team B
	 * 
	 * @return
	 * @throws GenerateException
	 */

	public Position letztePositionRechtsUnten() throws GenerateException {
		Position spielerNrPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);

		RangePosition erstSpielrNrRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, TEAM_A_SPALTE,
				ERSTE_DATEN_ZEILE + 999);

		// alle Daten einlesen (String oder Integer je nach Spielplan-Anzeige-Modus)
		RangeData nrDaten = RangeHelper.from(this, erstSpielrNrRange).getDataFromRange();

		// erste leere Zelle (leer = weder Int-Wert noch String-Inhalt)
		int index = IntStream.range(0, nrDaten.size()).filter(nrDatenIdx -> {
			String val = nrDaten.get(nrDatenIdx).get(0).getStringVal();
			return val == null || val.isEmpty();
		}).findFirst().orElse(-1);

		if (index == 0) {
			return null; // Keine Daten
		}
		if (index > 0) {
			spielerNrPos.zeilePlus(index - 1);
		}

		return spielerNrPos.spalte(ERG_TEAM_B_SPALTE);
	}

	protected boolean neueSpielrunde(TeamMeldungen meldungen, SpielRundeNr neueSpielrundeNr,
			List<SchweizerTeamErgebnis> ergebnisse) throws GenerateException {
		return neueSpielrunde(meldungen, neueSpielrundeNr, ergebnisse, isForceOk());
	}

	protected boolean neueSpielrunde(TeamMeldungen meldungen, SpielRundeNr neueSpielrundeNr,
			List<SchweizerTeamErgebnis> ergebnisse, boolean force) throws GenerateException {
		checkNotNull(meldungen);

		processBoxinfo("processbox.neue.spielrunde", neueSpielrundeNr.getNr());
		processBoxinfo("processbox.anzahl.meldungen", meldungen.size());

		// wenn hier dann neu erstellen
		if (!NewSheet.from(this, getSheetName(neueSpielrundeNr), getSpielrundeSchluessel(neueSpielrundeNr.getNr()))
				.pos(DefaultSheetPos.SCHWEIZER_WORK).setForceCreate(force).setActiv().hideGrid()
				.tabColor(getKonfigurationSheet().getSpielrundeTabFarbe()).create().isDidCreate()) {
			ProcessBox.from().info(I18n.get("schweizer.spielrunde.abbruch"));
			return false;
		}

		// neue Spielrunde speichern, sheet vorhanden
		getKonfigurationSheet().setAktiveSpielRunde(neueSpielrundeNr);

		SchweizerSystem schweizerSystem = new SchweizerSystem();

		List<TeamPaarung> paarungen;

		if (neueSpielrundeNr.getNr() == 1) {
			paarungen = schweizerSystem.ersteRunde(meldungen.teams());
		} else {
			paarungen = schweizerSystem.weitereRunde(meldungen.teams(), ergebnisse);
		}

		teamPaarungenEinfuegen(paarungen);
		datenErsteSpalte(); // BahnNr
		bahnNummerierungProDurchgangFallsAktiv(paarungen.size()); // ueberschreibt BahnNr pro Durchgang neu ab 1
		datenformatieren();
		fehlerSpalteFormatieren();
		header();
		trennlinienSetzen();
		druckBereichSetzen();
		SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();

		return true;
	}

	/**
	 * Daten <br>
	 * kein hintergrund
	 * 
	 * @param paarungen
	 * @throws GenerateException
	 */

	private void teamPaarungenEinfuegen(List<TeamPaarung> paarungen) throws GenerateException {
		if (paarungen == null) {
			return;
		}

		boolean useTeamname = getKonfigurationSheet().getSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NAME;
		int freispielPlus = getKonfigurationSheet().getFreispielPunktePlus();
		int freispielMinus = getKonfigurationSheet().getFreispielPunkteMinus();
		RangeData rangeData = new RangeData();

		for (TeamPaarung teamPaarung : paarungen) {
			SheetRunner.testDoCancelTask();
			if (!teamPaarung.hasB()) {
				// Freilos – Team A ohne Gegner eintragen, ERG mit Freispiel-Werten vorbelegen
				RowData freilosRow = rangeData.addNewRow();
				if (useTeamname) {
					freilosRow.add(new CellData("")); // Name folgt per Formel (teamNamenFormelnSchreiben)
				} else {
					freilosRow.add(new CellData(teamPaarung.getA().getNr()));
				}
				freilosRow.add(new CellData("")); // kein Gegner = Freilos
				freilosRow.add(new CellData(freispielPlus)); // ERG_TEAM_A vorbelegen
				freilosRow.add(new CellData(freispielMinus)); // ERG_TEAM_B vorbelegen
				continue;
			}
			if (useTeamname) {
				RowData row = rangeData.addNewRow();
				row.add(new CellData("")); // Name folgt per Formel (teamNamenFormelnSchreiben)
				row.add(new CellData(""));
			} else {
				rangeData.addNewRow(teamPaarung.getA().getNr(), teamPaarung.getB().getNr());
			}
		}

		Position startPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);

		if (useTeamname) {
			teamNamenFormelnSchreiben(paarungen);
		}

		durchgangInfoSpaltenSchreiben(paarungen.size());
	}

	/**
	 * Schreibt (nur wenn {@code isDurchgangAufteilungWirksam()} und tatsaechlich mehr als ein
	 * Durchgang noetig ist) Label und live berechnete Startzeit je Durchgang in
	 * {@link #DURCHGANG_LABEL_SPALTE}/{@link #DURCHGANG_STARTZEIT_SPALTE} — jeweils nur auf der
	 * ersten Datenzeile eines neuen Durchgang-Blocks. Der Team-A/B/Erg-A/Erg-B-Datenstrom bleibt
	 * dadurch vollstaendig unangetastet (siehe {@code leseRundeEin()}).
	 */
	private void durchgangInfoSpaltenSchreiben(int anzahlPaarungen) throws GenerateException {
		if (!getKonfigurationSheet().isDurchgangAufteilungWirksam() || anzahlPaarungen <= 0) {
			return;
		}
		int bahnen = getKonfigurationSheet().getZeitplanAnzahlBahnen();
		List<Integer> bloecke = DurchgangAufteilungRechner.berechne(anzahlPaarungen, bahnen);
		if (bloecke.size() <= 1) {
			return; // Paarungen passen in einen Durchgang, keine Aufteilung noetig
		}

		String rundenStartzeitAdresse = Position.from(FEHLER_SPALTE, ZWEITE_HEADER_ZEILE).getAddressWith$();
		String zeitlimit = GlobalImpl.FORMAT_PTM_INT_PROPERTY(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN);
		String pause = GlobalImpl.FORMAT_PTM_INT_PROPERTY(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN);
		var oberrand = BorderFactory.from().allThin().boldLn().forTop().toBorder();

		int zeile = ERSTE_DATEN_ZEILE;
		int durchgangNr = 1;
		for (int groesse : bloecke) {
			SheetRunner.testDoCancelTask();

			StringCellValue labelValue = StringCellValue.from(getXSpreadSheet(), Position.from(DURCHGANG_LABEL_SPALTE, zeile))
					.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
					.setCharWeight(FontWeight.BOLD).setCharHeight(12).setBorder(oberrand)
					.setValue(I18n.get("schweizer.spielrunde.durchgang.label", durchgangNr));
			getSheetHelper().setStringValueInCell(labelValue);

			String formel = durchgangNr == 1 ? rundenStartzeitAdresse
					: rundenStartzeitAdresse + "+TIME(0;(" + (durchgangNr - 1) + ")*(" + zeitlimit + "+" + pause + ");0)";
			Position startzeitPos = Position.from(DURCHGANG_STARTZEIT_SPALTE, zeile);
			StringCellValue startzeitValue = StringCellValue.from(getXSpreadSheet(), startzeitPos, formel)
					.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
					.setCharHeight(12).setBorder(oberrand);
			getSheetHelper().setFormulaInCell(startzeitValue);

			RangePosition startzeitRange = RangePosition.from(startzeitPos, startzeitPos);
			RangeHelper.from(this, startzeitRange).setRangeProperties(RangeProperties.from().numberFormat(UserNumberFormat.TIME));

			zeile += groesse;
			durchgangNr++;
		}
	}

	/**
	 * Muss NACH {@link #datenErsteSpalte()} aufgerufen werden, da {@link #bahnNummerierungProDurchgang}
	 * dessen fortlaufende 1..Anzahl-Paarungen-Nummerierung gezielt ueberschreibt.
	 */
	private void bahnNummerierungProDurchgangFallsAktiv(int anzahlPaarungen) throws GenerateException {
		if (!getKonfigurationSheet().isDurchgangAufteilungWirksam() || anzahlPaarungen <= 0) {
			return;
		}
		List<Integer> bloecke = DurchgangAufteilungRechner.berechne(anzahlPaarungen,
				getKonfigurationSheet().getZeitplanAnzahlBahnen());
		if (bloecke.size() <= 1) {
			return;
		}
		bahnNummerierungProDurchgang(bloecke);
	}

	/**
	 * Bei aktiver Durchgang-Aufteilung beginnt die Bahn-Nummer in {@link #BAHN_NR_SPALTE} pro
	 * Durchgang-Block neu bei 1 (physische Bahnen werden zwischen Durchgaengen wiederverwendet).
	 * Ueberschreibt gezielt nur die Datenzeilen, die {@link #datenErsteSpalte()} zuvor im
	 * Standard-Modus (fortlaufend 1..Anzahl Paarungen) geschrieben hat. Nur fuer die
	 * "nummerierten" {@link SpielrundeSpielbahn}-Modi X/N sinnvoll — L (haendisch) und
	 * R (zufaellig) bleiben unveraendert, siehe Review-Punkt 2: diese Property ist unabhaengig
	 * von {@link SpielrundeSpielbahn} und veraendert dessen bestehende Semantik nicht.
	 */
	private void bahnNummerierungProDurchgang(List<Integer> bloecke) throws GenerateException {
		SpielrundeSpielbahn modus = getKonfigurationSheet().getSpielrundeSpielbahn();
		if (modus != SpielrundeSpielbahn.X && modus != SpielrundeSpielbahn.N) {
			return;
		}
		RangeData rangeData = new RangeData();
		for (int groesse : bloecke) {
			for (int n = 1; n <= groesse; n++) {
				rangeData.addNewRow(n);
			}
		}
		Position startPos = Position.from(BAHN_NR_SPALTE, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	/**
	 * Schreibt die Team-Namen im Anzeigemodus {@link SpielplanTeamAnzeige#NAME} als
	 * SVERWEIS-Formel (statt statischem Text), damit eine spätere Umbenennung in der
	 * Meldeliste im Spielplan sofort sichtbar bleibt (siehe auch {@link #resolveTeamNr}).
	 */
	private void teamNamenFormelnSchreiben(List<TeamPaarung> paarungen) throws GenerateException {
		if (paarungen.isEmpty()) {
			return;
		}
		boolean teamnameAnzeigen = getKonfigurationSheet().isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAnzeigen = getKonfigurationSheet().isMeldeListeVereinsnameAnzeigen();
		Formation formation = getKonfigurationSheet().getMeldeListeFormation();

		String[][] formulas = new String[paarungen.size()][2];
		for (int i = 0; i < paarungen.size(); i++) {
			TeamPaarung teamPaarung = paarungen.get(i);
			formulas[i][0] = MeldeListeHelper.teamNameFormel(String.valueOf(teamPaarung.getA().getNr()),
					teamnameAnzeigen, formation, vereinsnameAnzeigen);
			formulas[i][1] = teamPaarung.hasB()
					? MeldeListeHelper.teamNameFormel(String.valueOf(teamPaarung.getB().getNr()), teamnameAnzeigen,
							formation, vereinsnameAnzeigen)
					: "";
		}

		RangePosition teamRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, TEAM_B_SPALTE,
				ERSTE_DATEN_ZEILE + paarungen.size() - 1);
		getSheetHelper().setFormulaArrayInRange(getXSpreadSheet(), teamRange, formulas);

		// Die Runden-Erzeugung läuft mit deaktivierter Automatikberechnung (Performance);
		// ohne expliziten Rechenlauf blieben die neuen Formelzellen bis zum nächsten
		// Nutzer-Trigger auf 0 stehen.
		getxCalculatable().calculateAll();
	}

	/**
	 * fuer Test
	 * 
	 * @return
	 */
	public boolean isForceOk() {
		return forceOk;
	}

	/**
	 * fuer Test
	 * 
	 * @return
	 */
	public void setForceOk(boolean forceOk) {
		this.forceOk = forceOk;
	}

}
