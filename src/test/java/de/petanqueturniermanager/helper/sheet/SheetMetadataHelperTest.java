package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XNamedRanges;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellAddress;

import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * Unit-Tests für {@link SheetMetadataHelper}.
 * <p>
 * Die Tests nutzen die package-private Überladungen, die ohne UNO-Interface-Casting
 * ({@code Lo.qi}) auskommen. Dadurch werden sie ohne LibreOffice-Laufzeitumgebung
 * und ohne PowerMock ausführbar.
 */
class SheetMetadataHelperTest {

	private XNamedRanges mockRanges;
	private XSpreadsheet mockSheet;
	private XSpreadsheetDocument mockDoc;

	@BeforeEach
	void setUp() {
		mockRanges = mock(XNamedRanges.class);
		mockSheet = mock(XSpreadsheet.class);
		mockDoc = mock(XSpreadsheetDocument.class);
	}

	// ── schreibeSheetMetadaten ───────────────────────────────────────────────

	@Test
	void schreibeSheetMetadaten_legtNeuenBereichAn() throws Exception {
		when(mockRanges.hasByName("__PTM_TEST__")).thenReturn(false);

		SheetMetadataHelper.schreibeSheetMetadaten(mockRanges, "Rangliste", 2, "__PTM_TEST__");

		verify(mockRanges, never()).removeByName(any());
		verify(mockRanges).addNewByName(eq("__PTM_TEST__"), eq("$'Rangliste'.$A$1"),
				any(CellAddress.class), eq(0));
	}

	@Test
	void schreibeSheetMetadaten_ueberschreibtVorhandenenBereich() throws Exception {
		when(mockRanges.hasByName("__PTM_TEST__")).thenReturn(true);

		SheetMetadataHelper.schreibeSheetMetadaten(mockRanges, "Rangliste", 0, "__PTM_TEST__");

		verify(mockRanges).removeByName("__PTM_TEST__");
		verify(mockRanges).addNewByName(eq("__PTM_TEST__"), eq("$'Rangliste'.$A$1"),
				any(CellAddress.class), eq(0));
	}

	@Test
	void schreibeSheetMetadaten_apostrophImSheetNamenWirdEscaped() throws Exception {
		when(mockRanges.hasByName("__PTM_TEST__")).thenReturn(false);

		SheetMetadataHelper.schreibeSheetMetadaten(mockRanges, "O'Brien", 0, "__PTM_TEST__");

		verify(mockRanges).addNewByName(eq("__PTM_TEST__"), eq("$'O''Brien'.$A$1"),
				any(CellAddress.class), eq(0));
	}

	@Test
	void schreibeSheetMetadaten_sheetIndexWirdInCellAddressGesetzt() throws Exception {
		when(mockRanges.hasByName("__PTM_TEST__")).thenReturn(false);
		var capturedAddr = new CellAddress[1];
		org.mockito.stubbing.Answer<?> captureAddr = invocation -> {
			capturedAddr[0] = (CellAddress) invocation.getArgument(2);
			return null;
		};
		org.mockito.Mockito.doAnswer(captureAddr)
				.when(mockRanges).addNewByName(any(), any(), any(CellAddress.class), anyInt());

		SheetMetadataHelper.schreibeSheetMetadaten(mockRanges, "Blatt", 5, "__PTM_TEST__");

		assertThat(capturedAddr[0].Sheet).isEqualTo((short) 5);
	}

	@Test
	void schreibeSheetMetadaten_fehlerWirdNichtPropagiert() {
		when(mockRanges.hasByName(any())).thenThrow(new RuntimeException("UNO-Fehler"));

		// kein throw erwartet
		SheetMetadataHelper.schreibeSheetMetadaten(mockRanges, "Test", 0, "__PTM_TEST__");
	}

	// ── findeSheet ───────────────────────────────────────────────────────────

	@Test
	void findeSheet_findetKorrekteSheet() throws Exception {
		var rangeObj = new Object();
		when(mockRanges.hasByName("__PTM_SCHWEIZER_RANGLISTE__")).thenReturn(true);
		when(mockRanges.getByName("__PTM_SCHWEIZER_RANGLISTE__")).thenReturn(rangeObj);

		Optional<XSpreadsheet> result = SheetMetadataHelper.findeSheet(
				mockRanges,
				obj -> 3,                // Named Range zeigt auf Sheet-Index 3
				idx -> idx == 3 ? mockSheet : null,
				"__PTM_SCHWEIZER_RANGLISTE__");

		assertThat(result).contains(mockSheet);
	}

	@Test
	void findeSheet_gibtLeerZurueckWennSchluesselNichtVorhanden() {
		when(mockRanges.hasByName("__PTM_SCHWEIZER_RANGLISTE__")).thenReturn(false);

		Optional<XSpreadsheet> result = SheetMetadataHelper.findeSheet(
				mockRanges,
				obj -> 0,
				idx -> mockSheet,
				"__PTM_SCHWEIZER_RANGLISTE__");

		assertThat(result).isEmpty();
	}

	@Test
	void findeSheet_gibtLeerZurueckWennNamedRangesNull() {
		Optional<XSpreadsheet> result = SheetMetadataHelper.findeSheet(
				null,
				obj -> 0,
				idx -> mockSheet,
				"__PTM_TEST__");

		assertThat(result).isEmpty();
	}

	@Test
	void findeSheet_gibtLeerZurueckWennSheetIndexNegativ() throws Exception {
		when(mockRanges.hasByName("__PTM_TEST__")).thenReturn(true);
		when(mockRanges.getByName("__PTM_TEST__")).thenReturn(new Object());

		Optional<XSpreadsheet> result = SheetMetadataHelper.findeSheet(
				mockRanges,
				obj -> -1,   // Named Range nicht auflösbar
				idx -> mockSheet,
				"__PTM_TEST__");

		assertThat(result).isEmpty();
	}

	@Test
	void findeSheet_fehlerWirdNichtPropagiert() {
		when(mockRanges.hasByName("__PTM_TEST__")).thenThrow(new RuntimeException("UNO-Fehler"));

		Optional<XSpreadsheet> result = SheetMetadataHelper.findeSheet(
				mockRanges, obj -> 0, idx -> mockSheet, "__PTM_TEST__");

		assertThat(result).isEmpty();
	}

	// ── istRegistriertesSheet ────────────────────────────────────────────────

	@Test
	void istRegistriertesSheet_trueWennSheetIndexUebereinstimmt() throws Exception {
		var rangeObj = new Object();
		when(mockRanges.hasByName("__PTM_SCHWEIZER_RANGLISTE__")).thenReturn(true);
		when(mockRanges.getByName("__PTM_SCHWEIZER_RANGLISTE__")).thenReturn(rangeObj);

		boolean result = SheetMetadataHelper.istRegistriertesSheet(
				mockRanges,
				obj -> 4,   // Named Range zeigt auf Index 4
				4,           // Ziel-Sheet hat Index 4
				"__PTM_SCHWEIZER_RANGLISTE__");

		assertThat(result).isTrue();
	}

	@Test
	void istRegistriertesSheet_falseWennAnderesSheetIndex() throws Exception {
		var rangeObj = new Object();
		when(mockRanges.hasByName("__PTM_SCHWEIZER_RANGLISTE__")).thenReturn(true);
		when(mockRanges.getByName("__PTM_SCHWEIZER_RANGLISTE__")).thenReturn(rangeObj);

		boolean result = SheetMetadataHelper.istRegistriertesSheet(
				mockRanges,
				obj -> 2,   // Named Range zeigt auf Index 2
				4,           // Ziel-Sheet hat Index 4
				"__PTM_SCHWEIZER_RANGLISTE__");

		assertThat(result).isFalse();
	}

	@Test
	void istRegistriertesSheet_falseWennSchluesselFehlt() {
		when(mockRanges.hasByName("__PTM_SCHWEIZER_RANGLISTE__")).thenReturn(false);

		boolean result = SheetMetadataHelper.istRegistriertesSheet(
				mockRanges, obj -> 0, 0, "__PTM_SCHWEIZER_RANGLISTE__");

		assertThat(result).isFalse();
	}

	// ── findeSpieltagNr ──────────────────────────────────────────────────────

	@Test
	void findeSpieltagNr_findetKorrekteNummer() throws Exception {
		var rangeObj = new Object();
		when(mockRanges.getElementNames()).thenReturn(new String[]{"__PTM_SPIELTAG_5__"});
		when(mockRanges.getByName("__PTM_SPIELTAG_5__")).thenReturn(rangeObj);

		Optional<SpielTagNr> result = SheetMetadataHelper.findeSpieltagNr(
				mockRanges,
				obj -> 7,   // Named Range zeigt auf Index 7
				7);          // Ziel-Sheet hat Index 7

		assertThat(result).isPresent();
		assertThat(result.get().getNr()).isEqualTo(5);
	}

	@Test
	void findeSpieltagNr_gibtLeerZurueckWennKeinMatch() throws Exception {
		var rangeObj = new Object();
		when(mockRanges.getElementNames()).thenReturn(new String[]{"__PTM_SPIELTAG_3__"});
		when(mockRanges.getByName("__PTM_SPIELTAG_3__")).thenReturn(rangeObj);

		Optional<SpielTagNr> result = SheetMetadataHelper.findeSpieltagNr(
				mockRanges,
				obj -> 2,   // Named Range zeigt auf Index 2
				7);          // Ziel-Sheet hat anderen Index

		assertThat(result).isEmpty();
	}

	@Test
	void findeSpieltagNr_ignoriertNichtPassendeSchluessel() throws Exception {
		when(mockRanges.getElementNames()).thenReturn(
				new String[]{"__PTM_SCHWEIZER_RANGLISTE__", "__PTM_SPIELTAG_2__"});
		when(mockRanges.getByName("__PTM_SPIELTAG_2__")).thenReturn(new Object());
		when(mockRanges.getByName("__PTM_SCHWEIZER_RANGLISTE__")).thenReturn(new Object());

		Optional<SpielTagNr> result = SheetMetadataHelper.findeSpieltagNr(
				mockRanges,
				obj -> 1,
				1);

		// Nur __PTM_SPIELTAG_2__ matcht – SCHWEIZER_RANGLISTE wird ignoriert
		assertThat(result).isPresent();
		assertThat(result.get().getNr()).isEqualTo(2);
	}

	// ── Builder-Methoden ─────────────────────────────────────────────────────

	@Test
	void schluesselSpieltagRangliste_korrekteFormatierung() {
		assertThat(SheetMetadataHelper.schluesselSpieltagRangliste(3))
				.isEqualTo("__PTM_SPIELTAG_3__");
	}

	@Test
	void schluesselSchweizerSpielrunde_korrekteFormatierung() {
		assertThat(SheetMetadataHelper.schluesselSchweizerSpielrunde(2))
				.isEqualTo("__PTM_SCHWEIZER_SPIELRUNDE_2__");
	}

	@Test
	void schluesselMaastrichterVorrunde_korrekteFormatierung() {
		assertThat(SheetMetadataHelper.schluesselMaastrichterVorrunde(1))
				.isEqualTo("__PTM_MAASTRICHTER_VORRUNDE_1__");
	}

	@Test
	void schluesselMaastrichterFinalrunde_korrekteFormatierung() {
		assertThat(SheetMetadataHelper.schluesselMaastrichterFinalrunde("A"))
				.isEqualTo("__PTM_MAASTRICHTER_FINALRUNDE_A__");
	}

	@Test
	void schluesselSupermeleeSpielrunde_korrekteFormatierung() {
		assertThat(SheetMetadataHelper.schluesselSupermeleeSpielrunde(2, 3))
				.isEqualTo("__PTM_SUPERMELEE_SPIELRUNDE_2_3__");
	}

	@Test
	void schluesselKoTurnierbaum_korrekteFormatierungEinzel() {
		assertThat(SheetMetadataHelper.schluesselKoTurnierbaum(""))
				.isEqualTo("__PTM_KO_TURNIERBAUM___");
	}

	@Test
	void schluesselKoTurnierbaum_korrekteFormatierungGruppe() {
		assertThat(SheetMetadataHelper.schluesselKoTurnierbaum("A"))
				.isEqualTo("__PTM_KO_TURNIERBAUM_A__");
	}
}
