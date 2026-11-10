/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XTopWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerRundenLeser;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.basesheet.spielrunde.AbstractSpielrundeWertungUmschalten;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RundenWertungHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.BlattKnoten;
import de.petanqueturniermanager.sidebar.sheets.SheetBaumOrganisierer;

/**
 * „Spielrunde werten / nicht werten" im Schweizer System: Eine nicht gewertete Runde zählt weder in
 * der Rangliste noch in der Paarungshistorie; Blatt und Nummerierung bleiben erhalten.
 */
class SchweizerSpielrundeWertungUITest extends BaseCalcUITest {

	private static final int ANZ_RUNDEN = 3;
	private static final int NICHT_GEWERTETE_RUNDE = 2;
	private static final String DURCHSTRICH = "\u0336";

	private SchweizerTurnierTestDaten testDaten;
	private XSpreadsheetDocument zweitesDokument;

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(42L);
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
		if (zweitesDokument != null) {
			OfficeDocumentHelper.closeDoc(zweitesDokument);
			zweitesDokument = null;
		}
	}

	@Test
	void nichtGewerteteRundeZaehltNichtInDerRangliste() throws Exception {
		turnierMitDreiRundenErstellen();
		var bisRunde1 = leseRangliste(1);
		var bisRunde2 = leseRangliste(2);
		var alle = leseRangliste(ANZ_RUNDEN);

		markiereRunde2();
		var ohneRunde2 = leseRangliste(ANZ_RUNDEN);

		assertThat(ohneRunde2).hasSameSizeAs(alle);
		ohneRunde2.forEach((teamNr, erg) -> {
			int siegeRunde2 = bisRunde2.get(teamNr).siege() - bisRunde1.get(teamNr).siege();
			int punkteRunde2 = bisRunde2.get(teamNr).erzieltePunkte() - bisRunde1.get(teamNr).erzieltePunkte();
			int gegnerRunde2 = bisRunde2.get(teamNr).gegnerNrn().size() - bisRunde1.get(teamNr).gegnerNrn().size();
			assertThat(erg.siege()).as("Siege Team %d ohne Runde 2", teamNr)
					.isEqualTo(alle.get(teamNr).siege() - siegeRunde2);
			assertThat(erg.erzieltePunkte()).as("Punkte Team %d ohne Runde 2", teamNr)
					.isEqualTo(alle.get(teamNr).erzieltePunkte() - punkteRunde2);
			assertThat(erg.gegnerNrn()).as("Gegner (BHZ) Team %d ohne Runde 2", teamNr)
					.hasSize(alle.get(teamNr).gegnerNrn().size() - gegnerRunde2);
		});
	}

	@Test
	void nichtGewerteteRundeZaehltNichtInDerPaarungshistorie() throws Exception {
		turnierMitDreiRundenErstellen();
		var auslosung = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		TeamMeldungen aktive = auslosung.getMeldeListe().getAktiveMeldungen();
		var alle = nachTeam(auslosung.gespieltenRundenEinlesen(aktive, 1, ANZ_RUNDEN));
		var nurRunde2 = nachTeam(auslosung.gespieltenRundenEinlesen(aktive, NICHT_GEWERTETE_RUNDE,
				NICHT_GEWERTETE_RUNDE));

		markiereRunde2();
		var ohneRunde2 = nachTeam(auslosung.gespieltenRundenEinlesen(aktive, 1, ANZ_RUNDEN));

		ohneRunde2.forEach((teamNr, erg) -> {
			List<Integer> erwarteteGegner = new ArrayList<>(alle.get(teamNr).gegnerNrn());
			nurRunde2.get(teamNr).gegnerNrn().forEach(gegner -> erwarteteGegner.remove(gegner));
			assertThat(erg.gegnerNrn()).as("Gegner-Historie Team %d ohne Runde 2", teamNr)
					.containsExactlyInAnyOrderElementsOf(erwarteteGegner);
		});
	}

	@Test
	void umschaltenMarkiertBlattUndNimmtMarkierungWiederZurueck() throws Exception {
		turnierMitDreiRundenErstellen();
		MessageBox.setDialogeUeberspringen(true);
		String rundenSchluessel = SheetMetadataHelper.schluesselSchweizerSpielrunde(NICHT_GEWERTETE_RUNDE);
		XSpreadsheet runde2 = SheetMetadataHelper.findeSheet(doc, rundenSchluessel).orElseThrow();

		sheetHlp.setActiveSheet(runde2);
		new SchweizerSpielrundeWertungUmschalten(wkingSpreadsheet).doRun();

		assertThat(RundenWertungHelper.istNichtGewertet(doc, rundenSchluessel)).as("Runde 2 nicht gewertet").isTrue();
		assertThat(tabFarbe(runde2)).isEqualTo(AbstractSpielrundeWertungUmschalten.TAB_FARBE_NICHT_GEWERTET);
		assertThat(SheetMetadataHelper.findeSheet(doc, rundenSchluessel)).as("Blatt bleibt erhalten").isPresent();
		assertThat(RundenWertungHelper.istNichtGewertet(doc, SheetMetadataHelper.schluesselSchweizerSpielrunde(1)))
				.as("andere Runden bleiben gewertet").isFalse();

		sheetHlp.setActiveSheet(runde2);
		new SchweizerSpielrundeWertungUmschalten(wkingSpreadsheet).doRun();

		assertThat(RundenWertungHelper.istNichtGewertet(doc, rundenSchluessel)).as("Runde 2 wieder gewertet").isFalse();
		assertThat(tabFarbe(runde2)).isEqualTo(testDaten.getKonfigurationSheet().getSpielrundeTabFarbe());
	}

	@Test
	void umschaltenAufFremdemBlattSetztKeineMarkierung() throws Exception {
		turnierMitDreiRundenErstellen();
		MessageBox.setDialogeUeberspringen(true);

		sheetHlp.setActiveSheet(SheetMetadataHelper
				.findeSheet(doc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE).orElseThrow());
		new SchweizerSpielrundeWertungUmschalten(wkingSpreadsheet).doRun();

		assertThat(SheetMetadataHelper.getSchluesselMitPrefix(doc, SheetMetadataHelper.PRAEFIX_NICHT_GEWERTET))
				.as("Rangliste ist keine Spielrunde").isEmpty();
	}

	@Test
	void markierungBleibtBeiAndererRundeNichtHaengen() throws Exception {
		turnierMitDreiRundenErstellen();
		markiereRunde2();

		assertThat(SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.schluesselSchweizerSpielrunde(2)))
				.as("Identitäts-Schlüssel der Runde bleibt neben der Markierung bestehen").isPresent();
		assertThat(SheetMetadataHelper.getSchluesselMitPrefix(doc,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX))
				.as("Markierung zählt nicht als Spielrunde").hasSize(ANZ_RUNDEN);
	}

	@Test
	void sidebarListeStreichtNichtGewerteteRundeDurch() throws Exception {
		turnierMitDreiRundenErstellen();
		markiereRunde2();

		Map<String, String> anzeige = new SheetBaumOrganisierer()
				.baumAufbauen(doc, Set.of(), Set.of(), Set.of()).stream()
				.filter(BlattKnoten.class::isInstance).map(BlattKnoten.class::cast)
				.collect(Collectors.toMap(BlattKnoten::metadatenSchluessel, BlattKnoten::anzeigeText));

		assertThat(anzeige.get(SheetMetadataHelper.schluesselSchweizerSpielrunde(NICHT_GEWERTETE_RUNDE)))
				.as("nicht gewertete Runde durchgestrichen").contains(DURCHSTRICH);
		assertThat(anzeige.get(SheetMetadataHelper.schluesselSchweizerSpielrunde(1)))
				.as("gewertete Runde unverändert").isNotNull().doesNotContain(DURCHSTRICH);
		assertThat(anzeige.get(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE))
				.as("Rangliste unverändert").isNotNull().doesNotContain(DURCHSTRICH);
	}

	/**
	 * Pflicht-Regressionstest „mehrere offene Turnier-Dokumente": Die Markierung gilt nur für das
	 * gemeinte Dokument, auch wenn ein anderes Turnier-Dokument den UI-Fokus hält.
	 */
	@Test
	void markierungGiltNurFuerGemeintesDokumentTrotzFokusAufAnderem() throws Exception {
		turnierMitDreiRundenErstellen();
		zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
		assertThat(zweitesDokument).as("zweites Dokument konnte nicht erstellt werden").isNotNull();
		WorkingSpreadsheet zweitesWs = new WorkingSpreadsheet(starter.getxComponentContext(), zweitesDokument);
		var zweiteTestDaten = new SchweizerTurnierTestDaten(zweitesWs);
		zweiteTestDaten.generate(ANZ_RUNDEN, true);
		var zweitesVorher = leseRangliste(zweitesWs, zweiteTestDaten, ANZ_RUNDEN);
		var bisRunde1 = leseRangliste(1);
		var bisRunde2 = leseRangliste(2);
		var alle = leseRangliste(ANZ_RUNDEN);

		String rundenSchluessel = SheetMetadataHelper.schluesselSchweizerSpielrunde(NICHT_GEWERTETE_RUNDE);
		sheetHlp.setActiveSheet(SheetMetadataHelper.findeSheet(doc, rundenSchluessel).orElseThrow());
		fokussiere(zweitesDokument);
		MessageBox.setDialogeUeberspringen(true);
		new SchweizerSpielrundeWertungUmschalten(wkingSpreadsheet).doRun();

		assertThat(RundenWertungHelper.istNichtGewertet(doc, rundenSchluessel))
				.as("Markierung im gemeinten Dokument A").isTrue();
		assertThat(RundenWertungHelper.istNichtGewertet(zweitesDokument, rundenSchluessel))
				.as("keine Markierung im fokussierten Dokument B").isFalse();
		leseRangliste(ANZ_RUNDEN).forEach((teamNr, erg) -> assertThat(erg.siege())
				.as("Dokument A: Siege Team %d ohne Runde 2", teamNr)
				.isEqualTo(alle.get(teamNr).siege() - (bisRunde2.get(teamNr).siege() - bisRunde1.get(teamNr).siege())));
		assertThat(leseRangliste(zweitesWs, zweiteTestDaten, ANZ_RUNDEN))
				.as("Dokument B wertet weiterhin alle Runden").isEqualTo(zweitesVorher);
	}

	private void turnierMitDreiRundenErstellen() throws Exception {
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet);
		testDaten.generate(ANZ_RUNDEN, true);
	}

	private void markiereRunde2() {
		String rundenSchluessel = SheetMetadataHelper.schluesselSchweizerSpielrunde(NICHT_GEWERTETE_RUNDE);
		XSpreadsheet runde2 = SheetMetadataHelper.findeSheet(doc, rundenSchluessel).orElseThrow();
		RundenWertungHelper.setzeNichtGewertet(doc, runde2, rundenSchluessel, true);
	}

	private Map<Integer, SchweizerTeamErgebnis> leseRangliste(int anzRunden) throws Exception {
		return leseRangliste(wkingSpreadsheet, testDaten, anzRunden);
	}

	private static Map<Integer, SchweizerTeamErgebnis> leseRangliste(WorkingSpreadsheet ws,
			SchweizerTurnierTestDaten daten, int anzRunden) throws Exception {
		var meldeliste = daten.getMeldeListe();
		var konfig = daten.getKonfigurationSheet();
		return nachTeam(SchweizerRundenLeser.leseErgebnisse(ws, meldeliste.getAktiveMeldungen(),
				anzRunden, SheetMetadataHelper::schluesselSchweizerSpielrunde,
				SchweizerAbstractSpielrundeSheet.SHEET_NAMEN, meldeliste, konfig.getFreispielPunktePlus(),
				konfig.getFreispielPunkteMinus()));
	}

	private static Map<Integer, SchweizerTeamErgebnis> nachTeam(List<SchweizerTeamErgebnis> ergebnisse) {
		return ergebnisse.stream().collect(Collectors.toMap(SchweizerTeamErgebnis::teamNr, Function.identity()));
	}

	/** Gleiche Fokus-Sequenz wie {@code TurnierSystemNeueDateiAuswahlDialog.fokussiereDokument()}. */
	private static void fokussiere(XSpreadsheetDocument dokument) {
		XFrame frame = Lo.qi(XModel.class, dokument).getCurrentController().getFrame();
		frame.activate();
		XTopWindow topWindow = Lo.qi(XTopWindow.class, frame.getContainerWindow());
		if (topWindow != null) {
			topWindow.toFront();
		}
	}

	private static int tabFarbe(XSpreadsheet sheet) throws Exception {
		return (Integer) Lo.qi(XPropertySet.class, sheet).getPropertyValue("TabColor");
	}
}
