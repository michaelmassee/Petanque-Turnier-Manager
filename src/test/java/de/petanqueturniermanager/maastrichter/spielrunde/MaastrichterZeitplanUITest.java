package de.petanqueturniermanager.maastrichter.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.SheetSchutzInfo;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.maastrichter.blattschutz.MaastrichterBlattschutzKonfiguration;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * UI-Tests für die optionale Rundenzeitplanung (Turnier-Startzeit, Rundenpause,
 * Durchgang-Aufteilung) im Maastrichter System. Maastrichter erbt die eigentliche
 * Zeitplan-Logik vollständig von {@link de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste}
 * (siehe {@link de.petanqueturniermanager.schweizer.spielrunde.SchweizerZeitplanUITest} für die
 * dort bereits abgedeckte Kernlogik) — dieser Test
 * deckt daher gezielt die Maastrichter-spezifischen Punkte ab: dass die geerbte Aufteilung auch
 * über die Maastrichter-Vorrunden-Klassen funktioniert, und die Maastrichter-eigene
 * Blattschutz-Ergänzung (siehe {@link MaastrichterBlattschutzKonfiguration}), die die
 * Rundenstartzeit-Zelle im Turnier-Modus editierbar freigibt.
 */
public class MaastrichterZeitplanUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 16; // Doublette -> 8 Paarungen pro Vorrunde
	private static final int BAHNEN = 3; // 8 Paarungen / 3 Bahnen -> Bloecke [3, 3, 2]

	private RangeData ladeZeitSpalte(XSpreadsheet sheet, int anzZeilen) throws GenerateException {
		RangePosition range = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzZeilen - 1);
		return RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range).getDataFromRange();
	}

	private RangeData ladeBahnNummern(XSpreadsheet sheet, int anzZeilen) throws GenerateException {
		RangePosition range = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + anzZeilen - 1);
		return RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range).getDataFromRange();
	}

	private boolean istInEditierbaremBereich(SheetSchutzInfo info, int spalte, int zeile) {
		return info.editierbareBereich().stream().anyMatch(range ->
				spalte >= range.getStartSpalte() && spalte <= range.getEndeSpalte()
						&& zeile >= range.getStartZeile() && zeile <= range.getEndeZeile());
	}

	/**
	 * Feature aktiv, mehr Paarungen als Bahnen — dieselben strukturellen Erwartungen wie in
	 * {@code SchweizerZeitplanUITest.featureAn_MehrPaarungenAlsBahnen_ErzeugtErwarteteDurchgaenge},
	 * hier über die Maastrichter-Vorrunden-Klasse ({@link MaastrichterSpielrundeSheetNaechste}
	 * erbt {@code SchweizerSpielrundeSheetNaechste}) erzeugt: Durchgang-Anzahl/-Groessen,
	 * Rundenstartzeit aus der Turnier-Startzeit, pro-Durchgang neu beginnende Bahn-Nr.
	 */
	@Test
	public void featureAn_MehrPaarungenAlsBahnen_ErzeugtErwarteteDurchgaengeInDerVorrunde() throws GenerateException {
		new MaastrichterMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleTestdaten();
		MaastrichterSpielrundeSheetNaechste vorrundeNaechste = new MaastrichterSpielrundeSheetNaechste(wkingSpreadsheet);
		var konfig = vorrundeNaechste.getKonfigurationSheet();
		konfig.setSpielplanTeamAnzeige(SpielplanTeamAnzeige.NR);
		konfig.setSpielrundeSpielbahn(SpielrundeSpielbahn.N);
		konfig.setZeitplanAktiv(true);
		konfig.setZeitplanAnzahlBahnen(BAHNEN);
		konfig.setZeitplanZeitlimitMinuten(15);
		konfig.setZeitplanDurchgangPauseMinuten(5);
		konfig.setZeitplanTurnierStartzeit("09:00");
		vorrundeNaechste.doRun();

		XSpreadsheet vorrunde1 = vorrundeNaechste.getXSpreadSheet();
		assertThat(vorrunde1).isNotNull();
		vorrundeNaechste.getxCalculatable().calculateAll();

		// Rundenstartzeit (Vorrunde 1) = Turnier-Startzeit
		String rundenStartzeit = sheetHlp.getTextFromCell(vorrunde1,
				Position.from(SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE, SchweizerAbstractSpielrundeSheet.ZWEITE_HEADER_ZEILE));
		assertThat(rundenStartzeit).as("Rundenstartzeit Vorrunde 1 muss der Turnier-Startzeit entsprechen").isEqualTo("09:00");

		// 8 Paarungen / 3 Bahnen -> Bloecke [3, 3, 2] (relative Zeilen 0-2 | 3-5 | 6-7).
		RangeData zeitSpalte = ladeZeitSpalte(vorrunde1, ANZ_TEAMS / 2);
		for (int i : new int[] { 0, 2, 3, 5, 6, 7 }) {
			assertThat(zeitSpalte.get(i).get(0).getStringVal())
					.as("Zeile %d ist Start oder Ende eines Durchgangs", i).isNotBlank();
		}
		for (int i : new int[] { 1, 4 }) {
			assertThat(zeitSpalte.get(i).get(0).getStringVal())
					.as("Zeile %d ist weder Start noch Ende eines Durchgangs", i).isNullOrEmpty();
		}

		// Bahn-Nr beginnt in jedem Durchgang neu bei 1
		RangeData bahnNrn = ladeBahnNummern(vorrunde1, ANZ_TEAMS / 2);
		int[] erwarteteBahnNr = { 1, 2, 3, 1, 2, 3, 1, 2 };
		for (int i = 0; i < erwarteteBahnNr.length; i++) {
			assertThat(bahnNrn.get(i).get(0).getIntVal(-1))
					.as("Bahn-Nr Zeile %d", i).isEqualTo(erwarteteBahnNr[i]);
		}
	}

	/**
	 * Regressionstest für den in Commit a125a8b7 behobenen Bug: {@code MaastrichterBlattschutzKonfiguration}
	 * gab die Rundenstartzeit-Zelle der Vorrunde ursprünglich nicht als editierbar frei, obwohl das
	 * Zeitplan-Feature vollständig von Schweizer geerbt ist — bei aktivem Turnier-Modus (Blattschutz)
	 * war die Zelle dadurch nicht mehr bearbeitbar. Prüft direkt gegen
	 * {@link MaastrichterBlattschutzKonfiguration#berechneSchutzInfos}, ohne den vollen
	 * Turnier-Modus/BlattschutzManager-Lebenszyklus zu durchlaufen (analog zum Muster in
	 * {@code LigaTurnierTestDatenUITest.validiereFreispielNichtEditierbar}).
	 */
	@Test
	public void featureAn_RundenstartzeitZelleIstImBlattschutzAlsEditierbarFreigegeben() throws GenerateException {
		new MaastrichterMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleTestdaten();
		MaastrichterSpielrundeSheetNaechste vorrundeNaechste = new MaastrichterSpielrundeSheetNaechste(wkingSpreadsheet);
		var konfig = vorrundeNaechste.getKonfigurationSheet();
		konfig.setZeitplanAktiv(true);
		konfig.setZeitplanAnzahlBahnen(BAHNEN);
		vorrundeNaechste.doRun();

		String vorrunde1Name = SheetNamen.maastrichterVorrunde(1);
		SheetSchutzInfo vorrundeSchutz = MaastrichterBlattschutzKonfiguration.get()
				.berechneSchutzInfos(wkingSpreadsheet).stream()
				.filter(info -> vorrunde1Name.equals(
						Lo.qi(XNamed.class, info.sheet()).getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Vorrunde-1-Schutzinfo nicht gefunden"));

		assertThat(istInEditierbaremBereich(vorrundeSchutz, SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE,
				SchweizerAbstractSpielrundeSheet.ZWEITE_HEADER_ZEILE))
				.as("Rundenstartzeit-Zelle muss bei aktivem Zeitplan-Feature trotz Blattschutz editierbar bleiben")
				.isTrue();
		assertThat(istInEditierbaremBereich(vorrundeSchutz, SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE))
				.as("Berechnete Durchgang-Zeitzellen bleiben gesperrt (nur die Rundenstartzeit ist manuell editierbar)")
				.isFalse();
	}

	/** Feature deaktiviert: keine Zeitplan-Zelle im editierbaren Bereich (Default-Verhalten unveraendert). */
	@Test
	public void featureAus_KeineZusaetzlicheEditierbareZeitZelleImBlattschutz() throws GenerateException {
		new MaastrichterMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleTestdaten();
		MaastrichterSpielrundeSheetNaechste vorrundeNaechste = new MaastrichterSpielrundeSheetNaechste(wkingSpreadsheet);
		assertThat(vorrundeNaechste.getKonfigurationSheet().isZeitplanAktiv())
				.as("Default muss Zeitplanung deaktiviert sein").isFalse();
		vorrundeNaechste.doRun();

		String vorrunde1Name = SheetNamen.maastrichterVorrunde(1);
		SheetSchutzInfo vorrundeSchutz = MaastrichterBlattschutzKonfiguration.get()
				.berechneSchutzInfos(wkingSpreadsheet).stream()
				.filter(info -> vorrunde1Name.equals(
						Lo.qi(XNamed.class, info.sheet()).getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Vorrunde-1-Schutzinfo nicht gefunden"));

		assertThat(istInEditierbaremBereich(vorrundeSchutz, SchweizerAbstractSpielrundeSheet.ZEIT_SPALTE,
				SchweizerAbstractSpielrundeSheet.ZWEITE_HEADER_ZEILE))
				.as("Ohne aktives Zeitplan-Feature darf die Rundenstartzeit-Zelle nicht extra freigegeben werden")
				.isFalse();
	}
}
