/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.NichtGewertetFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RundenWertungHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;
import de.petanqueturniermanager.supermelee.RanglisteTestDaten;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;
import de.petanqueturniermanager.supermelee.spielrunde.SupermeleeSpielrundeWertungUmschalten;

/**
 * Supermêlée: Eine nicht gewertete Spielrunde bleibt in der Spieltag-Rangliste als leere Spalten
 * stehen und geht weder mit Ergebnissen noch mit Ersatzpunkten in die Summen ein.
 */
class SpieltagRanglisteNichtGewerteteRundeUITest extends BaseCalcUITest {

	private static final int ANZ_RUNDEN = 3;
	private static final int NICHT_GEWERTETE_RUNDE = 2;
	private static final SpielTagNr SPIELTAG = SpielTagNr.from(1);

	@BeforeEach
	void spielrundenErstellen() throws Exception {
		new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc).run();
		new RanglisteTestDaten<>(wkingSpreadsheet, sheetHlp, this).erstelleTestSpielrunden(ANZ_RUNDEN, false);
	}

	@Test
	void nichtGewerteteRundeBleibtLeerUndZaehltNichtInDenSummen() throws Exception {
		String rundenSchluessel = SheetMetadataHelper.schluesselSupermeleeSpielrunde(SPIELTAG.getNr(),
				NICHT_GEWERTETE_RUNDE);
		XSpreadsheet runde2 = SheetMetadataHelper.findeSheet(doc, rundenSchluessel).orElseThrow();
		RundenWertungHelper.setzeNichtGewertet(doc, runde2, rundenSchluessel, true);

		SpieltagRanglisteSheet rangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
		rangliste.generate(SPIELTAG);

		pruefeRanglisteOhneRunde2(rangliste);
	}

	@Test
	void umschaltenBautSpieltagRanglisteOhneDieRundeNeuAuf() throws Exception {
		SpieltagRanglisteSheet rangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
		rangliste.generate(SPIELTAG);
		MessageBox.setDialogeUeberspringen(true);
		String rundenSchluessel = SheetMetadataHelper.schluesselSupermeleeSpielrunde(SPIELTAG.getNr(),
				NICHT_GEWERTETE_RUNDE);
		XSpreadsheet runde2 = SheetMetadataHelper.findeSheet(doc, rundenSchluessel).orElseThrow();
		sheetHlp.setActiveSheet(runde2);

		new SupermeleeSpielrundeWertungUmschalten(wkingSpreadsheet).doRun();

		assertThat(RundenWertungHelper.istNichtGewertet(doc, rundenSchluessel)).isTrue();
		assertThat(NichtGewertetFormatHelper.istVorangestellt(doc, runde2,
				SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE))
				.as("Ergebniszelle der Spielrunde grau/durchgestrichen").isTrue();
		assertThat(NichtGewertetFormatHelper.istVorangestellt(doc, runde2, SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE,
				SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE)).as("Hilfsspalten bleiben unberührt").isFalse();
		SpieltagRanglisteSheet neueRangliste = new SpieltagRanglisteSheet(wkingSpreadsheet, SPIELTAG);
		neueRangliste.setSpieltagNr(SPIELTAG);
		pruefeRanglisteOhneRunde2(neueRangliste);
	}

	private void pruefeRanglisteOhneRunde2(SpieltagRanglisteSheet rangliste) throws Exception {
		assertThat(rangliste.getAnzahlRunden()).as("die Runde bleibt als Spalte erhalten").isEqualTo(ANZ_RUNDEN);
		XSpreadsheet sheet = rangliste.getXSpreadSheet();
		int ersteSummeSpalte = rangliste.getErsteSummeSpalte();
		int letzteZeile = rangliste.sucheLetzteZeileMitSpielerNummer();
		RangeData daten = RangeHelper.from(sheet, doc,
				RangePosition.from(SpieltagRanglisteSheet.ERSTE_SPIELRUNDE_SPALTE, SpieltagRanglisteSheet.ERSTE_DATEN_ZEILE,
						ersteSummeSpalte + SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS, letzteZeile))
				.getDataFromRange();
		assertThat(daten).isNotEmpty();

		int summenIndex = ersteSummeSpalte - SpieltagRanglisteSheet.ERSTE_SPIELRUNDE_SPALTE;
		int runde2Index = (NICHT_GEWERTETE_RUNDE - 1) * 2;
		for (RowData zeile : daten) {
			assertThat(zeile.get(runde2Index).getStringVal()).as("Runde 2 Punkte+").isNullOrEmpty();
			assertThat(zeile.get(runde2Index + 1).getStringVal()).as("Runde 2 Punkte-").isNullOrEmpty();
			int punktePlusOhneRunde2 = zeile.get(0).getIntVal(0) + zeile.get(4).getIntVal(0);
			int punkteMinusOhneRunde2 = zeile.get(1).getIntVal(0) + zeile.get(5).getIntVal(0);
			assertThat(zeile.get(summenIndex + SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS).getIntVal(-1))
					.as("Summe Punkte+ = Runde 1 + Runde 3").isEqualTo(punktePlusOhneRunde2);
			assertThat(zeile.get(summenIndex + SuperMeleeSummenSpalten.PUNKTE_MINUS_OFFS).getIntVal(-1))
					.as("Summe Punkte- = Runde 1 + Runde 3").isEqualTo(punkteMinusOhneRunde2);
		}

		String kennzeichen = RangeHelper.from(sheet, doc,
				RangePosition.from(SpieltagRanglisteSheet.ERSTE_SPIELRUNDE_SPALTE + runde2Index,
						AbstractSuperMeleeRanglisteFormatter.DRITTE_KOPFDATEN_ZEILE,
						SpieltagRanglisteSheet.ERSTE_SPIELRUNDE_SPALTE + runde2Index,
						AbstractSuperMeleeRanglisteFormatter.DRITTE_KOPFDATEN_ZEILE))
				.getDataFromRange().get(0).get(0).getStringVal();
		assertThat(kennzeichen).isEqualTo(I18n.get("supermelee.spieltagrangliste.header.nicht.gewertet"));
	}
}
