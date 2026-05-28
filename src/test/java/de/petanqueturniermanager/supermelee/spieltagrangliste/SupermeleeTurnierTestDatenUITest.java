package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XNamedRanges;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * UITest für das vollständige Supermelee-Beispielturnier (100 Spieler, 5 Spieltage):
 * Meldeliste plus Spieltag-Ranglisten 1 und 5 gegen JSON-Referenzen validiert.
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}; bei
 * Algorithmen-Änderungen Referenz-JSONs neu erfassen (writeToJson temporär aktivieren).
 */
public class SupermeleeTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int ANZ_SPIELER = 100;
	private static final int ANZ_SPIELTAGE = 5;
	private static final int ANZ_SPIELRUNDEN_PRO_SPIELTAG = 4;
	private static final int MIN_ANZ_AKTIVE_SPIELER = 30;
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(SEED_FUER_TESTS);
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	public void testSupermeleeTurnier100SpielerFuenfSpieltage() throws GenerateException {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();

		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.spieltagRangliste(1)))
				.as("Spieltag-Rangliste 1 muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.spieltagRangliste(5)))
				.as("Spieltag-Rangliste 5 muss existieren").isNotNull();

		validiereMindestensAktiveProSpieltag();
		validiereMeldelistePerJson();
		validiereSpieltagRanglistePerJson(1, "supermelee-spieltagrangliste-1.json");
		validiereSpieltagRanglistePerJson(2, "supermelee-spieltagrangliste-2.json");
		validiereSpieltagRanglistePerJson(3, "supermelee-spieltagrangliste-3.json");
		validiereSpieltagRanglistePerJson(4, "supermelee-spieltagrangliste-4.json");
		validiereSpieltagRanglistePerJson(5, "supermelee-spieltagrangliste-5.json");
	}

	/**
	 * Garantie aus der Testdaten-Generierung: jeder Spieltag hat mindestens
	 * {@link #MIN_ANZ_AKTIVE_SPIELER} aktive Meldungen. Die Meldeliste-JSON erfasst nur
	 * Nr/Vorname/Nachname (nicht die Spieltag-Aktiv-Spalten), daher ist diese Assertion der
	 * eigentliche Nachweis – eine gruene JSON-Validierung allein belegt die Grenze nicht.
	 */
	private void validiereMindestensAktiveProSpieltag() throws GenerateException {
		MeldeListeSheet_Update meldeListe = new MeldeListeSheet_Update(wkingSpreadsheet);
		for (int spieltagNr = 1; spieltagNr <= ANZ_SPIELTAGE; spieltagNr++) {
			meldeListe.setSpielTag(SpielTagNr.from(spieltagNr));
			assertThat(meldeListe.getAktiveMeldungen().size())
					.as("Aktive Meldungen Spieltag %d", spieltagNr)
					.isGreaterThanOrEqualTo(MIN_ANZ_AKTIVE_SPIELER);
		}
	}

	private void validiereMeldelistePerJson() throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		// Meldeliste: Spalten 0..2 (Nr, Vorname, Nachname), 100 Spieler-Zeilen ab Zeile 3.
		RangePosition meldelisteRange = RangePosition.from(
				0, MELDELISTE_ERSTE_DATEN_ZEILE,
				2, MELDELISTE_ERSTE_DATEN_ZEILE + ANZ_SPIELER - 1);

		// writeToJson("supermelee-meldeliste.json", meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = SupermeleeTurnierTestDatenUITest.class.getResourceAsStream("supermelee-meldeliste.json");
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereSpieltagRanglistePerJson(int spieltagNr, String referenzDatei) throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.spieltagRangliste(spieltagNr));
		assertThat(sheet).as("Spieltag-Rangliste %d", spieltagNr).isNotNull();

		// Großzügiger Bereich – Spalten 0..8 für 100 Spieler.
		RangePosition ranglisteRange = RangePosition.from(0, 0, 8, 110);

		// writeToJson(referenzDatei, ranglisteRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = SupermeleeTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	/**
	 * Invariante „höchstens ein Identitäts-Schlüssel pro Blatt": nach vollständiger Generierung
	 * (inkl. Teams, SpielrundePlan, Endrangliste) darf kein Blatt von zwei verschiedenen
	 * Nicht-Score-Schlüsseln referenziert werden – sonst entstünden Doppel-Einträge in der Sidebar.
	 */
	@Test
	public void keinBlattMitMehrerenIdentitaetsSchluesseln() throws Exception {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
		new de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet(wkingSpreadsheet).run();
		new de.petanqueturniermanager.supermelee.spielrunde.SpielrundePlan(wkingSpreadsheet).run();
		new de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet(wkingSpreadsheet).run();

		Map<String, List<String>> proBlatt = identitaetsSchluesselProBlatt();
		assertThat(proBlatt).as("kein Blatt darf mehr als einen Identitäts-Schlüssel tragen")
				.allSatisfy((blatt, schluessel) -> assertThat(schluessel)
						.as("Schlüssel auf Blatt '%s'", blatt).hasSize(1));
	}

	/**
	 * Korrektheit der PTM-Metadaten: nach voller Generierung (Meldeliste, alle Spieltage mit
	 * Anmeldungen/Teilnehmer/4 Spielrunden/Rangliste, dazu Teams- und Endrangliste-Blatt) muss
	 * jedes Blatt exakt seinen erwarteten Identitäts-Schlüssel tragen – und kein weiteres Blatt
	 * einen unerwarteten. Die Soll-Tabelle ist ein vom Produktivcode unabhängiger Gegencheck.
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel() throws Exception {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
		new de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet(wkingSpreadsheet).run();
		new de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet(wkingSpreadsheet).run();

		Map<String, String> erwartung = new LinkedHashMap<>();
		erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE);
		erwartung.put(SheetNamen.supermeleeTeams(), SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_TEAMS);
		erwartung.put(SheetNamen.endrangliste(), SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE);
		for (int spieltag = 1; spieltag <= ANZ_SPIELTAGE; spieltag++) {
			erwartung.put(SheetNamen.checkinListe(spieltag),
					SheetMetadataHelper.schluesselSupermeleeAnmeldungen(spieltag));
			erwartung.put(SheetNamen.teilnehmer(spieltag),
					SheetMetadataHelper.schluesselSupermeleeTeilnehmer(spieltag));
			erwartung.put(SheetNamen.spieltagRangliste(spieltag),
					SheetMetadataHelper.schluesselSpieltagRangliste(spieltag));
			for (int runde = 1; runde <= ANZ_SPIELRUNDEN_PRO_SPIELTAG; runde++) {
				erwartung.put(SheetNamen.supermeleeSpielrunde(spieltag, runde),
						SheetMetadataHelper.schluesselSupermeleeSpielrunde(spieltag, runde));
			}
		}

		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	/**
	 * Schreib-seitiger Purge: schreibt man den Schlüssel eines anderen Turniersystems auf ein
	 * bereits beanspruchtes (gleichnamiges) Blatt, muss der alte Schlüssel verschwinden – das
	 * Blatt darf nie zwei Identitäts-Schlüssel gleichzeitig tragen.
	 */
	@Test
	public void systemwechselSchreibtNurEinenSchluesselAufGeteiltesBlatt() throws Exception {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
		XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();

		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		assertThat(meldeliste).as("Meldeliste muss existieren").isNotNull();
		assertThat(schluesselFuer(meldeliste))
				.as("vor Systemwechsel: nur der Supermelee-Schlüssel")
				.containsExactly(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE);

		// Systemwechsel simulieren: Schweizer beansprucht dasselbe physische "Meldeliste"-Blatt
		SheetMetadataHelper.schreibeSheetMetadaten(xDoc, meldeliste,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE);

		assertThat(schluesselFuer(meldeliste))
				.as("nach Systemwechsel: alter Supermelee-Schlüssel gepurgt, nur Schweizer übrig")
				.containsExactly(SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE);
	}

	/**
	 * Anzeige-seitige Heilung: ein (z.B. durch eine Altversion) künstlich doppelt beschlüsseltes
	 * Blatt darf in der Sidebar-Baumstruktur trotzdem nur EINEN Eintrag erzeugen.
	 */
	@Test
	public void sidebarBaumZeigtDoppeltBeschluesseltesBlattNurEinmal() throws Exception {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
		XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());

		// Fremdschlüssel am Purge vorbei direkt injizieren (simuliert Alt-Dokument)
		injiziereRohenSchluessel(xDoc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE,
				Lo.qi(XNamed.class, meldeliste).getName());
		assertThat(schluesselFuer(meldeliste))
				.as("Injektion erzeugt absichtlich zwei Schlüssel auf demselben Blatt")
				.hasSize(2);

		var baum = new de.petanqueturniermanager.sidebar.sheets.SheetBaumOrganisierer()
				.baumAufbauen(xDoc, Set.of(), Set.of(), Set.of());
		String meldelisteName = Lo.qi(XNamed.class, meldeliste).getName();
		long anzEintraege = baum.stream()
				.filter(e -> e instanceof de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.BlattKnoten)
				.map(e -> (de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.BlattKnoten) e)
				.filter(k -> meldelisteName.equals(Lo.qi(XNamed.class, k.sheet()).getName()))
				.count();
		assertThat(anzEintraege).as("Meldeliste darf trotz Doppel-Schlüssel nur einmal erscheinen").isEqualTo(1);
	}

	/** Schreibt einen Named Range direkt (am schreib-seitigen Purge vorbei), um Alt-Datenstände zu simulieren. */
	private void injiziereRohenSchluessel(XSpreadsheetDocument xDoc, String schluessel, String blattName)
			throws Exception {
		XNamedRanges namedRanges = Lo.qi(XNamedRanges.class,
				Lo.qi(com.sun.star.beans.XPropertySet.class, xDoc).getPropertyValue("NamedRanges"));
		short idx = 0;
		String[] namen = xDoc.getSheets().getElementNames();
		for (short i = 0; i < namen.length; i++) {
			if (blattName.equals(namen[i])) {
				idx = i;
				break;
			}
		}
		com.sun.star.table.CellAddress addr = new com.sun.star.table.CellAddress();
		addr.Sheet = idx;
		namedRanges.addNewByName(schluessel, "$'" + blattName.replace("'", "''") + "'.$A$1", addr, 0);
	}

	/**
	 * Regression im Kiosk-Modus: nach voller Beispielturnier-Generierung (100 Spieler, 5 Spieltage)
	 * muss ein erneutes Update der Spieltag-1-Rangliste unter aktivem TurnierModus durchlaufen
	 * und die Schutz-Invariante erfüllt bleiben.
	 */
	@Test
	public void kioskModus_spieltagRanglisteUpdateNach100SpielerTurnier() throws GenerateException {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
		mitKioskModus(TurnierSystem.SUPERMELEE, () ->
				new SpieltagRanglisteSheet(wkingSpreadsheet, SpielTagNr.from(1)).run());

		assertThat(sheetHlp.findByName(SheetNamen.spieltagRangliste(1)))
				.as("Spieltag-Rangliste 1 muss nach Kiosk-Update weiterhin existieren")
				.isNotNull();
	}
}
