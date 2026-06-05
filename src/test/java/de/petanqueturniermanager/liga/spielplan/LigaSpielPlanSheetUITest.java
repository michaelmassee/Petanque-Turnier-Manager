package de.petanqueturniermanager.liga.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheetsync.SpielplanFormatiererKonfig;
import de.petanqueturniermanager.helper.sheetsync.SpielplanFormatiererSheetRunner;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.liga.rangliste.LigaTestMeldeListeErstellen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * UITest fuer den Liga-Spielplan.<br>
 * 4 Teams: 3 Runden Hinrunde (HR-1..HR-6) + 3 Runden Rueckrunde (RR-1..RR-6) = 12 Datenzeilen.
 */
public class LigaSpielPlanSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(LigaSpielPlanSheetUITest.class);

	// private Konstanten aus LigaSpielPlanSheet nachgebaut
	private static final int SPIEL_NR_SPALTE = 0; // Spalte A
	private static final int NAME_A_SPALTE = 6; // ORT_SPALTE(5) + 1
	private static final int NAME_B_SPALTE = 7; // NAME_A_SPALTE + 1

	// 4 Teams: 3 Runden x 2 Paarungen = 6 pro Halbzeit, x 2 Halbzeiten = 12 gesamt
	private static final int ANZ_HR_ZEILEN = 6;
	private static final int ANZ_DATEN_ZEILEN = ANZ_HR_ZEILEN * 2;

	@BeforeEach
	public void testMeldeListeErstellen() throws GenerateException {
		LigaTestMeldeListeErstellen testMeldeListeErstellen = new LigaTestMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListeErstellen.run();
	}

	/**
	 * Prueft, dass das Sheet "Spielplan" nach dem Erstellen existiert.
	 */
	@Test
	public void testSpielplanWirdErstellt() throws GenerateException {
		logger.info("testSpielplanWirdErstellt");
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		XSpreadsheet sheet = spielPlan.getXSpreadSheet();
		assertThat(sheet).as("Sheet 'Spielplan' muss existieren").isNotNull();
	}

	/**
	 * Prueft die Spielnummern-Spalte: HR-1 bis HR-6 (Hinrunde), dann RR-1 bis RR-6 (Rueckrunde).
	 */
	@Test
	public void testSpielplanNummern() throws GenerateException {
		logger.info("testSpielplanNummern");
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		int ersteZeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		int letzteZeile = ersteZeile + ANZ_DATEN_ZEILEN - 1;

		RangeData nummern = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(SPIEL_NR_SPALTE, ersteZeile, SPIEL_NR_SPALTE, letzteZeile))
				.getDataFromRange();

		assertThat(nummern).as("Anzahl Datenzeilen").hasSize(ANZ_DATEN_ZEILEN);

		// Hinrunde: HR-1 bis HR-6
		for (int i = 0; i < ANZ_HR_ZEILEN; i++) {
			assertThat(nummern.get(i).get(0).getStringVal())
					.as("Hinrunde Zeile " + (i + 1))
					.isEqualTo("HR-" + (i + 1));
		}
		// Rueckrunde: RR-1 bis RR-6
		for (int i = 0; i < ANZ_HR_ZEILEN; i++) {
			assertThat(nummern.get(ANZ_HR_ZEILEN + i).get(0).getStringVal())
					.as("Rueckrunde Zeile " + (i + 1))
					.isEqualTo("RR-" + (i + 1));
		}
	}

	/**
	 * Prueft, dass die VLOOKUP-Formeln in den Teamnamen-Spalten korrekt auf die Meldeliste verweisen.<br>
	 * Feste Paarung (Team 1=Alpha vs Team 4=Delta) in Arbeitsspalten schreiben,
	 * danach werden die Teamnamen aus der Meldeliste gelesen.
	 */
	@Test
	public void testSpielplanTeamNamenViaVLOOKUP() throws GenerateException {
		logger.info("testSpielplanTeamNamenViaVLOOKUP");
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		// Feste Paarung in erste Datenzeile schreiben: Team 1 (Alpha) vs Team 4 (Delta)
		RangeData paarung = new RangeData(new Object[][] { { 1, 4 } });
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						LigaSpielPlanSheet.TEAM_B_NR_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE))
				.setDataInRange(paarung);

		// Neuberechnung erzwingen, da VLOOKUP mit blattübergreifenden Referenzen
		// nach setDataArray nicht automatisch neu berechnet wird
		Lo.qi(XCalculatable.class, doc).calculateAll();

		// Teamnamen-Formeln auslesen (NAME_A_SPALTE=6, NAME_B_SPALTE=7)
		RangeData namesData = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(NAME_A_SPALTE, LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						NAME_B_SPALTE, LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE))
				.getDataFromRange();

		assertThat(namesData).as("Genau eine Datenzeile erwartet").hasSize(1);
		assertThat(namesData.get(0).get(0).getStringVal()).as("Heim-Team (Nr. 1 = Alpha)").isEqualTo("Alpha");
		assertThat(namesData.get(0).get(1).getStringVal()).as("Gast-Team (Nr. 4 = Gamma)").isEqualTo("Gamma");
	}

	/**
	 * Prueft, dass die Punkte-Formeln korrekt berechnen: 1 fuer den Gewinner, 0 fuer den Verlierer.<br>
	 * Formel: WENN(SiegeA > SiegeB; 1; 0)
	 */
	@Test
	public void testSpielplanPunkteFormeln() throws GenerateException {
		logger.info("testSpielplanPunkteFormeln");
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		// Siege in die ersten zwei Zeilen schreiben
		// Zeile 1: Team A gewinnt (SiegeA=1, SiegeB=0)
		// Zeile 2: Team B gewinnt (SiegeA=0, SiegeB=1)
		RangeData siege = new RangeData(new Object[][] { { 1, 0 }, { 0, 1 } });
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(LigaSpielPlanSheet.SPIELE_A_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						LigaSpielPlanSheet.SPIELE_B_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + 1))
				.setDataInRange(siege);

		// Punkte-Formeln lesen
		RangeData punkte = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(LigaSpielPlanSheet.PUNKTE_A_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						LigaSpielPlanSheet.PUNKTE_B_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + 1))
				.getDataFromRange();

		assertThat(punkte).as("Zwei Ergebniszeilen erwartet").hasSize(2);
		// Zeile 1: Team A gewinnt
		assertThat(punkte.get(0).get(0).getIntVal()).as("Zeile 1: Punkte A (Gewinner)").isEqualTo(1);
		assertThat(punkte.get(0).get(1).getIntVal()).as("Zeile 1: Punkte B (Verlierer)").isEqualTo(0);
		// Zeile 2: Team B gewinnt
		assertThat(punkte.get(1).get(0).getIntVal()).as("Zeile 2: Punkte A (Verlierer)").isEqualTo(0);
		assertThat(punkte.get(1).get(1).getIntVal()).as("Zeile 2: Punkte B (Gewinner)").isEqualTo(1);
	}

	/**
	 * Regression im Kiosk-Modus: nach Erstaufbau muss ein erneuter
	 * {@link LigaSpielPlanSheet#run()} unter aktivem TurnierModus + Liga-Blattschutz
	 * sauber durchlaufen.
	 */
	@Test
	public void kioskModus_spielplanRebuildUnterSchutz() throws GenerateException {
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();
		assertThat(spielPlan.getXSpreadSheet()).isNotNull();

		mitKioskModus(TurnierSystem.LIGA, () -> new LigaSpielPlanSheet(wkingSpreadsheet).run());

		assertThat(sheetHlp.findByName(de.petanqueturniermanager.helper.i18n.SheetNamen.spielplan()))
				.as("Spielplan muss nach Kiosk-Rebuild weiterhin existieren")
				.isNotNull();
	}

	/**
	 * Regression (Bug "Punkte H/G editierbar im Turniermodus"): Die Punkte-Formelspalten
	 * (PUNKTE_A/PUNKTE_B) müssen im Kiosk-Modus gesperrt sein – auch dann, wenn sie wie in
	 * einem Bestandsdokument zuvor fälschlich entsperrt waren. Die editierbaren Siege-Spalten
	 * bleiben dagegen entsperrt.
	 */
	@Test
	public void kioskModus_punkteFormelSpaltenWerdenGesperrt() throws Exception {
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();
		XSpreadsheet sheet = spielPlan.getXSpreadSheet();
		int ersteZeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		int letzteZeile = ersteZeile + ANZ_DATEN_ZEILEN - 1;

		// Bestandsdokument simulieren: Punkte-Spalten vorab fälschlich entsperren
		setzeIsLocked(sheet, LigaSpielPlanSheet.PUNKTE_A_SPALTE, ersteZeile,
				LigaSpielPlanSheet.PUNKTE_B_SPALTE, letzteZeile, false);

		mitKioskModus(TurnierSystem.LIGA, () -> { /* nur Schutz anwenden, keine Aktion */ });

		assertThat(istGesperrt(sheet, LigaSpielPlanSheet.PUNKTE_A_SPALTE, ersteZeile))
				.as("Punkte H (Formelspalte) muss im Kiosk-Modus gesperrt sein").isTrue();
		assertThat(istGesperrt(sheet, LigaSpielPlanSheet.PUNKTE_B_SPALTE, ersteZeile))
				.as("Punkte G (Formelspalte) muss im Kiosk-Modus gesperrt sein").isTrue();
		assertThat(istGesperrt(sheet, LigaSpielPlanSheet.SPIELE_A_SPALTE, ersteZeile))
				.as("Siege H (editierbar) darf im Kiosk-Modus nicht gesperrt sein").isFalse();
	}

	/**
	 * Regression (Bug "Punkte H/G – kein Editierfeld-Hintergrund, sondern Zebra"): Der
	 * Spielplan-Formatierer (Tab-Wechsel) muss eine veraltete Editierbar-CF auf den
	 * Punkte-Formelspalten entfernen, damit dort nur noch das direkte Zebra sichtbar ist.
	 */
	@Test
	public void formatierer_entferntVeralteteCfAufPunkteSpalten() throws Exception {
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();
		XSpreadsheet sheet = spielPlan.getXSpreadSheet();
		int ersteZeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		int letzteZeile = ersteZeile + ANZ_DATEN_ZEILEN - 1;

		// Bestandsdokument simulieren: veraltete Editierbar-CF auf den Punkte-Spalten setzen
		RangePosition punkteRange = RangePosition.from(LigaSpielPlanSheet.PUNKTE_A_SPALTE, ersteZeile,
				LigaSpielPlanSheet.PUNKTE_B_SPALTE, letzteZeile);
		EditierbaresZelleFormatHelper.anwenden(spielPlan, punkteRange);
		assertThat(hatConditionalFormat(sheet, LigaSpielPlanSheet.PUNKTE_A_SPALTE, ersteZeile))
				.as("Vorbedingung: Punkte-Spalte hat eine (simulierte) Editierbar-CF").isTrue();

		// Formatierer-Lauf (wie beim Tab-Wechsel) muss die CF wieder entfernen
		RangePosition datenRange = RangePosition.from(LigaSpielPlanSheet.SPIEL_NR_SPALTE, ersteZeile,
				LigaSpielPlanSheet.SPIELPNKT_B_SPALTE, letzteZeile);
		var konfig = new SpielplanFormatiererKonfig(datenRange, java.util.List.of(),
				java.util.List.of(punkteRange), 0xFFFFFF, 0xEEEEEE, null);
		new SpielplanFormatiererSheetRunner(wkingSpreadsheet, sheet, iSheet -> konfig).run();

		assertThat(hatConditionalFormat(sheet, LigaSpielPlanSheet.PUNKTE_A_SPALTE, ersteZeile))
				.as("Punkte H darf nach Formatierer-Lauf keine CF mehr haben").isFalse();
		assertThat(hatConditionalFormat(sheet, LigaSpielPlanSheet.PUNKTE_B_SPALTE, ersteZeile))
				.as("Punkte G darf nach Formatierer-Lauf keine CF mehr haben").isFalse();
	}

	private boolean hatConditionalFormat(XSpreadsheet sheet, int spalte, int zeile) throws Exception {
		var cell = sheet.getCellByPosition(spalte, zeile);
		XPropertySet props = Lo.qi(XPropertySet.class, cell);
		XSheetConditionalEntries cf = Lo.qi(XSheetConditionalEntries.class,
				props.getPropertyValue("ConditionalFormat"));
		return cf != null && cf.getCount() > 0;
	}

	private boolean istGesperrt(XSpreadsheet sheet, int spalte, int zeile) throws Exception {
		var cell = sheet.getCellByPosition(spalte, zeile);
		XPropertySet props = Lo.qi(XPropertySet.class, cell);
		return ((CellProtection) props.getPropertyValue("CellProtection")).IsLocked;
	}

	private void setzeIsLocked(XSpreadsheet sheet, int startSpalte, int startZeile,
			int endeSpalte, int endeZeile, boolean locked) throws Exception {
		var range = sheet.getCellRangeByPosition(startSpalte, startZeile, endeSpalte, endeZeile);
		XPropertySet props = Lo.qi(XPropertySet.class, range);
		var cp = new CellProtection();
		cp.IsLocked = locked;
		props.setPropertyValue("CellProtection", cp);
	}
}
