/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.endrangliste;

import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.helper.sheet.SummenSpalten;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class EndranglisteSheet extends SheetRunner implements IEndRangliste {
	private static final Logger logger = LogManager.getLogger(EndranglisteSheet.class);

	public static final int ERSTE_DATEN_ZEILE = 3; // Zeile 4
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0, B=1
	public static final int RANGLISTE_SPALTE = 2; // Spalte C=2
	public static final int ERSTE_SPIELTAG_SPALTE = 3; // Spalte D=3

	public static final int ERSTE_SORTSPALTE_OFFSET = 3; // zur letzte spalte = anz Spieltage

	public static final String SHEETNAME = "Endrangliste";
	public static final short SHEET_POS = (short) 2;
	public static final String SHEET_COLOR = "d637e8";

	private final SpieltagRanglisteSheet spieltagRanglisteSheet;
	private final SpielerSpalte spielerSpalte;
	private final MeldeListeSheet_New meldeListeSheetNew;
	private final KonfigurationSheet konfigurationSheet;
	private final EndRanglisteFormatter endRanglisteFormatter;
	private final RangListeSpalte rangListeSpalte;
	private final RangListeSorter rangListeSorter;

	public EndranglisteSheet(XComponentContext xContext) {
		super(xContext);
		this.konfigurationSheet = new KonfigurationSheet(xContext);
		this.spieltagRanglisteSheet = new SpieltagRanglisteSheet(xContext);
		this.meldeListeSheetNew = new MeldeListeSheet_New(xContext);
		this.spielerSpalte = new SpielerSpalte(xContext, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this,
				this.meldeListeSheetNew, Formation.MELEE);
		this.endRanglisteFormatter = new EndRanglisteFormatter(xContext, this, getAnzSpaltenInSpieltag(),
				this.spielerSpalte, ERSTE_SPIELTAG_SPALTE, this.konfigurationSheet);
		this.rangListeSpalte = new RangListeSpalte(xContext, RANGLISTE_SPALTE, this);
		this.rangListeSorter = new RangListeSorter(xContext, this);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(getxContext(), SHEETNAME).pos(SHEET_POS).tabColor(SHEET_COLOR).setActiv().forceCreate()
				.create()) {
			upDateSheet();
		}
	}

	private void upDateSheet() throws GenerateException {
		Integer headerColor = this.konfigurationSheet.getRanglisteHeaderFarbe();
		spielerEinfügen();
		this.spielerSpalte.insertHeaderInSheet(headerColor);
		this.spielerSpalte.formatDaten();
		this.endRanglisteFormatter.updateHeader();

		spielTageEinfuegen();
		updateEndSummenSpalten();

		this.rangListeSorter.insertSortValidateSpalte();
		this.rangListeSorter.insertManuelsortSpalten();

		this.endRanglisteFormatter.formatDaten();
		this.rangListeSpalte.upDateRanglisteSpalte();
		this.rangListeSpalte.insertHeaderInSheet(headerColor);

		updateAnzSpieltageSpalte();
		this.endRanglisteFormatter.formatDatenGeradeUngerade();
		this.rangListeSorter.doSort();
	}

	private void spielerEinfügen() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();

		HashSet<Integer> spielerNummer = new HashSet<>();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			List<Integer> spielerListe = this.spieltagRanglisteSheet.getSpielerNrList(SpielTagNr.from(spieltagCntr));
			spielerNummer.addAll(spielerListe);
		}
		this.spielerSpalte.alleSpielerNrEinfuegen(spielerNummer);
	}

	private void spielTageEinfuegen() throws GenerateException {
		// verwende fill down
		// =WENNNV(SVERWEIS(A4;$'2. Spieltag Rangliste'.$A4:$D1000;4;0);"")

		int anzSpieltage = getAnzahlSpieltage();
		int letzteDatenZeile = this.spielerSpalte.getLetzteDatenZeile();

		String verweisAufSpalteSpielerNr = "INDIRECT(ADDRESS(ROW();" + (SPIELER_NR_SPALTE + 1) + ";8))";

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			int spieltagSummeErsteSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * getAnzSpaltenInSpieltag());
			Position positionSumme = Position.from(spieltagSummeErsteSpalte, ERSTE_DATEN_ZEILE);
			StringCellValue strVal = StringCellValue.from(getSheet(), positionSumme);

			for (int summeSpalteCntr = 0; summeSpalteCntr < getAnzSpaltenInSpieltag(); summeSpalteCntr++) {
				String verweisAufSummeSpalte = this.spieltagRanglisteSheet.formulaSverweisAufSummeSpalte(
						SpielTagNr.from(spieltagCntr), summeSpalteCntr, verweisAufSpalteSpielerNr);
				strVal.setValue("IFNA(" + verweisAufSummeSpalte + ";\"\")");
				// kein filldown verwenden weil die Adresse in der SVERWEIS hochgezaehlt wird
				// for (int zeile = ERSTE_DATEN_ZEILE; zeile < letzteDatenZeile; zeile++) {
				this.getSheetHelper().setFormulaInCell(strVal.setFillAutoDown(letzteDatenZeile));
				// }
				strVal.spaltePlusEins();
			}
		}
	}

	private int anzSpielTageSpalte() throws GenerateException {
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		return ersteSpalteEndsumme + ANZAHL_SPALTEN_IN_SUMME;
	}

	/**
	 * Anzahl gespielte Spieltage<br>
	 * =ZÄHLENWENN(D4:AG4;"<>")/6
	 *
	 * @throws GenerateException
	 */
	private void updateAnzSpieltageSpalte() throws GenerateException {
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		int letzteSpieltagLetzteSpalte = ersteSpalteEndsumme - 1;
		int letzteZeile = getLetzteDatenZeile();

		Position ersteSpielTagErsteZelle = Position.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE);
		Position letzteSpielTagLetzteZelle = Position.from(letzteSpieltagLetzteSpalte, ERSTE_DATEN_ZEILE);

		String formula = "=COUNTIF(" + ersteSpielTagErsteZelle.getAddress() + ":"
				+ letzteSpielTagLetzteZelle.getAddress() + ";\"<>\")/" + ANZAHL_SPALTEN_IN_SUMME;

		// letzte Spalte ist anzahl spieltage
		StringCellValue formulaVal = StringCellValue
				.from(getSheet(), Position.from(anzSpielTageSpalte(), ERSTE_DATEN_ZEILE)).setValue(formula)
				.setFillAutoDown(letzteZeile);

		this.getSheetHelper().setFormulaInCell(formulaVal);

		// Spalte formatieren
		CellProperties celColumProp = CellProperties.from().setWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER);
		this.getSheetHelper().setColumnProperties(this.getSheet(), anzSpielTageSpalte(), celColumProp);

		// Daten
		RangePosition rangPos = RangePosition.from(formulaVal.getPos(), formulaVal.getFillAuto());
		CellProperties celRangeProp = CellProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder());
		this.getSheetHelper().setPropertiesInRange(this.getSheet(), rangPos, celRangeProp);
	}

	private void updateEndSummenSpalten() throws GenerateException {
		List<Integer> spielerNrList = this.spielerSpalte.getSpielerNrList();
		for (int spielerNr : spielerNrList) {
			endSummeSpalte(spielerNr);
		}
	}

	private void endSummeSpalte(int spielrNr) throws GenerateException {
		SpielTagNr schlechtesteSpielTag = schlechtesteSpieltag(spielrNr);
		int anzSpieltage = getAnzahlSpieltage();
		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);

		if (anzSpieltage < 2) {
			return;
		}

		StringCellValue[] endsummeFormula = new StringCellValue[ANZAHL_SPALTEN_IN_SUMME];
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
			endsummeFormula[summeSpalteCntr] = StringCellValue.from(getSheet(),
					Position.from(ersteSpalteEndsumme + summeSpalteCntr, spielerZeile));
		}

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			if (schlechtesteSpielTag.getNr() != spieltagCntr) {
				int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
				for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
					Position spielSummeSpalte = Position.from(ersteSpieltagSummeSpalte + summeSpalteCntr, spielerZeile);
					if (!endsummeFormula[summeSpalteCntr].isValueEmpty()) {
						endsummeFormula[summeSpalteCntr].appendValue(" + ");
					}
					endsummeFormula[summeSpalteCntr].appendValue(spielSummeSpalte.getAddress());
				}
			}
		}

		for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
			this.getSheetHelper().setFormulaInCell(endsummeFormula[summeSpalteCntr]);
		}
	}

	private SpielTagNr schlechtesteSpieltag(int spielrNr) throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();
		if (anzSpieltage < 2) {
			return null;
		}
		List<SpielerSpieltagErgebnis> spielerSpieltagErgebniss = spielerErgebnisseEinlesen(spielrNr);
		spielerSpieltagErgebniss.sort(new Comparator<SpielerSpieltagErgebnis>() {
			@Override
			public int compare(SpielerSpieltagErgebnis o1, SpielerSpieltagErgebnis o2) {
				// schlechteste oben
				return o1.reversedCompareTo(o2);
			}
		});
		return spielerSpieltagErgebniss.get(0).getSpielTag();
	}

	private List<SpielerSpieltagErgebnis> spielerErgebnisseEinlesen(int spielrNr) throws GenerateException {
		List<SpielerSpieltagErgebnis> spielerErgebnisse = new ArrayList<>();
		int anzSpieltage = getAnzahlSpieltage();

		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);

		XSpreadsheet sheet = getSheet();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {

			SpielTagNr spielTagNr = SpielTagNr.from(spieltagCntr);

			int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
			// summe vorhanden ?
			String spielPlus = getSheetHelper().getTextFromCell(sheet,
					Position.from(ersteSpieltagSummeSpalte, spielerZeile));
			if (StringUtils.isNotBlank(spielPlus)) {
				SpielerSpieltagErgebnis ergebniss = new SpielerSpieltagErgebnis(spielTagNr, spielrNr);
				ergebniss.setSpielPlus(NumberUtils.toInt(spielPlus));
				ergebniss.setSpielMinus(NumberUtils.toInt(this.getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + SPIELE_MINUS_OFFS, spielerZeile))));
				ergebniss.setPunktePlus(NumberUtils.toInt(this.getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_PLUS_OFFS, spielerZeile))));
				ergebniss.setPunkteMinus(NumberUtils.toInt(this.getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_MINUS_OFFS, spielerZeile))));
				spielerErgebnisse.add(ergebniss);
			} else {
				// nuller spieltag
				SpielerSpieltagErgebnis nullerSpielTag = new SpielerSpieltagErgebnis(spielTagNr, spielrNr);
				// nicht gespielten spieltage sind immer schlechter als gespielte spieltage
				nullerSpielTag.setSpielPlus(-1); // -1 Plus Punkte sind im normalen Spiel nicht möglich
				spielerErgebnisse.add(nullerSpielTag);
			}
		}
		return spielerErgebnisse;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public XSpreadsheet getSheet() {
		return getSheetHelper().newIfNotExist(SHEETNAME, SHEET_POS, SHEET_COLOR);
	}

	@Override
	public int getAnzahlSpieltage() throws GenerateException {
		return this.spieltagRanglisteSheet.countNumberOfSpieltage();
	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();
		return ERSTE_SPIELTAG_SPALTE + (anzSpieltage * ANZAHL_SPALTEN_IN_SUMME);
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		// plus 1 spalte fuer spieltag
		return getErsteSummeSpalte() + PUNKTE_DIV_OFFS + 1;
	}

	private int getAnzSpaltenInSpieltag() {
		return SummenSpalten.ANZAHL_SPALTEN_IN_SUMME;
	}

	@Override
	public int getLetzteDatenZeile() throws GenerateException {
		return this.spielerSpalte.getLetzteDatenZeile();
	}

	@Override
	public int getErsteDatenZiele() throws GenerateException {
		return ERSTE_DATEN_ZEILE;
	}

	@Override
	public int getManuellSortSpalte() throws GenerateException {
		return getLetzteSpalte() + ERSTE_SORTSPALTE_OFFSET;
	}

}
