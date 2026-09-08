/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;

/**
 * Jede teamorientierte Meldeliste muss doppelte Vor-/Nachnamen durch eine
 * bedingte Fehlerformatierung rot markieren. Die Systeme verwenden denselben
 * {@link MeldeListeHelper}; daher darf die Regel nicht nur in einem Delegate
 * eingebunden sein.
 */
class MeldeListeSpielerDoppelteNamenFehlerfarbeUITest extends BaseCalcUITest {

	@Test
	void schweizerHatEineDublettenFehlerregel() throws Exception {
		assertDublettenFehlerregel("Schweizer", this::erstelleSchweizerMeldeliste);
	}

	@Test
	void jederGegenJedenHatEineDublettenFehlerregel() throws Exception {
		assertDublettenFehlerregel("Jeder gegen Jeden", this::erstelleJGJMeldeliste);
	}

	@Test
	void koHatEineDublettenFehlerregel() throws Exception {
		assertDublettenFehlerregel("KO", () -> {
			KoMeldeListeForTest meldeliste = new KoMeldeListeForTest();
			KoKonfigurationSheet konfiguration = meldeliste.konfiguration();
			konfiguration.update();
			konfiguration.setMeldeListeFormation(Formation.DOUBLETTE);
			konfiguration.setMeldeListeTeamnameAnzeigen(false);
			konfiguration.setMeldeListeVereinsnameAnzeigen(false);
			konfiguration.update();
			meldeliste.createMeldelisteWithParams();
			return meldeliste.getXSpreadSheet();
		});
	}

	@Test
	void kaskadeHatEineDublettenFehlerregel() throws Exception {
		assertDublettenFehlerregel("Kaskade", this::erstelleKaskadeMeldeliste);
	}

	@Test
	void formuleXHatEineDublettenFehlerregel() throws Exception {
		assertDublettenFehlerregel("Formule X", this::erstelleFormuleXMeldeliste);
	}

	@Test
	void pouleHatEineDublettenFehlerregel() throws Exception {
		assertDublettenFehlerregel("Poule", this::erstellePouleMeldeliste);
	}

	private XSpreadsheet erstelleSchweizerMeldeliste() throws Exception {
		SchweizerMeldeListeSheetNew meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		return meldeliste.getXSpreadSheet();
	}

	private XSpreadsheet erstelleJGJMeldeliste() throws Exception {
		JGJMeldeListeSheet_New meldeliste = new JGJMeldeListeSheet_New(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false, SpielplanTeamAnzeige.NR);
		return meldeliste.getXSpreadSheet();
	}

	private XSpreadsheet erstelleKaskadeMeldeliste() throws Exception {
		KaskadeMeldeListeSheetNew meldeliste = new KaskadeMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false, 4);
		return meldeliste.getXSpreadSheet();
	}

	private XSpreadsheet erstelleFormuleXMeldeliste() throws Exception {
		FormuleXMeldeListeSheetNew meldeliste = new FormuleXMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false, 4);
		return meldeliste.getXSpreadSheet();
	}

	private XSpreadsheet erstellePouleMeldeliste() throws Exception {
		PouleMeldeListeSheetNew meldeliste = new PouleMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		return meldeliste.getXSpreadSheet();
	}

	private void assertDublettenFehlerregel(String system, SheetErsteller ersteller) throws Exception {
		XSpreadsheet sheet = ersteller.erstelle();
		assertThat(conditionalFormatFormeln(sheet, Position.from(1, 3)))
				.as("%s: Vorname braucht eine bereinigte COUNTIFS-Fehlerregel für doppelte Namen", system)
				.anySatisfy(formel -> assertThat(formel).containsIgnoringCase("COUNTIFS")
						.containsIgnoringCase("CLEAN").doesNotContain("#REF!"));
	}

	private List<String> conditionalFormatFormeln(XSpreadsheet sheet, Position pos) throws GenerateException {
		try {
			XCell cell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			XPropertySet properties = Lo.qi(XPropertySet.class, cell);
			XSheetConditionalEntries entries = Lo.qi(XSheetConditionalEntries.class,
					properties.getPropertyValue("ConditionalFormat"));
			List<String> formeln = new ArrayList<>();
			for (int i = 0; i < entries.getCount(); i++) {
				formeln.add(Lo.qi(XSheetCondition.class, entries.getByIndex(i)).getFormula1());
			}
			return formeln;
		} catch (Exception e) {
			throw new GenerateException(e.getMessage());
		}
	}

	@FunctionalInterface
	private interface SheetErsteller {
		XSpreadsheet erstelle() throws Exception;
	}

	private final class KoMeldeListeForTest extends KoMeldeListeSheetNew {
		private KoMeldeListeForTest() throws GenerateException {
			super(wkingSpreadsheet);
		}

		private KoKonfigurationSheet konfiguration() throws GenerateException {
			return getKonfigurationSheet();
		}
	}
}
