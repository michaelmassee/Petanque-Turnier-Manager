/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheetsync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.container.XNamed;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerSupplier;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;

/**
 * Nachziehen der Sync-Blätter ohne Tab-Wechsel ({@link SheetSyncNachzieher}): Ein geändertes Ergebnis muss die
 * Rangliste aktualisieren, ohne den Fokus zu wechseln und ohne Einträge in der Rückgängig-Liste.
 */
class SheetSyncNachzieherUITest extends BaseCalcUITest {

	/** Großzügiger Lesebereich der Rangliste (Nr, Name, Siege, Punkte …). */
	private static final RangePosition RANGLISTE_BEREICH = RangePosition.from(0, 0, 15, 40);

	private SheetSyncNachzieher nachzieher;
	private XSpreadsheetDocument xDoc;

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(42L);
		xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
		nachzieher = SheetSyncNachzieher.eigeneRegistry();
		nachzieher.registriere(SheetSyncListener.fuerSchluessel(wkingSpreadsheet.getxContext(),
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE, TurnierSystem.SCHWEIZER,
				new EingabeSignatur(SignaturQuellen::fuerSchweizer),
				(ws, ignored) -> new SchweizerRanglisteSheetUpdate(ws)));
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	void geaendertesErgebnis_aktualisiertRanglisteOhneFokuswechselUndOhneRueckgaengig() throws Exception {
		generiereSchweizerTurnier();
		XSpreadsheet runde1 = spielrunde1();
		XSpreadsheet rangliste = rangliste();
		// Ausgangslage: Hash passend zum generierten Stand (unabhängig davon, ob der Vollaufbau ihn schon schrieb).
		wieImSheetRunner(() -> nachzieher.zieheNach(wkingSpreadsheet, NachziehModus.NUR_VORHANDENE));
		assertThat(nachzieher.istVeraltet(xDoc)).as("Nach dem Nachziehen ist nichts veraltet").isFalse();

		sheetHlp.setActiveSheet(runde1);
		RangeData ranglisteVorher = leseRangliste(rangliste);
		int alterWert = leseErgebnisA(runde1);
		// Leeren: nach dem Generieren ist die Liste ggf. schon am Maximum und zählt nicht mehr hoch.
		undoManager().clear();
		schreibeErgebnisA(runde1, alterWert == 13 ? 7 : 13);
		int undoNachEingabe = anzahlUndoEintraege();
		assertThat(undoNachEingabe).as("Kontrolle: die Eingabe selbst landet in der Rückgängig-Liste")
				.isPositive();

		assertThat(nachzieher.istVeraltet(xDoc)).as("Geändertes Ergebnis → Rangliste veraltet").isTrue();
		boolean nachgezogen = wieImSheetRunnerMitErgebnis();

		assertThat(nachgezogen).as("Rangliste muss neu aufgebaut werden").isTrue();
		assertThat(leseRangliste(rangliste).toString()).as("Rangliste spiegelt das neue Ergebnis")
				.isNotEqualTo(ranglisteVorher.toString());
		assertThat(aktivesBlattName()).as("Kein Fokuswechsel auf die Rangliste").isEqualTo(name(runde1));
		assertThat(anzahlUndoEintraege()).as("Neuaufbau ist kein eigener Eintrag der Rückgängig-Liste")
				.isEqualTo(undoNachEingabe);
		assertThat(nachzieher.istVeraltet(xDoc)).as("Danach ist nichts mehr veraltet").isFalse();
		assertThat(wieImSheetRunnerMitErgebnis()).as("Zweiter Lauf ohne Änderung baut nichts neu").isFalse();

		// Strg+Z nimmt die Eingabe und den daraus folgenden Neuaufbau gemeinsam zurück.
		undoManager().undo();
		assertThat(leseErgebnisA(runde1)).as("Undo nimmt die Eingabe zurück").isEqualTo(alterWert);
		assertThat(leseRangliste(rangliste).toString()).as("Undo stellt auch die Rangliste konsistent wieder her")
				.isEqualTo(ranglisteVorher.toString());
	}

	@Test
	void fehlendeRangliste_wirdImHintergrundNichtAngelegt() throws Exception {
		generiereSchweizerTurnier();
		xDoc.getSheets().removeByName(name(rangliste()));
		assertThat(SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE))
				.as("Vorbedingung: keine Rangliste").isEmpty();

		assertThat(wieImSheetRunnerMitErgebnis()).isFalse();
		assertThat(SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE))
				.as("NUR_VORHANDENE legt keine Blätter an").isEmpty();
	}

	// -------------------------------------------------------------------------

	/** Die Testdaten setzen das Turniersystem nicht; der Nachzieher wählt die Einträge danach aus. */
	private void generiereSchweizerTurnier() throws GenerateException {
		new SchweizerTurnierTestDaten(wkingSpreadsheet).generate();
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SCHWEIZER.getId());
	}

	private boolean wieImSheetRunnerMitErgebnis() throws Exception {
		AtomicBoolean nachgezogen = new AtomicBoolean();
		wieImSheetRunner(() -> nachgezogen.set(nachzieher.zieheNach(wkingSpreadsheet, NachziehModus.NUR_VORHANDENE)));
		return nachgezogen.get();
	}

	private XSpreadsheet spielrunde1() {
		XSpreadsheet runde1 = SheetMetadataHelper.findeSheetUndHeile(xDoc,
				SheetMetadataHelper.schluesselSchweizerSpielrunde(1), null);
		assertThat(runde1).as("Spielrunde 1 muss existieren").isNotNull();
		return runde1;
	}

	private XSpreadsheet rangliste() {
		return SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE)
				.orElseThrow(() -> new AssertionError("Rangliste muss existieren"));
	}

	private RangeData leseRangliste(XSpreadsheet rangliste) throws GenerateException {
		return RangeHelper.from(rangliste, xDoc, RANGLISTE_BEREICH).getDataFromRange();
	}

	private int leseErgebnisA(XSpreadsheet runde) throws GenerateException {
		return RangeHelper.from(runde, xDoc, ergebnisAZelle()).getDataFromRange().get(0).get(0).getIntVal(0);
	}

	private void schreibeErgebnisA(XSpreadsheet runde, int wert) throws GenerateException {
		RangeHelper.from(runde, xDoc, ergebnisAZelle()).setDataInRange(new RangeData(1, wert));
	}

	private static RangePosition ergebnisAZelle() {
		return RangePosition.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE, SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
	}

	private XUndoManager undoManager() {
		return Lo.qi(XUndoManagerSupplier.class, xDoc).getUndoManager();
	}

	private int anzahlUndoEintraege() {
		return undoManager().getAllUndoActionTitles().length;
	}

	private String aktivesBlattName() {
		return name(wkingSpreadsheet.getWorkingSpreadsheetView().getActiveSheet());
	}

	private static String name(XSpreadsheet sheet) {
		return Lo.qi(XNamed.class, sheet).getName();
	}
}
