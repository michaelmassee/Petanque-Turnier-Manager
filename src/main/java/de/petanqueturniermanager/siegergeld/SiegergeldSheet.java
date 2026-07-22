/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

public class SiegergeldSheet extends SheetRunner implements ISheet {

	private static final int HEADER_COLOR = 0xD9EAF7;
	private static final int INPUT_COLOR = 0xFFF2CC;

	private static final int SPALTE_GRUPPE = 0;
	private static final int SPALTE_PLATZ = 1;
	private static final int SPALTE_BETRAG_MANUELL = 2;
	private static final int SPALTE_GRUPPENANTEIL = 3;
	private static final int SPALTE_PLATZANTEIL = 4;
	private static final int SPALTE_BETRAG = 5;
	private static final int SPALTE_BETRAG_AUFGERUNDET = 6;

	private static final int ZEILE_TEILNEHMER = 1;
	private static final int ZEILE_STARTGELD = 2;
	private static final int ZEILE_AUSZAHLUNGSANTEIL = 3;
	private static final int ZEILE_STARTGELDTOPF = 4;
	private static final int ZEILE_AUSZAHLUNGSTOPF = 5;
	private static final int ZEILE_HEADER = 7;
	private static final int ERSTE_DATEN_ZEILE = 8;

	public SiegergeldSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.KEIN, "SiegergeldSheet");
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_SIEGERGELD, SheetNamen.LEGACY_SIEGERGELD);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	@Override
	protected void doRun() throws GenerateException {
		TurnierSystem turnierSystem = ermittleTurnierSystem();
		var quelle = SiegergeldQuellen.fuer(getWorkingSpreadsheet(), turnierSystem);
		if (quelle.isEmpty()) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
					.caption(I18n.get("msg.caption.siegergeld"))
					.message(I18n.get("msg.text.siegergeld.nicht.unterstuetzt", turnierSystem.getBezeichnung()))
					.show();
			return;
		}

		SiegergeldQuelle siegergeldQuelle = quelle.get();
		List<SiegergeldEintrag> eintraege = siegergeldQuelle.leseTop3();
		if (eintraege.isEmpty()) {
			eintraege = siegergeldQuelle.allgemeineEintraege();
		}

		if (!NewSheet.from(this, SheetNamen.siegergeld(), SheetMetadataHelper.SCHLUESSEL_SIEGERGELD)
				.pos(DefaultSheetPos.SIEGERGELD).setForceCreate(true).setActiv()
				.hideGrid().tabColor(0xF4B183).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Siegergeld wurde nicht erstellt");
			return;
		}

		XSpreadsheet sheet = getXSpreadSheet();
		schreibeSheet(sheet, eintraege, siegergeldQuelle.teilnehmerAnzahl());
		if (SheetRunner.isRunning()) {
			getSheetHelper().setActiveSheet(sheet);
			SheetRunner.unterdrückeNaechstesSelectionChange();
		}
	}

	private TurnierSystem ermittleTurnierSystem() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());
		int id = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.KEIN.getId());
		TurnierSystem turnierSystem = TurnierSystem.findById(id);
		return turnierSystem == null ? TurnierSystem.KEIN : turnierSystem;
	}

	private void schreibeSheet(XSpreadsheet sheet, List<SiegergeldEintrag> eintraege, int teilnehmer) throws GenerateException {
		processBoxinfo("processbox.erstelle.sheet", SheetNamen.siegergeld());
		SheetHelper sh = getSheetHelper();
		List<SiegergeldEintrag> sortierteEintraege = sortierteEintraege(eintraege);

		schreibeTitel(sheet);
		schreibeEingaben(sheet, teilnehmer);
		schreibeTabellenHeader(sheet);
		int letzteZeile = schreibeEintraege(sheet, sortierteEintraege);

		for (int spalte = SPALTE_GRUPPE; spalte <= SPALTE_BETRAG_AUFGERUNDET; spalte++) {
			sh.setOptimaleBreitePlusMarge(sheet, spalte, SheetHelper.OPTIMALE_BREITE_MARGE);
		}
		sh.setPropertyInRange(sheet, RangePosition.from(SPALTE_GRUPPE, ZEILE_STARTGELD,
				SPALTE_BETRAG_AUFGERUNDET, letzteZeile),
				CellProperties.SHRINK_TO_FIT, Boolean.TRUE);
		SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();
		PrintArea.from(sheet, getWorkingSpreadsheet()).setPrintArea(
				RangePosition.from(SPALTE_GRUPPE, 0, SPALTE_BETRAG_AUFGERUNDET, letzteZeile));
	}

	static List<SiegergeldEintrag> sortierteEintraege(List<SiegergeldEintrag> eintraege) {
		return eintraege.stream()
				.sorted(Comparator.comparing(SiegergeldEintrag::gruppe).thenComparingInt(SiegergeldEintrag::platz))
				.toList();
	}

	private void schreibeTitel(XSpreadsheet sheet) throws GenerateException {
		var titel = StringCellValue.from(sheet, Position.from(SPALTE_GRUPPE, 0), I18n.get("sheet.name.siegergeld"))
				.setEndPosMergeSpalte(SPALTE_BETRAG_AUFGERUNDET)
				.setCellProperties(CellProperties.from().setCellBackColor(HEADER_COLOR)
						.setCharWeight(FontWeight.BOLD).setCharHeight(16).centerJustify());
		getSheetHelper().setStringValueInCell(titel);
	}

	private void schreibeEingaben(XSpreadsheet sheet, int teilnehmer) throws GenerateException {
		label(sheet, ZEILE_TEILNEHMER, I18n.get("siegergeld.label.teilnehmer"));
		getSheetHelper().setValInCell(sheet, Position.from(1, ZEILE_TEILNEHMER), teilnehmer);

		label(sheet, ZEILE_STARTGELD, I18n.get("siegergeld.label.startgeld"));
		getSheetHelper().setValInCell(sheet, Position.from(1, ZEILE_STARTGELD), 0);
		input(sheet, Position.from(1, ZEILE_STARTGELD));

		label(sheet, ZEILE_AUSZAHLUNGSANTEIL, I18n.get("siegergeld.label.auszahlungsanteil"));
		getSheetHelper().setValInCell(sheet, Position.from(1, ZEILE_AUSZAHLUNGSANTEIL), 100);
		input(sheet, Position.from(1, ZEILE_AUSZAHLUNGSANTEIL));

		label(sheet, ZEILE_STARTGELDTOPF, I18n.get("siegergeld.label.startgeldtopf"));
		getSheetHelper().setFormulaInCell(sheet, Position.from(1, ZEILE_STARTGELDTOPF), "="
				+ Position.from(1, ZEILE_TEILNEHMER).getAddress() + "*" + Position.from(1, ZEILE_STARTGELD).getAddress());

		label(sheet, ZEILE_AUSZAHLUNGSTOPF, I18n.get("siegergeld.label.auszahlungstopf"));
		getSheetHelper().setFormulaInCell(sheet, Position.from(1, ZEILE_AUSZAHLUNGSTOPF), "="
				+ Position.from(1, ZEILE_STARTGELDTOPF).getAddress() + "*"
				+ Position.from(1, ZEILE_AUSZAHLUNGSANTEIL).getAddress() + "/100");
	}

	private void schreibeTabellenHeader(XSpreadsheet sheet) throws GenerateException {
		String[] header = {
				I18n.get("siegergeld.header.gruppe"),
				I18n.get("siegergeld.header.platz"),
				I18n.get("siegergeld.header.betrag.manuell"),
				I18n.get("siegergeld.header.gruppenanteil"),
				I18n.get("siegergeld.header.platzanteil"),
				I18n.get("siegergeld.header.betrag"),
				I18n.get("siegergeld.header.betrag.aufgerundet")
		};
		for (int i = 0; i < header.length; i++) {
			getSheetHelper().setStringValueInCell(StringCellValue.from(sheet, Position.from(i, ZEILE_HEADER), header[i])
					.setCellProperties(CellProperties.from().setCellBackColor(HEADER_COLOR).setCharWeight(FontWeight.BOLD)
							.centerJustify().setAllThinBorder()));
		}
	}

	private int schreibeEintraege(XSpreadsheet sheet, List<SiegergeldEintrag> eintraege) throws GenerateException {
		Map<String, Integer> gruppenStartZeilen = gruppenStartZeilen(eintraege);
		int letzteZeile = ERSTE_DATEN_ZEILE + eintraege.size() - 1;

		schreibeWerteBlock(sheet, eintraege, gruppenStartZeilen, letzteZeile);

		for (Map.Entry<String, Integer> gruppenStart : gruppenStartZeilen.entrySet()) {
			schreibeGruppenZelle(sheet, gruppenStart.getKey(), gruppenStart.getValue(), gruppenStartZeilen, eintraege);
			input(sheet, Position.from(SPALTE_GRUPPENANTEIL, gruppenStart.getValue()));
		}

		for (int i = 0; i < eintraege.size(); i++) {
			int zeile = ERSTE_DATEN_ZEILE + i;
			getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_BETRAG, zeile),
					betragFormel(zeile, gruppenStartZeilen, eintraege));
			getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_BETRAG_AUFGERUNDET, zeile),
					betragAufgerundetFormel(zeile));
		}

		getSheetHelper().setPropertyInRange(sheet, RangePosition.from(SPALTE_GRUPPE, ERSTE_DATEN_ZEILE,
				SPALTE_BETRAG_AUFGERUNDET, letzteZeile),
				CellProperties.TABLE_BORDER2, BorderFactory.from().allThin().toBorder());
		int summenZeile = letzteZeile + 1;
		return schreibeSummenZeilen(sheet, summenZeile, letzteZeile, gruppenStartZeilen, eintraege);
	}

	/**
	 * Platz-, Betrag-manuell-, Gruppenanteil- und Platzanteil-Spalte als ein Block schreiben
	 * (RangeHelper/RangeData/RowData) statt zellenweise in der Schleife.
	 */
	private void schreibeWerteBlock(XSpreadsheet sheet, List<SiegergeldEintrag> eintraege,
			Map<String, Integer> gruppenStartZeilen, int letzteZeile) throws GenerateException {
		String ersteGruppenName = eintraege.get(0).gruppe();
		RangeData werteBlock = new RangeData();
		for (int i = 0; i < eintraege.size(); i++) {
			SiegergeldEintrag eintrag = eintraege.get(i);
			int zeile = ERSTE_DATEN_ZEILE + i;
			RowData zeileData = werteBlock.addNewRow();
			zeileData.newInt(eintrag.platz());
			zeileData.newInt(0);
			if (gruppenStartZeilen.get(eintrag.gruppe()) == zeile) {
				zeileData.newInt(SiegergeldVerteilung.gruppenAnteil(ersteGruppenName, eintrag.gruppe()));
			} else {
				zeileData.newEmpty();
			}
			zeileData.newInt(SiegergeldVerteilung.platzAnteil(eintrag.platz()));
		}
		RangeHelper.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				RangePosition.from(SPALTE_PLATZ, ERSTE_DATEN_ZEILE, SPALTE_PLATZANTEIL, letzteZeile))
				.setDataInRange(werteBlock);

		CellProperties inputProperties = CellProperties.from().setCellBackColor(INPUT_COLOR)
				.setHoriJustify(CellHoriJustify.RIGHT);
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(SPALTE_BETRAG_MANUELL, ERSTE_DATEN_ZEILE, SPALTE_BETRAG_MANUELL, letzteZeile),
				inputProperties);
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(SPALTE_PLATZANTEIL, ERSTE_DATEN_ZEILE, SPALTE_PLATZANTEIL, letzteZeile),
				inputProperties);
	}

	private int schreibeSummenZeilen(XSpreadsheet sheet, int summenStartZeile, int letzteEintragZeile,
			Map<String, Integer> gruppenStartZeilen, List<SiegergeldEintrag> eintraege) throws GenerateException {
		int zeile = summenStartZeile;
		schreibeSummenLabel(sheet, zeile, I18n.get("siegergeld.label.summe") + " "
				+ I18n.get("siegergeld.header.gruppenanteil"));
		getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_GRUPPENANTEIL, zeile),
				gruppenanteilSummeFormel(letzteEintragZeile));

		zeile++;
		for (String gruppe : gruppenStartZeilen.keySet()) {
			schreibeSummenLabel(sheet, zeile, I18n.get("siegergeld.label.summe") + " "
					+ I18n.get("siegergeld.header.platzanteil") + " " + gruppe);
			getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_PLATZANTEIL, zeile),
					platzanteilSummeFormel(gruppe, gruppenStartZeilen, eintraege));
			zeile++;
		}

		schreibeSummenLabel(sheet, zeile, I18n.get("siegergeld.label.summe"));
		getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_BETRAG_MANUELL, zeile),
				summenFormel(SPALTE_BETRAG_MANUELL, letzteEintragZeile));
		getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_BETRAG, zeile),
				summenFormel(SPALTE_BETRAG, letzteEintragZeile));
		getSheetHelper().setFormulaInCell(sheet, Position.from(SPALTE_BETRAG_AUFGERUNDET, zeile),
				summenFormel(SPALTE_BETRAG_AUFGERUNDET, letzteEintragZeile));
		getSheetHelper().setPropertyInRange(sheet, RangePosition.from(SPALTE_GRUPPE, summenStartZeile,
				SPALTE_BETRAG_AUFGERUNDET, zeile), CellProperties.TABLE_BORDER2,
				BorderFactory.from().allThin().toBorder());
		return zeile;
	}

	private void schreibeGruppenZelle(XSpreadsheet sheet, String gruppe, int startZeile,
			Map<String, Integer> gruppenStartZeilen, List<SiegergeldEintrag> eintraege) throws GenerateException {
		StringCellValue gruppenZelle = StringCellValue.from(sheet, SPALTE_GRUPPE, startZeile, gruppe)
				.setCellProperties(CellProperties.from().centerJustify());
		int endZeile = gruppenEndZeile(gruppe, gruppenStartZeilen, eintraege);
		if (endZeile > startZeile) {
			gruppenZelle.setEndPosMergeZeile(endZeile);
		}
		getSheetHelper().setStringValueInCell(gruppenZelle);
	}

	private void schreibeSummenLabel(XSpreadsheet sheet, int zeile, String label) throws GenerateException {
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(SPALTE_PLATZ, zeile), label)
				.setCellProperties(CellProperties.from().setCharWeight(FontWeight.BOLD).setAllThinBorder()));
	}

	private static Map<String, Integer> gruppenStartZeilen(List<SiegergeldEintrag> eintraege) {
		Map<String, Integer> gruppenStartZeilen = new LinkedHashMap<>();
		for (int i = 0; i < eintraege.size(); i++) {
			gruppenStartZeilen.putIfAbsent(eintraege.get(i).gruppe(), ERSTE_DATEN_ZEILE + i);
		}
		return gruppenStartZeilen;
	}

	static String betragFormel(int zeile, Map<String, Integer> gruppenStartZeilen,
			List<SiegergeldEintrag> eintraege) {
		String gruppe = eintraege.get(zeile - ERSTE_DATEN_ZEILE).gruppe();
		String auszahlungstopf = Position.from(1, ZEILE_AUSZAHLUNGSTOPF).getAddressWith$();
		String gruppenanteil = Position.from(SPALTE_GRUPPENANTEIL, gruppenStartZeilen.get(gruppe)).getAddress();
		String platzanteil = Position.from(SPALTE_PLATZANTEIL, zeile).getAddress();
		return "=" + auszahlungstopf + "*" + gruppenanteil + "/100*" + platzanteil + "/100";
	}

	static String betragAufgerundetFormel(int zeile) {
		return "=CEILING(" + Position.from(SPALTE_BETRAG, zeile).getAddress() + ";1)";
	}

	static String summenFormel(int spalte, int letzteEintragZeile) {
		return "=SUM(" + Position.from(spalte, ERSTE_DATEN_ZEILE).getAddress()
				+ ":" + Position.from(spalte, letzteEintragZeile).getAddress() + ")";
	}

	static String gruppenanteilSummeFormel(int letzteEintragZeile) {
		return "=SUM(" + Position.from(SPALTE_GRUPPENANTEIL, ERSTE_DATEN_ZEILE).getAddress()
				+ ":" + Position.from(SPALTE_GRUPPENANTEIL, letzteEintragZeile).getAddress() + ")";
	}

	static String platzanteilSummeFormel(String gruppe, Map<String, Integer> gruppenStartZeilen,
			List<SiegergeldEintrag> eintraege) {
		int startZeile = gruppenStartZeilen.get(gruppe);
		int endZeile = gruppenEndZeile(gruppe, gruppenStartZeilen, eintraege);
		return "=SUM(" + Position.from(SPALTE_PLATZANTEIL, startZeile).getAddress()
				+ ":" + Position.from(SPALTE_PLATZANTEIL, endZeile).getAddress() + ")";
	}

	static int gruppenEndZeile(String gruppe, Map<String, Integer> gruppenStartZeilen,
			List<SiegergeldEintrag> eintraege) {
		int startZeile = gruppenStartZeilen.get(gruppe);
		int endZeile = ERSTE_DATEN_ZEILE + eintraege.size() - 1;
		for (int i = startZeile - ERSTE_DATEN_ZEILE + 1; i < eintraege.size(); i++) {
			if (!gruppe.equals(eintraege.get(i).gruppe())) {
				return ERSTE_DATEN_ZEILE + i - 1;
			}
		}
		return endZeile;
	}

	private void label(XSpreadsheet sheet, int zeile, String text) throws GenerateException {
		getSheetHelper().setStringValueInCell(StringCellValue.from(sheet, Position.from(0, zeile), text)
				.setCellProperties(CellProperties.from().setCharWeight(FontWeight.BOLD)));
	}

	private void input(XSpreadsheet sheet, Position pos) throws GenerateException {
		getSheetHelper().setFormatInCell(StringCellValue.from(sheet, pos)
				.setCellProperties(CellProperties.from().setCellBackColor(INPUT_COLOR)
						.setHoriJustify(CellHoriJustify.RIGHT)));
	}
}
