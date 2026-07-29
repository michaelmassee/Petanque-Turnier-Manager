package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.DoppelteStartnummerException;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
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
		xSpreadsheetMock = Mockito.mock(XSpreadsheet.class);
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

		ArgumentCaptor<NumberCellValue> captor = ArgumentCaptor.forClass(NumberCellValue.class);
		verify(sheetHelperMock, times(1)).setNumberValueInCell(captor.capture());
		NumberCellValue neuerWert = captor.getValue();
		// nur die zweite Fundstelle (Zeile mit "Heinz") wird neu nummeriert, mit naechster freier Nr (32+1)
		assertThat(neuerWert.getValue()).isEqualTo(33.0);
		assertThat(neuerWert.getPos().getZeile())
				.isEqualTo(MeldeListeKonstanten.ERSTE_DATEN_ZEILE + 3);
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
		// nur die Rot-Markierung durch testDoppelteMeldungen() selbst, keine zusaetzliche Korrektur
		verify(sheetHelperMock, times(1)).setNumberValueInCell(any(NumberCellValue.class));
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
	public void testUpdateMeldungenNr_EinzelneMeldungOhneNummer_bekommtNummer() throws Exception {
		// Regression: einzelne Meldung (letzteSpielZeile == ErsteDatenZiele) darf beim
		// Nummerieren nicht als "keine Daten" behandelt werden (siehe Trip-Tête-Bug).
		Mockito.when(iMeldelisteMock.getErsteDatenZiele()).thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		Mockito.when(meldungenSpalteMock.letzteZeileMitSpielerName())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		Mockito.when(iMeldelisteMock.naechsteFreieDatenZeileInSpielerNrSpalte())
				.thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class), eq(Position
				.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, MeldeListeKonstanten.ERSTE_DATEN_ZEILE))))
				.thenReturn(-1);

		meldeListeHelper.updateMeldungenNr(TeilnehmerListeSortModus.NUMMER);

		verify(sheetHelperMock, times(1)).setNumberValueInCell(any());
	}

	@Test
	public void testUpdateMeldungenNr_LueckeVorhanden_wirdZuerstGefuellt() throws Exception {
		// Regression: eine neue Meldung ohne Nummer muss zuerst eine freie Lücke (hier Nr. 2,
		// z.B. durch eine gelöschte Meldung entstanden) bekommen, nicht einfach die höchste
		// vorhandene Nummer + 1.
		int zeileMitNr3 = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;
		int zeileMitNr1 = zeileMitNr3 + 1;
		int zeileOhneNr = zeileMitNr3 + 2;

		Mockito.when(iMeldelisteMock.getErsteDatenZiele()).thenReturn(MeldeListeKonstanten.ERSTE_DATEN_ZEILE);
		Mockito.when(meldungenSpalteMock.letzteZeileMitSpielerName()).thenReturn(zeileOhneNr);
		Mockito.when(iMeldelisteMock.naechsteFreieDatenZeileInSpielerNrSpalte()).thenReturn(zeileOhneNr);

		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
				eq(Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, zeileMitNr3)))).thenReturn(3);
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
				eq(Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, zeileMitNr1)))).thenReturn(1);
		Mockito.when(sheetHelperMock.getIntFromCell(any(XSpreadsheet.class),
				eq(Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, zeileOhneNr)))).thenReturn(-1);

		ArgumentCaptor<NumberCellValue> captor = ArgumentCaptor.forClass(NumberCellValue.class);

		meldeListeHelper.updateMeldungenNr(TeilnehmerListeSortModus.NUMMER);

		verify(sheetHelperMock, times(1)).setNumberValueInCell(captor.capture());
		assertThat(captor.getValue().getValue()).isEqualTo(2.0);
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
