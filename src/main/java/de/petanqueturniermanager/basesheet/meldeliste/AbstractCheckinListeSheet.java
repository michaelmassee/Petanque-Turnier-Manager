/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.ArrayList;
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
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder.TeilnehmerEintrag;

/**
 * Gemeinsame Basis für die Checkin-Listen aller Turniersysteme (außer Liga).
 * <p>
 * Erzeugt eine kompakte, druckbare Ansicht zum Abhaken der anwesenden Teilnehmer:
 * mehrere Blöcke nebeneinander (je {@link #getMaxProSpalte()} Einträge pro Block),
 * je Eintrag eine Nummer-Spalte, eine Namens-Spalte (als Wert aus der Meldeliste,
 * {@link #namenNachNummer()}) und eine leere Checkbox-Spalte.
 * <p>
 * Die Sortierreihenfolge ist über die Turnier-Konfiguration einstellbar
 * ({@link TeilnehmerListeSortModus}, Default {@link TeilnehmerListeSortModus#NAME}) und wird
 * in-memory angewandt. Systeme ohne Teamname lassen den Teamname-Modus auf den Namen zurückfallen
 * ({@link #teamnameVerfuegbar()}).
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
	 * Sortiert die Melde-/Teamnummern (sofern vorhanden) in-memory nach dem konfigurierten
	 * {@link TeilnehmerListeSortModus} und befüllt den Checkin-Bereich. Es wird – anders als früher –
	 * <b>nicht</b> die Meldeliste physisch umsortiert.
	 * <p>
	 * Hat das System keinen Teamnamen ({@link #teamnameVerfuegbar()} {@code == false}), fällt
	 * der Modus {@link TeilnehmerListeSortModus#TEAMNAME} auf {@link TeilnehmerListeSortModus#NAME}
	 * zurück.
	 * <p>
	 * Bei leerer Meldeliste wird dennoch eine gültige (leere) Checkin-Liste erstellt.
	 */
	private void sortiereUndFuelle() throws GenerateException {
		List<Integer> nummern = ladeNummern();
		if (!nummern.isEmpty()) {
			TeilnehmerListeSortModus modus = getKonfigurationSheet().getCheckinListeSortModus();
			if (modus == TeilnehmerListeSortModus.TEAMNAME && !teamnameVerfuegbar()) {
				modus = TeilnehmerListeSortModus.NAME;
			}
			nummern = sortiereNummern(nummern, ladeSortDaten(), modus);
		}
		fuelleBereich(nummern);
	}

	/**
	 * Sortiert die Nummern in-memory über den {@link TeilnehmerListeSortModus#comparator()}.
	 * Nummern ohne Sortierdaten werden mit leeren Schlüsseln behandelt.
	 */
	private static List<Integer> sortiereNummern(List<Integer> nummern, Map<Integer, SortSchluessel> sortDaten,
			TeilnehmerListeSortModus modus) {
		List<TeilnehmerEintrag> eintraege = new ArrayList<>(nummern.size());
		for (int nr : nummern) {
			SortSchluessel sk = sortDaten.getOrDefault(nr, SortSchluessel.LEER);
			eintraege.add(new TeilnehmerEintrag(nr, sk.teamname(), "", sk.sortNachname()));
		}
		eintraege.sort(modus.comparator());
		return eintraege.stream().map(TeilnehmerEintrag::nr).toList();
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
			// Leere Meldeliste: dennoch Kopfzeile + Fußzeile (Anzahl 0) anzeigen und Druckbereich setzen.
			kopfzeileSchreiben(NR_SPALTE);
			int footerZeile = footerSchreiben(0, KOPF_ZEILE, NAME_SPALTE);
			printBereichDefinieren(footerZeile, NAME_SPALTE);
			return;
		}

		final int maxProSpalte = getMaxProSpalte();
		final int anzBloecke = (int) Math.ceil((double) nummern.size() / maxProSpalte);
		final Map<Integer, String> namen = namenNachNummer();

		RangeData data = new RangeData();
		for (int zeileImBlock = 0; zeileImBlock < maxProSpalte; zeileImBlock++) {
			RowData zeileData = data.addNewRow();
			for (int blkCntr = 1; blkCntr <= anzBloecke; blkCntr++) {
				int idx = zeileImBlock + (blkCntr - 1) * maxProSpalte;
				if (idx < nummern.size()) {
					int nr = nummern.get(idx);
					zeileData.newInt(nr);
					zeileData.newString(namen.getOrDefault(nr, ""));
					if (blkCntr != anzBloecke) {
						zeileData.newEmpty(); // Checkbox-Spalte
						zeileData.newEmpty(); // Trennspalte
					}
				}
			}
		}
		RangePosition rangePosition = data.getRangePosition(Position.from(NR_SPALTE, ERSTE_DATEN_ZEILE));
		RangeHelper.from(this, rangePosition).setDataInRange(data);

		FormatErgebnis fmt = spaltenFormatieren(nummern, anzBloecke, maxProSpalte);
		int footerZeile = footerSchreiben(nummern.size(), fmt.letzteDatenZeile(), fmt.letzteSpalte());
		printBereichDefinieren(footerZeile, fmt.letzteSpalte());
	}

	/** Ergebnis der Spaltenformatierung: letzte Datenzeile und letzte Spalte des Blockbereichs. */
	private record FormatErgebnis(int letzteDatenZeile, int letzteSpalte) {
	}

	private FormatErgebnis spaltenFormatieren(List<Integer> nummern, int anzBloecke, int maxProSpalte)
			throws GenerateException {
		RangeProperties rangePropNr = RangeProperties.from().setHoriJustify(CellHoriJustify.CENTER)
				.setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR);
		ColumnProperties columnPropNr = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH);

		ColumnProperties columnPropName = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER)
				.setWidth(getNameSpalteWidth());

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

			// Namen sind bereits als Werte geschrieben; nur Spaltenbreite/Ausrichtung setzen.
			blockNrRange.spaltePlusEins();
			getSheetHelper().setColumnProperties(getXSpreadSheet(), blockNrRange.getStartSpalte(), columnPropName);

			blockNrRange.spaltePlusEins();
			getSheetHelper().setColumnProperties(getXSpreadSheet(), blockNrRange.getStartSpalte(), columnPropChkBox);

			RangePosition blockAll = RangePosition.from(Position.from(startNrPos), Position.from(blockNrRange.getEnde()));
			RangeHelper.from(this, blockAll).setRangeProperties(rangePropBorderOnly);

			getSheetHelper().setColumnProperties(getXSpreadSheet(), blockNrRange.getEnde().getSpalte() + 1, columnPropNr);
			letzteSpalte = blockNrRange.getEnde().getSpalte();
		}

		return new FormatErgebnis(maxMeldungZeile, letzteSpalte);
	}

	/**
	 * Schreibt die Fußzeile mit der Teilnehmer-Anzahl (gemergt über die Blockbreite) eine Zeile
	 * unter dem Datenbereich – im Stil der Teilnehmerliste.
	 *
	 * @return Zeilenindex der Fußzeile (für den Druckbereich)
	 */
	private int footerSchreiben(int anzahl, int letzteDatenZeile, int letzteSpalte) throws GenerateException {
		int footerZeile = letzteDatenZeile + 1;
		StringCellValue footer = StringCellValue.from(getXSpreadSheet(), Position.from(NR_SPALTE, footerZeile))
				.setValue(I18n.get("teilnehmer.footer.anzahl", anzahl))
				.setEndPosMergeSpalte(letzteSpalte).setCharWeight(FontWeight.BOLD).setCharHeight(12)
				.setShrinkToFit(true);
		getSheetHelper().setStringValueInCell(footer);
		return footerZeile;
	}

	/**
	 * Schreibt eine Kopfzeile (Spaltenüberschriften „Nr"/„Name") in {@link #KOPF_ZEILE}
	 * ab der angegebenen Nr-Spalte (je Block). Wird im befüllten wie im leeren Fall genutzt.
	 *
	 * @param nrSpalte Spaltenindex der Nr-Spalte des Blocks (Name folgt in {@code nrSpalte + 1})
	 */
	private void kopfzeileSchreiben(int nrSpalte) throws GenerateException {
		int headerColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		// Header-Stil wie Teilnehmerliste: untere fette Trennlinie, nicht fett, ShrinkToFit.
		var border = BorderFactory.from().allThin().boldLn().forBottom().toBorder();

		StringCellValue nrHeader = StringCellValue.from(getXSpreadSheet(), Position.from(nrSpalte, KOPF_ZEILE))
				.setValue(I18n.get("column.header.nr"))
				.setCellBackColor(headerColor).setBorder(border).setShrinkToFit(true)
				.addColumnProperties(ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
						.setHoriJustify(CellHoriJustify.CENTER));
		getSheetHelper().setStringValueInCell(nrHeader);

		StringCellValue nameHeader = StringCellValue.from(getXSpreadSheet(), Position.from(nrSpalte + 1, KOPF_ZEILE))
				.setValue(I18n.get("column.header.name"))
				.setCellBackColor(headerColor).setBorder(border).setShrinkToFit(true)
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

	/** Alle Melde-/Teamnummern in der Reihenfolge der Meldeliste (unsortiert – Sortierung erfolgt in-memory). */
	protected abstract List<Integer> ladeNummern() throws GenerateException;

	/**
	 * Liefert je Melde-/Teamnummer die Sortierschlüssel (Teamname und Nachname-Schlüssel).
	 * Für Systeme ohne Teamname bleibt {@link SortSchluessel#teamname()} leer.
	 */
	protected abstract Map<Integer, SortSchluessel> ladeSortDaten() throws GenerateException;

	/**
	 * Ob in diesem System überhaupt ein Teamname zum Sortieren verfügbar ist. Default {@code false}
	 * (Einzelspieler-Systeme). Bei {@code false} fällt der Teamname-Modus auf den Namen zurück.
	 */
	protected boolean teamnameVerfuegbar() {
		return false;
	}

	/**
	 * Sortierschlüssel einer Melde-/Teamnummer für die Checkin-Liste.
	 *
	 * @param teamname     freier Teamname (leer, wenn nicht verfügbar)
	 * @param sortNachname Nachname-Schlüssel (Nachname Spieler 1, Fallback Vorname)
	 */
	protected record SortSchluessel(String teamname, String sortNachname) {
		/** Leerer Schlüssel als Fallback für Nummern ohne Sortierdaten. */
		public static final SortSchluessel LEER = new SortSchluessel("", "");
	}

	/**
	 * Liefert je Melde-/Teamnummer den anzuzeigenden Namen (als Wert). Default leer; konkrete
	 * Subklassen (bzw. {@link AbstractTeilnehmerNamenCheckinListeSheet}) liefern die Namen.
	 */
	protected Map<Integer, String> namenNachNummer() throws GenerateException {
		return Map.of();
	}
}
