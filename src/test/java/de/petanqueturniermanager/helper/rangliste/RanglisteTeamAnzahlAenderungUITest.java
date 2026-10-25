/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.container.XNamed;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.XMergeable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetTestDaten;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetUpdate;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXSpielrundeSheetNaechste;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheetTestDaten;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetTestDaten;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetUpdate;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheetUpdate;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetTestDaten;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetUpdate;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.poule.vorrunde.PouleVorrundeSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetTestDaten;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheetUpdate;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Ändert sich die Anzahl der Teams, darf das Ranglisten-Update keine alten Datenzeilen oder Fußzeilen stehen lassen
 * und keine verbundene Fußzeile mitten in den neuen Datenzeilen.
 * <p>
 * Ablauf je System: alle Teams → zwei Teams aus der Meldeliste entfernen → wieder einfügen. Nach jedem Schritt wird
 * der Spielplan bzw. die Vorrunde neu erstellt (nur bei Systemen ohne einzelne Spielrunden) und die Rangliste per
 * Update aktualisiert.
 */
class RanglisteTeamAnzahlAenderungUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 8;
	private static final int ENTFERNTE_TEAMS = 2;
	private static final int MELDELISTE_LETZTE_SPALTE = 30;
	private static final int MAX_PRUEF_ZEILEN = 30;

	@FunctionalInterface
	private interface Aktion {
		void ausfuehren() throws GenerateException;
	}

	@FunctionalInterface
	private interface SheetLieferant {
		XSpreadsheet liefern() throws GenerateException;
	}

	/**
	 * @param aufbau        erste Runde bzw. Spielplan erstellen (legt die Rangliste an)
	 * @param neuAufbau     nach geänderter Meldeliste: Spielplan bzw. Vorrunde neu erstellen
	 * @param update        Ranglisten-Update
	 * @param meldeliste    Meldelisten-Sheet
	 * @param ersteMeldung  erste Datenzeile der Meldeliste
	 * @param rangliste     Ranglisten-Sheet
	 * @param nrSpalte      Spalte mit einer Zahl in jeder Datenzeile der Rangliste
	 * @param ersteDatenZeile erste Datenzeile der Rangliste
	 */
	private record Szenario(Aktion aufbau, Aktion neuAufbau, Aktion update, SheetLieferant meldeliste,
			int ersteMeldung, SheetLieferant rangliste, int nrSpalte, int ersteDatenZeile) {
	}

	@Test
	void schweizer() throws Exception {
		new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).run();
		var rangliste = new SchweizerRanglisteSheet(wkingSpreadsheet);
		var meldeliste = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
		pruefeSzenario(new Szenario(() -> new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet).run(), () -> {
		}, () -> new SchweizerRanglisteSheetUpdate(wkingSpreadsheet).run(), meldeliste::getXSpreadSheet,
				meldeliste.getErsteDatenZiele(), rangliste::getXSpreadSheet, rangliste.getErsteSpalte(),
				rangliste.getErsteDatenZiele()));
	}

	@Test
	void formuleX() throws Exception {
		new FormuleXMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).erstelleMeldelisteWithTestdaten();
		var rangliste = new FormuleXRanglisteSheet(wkingSpreadsheet);
		var meldeliste = new FormuleXMeldeListeSheetUpdate(wkingSpreadsheet);
		pruefeSzenario(new Szenario(() -> new FormuleXSpielrundeSheetNaechste(wkingSpreadsheet).run(), () -> {
		}, () -> new FormuleXRanglisteSheetUpdate(wkingSpreadsheet).run(), meldeliste::getXSpreadSheet,
				meldeliste.getErsteDatenZiele(), rangliste::getXSpreadSheet, rangliste.getErsteSpalte(),
				rangliste.getErsteDatenZiele()));
	}

	@Test
	void jederGegenJeden() throws Exception {
		new JGJMeldeListeSheetTestDaten(wkingSpreadsheet, Formation.DOUBLETTE, ANZ_TEAMS).erstellenUndBefuellen();
		var rangliste = new JGJRanglisteSheet(wkingSpreadsheet);
		var meldeliste = new JGJMeldeListeSheet_Update(wkingSpreadsheet);
		Aktion spielplan = () -> new JGJSpielPlanSheet(wkingSpreadsheet).run();
		pruefeSzenario(new Szenario(spielplan, spielplanNeu(new JGJSpielPlanSheet(wkingSpreadsheet), spielplan),
				() -> new JGJRanglisteSheetUpdate(wkingSpreadsheet).run(), meldeliste::getXSpreadSheet,
				meldeliste.getErsteDatenZiele(), rangliste::getXSpreadSheet, rangliste.getErsteSpalte(),
				rangliste.getErsteDatenZiele()));
	}

	@Test
	void liga() throws Exception {
		new LigaMeldeListeSheetTestDaten(wkingSpreadsheet, false).generate();
		var rangliste = new LigaRanglisteSheet(wkingSpreadsheet);
		var meldeliste = new LigaMeldeListeSheetUpdate(wkingSpreadsheet);
		Aktion spielplan = () -> new LigaSpielPlanSheet(wkingSpreadsheet).run();
		pruefeSzenario(new Szenario(spielplan, spielplanNeu(new LigaSpielPlanSheet(wkingSpreadsheet), spielplan),
				() -> new LigaRanglisteSheetUpdate(wkingSpreadsheet).run(), meldeliste::getXSpreadSheet,
				meldeliste.getErsteDatenZiele(), rangliste::getXSpreadSheet, rangliste.getErsteSpalte(),
				rangliste.getErsteDatenZiele()));
	}

	@Test
	void tripTete() throws Exception {
		new TripTeteMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).generate();
		var meldeliste = new TripTeteMeldeListeSheetUpdate(wkingSpreadsheet);
		Aktion spielplan = () -> new TripTeteSpielPlanSheet(wkingSpreadsheet).run();
		pruefeSzenario(new Szenario(spielplan, spielplanNeu(new TripTeteSpielPlanSheet(wkingSpreadsheet), spielplan),
				() -> new TripTeteRanglisteSheetUpdate(wkingSpreadsheet).run(), meldeliste::getXSpreadSheet,
				meldeliste.getErsteDatenZiele(), new TripTeteRanglisteSheet(wkingSpreadsheet)::getXSpreadSheet,
				TripTeteRanglisteSheet.TEAM_NR_SPALTE, TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE));
	}

	@Test
	void poule() throws Exception {
		new PouleMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).run();
		var meldeliste = new PouleMeldeListeSheetUpdate(wkingSpreadsheet);
		Aktion vorrunde = () -> new PouleVorrundeSheet(wkingSpreadsheet).run();
		pruefeSzenario(new Szenario(vorrunde, vorrunde,
				() -> new PouleVorrundenRanglisteSheetUpdate(wkingSpreadsheet).run(), meldeliste::getXSpreadSheet,
				meldeliste.getErsteDatenZiele(), new PouleVorrundenRanglisteSheet(wkingSpreadsheet)::getXSpreadSheet,
				PouleVorrundenRanglisteSheet.SPALTE_PLATZ, PouleVorrundenRanglisteSheet.HEADER_ZEILEN));
	}

	/** Spielplan löschen und neu erstellen, damit keine Rückfrage zum Überschreiben kommt. */
	private Aktion spielplanNeu(ISheet spielplanSheet, Aktion spielplan) {
		return () -> {
			sheetHlp.removeSheet(Lo.qi(XNamed.class, spielplanSheet.getXSpreadSheet()).getName());
			spielplan.ausfuehren();
		};
	}

	private void pruefeSzenario(Szenario szenario) throws GenerateException, IndexOutOfBoundsException {
		szenario.aufbau().ausfuehren();
		int anzTeams = zaehleDatenZeilen(szenario);
		assertThat(anzTeams).as("Rangliste nach dem Aufbau").isGreaterThan(ENTFERNTE_TEAMS);
		pruefeTabelle(szenario, anzTeams);

		RangePosition letzteMeldungen = RangePosition.from(0, szenario.ersteMeldung() + anzTeams - ENTFERNTE_TEAMS,
				MELDELISTE_LETZTE_SPALTE, szenario.ersteMeldung() + anzTeams - 1);
		var meldungenBereich = RangeHelper.from(szenario.meldeliste().liefern(), doc, letzteMeldungen);
		RangeData entfernt = meldungenBereich.getDataFromRange();
		meldungenBereich.clearRange();
		neuAufbauUndUpdate(szenario);
		pruefeTabelle(szenario, anzTeams - ENTFERNTE_TEAMS);

		RangeHelper.from(szenario.meldeliste().liefern(), doc, letzteMeldungen).setDataInRange(entfernt);
		neuAufbauUndUpdate(szenario);
		pruefeTabelle(szenario, anzTeams);
	}

	private static void neuAufbauUndUpdate(Szenario szenario) throws GenerateException {
		szenario.neuAufbau().ausfuehren();
		szenario.update().ausfuehren();
	}

	private int zaehleDatenZeilen(Szenario szenario) throws GenerateException {
		RangeData nrSpalte = leseNrSpalte(szenario);
		int anz = 0;
		while (anz < nrSpalte.size() && istNummer(nrSpalte.get(anz).get(0).getStringVal())) {
			anz++;
		}
		return anz;
	}

	/**
	 * Erwartet genau {@code anzTeams} Datenzeilen mit Nummer, danach keine Nummer mehr und genau eine Fußzeile mit
	 * Text. Keine Datenzeile darf verbunden sein.
	 */
	private void pruefeTabelle(Szenario szenario, int anzTeams) throws GenerateException, IndexOutOfBoundsException {
		RangeData nrSpalte = leseNrSpalte(szenario);
		XSpreadsheet rangliste = szenario.rangliste().liefern();
		int anzTextZeilen = 0;
		for (int i = 0; i < nrSpalte.size(); i++) {
			String wert = nrSpalte.get(i).get(0).getStringVal();
			if (i < anzTeams) {
				assertThat(istNummer(wert)).as("Datenzeile %d hat eine Nummer: '%s'", i, wert).isTrue();
				int zeile = szenario.ersteDatenZeile() + i;
				var zeilenBereich = rangliste.getCellRangeByPosition(szenario.nrSpalte(), zeile,
						szenario.nrSpalte() + 1, zeile);
				assertThat(Lo.qi(XMergeable.class, zeilenBereich).getIsMerged())
						.as("Datenzeile %d darf nicht verbunden sein", i).isFalse();
			} else {
				assertThat(istNummer(wert)).as("Zeile %d nach den Daten ohne Nummer: '%s'", i, wert).isFalse();
				if (!wert.isBlank()) {
					anzTextZeilen++;
				}
			}
		}
		assertThat(anzTextZeilen).as("genau eine Fußzeile").isEqualTo(1);
	}

	private RangeData leseNrSpalte(Szenario szenario) throws GenerateException {
		return RangeHelper.from(szenario.rangliste().liefern(), doc, RangePosition.from(szenario.nrSpalte(),
				szenario.ersteDatenZeile(), szenario.nrSpalte(), szenario.ersteDatenZeile() + MAX_PRUEF_ZEILEN - 1))
				.getDataFromRange();
	}

	private static boolean istNummer(String wert) {
		return wert != null && wert.matches("\\d+(\\.0)?");
	}
}
