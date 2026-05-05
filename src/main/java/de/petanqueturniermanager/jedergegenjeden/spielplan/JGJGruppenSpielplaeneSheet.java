package de.petanqueturniermanager.jedergegenjeden.spielplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.jedergegenjeden.JGJGruppenAufteiler;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.model.LigaSpielPlan;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt für jede JGJ-Gruppe ein eigenes minimales Spielplan-Sheet (Aushang).
 * <p>
 * Reine Anzeige – keine Eingabe vorgesehen, Werte werden statisch geschrieben
 * und sind nicht mit der Ergebnis-Erfassung verknüpft.
 * Geeignet zum Drucken oder als Quelle für die Webseiten-Ausgabe.
 */
public class JGJGruppenSpielplaeneSheet extends SheetRunner implements ISheet {

	private static final int HEADER_TITEL_ZEILE = 0;
	private static final int HEADER_SPALTEN_ZEILE = 1;
	private static final int ERSTE_DATEN_ZEILE = HEADER_SPALTEN_ZEILE + 2; // 2 Header-Zeilen: Gruppen-Labels + A/B Sub-Labels

	private static final int SPALTE_NR = 0;
	private static final int SPALTE_TEAM_A = 1;
	private static final int SPALTE_TEAM_B = 2;
	private static final int SPALTE_ERGEBNIS_A = 3;
	private static final int SPALTE_ERGEBNIS_B = 4;
	private static final int LETZTE_SPALTE = SPALTE_ERGEBNIS_B;

	private static final int SPALTE_BREITE_NR = 1000;        // 1 cm
	private static final int SPALTE_BREITE_ERGEBNIS = 1000;  // 1 cm

	private final JGJKonfigurationSheet konfigurationSheet;
	private final JGJMeldeListeSheet_Update meldeListe;

	private XSpreadsheet aktuellesSheet;

	public JGJGruppenSpielplaeneSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ);
		konfigurationSheet = new JGJKonfigurationSheet(workingSpreadsheet);
		meldeListe = new JGJMeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	protected JGJKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() {
		return aktuellesSheet;
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		processBoxinfo("processbox.jgj.gruppen.spielplaene.erstellen");

		int gruppengroesse = konfigurationSheet.getGruppengroesse();
		TeamMeldungen meldungen = meldeListe.getAktiveMeldungen();

		if (gruppengroesse <= 0 || meldungen.size() <= gruppengroesse) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.jgj.gruppen.spielplaene"))
					.message(I18n.get("msg.text.jgj.gruppen.spielplaene.keine.gruppen"))
					.show();
			return;
		}

		boolean mitRueckrunde = konfigurationSheet.isRueckrunde();
		List<TeamMeldungen> gruppen = JGJGruppenAufteiler.teileInGruppen(meldungen, gruppengroesse);
		boolean zeigeNr = konfigurationSheet.getSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NR;
		Map<Integer, String> teamNamen = zeigeNr ? Map.of() : meldeListe.leseTeamNamen();
		String freispielText = I18n.get("spielplan.freispiel.name");

		// Rückwärts erstellen: Da jedes neue Sheet an Position JGJ_WORK eingefügt wird und
		// vorhandene Sheets nach hinten verschiebt, ergibt rückwärtige Iteration die
		// natürliche Tab-Reihenfolge A, B, C, … für den Anwender.
		for (int g = gruppen.size() - 1; g >= 0; g--) {
			SheetRunner.testDoCancelTask();
			String buchstabe = gruppenBuchstabe(g);
			erstelleGruppenSheet(gruppen.get(g), buchstabe, mitRueckrunde, zeigeNr, teamNamen, freispielText);
		}

		// Nach dem Lauf das erste Gruppen-Sheet (Gruppe A) aktivieren, damit der Anwender
		// sofort den korrekten Tab-Inhalt sieht.
		String ersterMetaKey = SheetMetadataHelper.schluesselJgjGruppeSpielplan(gruppenBuchstabe(0));
		String ersterName = SheetNamen.jgjGruppeSpielplan(gruppenBuchstabe(0));
		XSpreadsheet ersteGruppe = SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), ersterMetaKey, ersterName);
		if (ersteGruppe != null && SheetRunner.isRunning()) {
			TurnierSheet.from(ersteGruppe, getWorkingSpreadsheet()).setActiv();
		}
	}

	private void erstelleGruppenSheet(TeamMeldungen gruppe, String buchstabe, boolean mitRueckrunde,
			boolean zeigeNr, Map<Integer, String> teamNamen, String freispielText) throws GenerateException {

		String sheetName = SheetNamen.jgjGruppeSpielplan(buchstabe);
		String metaKey = SheetMetadataHelper.schluesselJgjGruppeSpielplan(buchstabe);

		NewSheet.from(this, sheetName, metaKey)
				.tabColor(konfigurationSheet.getSpielrundeTabFarbe())
				.pos(DefaultSheetPos.JGJ_WORK)
				.forceCreate().hideGrid().create();

		aktuellesSheet = SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), metaKey, sheetName);
		try {
			schreibeSpaltenBreiten();
			schreibeTitelZeile(buchstabe);
			schreibeSpaltenHeader();

			LigaSpielPlan ligaSpielPlan = new LigaSpielPlan(gruppe);
			List<List<TeamPaarung>> hRunde = ligaSpielPlan.schufflePlan().getSpielPlanClone();
			List<List<TeamPaarung>> rRunde = mitRueckrunde
					? ligaSpielPlan.flipTeams().getSpielPlanClone()
					: List.<List<TeamPaarung>>of();

			int letzteDatenZeile = schreibeSpieleDaten(hRunde, rRunde, zeigeNr, teamNamen, freispielText);

			formatiereSheet(letzteDatenZeile, hRunde, rRunde);
			definierePrintBereich(letzteDatenZeile);

			if (SheetRunner.isRunning()) {
				SheetFreeze.from(aktuellesSheet, getWorkingSpreadsheet())
						.anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();
			}
		} finally {
			aktuellesSheet = null;
		}
	}

	private void schreibeSpaltenBreiten() throws GenerateException {
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_NR,
				ColumnProperties.from().setWidth(SPALTE_BREITE_NR).centerJustify());
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_TEAM_A,
				ColumnProperties.from().setWidth(JGJKonfigurationSheet.MELDUNG_NAME_WIDTH));
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_TEAM_B,
				ColumnProperties.from().setWidth(JGJKonfigurationSheet.MELDUNG_NAME_WIDTH));
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_ERGEBNIS_A,
				ColumnProperties.from().setWidth(SPALTE_BREITE_ERGEBNIS).centerJustify());
		getSheetHelper().setColumnProperties(aktuellesSheet, SPALTE_ERGEBNIS_B,
				ColumnProperties.from().setWidth(SPALTE_BREITE_ERGEBNIS).centerJustify());
	}

	private void schreibeTitelZeile(String buchstabe) throws GenerateException {
		String titel = I18n.get("jgj.gruppe.name") + " " + buchstabe;
		StringCellValue titelVal = StringCellValue
				.from(aktuellesSheet, Position.from(SPALTE_NR, HEADER_TITEL_ZEILE), titel)
				.setEndPosMergeSpalte(LETZTE_SPALTE)
				.setCellBackColor(konfigurationSheet.getSpielPlanHeaderFarbe())
				.setHoriJustify(CellHoriJustify.CENTER)
				.setCharHeight(14)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
		getSheetHelper().setStringValueInCell(titelVal);
	}

	private void schreibeSpaltenHeader() throws GenerateException {
		int farbe = konfigurationSheet.getSpielPlanHeaderFarbe();
		var border = BorderFactory.from().allThin().toBorder();

		StringCellValue val = StringCellValue
				.from(aktuellesSheet, Position.from(SPALTE_NR, HEADER_SPALTEN_ZEILE))
				.setCellBackColor(farbe)
				.setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(border);

		// Nr. über beide Header-Zeilen zusammenführen
		getSheetHelper().setStringValueInCell(
				val.setValue(I18n.get("column.header.nr")).setEndPosMergeZeilePlus(1));

		// "Mannschaft" über Spalten A+B zusammenführen
		getSheetHelper().setStringValueInCell(
				val.setValue(I18n.get("column.header.mannschaft"))
						.spalte(SPALTE_TEAM_A).setEndPosMergeSpaltePlus(1));

		// "Ergebnis" über Spalten A+B zusammenführen
		getSheetHelper().setStringValueInCell(
				val.setValue(I18n.get("column.header.ergebnis"))
						.spalte(SPALTE_ERGEBNIS_A).setEndPosMergeSpaltePlus(1));

		// Unterzeile: A / B Sub-Labels
		val.setEndPosMerge(null).zeilePlusEins();
		getSheetHelper().setStringValueInCell(val.setValue("A").spalte(SPALTE_TEAM_A));
		getSheetHelper().setStringValueInCell(val.setValue("B").spalte(SPALTE_TEAM_B));
		getSheetHelper().setStringValueInCell(val.setValue("A").spalte(SPALTE_ERGEBNIS_A));
		getSheetHelper().setStringValueInCell(val.setValue("B").spalte(SPALTE_ERGEBNIS_B));
	}

	private int schreibeSpieleDaten(List<List<TeamPaarung>> hRunde, List<List<TeamPaarung>> rRunde,
			boolean zeigeNr, Map<Integer, String> teamNamen, String freispielText) throws GenerateException {

		List<List<TeamPaarung>> alleSpieltage = new ArrayList<>();
		alleSpieltage.addAll(hRunde);
		alleSpieltage.addAll(rRunde);

		int freispielPlus = konfigurationSheet.getFreispielPunktePlus();
		int freispielMinus = konfigurationSheet.getFreispielPunkteMinus();

		RangeData daten = new RangeData();
		int hIndex = 1;
		int rIndex = 1;
		boolean inHinrunde = true;
		int hAnzahl = anzPaarungen(hRunde);

		for (List<TeamPaarung> spieltag : alleSpieltage) {
			for (TeamPaarung paarung : spieltag) {
				SheetRunner.testDoCancelTask();
				RowData row = daten.addNewRow();
				if (inHinrunde) {
					row.newString(JGJSpielPlanSheet.NR_HINRUNDE_PREFIX + hIndex++);
				} else {
					row.newString(JGJSpielPlanSheet.NR_RUECKRUNDE_PREFIX + rIndex++);
				}
				int nrA = paarung.getA().getNr();
				boolean hatB = paarung.getOptionalB().isPresent();
				int nrB = hatB ? paarung.getB().getNr() : 0;

				row.newString(zeigeNr ? String.valueOf(nrA) : teamNamen.getOrDefault(nrA, ""));
				if (!hatB) {
					row.newString(freispielText);
					row.newInt(freispielPlus);
					row.newInt(freispielMinus);
				} else {
					row.newString(zeigeNr ? String.valueOf(nrB) : teamNamen.getOrDefault(nrB, ""));
					row.newString("");
					row.newString("");
				}
			}
			if (inHinrunde && hIndex > hAnzahl) {
				inHinrunde = false;
			}
		}

		Position startPos = Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, daten.getRangePosition(startPos))
				.setDataInRange(daten)
				.setRangeProperties(RangeProperties.from()
						.setBorder(BorderFactory.from().allThin().toBorder())
						.centerJustify().setShrinkToFit(true)
						.topMargin(110).bottomMargin(110).setCharHeight(12));

		return ERSTE_DATEN_ZEILE + daten.size() - 1;
	}

	private static int anzPaarungen(List<List<TeamPaarung>> spielplan) {
		int anz = 0;
		for (List<TeamPaarung> spieltag : spielplan) {
			anz += spieltag.size();
		}
		return anz;
	}

	private void formatiereSheet(int letzteDatenZeile, List<List<TeamPaarung>> hRunde,
			List<List<TeamPaarung>> rRunde) throws GenerateException {
		RangePosition zebraRange = RangePosition.from(SPALTE_NR, ERSTE_DATEN_ZEILE,
				LETZTE_SPALTE, letzteDatenZeile);
		RanglisteGeradeUngeradeFormatHelper.from(this, zebraRange)
				.geradeFarbe(konfigurationSheet.getSpielPlanHintergrundFarbeGerade())
				.ungeradeFarbe(konfigurationSheet.getSpielPlanHintergrundFarbeUnGerade())
				.apply();

		RangeProperties horTrennerDouble = RangeProperties.from()
				.setBorder(BorderFactory.from().doubleLn().forBottom().toBorder());
		RangeProperties horTrennerBoldBottom = RangeProperties.from()
				.setBorder(BorderFactory.from().boldLn().forBottom().toBorder());

		int zeile = ERSTE_DATEN_ZEILE;
		int anzRunden = hRunde.size();
		int anzPaarungenProRunde = hRunde.isEmpty() ? 0 : hRunde.get(0).size();

		for (int i = 1; i < anzRunden; i++) {
			zeile += anzPaarungenProRunde - 1;
			RangeHelper.from(this,
					RangePosition.from(SPALTE_NR, zeile, LETZTE_SPALTE, zeile))
					.setRangeProperties(horTrennerDouble);
			zeile++;
		}
		zeile += anzPaarungenProRunde - 1;
		RangeHelper.from(this,
				RangePosition.from(SPALTE_NR, zeile, LETZTE_SPALTE, zeile))
				.setRangeProperties(horTrennerBoldBottom);
		zeile++;

		if (!rRunde.isEmpty()) {
			for (int i = 1; i < anzRunden; i++) {
				zeile += anzPaarungenProRunde - 1;
				RangeHelper.from(this,
						RangePosition.from(SPALTE_NR, zeile, LETZTE_SPALTE, zeile))
						.setRangeProperties(horTrennerDouble);
				zeile++;
			}
		}

		RangeProperties vertTrennerBoldLeft = RangeProperties.from()
				.setBorder(BorderFactory.from().boldLn().forLeft().toBorder());
		RangeProperties vertTrennerDoubleLeft = RangeProperties.from()
				.setBorder(BorderFactory.from().doubleLn().forLeft().toBorder());

		RangePosition vertikal = RangePosition.from(SPALTE_NR, HEADER_SPALTEN_ZEILE,
				SPALTE_NR, letzteDatenZeile);
		RangeHelper.from(this, vertikal.spalte(SPALTE_NR)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(this, vertikal.spalte(SPALTE_TEAM_B)).setRangeProperties(vertTrennerDoubleLeft);
		RangeHelper.from(this, vertikal.spalte(SPALTE_ERGEBNIS_A)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(this, vertikal.spalte(LETZTE_SPALTE + 1)).setRangeProperties(vertTrennerBoldLeft);
	}

	private void definierePrintBereich(int letzteDatenZeile) throws GenerateException {
		RangePosition bereich = RangePosition.from(
				Position.from(SPALTE_NR, HEADER_TITEL_ZEILE),
				Position.from(LETZTE_SPALTE, letzteDatenZeile));
		PrintArea.from(aktuellesSheet, getWorkingSpreadsheet()).setPrintArea(bereich);
	}

	private static String gruppenBuchstabe(int index) {
		return String.valueOf((char) ('A' + index));
	}
}
