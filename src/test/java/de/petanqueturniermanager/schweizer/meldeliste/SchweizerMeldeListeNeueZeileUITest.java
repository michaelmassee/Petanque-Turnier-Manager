/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.blattschutz.SchweizerBlattschutzKonfiguration;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.MeldelisteSchreibException;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.MeldelisteStatus;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Regressionstest: Eine neu eingetragene Meldung ohne Nummer in einer Doublette-Meldeliste ohne
 * Teamname- und Vereinsspalte meldete beim Aktualisieren „Startnummer 2 ist mehrfach vergeben“,
 * obwohl alle vorhandenen Nummern eindeutig waren.
 */
class SchweizerMeldeListeNeueZeileUITest extends BaseCalcUITest {

	private static final String[][] MELDUNGEN = { { "Basti", "Bielke", "Maria", "Bieder" },
			{ "Christian", "Birk", "Ralf", "Bender" }, { "Kai", "Bepperling", "Michael", "Bender" },
			{ "Walter", "Bischoff", "Lisa", "Bischoff" }, { "Enrico", "Birk", "Harry", "Bernadis" },
			{ "Victor", "Bockelmann", "Carla", "Bockelmann" }, { "Martina", "Bockelmann", "Sieghardt", "Bitsch" },
			{ "Günter", "Bohlender", "Marco", "Bischoff" } };
	private static final String[] NEUE_MELDUNG = { "Joachim", "Bopf", "Danika", "Bopf" };

	@Test
	void neueMeldungOhneNummerBehaeltBestehendeNummern() throws GenerateException {
		pruefeNeueMeldung(false);
	}

	@Test
	void neueMeldungOhneNummerImTurnierModus() throws GenerateException {
		try {
			pruefeNeueMeldung(true);
		} finally {
			BlattschutzManager.get().entsperren(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
			TurnierModus.get().setAktivForTest(false);
		}
	}

	/** Ablauf aus dem Fehlerbericht: Teams über die Spieler-DB-Übernahme eintragen, dann aktualisieren. */
	@Test
	void neueMeldungUeberSpielerDbUebernahme() throws GenerateException, MeldelisteSchreibException {
		new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.DOUBLETTE, false,
				false);
		var meldeListe = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
		int ersteZeile = meldeListe.getErsteDatenZiele();

		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SCHWEIZER.getId());
		uebernehmen(MELDUNGEN);
		assertThat(MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow().getMeldelisteStatus())
				.as("Übernommene Teams zählen als angemeldet, nicht als eingecheckt")
				.isEqualTo(new MeldelisteStatus(MELDUNGEN.length, 0, MELDUNGEN.length));
		aktualisieren();
		Map<String, Integer> nrVorher = nrJeMeldung(meldeListe, ersteZeile, MELDUNGEN.length);
		assertThat(nrVorher).as("Vorbedingung: alle Meldungen nummeriert").hasSize(MELDUNGEN.length);

		uebernehmen(new String[][] { NEUE_MELDUNG });
		aktualisieren();

		Map<String, Integer> nrNachher = nrJeMeldung(meldeListe, ersteZeile, MELDUNGEN.length + 1);
		assertThat(nrNachher).as("Bestehende Meldungen behalten ihre Nummer").containsAllEntriesOf(nrVorher);
		assertThat(nrNachher.get(String.join(" ", NEUE_MELDUNG))).as("Neue Meldung erhält die nächste Nummer")
				.isEqualTo(MELDUNGEN.length + 1);
	}

	/**
	 * Ablauf aus dem Fehlerbericht im Turnier-Modus: Die Meldeliste ist geschützt, die Spieler-DB
	 * schreibt in eigenem Blattschutz-Scope, danach sortiert die Aktualisierung die Nr-Spalte. Das
	 * Sortieren lief ohne Entsperren und blieb auf dem geschützten Blatt wirkungslos – die neue
	 * Meldung bekam dann Nr 2.
	 */
	@Test
	void neueMeldungUeberSpielerDbUebernahmeImTurnierModus() throws GenerateException, MeldelisteSchreibException {
		new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.DOUBLETTE, false,
				false);
		var meldeListe = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
		int ersteZeile = meldeListe.getErsteDatenZiele();
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SCHWEIZER.getId());
		try {
			TurnierModus.get().setAktivForTest(true);
			BlattschutzManager.get().schuetzen(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);

			uebernehmen(MELDUNGEN);
			aktualisieren();
			Map<String, Integer> nrVorher = nrJeMeldung(meldeListe, ersteZeile, MELDUNGEN.length);
			assertThat(nrVorher.values()).as("Vorbedingung: alle Meldungen eindeutig nummeriert")
					.containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8);

			uebernehmen(new String[][] { NEUE_MELDUNG });
			aktualisieren();

			Map<String, Integer> nrNachher = nrJeMeldung(meldeListe, ersteZeile, MELDUNGEN.length + 1);
			assertThat(nrNachher).as("Bestehende Meldungen behalten ihre Nummer").containsAllEntriesOf(nrVorher);
			assertThat(nrNachher.get(String.join(" ", NEUE_MELDUNG))).as("Neue Meldung erhält die nächste Nummer")
					.isEqualTo(MELDUNGEN.length + 1);
		} finally {
			BlattschutzManager.get().entsperren(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
			TurnierModus.get().setAktivForTest(false);
		}
	}

	/**
	 * LibreOffice bricht {@code XSortable.sort} auf einem gesperrten Bereich lautlos ab
	 * ({@code SortOperation}: {@code ScEditableTester} + {@code bApi}). Die Nr-Spalte ist im
	 * Turnier-Modus gesperrt – das Sortieren muss das Blatt im Scope deshalb selbst entsperren.
	 */
	@Test
	void sortierenImTurnierModusWirktAufGeschuetztemBlatt() throws GenerateException {
		new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.DOUBLETTE, false,
				false);
		var meldeListe = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
		int ersteZeile = meldeListe.getErsteDatenZiele();
		namenSchreiben(meldeListe, ersteZeile, MELDUNGEN);
		aktualisieren();
		try {
			TurnierModus.get().setAktivForTest(true);
			BlattschutzManager.get().schuetzen(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
			try (var scope = BlattschutzManager.get().scopeFuer(TurnierSystem.SCHWEIZER, wkingSpreadsheet)) {
				meldeListe.doSort(meldeListe.getTeamNrSpalte(), false);
			}
			assertThat(sheetHlp.getIntFromCell(meldeListe.getXSpreadSheet(),
					Position.from(meldeListe.getTeamNrSpalte(), ersteZeile))).as("Höchste Nr steht nach dem Sortieren oben")
					.isEqualTo(MELDUNGEN.length);
		} finally {
			BlattschutzManager.get().entsperren(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
			TurnierModus.get().setAktivForTest(false);
		}
	}

	/** Wie der Spieler-DB-Dialog: Schreiben im eigenen Blattschutz-Scope. */
	private void uebernehmen(String[][] meldungen) throws MeldelisteSchreibException {
		MeldelisteZiel ziel = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
		try (var scope = BlattschutzManager.get().scopeFuer(TurnierSystem.SCHWEIZER, wkingSpreadsheet)) {
			for (String[] meldung : meldungen) {
				ziel.schreibeBlock(List.of(spieler(meldung[0], meldung[1]), spieler(meldung[2], meldung[3])));
			}
		}
	}

	private static SpielerMitVerein spieler(String vorname, String nachname) {
		return new SpielerMitVerein(0, vorname, nachname, null, null, List.of(), List.of(), null);
	}

	private void pruefeNeueMeldung(boolean turnierModus) throws GenerateException {
		new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.DOUBLETTE, false,
				false);
		var meldeListe = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
		int ersteZeile = meldeListe.getErsteDatenZiele();

		namenSchreiben(meldeListe, ersteZeile, MELDUNGEN);
		aktualisieren();
		Map<String, Integer> nrVorher = nrJeMeldung(meldeListe, ersteZeile, MELDUNGEN.length);
		assertThat(nrVorher).as("Vorbedingung: alle Meldungen nummeriert").hasSize(MELDUNGEN.length);

		// Anwender trägt die neue Meldung in die (auch im Turnier-Modus freigegebenen) Namenszellen ein
		namenSchreiben(meldeListe, ersteZeile + MELDUNGEN.length, new String[][] { NEUE_MELDUNG });
		if (turnierModus) {
			TurnierModus.get().setAktivForTest(true);
			BlattschutzManager.get().schuetzen(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
			assertThat(com.sun.star.uno.UnoRuntime.queryInterface(com.sun.star.util.XProtectable.class,
					meldeListe.getXSpreadSheet()).isProtected()).as("DEBUG geschützt").isTrue();
		}
		aktualisieren();

		Map<String, Integer> nrNachher = nrJeMeldung(meldeListe, ersteZeile, MELDUNGEN.length + 1);
		assertThat(nrNachher).as("Bestehende Meldungen behalten ihre Nummer").containsAllEntriesOf(nrVorher);
		assertThat(nrNachher.get(String.join(" ", NEUE_MELDUNG))).as("Neue Meldung erhält die nächste Nummer")
				.isEqualTo(MELDUNGEN.length + 1);
	}

	/** Wie der Menüpunkt „Meldeliste aktualisieren“: synchron über den SheetRunner. */
	private void aktualisieren() {
		new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet).run();
	}

	private void namenSchreiben(SchweizerMeldeListeSheetUpdate meldeListe, int ersteZeile, String[][] meldungen)
			throws GenerateException {
		RangeData daten = new RangeData();
		for (String[] meldung : meldungen) {
			RowData zeile = daten.addNewRow();
			for (String name : meldung) {
				zeile.newString(name);
			}
		}
		RangeHelper.from(meldeListe.getXSpreadSheet(), doc,
				daten.getRangePosition(Position.from(meldeListe.getVornameSpalte(0), ersteZeile)))
				.setDataInRange(daten);
	}

	/** Liest „Vorname Nachname Vorname Nachname“ → Nr aus der Meldeliste. */
	private Map<String, Integer> nrJeMeldung(SchweizerMeldeListeSheetUpdate meldeListe, int ersteZeile, int anzZeilen)
			throws GenerateException {
		RangeData zeilen = RangeHelper.from(meldeListe.getXSpreadSheet(), doc,
				RangePosition.from(meldeListe.getTeamNrSpalte(), ersteZeile, meldeListe.getNachnameSpalte(1),
						ersteZeile + anzZeilen - 1))
				.getDataFromRange();
		Map<String, Integer> nrJeMeldung = new HashMap<>();
		for (RowData zeile : zeilen) {
			String namen = String.join(" ", zeile.get(1).getStringVal(), zeile.get(2).getStringVal(),
					zeile.get(3).getStringVal(), zeile.get(4).getStringVal());
			nrJeMeldung.put(namen, zeile.get(0).getIntVal(0));
		}
		return nrJeMeldung;
	}
}
