/**
 * Erstellung : 10.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellHoriJustify;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellstyle.NichtGespieltHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.NichtGespieltHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.rangliste.ISpielTagRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Update;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;
import de.petanqueturniermanager.toolbar.TurnierModus;

public class SpieltagRanglisteSheet extends SheetRunner implements ISpielTagRangliste {

	public static final int RANGLISTE_SPALTE = SpieltagRanglisteDelegate.RANGLISTE_SPALTE;
	public static final int ERSTE_SPIELRUNDE_SPALTE = SpieltagRanglisteDelegate.ERSTE_SPIELRUNDE_SPALTE;
	public static final int ERSTE_DATEN_ZEILE = SpieltagRanglisteDelegate.ERSTE_DATEN_ZEILE;
	public static final int SPIELER_NR_SPALTE = SpieltagRanglisteDelegate.SPIELER_NR_SPALTE;
	public static final String KOPFDATEN_SUMME = "Summe";
	public static final String KOPFDATEN_SUMME_SPIELE = "Spiele";
	public static final String KOPFDATEN_SUMME_PUNKTE = "Punkte";

	public static final int ANZAHL_SPALTEN_IN_SPIELRUNDE = 2;

	public static final int ERSTE_SORTSPALTE_OFFSET = 2; // zur letzte spalte = PUNKTE_DIV_OFFS

	private final SpieltagRanglisteDelegate delegate;
	final SpielrundeSheet_Update aktuelleSpielrundeSheet;
	private final RangListeSpalte rangListeSpalte;
	private final SpieltagRanglisteFormatter ranglisteFormatter;
	private final RangListeSorter rangListeSorter;

	/** Wenn gesetzt, wird dieser Spieltag in doRun() verwendet statt getAktiveSpieltag(). */
	final SpielTagNr spieltagNrFuerRefresh;

	public SpieltagRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, null);
	}

	/**
	 * Konstruktor für den automatischen Refresh-Listener.
	 * Der übergebene {@code spieltagNr} wird in {@code doRun()} direkt verwendet —
	 * unabhängig vom konfigurierten aktiven Spieltag.
	 */
	public SpieltagRanglisteSheet(WorkingSpreadsheet workingSpreadsheet, SpielTagNr spieltagNr) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE, "Spieltag Rangliste");
		this.spieltagNrFuerRefresh = spieltagNr;
		delegate = new SpieltagRanglisteDelegate(this);
		aktuelleSpielrundeSheet = new SpielrundeSheet_Update(workingSpreadsheet);
		rangListeSpalte = new RangListeSpalte(RANGLISTE_SPALTE, this);
		ranglisteFormatter = new SpieltagRanglisteFormatter(this, ANZAHL_SPALTEN_IN_SPIELRUNDE, getSpielerSpalte(),
				ERSTE_SPIELRUNDE_SPALTE, getKonfigurationSheet());
		rangListeSorter = new RangListeSorter(this);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return delegate.getSheet(delegate.getSpieltagNr());
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	public SpielTagNr getSpieltagNr() {
		return delegate.getSpieltagNr();
	}

	public void setSpieltagNr(SpielTagNr spieltagNr) {
		delegate.setSpieltagNr(spieltagNr);
	}

	protected MeldungenSpalte<SpielerMeldungen, Spieler> getSpielerSpalte() {
		return delegate.getSpielerSpalte();
	}

	public String getSheetName(SpielTagNr spielTagNr) {
		return delegate.getSheetName(spielTagNr);
	}

	public XSpreadsheet getSheet(SpielTagNr spielTagNr) throws GenerateException {
		return delegate.getSheet(spielTagNr);
	}

	protected List<Position> getRanglisteSpalten(int ersteSpalteEndsumme, int ersteDatenZeile) {
		return delegate.getRanglisteSpalten(ersteSpalteEndsumme, ersteDatenZeile);
	}

	@Override
	protected void doRun() throws GenerateException {
		getxCalculatable().enableAutomaticCalculation(false); // speed up
		SpielTagNr nr = spieltagNrFuerRefresh != null
				? spieltagNrFuerRefresh
				: getKonfigurationSheet().getAktiveSpieltag();
		generate(nr);
	}

	public void generate(SpielTagNr spielTagNr) throws GenerateException {
		setSpieltagNr(spielTagNr);

		MeldeListeSheet_Update meldeliste = new MeldeListeSheet_Update(getWorkingSpreadsheet());
		meldeliste.setSpielTag(getSpieltagNr());
		aktuelleSpielrundeSheet.setSpielTag(getSpieltagNr());

		int anzSpielRunden = aktuelleSpielrundeSheet.countNumberOfSpielRundenSheets(getSpieltagNr());

		if (anzSpielRunden == 0) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.spieltagrangliste"))
					.message(I18n.get("msg.text.keine.spielrunden")).show();
			return;
		}

		// neu erstellen
		NewSheet.from(this, getSheetName(getSpieltagNr()),
				SheetMetadataHelper.schluesselSpieltagRangliste(spielTagNr.getNr()))
				.pos(DefaultSheetPos.SUPERMELEE_WORK).hideGrid().setActiv()
				.tabColor(getKonfigurationSheet().getRanglisteTabFarbe())
				.forceCreate().spielTagPageStyle(getSpieltagNr()).create();

		Integer headerColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		getSpielerSpalte().alleAktiveUndAusgesetzteMeldungenAusmeldelisteEinfuegen(meldeliste);
		getSpielerSpalte().insertHeaderInSheet(headerColor);
		ranglisteFormatter.updateHeader();

		rangListeSorter.insertSortValidateSpalte(false);
		rangListeSorter.insertManuelsortSpalten(false);
		nichtGespieltSpalteEinrichten();
		ergebnisseAlsWerteEinfuegen(anzSpielRunden);
		getSpielerSpalte().formatSpielrNrUndNamenspalten(false);
		getRangListeSpalte().upDateRanglisteSpalte();
		getRangListeSpalte().insertHeaderInSheet(headerColor);
		ranglisteFormatter.formatDaten();
		ranglisteFormatter.formatDatenErrorGeradeUngerade(validateSpalte());
		int nichtGespieltGeradeFarbe = getKonfigurationSheet().getNichtGespieltHintergrundFarbeGerade();
		int nichtGespieltUnGeradeFarbe = getKonfigurationSheet().getNichtGespieltHintergrundFarbeUnGerade();
		formatNichtGespieltRunden(anzSpielRunden, nichtGespieltGeradeFarbe, nichtGespieltUnGeradeFarbe);
		getxCalculatable().calculate();
		rangListeSorter.doSort();
		Position footerPos = ranglisteFormatter.addFooter().getPos();
		printBereichDefinieren(footerPos);
		processBoxinfo("processbox.header.festsetzen");
		SheetFreeze.from(getTurnierSheet()).anzZeilen(3).anzSpalten(3).doFreeze();
		blattschutzSchuetzen();
	}

	protected void blattschutzEntsprerren() {
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE)
					.ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
		}
	}

	protected void blattschutzSchuetzen() {
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE)
					.ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
		}
	}

	private void printBereichDefinieren(Position footerPos) throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		Position rechtsUnten = Position.from(getLetzteSpalte(), footerPos.getZeile());
		Position linksOben = Position.from(SPIELER_NR_SPALTE,
				AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	public int countNumberOfSpielrundenInSheet() throws GenerateException {
		return ranglisteFormatter.countAnzahlRunden();
	}

	/**
	 * Richtet den Header und die Spalten-Eigenschaften der NichtGespielt-Spalte ein.
	 * Wird nur beim Vollaufbau (generate) aufgerufen.
	 */
	private void nichtGespieltSpalteEinrichten() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		int nichtGespieltSpalte = nichtGespieltSpalteNr();
		ColumnProperties columnProperties = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER)
				.isVisible(false);
		StringCellValue header = StringCellValue
				.from(sheet, Position.from(nichtGespieltSpalte, ERSTE_DATEN_ZEILE - 1))
				.addColumnProperties(columnProperties)
				.setValue("NG");
		getSheetHelper().setStringValueInCell(header);
	}

	/**
	 * Liest alle Spielrunden eines Spieltags als Block ein und liefert je Spieler
	 * eine Liste von {@link RundeErgebnis}-Einträgen (Index 0 = Runde 1).
	 * Spieler, die einer Runde nicht zugeteilt sind oder für die kein Ergebnis
	 * eingetragen wurde, erhalten ein {@link RundeErgebnis#nichtGespielt}-Objekt.
	 */
	private Map<Integer, List<RundeErgebnis>> leseAlleRundenDaten(int anzSpielRunden) throws GenerateException {
		int nichtgespieltPlus = getKonfigurationSheet().getNichtGespielteRundePlus();
		int nichtgespieltMinus = getKonfigurationSheet().getNichtGespielteRundeMinus();

		List<Integer> spielerNrListe = getSpielerSpalte().getSpielerNrList();
		Map<Integer, List<RundeErgebnis>> alleRundenDaten = new HashMap<>();
		for (int spielerNr : spielerNrListe) {
			List<RundeErgebnis> rundenErgebnisse = new ArrayList<>(anzSpielRunden);
			for (int i = 0; i < anzSpielRunden; i++) {
				rundenErgebnisse.add(RundeErgebnis.nichtGespielt(nichtgespieltPlus, nichtgespieltMinus));
			}
			alleRundenDaten.put(spielerNr, rundenErgebnisse);
		}

		XSpreadsheetDocument xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();

		for (int runde = 1; runde <= anzSpielRunden; runde++) {
			SheetRunner.testDoCancelTask();
			XSpreadsheet rundeSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
					SheetMetadataHelper.schluesselSupermeleeSpielrunde(getSpieltagNr().getNr(), runde),
					aktuelleSpielrundeSheet.getSheetName(getSpieltagNr(), SpielRundeNr.from(runde)));
			if (rundeSheet == null) {
				continue;
			}

			// Block-Read: Spalten H..Q (ERSTE_SPALTE_ERGEBNISSE=7 .. ERSTE_SPIELERNR_SPALTE+5=16)
			RangePosition readRange = RangePosition.from(
					SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE,
					SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
					SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE + 5,
					SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE + 999);
			RangeData rows = RangeHelper.from(rundeSheet, xDoc, readRange).getDataFromRange();

			int rundeIdx = runde - 1;
			for (RowData row : rows) {
				if (row.size() < 10) {
					break;
				}
				// offset 0=ergA(H), 1=ergB(I), 4-6=TeamA(L,M,N), 7-9=TeamB(O,P,Q)
				int ergA = row.get(0).getIntVal(-1);
				int ergB = row.get(1).getIntVal(-1);
				boolean ergebnisEingetragen = ergA >= 0 && ergB >= 0;

				for (int i = 4; i <= 6; i++) {
					int spielerNr = row.get(i).getIntVal(0);
					if (spielerNr > 0 && alleRundenDaten.containsKey(spielerNr)) {
						alleRundenDaten.get(spielerNr).set(rundeIdx,
								ergebnisEingetragen
										? RundeErgebnis.gespielt(ergA, ergB)
										: RundeErgebnis.nichtGespielt(nichtgespieltPlus, nichtgespieltMinus));
					}
				}
				for (int i = 7; i <= 9; i++) {
					int spielerNr = row.get(i).getIntVal(0);
					if (spielerNr > 0 && alleRundenDaten.containsKey(spielerNr)) {
						// Team B: plus und minus getauscht
						alleRundenDaten.get(spielerNr).set(rundeIdx,
								ergebnisEingetragen
										? RundeErgebnis.gespielt(ergB, ergA)
										: RundeErgebnis.nichtGespielt(nichtgespieltPlus, nichtgespieltMinus));
					}
				}
			}
		}

		return alleRundenDaten;
	}

	/**
	 * Berechnet alle Spielrunden-Ergebnisse und Summen in Java und schreibt sie
	 * als Werte (keine Formeln) per Block-Write in das Sheet.
	 */
	protected void ergebnisseAlsWerteEinfuegen(int anzSpielRunden) throws GenerateException {
		processBoxinfo("processbox.spieltage.ergebnisse.einfuegen");

		if (anzSpielRunden < 1) {
			return;
		}

		int nichtgespieltPlus = getKonfigurationSheet().getNichtGespielteRundePlus();
		int nichtgespieltMinus = getKonfigurationSheet().getNichtGespielteRundeMinus();
		Map<Integer, List<RundeErgebnis>> alleRundenDaten = leseAlleRundenDaten(anzSpielRunden);
		List<Integer> spielerNrListe = getSpielerSpalte().getSpielerNrList();
		int nichtGespieltSpalte = nichtGespieltSpalteNr();

		RangeData spielrundenUndSummenBlock = new RangeData();
		RangeData nichtGespieltBlock = new RangeData();

		for (int spielerNr : spielerNrListe) {
			SheetRunner.testDoCancelTask();
			List<RundeErgebnis> rundenErgebnisse = alleRundenDaten.getOrDefault(spielerNr, Collections.emptyList());

			RowData row = spielrundenUndSummenBlock.addNewRow();
			int spielePlus = 0;
			int spieleMinus = 0;
			int punktePlus = 0;
			int punkteMinus = 0;
			boolean hatNichtGespielt = false;

			for (int rundeIdx = 0; rundeIdx < anzSpielRunden; rundeIdx++) {
				RundeErgebnis erg = rundeIdx < rundenErgebnisse.size()
						? rundenErgebnisse.get(rundeIdx)
						: RundeErgebnis.nichtGespielt(nichtgespieltPlus, nichtgespieltMinus);

				row.newInt(erg.plus());
				row.newInt(erg.minus());

				if (erg.gespielt()) {
					if (erg.plus() > erg.minus()) spielePlus++;
					if (erg.minus() > erg.plus()) spieleMinus++;
				} else {
					hatNichtGespielt = true;
				}
				punktePlus += erg.plus();
				punkteMinus += erg.minus();
			}

			row.newInt(spielePlus);
			row.newInt(spieleMinus);
			row.newInt(spielePlus - spieleMinus);
			row.newInt(punktePlus);
			row.newInt(punkteMinus);
			row.newInt(punktePlus - punkteMinus);

			nichtGespieltBlock.addNewRow().newString(hatNichtGespielt ? "x" : "");
		}

		if (!spielerNrListe.isEmpty()) {
			processBoxinfo("processbox.summenspalten.aktualisieren");
			RangeHelper.from(this,
					spielrundenUndSummenBlock.getRangePosition(
							Position.from(ERSTE_SPIELRUNDE_SPALTE, ERSTE_DATEN_ZEILE)))
					.setDataInRange(spielrundenUndSummenBlock);

			RangeHelper.from(this,
					nichtGespieltBlock.getRangePosition(
							Position.from(nichtGespieltSpalte, ERSTE_DATEN_ZEILE)))
					.setDataInRange(nichtGespieltBlock);
		}
	}

	private void formatNichtGespieltRunden(int anzSpielRunden, int nichtGespieltGeradeFarbe,
			int nichtGespieltUnGeradeFarbe) throws GenerateException {
		if (anzSpielRunden < 1) {
			return;
		}

		int letzteDatenzeile = getSpielerSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();
		int validSpalteNr = validateSpalte();
		String formulaSortError = "LEN(TRIM(INDIRECT(ADDRESS(ROW();" + (validSpalteNr + 1) + "))))>0";

		int ersteSpalteVertikaleErgebnisse = SpielrundeSheetKonstanten.ERSTE_SPALTE_VERTIKALE_ERGEBNISSE;
		int spielrundeSheetErsteDatenzeile = SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
		Position erstePos = Position.from(ersteSpalteVertikaleErgebnisse, spielrundeSheetErsteDatenzeile);
		Position letztePosPlusPunkte = Position.from(SpielrundeSheetKonstanten.SPALTE_VERTIKALE_ERGEBNISSE_PLUS,
				1000 + spielrundeSheetErsteDatenzeile);
		String suchMatrixPlusPunkte = erstePos.getAddressWith$() + ":" + letztePosPlusPunkte.getAddressWith$();
		String verweisAufSpalteSpielerNr = "INDIRECT(ADDRESS(ROW();" + (SPIELER_NR_SPALTE + 1) + ";4))";

		int geradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeGerade();
		int ungeradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade();

		for (int spielRunde = 1; spielRunde <= anzSpielRunden; spielRunde++) {
			SheetRunner.testDoCancelTask();

			String formulaSheetName = "$'"
					+ aktuelleSpielrundeSheet.getSheetName(getSpieltagNr(), SpielRundeNr.from(spielRunde)) + "'.";
			String isnaFormula = "ISNA(VLOOKUP(" + verweisAufSpalteSpielerNr + ";" + formulaSheetName
					+ suchMatrixPlusPunkte + ";2;0))";

			int plusSpalte = ERSTE_SPIELRUNDE_SPALTE + ((spielRunde - 1) * ANZAHL_SPALTEN_IN_SPIELRUNDE);
			RangePosition rundenRange = RangePosition.from(plusSpalte, ERSTE_DATEN_ZEILE, plusSpalte + 1,
					letzteDatenzeile);

			SheetHelper.faerbeZeilenAbwechselnd(this, rundenRange, geradeColor, ungeradeColor);
			ConditionalFormatHelper cfHelper = ConditionalFormatHelper.from(this, rundenRange).clear();
			cfHelper.formula1(formulaSortError).operator(ConditionOperator.FORMULA)
					.style(new FehlerStyle()).applyAndDoReset();
			cfHelper.formula1("AND(ISEVEN(ROW());" + isnaFormula + ")").isFormula()
					.style(new NichtGespieltHintergrundFarbeGeradeStyle(nichtGespieltGeradeFarbe)).applyAndDoReset();
			cfHelper.formula1("AND(ISODD(ROW());" + isnaFormula + ")").isFormula()
					.style(new NichtGespieltHintergrundFarbeUnGeradeStyle(nichtGespieltUnGeradeFarbe)).applyAndDoReset();
		}
	}

	/**
	 * @param spieltagNr = welchen Spieltag ?
	 * @param spielrNrAdresse = die Adresse vom Spielrnr im Anfragende Sheet
	 * @return null when not found
	 * @throws GenerateException
	 */
	public String formulaSverweisAufSpielePlus(SpielTagNr spieltagNr, String spielrNrAdresse) throws GenerateException {
		return formulaSverweisAufSummeSpalte(spieltagNr, 0, spielrNrAdresse);
	}

	/**
	 * @param spieltagNr = welchen Spieltag ?
	 * @param summeSpalte = erste spalte = 0 = SpielePlusSpalte
	 * @param spielrNrAdresse = die Adresse vom Spielrnr im Anfragende Sheet
	 * @return null when not found
	 * @throws GenerateException
	 */
	public String formulaSverweisAufSummeSpalte(SpielTagNr spieltagNr, int summeSpalte, String spielrNrAdresse)
			throws GenerateException {
		int ersteSummeSpalte = getErsteSummeSpalte(spieltagNr);

		if (ersteSummeSpalte > -1) {
			int returnSpalte = ersteSummeSpalte + summeSpalte;
			String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
			String letzteZelleAddress = Position.from(returnSpalte, 999).getAddressWith$();
			return "VLOOKUP(" + spielrNrAdresse + ";$'" + getSheetName(spieltagNr) + "'." + ersteZelleAddress + ":"
					+ letzteZelleAddress + ";" + (returnSpalte + 1) + ";0)";
		}
		return null;
	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		return getErsteSummeSpalte(getSpieltagNr());
	}

	public int getErsteSummeSpalte(SpielTagNr spieltag) throws GenerateException {
		checkNotNull(spieltag);
		int anzSpielRunden = aktuelleSpielrundeSheet.countNumberOfSpielRundenSheets(spieltag);
		return ERSTE_SPIELRUNDE_SPALTE + (anzSpielRunden * 2);
	}

	public List<Integer> getSpielerNrList(SpielTagNr spielTagNr) throws GenerateException {
		setSpieltagNr(checkNotNull(spielTagNr));

		List<Integer> spielerNrlist = new ArrayList<>();
		RangePosition searchRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, 9999);
		Position lastNotEmptyPos = RangeSearchHelper.from(this, searchRange).searchLastNotEmptyInSpalte();

		RangePosition spielNrRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE,
				lastNotEmptyPos.getZeile());
		RangeData dataFromRange = RangeHelper.from(this, spielNrRange).getDataFromRange();

		for (RowData zeile : dataFromRange) {
			int spielerNr = zeile.get(0).getIntVal(-1);
			if (spielerNr < 1) {
				break;
			}
			spielerNrlist.add(spielerNr);
		}
		return spielerNrlist;
	}

	public int countNumberOfRanglisten() throws GenerateException {
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		int anz = 0;
		for (int nr = 1; nr <= 99; nr++) {
			boolean gefunden = SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.schluesselSpieltagRangliste(nr)).isPresent();
			if (!gefunden) {
				gefunden = getSheetHelper().findByName(SpieltagRanglisteDelegate.legacySheetName(nr)) != null;
			}
			if (gefunden) {
				anz++;
			} else {
				break;
			}
		}
		return anz;
	}

	@Override
	public int getManuellSortSpalte() throws GenerateException {
		return getLetzteSpalte() + ERSTE_SORTSPALTE_OFFSET;
	}

	public List<SpielerSpieltagErgebnis> spielTagErgebnisseEinlesen() throws GenerateException {
		List<SpielerSpieltagErgebnis> spielTagErgebnisse = new ArrayList<>();
		SpielTagNr spieltagNr = getSpieltagNr();

		for (int spielerNr : getSpielerSpalte().getSpielerNrList()) {
			SpielerSpieltagErgebnis erg = spielerErgebnisseEinlesen(spieltagNr, spielerNr);
			if (erg != null) {
				spielTagErgebnisse.add(erg);
			}
		}

		return spielTagErgebnisse;
	}

	public SpielerSpieltagErgebnis spielerErgebnisseEinlesen(SpielTagNr spieltag, int spielrNr)
			throws GenerateException {
		int spielerZeile = getSpielerSpalte().getSpielerZeileNr(spielrNr);

		if (spielerZeile < ERSTE_DATEN_ZEILE) {
			return null;
		}

		XSpreadsheet spieltagSheet = getXSpreadSheet();
		if (spieltagSheet == null) {
			return null;
		}

		int ersteSpieltagSummeSpalte = getErsteSummeSpalte();
		Position spielePlusSumme = Position.from(ersteSpieltagSummeSpalte, spielerZeile);
		SpielerSpieltagErgebnis erg = SpielerSpieltagErgebnis.from(spieltag, spielrNr);

		erg.setSpielPlus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme))
				.setPosSpielPlus(spielePlusSumme);
		erg.setSpielMinus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlusEins()))
				.setPosSpielMinus(spielePlusSumme);
		erg.setPunktePlus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlus(2)))
				.setPosPunktePlus(spielePlusSumme);
		erg.setPunkteMinus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlusEins()))
				.setPosPunkteMinus(spielePlusSumme);

		return erg;
	}

	public void clearAll() throws GenerateException {
		int letzteDatenzeile = getSpielerSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();
		if (letzteDatenzeile >= ERSTE_DATEN_ZEILE) {
			RangePosition range = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, getManuellSortSpalte(),
					letzteDatenzeile);
			RangeHelper.from(this, range).clearRange();
		}
	}

	// Delegates
	// --------------------------

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return getSpielerSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int getErsteDatenZiele() {
		return getSpielerSpalte().getErsteDatenZiele();
	}

	@Override
	public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
		return getSpielerSpalte().sucheLetzteZeileMitSpielerNummer();
	}

	public RangListeSpalte getRangListeSpalte() {
		return rangListeSpalte;
	}

	@Override
	public int getAnzahlRunden() throws GenerateException {
		return aktuelleSpielrundeSheet.countNumberOfSpielRundenSheets(getSpieltagNr());
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		return getErsteSummeSpalte() + PUNKTE_DIV_OFFS;
	}

	public void isErrorInSheet() throws GenerateException {
		rangListeSorter.isErrorInSheet();
	}

	protected RangListeSorter getRangListeSorter() {
		return rangListeSorter;
	}

	@Override
	public List<Position> getRanglisteSpalten() throws GenerateException {
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		return getRanglisteSpalten(ersteSpalteEndsumme, ERSTE_DATEN_ZEILE);
	}

	@Override
	public int validateSpalte() throws GenerateException {
		return getManuellSortSpalte() + PUNKTE_DIV_OFFS;
	}

	public int nichtGespieltSpalteNr() throws GenerateException {
		return validateSpalte() + 1;
	}

	@Override
	public int getErsteSpalte() throws GenerateException {
		return SPIELER_NR_SPALTE;
	}

	@Override
	public void calculateAll() {
		getxCalculatable().calculateAll();
	}

	public SpielrundeSheet_Update getAktuelleSpielrundeSheet() {
		return aktuelleSpielrundeSheet;
	}

}
