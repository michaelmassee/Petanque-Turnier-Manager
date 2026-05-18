/*
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.IMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class MeldeListeHelper<MLD_LIST_TYPE, MLDTYPE> implements MeldeListeKonstanten {

	private final IMeldeliste<MLD_LIST_TYPE, MLDTYPE> meldeListe;
	private final String metadatenSchluessel;

	public MeldeListeHelper(IMeldeliste<MLD_LIST_TYPE, MLDTYPE> newMeldeListe, String metadatenSchluessel) {
		meldeListe = checkNotNull(newMeldeListe);
		checkArgument(StringUtils.isNotBlank(metadatenSchluessel));
		this.metadatenSchluessel = metadatenSchluessel;
	}

	public void insertFormulaFuerDoppelteSpielerNrGeradeUngradeFarbe(int letzteDatenZeile, ISheet sheet,
			MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle) throws GenerateException {

		RangePosition nrSetPosRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE,
				letzteDatenZeile);
		String conditionfindDoppeltNr = "COUNTIF(" + Position.from(SPIELER_NR_SPALTE, 0).getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		ConditionalFormatHelper.from(sheet, nrSetPosRange).clear()
				.formulaIsText().styleIsFehler().applyAndDoReset()
				.formula1(conditionfindDoppeltNr).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset()
				// nr muss >0 und <999 sein
				.formula1("0").formula2("" + MeldungenSpalte.MAX_ANZ_MELDUNGEN)
				.operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset();
		SheetHelper.faerbeZeilenAbwechselnd(sheet, nrSetPosRange,
				meldungenHintergrundFarbeGeradeStyle.getFarbe(),
				meldungenHintergrundFarbeUnGeradeStyle.getFarbe());

	}

	public void insertFormulaFuerDoppelteNamenGeradeUngradeFarbe(int erstNameSpalte, int letzteNamespalte,
			int letzteDatenZeile, ISheet sheet,
			MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle) throws GenerateException {
		// -----------------------------------------------
		// Spieler Namen prüfen auf doppelte namen
		// -----------------------------------------------
		RangePosition nameSetPosRange = RangePosition.from(erstNameSpalte, ERSTE_DATEN_ZEILE, letzteNamespalte,
				letzteDatenZeile);
		String conditionfindDoppeltNamen;
		if (erstNameSpalte == letzteNamespalte) {
			conditionfindDoppeltNamen = "COUNTIF(" + Position.from(erstNameSpalte, 0).getSpalteAddressWith$() + ";"
					+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		} else {
			// Mehrere Namens-Spalten (z.B. Vorname+Nachname): Kombination per COUNTIFS prüfen.
			// Spalten absolut, Zeile relativ ($B3 / $C3) damit die Formel pro Zeile mitläuft.
			final int ankerZeile = ERSTE_DATEN_ZEILE + 1; // 1-basiert für Zell-Adresse
			final int endZeile = MeldungenSpalte.MAX_ANZ_MELDUNGEN + 1;
			StringBuilder sb = new StringBuilder("COUNTIFS(");
			for (int sp = erstNameSpalte; sp <= letzteNamespalte; sp++) {
				if (sp > erstNameSpalte) {
					sb.append(';');
				}
				String spalteAbs = Position.from(sp, 0).getSpalteAddressWith$();
				sb.append(spalteAbs).append(ankerZeile).append(":").append(spalteAbs).append(endZeile).append(';')
						.append(spalteAbs).append(ankerZeile);
			}
			sb.append(")>1");
			conditionfindDoppeltNamen = sb.toString();
		}
		ConditionalFormatHelper.from(sheet, nameSetPosRange).clear()
				.formula1(conditionfindDoppeltNamen).operator(ConditionOperator.FORMULA).styleIsFehler()
				.applyAndDoReset();
		SheetHelper.faerbeZeilenAbwechselnd(sheet, nameSetPosRange,
				meldungenHintergrundFarbeGeradeStyle.getFarbe(),
				meldungenHintergrundFarbeUnGeradeStyle.getFarbe());
		// -----------------------------------------------
	}

	public void insertFormulaSetzpositionGeradeUngradeFarbe(int letzteDatenZeile, ISheet sheet,
			MeldungenHintergrundFarbeGeradeStyle geradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle ungeradeStyle) throws GenerateException {
		int setzposSpalte = setzPositionSpalte();
		var setzposRangePos = RangePosition.from(setzposSpalte, ERSTE_DATEN_ZEILE, setzposSpalte, letzteDatenZeile);
		ConditionalFormatHelper.from(sheet, setzposRangePos).clear()
				.formula1("0").formula2("90").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
				.formulaIsText().styleIsFehler().applyAndDoReset();
		SheetHelper.faerbeZeilenAbwechselnd(sheet, setzposRangePos,
				geradeStyle.getFarbe(), ungeradeStyle.getFarbe());
	}

	public void insertFormulaSpieltageSpaltenGeradeUngradeFarbe(int letzteDatenZeile, int letzteSpieltagSpalte,
			ISheet sheet, MeldungenHintergrundFarbeGeradeStyle geradeStyle,
			MeldungenHintergrundFarbeUnGeradeStyle ungeradeStyle) throws GenerateException {
		var spieltageRangePos = RangePosition.from(ersteSpieltagSpalte(), ERSTE_DATEN_ZEILE,
				letzteSpieltagSpalte, letzteDatenZeile);
		ConditionalFormatHelper.from(sheet, spieltageRangePos).clear()
				.formula1("0").formula2("2").operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
				.formulaIsText().styleIsFehler().applyAndDoReset();
		SheetHelper.faerbeZeilenAbwechselnd(sheet, spieltageRangePos,
				geradeStyle.getFarbe(), ungeradeStyle.getFarbe());
	}

	/**
	 *
	 * @param spalteNr 0 = erste spalte
	 * @param isAscending
	 * @throws GenerateException
	 */

	public void doSort(int spalteNr, boolean isAscending) throws GenerateException {
		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile > ERSTE_DATEN_ZEILE) { // daten vorhanden
			RangePosition rangeToSort = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE,
					meldeListe.letzteSpielTagSpalte(), letzteSpielZeile);
			SortHelper.from(meldeListe, rangeToSort).spalteToSort(spalteNr).aufSteigendSortieren(isAscending).doSort();
		}
	}

	/**
	 * prüft auf doppelte spieler nr oder namen
	 *
	 * @return
	 * @throws GenerateException wenn doppelt daten
	 */
	public void testDoppelteMeldungen() throws GenerateException {
		meldeListe.processBoxinfo("processbox.meldeliste.pruefe.doppelte");
		XSpreadsheet xSheet = getXSpreadSheet();

		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile <= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // keine Daten
		}

		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		// doppelte spieler Nummer entfernen !?!?!
		HashSet<Integer> spielrNrInSheet = new HashSet<>();
		HashSet<String> spielrNamenInSheet = new HashSet<>();

		int spielrNr;
		String spielerName;
		NumberCellValue errCelVal = NumberCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE))
				.setCharColor(ColorHelper.CHAR_COLOR_RED);

		StringCellValue errStrCelVal = StringCellValue
				.from(xSheet,
						Position.from(meldeListe.getMeldungenSpalte().getErsteMeldungNameSpalte(), ERSTE_DATEN_ZEILE))
				.setCharColor(ColorHelper.CHAR_COLOR_RED);

		int letzteNamensSpalte = meldeListe.getMeldungenSpalte().getLetzteMeldungNameSpalte();

		for (int spielerZeilecntr = ERSTE_DATEN_ZEILE; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			// -------------------
			// Spieler nr testen
			// -------------------
			spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet,
					Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr > 0) {
				if (spielrNrInSheet.contains(spielrNr)) {
					// RED Color
					meldeListe.getSheetHelper()
							.setNumberValueInCell(errCelVal.setValue((double) spielrNr).zeile(spielerZeilecntr));
					throw new GenerateException(I18n.get("error.meldeliste.spieler.nr", spielrNr));
				}
				spielrNrInSheet.add(spielrNr);
			} else {
				// nr ist ungültig einfach löschen
				meldeListe.getSheetHelper().clearValInCell(xSheet, Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			}

			// -------------------
			// spieler namen testen
			// -------------------
			// Bei 2 Namens-Spalten (Supermelee Vorname+Nachname) Kombination als Dup-Schlüssel.
			spielerName = meldeListe.getMeldungenSpalte().leseSpielerNameZeile(xSheet, spielerZeilecntr);

			if (StringUtils.isNotEmpty(spielerName)) {
				if (spielrNamenInSheet.contains(cleanUpSpielerName(spielerName))) {
					// RED Color in alle Namens-Spalten
					for (int sp = meldeListe.getMeldungenSpalte().getErsteMeldungNameSpalte();
							sp <= letzteNamensSpalte; sp++) {
						String zellValue = meldeListe.getSheetHelper().getTextFromCell(xSheet,
								Position.from(sp, spielerZeilecntr));
						meldeListe.getSheetHelper().setStringValueInCell(
								errStrCelVal.spalte(sp).setValue(zellValue).zeile(spielerZeilecntr));
					}
					throw new GenerateException(I18n.get("error.meldeliste.spieler.name", spielerName, spielerZeilecntr));
				}
				spielrNamenInSheet.add(cleanUpSpielerName(spielerName));
			}
		}
	}

	/**
	 * für ein vergleich ,.: und leerzeichen entfernen
	 *
	 * @param name
	 * @return
	 */
	@VisibleForTesting
	String cleanUpSpielerName(String name) {
		return name.replaceAll("[^a-zA-Z0-9öäüÄÖÜß]+", "").toLowerCase();
	}

	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return NewSheet.from(meldeListe, SheetNamen.meldeliste(), metadatenSchluessel).setDocVersionWhenNew().useIfExist().hideGrid()
				.pos(DefaultSheetPos.MELDELISTE).tabColor(SHEET_COLOR).create().getSheet();
	}

	public int getSpielerNameSpalte() {
		return meldeListe.getSpielerNameErsteSpalte();
	}

	/**
	 * alle zeilen mit nummer ohne namen entfernen
	 *
	 * @throws GenerateException
	 */

	public void zeileOhneSpielerNamenEntfernen() throws GenerateException {
		meldeListe.processBoxinfo("processbox.meldeliste.zeilen.ohne.namen.entfernen");

		doSort(meldeListe.getMeldungenSpalte().getErsteMeldungNameSpalte(), true); // alle zeilen ohne namen nach unten sortieren, egal ob daten oder nicht
		int letzteNrZeile = meldeListe.naechsteFreieDatenZeileInSpielerNrSpalte();
		if (letzteNrZeile < ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // keine Daten
		}
		XSpreadsheet xSheet = getXSpreadSheet();

		// StringCellValue emptyVal = StringCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, 0)).setValue("");
		Position posEmptyVal = Position.from(SPIELER_NR_SPALTE, 0);

		int letzteZeileMitSpielerName = meldeListe.letzteZeileMitSpielerName(); // erst ab zeilen ohne namen anfangen

		if (letzteZeileMitSpielerName > 0) {
			for (int spielerNrZeilecntr = letzteZeileMitSpielerName; spielerNrZeilecntr < letzteNrZeile; spielerNrZeilecntr++) {
				Position posSpielerName = Position.from(meldeListe.getMeldungenSpalte().getErsteMeldungNameSpalte(),
						spielerNrZeilecntr);
				String spielerNamen = meldeListe.getSheetHelper().getTextFromCell(xSheet, posSpielerName);
				if (StringUtils.isBlank(spielerNamen)) { // null oder leer oder leerzeichen
					// nr ohne spieler namen entfernen
					meldeListe.getSheetHelper().clearValInCell(xSheet, posEmptyVal.zeile(spielerNrZeilecntr));
					// meldeListe.getSheetHelper().setStringValueInCell(emptyVal.zeile(spielerNrZeilecntr));
				}
			}
		}
	}

	/**
	 * Erzeugt eine VLOOKUP-Formel, die den Anzeigenamen eines Teams anhand der Teamnummer
	 * aus der Meldeliste liest.<br>
	 * Bei aktivierter Teamname-Spalte wird der Teamname zurückgegeben (Spalte B). Andernfalls
	 * werden alle Spielernamen ("Vorname Nachname") konkateniert und mit " / " getrennt – damit
	 * z.B. eine Doublette ohne Teamname-Spalte beide Spieler vollständig anzeigt.<br>
	 * Der Sheet-Name wird korrekt quotiert, damit lokalisierte Namen mit Leerzeichen funktionieren.
	 *
	 * @param nrAdresse           Zell-Adresse oder Literal, das die Teamnummer enthält
	 * @param teamnameAnzeigen    {@code true} wenn die Meldeliste eine Teamname-Spalte hat
	 * @param formation           Formation der Meldeliste (Anzahl Spieler pro Team)
	 * @param vereinsnameAnzeigen {@code true} wenn die Meldeliste eine Vereinsname-Spalte pro Spieler hat
	 * @return Formel-String (ODF/englisch)
	 */
	public static String teamNameFormel(String nrAdresse, boolean teamnameAnzeigen,
			Formation formation, boolean vereinsnameAnzeigen) {
		String range = "$'" + SheetNamen.meldeliste() + "'.$A$1:$Z$999";
		if (teamnameAnzeigen) {
			return "VLOOKUP(" + nrAdresse + ";" + range + ";2;0)";
		}
		int spaltenProSpieler = vereinsnameAnzeigen ? 3 : 2;
		// Erste Spieler-Spalte ohne Teamname: Spalte B (1-basiert für VLOOKUP = 2)
		int ersterSpielerVlookupIdx = 2;
		int anzSpieler = formation.getAnzSpieler();
		StringBuilder sb = new StringBuilder();
		for (int s = 0; s < anzSpieler; s++) {
			int vornameIdx = ersterSpielerVlookupIdx + s * spaltenProSpieler;
			int nachnameIdx = vornameIdx + 1;
			if (s > 0) {
				sb.append(" & \" / \" & ");
			}
			sb.append("VLOOKUP(").append(nrAdresse).append(";").append(range)
					.append(";").append(vornameIdx).append(";0)")
					.append(" & \" \" & ")
					.append("VLOOKUP(").append(nrAdresse).append(";").append(range)
					.append(";").append(nachnameIdx).append(";0)");
		}
		return sb.toString();
	}

	public String formulaSverweisSpielernamen(String spielrNrAdresse) {
		int ersteName = meldeListe.getMeldungenSpalte().getErsteMeldungNameSpalte();
		int letzteName = meldeListe.getMeldungenSpalte().getLetzteMeldungNameSpalte();
		String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
		String letzteZelleAddress = Position.from(letzteName, 999).getAddressWith$();
		String range = "$'" + SheetNamen.meldeliste() + "'." + ersteZelleAddress + ":" + letzteZelleAddress;

		if (ersteName == letzteName) {
			return "VLOOKUP(" + spielrNrAdresse + ";" + range + ";2;0)";
		}
		// 2 Namens-Spalten (Supermelee): "Nachname, Vorname" zusammensetzen.
		int vornameIdx = ersteName - SPIELER_NR_SPALTE + 1;
		int nachnameIdx = letzteName - SPIELER_NR_SPALTE + 1;
		return "VLOOKUP(" + spielrNrAdresse + ";" + range + ";" + nachnameIdx + ";0)"
				+ " & \", \" & "
				+ "VLOOKUP(" + spielrNrAdresse + ";" + range + ";" + vornameIdx + ";0)";
	}

	/**
	 *
	 * @param spieltag
	 * @param spielrundeGespielt list mit Flags. null für alle
	 * @return
	 * @throws GenerateException
	 */

	public IMeldungen<MLD_LIST_TYPE, MLDTYPE> getMeldungen(final SpielTagNr spieltag,
			final List<SpielrundeGespielt> spielrundeGespielt, IMeldungen<MLD_LIST_TYPE, MLDTYPE> meldungen)
			throws GenerateException {
		checkNotNull(spieltag, "spieltag == null");
		checkNotNull(meldungen, "meldungen == null");

		int letzteZeile = meldeListe.getMeldungenSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();

		if (letzteZeile >= ERSTE_DATEN_ZEILE) {
			// daten vorhanden
			int spieltagSpalte = spieltagSpalte(spieltag);

			// Use getDataArray to get an Array off all Spieler bis SpieltagSpalte
			RangePosition rangebisSpieltagSpalte = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE,
					spieltagSpalte, letzteZeile);
			RangeData meldungenDaten = RangeHelper.from(meldeListe, rangebisSpieltagSpalte).getDataFromRange();

			// 0 = nr
			// 1-3 = namen je nach Modus, Tete,Doubl,Tripl
			// 4 = setzpos
			// letzte ! spalte = aktuelle Spieltag status
			for (RowData meldungZeile : meldungenDaten) {
				int isAktivStatus = meldungZeile.getLast().getIntVal(-1);
				SpielrundeGespielt status = SpielrundeGespielt.findById(isAktivStatus);
				if (spielrundeGespielt == null || spielrundeGespielt.contains(status)) {
					meldungen.addNewWennNichtVorhanden(meldungZeile);
				}
			}
		}
		return meldungen;
	}

	public int setzPositionSpalte() {
		return meldeListe.getMeldungenSpalte().getLetzteMeldungNameSpalte() + 1;
	}

	public int ersteSpieltagSpalte() {
		if (setzPositionSpalte() > -1) {
			return setzPositionSpalte() + 1;
		}
		return SPIELER_NR_SPALTE + meldeListe.getMeldungenSpalte().getAnzahlSpielerNamenSpalten();
	}

	public int spieltagSpalte(SpielTagNr spieltag) {
		return ersteSpieltagSpalte() + spieltag.getNr() - 1;
	}

	public void updateMeldungenNr() throws GenerateException {

		meldeListe.processBoxinfo("processbox.meldeliste.nummern.aktualisieren");

		int letzteSpielZeile = meldeListe.getMeldungenSpalte().letzteZeileMitSpielerName();
		if (letzteSpielZeile <= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			return; // nur 1 Meldung
		}
		XSpreadsheet xSheet = getXSpreadSheet();
		doSort(SPIELER_NR_SPALTE, false); // hoechste nummer oben, ohne nummer nach unten

		int letzteSpielerNr = 0;
		int spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet,
				Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		if (spielrNr > -1) {
			letzteSpielerNr = spielrNr;
		}

		// Zeile erste Meldung ohne Nummer, weil ohne nummer nach unten sortiert
		int ersteZeileOhneNummer = meldeListe.naechsteFreieDatenZeileInSpielerNrSpalte(); // letzte Zeile ohne Spieler Nr

		// lücken füllen
		NumberCellValue celVal = NumberCellValue.from(xSheet, Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE));
		for (int spielerZeilecntr = ersteZeileOhneNummer; spielerZeilecntr <= letzteSpielZeile; spielerZeilecntr++) {
			spielrNr = meldeListe.getSheetHelper().getIntFromCell(xSheet,
					Position.from(SPIELER_NR_SPALTE, spielerZeilecntr));
			if (spielrNr == -1) {
				meldeListe.getSheetHelper()
						.setNumberValueInCell(celVal.setValue((double) ++letzteSpielerNr).zeile(spielerZeilecntr));
			}
		}
		// spieler nach Alphabet sortieren
		doSort(meldeListe.getMeldungenSpalte().getErsteMeldungNameSpalte(), true);
	}

	/**
	 * @param turnierSystem
	 * @throws GenerateException
	 */
	public void insertTurnierSystemInHeader(TurnierSystem turnierSystem) throws GenerateException {
		// oben links
		meldeListe.getSheetHelper().setStringValueInCell(StringCellValue
				.from(getXSpreadSheet(), Position.from(0, 0), I18n.get("meldeliste.header.turniersystem", turnierSystem.getBezeichnung()))
				.setEndPosMergeSpaltePlus(1).setCharWeight(FontWeight.BOLD).setHoriJustify(CellHoriJustify.LEFT)
				.setVertJustify(CellVertJustify2.TOP).setShrinkToFit(true).setCharColor("00599d"));
	}

}
