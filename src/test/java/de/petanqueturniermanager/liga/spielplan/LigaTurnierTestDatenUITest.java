package de.petanqueturniermanager.liga.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.util.XMergeable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.blattschutz.SheetSchutzInfo;
import de.petanqueturniermanager.liga.blattschutz.LigaBlattschutzKonfiguration;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SignaturErgebnis;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetUpdate;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * UITest für die Liga-Beispielturniere in zwei Konstellationen:
 * <ul>
 *   <li>6 Teams Hin- und Rückrunde (Standardvariante, gerade Teamanzahl).</li>
 *   <li>7 Teams mit Freispiel-Pfad (ungerade Teamanzahl) – deckt den
 *       Freilos-Code in Spielplan und Rangliste ab.</li>
 * </ul>
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}; bei
 * Algorithmen-Änderungen Referenz-JSONs neu erfassen (writeToJson temporär
 * aktivieren und Datei nach src/test/resources/.../liga/spielplan/ kopieren).
 */
public class LigaTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 2;
	private static final String STATUS_BULLET = "\u25cf";
	private static final String SUMMEN_LABEL_ABSTAND_RECHTS = "\u00a0\u00a0";
	private static final String[] TEAMNAMEN_6 = {
			"Boule Biebertal",
			"Boule-Freunde Fernwald",
			"Boulefreunde Marburg",
			"Boulodromedare Fulda 2",
			"DFG Wettenberg 1",
			"PC Petterweil"
	};
	private static final String[] TEAMNAMEN_7 = {
			"BC-Linden 1",
			"Boule Biebertal",
			"Boule-Freunde Fernwald",
			"Boulefreunde Marburg",
			"Boulodromedare Fulda 2",
			"DFG Wettenberg 1",
			"PC Petterweil"
	};

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
	public void testLigaTurnier6Teams() throws GenerateException {
		final int anzTeams = 6;
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();

		validiereGrundstruktur(TEAMNAMEN_6[0]);
		validiereMeldelistePerJson(anzTeams, "liga-meldeliste.json");
		validiereSpielplanPerJson("liga-spielplan.json");
		validiereTermineProTeilnehmer(TEAMNAMEN_6, 10);
		validiereRanglistePerJson(anzTeams, "liga-rangliste.json");
	}

	@Test
	public void testLigaTurnierMitFreispiel() throws GenerateException {
		final int anzTeams = 7;
		new LigaMitFreispielTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();

		validiereGrundstruktur(TEAMNAMEN_7[0]);
		validiereMeldelistePerJson(anzTeams, "liga-freispiel-meldeliste.json");
		validiereSpielplanPerJson("liga-freispiel-spielplan.json");
		validiereFreispielNurInfoImSpielplan();
		validiereTermineProTeilnehmer(TEAMNAMEN_7, 12);
		validiereRanglistePerJson(anzTeams, "liga-freispiel-rangliste.json");
		validiereRanglisteZaehltNurEchteBegegnungen(anzTeams);
		validiereStatusZaehltNurEchteBegegnungen();
	}

	/**
	 * Korrektheit der PTM-Metadaten (6 Teams): Meldeliste, Spielplan, Termine und Rangliste müssen je
	 * exakt ihren Identitäts-Schlüssel tragen – kein weiteres Blatt einen unerwarteten.
	 * (Liga erzeugt kein Direktvergleich-Blatt im Beispielturnier.)
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel() throws GenerateException {
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();

		Map<String, String> erwartung = new LinkedHashMap<>();
		erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE);
		erwartung.put(SheetNamen.spielplan(), SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN);
		for (int teamNr = 1; teamNr <= TEAMNAMEN_6.length; teamNr++) {
			erwartung.put(TEAMNAMEN_6[teamNr - 1],
					SheetMetadataHelper.schluesselLigaTermineProTeilnehmer(teamNr));
		}
		erwartung.put(SheetNamen.rangliste(), SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE);

		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	@Test
	public void terminlistenLassenPunkteLeerWennSiegeLeerSindUndBehaltenNullen() throws GenerateException {
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();

		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		int spielplanZeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		RowData spielplanRow = rangeDateFromRangePosition(
				RangePosition.from(0, spielplanZeile, LigaSpielPlanSheet.TEAM_B_NR_SPALTE, spielplanZeile),
				spielplan, wkingSpreadsheet.getWorkingSpreadsheetDocument()).get(0);
		String spielNr = spielplanRow.get(LigaSpielPlanSheet.SPIEL_NR_SPALTE).getStringVal();

		sheetHlp.clearValInCell(spielplan, Position.from(LigaSpielPlanSheet.SPIELE_A_SPALTE, spielplanZeile));
		sheetHlp.clearValInCell(spielplan, Position.from(LigaSpielPlanSheet.SPIELE_B_SPALTE, spielplanZeile));
		sheetHlp.clearValInCell(spielplan, Position.from(LigaSpielPlanSheet.SPIELPNKT_A_SPALTE, spielplanZeile));
		sheetHlp.setValInCell(spielplan, Position.from(LigaSpielPlanSheet.SPIELPNKT_B_SPALTE, spielplanZeile), 0);
		recalcAll();

		new LigaTermineProTeilnehmerSheet(wkingSpreadsheet)
				.generate(new LigaMeldeListeSheetUpdate(wkingSpreadsheet).getAlleMeldungen());
		recalcAll();

		RowData terminRow = terminZeile(spielNr);
		assertThat(terminRow.get(6).getStringVal()).as("Status ohne Ergebnis leer").isEmpty();
		assertThat(terminRow.get(7).getStringVal()).as("Punkte Eigene leer").isEmpty();
		assertThat(terminRow.get(8).getStringVal()).as("Punkte Gegner leer").isEmpty();
		assertThat(terminRow.get(9).getStringVal()).as("Siege Eigene leer").isEmpty();
		assertThat(terminRow.get(10).getStringVal()).as("Siege Gegner leer").isEmpty();
		assertThat(terminRow.get(11).getStringVal()).as("SpPunkte Eigene leer").isEmpty();
		assertThat(terminRow.get(12).getIntVal()).as("SpPunkte Gegner echte 0").isZero();
	}

	@Test
	public void terminlistenSignaturWirdBeiTerminrelevanterSpielplanaenderungDirty() throws GenerateException {
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();

		XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
		EingabeSignatur engine = new EingabeSignatur(SignaturQuellen::fuerLigaTermineProTeilnehmer);
		String hashVorher = berechneOk(engine, xDoc);

		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		sheetHlp.setStringValueInCell(StringCellValue.from(spielplan,
				Position.from(LigaSpielPlanSheet.ORT_SPALTE, LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE))
				.setValue("Boulodrome Test"));
		recalcAll();

		String hashNachher = berechneOk(engine, xDoc);
		assertThat(hashNachher)
				.as("Terminlisten-Fokus-Sync muss dirty werden, wenn Ort/Termin im Spielplan geändert wurde")
				.isNotEqualTo(hashVorher);
	}

	private void validiereGrundstruktur(String ersterTerminSheetName) {
		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.spielplan()))
				.as("Spielplan-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(ersterTerminSheetName))
				.as("Termine-pro-Teilnehmer-Sheet für Team 1 muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.rangliste()))
				.as("Rangliste-Sheet muss existieren").isNotNull();
	}

	private void validiereMeldelistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		RangePosition meldelisteRange = RangePosition.from(
				0, MELDELISTE_ERSTE_DATEN_ZEILE,
				6, MELDELISTE_ERSTE_DATEN_ZEILE + anzTeams - 1);

		// writeToJson(referenzDatei, meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = LigaTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private static String berechneOk(EingabeSignatur engine, XSpreadsheetDocument xDoc) {
		SignaturErgebnis ergebnis = engine.berechne(xDoc, 1);
		assertThat(ergebnis)
				.as("Liga-Terminlisten-Signatur muss für ein vollständiges Liga-Turnier berechenbar sein")
				.isInstanceOf(SignaturErgebnis.Ok.class);
		return ((SignaturErgebnis.Ok) ergebnis).hash();
	}

	private void validiereTermineProTeilnehmer(String[] teamNamen, int erwarteteTermineProTeam) {
		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		RangeData spielplanDaten = rangeDateFromRangePosition(
				RangePosition.from(0, 0, LigaSpielPlanSheet.SPIELPNKT_B_SPALTE, 110),
				spielplan, wkingSpreadsheet.getWorkingSpreadsheetDocument());
		for (int teamIndex = 0; teamIndex < teamNamen.length; teamIndex++) {
			int teamNr = teamIndex + 1;
			XSpreadsheet termine = sheetHlp.findByName(teamNamen[teamIndex]);
			assertThat(termine)
					.as("Termine-Sheet für Team %d muss den Teilnehmernamen tragen", teamNr)
					.isNotNull();
			RangeData rangeData = rangeDateFromRangePosition(
					RangePosition.from(0, 0, 12, 2 + erwarteteTermineProTeam + 5),
					termine, wkingSpreadsheet.getWorkingSpreadsheetDocument());

			assertThat(rangeData.get(0).get(0).getStringVal()).isEqualTo("Spiel");
			assertThat(rangeData.get(0).get(1).getStringVal()).isEqualTo("Datum");
			assertThat(rangeData.get(0).get(5).getStringVal()).isEqualTo("Gegner");
			assertThat(rangeData.get(0).get(6).getStringVal()).isEqualTo(I18n.get("liga.termine.header.status"));
			assertThat(zellProperty(termine, 6, 0, ICommonProperties.ROTATEANGLE))
					.as("Status-Header muss um 90 Grad gedreht sein")
					.isEqualTo(StringCellValue.ROTATEANGLE_PLUS_90);
			assertThat(rangeData.get(0).get(7).getStringVal()).isEqualTo("Punkte");
			assertThat(rangeData.get(0).get(9).getStringVal()).isEqualTo("Siege");
			assertThat(rangeData.get(0).get(11).getStringVal()).isEqualTo(I18n.get("liga.termine.header.sp.punkte"));
			assertThat(rangeData.get(1).get(7).getStringVal()).isEqualTo(I18n.get("liga.termine.header.eigene.kurz"));
			assertThat(rangeData.get(1).get(8).getStringVal()).isEqualTo(I18n.get("liga.termine.header.gegner.kurz"));
			assertThat(rangeData.get(1).get(9).getStringVal()).isEqualTo(I18n.get("liga.termine.header.eigene.kurz"));
			assertThat(rangeData.get(1).get(10).getStringVal()).isEqualTo(I18n.get("liga.termine.header.gegner.kurz"));
			assertThat(rangeData.get(1).get(11).getStringVal()).isEqualTo(I18n.get("liga.termine.header.eigene.kurz"));
			assertThat(rangeData.get(1).get(12).getStringVal()).isEqualTo(I18n.get("liga.termine.header.gegner.kurz"));

			int anzahlTermine = 0;
			int summenZeile = -1;
			for (int zeile = 2; zeile < rangeData.size(); zeile++) {
				String spielNr = rangeData.get(zeile).get(0).getStringVal();
				if (spielNr == null || spielNr.isBlank()) {
					break;
				}
				if ((I18n.get("column.header.summen") + SUMMEN_LABEL_ABSTAND_RECHTS).equals(spielNr)) {
					summenZeile = zeile;
					break;
				}
				assertThat(rangeData.get(zeile).get(5).getStringVal())
						.as("Team %d Termin %s darf kein Freispiel enthalten", teamNr, spielNr)
						.isNotEqualTo("Freispiel");
				pruefeTerminErgebnis(rangeData, zeile, spielplanDaten);
				pruefeTerminStatus(termine, rangeData, zeile);
				anzahlTermine++;
			}
			pruefeTerminSummen(termine, rangeData, summenZeile, anzahlTermine);

			assertThat(anzahlTermine)
					.as("Team %d muss die erwartete Anzahl Termine haben", teamNr)
					.isEqualTo(erwarteteTermineProTeam);
		}
	}

	private void validiereFreispielNurInfoImSpielplan() {
		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		RangeData spielplanDaten = rangeDateFromRangePosition(
				RangePosition.from(0, 0, LigaSpielPlanSheet.SPIELPNKT_B_SPALTE, 110),
				spielplan, wkingSpreadsheet.getWorkingSpreadsheetDocument());
		int freispielZeilen = 0;
		int ersteFreispielZeile = -1;
		int ersteEchteBegegnungZeile = -1;
		for (int zeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE; zeile < spielplanDaten.size(); zeile++) {
			RowData row = spielplanDaten.get(zeile);
			String spielNr = row.get(LigaSpielPlanSheet.SPIEL_NR_SPALTE).getStringVal();
			if (spielNr == null || spielNr.isBlank()) {
				break;
			}
			if (!"Freispiel".equals(row.get(LigaSpielPlanSheet.NAME_B_SPALTE).getStringVal())) {
				if (ersteEchteBegegnungZeile < 0) {
					ersteEchteBegegnungZeile = zeile;
				}
				continue;
			}
			freispielZeilen++;
			if (ersteFreispielZeile < 0) {
				ersteFreispielZeile = zeile;
			}
			for (int spalte = LigaSpielPlanSheet.KW_SPALTE; spalte <= LigaSpielPlanSheet.SPIELPNKT_B_SPALTE; spalte++) {
				if (spalte == LigaSpielPlanSheet.NAME_A_SPALTE) {
					assertThat(row.get(spalte).getStringVal())
							.as("%s: Freispiel-Heimteam muss gefuellt bleiben", spielNr)
							.isNotBlank();
					continue;
				}
				if (spalte == LigaSpielPlanSheet.NAME_B_SPALTE) {
					assertThat(row.get(spalte).getStringVal())
							.as("%s: Freispiel-Hinweis muss in der Gast-Spalte stehen", spielNr)
							.isEqualTo("Freispiel");
					continue;
				}
				assertThat(row.get(spalte).getStringVal())
						.as("%s: Freispiel-Zelle %d muss leer bleiben", spielNr, spalte)
						.isEmpty();
			}
			for (int spalte = LigaSpielPlanSheet.PUNKTE_A_SPALTE; spalte <= LigaSpielPlanSheet.SPIELPNKT_B_SPALTE; spalte++) {
				assertThat(row.get(spalte).getStringVal())
						.as("%s: Freispiel-Ergebniszelle %d muss leer bleiben", spielNr, spalte)
						.isEmpty();
			}
		}
		assertThat(freispielZeilen).as("Freispiel-Zeilen in Hin- und Rueckrunde").isEqualTo(14);
		validiereFreispielZebraFarbe(spielplan, ersteFreispielZeile);
		validiereFreispielNichtEditierbar(spielplan, ersteFreispielZeile, ersteEchteBegegnungZeile);
	}

	private void validiereFreispielZebraFarbe(XSpreadsheet spielplan, int zeile) {
		assertThat(zeile).as("Freispiel-Zeile fuer Farbpruefung").isGreaterThanOrEqualTo(0);
		int erwarteteFarbe = zeile % 2 == 0
				? new LigaKonfigurationSheet(wkingSpreadsheet).getSpielPlanHintergrundFarbeUnGerade()
				: new LigaKonfigurationSheet(wkingSpreadsheet).getSpielPlanHintergrundFarbeGerade();
		for (int spalte = LigaSpielPlanSheet.KW_SPALTE; spalte <= LigaSpielPlanSheet.SPIELPNKT_B_SPALTE; spalte++) {
			int farbe = zellFarbe(spielplan, spalte, zeile);
			assertThat(farbe)
					.as("Freispiel-Zelle %d/%d muss normale Zebra-Farbe haben", spalte, zeile)
					.isEqualTo(erwarteteFarbe)
					.isNotEqualTo(EditierbaresZelleFormatHelper.EDITIERBAR_GERADE_FARBE)
					.isNotEqualTo(EditierbaresZelleFormatHelper.EDITIERBAR_UNGERADE_FARBE);
		}
	}

	private int zellFarbe(XSpreadsheet sheet, int spalte, int zeile) {
		try {
			XPropertySet props = Lo.qi(XPropertySet.class, sheet.getCellByPosition(spalte, zeile));
			return (Integer) props.getPropertyValue(ICommonProperties.CELL_BACK_COLOR);
		} catch (Exception e) {
			throw new AssertionError("CellBackColor konnte nicht gelesen werden", e);
		}
	}

	private Object zellProperty(XSpreadsheet sheet, int spalte, int zeile, String propertyName) {
		try {
			XPropertySet props = Lo.qi(XPropertySet.class, sheet.getCellByPosition(spalte, zeile));
			return props.getPropertyValue(propertyName);
		} catch (Exception e) {
			throw new AssertionError(propertyName + " konnte nicht gelesen werden", e);
		}
	}

	private void validiereFreispielNichtEditierbar(XSpreadsheet spielplan, int freispielZeile,
			int echteBegegnungZeile) {
		assertThat(freispielZeile).as("Freispiel-Zeile fuer Schutzpruefung").isGreaterThanOrEqualTo(0);
		assertThat(echteBegegnungZeile).as("Echte Begegnungszeile fuer Schutzpruefung").isGreaterThanOrEqualTo(0);
		SheetSchutzInfo spielplanSchutz = LigaBlattschutzKonfiguration.get()
				.berechneSchutzInfos(wkingSpreadsheet).stream()
				.filter(info -> SheetNamen.spielplan().equals(Lo.qi(XNamed.class, info.sheet()).getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Liga-Spielplan-Schutzinfo nicht gefunden"));

		assertThat(istInEditierbaremBereich(spielplanSchutz, LigaSpielPlanSheet.DATUM_SPALTE, echteBegegnungZeile))
				.as("Datum echter Begegnungen bleibt editierbar")
				.isTrue();
		assertThat(istInEditierbaremBereich(spielplanSchutz, LigaSpielPlanSheet.DATUM_SPALTE, freispielZeile))
				.as("Datum in Freispiel-Zeilen darf nicht editierbar sein")
				.isFalse();
		assertThat(istInEditierbaremBereich(spielplanSchutz, LigaSpielPlanSheet.SPIELE_A_SPALTE, freispielZeile))
				.as("Ergebnis in Freispiel-Zeilen darf nicht editierbar sein")
				.isFalse();
	}

	private boolean istInEditierbaremBereich(SheetSchutzInfo info, int spalte, int zeile) {
		return info.editierbareBereich().stream().anyMatch(range ->
				spalte >= range.getStartSpalte() && spalte <= range.getEndeSpalte()
						&& zeile >= range.getStartZeile() && zeile <= range.getEndeZeile());
	}

	private void validiereStatusZaehltNurEchteBegegnungen() {
		LigaTurnierSchritt status = LigaStatusLeser.von(wkingSpreadsheet).liesStatus();

		assertThat(status.hrGesamt()).as("HR echte Begegnungen ohne Freispiel").isEqualTo(21);
		assertThat(status.rrGesamt()).as("RR echte Begegnungen ohne Freispiel").isEqualTo(21);
		assertThat(status.hrGespielt()).as("HR gespielt ohne Freispiel").isEqualTo(21);
		assertThat(status.rrGespielt()).as("RR gespielt ohne Freispiel").isEqualTo(21);
	}

	private void pruefeTerminErgebnis(RangeData termine, int terminZeile, RangeData spielplanDaten) {
		String spielNr = termine.get(terminZeile).get(0).getStringVal();
		RowData spielplanZeile = spielplanZeile(spielplanDaten, spielNr);
		boolean heim = I18n.get("liga.termine.heim").equals(termine.get(terminZeile).get(4).getStringVal());

		int[] eigeneSpalten = heim
				? new int[] {LigaSpielPlanSheet.PUNKTE_A_SPALTE, LigaSpielPlanSheet.SPIELE_A_SPALTE,
						LigaSpielPlanSheet.SPIELPNKT_A_SPALTE}
				: new int[] {LigaSpielPlanSheet.PUNKTE_B_SPALTE, LigaSpielPlanSheet.SPIELE_B_SPALTE,
						LigaSpielPlanSheet.SPIELPNKT_B_SPALTE};
		int[] gegnerSpalten = heim
				? new int[] {LigaSpielPlanSheet.PUNKTE_B_SPALTE, LigaSpielPlanSheet.SPIELE_B_SPALTE,
						LigaSpielPlanSheet.SPIELPNKT_B_SPALTE}
				: new int[] {LigaSpielPlanSheet.PUNKTE_A_SPALTE, LigaSpielPlanSheet.SPIELE_A_SPALTE,
						LigaSpielPlanSheet.SPIELPNKT_A_SPALTE};
		int[] eigeneTerminSpalten = {7, 9, 11};
		int[] gegnerTerminSpalten = {8, 10, 12};

		for (int i = 0; i < eigeneSpalten.length; i++) {
			assertThat(termine.get(terminZeile).get(eigeneTerminSpalten[i]).getIntVal())
					.as("%s: eigene Ergebnisspalte %d muss teambezogen aus dem Spielplan übernommen sein",
							spielNr, eigeneTerminSpalten[i])
					.isEqualTo(spielplanZeile.get(eigeneSpalten[i]).getIntVal());
			assertThat(termine.get(terminZeile).get(gegnerTerminSpalten[i]).getIntVal())
					.as("%s: Gegner-Ergebnisspalte %d muss teambezogen aus dem Spielplan übernommen sein",
							spielNr, gegnerTerminSpalten[i])
					.isEqualTo(spielplanZeile.get(gegnerSpalten[i]).getIntVal());
		}
	}

	private void pruefeTerminStatus(XSpreadsheet termineSheet, RangeData termine, int terminZeile) {
		assertThat(termine.get(terminZeile).get(6).getStringVal())
				.as("Status-Bullet muss bei gespielten Begegnungen sichtbar sein")
				.isEqualTo(STATUS_BULLET);
		int eigenePunkte = termine.get(terminZeile).get(7).getIntVal();
		int gegnerPunkte = termine.get(terminZeile).get(8).getIntVal();
		int erwarteteFarbe = eigenePunkte > gegnerPunkte
				? ColorHelper.CHAR_COLOR_GREEN
				: ColorHelper.CHAR_COLOR_RED;
		assertThat(zellProperty(termineSheet, 6, terminZeile, ICommonProperties.CHAR_COLOR))
				.as("Status-Bullet darf nicht schwarz bleiben")
				.isEqualTo(erwarteteFarbe);
	}

	private void pruefeTerminSummen(XSpreadsheet termineSheet, RangeData termine, int summenZeile, int anzahlTermine) {
		assertThat(summenZeile).as("Unter den Team-Terminen muss eine Summenzeile stehen").isGreaterThanOrEqualTo(2);
		assertThat(termine.get(summenZeile).get(0).getStringVal())
				.isEqualTo(I18n.get("column.header.summen") + SUMMEN_LABEL_ABSTAND_RECHTS);
		assertThat(zellProperty(termineSheet, 0, summenZeile, ICommonProperties.HORI_JUSTIFY))
				.as("Summe-Label muss rechtsbündig sein")
				.isEqualTo(CellHoriJustify.RIGHT);
		assertThat(istBereichGemerged(termineSheet, 0, summenZeile, 6, summenZeile))
				.as("Summe-Label muss bis einschließlich Status-Spalte gemergt sein")
				.isTrue();
		assertThat(termine.get(summenZeile).get(6).getStringVal()).as("Status hat keine Summe").isEmpty();
		for (int spalte = 7; spalte <= 12; spalte++) {
			int erwarteteSumme = 0;
			for (int zeile = 2; zeile < 2 + anzahlTermine; zeile++) {
				erwarteteSumme += termine.get(zeile).get(spalte).getIntVal(0);
			}
			assertThat(termine.get(summenZeile).get(spalte).getIntVal())
					.as("Summenzeile Spalte %d muss die Team-Terminwerte summieren", spalte)
					.isEqualTo(erwarteteSumme);
		}
	}

	private boolean istBereichGemerged(XSpreadsheet sheet, int startSpalte, int startZeile, int endeSpalte, int endeZeile) {
		try {
			XMergeable mergeable = Lo.qi(XMergeable.class,
					sheet.getCellRangeByPosition(startSpalte, startZeile, endeSpalte, endeZeile));
			return mergeable.getIsMerged();
		} catch (Exception e) {
			throw new AssertionError("Merge-Status konnte nicht gelesen werden", e);
		}
	}

	private RowData spielplanZeile(RangeData spielplanDaten, String spielNr) {
		for (RowData row : spielplanDaten) {
			if (spielNr.equals(row.get(0).getStringVal())) {
				return row;
			}
		}
		throw new AssertionError("Spielplan-Zeile nicht gefunden: " + spielNr);
	}

	private RowData terminZeile(String spielNr) {
		for (String teamName : TEAMNAMEN_6) {
			XSpreadsheet termine = sheetHlp.findByName(teamName);
			RangeData termineDaten = rangeDateFromRangePosition(
					RangePosition.from(0, 0, 12, 80),
					termine, wkingSpreadsheet.getWorkingSpreadsheetDocument());
			for (RowData row : termineDaten) {
				if (spielNr.equals(row.get(0).getStringVal())) {
					return row;
				}
			}
		}
		throw new AssertionError("Termin-Zeile nicht gefunden: " + spielNr);
	}

	private void validiereSpielplanPerJson(String referenzDatei) throws GenerateException {
		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		// 6 Teams: 10 Spieltage; 7 Teams mit Freispiel: 14 Spieltage. Großzügiger Bereich.
		RangePosition spielplanRange = RangePosition.from(0, 0, 12, 110);

		// writeToJson(referenzDatei, spielplanRange, spielplan, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(spielplanRange, spielplan,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = LigaTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereRanglistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		// Rangliste hat 2 Header-Zeilen plus anzTeams Datenzeilen; +1 Puffer am Ende.
		RangePosition ranglisteRange = RangePosition.from(0, 0, 6, 2 + anzTeams);

		// writeToJson(referenzDatei, ranglisteRange, rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, rangliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = LigaTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereRanglisteZaehltNurEchteBegegnungen(int anzTeams) {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		RangeData begegnungen = rangeDateFromRangePosition(
				RangePosition.from(11, 2, 11, 2 + anzTeams - 1),
				rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		for (int i = 0; i < begegnungen.size(); i++) {
			assertThat(begegnungen.get(i).get(0).getIntVal())
					.as("Ranglisten-Zeile %d muss 12 echte Begegnungen ohne Freispiel zaehlen", i + 1)
					.isEqualTo(12);
		}
	}

	/**
	 * Regression im Kiosk-Modus: nach voller 6-Team-Turniergenerierung muss ein
	 * erneutes {@link LigaRanglisteSheet#run()} unter aktivem TurnierModus +
	 * Liga-Blattschutz sauber durchlaufen.
	 */
	@Test
	public void kioskModus_ranglisteUpdateNach6TeamTurnier() throws GenerateException {
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();
		mitKioskModus(TurnierSystem.LIGA, () -> new LigaRanglisteSheet(wkingSpreadsheet).run());

		assertThat(sheetHlp.findByName(SheetNamen.rangliste()))
				.as("Liga-Rangliste muss nach Kiosk-Update weiterhin existieren")
				.isNotNull();
	}
}
