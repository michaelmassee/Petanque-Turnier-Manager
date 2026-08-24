package de.petanqueturniermanager.helper.msgbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.star.awt.Rectangle;
import com.sun.star.sheet.XSpreadsheetDocument;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;

/**
 * Reproduktionstest für den gemeldeten Bug: Dokument A wird geöffnet, die ProcessBox
 * (Protokollfenster) wird dort gezeigt. Dokument B wird zusätzlich geöffnet. Dokument A
 * wird geschlossen. Danach lässt sich die ProcessBox in Dokument B angeblich nicht mehr
 * öffnen – ohne Fehlermeldung.
 * <p>
 * Dieser Test dokumentiert zunächst nur den Reproduktionsschritt; siehe
 * {@code /home/michael/.claude/plans/bug-bei-mehrere-calc-warm-flamingo.md} für den
 * Diagnose-Plan. Solange die Root Cause nicht bestätigt ist, ist offen, ob der Test grün
 * oder rot läuft – das Log (Diagnose-Logging in {@link ProcessBox#runOnMain}, {@link
 * ProcessBox#visible()}, {@link ProcessBox#toFront()}, {@link ProcessBox#zeigeImVordergrund()})
 * liefert die eigentliche Evidenz.
 */
class ProcessBoxMehrDokumenteUITest extends BaseCalcUITest {

	private XSpreadsheetDocument zweitesDokument;

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		// Gradle erzwingt für uiTests 'uitest.headless=true' → BaseCalcUITest.beforeTest()
		// initialisiert die ProcessBox headless (kein echter Dialog, xWindow bleibt null).
		// Für diesen Test brauchen wir den echten UNO-Dialog, siehe Muster in ProcessBoxUITest.
		ProcessBox.setHeadlessMode(false);
		ProcessBox.forceinit(starter.getxComponentContext());
	}

	@AfterEach
	void schliesseZweitesDokument() {
		if (zweitesDokument != null) {
			OfficeDocumentHelper.closeDoc(zweitesDokument);
			zweitesDokument = null;
		}
		try {
			ProcessBox.from().hide();
		} catch (IllegalStateException ignored) {
			// bereits disposed
		}
	}

	@Test
	void prozessboxWeiterhinNutzbarInZweitemDokumentNachSchliessenDesErsten() {
		// Schritt 1: ProcessBox in Dokument A (doc, aus BaseCalcUITest) benutzen.
		ProcessBox.zeigeImVordergrund();
		ProcessBox.from().flushUiUpdatesForTest();
		assertThat(ProcessBox.from().istSichtbar())
				.as("ProcessBox muss in Dokument A sichtbar werden")
				.isTrue();
		assertFenstergroessePositiv("Dokument A");
		ProcessBox.from().hide();
		ProcessBox.from().flushUiUpdatesForTest();

		// Schritt 2: Dokument B zusätzlich öffnen.
		zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
		assertThat(zweitesDokument).as("zweites Dokument konnte nicht erstellt werden").isNotNull();

		// Schritt 3: Dokument A schließen.
		OfficeDocumentHelper.closeDoc(doc);
		doc = null; // verhindert doppeltes closeDoc() in BaseCalcUITest.afterTest()

		// Schritt 4: ProcessBox muss in Dokument B weiterhin funktionieren.
		ProcessBox.zeigeImVordergrund();
		ProcessBox.from().flushUiUpdatesForTest();
		assertThat(ProcessBox.from().istSichtbar())
				.as("ProcessBox muss nach Schließen von Dokument A weiterhin (für Dokument B) "
						+ "sichtbar gemacht werden können")
				.isTrue();
		assertFenstergroessePositiv("Dokument B nach Schließen von Dokument A");
	}

	private static void assertFenstergroessePositiv(String kontext) {
		Rectangle posSize = ProcessBox.from().getXWindow().getPosSize();
		assertThat(posSize.Width)
				.as("ProcessBox-Fensterbreite muss positiv sein: " + kontext)
				.isPositive();
		assertThat(posSize.Height)
				.as("ProcessBox-Fensterhöhe muss positiv sein: " + kontext)
				.isPositive();
	}
}
