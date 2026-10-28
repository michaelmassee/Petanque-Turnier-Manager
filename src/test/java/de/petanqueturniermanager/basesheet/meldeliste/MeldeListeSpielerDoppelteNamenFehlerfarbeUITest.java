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
import de.petanqueturniermanager.helper.sheet.FehlerregelAuswertung;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetNew;

/**
 * Jede Meldeliste muss doppelte Vor-/Nachnamen durch eine bedingte Fehlerformatierung rot markieren. Die Regel
 * wird über {@link FehlerregelAuswertung} wie in LibreOffice tatsächlich ausgewertet – ein Textvergleich der
 * Formel genügte nicht (die Regel lieferte jahrelang Err:504 bzw. las die falsche Zeile).
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
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false, TeamAnzeige.NR);
		return meldeliste.getXSpreadSheet();
	}

	private XSpreadsheet erstelleKaskadeMeldeliste() throws Exception {
		KaskadeMeldeListeSheetNew meldeliste = new KaskadeMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false, 4);
		return meldeliste.getXSpreadSheet();
	}

	private XSpreadsheet erstelleFormuleXMeldeliste() throws Exception {
		FormuleXMeldeListeSheetNew meldeliste = new FormuleXMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		return meldeliste.getXSpreadSheet();
	}

	private XSpreadsheet erstellePouleMeldeliste() throws Exception {
		PouleMeldeListeSheetNew meldeliste = new PouleMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		return meldeliste.getXSpreadSheet();
	}

	@Test
	void tripTeteHatEineDublettenFehlerregel() throws Exception {
		assertDublettenFehlerregel("Trip-Tête", () -> {
			TripTeteMeldeListeSheetNew meldeliste = new TripTeteMeldeListeSheetNew(wkingSpreadsheet);
			meldeliste.createMeldeliste();
			return meldeliste.getXSpreadSheet();
		});
	}

	/** Supermêlée: ein Spieler je Zeile (Vorname, Nachname), eigene Regel. */
	@Test
	void supermeleeMarkiertDoppelteSpieler() throws Exception {
		MeldeListeSheet_New meldeliste = new MeldeListeSheet_New(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(SuperMeleeMode.Triplette);
		XSpreadsheet sheet = meldeliste.getXSpreadSheet();
		int ersteZeile = meldeliste.getMeldungenSpalte().getErsteDatenZiele();
		int vornameSpalte = meldeliste.getSpielerNameErsteSpalte();
		schreibeZeilen(sheet, vornameSpalte, ersteZeile, List.of("Anna", "Müller"), List.of("Klaus", "Weber"),
				List.of("anna", "MÜLLER"));

		for (int spalte : List.of(vornameSpalte, vornameSpalte + 1)) {
			assertThat(FehlerregelAuswertung.greift(sheet, spalte, ersteZeile)).as("Anna Müller").isTrue();
			assertThat(FehlerregelAuswertung.greift(sheet, spalte, ersteZeile + 1)).as("Klaus Weber").isFalse();
			assertThat(FehlerregelAuswertung.greift(sheet, spalte, ersteZeile + 2)).as("anna MÜLLER").isTrue();
		}
	}

	/** Liga: ein Mannschaftsname je Zeile. */
	@Test
	void ligaMarkiertDoppelteMannschaften() throws Exception {
		LigaMeldeListeSheetNew meldeliste = new LigaMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams("TestGruppe");
		XSpreadsheet sheet = meldeliste.getXSpreadSheet();
		int ersteZeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;
		schreibeZeilen(sheet, 1, ersteZeile, List.of("Boule Club"), List.of("Kugelfreunde"), List.of("Boule Club"));

		assertThat(FehlerregelAuswertung.greift(sheet, 1, ersteZeile)).as("Boule Club").isTrue();
		assertThat(FehlerregelAuswertung.greift(sheet, 1, ersteZeile + 1)).as("Kugelfreunde").isFalse();
		assertThat(FehlerregelAuswertung.greift(sheet, 1, ersteZeile + 2)).as("Boule Club, 2. Zeile").isTrue();
	}

	/**
	 * Teamlisten (Doublette, ohne Teamname/Verein): Nr | Vorname 1 | Nachname 1 | Vorname 2 | Nachname 2. „Anna
	 * Müller“ steht in Team 1 als Spieler 1 und in Team 2 – anders geschrieben – als Spieler 2.
	 */
	private void assertDublettenFehlerregel(String system, SheetErsteller ersteller) throws Exception {
		XSpreadsheet sheet = ersteller.erstelle();
		int zeile = MeldeListeKonstanten.TEAM_MELDELISTE_ERSTE_DATEN_ZEILE;
		assertThat(conditionalFormatFormeln(sheet, Position.from(1, zeile)))
				.as("%s: Vorname braucht eine Fehlerregel für doppelte Namen", system)
				.anySatisfy(formel -> assertThat(formel).containsIgnoringCase("COUNTIFS").doesNotContain("#REF!"));
		schreibeZeilen(sheet, 1, zeile, List.of("Anna", "Müller", "Peter", "Schmidt"),
				List.of("Klaus", "Weber", "anna", "MÜLLER"));

		assertThat(FehlerregelAuswertung.greift(sheet, 1, zeile)).as("%s: Anna Müller, Team 1", system).isTrue();
		assertThat(FehlerregelAuswertung.greift(sheet, 3, zeile + 1)).as("%s: Anna Müller, Team 2", system).isTrue();
		assertThat(FehlerregelAuswertung.greift(sheet, 1, zeile + 1)).as("%s: Klaus Weber", system).isFalse();
		assertThat(FehlerregelAuswertung.greift(sheet, 3, zeile)).as("%s: Peter Schmidt", system).isFalse();
	}

	@SafeVarargs
	private void schreibeZeilen(XSpreadsheet sheet, int ersteSpalte, int ersteZeile, List<String>... zeilen)
			throws GenerateException {
		RangeData data = new RangeData();
		for (List<String> werte : zeilen) {
			RowData zeile = data.addNewRow();
			werte.forEach(zeile::newString);
		}
		RangeHelper.from(sheet, doc, data.getRangePosition(Position.from(ersteSpalte, ersteZeile))).setDataInRange(data);
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
