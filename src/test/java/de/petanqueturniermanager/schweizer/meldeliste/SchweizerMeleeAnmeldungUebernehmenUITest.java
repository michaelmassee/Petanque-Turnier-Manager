/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.ValidationType;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungLeser;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/** Regressionen der Mêlée-Übernahme: keine doppelten Spieler, Neu-Auslosen ohne Datenverlust. */
class SchweizerMeleeAnmeldungUebernehmenUITest extends BaseCalcUITest implements MeleeAnmeldungKonstanten {

	private static final String VEREIN_PRAEFIX = "Verein ";

	private SchweizerMeldeListeSheetNew meldeliste;

	@Test
	void eingechecktSpalteHatNativeDatengueltigkeitUndNormalisiertAlteMarkierung() throws Exception {
		SchweizerMeleeAnmeldungSheet melee = new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet);
		melee.generate();
		RangeData alteAnmeldung = new RangeData();
		RowData alteZeile = alteAnmeldung.addNewRow();
		alteZeile.newEmpty();
		alteZeile.newString("Anna");
		alteZeile.newString("Altmarkierung");
		alteZeile.newEmpty();
		alteZeile.newString("ja");
		alteZeile.newEmpty();
		RangeHelper.from(melee.getXSpreadSheet(), doc,
				alteAnmeldung.getRangePosition(Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE))).setDataInRange(alteAnmeldung);

		melee.generate();

		assertThat(RangeHelper.from(melee.getXSpreadSheet(), doc,
				RangePosition.from(SPALTE_EINGECHECKT, ERSTE_DATEN_ZEILE, SPALTE_EINGECHECKT, ERSTE_DATEN_ZEILE))
				.getDataFromRange().get(0).get(0).getStringVal()).isEqualTo(MARKIERUNG);
		XPropertySet cellProperties = Lo.qi(XPropertySet.class,
				melee.getXSpreadSheet().getCellByPosition(SPALTE_EINGECHECKT, ERSTE_DATEN_ZEILE));
		XPropertySet validation = Lo.qi(XPropertySet.class, cellProperties.getPropertyValue("Validation"));

		assertThat(validation.getPropertyValue("Type")).isEqualTo(ValidationType.LIST);
		assertThat(validation.getPropertyValue("IgnoreBlankCells")).isEqualTo(Boolean.TRUE);
		assertThat(validation.getPropertyValue("ShowInputMessage")).isEqualTo(Boolean.TRUE);
		assertThat(validation.getPropertyValue("InputMessage"))
				.isEqualTo(I18n.get("melee.anmeldung.eingecheckt.eingabehilfe"));
		assertThat(validation.getPropertyValue("ShowErrorMessage")).isEqualTo(Boolean.TRUE);
		assertThat(validation.getPropertyValue("ErrorMessage")).isEqualTo(I18n.get("melee.anmeldung.eingecheckt.fehler"));
		assertThat(Lo.qi(XSheetCondition.class, validation).getFormula1()).isEqualTo("{\"X\"}");
	}

	/**
	 * Doppelt eingecheckte Namen (ohne Groß-/Kleinschreibung) brechen die Übernahme ab und werden rot markiert;
	 * nach der Korrektur läuft die Übernahme und die Markierung verschwindet. Eine Live-Regel gibt es im
	 * Mêlée-Blatt bewusst nicht (sie bremste jede Eingabe).
	 */
	@Test
	void doppelteNamenBrechenUebernahmeAbUndWerdenRotMarkiert() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		XSpreadsheet meleeSheet = meleeNamenAnlegen("sa", "wqe", "as", "qw", "SA", "Wqe", "x", "y");

		assertThat(conditionalFormatFormeln(meleeSheet, SPALTE_VORNAME, ERSTE_DATEN_ZEILE))
				.as("keine Live-Regel für doppelte Namen").noneSatisfy(formel -> assertThat(formel).contains("COUNTIF"));

		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(schweizerMeleeZeilen()).as("nichts übernommen").allMatch(MeleeAnmeldungZeile::istOffen);
		assertThat(schriftfarbe(meleeSheet, ERSTE_DATEN_ZEILE)).isEqualTo(ColorHelper.CHAR_COLOR_RED);
		assertThat(schriftfarbe(meleeSheet, ERSTE_DATEN_ZEILE + 1)).isEqualTo(ColorHelper.CHAR_COLOR_BLACK);
		assertThat(schriftfarbe(meleeSheet, ERSTE_DATEN_ZEILE + 2)).isEqualTo(ColorHelper.CHAR_COLOR_RED);

		meleeSheet.getCellByPosition(SPALTE_VORNAME, ERSTE_DATEN_ZEILE + 2).setFormula("neu");
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(schweizerMeleeZeilen()).as("nach der Korrektur alle übernommen").noneMatch(MeleeAnmeldungZeile::istOffen);
		assertThat(schriftfarbe(meleeSheet, ERSTE_DATEN_ZEILE)).as("Markierung zurückgesetzt")
				.isEqualTo(ColorHelper.CHAR_COLOR_BLACK);
	}

	/** Ein neuer Mêlée-Spieler, der schon von Hand in der Meldeliste steht, wird in beiden Blättern rot. */
	@Test
	void spielerSchonInMeldelisteBrichtUebernahmeAb() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		int meldelisteZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
		RangeData team = new RangeData();
		RowData zeile = team.addNewRow();
		zeile.newInt(1);
		zeile.newString("Anna");
		zeile.newString("Müller");
		zeile.newString("Bert");
		zeile.newString("Becker");
		RangeHelper.from(meldeliste.getXSpreadSheet(), doc, team.getRangePosition(Position.from(0, meldelisteZeile)))
				.setDataInRange(team);
		XSpreadsheet meleeSheet = meleeNamenAnlegen("anna", "müller", "c", "d");

		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(schweizerMeleeZeilen()).allMatch(MeleeAnmeldungZeile::istOffen);
		assertThat(schriftfarbe(meleeSheet, ERSTE_DATEN_ZEILE)).isEqualTo(ColorHelper.CHAR_COLOR_RED);
		assertThat(schriftfarbe(meldeliste.getXSpreadSheet(), meldelisteZeile)).isEqualTo(ColorHelper.CHAR_COLOR_RED);
	}

	/**
	 * Regression: Wird bei einem übernommenen Spieler der Check-in entfernt und derselbe Name neu eingecheckt,
	 * darf er nicht ein zweites Mal in die Meldeliste kommen.
	 */
	@Test
	void uebernommenerSpielerOhneCheckinWirdNichtErneutUebernommen() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		XSpreadsheet meleeSheet = meleeNamenAnlegen("Max", "Muster", "Eva", "Beispiel");
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		meleeSheet.getCellByPosition(SPALTE_EINGECHECKT, ERSTE_DATEN_ZEILE).setFormula("");
		RangeData nachtrag = new RangeData();
		RowData zeile = nachtrag.addNewRow();
		zeile.newEmpty();
		zeile.newString("max");
		zeile.newString("muster");
		zeile.newEmpty();
		zeile.newString(MARKIERUNG);
		zeile.newEmpty();
		RangeHelper.from(meleeSheet, doc, nachtrag.getRangePosition(Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE + 2)))
				.setDataInRange(nachtrag);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(schweizerMeleeZeilen()).filteredOn(MeleeAnmeldungZeile::istOffen)
				.extracting(MeleeAnmeldungZeile::zeile).as("Nachtrag bleibt offen")
				.containsExactly(ERSTE_DATEN_ZEILE + 2);
		assertThat(schriftfarbe(meleeSheet, ERSTE_DATEN_ZEILE + 2)).isEqualTo(ColorHelper.CHAR_COLOR_RED);
	}

	/** Legt eingecheckte Mêlée-Anmeldungen mit den Namen (Vorname, Nachname, …) an. */
	private XSpreadsheet meleeNamenAnlegen(String... namen) throws Exception {
		SchweizerMeleeAnmeldungSheet melee = new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet);
		melee.generate();
		RangeData data = new RangeData();
		for (int i = 0; i < namen.length; i += 2) {
			RowData zeile = data.addNewRow();
			zeile.newEmpty();
			zeile.newString(namen[i]);
			zeile.newString(namen[i + 1]);
			zeile.newEmpty();
			zeile.newString(MARKIERUNG);
			zeile.newEmpty();
		}
		XSpreadsheet meleeSheet = melee.getXSpreadSheet();
		RangeHelper.from(meleeSheet, doc, data.getRangePosition(Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE)))
				.setDataInRange(data);
		return meleeSheet;
	}

	private List<MeleeAnmeldungZeile> schweizerMeleeZeilen() throws Exception {
		return MeleeAnmeldungLeser.lesen(wkingSpreadsheet, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG);
	}

	private static int schriftfarbe(XSpreadsheet sheet, int zeile) throws Exception {
		return (int) Lo.qi(XPropertySet.class, sheet.getCellByPosition(SPALTE_VORNAME, zeile)).getPropertyValue("CharColor");
	}

	private static List<String> conditionalFormatFormeln(XSpreadsheet sheet, int spalte, int zeile) throws Exception {
		XPropertySet properties = Lo.qi(XPropertySet.class, sheet.getCellByPosition(spalte, zeile));
		XSheetConditionalEntries entries = Lo.qi(XSheetConditionalEntries.class,
				properties.getPropertyValue("ConditionalFormat"));
		List<String> formeln = new ArrayList<>();
		for (int i = 0; i < entries.getCount(); i++) {
			formeln.add(Lo.qi(XSheetCondition.class, entries.getByIndex(i)).getFormula1());
		}
		return formeln;
	}

	@Test
	void zweiteUebernahmeSchreibtKeineDoppeltenTeilnehmer() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.TRIPLETTE, false, false);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		meleeAnmeldungenAnlegen(6);

		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();
		List<String> nachErsterUebernahme = nachnamenInMeldeliste();

		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(nachnamenInMeldeliste())
				.as("eine erneute Übernahme darf keine Spieler ein zweites Mal in die Meldeliste schreiben")
				.containsExactlyInAnyOrderElementsOf(nachErsterUebernahme)
				.doesNotHaveDuplicates()
				.hasSize(6);
	}

	/**
	 * Regression: 7 eingecheckte Anmeldungen bei Formation Doublette müssen 3 volle Doubletten
	 * ergeben; die 7. Anmeldung muss offen (nicht übernommen) stehen bleiben, statt die gesamte
	 * Übernahme abzubrechen (Bug: der Team-Bildner erzwang für genau 7 Spieler unabhängig von der
	 * Formation ein 3er-Team, was bei Doublette nicht in die Meldeliste passte).
	 * <p>
	 * Zusätzlich muss zwingend die <b>letzte</b> Anmeldung (Nr. 7) offen bleiben, nicht irgendeine
	 * zufällig ausgewählte (Bug-Report: Nr. 5 blieb offen, obwohl Nr. 7 die letzte in der Liste war).
	 */
	@Test
	void siebenSpielerBeiDoubletteBildenDreiTeamsUndLassenLetztenOffen() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		meleeAnmeldungenAnlegen(7);

		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(nachnamenInMeldeliste())
				.as("bei Doublette muessen aus 7 Anmeldungen genau 3 volle Teams (6 Spieler) entstehen")
				.hasSize(6)
				.doesNotContain("MeleeTestNachname6");

		List<MeleeAnmeldungZeile> zeilen = MeleeAnmeldungLeser.lesen(wkingSpreadsheet,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG);
		assertThat(zeilen).filteredOn(MeleeAnmeldungZeile::istOffen)
				.as("nur die letzte Anmeldung (Nr. 7) darf offen bleiben")
				.extracting(MeleeAnmeldungZeile::nachname)
				.containsExactly("MeleeTestNachname6");
	}

	/**
	 * Regression: Zeilen, die ohne Nr eingefügt wurden (z.B. per Copy-Paste, ohne den Menüpunkt
	 * „Mêlée Anmeldung" zwischendurch erneut auszuführen), müssen spätestens beim „Übernehmen"
	 * lückenlos durchnummeriert werden – die Nr-Spalte darf keine leeren/0-Werte zeigen.
	 */
	@Test
	void uebernehmenNummeriertZeilenOhneNrNach() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);
		meleeAnmeldungenAnlegen(4, false);

		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		List<MeleeAnmeldungZeile> zeilen = MeleeAnmeldungLeser.lesen(wkingSpreadsheet,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG);
		assertThat(zeilen).extracting(MeleeAnmeldungZeile::nr)
				.as("nach dem Übernehmen muss die Nr-Spalte lückenlos 1..n sein, auch wenn sie vorher leer war")
				.containsExactly(1, 2, 3, 4);
	}

	/**
	 * „Doublette ohne Freilos": 10 Spieler ergeben wie im Supermêlée 2 Triplettes und 2 Doubletten
	 * (gerade Teamzahl, kein Freilos) in der Triplette-Meldeliste – bei Doubletten bleibt Spieler 3 leer.
	 */
	@Test
	void doubletteOhneFreilosBildetGemischteTeamsInTripletteMeldeliste() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.TRIPLETTE, false, false);
		MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet, MeleeAnmeldungModus.DOUBLETTE_OHNE_FREILOS);
		meleeAnmeldungenAnlegen(10);

		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(anzahlSpielerJeTeam()).as("2 Doubletten + 2 Triplettes, Doubletten zuerst nummeriert")
				.containsExactly(2, 2, 3, 3);
		assertThat(nachnamenInMeldeliste()).hasSize(10).doesNotHaveDuplicates();
	}

	/** 7 Spieler lassen sich nicht ohne Freilos aufteilen: 6 Spieler in 2 Teams, Nr. 7 bleibt offen. */
	@Test
	void tripletteOhneFreilosLaesstBeiSiebenSpielernLetztenOffen() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.TRIPLETTE, false, false);
		MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet, MeleeAnmeldungModus.TRIPLETTE_OHNE_FREILOS);
		meleeAnmeldungenAnlegen(7);

		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(anzahlSpielerJeTeam().size() % 2).as("gerade Teamzahl").isZero();
		assertThat(nachnamenInMeldeliste()).hasSize(6).doesNotContain("MeleeTestNachname6");
	}

	/** Gemischte Teams passen nicht in eine Doublette-Meldeliste: Übernahme bricht ohne Änderung ab. */
	@Test
	void ohneFreilosBeiDoubletteMeldelisteUebernimmtNichts() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
		MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet, MeleeAnmeldungModus.DOUBLETTE_OHNE_FREILOS);
		meleeAnmeldungenAnlegen(10);

		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(nachnamenInMeldeliste()).isEmpty();
		assertThat(MeleeAnmeldungLeser.lesen(wkingSpreadsheet, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG))
				.allMatch(MeleeAnmeldungZeile::istOffen);
	}

	/**
	 * „Doublette ohne Freilos": Ein einzelner Nachzügler passt in kein eigenes Team. Vor der ersten
	 * Spielrunde werden deshalb alle Mêlée-Teams zusammen mit ihm neu ausgelost – 17 Spieler ergeben
	 * 7 Doubletten und 1 Triplette (gerade Teamzahl), niemand wird doppelt übernommen.
	 */
	@Test
	void einzelnerNachzueglerWirdVorErsterSpielrundeMitAllenNeuAusgelost() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.TRIPLETTE, false, false);
		MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet, MeleeAnmeldungModus.DOUBLETTE_OHNE_FREILOS);
		meleeAnmeldungenAnlegen(16);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();
		assertThat(anzahlSpielerJeTeam()).as("16 Spieler = 8 Doubletten").hasSize(8).containsOnly(2);

		meleeAnmeldungenAnlegen(16, 1, true);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(anzahlSpielerJeTeam()).as("17 Spieler = 7 Doubletten + 1 Triplette, nach Nr sortiert "
				+ "Doubletten vor Triplettes wie im Supermêlée").containsExactly(2, 2, 2, 2, 2, 2, 2, 3);
		assertThat(nachnamenInMeldeliste()).hasSize(17).doesNotHaveDuplicates().contains("MeleeTestNachname16");
		assertThat(MeleeAnmeldungLeser.lesen(wkingSpreadsheet, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG))
				.as("alle 17 Anmeldungen sind übernommen").noneMatch(MeleeAnmeldungZeile::istOffen);
	}

	/**
	 * Regression: In der Meldeliste nachgetragene Vereine bleiben beim Neu-Auslosen beim jeweiligen Spieler; der
	 * Nachzügler hat noch keinen Verein.
	 */
	@Test
	void neuAuslosenBehaeltVereineDerBisherigenSpieler() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.TRIPLETTE, false, true);
		MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet, MeleeAnmeldungModus.DOUBLETTE_OHNE_FREILOS);
		meleeAnmeldungenAnlegen(16);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();
		vereineNachtragen();

		meleeAnmeldungenAnlegen(16, 1, true);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		Map<String, String> vereine = vereinJeNachname();
		assertThat(vereine).as("alle 17 Spieler neu ausgelost").hasSize(17);
		vereine.forEach((nachname, verein) -> assertThat(verein).as(nachname)
				.isEqualTo("MeleeTestNachname16".equals(nachname) ? "" : VEREIN_PRAEFIX + nachname));
	}

	/**
	 * Regression: Ein übernommener Spieler, dessen Check-in vor der ersten Spielrunde entfernt wurde, fällt beim
	 * Neu-Auslosen heraus und seine Anmeldung ist danach wieder offen (Bug: er wurde mit ausgelost und blieb als
	 * „Übernommen" markiert).
	 */
	@Test
	void neuAuslosenLaesstAusgechecktenSpielerWeg() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.TRIPLETTE, false, false);
		MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet, MeleeAnmeldungModus.DOUBLETTE_OHNE_FREILOS);
		meleeAnmeldungenAnlegen(16);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();
		XSpreadsheet meleeSheet = new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet).getXSpreadSheet();
		meleeSheet.getCellByPosition(SPALTE_EINGECHECKT, ERSTE_DATEN_ZEILE).setFormula("");

		meleeAnmeldungenAnlegen(16, 1, true);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(nachnamenInMeldeliste()).as("15 bisherige + Nachzügler, ohne den ausgecheckten Spieler")
				.hasSize(16).doesNotHaveDuplicates().doesNotContain("MeleeTestNachname0")
				.contains("MeleeTestNachname16");
		assertThat(schweizerMeleeZeilen()).filteredOn(MeleeAnmeldungZeile::istOffen)
				.extracting(MeleeAnmeldungZeile::nachname).as("Übernommen-Markierung zurückgesetzt")
				.containsExactly("MeleeTestNachname0");
	}

	/** Ein von Hand inaktiv gesetztes Mêlée-Team bleibt beim Neu-Auslosen unverändert stehen. */
	@Test
	void neuAuslosenLaesstInaktivesTeamUnveraendert() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.TRIPLETTE, false, false);
		MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet, MeleeAnmeldungModus.DOUBLETTE_OHNE_FREILOS);
		meleeAnmeldungenAnlegen(16);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();
		List<String> inaktivesTeam = nachnamenJeMeldelisteZeile().get(0);
		meldeliste.getXSpreadSheet().getCellByPosition(meldeliste.getAktivSpalte(), SchweizerListeDelegate.ERSTE_DATEN_ZEILE)
				.setValue(SchweizerListeDelegate.AKTIV_WERT_AUSGESTIEGEN);

		meleeAnmeldungenAnlegen(16, 1, true);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(nachnamenInMeldeliste()).hasSize(17).doesNotHaveDuplicates().contains("MeleeTestNachname16");
		assertThat(aktivJeTeam()).as("inaktives Team unverändert und weiterhin ausgestiegen")
				.containsEntry(inaktivesTeam, SchweizerListeDelegate.AKTIV_WERT_AUSGESTIEGEN);
	}

	/** Trägt je Spieler den Verein „Verein &lt;Nachname&gt;" ein – spaltenweise am Stück. */
	private void vereineNachtragen() throws Exception {
		int ersteZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
		RangeData data = meldelisteDaten();
		for (int vereinSpalte : vereinSpalten()) {
			RangeData verein = new RangeData();
			for (RowData zeile : data) {
				String nachname = zeile.get(vereinSpalte - 1).getStringVal();
				verein.addNewRow().newString(StringUtils.isNotBlank(nachname) ? VEREIN_PRAEFIX + nachname : "");
			}
			RangeHelper.from(meldeliste.getXSpreadSheet(), doc,
					verein.getRangePosition(Position.from(vereinSpalte, ersteZeile))).setDataInRange(verein);
		}
	}

	private Map<String, String> vereinJeNachname() throws Exception {
		Map<String, String> vereine = new HashMap<>();
		for (RowData zeile : meldelisteDaten()) {
			for (int vereinSpalte : vereinSpalten()) {
				String nachname = zeile.get(vereinSpalte - 1).getStringVal();
				if (StringUtils.isNotBlank(nachname)) {
					vereine.put(nachname, Objects.toString(zeile.get(vereinSpalte).getStringVal(), ""));
				}
			}
		}
		return vereine;
	}

	/** Vereinsspalten der Triplette-Meldeliste ohne Teamname: Nr | (Vorname | Nachname | Verein) x 3 | SP | Aktiv. */
	private static List<Integer> vereinSpalten() {
		return List.of(3, 6, 9);
	}

	private Map<List<String>, Integer> aktivJeTeam() throws Exception {
		List<List<String>> nachnamen = nachnamenJeMeldelisteZeile();
		RangeData data = meldelisteDaten();
		Map<List<String>, Integer> aktiv = new HashMap<>();
		for (int i = 0; i < nachnamen.size(); i++) {
			if (!nachnamen.get(i).isEmpty()) {
				aktiv.put(nachnamen.get(i), data.get(i).get(meldeliste.getAktivSpalte()).getIntVal(0));
			}
		}
		return aktiv;
	}

	private RangeData meldelisteDaten() throws Exception {
		return RangeHelper.from(meldeliste.getXSpreadSheet(), doc,
				RangePosition.from(0, SchweizerListeDelegate.ERSTE_DATEN_ZEILE, meldeliste.getAktivSpalte(),
						SchweizerListeDelegate.ERSTE_DATEN_ZEILE + 30)).getDataFromRange();
	}

	/** Nach Beginn der ersten Spielrunde bleiben die Teams unverändert; der Nachzügler bleibt offen. */
	@Test
	void nachzueglerBleibtNachErsterSpielrundeOffen() throws Exception {
		meldeliste = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeliste.createMeldelisteWithParams(Formation.TRIPLETTE, false, false);
		MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet, MeleeAnmeldungModus.DOUBLETTE_OHNE_FREILOS);
		meleeAnmeldungenAnlegen(16);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();
		List<List<String>> teamsVorher = nachnamenJeMeldelisteZeile();
		spielrundeRegistrieren();

		meleeAnmeldungenAnlegen(16, 1, true);
		new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();

		assertThat(nachnamenJeMeldelisteZeile()).as("bestehende Teams unverändert").isEqualTo(teamsVorher);
		assertThat(MeleeAnmeldungLeser.lesen(wkingSpreadsheet, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG))
				.filteredOn(MeleeAnmeldungZeile::istOffen).extracting(MeleeAnmeldungZeile::nachname)
				.containsExactly("MeleeTestNachname16");
	}

	/** Ein Blatt mit dem Metadaten-Schlüssel der 1. Spielrunde kennzeichnet das Turnier als begonnen. */
	private void spielrundeRegistrieren() throws Exception {
		String name = "Test Spielrunde";
		doc.getSheets().insertNewByName(name, (short) doc.getSheets().getElementNames().length);
		XSpreadsheet spielrunde = Lo.qi(XSpreadsheet.class, doc.getSheets().getByName(name));
		SheetMetadataHelper.schreibeSheetMetadaten(doc, spielrunde, SheetMetadataHelper.schluesselSchweizerSpielrunde(1));
	}

	private void meleeAnmeldungenAnlegen(int anzahl) throws Exception {
		meleeAnmeldungenAnlegen(anzahl, true);
	}

	private void meleeAnmeldungenAnlegen(int anzahl, boolean mitNr) throws Exception {
		meleeAnmeldungenAnlegen(0, anzahl, mitNr);
	}

	/**
	 * @param ab Index der ersten neuen Anmeldung (ab 0) – Zeile, Nr und Name ergeben sich daraus
	 */
	private void meleeAnmeldungenAnlegen(int ab, int anzahl, boolean mitNr) throws Exception {
		SchweizerMeleeAnmeldungSheet melee = new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet);
		melee.generate();
		RangeData data = new RangeData();
		for (int i = ab; i < ab + anzahl; i++) {
			RowData zeile = data.addNewRow();
			if (mitNr) {
				zeile.newInt(i + 1);
			} else {
				zeile.newEmpty();
			}
			zeile.newString("Vorname " + i);
			zeile.newString("MeleeTestNachname" + i);
			zeile.newEmpty();
			zeile.newString(MARKIERUNG);
			zeile.newEmpty();
		}
		RangeHelper.from(melee.getXSpreadSheet(), doc,
				data.getRangePosition(Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE + ab))).setDataInRange(data);
	}

	private List<String> nachnamenInMeldeliste() throws Exception {
		return nachnamenJeMeldelisteZeile().stream().flatMap(List::stream).toList();
	}

	private List<Integer> anzahlSpielerJeTeam() throws Exception {
		return nachnamenJeMeldelisteZeile().stream().map(List::size).filter(anzahl -> anzahl > 0).toList();
	}

	private List<List<String>> nachnamenJeMeldelisteZeile() throws Exception {
		RangeData data = meldelisteDaten();
		List<List<String>> nachnamenJeZeile = new ArrayList<>();
		for (RowData zeile : data) {
			List<String> nachnamen = new ArrayList<>();
			for (int spalte = 0; spalte <= meldeliste.getAktivSpalte(); spalte++) {
				String wert = zeile.get(spalte).getStringVal();
				if (wert != null && wert.startsWith("MeleeTestNachname")) {
					nachnamen.add(wert);
				}
			}
			nachnamenJeZeile.add(nachnamen);
		}
		return nachnamenJeZeile;
	}
}
