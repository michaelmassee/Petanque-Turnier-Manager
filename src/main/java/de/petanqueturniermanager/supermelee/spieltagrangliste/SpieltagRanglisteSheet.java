/*
 * Erstellung : 10.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
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
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.ergebnis.SpielrundeErgebnisLeser;
import de.petanqueturniermanager.supermelee.ergebnis.SpielrundeErgebnisLeser.RundenErgebnis;
import de.petanqueturniermanager.supermelee.ergebnis.SpielrundeErgebnisLeser.SpieltagErgebnisse;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
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
	private final SpielrundeSheet_Update aktuelleSpielrundeSheet;
	private final RangListeSpalte rangListeSpalte;
	private final SpieltagRanglisteFormatter ranglisteFormatter;
	private final RangListeSorter rangListeSorter;

	/** Wenn gesetzt, wird dieser Spieltag in doRun() verwendet statt getAktiveSpieltag(). */
	private final SpielTagNr spieltagNrFuerRefresh;

	protected SpielTagNr getSpielTagFuerRefresh() {
		return spieltagNrFuerRefresh;
	}

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
		nichtGespieltSpalteHeader();
		berechnungUndSchreiben(getXSpreadSheet(), anzSpielRunden);
		getSpielerSpalte().formatSpielrNrUndNamenspalten(false);
		getRangListeSpalte().insertHeaderInSheet(headerColor);
		ranglisteFormatter.formatDaten();
		ranglisteFormatter.formatDatenErrorGeradeUngerade(validateSpalte());
		int nichtGespieltGeradeFarbe = getKonfigurationSheet().getNichtGespieltHintergrundFarbeGerade();
		int nichtGespieltUnGeradeFarbe = getKonfigurationSheet().getNichtGespieltHintergrundFarbeUnGerade();
		formatNichtGespieltRunden(anzSpielRunden, nichtGespieltGeradeFarbe, nichtGespieltUnGeradeFarbe);
		getxCalculatable().calculate();
		rangListeSorter.doSort();
		getRangListeSpalte().upDateRanglisteSpalte();
		Position footerPos = ranglisteFormatter.addFooter().getPos();
		printBereichDefinieren(footerPos);
		processBoxinfo("processbox.header.festsetzen");
		SheetFreeze.from(getTurnierSheet()).anzZeilen(3).anzSpalten(3).doFreeze();
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(TurnierSystem.SUPERMELEE).ifPresent(
					k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
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

	/**
	 * Die Anzahl an Spielrunden im Rangliste-Sheet zahlen
	 *
	 * @return
	 * @throws GenerateException
	 */

	public int countNumberOfSpielrundenInSheet() throws GenerateException {
		return ranglisteFormatter.countAnzahlRunden();
	}

	/**
	 * Schreibt alle Spielrunden-Ergebnisse, Summen und Nicht-Gespielt-Flags als
	 * Werte (keine Formeln) per Block-Write in das Sheet. Liest die Ergebnisse
	 * der Spielrunden einmalig über {@link SpielrundeErgebnisLeser} ein.
	 * <p>
	 * Gemeinsame Methode für Vollaufbau {@link #generate(SpielTagNr)} und
	 * inkrementelles Update aus {@code SpieltagRanglisteSheetUpdate}.
	 */
	protected void berechnungUndSchreiben(XSpreadsheet sheet, int anzSpielRunden) throws GenerateException {
		processBoxinfo("processbox.spieltage.ergebnisse.einfuegen");

		if (anzSpielRunden < 1) {
			return;
		}

		List<Integer> spielerNrList = leseSpielerNrInSheetOrdnung();
		if (spielerNrList.isEmpty()) {
			return;
		}

		// Die vertikalen Ergebnisspalten der Spielrunde-Sheets sind formel-gestützte
		// Views auf die horizontalen Ergebnisse. Bei deaktiviertem AutoCalc können
		// sie veraltete Werte zeigen, wenn unmittelbar zuvor Ergebnisse geändert
		// wurden. Vor dem Block-Read einmal recalculaten.
		getxCalculatable().calculate();

		SpieltagErgebnisse ergebnisse = new SpielrundeErgebnisLeser(getWorkingSpreadsheet(), getSpieltagNr())
				.lese(anzSpielRunden);

		int nichtgespieltPlus = getKonfigurationSheet().getNichtGespielteRundePlus();
		int nichtgespieltMinus = getKonfigurationSheet().getNichtGespielteRundeMinus();
		int ersteSummeSpalte = getErsteSummeSpalte();
		int letzteSummenSpalte = ersteSummeSpalte + SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;
		int ngSpalte = nichtGespieltSpalteNr();
		int ersteDatenzeile = ERSTE_DATEN_ZEILE;
		int letzteDatenzeile = ersteDatenzeile + spielerNrList.size() - 1;

		RangeData runden = new RangeData();
		RangeData nichtGespielt = new RangeData();

		for (Integer spielerNr : spielerNrList) {
			SheetRunner.testDoCancelTask();
			RowData rundenRow = runden.addNewRow();
			int spielePlus = 0;
			int spieleMinus = 0;
			int punktePlus = 0;
			int punkteMinus = 0;
			boolean hatLuecke = false;

			for (int rundeNr = 1; rundeNr <= anzSpielRunden; rundeNr++) {
				Optional<RundenErgebnis> erg = ergebnisse.ergebnis(rundeNr, spielerNr);
				int plus;
				int minus;
				if (erg.isPresent()) {
					plus = erg.get().plus();
					minus = erg.get().minus();
				} else {
					plus = nichtgespieltPlus;
					minus = nichtgespieltMinus;
					hatLuecke = true;
				}
				if (ergebnisse.istRundeNichtGespielt(rundeNr)) {
					// Runde komplett leer – die Vertikalen liefern zwar 0/0, semantisch
					// hat dieser Spieler aber nicht gespielt.
					hatLuecke = true;
				}
				rundenRow.newInt(plus);
				rundenRow.newInt(minus);
				punktePlus += plus;
				punkteMinus += minus;
				if (plus > minus) {
					spielePlus++;
				} else if (minus > plus) {
					spieleMinus++;
				}
			}
			rundenRow.newInt(spielePlus);
			rundenRow.newInt(spieleMinus);
			rundenRow.newInt(spielePlus - spieleMinus);
			rundenRow.newInt(punktePlus);
			rundenRow.newInt(punkteMinus);
			rundenRow.newInt(punktePlus - punkteMinus);

			nichtGespielt.addNewRow().newString(hatLuecke ? "x" : "");
		}

		RangeHelper.from(this,
				RangePosition.from(ERSTE_SPIELRUNDE_SPALTE, ersteDatenzeile, letzteSummenSpalte, letzteDatenzeile))
				.setDataInRange(runden);
		RangeHelper.from(this,
				RangePosition.from(ngSpalte, ersteDatenzeile, ngSpalte, letzteDatenzeile))
				.setDataInRange(nichtGespielt);
	}

	private List<Integer> leseSpielerNrInSheetOrdnung() throws GenerateException {
		int letzteDatenzeile = getSpielerSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();
		if (letzteDatenzeile < ERSTE_DATEN_ZEILE) {
			return new ArrayList<>();
		}
		RangeData data = RangeHelper.from(this,
				RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, letzteDatenzeile))
				.getDataFromRange();
		List<Integer> result = new ArrayList<>();
		for (RowData row : data) {
			int nr = row.get(0).getIntVal(-1);
			if (nr < 1) {
				break;
			}
			result.add(nr);
		}
		return result;
	}

	private void nichtGespieltSpalteHeader() throws GenerateException {
		int nichtGespieltSpalte = nichtGespieltSpalteNr();
		ColumnProperties columnProperties = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER)
				.isVisible(false);
		StringCellValue header = StringCellValue
				.from(getXSpreadSheet(), Position.from(nichtGespieltSpalte, ERSTE_DATEN_ZEILE - 1))
				.addColumnProperties(columnProperties)
				.setValue("NG");
		getSheetHelper().setStringValueInCell(header);
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
	 * @return null when not found <br>
	 * =VLOOKUP<br>
	 * =SVERWEIS(A65;$'5. Spieltag Summe'.$A3:$Q1000;12;0)
	 * @throws GenerateException
	 *
	 */
	public String formulaSverweisAufSpielePlus(SpielTagNr spieltagNr, String spielrNrAdresse) throws GenerateException {
		return formulaSverweisAufSummeSpalte(spieltagNr, 0, spielrNrAdresse);
	}

	/**
	 *
	 * @param spieltagNr = welchen Spieltag ?
	 * @param summeSpalte = erste spalte = 0 = SpielePlusSpalte
	 * @param spielrNrAdresse = die Adresse vom Spielrnr im Anfragende Sheet
	 * @return null when not found <br>
	 * =VLOOKUP<br>
	 * =SVERWEIS(A65;$'5. Spieltag Summe'.$A3:$Q1000;12;0)
	 * @throws GenerateException
	 *
	 */
	public String formulaSverweisAufSummeSpalte(SpielTagNr spieltagNr, int summeSpalte, String spielrNrAdresse)
			throws GenerateException {
		int ersteSummeSpalte = getErsteSummeSpalte(spieltagNr);

		if (ersteSummeSpalte > -1) {
			// gefunden
			int returnSpalte = ersteSummeSpalte + summeSpalte;

			String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
			String letzteZelleAddress = Position.from(returnSpalte, 999).getAddressWith$();
			// erste spalte = 1
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
		// letzte Zeile ?
		RangePosition searchRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, 9999);
		Position lastNotEmptyPos = RangeSearchHelper.from(this, searchRange).searchLastNotEmptyInSpalte();

		// daten in array einlesen
		RangePosition spielNrRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE,
				lastNotEmptyPos.getZeile());
		RangeData dataFromRange = RangeHelper.from(this, spielNrRange).getDataFromRange();

		for (RowData zeile : dataFromRange) {
			int spielerNr = zeile.get(0).getIntVal(-1);
			if (spielerNr < 1) {
				break; // fertig
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

	/**
	 * spalte mit sortierdaten rangliste
	 *
	 * @return
	 * @throws GenerateException
	 */
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
		if (letzteDatenzeile >= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
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
