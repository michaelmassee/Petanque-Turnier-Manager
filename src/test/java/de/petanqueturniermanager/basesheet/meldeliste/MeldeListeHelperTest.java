package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.SortedMap;
import java.util.TreeMap;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XText;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.DoppelteStartnummerException;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;

public class MeldeListeHelperTest {
	private MeldeListeHelper<SpielerMeldungen, Spieler> meldeListeHelper;

	private IMeldeliste<SpielerMeldungen, Spieler> iMeldelisteMock;
	private SheetHelper sheetHelperMock;
	private MeldungenSpalte<SpielerMeldungen, Spieler> meldungenSpalteMock;
	XSpreadsheet xSpreadsheetMock;

	/** steuert die Test-Antwort von {@link MeldeListeHelper#sollAutomatischNeuNummeriertWerden(int)}. */
	private boolean automatischNeuNummerierenAntwortJa;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void init() throws GenerateException {
		I18n.init(null);
		iMeldelisteMock = Mockito.mock(IMeldeliste.class);
		sheetHelperMock = Mockito.mock(SheetHelper.class);
		meldungenSpalteMock = Mockito.mock(MeldungenSpalte.class);
		// XProtectable: Nr-Formeln werden über BlattschutzManager.schreibeEntsperrt geschrieben
		xSpreadsheetMock = Mockito.mock(XSpreadsheet.class, Mockito.withSettings().extraInterfaces(XProtectable.class));
		Mockito.when(sheetHelperMock.getFormulaArrayFromRange(any(XSpreadsheet.class), any(RangePosition.class)))
				.thenReturn(new String[0][0]);
		automatischNeuNummerierenAntwortJa = true;

		Mockito.when(iMeldelisteMock.getMeldungenSpalte()).thenReturn(meldungenSpalteMock);
		Mockito.when(iMeldelisteMock.getSheetHelper()).thenReturn(sheetHelperMock);

		// für RangeHelper.from(ISheet, ...) in zeileOhneSpielerNamenEntfernen() benötigt
		WorkingSpreadsheet workingSpreadsheetMock = Mockito.mock(WorkingSpreadsheet.class);
		Mockito.when(workingSpreadsheetMock.getWorkingSpreadsheetDocument())
				.thenReturn(Mockito.mock(XSpreadsheetDocument.class));
		Mockito.when(iMeldelisteMock.getWorkingSpreadsheet()).thenReturn(workingSpreadsheetMock);
		Mockito.when(iMeldelisteMock.getXSpreadSheet()).thenReturn(xSpreadsheetMock);

		meldeListeHelper = new MeldeListeHelper<SpielerMeldungen, Spieler>(iMeldelisteMock, "TEST_SCHLUESSEL") {
			@Override
			public void doSort(int spalteNr, boolean isAscending) throws GenerateException {
				// nichts
			}

			@Override
			public XSpreadsheet getXSpreadSheet() {
				return xSpreadsheetMock;
			}

			@Override
			boolean sollAutomatischNeuNummeriertWerden(int doppelteNr) {
				return automatischNeuNummerierenAntwortJa;
			}
		};
	}

	@Test
	public void testCleanUpSpielerName() throws Exception {
		String result = meldeListeHelper.cleanUpSpielerName(" ÄÜö # + ösX YSDdRfÖßßäslkg .,m..,m ");
		assertThat(result).isEqualTo("äüöösxysddrfößßäslkgmm");
	}

	@Test
	public void teamNameFormelVerwendetZentralePtmTeamAnzeige() {
		String formel = MeldeListeHelper.teamNameFormel("$G$6", false, Formation.TRIPLETTE, true);

		assertThat(formel)
				.contains("PTM.ALG.TEAMANZEIGE(0;3;1;")
				.contains("INDEX($'" + I18n.get("sheet.name.meldeliste") + "'.$A$1:$Z$999;")
				.contains("MATCH($G$6;$'" + I18n.get("sheet.name.meldeliste") + "'.$A$1:$A$999;0);0)");
	}

	@Test
	public void uuidTeamAnzeigeVerwendetEineEinzigeFunktion() {
		String uuid = "abba7c10-dbbf-4910-b7ec-b78af3070b1f";
		String bereich = "$'" + I18n.get("sheet.name.meldeliste") + "'.$A$1:$Z$999";
		assertThat(MeldeListeHelper.teamNameFormel(TeamSchluessel.uuidLiteral(uuid), false, Formation.DOUBLETTE, false))
				.isEqualTo("PTM.ALG.UUID.TEAMANZEIGE(0;2;0;\"" + uuid + "\";" + bereich + ")");
		assertThat(MeldeListeHelper.teamNameFormel(TeamSchluessel.uuidLiteral(uuid), true, Formation.DOUBLETTE, false))
				.isEqualTo("PTM.ALG.UUID.TEAMANZEIGE(1;2;0;\"" + uuid + "\";" + bereich + ")");
		assertThat(MeldeListeHelper.spielerNamenFormel(TeamSchluessel.uuidLiteral(uuid), true, Formation.TRIPLETTE, true))
				.isEqualTo("PTM.ALG.UUID.TEAMANZEIGE(0;3;1;\"" + uuid + "\";" + bereich + ";1)");
	}

	@Test
	public void teamNameFormelMitTeamnameSpalteLiestTeamnameAbSpalteA() {
		String formel = MeldeListeHelper.teamNameFormel("$G$6", true, Formation.DOUBLETTE, false);

		assertThat(formel)
				.contains("PTM.ALG.TEAMANZEIGE(1;2;0;")
				.contains("INDEX($'" + I18n.get("sheet.name.meldeliste") + "'.$A$1:$Z$999;");
	}

	@Test
	public void spielerNamenFormelMitTeamnameSpalteBeginntBeiSpalteB() {
		String formel = MeldeListeHelper.spielerNamenFormel("$G$6", true, Formation.TRIPLETTE, true);

		assertThat(formel)
				.as("Teamname darf nicht als Vorname von Spieler 1 gelesen werden")
				.contains("PTM.ALG.TEAMANZEIGE(0;3;1;")
				.contains("INDEX($'" + I18n.get("sheet.name.meldeliste") + "'.$B$1:$Z$999;")
				.contains("MATCH($G$6;$'" + I18n.get("sheet.name.meldeliste") + "'.$A$1:$A$999;0);0)");
	}

	@Test
	public void spielerNamenFormelOhneTeamnameSpalteBeginntBeiSpalteA() {
		String formel = MeldeListeHelper.spielerNamenFormel("$G$6", false, Formation.DOUBLETTE, false);

		assertThat(formel)
				.contains("PTM.ALG.TEAMANZEIGE(0;2;0;")
				.contains("INDEX($'" + I18n.get("sheet.name.meldeliste") + "'.$A$1:$Z$999;");
	}

	@Test
	public void testTestDoppelteNamenMitUmlauten() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Müller"),
				new SpielerNrName(10, "Müller") };

		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
			fail("Erwarte GenerateException");
		} catch (GenerateException exc) {
			assertThat(exc.getMessage()).containsOnlyOnce("Spieler Namen Müller ist doppelt in der Meldeliste");
		}
	}

	@Test
	public void testTestDoppelteNamenMitZonderZeichenundLeerstellen() throws Exception {
		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Maja   Biene"),
				new SpielerNrName(10, "Maja, Biene") };
		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
			fail("Erwarte GenerateException");
		} catch (GenerateException exc) {
			assertThat(exc.getMessage()).containsOnlyOnce("Spieler Namen Maja, Biene ist doppelt in der Meldeliste");
		}
	}

	@Test
	public void testTestDoppelteNamenIgnoreCase() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(10, "heinz"), new SpielerNrName(4, "Klaus"), new SpielerNrName(12, "Heinz") };

		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
			fail("Erwarte GenerateException");
		} catch (GenerateException exc) {
			assertThat(exc.getMessage()).containsOnlyOnce("Spieler Namen Heinz ist doppelt in der Meldeliste");
		}
	}

	@Test
	public void testTestDoppelteSpielrNr() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(12, "Petra"), new SpielerNrName(4, "Klaus"), new SpielerNrName(12, "Heinz") };
		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
			fail("Erwarte DoppelteStartnummerException");
		} catch (DoppelteStartnummerException exc) {
			assertThat(exc.getStartnummer()).isEqualTo(12);
			assertThat(exc.getMessage()).containsOnlyOnce("Spieler Nr. 12 ist doppelt");
		}
	}

	@Test
	public void testKorrigiereDoppelteStartnummer_NurBetroffeneZeilenNeuNummeriert() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(12, "Petra"), new SpielerNrName(4, "Klaus"), new SpielerNrName(12, "Heinz") };
		initReturnSpielerDaten(spielerNrNameList);

		meldeListeHelper.korrigiereDoppelteStartnummer(12);

		// nur die zweite Fundstelle (Zeile mit "Heinz") wird neu nummeriert, mit der kleinsten
		// freien Lücke (1 ist frei, da nur 32/12/4 belegt sind) statt max+1 – als TNRUUID-Formel.
		assertThat(geschriebeneNrFormeln()).containsOnlyKeys(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + 3)
				.hasValueSatisfying(new Condition<>(
						formel -> formel.startsWith(GlobalImpl.PTM_TNR_UUID + "(1;"), "Nr 1 mit UUID"));
	}

	@Test
	public void testKorrigiereDoppelteStartnummer_MehrfacheDuplikate_bekommenVerschiedeneLuecken() throws Exception {
		// Regression: bei mehr als zwei gleichen Startnummern (Mehrfach-Duplikat) muss jede
		// weitere Fundstelle eine eigene freie Lücke bekommen, nicht dieselbe.
		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(12, "Anna"),
				new SpielerNrName(3, "Petra"), new SpielerNrName(12, "Klaus"), new SpielerNrName(12, "Heinz") };
		initReturnSpielerDaten(spielerNrNameList);

		meldeListeHelper.korrigiereDoppelteStartnummer(12);

		// Erste Fundstelle (Anna) behält 12; Klaus und Heinz bekommen je eine eigene freie Nr.
		// Belegt sind 12 und 3, also sind 1 und 2 frei.
		assertThat(geschriebeneNrFormeln().values()).map(formel -> formel.substring(0, formel.indexOf(';')))
				.containsExactly(GlobalImpl.PTM_TNR_UUID + "(1", GlobalImpl.PTM_TNR_UUID + "(2");
	}

	/** Alle per setFormulaArrayInRange geschriebenen Nr-Formeln je Zeile. */
	private SortedMap<Integer, String> geschriebeneNrFormeln() {
		ArgumentCaptor<RangePosition> bereiche = ArgumentCaptor.forClass(RangePosition.class);
		ArgumentCaptor<String[][]> formeln = ArgumentCaptor.forClass(String[][].class);
		verify(sheetHelperMock, Mockito.atLeastOnce()).setFormulaArrayInRange(any(XSpreadsheet.class),
				bereiche.capture(), formeln.capture());
		SortedMap<Integer, String> ergebnis = new TreeMap<>();
		for (int i = 0; i < bereiche.getAllValues().size(); i++) {
			String[][] block = formeln.getAllValues().get(i);
			for (int zeile = 0; zeile < block.length; zeile++) {
				ergebnis.put(bereiche.getAllValues().get(i).getStartZeile() + zeile, block[zeile][0]);
			}
		}
		return ergebnis;
	}

	@Test
	public void testPruefeUndKorrigiereDoppelteStartnummern_UserSagtJa_korrigiertUndPrueftErneut() throws Exception {
		// testDoppelteMeldungen() findet nur beim ersten Aufruf die doppelte Startnummer;
		// beim zweiten (nach der Korrektur) sind die Mock-Daten "bereinigt" simuliert.
		MeldeListeHelper<SpielerMeldungen, Spieler> spyHelper = Mockito.spy(meldeListeHelper);
		Mockito.doThrow(new DoppelteStartnummerException(12, "doppelt"))
				.doNothing()
				.when(spyHelper).testDoppelteMeldungen();
		Mockito.doNothing().when(spyHelper).korrigiereDoppelteStartnummer(12);

		spyHelper.pruefeUndKorrigiereDoppelteStartnummern();

		verify(spyHelper, times(1)).korrigiereDoppelteStartnummer(12);
		verify(spyHelper, times(2)).testDoppelteMeldungen();
	}

	@Test
	public void testPruefeUndKorrigiereDoppelteStartnummern_UserSagtNein_wirftExceptionOhneKorrektur() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(12, "Petra"), new SpielerNrName(4, "Klaus"), new SpielerNrName(12, "Heinz") };
		initReturnSpielerDaten(spielerNrNameList);
		automatischNeuNummerierenAntwortJa = false;

		try {
			meldeListeHelper.pruefeUndKorrigiereDoppelteStartnummern();
			fail("Erwarte DoppelteStartnummerException");
		} catch (DoppelteStartnummerException exc) {
			assertThat(exc.getStartnummer()).isEqualTo(12);
			assertThat(exc.getMessage()).containsOnlyOnce("Spieler Nr. 12 ist doppelt");
		}
		// nur die Markierung durch testDoppelteMeldungen() selbst (Zellfarbe), keine Korrektur der Nr-Formeln
		verify(sheetHelperMock, Mockito.never()).setFormulaArrayInRange(any(XSpreadsheet.class), any(RangePosition.class),
				any(String[][].class));
		verify(sheetHelperMock, Mockito.never()).setNumberValueInCell(any(NumberCellValue.class));
	}

	@Test
	public void testDoppelteDaten_NoError() throws Exception {

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(10, "Petra"), new SpielerNrName(4, "Klaus"), new SpielerNrName(12, "Heinz") };

		initReturnSpielerDaten(spielerNrNameList);

		try {
			meldeListeHelper.testDoppelteMeldungen();
		} catch (GenerateException exc) {
			fail("GenerateException", exc);
		}
	}

	@Test
	public void testDoppelteMeldungen_EinzelneMeldungMitUngueltigerNr_wirdBereinigt() throws Exception {
		// Regression: einzelne Meldung (letzteSpielZeile == ErsteDatenZiele) darf nicht als
		// "keine Daten" behandelt werden - eine ungueltige Nr in der einzigen Zeile muss
		// weiterhin geloescht werden (derselbe Off-by-One wie bei updateMeldungenNr()).
		Mockito.when(iMeldelisteMock.getErsteDatenZiele()).thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE);

		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(0, "Anna") };
		initReturnSpielerDaten(spielerNrNameList);

		meldeListeHelper.testDoppelteMeldungen();

		verify(sheetHelperMock, times(1)).clearValInCell(any(XSpreadsheet.class),
				eq(Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, MeldeListeKonstanten.ERSTE_DATEN_ZEILE)));
	}

	@Test
	public void testZeileOhneSpielerNamenEntfernen() throws Exception {

		// Achtung: die liste wird Sortiert mit leeren namen nach unten
		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(32, "Anna"),
				new SpielerNrName(10, "Petra"), new SpielerNrName(12, "Heinz"),
				// --------------------
				new SpielerNrName(4, "") // muss entfernt werden
				// --------------------
		};

		initReturnSpielerDaten(spielerNrNameList);

		Mockito.when(iMeldelisteMock.letzteZeileMitSpielerName())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrNameList.length - 2);
		Mockito.when(meldungenSpalteMock.letzteZeileMitSpielerName())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrNameList.length - 2);
		// Aktiv-Spalte/Spieltag-Ende der Meldeliste - muss beim Leeren der Zeile mitgenommen werden
		int letzteSpielTagSpalte = 6;
		Mockito.when(iMeldelisteMock.letzteSpielTagSpalte()).thenReturn(letzteSpielTagSpalte);

		meldeListeHelper.zeileOhneSpielerNamenEntfernen();

		int leereZeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrNameList.length - 1;
		// Ganze Zeile (Nr bis Spieltag-Ende) muss geleert werden, nicht nur die Nr-Zelle -
		// sonst bleiben verwaiste Werte (z.B. Aktiv-Flag) in der Leerzeile stehen.
		verify(xSpreadsheetMock, times(1)).getCellRangeByPosition(MeldeListeKonstanten.SPIELER_NR_SPALTE, leereZeile,
				letzteSpielTagSpalte, leereZeile);
	}

	@Test
	public void testZeileOhneSpielerNamenEntfernen_KeinNameMehrVorhanden_leertAlleZeilen() throws Exception {
		SpielerNrName[] spielerNrNameList = new SpielerNrName[] { new SpielerNrName(1, ""),
				new SpielerNrName(2, ""), new SpielerNrName(3, "") };
		initReturnSpielerDaten(spielerNrNameList);
		// keine Zeile hat mehr einen Namen -> letzteZeileMitSpielerName liefert 0
		Mockito.when(iMeldelisteMock.letzteZeileMitSpielerName()).thenReturn(0);
		Mockito.when(iMeldelisteMock.getErsteDatenZiele()).thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		int letzteSpielTagSpalte = 6;
		Mockito.when(iMeldelisteMock.letzteSpielTagSpalte()).thenReturn(letzteSpielTagSpalte);

		meldeListeHelper.zeileOhneSpielerNamenEntfernen();

		for (int i = 0; i < spielerNrNameList.length; i++) {
			int zeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE + i;
			verify(xSpreadsheetMock, times(1)).getCellRangeByPosition(MeldeListeKonstanten.SPIELER_NR_SPALTE, zeile,
					letzteSpielTagSpalte, zeile);
		}
	}



	/** Stubbt sowohl getTextFromCell (getrimmt) als auch getXTextFromCell (roh, ungetrimmt) für eine Zelle. */
	private void stubZelle(Position pos, String rawText) {
		Mockito.when(sheetHelperMock.getTextFromCell(any(XSpreadsheet.class), eq(pos)))
				.thenReturn(rawText == null ? null : rawText.trim());
		XText xTextMock = Mockito.mock(XText.class);
		Mockito.when(xTextMock.getString()).thenReturn(rawText == null ? "" : rawText);
		Mockito.when(sheetHelperMock.getXTextFromCell(any(XSpreadsheet.class), eq(pos))).thenReturn(xTextMock);
	}

	@Test
	public void testBereinigeUngueltigeSetzpositionWerte_LoeschtNurUngueltigeWerte() throws Exception {
		int spalte = 4;
		int nameSpalte = 1;
		int ersteZeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;

		// Alle Zeilen haben einen Namen -> nur der SP-Wert selbst entscheidet
		for (int i = 0; i <= 4; i++) {
			stubZelle(Position.from(nameSpalte, ersteZeile + i), "Team " + i);
		}

		// Zeile 0: leer -> bleibt unangetastet
		stubZelle(Position.from(spalte, ersteZeile), null);
		// Zeile 1: "0" -> gueltig (kein Setzstatus), bleibt
		stubZelle(Position.from(spalte, ersteZeile + 1), "0");
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
				eq(Position.from(spalte, ersteZeile + 1)))).thenReturn(0);
		// Zeile 2: "5" -> gueltig, bleibt
		stubZelle(Position.from(spalte, ersteZeile + 2), "5");
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
				eq(Position.from(spalte, ersteZeile + 2)))).thenReturn(5);
		// Zeile 3: "abc" -> ungueltig (Text), muss geloescht werden
		stubZelle(Position.from(spalte, ersteZeile + 3), "abc");
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
				eq(Position.from(spalte, ersteZeile + 3)))).thenReturn(-1);
		// Zeile 4: "-3" -> ungueltig (negativ), muss geloescht werden
		stubZelle(Position.from(spalte, ersteZeile + 4), "-3");
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
				eq(Position.from(spalte, ersteZeile + 4)))).thenReturn(-3);

		meldeListeHelper.bereinigeUngueltigeSetzpositionWerte(spalte, nameSpalte, ersteZeile, ersteZeile + 4);

		verify(sheetHelperMock, times(1)).clearValInCell(any(XSpreadsheet.class),
				eq(Position.from(spalte, ersteZeile + 3)));
		verify(sheetHelperMock, times(1)).clearValInCell(any(XSpreadsheet.class),
				eq(Position.from(spalte, ersteZeile + 4)));
		verify(sheetHelperMock, times(2)).clearValInCell(any(XSpreadsheet.class), any(Position.class));
	}

	@Test
	public void testBereinigeUngueltigeSetzpositionWerte_LoeschtSpOhneZugehoerigeMeldung() throws Exception {
		int spalte = 4;
		int nameSpalte = 1;
		int ersteZeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;

		// Zeile 0: gueltige SP "1", aber kein Name -> "verwaist", muss geloescht werden
		stubZelle(Position.from(nameSpalte, ersteZeile), null);
		stubZelle(Position.from(spalte, ersteZeile), "1");
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(Position.from(spalte, ersteZeile))))
				.thenReturn(1);
		// Zeile 1: gueltige SP "1" mit Name -> bleibt
		stubZelle(Position.from(nameSpalte, ersteZeile + 1), "Team 1");
		stubZelle(Position.from(spalte, ersteZeile + 1), "1");
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
				eq(Position.from(spalte, ersteZeile + 1)))).thenReturn(1);

		meldeListeHelper.bereinigeUngueltigeSetzpositionWerte(spalte, nameSpalte, ersteZeile, ersteZeile + 1);

		verify(sheetHelperMock, times(1)).clearValInCell(any(XSpreadsheet.class),
				eq(Position.from(spalte, ersteZeile)));
		verify(sheetHelperMock, times(1)).clearValInCell(any(XSpreadsheet.class), any(Position.class));
	}

	@Test
	public void testBereinigeUngueltigeSetzpositionWerte_LoeschtReinesLeerzeichen() throws Exception {
		int spalte = 4;
		int nameSpalte = 1;
		int ersteZeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;

		stubZelle(Position.from(nameSpalte, ersteZeile), "Team 0");
		// Zelle enthaelt nur ein Leerzeichen (roh " ", getrimmt "") - muss trotzdem als "hat Inhalt"
		// erkannt und wegen ungueltigem Wert geloescht werden, statt als leer uebersprungen zu werden.
		stubZelle(Position.from(spalte, ersteZeile), " ");
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(Position.from(spalte, ersteZeile))))
				.thenReturn(-1);

		meldeListeHelper.bereinigeUngueltigeSetzpositionWerte(spalte, nameSpalte, ersteZeile, ersteZeile);

		verify(sheetHelperMock, times(1)).clearValInCell(any(XSpreadsheet.class),
				eq(Position.from(spalte, ersteZeile)));
	}

	private void initReturnSpielerDaten(SpielerNrName[] spielerNrnameList) throws GenerateException {

		Position spielerNrPos = Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		Position spielerNamePos = Position.from(meldeListeHelper.getSpielerNameSpalte(),
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		int zeileCntr = 0;
		for (SpielerNrName spielerNrName : spielerNrnameList) {
			int zeile = MeldeListeKonstanten.ERSTE_DATEN_ZEILE + zeileCntr;
			Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
					eq(Position.from(spielerNrPos.zeile(zeile))))).thenReturn(spielerNrName.nr);
			Mockito.when(sheetHelperMock.getTextFromCell(any(XSpreadsheet.class),
					eq(Position.from(spielerNamePos.zeile(zeile))))).thenReturn(spielerNrName.name);
			Mockito.when(meldungenSpalteMock.leseSpielerNameZeile(any(XSpreadsheet.class), eq(zeile)))
					.thenReturn(spielerNrName.name);
			zeileCntr++;
		}

		// Nr-Formeln wie im Altbestand: reine Zahlen ohne UUID
		String[][] nrFormeln = new String[spielerNrnameList.length][1];
		for (int i = 0; i < spielerNrnameList.length; i++) {
			nrFormeln[i][0] = String.valueOf(spielerNrnameList[i].nr);
		}
		Mockito.when(sheetHelperMock.getFormulaArrayFromRange(any(XSpreadsheet.class), any(RangePosition.class)))
				.thenReturn(nrFormeln);

		Mockito.when(iMeldelisteMock.letzteZeileMitSpielerName())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length - 1);
		Mockito.when(meldungenSpalteMock.letzteZeileMitSpielerName())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length - 1);

		Mockito.when(iMeldelisteMock.naechsteFreieDatenZeileInSpielerNrSpalte())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length);
		Mockito.when(meldungenSpalteMock.naechsteFreieDatenZeileInSpielerNrSpalte())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + spielerNrnameList.length);
	}
}

class SpielerNrName {
	public final int nr;
	public final String name;

	SpielerNrName(int nr, String name) {
		this.nr = nr;
		this.name = name;
	}

}
