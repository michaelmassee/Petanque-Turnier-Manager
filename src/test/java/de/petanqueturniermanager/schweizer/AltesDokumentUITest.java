/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;

/**
 * Ein Dokument einer Plugin-Version vor der Team-UUID (5.40.00) wird nicht umgestellt: Jeder Lauf bricht ab, bevor
 * er etwas ändert, auch die gespeicherte Version bleibt. Entscheidend ist die Version, nicht der Inhalt – eine in ein
 * aktuelles Dokument eingefügte, nummerierte Liste ohne UUID wird normal übernommen.
 */
public class AltesDokumentUITest extends BaseCalcUITest {

	private static final int ANZ_RUNDEN = 1;
	private static final String ALTE_VERSION = "5.39.12";
	/** Nr-Spalte der Meldeliste (A) bis zur letzten möglichen Zeile. */
	private static final RangePosition NR_SPALTE = RangePosition.from(0, 0, 0, 998);

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
	public void altesDokumentBrichtAbUndStelltNichtUm() throws GenerateException, InterruptedException {
		XSpreadsheet meldeliste = turnierMitNummernOhneUuid();
		docPropHelper.setStringProperty(BasePropertiesSpalte.KONFIG_PROP_ERSTELLT_MIT_VERSION, ALTE_VERSION);

		SchweizerSpielrundeSheetNaechste runde = naechsteRunde();

		assertThat(runde.isLetzterLaufFehlgeschlagen()).as("Lauf abgebrochen").isTrue();
		assertThat(sheetHlp.findByName(SheetNamen.spielrunde(ANZ_RUNDEN + 1))).as("keine neue Runde").isNull();
		assertThat(ohneUuid(meldeliste)).as("Meldeliste nicht still umgestellt").isTrue();
		assertThat(docPropHelper.getStringProperty(BasePropertiesSpalte.KONFIG_PROP_ERSTELLT_MIT_VERSION, ""))
				.as("gespeicherte Version nicht angehoben").isEqualTo(ALTE_VERSION);
	}

	@Test
	public void eingefuegteNummernInAktuellemDokumentWerdenUebernommen() throws GenerateException, InterruptedException {
		XSpreadsheet meldeliste = turnierMitNummernOhneUuid();
		docPropHelper.setStringProperty(BasePropertiesSpalte.KONFIG_PROP_ERSTELLT_MIT_VERSION,
				ExtensionsHelper.from(wkingSpreadsheet.getxContext()).getVersionNummer());

		SchweizerSpielrundeSheetNaechste runde = naechsteRunde();

		assertThat(runde.isLetzterLaufFehlgeschlagen()).as("Lauf erfolgreich").isFalse();
		assertThat(ohneUuid(meldeliste)).as("Nr-Zellen tragen jetzt UUIDs").isFalse();
	}

	/** Schweizer Turnier mit einer Runde, die Nr-Zellen danach als reine Werte (wie eingefügt). */
	private XSpreadsheet turnierMitNummernOhneUuid() throws GenerateException {
		new SchweizerTurnierTestDaten(wkingSpreadsheet).generate(ANZ_RUNDEN, false);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SCHWEIZER.getId());
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		RangeHelper nrSpalte = RangeHelper.from(meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				NR_SPALTE);
		RangeData werte = nrSpalte.getDataFromRange();
		nrSpalte.setDataInRange(werte);
		assertThat(ohneUuid(meldeliste)).as("Nr-Zellen ohne UUID hergestellt").isTrue();
		return meldeliste;
	}

	private SchweizerSpielrundeSheetNaechste naechsteRunde() throws InterruptedException {
		SchweizerSpielrundeSheetNaechste runde = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		runde.start();
		runde.join();
		return runde;
	}

	private boolean ohneUuid(XSpreadsheet meldeliste) {
		return Arrays.stream(sheetHlp.getFormulaArrayFromRange(meldeliste, NR_SPALTE))
				.noneMatch(zeile -> zeile.length > 0 && TeamUuidFormel.findeUuid(zeile[0]).isPresent());
	}
}
