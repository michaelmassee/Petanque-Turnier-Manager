/**
 * Erstellung : 01.03.2024 / Michael Massee
 *
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Basisklasse für die Schweizer Team-Meldeliste.
 * <p>
 * Pro Zeile wird ein Team eingetragen. Die Spaltenstruktur richtet sich
 * nach der konfigurierten Formation (Tete/Doublette/Triplette) sowie den
 * optionalen Einstellungen für Teamname und Vereinsname.
 * <p>
 * Layout:
 * <pre>
 * Spalte 0:  Team-Nr
 * Spalte 1:  Teamname (optional)
 * Ab Spalte n: je Spieler Vorname, Nachname [, Vereinsname]
 * </pre>
 *
 * @author Michael Massee
 */
public abstract class AbstractSchweizerMeldeListeSheet extends SchweizerSheet implements MeldeListeKonstanten, ISheet {

	protected static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 32;

	/** Dritte Header-Zeile (Spalten-Namen: Vorname, Nachname, Verein). */
	protected static final int DRITTE_HEADER_ZEILE = 2;
	/** Erste Daten-Zeile (überschreibt MeldeListeKonstanten.ERSTE_DATEN_ZEILE=2): 3 Header-Zeilen. */
	protected static final int ERSTE_DATEN_ZEILE = 3;

	protected static final int NR_SPALTE_WIDTH = 800;
	protected static final int NAME_SPALTE_WIDTH = 3000;
	protected static final int TEAMNAME_SPALTE_WIDTH = 3000;
	protected static final int VEREINSNAME_SPALTE_WIDTH = 2500;

	protected static final String HEADER_NR = "Nr";
	protected static final String HEADER_TEAMNAME = "Teamname";
	protected static final String HEADER_VORNAME = "Vorname";
	protected static final String HEADER_NACHNAME = "Nachname";
	protected static final String HEADER_VEREINSNAME = "Verein";
	protected static final String HEADER_SETZPOSITION = "SP";
	protected static final String HEADER_AKTIV = "Aktiv";
	protected static final int AKTIV_SPALTE_WIDTH = 700;
	public static final int AKTIV_WERT_NIMMT_TEIL = 1;
	public static final int AKTIV_WERT_AUSGESTIEGEN = 2;

	protected AbstractSchweizerMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, "Schweizer-Meldeliste");
	}

	protected AbstractSchweizerMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet, String prefix) {
		super(workingSpreadsheet, prefix);
	}

	// ---------------------------------------------------------------
	// Spalten-Berechnung (abhängig von Konfiguration)
	// ---------------------------------------------------------------

	/** Spalte für die Team-Nummer (immer Spalte 0). */
	protected int getTeamNrSpalte() {
		return SPIELER_NR_SPALTE; // = 0
	}

	/** Spalte für den Teamnamen, oder -1 wenn deaktiviert. */
	protected int getTeamnameSpalte() throws GenerateException {
		return getKonfigurationSheet().isMeldeListeTeamnameAnzeigen() ? 1 : -1;
	}

	/** Anzahl Spalten pro Spieler: 2 (Vorname+Nachname) oder 3 (+Vereinsname). */
	protected int getSpaltenProSpieler() throws GenerateException {
		return getKonfigurationSheet().isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
	}

	/** Index der ersten Spieler-Spalte (Vorname Spieler 1). */
	protected int getErsterSpielerOffset() throws GenerateException {
		return getKonfigurationSheet().isMeldeListeTeamnameAnzeigen() ? 2 : 1;
	}

	/** Erste Spieler-Namens-Spalte (Vorname Spieler 1) — Alias für getErsterSpielerOffset(). */
	public int getSpielerNameErsteSpalte() throws GenerateException {
		return getErsterSpielerOffset();
	}

	/** Vorname-Spalte für Spieler spielerIdx (0-basiert). */
	protected int getVornameSpalte(int spielerIdx) throws GenerateException {
		return getErsterSpielerOffset() + spielerIdx * getSpaltenProSpieler();
	}

	/** Nachname-Spalte für Spieler spielerIdx. */
	protected int getNachnameSpalte(int spielerIdx) throws GenerateException {
		return getVornameSpalte(spielerIdx) + 1;
	}

	/** Vereinsname-Spalte für Spieler spielerIdx, oder -1 wenn deaktiviert. */
	protected int getVereinsnameSpalte(int spielerIdx) throws GenerateException {
		if (!getKonfigurationSheet().isMeldeListeVereinsnameAnzeigen()) {
			return -1;
		}
		return getVornameSpalte(spielerIdx) + 2;
	}

	/** Letzte Spieler-Datenspalte (0-basiert, ohne Setzposition). */
	protected int getLetzteDataSpalte() throws GenerateException {
		Formation f = getKonfigurationSheet().getMeldeListeFormation();
		return getErsterSpielerOffset() + f.getAnzSpieler() * getSpaltenProSpieler() - 1;
	}

	/** Setzposition-Spalte (SP) – direkt nach der letzten Spieler-Spalte. */
	public int getSetzPositionSpalte() throws GenerateException {
		return getLetzteDataSpalte() + 1;
	}

	/** Aktiv/Inaktiv-Spalte – direkt nach der Setzposition-Spalte. */
	public int getAktivSpalte() throws GenerateException {
		return getSetzPositionSpalte() + 1;
	}

	// ---------------------------------------------------------------
	// Sheet-Aufbau
	// ---------------------------------------------------------------

	public void upDateSheet() throws GenerateException {
		processBoxinfo("Aktualisiere Schweizer Meldeliste");

		TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();

		insertHeaderInSheet(getKonfigurationSheet().getMeldeListeHeaderFarbe());
		formatDatenSpalten();
		formatZeilenfarben();

		// Headerzeilen fixieren
		SheetFreeze.from(getTurnierSheet()).anzZeilen(3).doFreeze();
	}

	protected void insertHeaderInSheet(int headerColor) throws GenerateException {
		processBoxinfo("Meldeliste Header");

		// Turniersystem oben links (Zeile 0 = ERSTE_HEADER_ZEILE)
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(getXSpreadSheet(), Position.from(0, ERSTE_HEADER_ZEILE),
						"Turniersystem: " + getTurnierSystem().getBezeichnung())
				.setEndPosMergeSpaltePlus(1).setCharWeight(FontWeight.BOLD)
				.setHoriJustify(CellHoriJustify.LEFT).setVertJustify(CellVertJustify2.TOP)
				.setShrinkToFit(true).setCharColor("00599d"));

		Formation formation = getKonfigurationSheet().getMeldeListeFormation();
		int anzSpieler = formation.getAnzSpieler();
		int spaltenProSpieler = getSpaltenProSpieler();
		boolean teamnameAktiv = getKonfigurationSheet().isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAktiv = getKonfigurationSheet().isMeldeListeVereinsnameAnzeigen();

		ColumnProperties colPropNr = ColumnProperties.from().setWidth(NR_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		ColumnProperties colPropName = ColumnProperties.from().setWidth(NAME_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.LEFT).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);

		// Team-Nr Spalte: merged über Zeile 1+2 (ZWEITE + DRITTE Header-Zeile)
		StringCellValue nrHeader = StringCellValue
				.from(getXSpreadSheet(), Position.from(getTeamNrSpalte(), ZWEITE_HEADER_ZEILE), HEADER_NR)
				.addColumnProperties(colPropNr)
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
				.setVertJustify(CellVertJustify2.CENTER)
				.setEndPosMergeZeilePlus(1);
		getSheetHelper().setStringValueInCell(nrHeader);

		// Teamname-Spalte (optional): merged über Zeile 1+2 (ZWEITE + DRITTE Header-Zeile)
		if (teamnameAktiv) {
			StringCellValue teamnameHeader = StringCellValue
					.from(getXSpreadSheet(), Position.from(1, ZWEITE_HEADER_ZEILE), HEADER_TEAMNAME)
					.addColumnProperties(colPropName.setWidth(TEAMNAME_SPALTE_WIDTH))
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
					.setVertJustify(CellVertJustify2.CENTER)
					.setEndPosMergeZeilePlus(1);
			getSheetHelper().setStringValueInCell(teamnameHeader);
		}

		// Setzposition-Spalte (SP) – merged über beide Header-Zeilen
		ColumnProperties colPropSP = ColumnProperties.from().setWidth(NR_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		StringCellValue spHeader = StringCellValue
				.from(getXSpreadSheet(), Position.from(getSetzPositionSpalte(), ZWEITE_HEADER_ZEILE), HEADER_SETZPOSITION)
				.addColumnProperties(colPropSP)
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
				.setVertJustify(CellVertJustify2.CENTER)
				.setComment("Setzposition: Teams mit gleicher SP werden in Runde 1 nicht gegeneinander ausgelost.")
				.setEndPosMergeZeilePlus(1);
		getSheetHelper().setStringValueInCell(spHeader);

		// Aktiv-Spalte: merged über beide Header-Zeilen
		ColumnProperties colPropAktiv = ColumnProperties.from().setWidth(AKTIV_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		StringCellValue aktivHeader = StringCellValue
				.from(getXSpreadSheet(), Position.from(getAktivSpalte(), ZWEITE_HEADER_ZEILE), HEADER_AKTIV)
				.addColumnProperties(colPropAktiv)
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
				.setVertJustify(CellVertJustify2.CENTER)
				.setComment("1 = Nimmt teil, 2 = Ausgestiegen, leer = Nimmt nicht teil")
				.setEndPosMergeZeilePlus(1)
				.setRotate90();
		getSheetHelper().setStringValueInCell(aktivHeader);

		// Spieler-Blöcke
		for (int s = 0; s < anzSpieler; s++) {
			int vornameSpalte = getVornameSpalte(s);
			String spielerTitel = "Spieler " + (s + 1);

			// Zeile 1 (ZWEITE_HEADER_ZEILE): Block-Titel "Spieler n" über alle Spieler-Spalten
			StringCellValue spielerHeader = StringCellValue
					.from(getXSpreadSheet(), Position.from(vornameSpalte, ZWEITE_HEADER_ZEILE), spielerTitel)
					.addColumnProperties(colPropName)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
					.setVertJustify(CellVertJustify2.CENTER)
					.setHoriJustify(CellHoriJustify.CENTER)
					.setEndPosMergeSpalte(vornameSpalte + spaltenProSpieler - 1);
			getSheetHelper().setStringValueInCell(spielerHeader);

			// Zeile 2 (DRITTE_HEADER_ZEILE): Vorname
			StringCellValue vornameHeader = StringCellValue
					.from(getXSpreadSheet(), Position.from(vornameSpalte, DRITTE_HEADER_ZEILE), HEADER_VORNAME)
					.addColumnProperties(colPropName)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().doubleLn().forLeft().toBorder())
					.setVertJustify(CellVertJustify2.CENTER);
			getSheetHelper().setStringValueInCell(vornameHeader);

			// Zeile 2: Nachname
			StringCellValue nachnameHeader = StringCellValue
					.from(getXSpreadSheet(), Position.from(getNachnameSpalte(s), DRITTE_HEADER_ZEILE), HEADER_NACHNAME)
					.addColumnProperties(colPropName)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().toBorder())
					.setVertJustify(CellVertJustify2.CENTER);
			getSheetHelper().setStringValueInCell(nachnameHeader);

			// Zeile 2: Vereinsname (optional)
			if (vereinsnameAktiv) {
				StringCellValue vereinsHeader = StringCellValue
						.from(getXSpreadSheet(), Position.from(getVereinsnameSpalte(s), DRITTE_HEADER_ZEILE),
								HEADER_VEREINSNAME)
						.addColumnProperties(colPropName.setWidth(VEREINSNAME_SPALTE_WIDTH))
						.setCellBackColor(headerColor)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setVertJustify(CellVertJustify2.CENTER);
				getSheetHelper().setStringValueInCell(vereinsHeader);
			}
		}
	}

	protected void formatDatenSpalten() throws GenerateException {
		Formation formation = getKonfigurationSheet().getMeldeListeFormation();
		int anzSpieler = formation.getAnzSpieler();
		int letzteDatenZeile = getLetzteDatenZeileUseMin();

		// Team-Nr Spalte
		RangePosition nrRange = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
				getTeamNrSpalte(), letzteDatenZeile);
		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), nrRange,
				CellProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().doubleLn().forRight().toBorder()));

		// Teamname-Spalte (optional)
		if (getKonfigurationSheet().isMeldeListeTeamnameAnzeigen()) {
			RangePosition teamnameRange = RangePosition.from(1, ERSTE_DATEN_ZEILE, 1, letzteDatenZeile);
			getSheetHelper().setPropertiesInRange(getXSpreadSheet(), teamnameRange,
					CellProperties.from().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()).setShrinkToFit(true));
		}

		// Spieler-Spalten (Vorname + Nachname [+ Vereinsname])
		for (int s = 0; s < anzSpieler; s++) {
			int ersteSpielSpalte = getVornameSpalte(s);
			int letzteSpielSpalte = getKonfigurationSheet().isMeldeListeVereinsnameAnzeigen()
					? getVereinsnameSpalte(s)
					: getNachnameSpalte(s);
			RangePosition spielerRange = RangePosition.from(ersteSpielSpalte, ERSTE_DATEN_ZEILE,
					letzteSpielSpalte, letzteDatenZeile);
			getSheetHelper().setPropertiesInRange(getXSpreadSheet(), spielerRange,
					CellProperties.from().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()).setShrinkToFit(true));
		}

		// Setzposition-Spalte
		RangePosition spRange = RangePosition.from(getSetzPositionSpalte(), ERSTE_DATEN_ZEILE,
				getSetzPositionSpalte(), letzteDatenZeile);
		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), spRange,
				CellProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));

		// Aktiv-Spalte
		RangePosition aktivRange = RangePosition.from(getAktivSpalte(), ERSTE_DATEN_ZEILE,
				getAktivSpalte(), letzteDatenZeile);
		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), aktivRange,
				CellProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
	}

	protected void formatZeilenfarben() throws GenerateException {
		Integer geradeColor = getKonfigurationSheet().getMeldeListeHintergrundFarbeGerade();
		Integer ungeradeColor = getKonfigurationSheet().getMeldeListeHintergrundFarbeUnGerade();

		int letzteDatenZeile = getLetzteDatenZeileUseMin();
		int letzteSpalte = getAktivSpalte();

		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteDatenZeile; zeile++) {
			RangePosition zeileRange = RangePosition.from(getTeamNrSpalte(), zeile, letzteSpalte, zeile);
			Integer color = ((zeile - ERSTE_DATEN_ZEILE) % 2 == 0) ? geradeColor : ungeradeColor;
			getSheetHelper().setPropertiesInRange(getXSpreadSheet(), zeileRange,
					CellProperties.from().setCellBackColor(color));
		}
	}

	protected int getLetzteDatenZeileUseMin() {
		return ERSTE_DATEN_ZEILE + MIN_ANZAHL_MELDUNGEN_ZEILEN - 1;
	}

	public int getErsteDatenZiele() {
		return ERSTE_DATEN_ZEILE;
	}

	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	/**
	 * Liest alle aktiven Team-Meldungen aus dem Sheet.
	 * <p>
	 * Fallback: Wenn kein Team einen Aktiv-Wert hat, nehmen alle teil.
	 */
	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		XSpreadsheet xSheet = getXSpreadSheet();
		int letzteZeile = letzteZeileMitDaten(xSheet);

		record TeamZeile(int nr, int setzPos, int aktivWert) {}
		List<TeamZeile> alleTeams = new ArrayList<>();
		boolean hatAktivEintrag = false;

		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = getSheetHelper().getTextFromCell(xSheet, Position.from(getVornameSpalte(0), zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				continue;
			}
			int setzPos = getSheetHelper().getIntFromCell(xSheet, Position.from(getSetzPositionSpalte(), zeile));
			int aktivWert = getSheetHelper().getIntFromCell(xSheet, Position.from(getAktivSpalte(), zeile));
			if (aktivWert > 0) {
				hatAktivEintrag = true;
			}
			alleTeams.add(new TeamZeile(nr, setzPos, aktivWert));
		}

		TeamMeldungen meldungen = new TeamMeldungen();
		for (TeamZeile tz : alleTeams) {
			if (!hatAktivEintrag || tz.aktivWert() == AKTIV_WERT_NIMMT_TEIL) {
				meldungen.addTeamWennNichtVorhanden(Team.from(tz.nr()).setSetzPos(tz.setzPos()));
			}
		}
		return meldungen;
	}

	/**
	 * Sucht die Teamnummer anhand des Teamnamens (Reverse-Lookup).
	 * Gibt -1 zurück wenn nicht gefunden oder Teamname-Spalte deaktiviert.
	 */
	public int getTeamNrByTeamname(String teamname) throws GenerateException {
		if (teamname == null || teamname.isEmpty() || !getKonfigurationSheet().isMeldeListeTeamnameAnzeigen()) {
			return -1;
		}
		XSpreadsheet xSheet = getXSpreadSheet();
		int letzteZeile = letzteZeileMitDaten(xSheet);
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String name = getSheetHelper().getTextFromCell(xSheet, Position.from(1, zeile));
			if (teamname.equals(name)) {
				return getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			}
		}
		return -1;
	}

	/**
	 * Liest den Teamnamen für die angegebene Teamnummer aus der Meldeliste.
	 * Gibt null zurück wenn die Teamname-Spalte deaktiviert ist oder die Nr nicht gefunden wird.
	 */
	public String getTeamNameByNr(int teamNr) throws GenerateException {
		if (!getKonfigurationSheet().isMeldeListeTeamnameAnzeigen()) {
			return null;
		}
		XSpreadsheet xSheet = getXSpreadSheet();
		int letzteZeile = letzteZeileMitDaten(xSheet);
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			int nr = getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr == teamNr) {
				return getSheetHelper().getTextFromCell(xSheet, Position.from(1, zeile));
			}
		}
		return null;
	}

	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		getKonfigurationSheet().setAktiveSpielRunde(spielRundeNr);
	}

	/**
	 * Letzte Zeile mit einem nicht-leeren Vorname (Spieler 1) ab ERSTE_DATEN_ZEILE.
	 * Gibt ERSTE_DATEN_ZEILE - 1 zurück wenn keine Daten vorhanden.
	 */
	protected int letzteZeileMitDaten(XSpreadsheet xSheet) throws GenerateException {
		int vornameSpalte = getVornameSpalte(0);
		int letzte = ERSTE_DATEN_ZEILE - 1;
		int maxZeile = ERSTE_DATEN_ZEILE + 500;
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= maxZeile; zeile++) {
			String vorname = getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname != null && !vorname.isEmpty()) {
				letzte = zeile;
			}
		}
		return letzte;
	}

	/**
	 * Prüft auf doppelte Team-Nummern nach der aufsteigenden Sortierung.
	 * Wirft GenerateException mit allen betroffenen Zeilen und Nummern.
	 * Muss NACH der aufsteigenden Sortierung nach Team-Nr aufgerufen werden.
	 */
	protected void pruefeAufDoppelteTeamNr(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = letzteZeileMitDaten(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		int vornameSpalte = getVornameSpalte(0);
		Map<Integer, List<Integer>> alleNrn = new LinkedHashMap<>();
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				continue;
			}
			alleNrn.computeIfAbsent(nr, k -> new ArrayList<>()).add(zeile);
		}
		Map<Integer, List<Integer>> duplikate = new LinkedHashMap<>();
		for (Map.Entry<Integer, List<Integer>> entry : alleNrn.entrySet()) {
			if (entry.getValue().size() > 1) {
				duplikate.put(entry.getKey(), entry.getValue());
			}
		}
		if (duplikate.isEmpty()) {
			return;
		}
		StringBuilder sb = new StringBuilder("Meldeliste wurde nicht aktualisiert.\nDoppelte Startnummern:");
		for (Map.Entry<Integer, List<Integer>> entry : duplikate.entrySet()) {
			sb.append("\nNr. ").append(entry.getKey()).append(": Zeilen ");
			List<Integer> zeilen = entry.getValue();
			for (int i = 0; i < zeilen.size(); i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(zeilen.get(i) + 1); // 0-basiert → 1-basiert für Benutzer
			}
		}
		throw new GenerateException(sb.toString());
	}

}
