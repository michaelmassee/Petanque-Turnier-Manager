/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Gemeinsame Basis für die Checkin-Listen aller Turniersysteme (außer Liga).
 * <p>
 * Erzeugt eine kompakte, druckbare Ansicht zum Abhaken der anwesenden Teilnehmer:
 * mehrere Blöcke nebeneinander (je {@link #getMaxProSpalte()} Einträge pro Block),
 * je Eintrag eine Nummer-Spalte, eine Namens-Spalte (per VLOOKUP-Formel aus der
 * Meldeliste) und eine leere Checkbox-Spalte.
 * <p>
 * Die Sortierreihenfolge ist über die Turnier-Konfiguration einstellbar
 * ({@link CheckinListeSortModus}, Default {@link CheckinListeSortModus#NACHNAME}).
 * <p>
 * Konkrete Subklassen liefern die system-spezifische Meldeliste-Anbindung über die
 * abstrakten Hook-Methoden.
 */
public abstract class AbstractCheckinListeSheet extends SheetRunner implements ISheet {

	public static final int KOPF_ZEILE = 0;
	public static final int ERSTE_DATEN_ZEILE = 1;
	public static final int NR_SPALTE = 0;
	public static final int NAME_SPALTE = 1;

	private static final int SPALTEN_PRO_BLOCK = 4; // Nr, Name, Checkbox, Trennspalte
	private static final int DEFAULT_NAME_SPALTE_WIDTH = 4000;

	/** Großzügige Clear-Range für den Update-Pfad: deckt jede plausible Block-Konfiguration ab. */
	private static final int CLEAR_LETZTE_SPALTE = 40;
	private static final int CLEAR_LETZTE_ZEILE = 200;

	protected AbstractCheckinListeSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem turnierSystem,
			String logPrefix) {
		super(workingSpreadsheet, turnierSystem, logPrefix);
	}

	@Override
	protected void doRun() throws GenerateException {
		meldelisteVorbereiten();
		generate();
	}

	/**
	 * Baut das Checkin-Listen-Sheet auf (Sheet erzeugen, sortieren, befüllen).
	 * <p>
	 * Setzt voraus, dass die zugrunde liegende Meldeliste bereits existiert und befüllt ist
	 * (im normalen Ablauf via {@link #meldelisteVorbereiten()}, im Testdaten-Pfad durch den
	 * jeweiligen Testdaten-Generator).
	 */
	public void generate() throws GenerateException {
		meldelisteAusrichten();
		NewSheet newSheet = NewSheet
				.from(this, getCheckinSheetName(), getMetadatenSchluessel())
				.tabColor(getKonfigurationSheet().getTeilnehmerTabFarbe())
				.pos(getSheetPos()).forceCreate().hideGrid().setActiv();
		seitenStilAnwenden(newSheet);
		newSheet.create();
		sortiereUndFuelle();
	}

	/**
	 * Aktualisiert den Inhalt einer <b>bereits existierenden</b> Checkin-Liste, ohne das Sheet
	 * neu zu erzeugen (kein {@code forceCreate}, kein {@code setActiv}).
	 * <p>
	 * Wird vom {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener} über die
	 * {@code *CheckinListeSheetUpdate}-Subklassen aufgerufen, um die Liste bei einem Tab-Wechsel
	 * mit der Meldeliste zu synchronisieren. Der Erstaufbau erfolgt ausschließlich über das Menü
	 * ({@link #generate()}).
	 */
	protected final void aktualisiereInhalt() throws GenerateException {
		meldelisteAusrichten();
		leereDatenbereich();
		sortiereUndFuelle();
	}

	/**
	 * Sortiert die Meldeliste (sofern Einträge vorhanden) nach dem konfigurierten
	 * {@link CheckinListeSortModus} und befüllt den Checkin-Bereich.
	 * <p>
	 * Bei leerer Meldeliste wird dennoch eine gültige (leere) Checkin-Liste erstellt.
	 * Sortierung/Spaltenermittlung nur bei vorhandenen Einträgen, um Edge-Cases auf
	 * leerer Meldeliste zu vermeiden.
	 */
	private void sortiereUndFuelle() throws GenerateException {
		List<Integer> nummern = ladeSortierteNummern();
		if (!nummern.isEmpty()) {
			int sortSpalte = (getKonfigurationSheet().getCheckinListeSortModus() == CheckinListeSortModus.NUMMER)
					? getNummerSpalteMeldeliste()
					: getNachnameSpalteMeldeliste();
			meldelisteSortieren(sortSpalte, true);
			nummern = ladeSortierteNummern();
		}
		fuelleBereich(nummern);
	}

	/** Löscht den bisherigen Inhalt der existierenden Checkin-Liste (Update-Pfad). */
	private void leereDatenbereich() throws GenerateException {
		RangeHelper.from(this, RangePosition.from(0, 0, CLEAR_LETZTE_SPALTE, CLEAR_LETZTE_ZEILE)).clearRange();
	}

	/**
	 * Befüllt den Checkin-Bereich blockweise als Block-Schreibvorgang (kein zellenweises Schreiben).
	 */
	private void fuelleBereich(List<Integer> nummern) throws GenerateException {
		if (nummern.isEmpty()) {
			// Leere Meldeliste: dennoch eine Kopfzeile (Überschrift) anzeigen und Druckbereich setzen.
			kopfzeileSchreiben(NR_SPALTE);
			printBereichDefinieren(KOPF_ZEILE, NAME_SPALTE);
			return;
		}

		final int maxProSpalte = getMaxProSpalte();
		final int anzBloecke = (int) Math.ceil((double) nummern.size() / maxProSpalte);
		final boolean nameAlsFormel = nameAlsFormel();
		final Map<Integer, String> namen = nameAlsFormel ? Map.of() : namenNachNummer();

		RangeData data = new RangeData();
		for (int zeileImBlock = 0; zeileImBlock < maxProSpalte; zeileImBlock++) {
			RowData zeileData = data.addNewRow();
			for (int blkCntr = 1; blkCntr <= anzBloecke; blkCntr++) {
				int idx = zeileImBlock + (blkCntr - 1) * maxProSpalte;
				if (idx < nummern.size()) {
					int nr = nummern.get(idx);
					zeileData.newInt(nr);
					// Im Formel-Modus wird die Namens-Spalte später per Fill-Down-Formel gesetzt.
					zeileData.newString(nameAlsFormel ? "" : namen.getOrDefault(nr, ""));
					if (blkCntr != anzBloecke) {
						zeileData.newEmpty(); // Checkbox-Spalte
						zeileData.newEmpty(); // Trennspalte
					}
				}
			}
		}
		RangePosition rangePosition = data.getRangePosition(Position.from(NR_SPALTE, ERSTE_DATEN_ZEILE));
		RangeHelper.from(this, rangePosition).setDataInRange(data);

		spaltenFormatieren(nummern, anzBloecke, maxProSpalte, nameAlsFormel);
	}

	private void spaltenFormatieren(List<Integer> nummern, int anzBloecke, int maxProSpalte, boolean nameAlsFormel)
			throws GenerateException {
		RangeProperties rangePropNr = RangeProperties.from().setHoriJustify(CellHoriJustify.CENTER)
				.setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR);
		ColumnProperties columnPropNr = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);

		ColumnProperties columnPropName = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER)
				.setWidth(getNameSpalteWidth());
		StringCellValue nameFormula = StringCellValue.from(getXSpreadSheet(), Position.from(NAME_SPALTE, ERSTE_DATEN_ZEILE))
				.setShrinkToFit(true).setColumnProperties(columnPropName);

		ColumnProperties columnPropChkBox = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);
		RangeProperties rangePropBorderOnly = RangeProperties.from().setBorder(BorderFactory.from().allThin().toBorder());

		int maxMeldungZeile = 0;
		int letzteSpalte = 0;

		for (int blkCntr = 0; blkCntr < anzBloecke; blkCntr++) {
			int letzteZeile = ERSTE_DATEN_ZEILE + maxProSpalte - 1;
			if (blkCntr + 1 == anzBloecke) {
				letzteZeile = ERSTE_DATEN_ZEILE + (nummern.size() - ((anzBloecke - 1) * maxProSpalte)) - 1;
			}
			maxMeldungZeile = Math.max(maxMeldungZeile, letzteZeile);

			// Kopfzeile (Überschrift) je Block über der Nr-/Namens-Spalte
			kopfzeileSchreiben(NR_SPALTE + (blkCntr * SPALTEN_PRO_BLOCK));

			RangePosition blockNrRange = RangePosition.from(NR_SPALTE + (blkCntr * SPALTEN_PRO_BLOCK), ERSTE_DATEN_ZEILE,
					NR_SPALTE + (blkCntr * SPALTEN_PRO_BLOCK), letzteZeile);
			Position startNrPos = Position.from(blockNrRange.getStart());

			RangeHelper.from(this, blockNrRange).setRangeProperties(rangePropNr);
			getSheetHelper().setColumnProperties(getXSpreadSheet(), blockNrRange.getStartSpalte(), columnPropNr);

			blockNrRange.spaltePlusEins();
			if (nameAlsFormel) {
				String formula = getNameFormula(startNrPos.getAddress());
				nameFormula.setPos((Position) blockNrRange.getStart()).setFillAutoDown(letzteZeile).setValue(formula);
				getSheetHelper().setFormulaInCell(nameFormula);
			} else {
				// Namen sind bereits als Werte geschrieben; nur Spaltenbreite/Ausrichtung setzen.
				getSheetHelper().setColumnProperties(getXSpreadSheet(), blockNrRange.getStartSpalte(), columnPropName);
			}

			blockNrRange.spaltePlusEins();
			getSheetHelper().setColumnProperties(getXSpreadSheet(), blockNrRange.getStartSpalte(), columnPropChkBox);

			RangePosition blockAll = RangePosition.from(Position.from(startNrPos), Position.from(blockNrRange.getEnde()));
			RangeHelper.from(this, blockAll).setRangeProperties(rangePropBorderOnly);

			getSheetHelper().setColumnProperties(getXSpreadSheet(), blockNrRange.getEnde().getSpalte() + 1, columnPropNr);
			letzteSpalte = blockNrRange.getEnde().getSpalte();
		}

		printBereichDefinieren(maxMeldungZeile, letzteSpalte);
	}

	/**
	 * Schreibt eine Kopfzeile (Spaltenüberschriften „Nr"/„Name") in {@link #KOPF_ZEILE}
	 * ab der angegebenen Nr-Spalte (je Block). Wird im befüllten wie im leeren Fall genutzt.
	 *
	 * @param nrSpalte Spaltenindex der Nr-Spalte des Blocks (Name folgt in {@code nrSpalte + 1})
	 */
	private void kopfzeileSchreiben(int nrSpalte) throws GenerateException {
		int headerColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		var border = BorderFactory.from().allThin().boldLn().forTop().toBorder();

		StringCellValue nrHeader = StringCellValue.from(getXSpreadSheet(), Position.from(nrSpalte, KOPF_ZEILE))
				.setValue(I18n.get("column.header.nr")).setCharWeight(FontWeight.BOLD)
				.setCellBackColor(headerColor).setBorder(border)
				.addColumnProperties(ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
						.setHoriJustify(CellHoriJustify.CENTER));
		getSheetHelper().setStringValueInCell(nrHeader);

		StringCellValue nameHeader = StringCellValue.from(getXSpreadSheet(), Position.from(nrSpalte + 1, KOPF_ZEILE))
				.setValue(I18n.get("column.header.name")).setCharWeight(FontWeight.BOLD)
				.setCellBackColor(headerColor).setBorder(border)
				.addColumnProperties(ColumnProperties.from().setWidth(getNameSpalteWidth())
						.setHoriJustify(CellHoriJustify.CENTER));
		getSheetHelper().setStringValueInCell(nameHeader);
	}

	private void printBereichDefinieren(int letzteZeile, int letzteSpalte) throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		Position linksOben = Position.from(NR_SPALTE, KOPF_ZEILE);
		Position rechtsUnten = Position.from(letzteSpalte, letzteZeile);
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	// ── system-spezifische Konfiguration ─────────────────────────────────────

	@Override
	protected abstract BaseKonfigurationSheet getKonfigurationSheet();

	/** Maximale Anzahl Einträge je Block-Spalte. */
	protected int getMaxProSpalte() {
		return getKonfigurationSheet().getMaxAnzTeilnehmerInSpalte();
	}

	/** Spaltenbreite der Namens-Spalte. Default {@value #DEFAULT_NAME_SPALTE_WIDTH}. */
	protected int getNameSpalteWidth() {
		return DEFAULT_NAME_SPALTE_WIDTH;
	}

	/** Sheet-Position im Dokument (siehe {@code DefaultSheetPos}). */
	protected abstract short getSheetPos();

	/** Lokalisierter Tabellenname der Checkin-Liste. */
	protected abstract String getCheckinSheetName();

	/** Metadaten-Schlüssel zur Wiedererkennung des Sheets. */
	protected abstract String getMetadatenSchluessel();

	/**
	 * Setzt den Seitenstil auf dem {@link NewSheet}-Builder. Default: keine Anpassung
	 * (es gilt der Standard-Seitenstil). Spieltag-basierte Systeme überschreiben dies.
	 */
	protected void seitenStilAnwenden(NewSheet newSheet) {
		// Standard: keine spezielle Seitenstil-Behandlung
	}

	/** Aktualisiert die zugrunde liegende Meldeliste, damit die VLOOKUP-Daten vorhanden sind. */
	protected abstract void meldelisteVorbereiten() throws GenerateException;

	/**
	 * Richtet die Meldeliste vor dem Aufbau aus (z.B. Spieltag setzen). Default: keine Aktion.
	 * Wird zu Beginn von {@link #generate()} aufgerufen – auch im Testdaten-Pfad.
	 */
	protected void meldelisteAusrichten() throws GenerateException {
		// Standard: nichts auszurichten (Systeme mit einer einzelnen Meldeliste)
	}

	/** Sortiert die Meldeliste nach der angegebenen Spalte. */
	protected abstract void meldelisteSortieren(int spalteNr, boolean aufsteigend) throws GenerateException;

	/** Spaltenindex der Nachname-Spalte in der Meldeliste. */
	protected abstract int getNachnameSpalteMeldeliste() throws GenerateException;

	/** Spaltenindex der Nummer-Spalte in der Meldeliste. */
	protected abstract int getNummerSpalteMeldeliste() throws GenerateException;

	/** Alle Melde-/Teamnummern in der aktuellen (sortierten) Reihenfolge der Meldeliste. */
	protected abstract List<Integer> ladeSortierteNummern() throws GenerateException;

	/**
	 * Steuert die Namens-Quelle der Checkin-Liste.
	 *
	 * @return {@code true} (Default): Namen werden als Fill-Down-VLOOKUP-Formel gesetzt
	 *         (über {@link #getNameFormula(String)}); {@code false}: Namen werden als feste
	 *         Werte geschrieben (über {@link #namenNachNummer()}).
	 */
	protected boolean nameAlsFormel() {
		return true;
	}

	/**
	 * VLOOKUP-Formel, die zu einer Nummer-Zelladresse den anzuzeigenden Namen liefert.
	 * Nur relevant, wenn {@link #nameAlsFormel()} {@code true} liefert.
	 */
	protected String getNameFormula(String nrZelleAdresse) throws GenerateException {
		throw new GenerateException("getNameFormula() nicht implementiert");
	}

	/**
	 * Liefert je Melde-/Teamnummer den anzuzeigenden Namen.
	 * Nur relevant, wenn {@link #nameAlsFormel()} {@code false} liefert.
	 */
	protected Map<Integer, String> namenNachNummer() throws GenerateException {
		return Map.of();
	}
}
