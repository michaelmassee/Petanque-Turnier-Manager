
package de.petanqueturniermanager.helper.sheet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XNamedRange;
import com.sun.star.sheet.XNamedRanges;
import com.sun.star.sheet.XSpreadsheetDocument;

/**
 * Testet die Bereinigung von verwaisten (gelöschten) Sheet-Metadaten.
 */
class SheetMetadataCleanupTest {

    private XSpreadsheetDocument xDocMock;
    private XPropertySet docPropsMock;
    private XNamedRanges namedRangesMock;

    @BeforeEach
    void setUp() throws Exception {
        // Mocking des Dokuments und der Properties, um an die NamedRanges zu kommen
        // Da XSpreadsheetDocument und XPropertySet in UNO oft dasselbe Objekt sind,
        // mocken wir es mit extraInterfaces.
        xDocMock = mock(XSpreadsheetDocument.class, withSettings().extraInterfaces(XPropertySet.class));
        docPropsMock = (XPropertySet) xDocMock;
        namedRangesMock = mock(XNamedRanges.class);

        // Wenn "NamedRanges" abgefragt wird, liefern wir unseren Mock zurück
        when(docPropsMock.getPropertyValue("NamedRanges")).thenReturn(namedRangesMock);
    }

    @Test
    void testGueltigePtmMetadatenBleibenErhalten() throws Exception {
        // Setup: Ein Named Range, der intakt ist (zeigt auf Zelle A1)
        String gueltigerSchluessel = "__PTM_SPIELTAG_1__";
        setupNamedRange(gueltigerSchluessel, "$'1. Spieltag'.$A$1");

        // Act
        SheetMetadataHelper.bereinigeVerwaisteMetadaten(xDocMock);

        // Assert: removeByName darf NICHT aufgerufen werden
        verify(namedRangesMock, never()).removeByName(anyString());
    }

    @Test
    void testVerwaistePtmMetadatenMitRefWerdenGeloescht() throws Exception {
        // Setup: Ein Named Range, dessen Sheet gelöscht wurde (englische LibreOffice Version)
        String kaputterSchluessel = "__PTM_KO_TURNIERBAUM_A__";
        setupNamedRange(kaputterSchluessel, "#REF!.$A$1");

        // Act
        SheetMetadataHelper.bereinigeVerwaisteMetadaten(xDocMock);

        // Assert: removeByName MUSS für diesen Schlüssel aufgerufen werden
        verify(namedRangesMock, times(1)).removeByName(kaputterSchluessel);
    }

    @Test
    void testVerwaistePtmMetadatenMitBezugWerdenGeloescht() throws Exception {
        // Setup: Ein Named Range, dessen Sheet gelöscht wurde (deutsche LibreOffice Version)
        String kaputterSchluessel = "__PTM_SCHWEIZER_RANGLISTE__";
        setupNamedRange(kaputterSchluessel, "#BEZUG!.$A$1");

        // Act
        SheetMetadataHelper.bereinigeVerwaisteMetadaten(xDocMock);

        // Assert: removeByName MUSS für diesen Schlüssel aufgerufen werden
        verify(namedRangesMock, times(1)).removeByName(kaputterSchluessel);
    }

    @Test
    void testVerwaisteNutzerMetadatenWerdenIgnoriert() throws Exception {
        // Setup: Ein Named Range, der NICHT zu unserem Plugin gehört, aber kaputt ist
        String fremderSchluessel = "Meine_Eigene_Berechnung";
        setupNamedRange(fremderSchluessel, "#REF!.$D$4");

        // Act
        SheetMetadataHelper.bereinigeVerwaisteMetadaten(xDocMock);

        // Assert: Das Plugin darf keine fremden kaputten Ranges löschen!
        verify(namedRangesMock, never()).removeByName(anyString());
    }

    @Test
    void testGemischtesSzenario() throws Exception {
        // Setup: Eine Kombination aus allen Fällen
        String ptmGueltig = "__PTM_SPIELTAG_1__";
        String ptmKaputt = "__PTM_SPIELTAG_2__";
        String fremdKaputt = "User_Range";

        XNamedRange rangePtmGueltig = mock(XNamedRange.class);
        when(rangePtmGueltig.getContent()).thenReturn("$'1. Spieltag'.$A$1");

        XNamedRange rangePtmKaputt = mock(XNamedRange.class);
        when(rangePtmKaputt.getContent()).thenReturn("#REF!.$A$1");

        XNamedRange rangeFremdKaputt = mock(XNamedRange.class);
        when(rangeFremdKaputt.getContent()).thenReturn("#BEZUG!.$A$1");

        // Simuliere die Liste der Namen im Dokument
        when(namedRangesMock.getElementNames()).thenReturn(new String[]{ptmGueltig, ptmKaputt, fremdKaputt});

        // Simuliere den Zugriff auf die einzelnen Objekte
        when(namedRangesMock.getByName(ptmGueltig)).thenReturn(rangePtmGueltig);
        when(namedRangesMock.getByName(ptmKaputt)).thenReturn(rangePtmKaputt);
        when(namedRangesMock.getByName(fremdKaputt)).thenReturn(rangeFremdKaputt);

        // Act
        SheetMetadataHelper.bereinigeVerwaisteMetadaten(xDocMock);

        // Assert: Nur exakt der verwaiste PTM-Schlüssel darf gelöscht werden
        verify(namedRangesMock, times(1)).removeByName(ptmKaputt);
        verify(namedRangesMock, never()).removeByName(ptmGueltig);
        verify(namedRangesMock, never()).removeByName(fremdKaputt);
    }

    // --- Hilfsmethode zum sauberen Mocken eines einzelnen Named Ranges ---
    private void setupNamedRange(String name, String content) throws Exception {
        XNamedRange rangeMock = mock(XNamedRange.class);
        when(rangeMock.getContent()).thenReturn(content);

        when(namedRangesMock.getElementNames()).thenReturn(new String[]{name});
        when(namedRangesMock.getByName(name)).thenReturn(rangeMock);
    }
}



