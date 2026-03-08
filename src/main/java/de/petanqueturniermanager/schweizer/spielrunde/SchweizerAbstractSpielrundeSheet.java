package de.petanqueturniermanager.schweizer.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.SchweizerSystem;
import de.petanqueturniermanager.algorithmen.SchweizerTeamErgebnis;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;
import de.petanqueturniermanager.schweizer.meldeliste.AbstractSchweizerMeldeListeSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Erstellung 27.03.2024 / Michael Massee
 */

public abstract class SchweizerAbstractSpielrundeSheet extends SchweizerSheet implements ISheet {

	private static final Logger LOGGER = LogManager.getLogger(SchweizerAbstractSpielrundeSheet.class);

	public static final String SHEET_COLOR = "b0f442";
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

	private final AbstractSchweizerMeldeListeSheet meldeListe;
	private final SpielrundeHelper spielrundeHelper;
	private SpielRundeNr spielRundeNrInSheet = null;
	private boolean forceOk = false; // wird fuer Test verwendet

	//	private SpielRundeNr sheetSpielRundeNr = null; // muss nicht der Aktive sein

	protected SchweizerAbstractSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, SHEET_NAMEN);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
		spielrundeHelper = new SpielrundeHelper(this, NR_CHARHEIGHT, NR_CHARHEIGHT, true,
				getKonfigurationSheet().getSpielRundeHintergrundFarbeGeradeStyle(),
				getKonfigurationSheet().getSpielRundeHintergrundFarbeUnGeradeStyle());
	}

	protected final boolean canStart(TeamMeldungen meldungen) throws GenerateException {
		if (getSpielRundeNr().getNr() < 1) {
			getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());

			String errorMsg = "Ungültige Spielrunde in der Meldeliste '" + getSpielRundeNr().getNr() + "'";
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Aktuelle Spielrunde Fehler")
					.message(errorMsg).show();
			return false;
		}

		if (meldungen.size() < 6) {
			getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			String errorMsg = "Ungültige Anzahl '" + meldungen.size() + "' von Aktive Meldungen vorhanden."
					+ "\r\nmindestens 6 Meldungen aktivieren.";
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Aktuelle Spielrunde Fehler")
					.message(errorMsg).show();
			return false;
		}
		return true;
	}

	@VisibleForTesting
	AbstractSchweizerMeldeListeSheet initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	public final XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(getSheetName(getSpielRundeNr()));
	}

	public final String getSheetName(SpielRundeNr nr) {
		return nr.getNr() + ". " + SHEET_NAMEN;
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

	public AbstractSchweizerMeldeListeSheet getMeldeListe() {
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

		Map<Integer, int[]> statsMap = new HashMap<>(); // teamNr → [0]=siege, [1]=punktediff
		Map<Integer, List<Integer>> gegnerMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new int[2]);
			gegnerMap.put(team.getNr(), new ArrayList<>());
		}

		if (bisSpielrunde >= abSpielrunde && bisSpielrunde >= 1) {
			int spielrunde = (abSpielrunde > 1) ? abSpielrunde : 1;
			processBoxinfo("Gespielte Runden einlesen. Von Runde " + spielrunde + " bis " + bisSpielrunde);

			for (; spielrunde <= bisSpielrunde; spielrunde++) {
				SheetRunner.testDoCancelTask();
				XSpreadsheet sheet = getSheetHelper().findByName(getSheetName(SpielRundeNr.from(spielrunde)));
				if (sheet == null) {
					continue;
				}
				leseRundeEin(sheet, aktiveMeldungen, statsMap, gegnerMap);
			}
		}

		List<SchweizerTeamErgebnis> ergebnisse = new ArrayList<>();
		for (Team team : aktiveMeldungen.teams()) {
			int[] stats = statsMap.getOrDefault(team.getNr(), new int[2]);
			List<Integer> gegnerNrn = gegnerMap.getOrDefault(team.getNr(), new ArrayList<>());
			ergebnisse.add(new SchweizerTeamErgebnis(team.getNr(), stats[0], stats[1], gegnerNrn));
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
				statsMap.computeIfAbsent(nrA, k -> new int[2])[0]++;
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

			if (ergA > ergB) {
				statsMap.computeIfAbsent(nrA, k -> new int[2])[0]++;
				statsMap.computeIfAbsent(nrA, k -> new int[2])[1] += ergA - ergB;
				statsMap.computeIfAbsent(nrB, k -> new int[2])[1] -= ergA - ergB;
			} else if (ergB > ergA) {
				statsMap.computeIfAbsent(nrB, k -> new int[2])[0]++;
				statsMap.computeIfAbsent(nrB, k -> new int[2])[1] += ergB - ergA;
				statsMap.computeIfAbsent(nrA, k -> new int[2])[1] -= ergB - ergA;
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
			List<SchweizerTeamErgebnis> ergebnisse) {
		SchweizerSystem sortierer = new SchweizerSystem();
		List<SchweizerTeamErgebnis> sortiert = sortierer.sortiereNachAuswertungskriterien(ergebnisse);

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
		processBoxinfo("Header Formatieren");
		Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();

		Position headerStart = Position.from(TEAM_A_SPALTE, ERSTE_HEADER_ZEILE);

		StringCellValue headerValue = StringCellValue.from(getXSpreadSheet(), headerStart)
				.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor)
				.setCharHeight(NR_CHARHEIGHT).setEndPosMergeSpaltePlus(4)
				.setValue("Spielrunde " + getSpielRundeNr().getNr());
		getSheetHelper().setStringValueInCell(headerValue);

		StringCellValue headerValueZeile2 = StringCellValue
				.from(getXSpreadSheet(), headerStart.zeile(ZWEITE_HEADER_ZEILE)).setVertJustify(CellVertJustify2.CENTER)
				.setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder()).setCellBackColor(headerColor)
				.setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true);

		headerValueZeile2.setValue("A");
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		headerValueZeile2.setValue("B").spaltePlus(1);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		headerValueZeile2.setValue("Ergebnis").spaltePlus(1).setEndPosMergeSpaltePlus(1);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

	}

	/**
	 * spalten Teampaarungen + Ergebnis
	 * 
	 * @throws GenerateException
	 */

	private void datenformatieren() throws GenerateException {
		processBoxinfo("Daten Formatieren");

		// gitter
		Position datenStart = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position datenEnd = letztePositionRechtsUnten();

		// komplett mit normal gitter
		RangePosition datenRangeInclErg = RangePosition.from(datenStart, datenEnd);
		TableBorder2 border = BorderFactory.from().allThin().toBorder();
		getSheetHelper().setPropertyInRange(getXSpreadSheet(), datenRangeInclErg, TABLE_BORDER2, border);

		SpielrundeHintergrundFarbeGeradeStyle geradeColor = getKonfigurationSheet()
				.getSpielRundeHintergrundFarbeGeradeStyle();
		SpielrundeHintergrundFarbeUnGeradeStyle unGeradeColor = getKonfigurationSheet()
				.getSpielRundeHintergrundFarbeUnGeradeStyle();

		RangePosition datenRangeSpielpaarungen = RangePosition.from(datenRangeInclErg).endeSpalte(TEAM_B_SPALTE);
		spielrundeHelper.formatiereGeradeUngradeSpielpaarungen(this, datenRangeSpielpaarungen, geradeColor,
				unGeradeColor);
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

		processBoxinfo("Neue Spielrunde " + neueSpielrundeNr.getNr());
		processBoxinfo(meldungen.size() + " Meldungen");

		// wenn hier dann neu erstellen
		if (!NewSheet.from(this, getSheetName(getSpielRundeNr())).pos(DefaultSheetPos.SCHWEIZER_WORK)
				.setForceCreate(force).setActiv().hideGrid().create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Spielrunde wurde nicht erstellt");
			return false;
		}

		// neue Spielrunde speichern, sheet vorhanden
		getKonfigurationSheet().setAktiveSpielRunde(getSpielRundeNr());

		SchweizerSystem schweizerSystem = new SchweizerSystem();

		List<TeamPaarung> paarungen;

		if (neueSpielrundeNr.getNr() == 1) {
			paarungen = schweizerSystem.ersteRunde(meldungen.teams());
		} else {
			paarungen = schweizerSystem.weitereRunde(meldungen.teams(), ergebnisse);
		}

		teamPaarungenEinfuegen(paarungen);
		datenErsteSpalte(); // BahnNr
		datenformatieren();
		header();

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
		RangeData rangeData = new RangeData();

		for (TeamPaarung teamPaarung : paarungen) {
			SheetRunner.testDoCancelTask();
			if (!teamPaarung.hasB()) {
				// Freilos – keine Begegnung eintragen
				continue;
			}
			if (useTeamname) {
				String nameA = getMeldeListe().getTeamNameByNr(teamPaarung.getA().getNr());
				String nameB = getMeldeListe().getTeamNameByNr(teamPaarung.getB().getNr());
				RowData row = rangeData.addNewRow();
				row.add(new CellData(nameA != null ? nameA : String.valueOf(teamPaarung.getA().getNr())));
				row.add(new CellData(nameB != null ? nameB : String.valueOf(teamPaarung.getB().getNr())));
			} else {
				rangeData.addNewRow(teamPaarung.getA().getNr(), teamPaarung.getB().getNr());
			}
		}

		Position startPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
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
