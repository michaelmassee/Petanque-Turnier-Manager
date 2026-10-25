/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.container.XNamed;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetView;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetTestDaten;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXSpielrundeSheetNaechste;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheetTestDaten;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetTestDaten;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetTestDaten;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetTestDaten;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;
import de.petanqueturniermanager.poule.vorrunde.PouleVorrundeSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetTestDaten;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Beim Erstellen der ersten Spielrunde bzw. des Spielplans wird eine fehlende Rangliste angelegt. Das aktive Blatt
 * bleibt die neue Spielrunde bzw. der Spielplan.
 */
class RanglisteNachErsterRundeUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 8;

	@Test
	void schweizerLegtRanglisteNachErsterRundeAn() throws GenerateException {
		new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).run();
		var spielrunde = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
		spielrunde.run();

		pruefeRanglisteAngelegtUndBlattAktiv(spielrunde, new SchweizerRanglisteSheet(wkingSpreadsheet));
	}

	@Test
	void maastrichterLegtVorrundenRanglisteNachErsterRundeAn() throws GenerateException {
		new MaastrichterMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleTestdaten();
		var spielrunde = new MaastrichterSpielrundeSheetNaechste(wkingSpreadsheet);
		spielrunde.run();

		pruefeRanglisteAngelegtUndBlattAktiv(spielrunde, new MaastrichterVorrundenRanglisteSheet(wkingSpreadsheet));
	}

	@Test
	void formuleXLegtRanglisteNachErsterRundeAn() throws GenerateException {
		new FormuleXMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleMeldelisteWithTestdaten();
		var spielrunde = new FormuleXSpielrundeSheetNaechste(wkingSpreadsheet);
		spielrunde.run();

		pruefeRanglisteAngelegtUndBlattAktiv(spielrunde, new FormuleXRanglisteSheet(wkingSpreadsheet));
	}

	@Test
	void jgjLegtRanglisteMitSpielplanAn() throws GenerateException {
		new JGJMeldeListeSheetTestDaten(wkingSpreadsheet, Formation.DOUBLETTE, ANZ_TEAMS).erstellenUndBefuellen();
		var spielplan = new JGJSpielPlanSheet(wkingSpreadsheet);
		spielplan.run();

		pruefeRanglisteAngelegtUndBlattAktiv(spielplan, new JGJRanglisteSheet(wkingSpreadsheet));
	}

	@Test
	void ligaLegtRanglisteMitSpielplanAn() throws GenerateException {
		new LigaMeldeListeSheetTestDaten(wkingSpreadsheet, true).generate();
		var spielplan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielplan.run();

		pruefeRanglisteAngelegtUndBlattAktiv(spielplan, new LigaRanglisteSheet(wkingSpreadsheet));
	}

	@Test
	void tripTeteLegtRanglisteMitSpielplanAn() throws GenerateException {
		new TripTeteMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).generate();
		var spielplan = new TripTeteSpielPlanSheet(wkingSpreadsheet);
		spielplan.run();

		pruefeRanglisteAngelegtUndBlattAktiv(spielplan, new TripTeteRanglisteSheet(wkingSpreadsheet));
	}

	@Test
	void pouleLegtVorrundenRanglisteMitVorrundeAn() throws GenerateException {
		new PouleMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).run();
		var vorrunde = new PouleVorrundeSheet(wkingSpreadsheet);
		vorrunde.run();

		pruefeRanglisteAngelegtUndBlattAktiv(vorrunde, new PouleVorrundenRanglisteSheet(wkingSpreadsheet));
	}

	private void pruefeRanglisteAngelegtUndBlattAktiv(ISheet spielrunde, ISheet rangliste) throws GenerateException {
		XSpreadsheet spielrundeSheet = spielrunde.getXSpreadSheet();
		assertThat(spielrundeSheet).as("Spielrunde bzw. Spielplan muss existieren").isNotNull();
		assertThat(rangliste.getXSpreadSheet()).as("Rangliste muss angelegt sein").isNotNull();
		assertThat(aktiverBlattName()).as("Spielrunde bzw. Spielplan bleibt aktiv").isEqualTo(blattName(spielrundeSheet));
	}

	private String aktiverBlattName() {
		XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, Lo.qi(XModel.class, doc).getCurrentController());
		return blattName(view.getActiveSheet());
	}

	private static String blattName(XSpreadsheet sheet) {
		return Lo.qi(XNamed.class, sheet).getName();
	}
}
