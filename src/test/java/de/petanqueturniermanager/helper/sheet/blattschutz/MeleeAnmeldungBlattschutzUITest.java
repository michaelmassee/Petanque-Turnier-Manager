/*
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet.blattschutz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungKonstanten;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.schweizer.blattschutz.SchweizerBlattschutzKonfiguration;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeleeAnmeldungSheet;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Regression: das Mêlée-Anmeldung-Sheet war in keiner {@code *BlattschutzKonfiguration}
 * verzeichnet und blieb dadurch im Kiosk-Modus komplett ungeschützt (Bug-Report:
 * {@code table:protected} fehlte im ODS für das Blatt „Mêlée Anmeldung", während
 * die Meldeliste korrekt geschützt war).
 * <p>
 * Fix: {@link BlattschutzManager#mitGlobalenSchutzInfos} sperrt das Sheet jetzt
 * systemübergreifend, mit Vorname/Nachname/SP/Eingecheckt als editierbaren Spalten;
 * Nr (vom System nummeriert) und Übernommen (vom Übernehmen-Kommando gesetzt)
 * bleiben gesperrt.
 */
class MeleeAnmeldungBlattschutzUITest extends BaseCalcUITest {

	@Test
	void meleeAnmeldungSheet_wirdImKioskModusGeschuetzt() throws Exception {
		var meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet).generate();

		TurnierModus.get().setAktivForTest(true);
		try {
			BlattschutzManager.get().schuetzen(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);

			XSpreadsheet meleeSheet = SheetMetadataHelper
					.findeSheet(doc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG)
					.orElseThrow(() -> new AssertionError("Mêlée-Anmeldung-Sheet nicht gefunden"));

			assertThat(Lo.qi(XProtectable.class, meleeSheet).isProtected())
					.as("Mêlée-Anmeldung-Sheet muss im Kiosk-Modus geschützt sein")
					.isTrue();

			assertThat(istGesperrt(meleeSheet, MeleeAnmeldungKonstanten.SPALTE_NR,
					MeleeAnmeldungKonstanten.ERSTE_DATEN_ZEILE))
					.as("Nr bleibt gesperrt (wird vom System durchnummeriert)").isTrue();
			assertThat(istGesperrt(meleeSheet, MeleeAnmeldungKonstanten.SPALTE_VORNAME,
					MeleeAnmeldungKonstanten.ERSTE_DATEN_ZEILE))
					.as("Vorname ist editierbar").isFalse();
			assertThat(istGesperrt(meleeSheet, MeleeAnmeldungKonstanten.SPALTE_NACHNAME,
					MeleeAnmeldungKonstanten.ERSTE_DATEN_ZEILE))
					.as("Nachname ist editierbar").isFalse();
			assertThat(istGesperrt(meleeSheet, MeleeAnmeldungKonstanten.SPALTE_SETZPOSITION,
					MeleeAnmeldungKonstanten.ERSTE_DATEN_ZEILE))
					.as("SP ist editierbar").isFalse();
			assertThat(istGesperrt(meleeSheet, MeleeAnmeldungKonstanten.SPALTE_EINGECHECKT,
					MeleeAnmeldungKonstanten.ERSTE_DATEN_ZEILE))
					.as("Eingecheckt ist editierbar").isFalse();
			assertThat(istGesperrt(meleeSheet, MeleeAnmeldungKonstanten.SPALTE_UEBERNOMMEN,
					MeleeAnmeldungKonstanten.ERSTE_DATEN_ZEILE))
					.as("Übernommen bleibt gesperrt (wird vom Übernehmen-Kommando gesetzt)").isTrue();
		} finally {
			BlattschutzManager.get().entsperren(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
			TurnierModus.get().setAktivForTest(false);
		}
	}

	private boolean istGesperrt(XSpreadsheet sheet, int spalte, int zeile) throws Exception {
		XPropertySet props = Lo.qi(XPropertySet.class, sheet.getCellByPosition(spalte, zeile));
		return ((CellProtection) props.getPropertyValue("CellProtection")).IsLocked;
	}
}
