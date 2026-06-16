package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNamed;
import com.sun.star.sheet.XNamedRange;
import com.sun.star.sheet.XNamedRanges;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.XColumnRowRange;
import com.sun.star.table.XTableColumns;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;
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

		var analyseAssert = SupermeleeSpieltagAnalyseAssert.fuer(wkingSpreadsheet);
		for (int spieltagNr = 1; spieltagNr <= ANZ_SPIELTAGE; spieltagNr++) {
			analyseAssert.pruefe(spieltagNr, ANZ_SPIELRUNDEN_PRO_SPIELTAG);
		}
	}

	@Test
	public void endranglisteFaerbtStreichspieltagDirektFuerHtmlUndPdfExport() throws Exception {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
		new EndranglisteSheet(wkingSpreadsheet).run();

		EndranglisteSheet endrangliste = new EndranglisteSheet(wkingSpreadsheet);
		XSpreadsheet sheet = endrangliste.getXSpreadSheet();
		int streichSpalte = EndranglisteSheet.ERSTE_SPIELTAG_SPALTE
				+ (ANZ_SPIELTAGE * SuperMeleeSummenSpalten.ANZAHL_SPALTEN_IN_SUMME)
				+ SuperMeleeSummenSpalten.ANZAHL_SPALTEN_IN_SUMME + 1;

		for (int zeile = EndranglisteSheet.ERSTE_DATEN_ZEILE;
				zeile < EndranglisteSheet.ERSTE_DATEN_ZEILE + ANZ_SPIELER; zeile++) {
			Integer streichSpieltag = sheetHlp.getIntFromCell(sheet, Position.from(streichSpalte, zeile));
			if (streichSpieltag == null || streichSpieltag < 1 || streichSpieltag > ANZ_SPIELTAGE) {
				continue;
			}

			int startSpalte = EndranglisteSheet.ERSTE_SPIELTAG_SPALTE
					+ ((streichSpieltag - 1) * SuperMeleeSummenSpalten.ANZAHL_SPALTEN_IN_SUMME);
			int erwarteteFarbe = (zeile & 1) == 1
					? endrangliste.getKonfigurationSheet().getRanglisteHintergrundFarbeStreichSpieltagGerade()
					: endrangliste.getKonfigurationSheet().getRanglisteHintergrundFarbeStreichSpieltagUnGerade();

			XPropertySet props = Lo.qi(XPropertySet.class, sheet.getCellRangeByPosition(startSpalte, zeile,
					startSpalte + SuperMeleeSummenSpalten.ANZAHL_SPALTEN_IN_SUMME - 1, zeile));
			assertThat(props.getPropertyValue(ICommonProperties.CELL_BACK_COLOR))
					.as("Streich-Spieltag-Block in Zeile %d muss direkt gefaerbt sein", zeile)
					.isEqualTo(erwarteteFarbe);
			return;
		}

		throw new AssertionError("Testdaten muessen mindestens einen Streich-Spieltag erzeugen");
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
				.filter(k -> meldelisteName.equals(k.anzeigeText()))
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

	/**
	 * Anwender-Szenario „Blatt kopieren": kopiert der Anwender ein Turnier-Blatt (hier eine
	 * minimal von Hand aufgebaute Meldeliste mit Identitäts-Schlüssel), erzwingt LibreOffice
	 * einen neuen, eindeutigen Blattnamen. Der dokumentweite PTM-Identitäts-Schlüssel
	 * (globaler Named Range) darf dabei NICHT dupliziert werden: er zeigt weiterhin nur auf das
	 * Originalblatt, die Kopie trägt keinen eigenen Identitäts-Schlüssel. Andernfalls entstünde
	 * ein Doppel-Eintrag in der Sidebar-Blätterliste.
	 */
	@Test
	public void kopiertesBlattErzeugtKeinenDoppeltenIdentitaetsSchluessel() {
		XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
		final String quellName = "Meldeliste";
		final String kopieName = "Meldeliste_2";

		XSpreadsheet quelle = sheetHlp.newIfNotExist(quellName, (short) 0);
		SheetMetadataHelper.schreibeSheetMetadaten(xDoc, quelle,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE);
		assertThat(schluesselFuer(quelle))
				.as("Quellblatt trägt vor dem Kopieren genau den Supermelee-Meldeliste-Schlüssel")
				.containsExactly(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE);

		XSpreadsheets sheets = xDoc.getSheets();

		// Kopie mit neuem, eindeutigem Namen (Index >= Blattanzahl bedeutet „anhängen").
		sheets.copyByName(quellName, kopieName, (short) sheets.getElementNames().length);
		XSpreadsheet kopie = sheetHlp.findByName(kopieName);
		assertThat(kopie).as("Kopie muss unter dem neuen Namen existieren").isNotNull();

		// Kern-Erwartung: der dokumentweite Identitäts-Schlüssel wurde nicht dupliziert.
		assertThat(identitaetsSchluesselProBlatt().get(quellName))
				.as("Identitäts-Schlüssel zeigt nach dem Kopieren weiterhin nur auf das Originalblatt")
				.containsExactly(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE);
		assertThat(schluesselFuer(kopie))
				.as("die Kopie darf keinen (doppelten) Identitäts-Schlüssel tragen")
				.isEmpty();

		// LO erzwingt einen eindeutigen Namen: Kopie auf einen bereits vergebenen Namen schlägt fehl.
		assertThatThrownBy(() -> sheets.copyByName(quellName, quellName, (short) sheets.getElementNames().length))
				.as("LO darf kein Blatt unter einem bereits vergebenen Namen anlegen")
				.isInstanceOf(com.sun.star.uno.RuntimeException.class);
	}

	/**
	 * Anwender-Szenario „Blatt umbenennen": Die Verbindung Identitäts-Schlüssel → Blatt ist
	 * intern index-basiert (Tab-Index in der Named-Range-Referenz), nicht namensbasiert. Wird das
	 * Blatt umbenannt, bleibt der Tab-Index unverändert – der Schlüssel zeigt weiterhin auf
	 * dasselbe (jetzt umbenannte) Blatt, und der Lookup greift über den neuen Namen. Es entsteht
	 * weder ein verwaister noch ein doppelter Schlüssel.
	 */
	@Test
	public void umbenanntesBlattBehaeltSeinenIdentitaetsSchluessel() {
		XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
		final String altName = "Meldeliste";
		final String neuName = "Meldeliste_Umbenannt";

		XSpreadsheet blatt = sheetHlp.newIfNotExist(altName, (short) 0);
		SheetMetadataHelper.schreibeSheetMetadaten(xDoc, blatt,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE);
		assertThat(SheetMetadataHelper.istRegistriertesSheet(xDoc, blatt,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE))
				.as("vor dem Umbenennen ist das Blatt unter dem Schlüssel registriert")
				.isTrue();

		assertThat(sheetHlp.reNameSheet(blatt, neuName)).as("Umbenennen muss gelingen").isTrue();

		assertThat(sheetHlp.findByName(neuName)).as("Blatt unter neuem Namen muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(altName)).as("alter Blattname darf nicht mehr existieren").isNull();

		// Schlüssel folgt dem Blatt: zeigt weiterhin auf dasselbe (umbenannte) Blatt, kein Duplikat, kein Waise.
		assertThat(identitaetsSchluesselProBlatt().get(neuName))
				.as("Identitäts-Schlüssel zeigt nach dem Umbenennen auf das Blatt unter dem neuen Namen")
				.containsExactly(SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE);
		assertThat(identitaetsSchluesselProBlatt()).as("kein Schlüssel hängt am alten Namen")
				.doesNotContainKey(altName);

		// Lookup über den Schlüssel liefert exakt das umbenannte Blatt (index-basierte Auflösung).
		XSpreadsheet aufgeloest = SheetMetadataHelper.findeSheet(xDoc,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE).orElse(null);
		assertThat(aufgeloest).as("Schlüssel-Lookup muss ein Blatt liefern").isNotNull();
		assertThat(Lo.qi(XNamed.class, aufgeloest).getName())
				.as("Schlüssel-Lookup liefert das Blatt unter dem neuen Namen")
				.isEqualTo(neuName);
		assertThat(SheetMetadataHelper.istRegistriertesSheet(xDoc, blatt,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE))
				.as("dieselbe Blatt-Referenz bleibt nach dem Umbenennen registriert")
				.isTrue();
	}

	/**
	 * Belegt gegen echtes LibreOffice den von den Unit-Tests nur gemockten Sachverhalt:
	 * Ein dokumentweiter Named Range mit kaputter Referenz liefert über
	 * {@link XNamedRange#getContent()} (via {@code GRAM_API}) locale-unabhängig {@code #REF!} –
	 * genau die Grundlage von {@code SheetMetadataHelper.istKaputteReferenz}.
	 * <p>
	 * Wichtig: Das bloße Löschen des Blattes erzeugt <em>keinen</em> Waisen – LO entfernt den
	 * abhängigen globalen Named Range dann selbst. Eine kaputte Referenz bei <em>überlebendem</em>
	 * Namen entsteht, wenn der A1-Anker wegbricht; hier wird dazu Spalte A des Blattes gelöscht.
	 * Anschließend muss {@code findeSheet} nichts mehr liefern und
	 * {@code bereinigeVerwaisteMetadaten} den Schlüssel entfernen.
	 */
	@Test
	public void kaputteReferenzLiefertRefFehlerUndWirdBereinigt() throws Exception {
		XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
		final String blattName = "Meldeliste";
		final String schluessel = SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_MELDELISTE;

		XSpreadsheet blatt = sheetHlp.newIfNotExist(blattName, (short) 0);
		SheetMetadataHelper.schreibeSheetMetadaten(xDoc, blatt, schluessel);
		assertThat(SheetMetadataHelper.findeSheet(xDoc, schluessel))
				.as("vor dem Bruch muss das Blatt über den Schlüssel auffindbar sein").isPresent();

		// A1-Anker brechen: Spalte A löschen. Der Name überlebt (Blatt existiert), die Referenz
		// wird zu #REF! – im Gegensatz zum Blatt-Löschen, das den Namen ganz entfernen würde.
		XTableColumns spalten = Lo.qi(XColumnRowRange.class, blatt).getColumns();
		spalten.removeByIndex(0, 1);

		// Kern-Beweis: getContent() rendert die kaputte Referenz locale-unabhängig als "#REF!".
		XNamedRanges namedRanges = Lo.qi(XNamedRanges.class,
				Lo.qi(com.sun.star.beans.XPropertySet.class, xDoc).getPropertyValue("NamedRanges"));
		assertThat(namedRanges.hasByName(schluessel))
				.as("der Named Range überlebt das Spalten-Löschen (nur die Referenz bricht)").isTrue();
		XNamedRange referenz = Lo.qi(XNamedRange.class, namedRanges.getByName(schluessel));
		assertThat(referenz.getContent())
				.as("getContent() rendert die kaputte Referenz via GRAM_API als #REF!")
				.contains("#REF!");

		// findeSheet erkennt die kaputte Referenz und liefert das Blatt nicht mehr.
		assertThat(SheetMetadataHelper.findeSheet(xDoc, schluessel))
				.as("eine kaputte Referenz darf kein Blatt mehr auflösen").isEmpty();

		// Cleanup entfernt den verwaisten Schlüssel ersatzlos.
		SheetMetadataHelper.bereinigeVerwaisteMetadaten(xDoc);
		assertThat(SheetMetadataHelper.getSchluesselMitPrefix(xDoc, "__PTM_"))
				.as("nach der Bereinigung ist der verwaiste Schlüssel entfernt")
				.doesNotContain(schluessel);
	}
}
