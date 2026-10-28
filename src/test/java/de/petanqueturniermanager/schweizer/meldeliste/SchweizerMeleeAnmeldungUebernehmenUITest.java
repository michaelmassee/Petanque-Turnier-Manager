/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.ValidationType;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungLeser;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/** Regression: derselbe Mêlée-Spieler darf nicht ein zweites Mal übernommen werden. */
class SchweizerMeleeAnmeldungUebernehmenUITest extends BaseCalcUITest implements MeleeAnmeldungKonstanten {

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
		RangeData data = RangeHelper.from(meldeliste.getXSpreadSheet(), doc,
				RangePosition.from(0, SchweizerListeDelegate.ERSTE_DATEN_ZEILE, meldeliste.getAktivSpalte(),
						SchweizerListeDelegate.ERSTE_DATEN_ZEILE + 30)).getDataFromRange();
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
