/**
 * Erstellung : 01.03.2024 / Michael Massee
 *
 */
package de.petanqueturniermanager.schweizer.meldeliste;

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
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
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

	protected static final int NR_SPALTE_WIDTH = 800;
	protected static final int NAME_SPALTE_WIDTH = 3000;
	protected static final int TEAMNAME_SPALTE_WIDTH = 3000;
	protected static final int VEREINSNAME_SPALTE_WIDTH = 2500;

	protected static final String HEADER_NR = "Nr";
	protected static final String HEADER_TEAMNAME = "Teamname";
	protected static final String HEADER_VORNAME = "Vorname";
	protected static final String HEADER_NACHNAME = "Nachname";
	protected static final String HEADER_VEREINSNAME = "Verein";

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

	/** Letzte Datenspalte (0-basiert). */
	protected int getLetzteDataSpalte() throws GenerateException {
		Formation f = getKonfigurationSheet().getMeldeListeFormation();
		return getErsterSpielerOffset() + f.getAnzSpieler() * getSpaltenProSpieler() - 1;
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
	}

	protected void insertHeaderInSheet(int headerColor) throws GenerateException {
		processBoxinfo("Meldeliste Header");

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

		// Team-Nr Spalte: merged über beide Header-Zeilen
		StringCellValue nrHeader = StringCellValue
				.from(getXSpreadSheet(), Position.from(getTeamNrSpalte(), ERSTE_HEADER_ZEILE), HEADER_NR)
				.addColumnProperties(colPropNr)
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().toBorder())
				.setVertJustify(CellVertJustify2.CENTER)
				.setEndPosMergeZeilePlus(1);
		getSheetHelper().setStringValueInCell(nrHeader);

		// Teamname-Spalte (optional): merged über beide Header-Zeilen
		if (teamnameAktiv) {
			StringCellValue teamnameHeader = StringCellValue
					.from(getXSpreadSheet(), Position.from(1, ERSTE_HEADER_ZEILE), HEADER_TEAMNAME)
					.addColumnProperties(colPropName.setWidth(TEAMNAME_SPALTE_WIDTH))
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().toBorder())
					.setVertJustify(CellVertJustify2.CENTER)
					.setEndPosMergeZeilePlus(1);
			getSheetHelper().setStringValueInCell(teamnameHeader);
		}

		// Spieler-Blöcke
		for (int s = 0; s < anzSpieler; s++) {
			int vornameSpalte = getVornameSpalte(s);
			String spielerTitel = "Spieler " + (s + 1);

			// Zeile 1 (ERSTE_HEADER_ZEILE): Block-Titel "Spieler n" über alle Spieler-Spalten
			StringCellValue spielerHeader = StringCellValue
					.from(getXSpreadSheet(), Position.from(vornameSpalte, ERSTE_HEADER_ZEILE), spielerTitel)
					.addColumnProperties(colPropName)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().toBorder())
					.setVertJustify(CellVertJustify2.CENTER)
					.setHoriJustify(CellHoriJustify.CENTER)
					.setEndPosMergeSpalte(vornameSpalte + spaltenProSpieler - 1);
			getSheetHelper().setStringValueInCell(spielerHeader);

			// Zeile 2 (ZWEITE_HEADER_ZEILE): Vorname
			StringCellValue vornameHeader = StringCellValue
					.from(getXSpreadSheet(), Position.from(vornameSpalte, ZWEITE_HEADER_ZEILE), HEADER_VORNAME)
					.addColumnProperties(colPropName)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().toBorder())
					.setVertJustify(CellVertJustify2.CENTER);
			getSheetHelper().setStringValueInCell(vornameHeader);

			// Zeile 2: Nachname
			StringCellValue nachnameHeader = StringCellValue
					.from(getXSpreadSheet(), Position.from(getNachnameSpalte(s), ZWEITE_HEADER_ZEILE), HEADER_NACHNAME)
					.addColumnProperties(colPropName)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().toBorder())
					.setVertJustify(CellVertJustify2.CENTER);
			getSheetHelper().setStringValueInCell(nachnameHeader);

			// Zeile 2: Vereinsname (optional)
			if (vereinsnameAktiv) {
				StringCellValue vereinsHeader = StringCellValue
						.from(getXSpreadSheet(), Position.from(getVereinsnameSpalte(s), ZWEITE_HEADER_ZEILE),
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
				CellProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().toBorder()));

		// Teamname-Spalte (optional)
		if (getKonfigurationSheet().isMeldeListeTeamnameAnzeigen()) {
			RangePosition teamnameRange = RangePosition.from(1, ERSTE_DATEN_ZEILE, 1, letzteDatenZeile);
			getSheetHelper().setPropertiesInRange(getXSpreadSheet(), teamnameRange,
					CellProperties.from().setBorder(BorderFactory.from().allThin().toBorder()).setShrinkToFit(true));
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
					CellProperties.from().setBorder(BorderFactory.from().allThin().toBorder()).setShrinkToFit(true));
		}
	}

	protected void formatZeilenfarben() throws GenerateException {
		Integer geradeColor = getKonfigurationSheet().getMeldeListeHintergrundFarbeGerade();
		Integer ungeradeColor = getKonfigurationSheet().getMeldeListeHintergrundFarbeUnGerade();

		int letzteDatenZeile = getLetzteDatenZeileUseMin();
		int letzteSpalte = getLetzteDataSpalte();

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
	 * TODO: Implementierung der Sheet-Lesefunktion für Teams
	 */
	public TeamMeldungen getAktiveMeldungen() throws GenerateException {
		return new TeamMeldungen();
	}

	public void setAktiveSpielRunde(SpielRundeNr spielRundeNr) throws GenerateException {
		getKonfigurationSheet().setAktiveSpielRunde(spielRundeNr);
	}

}
