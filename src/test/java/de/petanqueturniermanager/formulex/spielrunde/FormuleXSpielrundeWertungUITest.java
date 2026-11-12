/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.text.XText;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.NichtGewertetFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RundenWertungHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Formule X: Eine nicht gewertete Spielrunde zählt nicht in der Rangliste; ein zweites Umschalten
 * nimmt sie wieder auf.
 */
class FormuleXSpielrundeWertungUITest extends BaseCalcUITest {

	private static final int ANZ_RUNDEN = 3;
	private static final int NICHT_GEWERTETE_RUNDE = 2;
	private static final int ERG_A = FormuleXAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE;
	private static final int ERSTE_DATEN_ZEILE = FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(42L);
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	void nichtGewerteteRundeZaehltNichtInDerRangliste() throws Exception {
		new FormuleXTurnierTestDaten(wkingSpreadsheet).generate(ANZ_RUNDEN, true);
		int siegeAlleRunden = summeSiege();
		// Je Runde gleich viele Sieger (Spiele + Freilos, kein Unentschieden)
		assertThat(siegeAlleRunden % ANZ_RUNDEN).isZero();
		int siegeJeRunde = siegeAlleRunden / ANZ_RUNDEN;

		MessageBox.setDialogeUeberspringen(true);
		String rundenSchluessel = SheetMetadataHelper.schluesselFormuleXSpielrunde(NICHT_GEWERTETE_RUNDE);
		XSpreadsheet runde2 = SheetMetadataHelper.findeSheet(doc, rundenSchluessel).orElseThrow();
		sheetHlp.setActiveSheet(runde2);
		new FormuleXSpielrundeWertungUmschalten(wkingSpreadsheet).doRun();

		assertThat(RundenWertungHelper.istNichtGewertet(doc, rundenSchluessel)).isTrue();
		assertThat(summeSiege()).as("Rangliste ohne Runde 2").isEqualTo(siegeAlleRunden - siegeJeRunde);
		assertThat(NichtGewertetFormatHelper.istVorangestellt(doc, runde2, ERG_A, ERSTE_DATEN_ZEILE))
				.as("Ergebniszelle grau/durchgestrichen").isTrue();
		assertThat(titel(runde2)).endsWith(I18n.get("spielrunde.wertung.titel.zusatz"));

		sheetHlp.setActiveSheet(runde2);
		new FormuleXSpielrundeWertungUmschalten(wkingSpreadsheet).doRun();

		assertThat(RundenWertungHelper.istNichtGewertet(doc, rundenSchluessel)).isFalse();
		assertThat(summeSiege()).as("Rangliste wieder mit Runde 2").isEqualTo(siegeAlleRunden);
		assertThat(NichtGewertetFormatHelper.istVorangestellt(doc, runde2, ERG_A, ERSTE_DATEN_ZEILE))
				.as("Kennzeichnung entfernt").isFalse();
		assertThat(titel(runde2)).doesNotEndWith(I18n.get("spielrunde.wertung.titel.zusatz"));
	}

	private static String titel(XSpreadsheet sheet) throws Exception {
		return Lo.qi(XText.class, sheet.getCellByPosition(1, 0)).getString();
	}

	private int summeSiege() throws Exception {
		XSpreadsheet rangliste = SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE)
				.orElseThrow();
		int summe = 0;
		for (RowData zeile : RangeHelper.from(rangliste, doc, RangePosition.from(FormuleXRanglisteSheet.TEAM_NR_SPALTE,
				FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE, FormuleXRanglisteSheet.SIEGE_SPALTE,
				FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE + 199)).getDataFromRange()) {
			if (zeile.get(0).getIntVal(0) <= 0) {
				break;
			}
			summe += zeile.get(FormuleXRanglisteSheet.SIEGE_SPALTE).getIntVal(0);
		}
		return summe;
	}
}
