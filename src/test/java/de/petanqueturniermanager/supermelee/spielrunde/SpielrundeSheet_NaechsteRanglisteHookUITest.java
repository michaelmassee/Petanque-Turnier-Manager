/*
 * Erstellung: 2026-05-11 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.RanglisteTestDaten;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheetUITest;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.toolbar.strategie.SupermeleeToolbarStrategie;

/**
 * UI-Tests für den {@code vorNaechsterRunde()}-Hook in
 * {@link SpielrundeSheet_Naechste#naechsteSpielrundeEinfuegen()}.
 * <p>
 * Verifiziert:
 * <ul>
 * <li>Die Endrangliste wird vom Hook aktualisiert (After-Add-Code in {@code doRun()}
 *     fasst die Endrangliste nicht an – ein verändertes Ergebnis wird nur durch den
 *     Hook in die Endrangliste übernommen).</li>
 * <li>Die Tagesrangliste reflektiert nach {@code Naechste.run()} die Ergebnisse des
 *     gerade beendeten Spielrunden.</li>
 * <li>Existenz-Guards: weder Tages- noch Endrangliste werden vom Hook angelegt,
 *     wenn sie zuvor nicht erstellt wurden.</li>
 * </ul>
 */
public class SpielrundeSheet_NaechsteRanglisteHookUITest extends BaseCalcUITest {

	private static final int ANZ_MELDUNGEN = 20;

	private TestSuperMeleeMeldeListeErstellen testMeldeListeErstellen;
	private MeldeListeSheet_NeuerSpieltag meldeListeSheet_NeuerSpieltag;
	private SuperMeleeKonfigurationSheet konfigSheet;
	private RanglisteTestDaten<EndranglisteSheetUITest> ranglisteTestDaten;

	@BeforeEach
	public void setup() throws GenerateException {
		testMeldeListeErstellen = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
		meldeListeSheet_NeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet);
		konfigSheet = new SuperMeleeKonfigurationSheet(wkingSpreadsheet);
		// Marker-Instanz für Resource-Lookup: die JSON-Testdaten für 2 Spieltage
		// liegen unter src/test/resources/.../supermelee/endrangliste/.
		ranglisteTestDaten = new RanglisteTestDaten<>(wkingSpreadsheet, sheetHlp, new EndranglisteSheetUITest());
	}

	/**
	 * Schlüssel-Test für die Hook-Isolation: das After-Add-{@code generate()} in
	 * {@link SpielrundeSheet_Naechste#doRun()} aktualisiert nur die Tagesrangliste,
	 * NICHT die Endrangliste. Wenn nach einem Ergebnis-Update in der aktuellen
	 * Spielrunde die Endrangliste neue Werte zeigt, ist das ausschließlich auf
	 * den {@code vorNaechsterRunde()}-Hook zurückzuführen.
	 */
	@Test
	public void testHookAktualisiertEndranglisteVorNaechsterRunde() throws GenerateException {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);

		SpieltagRanglisteSheet spieltagRangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
		for (int i = 1; i <= 2; i++) {
			SpielTagNr spieltag = SpielTagNr.from(i);
			meldeListeSheet_NeuerSpieltag.setAktiveSpieltag(spieltag);
			meldeListeSheet_NeuerSpieltag.setAktiveSpielRunde(SpielRundeNr.from(1));
			testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(spieltag);
			if (i == 1) {
				deaktiviereSpielerZweiInSpieltagEins();
			}
			ranglisteTestDaten.erstelleTestSpielrunden(2, false, spieltag);
			spieltagRangliste.run();
		}
		EndranglisteSheet endrangliste = new EndranglisteSheet(wkingSpreadsheet);
		endrangliste.run();

		// Spalte mit Spieltag-2-PunktePlus pro Spieler ist deterministisch durch
		// die Ergebnis-Änderung beeinflusst (im Gegensatz zur End-Summe, deren
		// Streichspieltag-Logik den Effekt für einzelne Spieler ausgleichen kann).
		int summeSpieltag2Vorher = summiereSpieltagPunktePlusInEndrangliste(endrangliste, 2);

		konfigSheet.setAktiveSpieltag(SpielTagNr.from(2));
		konfigSheet.setAktiveSpielRunde(SpielRundeNr.from(2));

		// Erstes Plus-Ergebnis in Spieltag 2, Runde 1 um 3 Punkte reduzieren.
		XSpreadsheet spielrundeSheet = sheetHlp.findByName(SpielrundeSheetKonstanten.sheetName(2, 1));
		assertThat(spielrundeSheet).isNotNull();
		RangePosition erstePlusZelle = RangePosition.from(
				SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
				SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE);
		RangeHelper zelle = RangeHelper.from(spielrundeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				erstePlusZelle);
		int plusAlt = zelle.getDataFromRange().get(0).get(0).getIntVal(0);
		int plusNeu = Math.max(0, plusAlt - 3);
		assertThat(plusNeu).as("Test braucht eine messbare Reduktion (plusAlt > plusNeu)").isLessThan(plusAlt);
		zelle.setDataInRange(new RangeData(new Object[][] { { plusNeu } }));

		// Hook läuft in naechsteSpielrundeEinfuegen()
		new SpielrundeSheet_Naechste(wkingSpreadsheet).run();

		int summeSpieltag2Nachher = summiereSpieltagPunktePlusInEndrangliste(endrangliste, 2);
		assertThat(summeSpieltag2Nachher)
				.as("Endrangliste muss durch vorNaechsterRunde-Hook neu berechnet sein "
						+ "(After-Add generate() berührt die Endrangliste nicht). "
						+ "Reduktion in Spieltag 2 muss in der Endrangliste sichtbar sein.")
				.isLessThan(summeSpieltag2Vorher);
	}

	/**
	 * Tagesrangliste wird nach {@code Naechste.run()} aktualisiert. Sowohl Hook
	 * (mit aktueller Rundenzahl) als auch After-Add-{@code generate()} (mit neuer
	 * Rundenzahl) tragen dazu bei. Endzustand: Rangliste enthält die geänderten
	 * Ergebnisse und hat eine zusätzliche Runden-Spalte.
	 */
	@Test
	public void testTagesranglisteWirdNachNaechsteAktualisiert() throws GenerateException {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);

		SpielTagNr spieltag = SpielTagNr.from(1);
		meldeListeSheet_NeuerSpieltag.setAktiveSpieltag(spieltag);
		meldeListeSheet_NeuerSpieltag.setAktiveSpielRunde(SpielRundeNr.from(1));
		testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(spieltag);
		deaktiviereSpielerZweiInSpieltagEins();
		ranglisteTestDaten.erstelleTestSpielrunden(2, false, spieltag);

		SpieltagRanglisteSheet ranglist = new SpieltagRanglisteSheet(wkingSpreadsheet);
		ranglist.run();

		int summeVorher = summiereTagesranglistePunktePlus(ranglist);

		XSpreadsheet spielrundeSheet = sheetHlp.findByName(SpielrundeSheetKonstanten.sheetName(1, 1));
		RangePosition erstePlusZelle = RangePosition.from(
				SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
				SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE);
		RangeHelper zelle = RangeHelper.from(spielrundeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				erstePlusZelle);
		int plusAlt = zelle.getDataFromRange().get(0).get(0).getIntVal(0);
		zelle.setDataInRange(new RangeData(new Object[][] { { Math.max(0, plusAlt - 3) } }));

		new SpielrundeSheet_Naechste(wkingSpreadsheet).run();

		assertThat(ranglist.countNumberOfSpielrundenInSheet())
				.as("Nach Naechste muss die Tagesrangliste eine zusätzliche Runden-Spalte enthalten")
				.isEqualTo(3);
		int summeNachher = summiereTagesranglistePunktePlus(ranglist);
		assertThat(summeNachher)
				.as("Geändertes Ergebnis muss in Tagesranglisten-PunktePlus-Summe sichtbar sein")
				.isLessThan(summeVorher);
	}

	/**
	 * Existenz-Guard für die Tagesrangliste: ist sie zuvor nie angelegt worden,
	 * darf weder der Hook noch das After-Add-{@code generate()} sie erzeugen.
	 */
	@Test
	public void testHookErstelltKeineTagesranglisteWennNichtVorhanden() throws GenerateException {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);

		SpielTagNr spieltag = SpielTagNr.from(1);
		meldeListeSheet_NeuerSpieltag.setAktiveSpieltag(spieltag);
		meldeListeSheet_NeuerSpieltag.setAktiveSpielRunde(SpielRundeNr.from(1));
		testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(spieltag);
		deaktiviereSpielerZweiInSpieltagEins();
		ranglisteTestDaten.erstelleTestSpielrunden(2, false, spieltag);

		String tagName = new SpieltagRanglisteSheet(wkingSpreadsheet).getSheetName(spieltag);
		assertThat(sheetHlp.findByName(tagName))
				.as("Tagesrangliste darf vor Naechste nicht existieren (Vorbedingung)")
				.isNull();

		new SpielrundeSheet_Naechste(wkingSpreadsheet).run();

		assertThat(sheetHlp.findByName(tagName))
				.as("Naechste darf keine Tagesrangliste anlegen, wenn sie nicht existiert")
				.isNull();
	}

	/**
	 * Existenz-Guard für die Endrangliste: zwei Spieltage mit Tagesranglisten,
	 * aber Endrangliste wurde nie aufgebaut. Naechste auf Spieltag 2 darf sie
	 * nicht durch die Hintertür erzeugen.
	 */
	@Test
	public void testHookErstelltKeineEndranglisteWennNichtVorhanden() throws GenerateException {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);

		SpieltagRanglisteSheet spieltagRangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
		for (int i = 1; i <= 2; i++) {
			SpielTagNr spieltag = SpielTagNr.from(i);
			meldeListeSheet_NeuerSpieltag.setAktiveSpieltag(spieltag);
			meldeListeSheet_NeuerSpieltag.setAktiveSpielRunde(SpielRundeNr.from(1));
			testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(spieltag);
			if (i == 1) {
				deaktiviereSpielerZweiInSpieltagEins();
			}
			ranglisteTestDaten.erstelleTestSpielrunden(2, false, spieltag);
			spieltagRangliste.run();
		}

		konfigSheet.setAktiveSpieltag(SpielTagNr.from(2));
		konfigSheet.setAktiveSpielRunde(SpielRundeNr.from(2));

		String endranglisteName = SheetNamen.endrangliste();
		assertThat(sheetHlp.findByName(endranglisteName))
				.as("Endrangliste darf vor Naechste nicht existieren (Vorbedingung)")
				.isNull();

		new SpielrundeSheet_Naechste(wkingSpreadsheet).run();

		assertThat(sheetHlp.findByName(endranglisteName))
				.as("Naechste darf keine Endrangliste anlegen, wenn sie nicht existiert")
				.isNull();
	}

	/**
	 * End-to-End-Pfad über die Toolbar: der „Weiter"-Button löst
	 * {@link SupermeleeToolbarStrategie#weiter(de.petanqueturniermanager.comp.WorkingSpreadsheet)}
	 * aus, der eine asynchrone {@link SpielrundeSheet_Naechste}-Instanz startet.
	 * Über deren {@code naechsteSpielrundeEinfuegen()}-Hook müssen Tages- und
	 * Endrangliste vor der neuen Spielrunde aktualisiert werden – analog zum
	 * direkten {@code .run()}-Test.
	 */
	@Test
	public void testToolbarWeiterLoestRanglistenUpdateAus() throws Exception {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);

		SpieltagRanglisteSheet spieltagRangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
		for (int i = 1; i <= 2; i++) {
			SpielTagNr spieltag = SpielTagNr.from(i);
			meldeListeSheet_NeuerSpieltag.setAktiveSpieltag(spieltag);
			meldeListeSheet_NeuerSpieltag.setAktiveSpielRunde(SpielRundeNr.from(1));
			testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(spieltag);
			if (i == 1) {
				deaktiviereSpielerZweiInSpieltagEins();
			}
			ranglisteTestDaten.erstelleTestSpielrunden(2, false, spieltag);
			spieltagRangliste.run();
		}
		EndranglisteSheet endrangliste = new EndranglisteSheet(wkingSpreadsheet);
		endrangliste.run();

		int summeSpieltag2Vorher = summiereSpieltagPunktePlusInEndrangliste(endrangliste, 2);

		konfigSheet.setAktiveSpieltag(SpielTagNr.from(2));
		konfigSheet.setAktiveSpielRunde(SpielRundeNr.from(2));

		XSpreadsheet spielrundeSheet = sheetHlp.findByName(SpielrundeSheetKonstanten.sheetName(2, 1));
		RangePosition erstePlusZelle = RangePosition.from(
				SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
				SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE);
		RangeHelper zelle = RangeHelper.from(spielrundeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				erstePlusZelle);
		int plusAlt = zelle.getDataFromRange().get(0).get(0).getIntVal(0);
		int plusNeu = Math.max(0, plusAlt - 3);
		assertThat(plusNeu).as("Test braucht eine messbare Reduktion").isLessThan(plusAlt);
		zelle.setDataInRange(new RangeData(new Object[][] { { plusNeu } }));

		// Echte Toolbar-Aktion: löst SpielrundeSheet_Naechste.start() (asynchron) aus.
		new SupermeleeToolbarStrategie().weiter(wkingSpreadsheet);
		wartenAufRunnerFertig(30_000);

		int summeSpieltag2Nachher = summiereSpieltagPunktePlusInEndrangliste(endrangliste, 2);
		assertThat(summeSpieltag2Nachher)
				.as("Toolbar-Weiter muss über den Hook die Endrangliste aktualisieren")
				.isLessThan(summeSpieltag2Vorher);
	}

	/**
	 * Pollt {@link SheetRunner#isRunning()} bis der asynchron gestartete Runner
	 * fertig ist. Notwendig, weil Toolbar-Aktionen {@code .start()} (Thread)
	 * statt {@code .run()} aufrufen.
	 */
	private void wartenAufRunnerFertig(long timeoutMs) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		// Kurzes Initial-Sleep, damit der gestartete Thread sich registrieren kann.
		Thread.sleep(50);
		while (SheetRunner.isRunning() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertThat(SheetRunner.isRunning())
				.as("Runner muss innerhalb von %d ms fertig sein", timeoutMs)
				.isFalse();
	}

	/**
	 * Setzt die Spielt-Mit-Flagge für zwei Spieler in Spieltag 1 auf leer, damit
	 * 18 statt 20 aktive Spieler übrig bleiben – passend zu den
	 * {@code spieltag1-runde*-paarungen.json}-Referenzdateien aus dem
	 * {@code endrangliste/}-Resource-Pfad.
	 */
	private void deaktiviereSpielerZweiInSpieltagEins() throws GenerateException {
		sheetHlp.setStringValueInCell(
				StringCellValue.from(meldeListeSheet_NeuerSpieltag, Position.from(3, 10)).setValue(""));
		sheetHlp.setStringValueInCell(
				StringCellValue.from(meldeListeSheet_NeuerSpieltag, Position.from(3, 11)).setValue(""));
	}

	/**
	 * Summiert die PunktePlus-Spalte eines konkreten Spieltags über alle Spieler
	 * im Endrangliste-Sheet. Diese Werte bilden 1:1 die jeweilige Spieltag-Rangliste
	 * ab; eine Ergebnis-Änderung im zugehörigen Spielrunde-Sheet schlägt
	 * deterministisch durch (im Gegensatz zur End-Summe, deren
	 * Streichspieltag-Logik den Effekt einzelner Spieler ausgleichen kann).
	 */
	private int summiereSpieltagPunktePlusInEndrangliste(EndranglisteSheet endrangliste, int spieltagNr)
			throws GenerateException {
		int spalte = EndranglisteSheet.ERSTE_SPIELTAG_SPALTE
				+ (spieltagNr - 1) * SuperMeleeSummenSpalten.ANZAHL_SPALTEN_IN_SUMME
				+ SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS;
		int letzteZeile = endrangliste.getLetzteMitDatenZeileInSpielerNrSpalte();
		RangePosition range = RangePosition.from(spalte, EndranglisteSheet.ERSTE_DATEN_ZEILE, spalte, letzteZeile);
		return RangeHelper.from(endrangliste.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				range).getDataFromRange().stream()
				.flatMap(List::stream)
				.mapToInt(c -> c.getIntVal(0))
				.sum();
	}

	private int summiereTagesranglistePunktePlus(SpieltagRanglisteSheet ranglist) throws GenerateException {
		int spalte = ranglist.getErsteSummeSpalte() + SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS;
		RangePosition range = RangePosition.from(
				spalte, ranglist.getErsteDatenZiele(),
				spalte, ranglist.sucheLetzteZeileMitSpielerNummer());
		return RangeHelper.from(ranglist.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(), range)
				.getDataFromRange().stream()
				.flatMap(List::stream)
				.mapToInt(c -> c.getIntVal(0))
				.sum();
	}
}
