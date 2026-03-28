package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontSlant;
import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XHeaderFooterContent;
import com.sun.star.sheet.XPrintAreas;
import com.sun.star.sheet.XSheetCellCursor;
import com.sun.star.sheet.XSheetCellRange;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XUsedAreaCursor;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.table.CellContentType;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCell;
import com.sun.star.table.XColumnRowRange;
import com.sun.star.table.XTableColumns;
import com.sun.star.table.XTableRows;
import com.sun.star.text.XText;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.pagestyle.PageProperties;

/**
 * Mappt ein {@link XSpreadsheet} auf ein {@link TabelleModel}.
 * <p>
 * Verwendet den definierten Druckbereich des Sheets; Fallback: Used-Area.
 * <p>
 * Merge-Logik: Master-Zellen erhalten colspan/rowspan via {@code createCursorByRange} +
 * {@code collapseToMergedArea}. Slave-Positionen werden im Gitter als {@code null} markiert.
 * Merge-Ergebnisse werden gecacht, um wiederholte UNO-Aufrufe zu vermeiden.
 * <p>
 * <strong>Muss im SheetRunner-Thread oder UNO-Event-Thread aufgerufen werden.</strong>
 */
public class TabellenMapper {

    private static final Logger logger = LogManager.getLogger(TabellenMapper.class);

    private static final String SOLID  = " solid ";
    private static final String DOUBLE = " double ";

    /**
     * Mappt das übergebene Sheet vollständig in ein {@link TabelleModel}.
     * <p>
     * Liest zusätzlich Kopf- und Fußzeile aus dem PageStyle des Sheets.
     *
     * @param sheet anzuzeigendes Sheet
     * @param doc   Dokument, aus dem der PageStyle gelesen wird
     * @return TabelleModel mit Grid, Zellen, Spaltenbreiten, Zeilenhöhen und Kopf-/Fußzeile
     */
    public TabelleModel map(XSpreadsheet sheet, XSpreadsheetDocument doc) {
        try {
            var bereich = ermittleDruckbereich(sheet);
            if (bereich == null) {
                return leeresModell();
            }
            return mapBereich(sheet, doc, bereich);
        } catch (Exception e) {
            logger.error("Fehler beim Mappen des Sheets auf TabelleModel", e);
            return leeresModell();
        }
    }

    private TabelleModel mapBereich(XSpreadsheet sheet, XSpreadsheetDocument doc, CellRangeAddress bereich) {
        int numZeilen = bereich.EndRow - bereich.StartRow + 1;
        int numSpalten = bereich.EndColumn - bereich.StartColumn + 1;

        // Gitter: null = Merge-Slave (kein <td>), "" = noch nicht verarbeitet
        var gitterRaw = new String[numZeilen][numSpalten];
        for (var zeile : gitterRaw) {
            Arrays.fill(zeile, "");
        }

        Map<String, ZelleModel> zellenMap = new LinkedHashMap<>();
        Map<Long, CellRangeAddress> mergeCache = new HashMap<>();

        for (int r = 0; r < numZeilen; r++) {
            for (int c = 0; c < numSpalten; c++) {
                if (gitterRaw[r][c] == null) {
                    continue; // Merge-Slave – von einer Master-Zelle bereits markiert
                }

                int absRow = bereich.StartRow + r;
                int absCol = bereich.StartColumn + c;
                long mergeKey = toMergeKey(absRow, absCol);

                try {
                    XCell cell = sheet.getCellByPosition(absCol, absRow);
                    XPropertySet props = Lo.qi(XPropertySet.class, cell);

                    int colspan = 1;
                    int rowspan = 1;

                    // Merge-Erkennung via Cursor: zuverlässiger als IsMerged-Property
                    // Ergebnis wird gecacht, um wiederholte UNO-Aufrufe zu vermeiden
                    CellRangeAddress mergeAdresse = mergeCache.get(mergeKey);
                    if (mergeAdresse == null) {
                        var einzelZelle = Lo.qi(XSheetCellRange.class,
                                sheet.getCellRangeByPosition(absCol, absRow, absCol, absRow));
                        var mergeKursor = sheet.createCursorByRange(einzelZelle);
                        mergeKursor.collapseToMergedArea();
                        mergeAdresse = Lo.qi(XCellRangeAddressable.class, mergeKursor).getRangeAddress();
                        for (int mr = mergeAdresse.StartRow; mr <= mergeAdresse.EndRow; mr++) {
                            for (int mc = mergeAdresse.StartColumn; mc <= mergeAdresse.EndColumn; mc++) {
                                mergeCache.put(toMergeKey(mr, mc), mergeAdresse);
                            }
                        }
                    }

                    boolean istMaster = mergeAdresse.StartRow == absRow
                            && mergeAdresse.StartColumn == absCol
                            && (mergeAdresse.EndRow > absRow || mergeAdresse.EndColumn > absCol);
                    if (istMaster) {
                        colspan = mergeAdresse.EndColumn - absCol + 1;
                        rowspan = mergeAdresse.EndRow - absRow + 1;
                        markiereSlaves(gitterRaw, r, c, rowspan, colspan, numZeilen, numSpalten);
                    }

                    String id = TabelleModel.zelleId(r, c);
                    gitterRaw[r][c] = id;

                    String wert = extrahiereZellwert(cell);

                    zellenMap.put(id, new ZelleModel(id, wert, extrahiereStil(props, colspan, rowspan)));

                } catch (Exception e) {
                    logger.debug("Fehler beim Mappen von Zelle [{},{}]", r, c, e);
                    gitterRaw[r][c] = TabelleModel.zelleId(r, c);
                }
            }
        }

        var kopfFuss = ermittleKopfUndFusszeile(sheet, doc);
        return new TabelleModel(
                numZeilen, numSpalten,
                rohGitterZuListe(gitterRaw, numZeilen),
                zellenMap,
                ermittleSpaltenBreiten(sheet, bereich, numSpalten),
                ermittleZeilenHoehen(sheet, bereich, numZeilen),
                bereich.StartRow,
                bereich.StartColumn,
                ermittleKopfZeilenAnzahl(sheet, bereich),
                kopfFuss[0], kopfFuss[1], kopfFuss[2],
                kopfFuss[3], kopfFuss[4], kopfFuss[5]);
    }

    /**
     * Ermittelt die Anzahl der Kopfzeilen im gerenderten Bereich aus den Wiederholungszeilen
     * des LibreOffice-Druckbereichs ({@code XPrintAreas.getPrintTitleRows()}).
     * <p>
     * Kopfzeilen sind die Zeilen, die beim Drucken auf jeder Seite oben wiederholt werden.
     * Sie beginnen üblicherweise am Anfang des Druckbereichs.
     *
     * @param sheet   das Sheet, aus dem die Wiederholungszeilen gelesen werden
     * @param bereich der Druckbereich, dessen Startzeile als Referenz dient
     * @return Anzahl der Kopfzeilen (0 wenn nicht definiert oder nicht am Bereichsanfang)
     */
    private int ermittleKopfZeilenAnzahl(XSpreadsheet sheet, CellRangeAddress bereich) {
        try {
            XPrintAreas printAreas = Lo.qi(XPrintAreas.class, sheet);
            if (printAreas == null || !printAreas.getPrintTitleRows()) {
                return 0;
            }
            CellRangeAddress titelZeilen = printAreas.getTitleRows();
            if (titelZeilen == null || titelZeilen.StartRow < 0 || titelZeilen.EndRow < titelZeilen.StartRow) {
                return 0;
            }
            // Nur Zeilen, die am Anfang des Druckbereichs liegen, sind Kopfzeilen
            if (titelZeilen.StartRow > bereich.StartRow) {
                return 0;
            }
            int anzahl = titelZeilen.EndRow - bereich.StartRow + 1;
            return Math.max(0, Math.min(anzahl, bereich.EndRow - bereich.StartRow + 1));
        } catch (Exception e) {
            logger.debug("Kopfzeilen-Anzahl nicht ermittelbar", e);
            return 0;
        }
    }

    /**
     * Liest Kopf- und Fußzeile (je links/mitte/rechts) aus dem PageStyle des Sheets.
     * <p>
     * Gibt ein Array der Länge 6 zurück:
     * [kopfLinks, kopfMitte, kopfRechts, fussLinks, fussMitte, fussRechts].
     * Nicht aktivierte Bereiche werden als {@code null} zurückgegeben.
     */
    private String[] ermittleKopfUndFusszeile(XSpreadsheet sheet, XSpreadsheetDocument doc) {
        var ergebnis = new String[6];
        try {
            var sheetProps = Lo.qi(XPropertySet.class, sheet);
            if (sheetProps == null || doc == null) {
                return ergebnis;
            }
            var styleNameObj = sheetProps.getPropertyValue("PageStyle");
            if (!(styleNameObj instanceof String styleName) || styleName.isBlank()) {
                return ergebnis;
            }
            var familienSupplier = Lo.qi(XStyleFamiliesSupplier.class, doc);
            if (familienSupplier == null) {
                return ergebnis;
            }
            XNameAccess familienNA = familienSupplier.getStyleFamilies();
            var pageStylesObj = familienNA.getByName("PageStyles");
            var pageStylesNA = Lo.qi(XNameAccess.class, pageStylesObj);
            if (pageStylesNA == null || !pageStylesNA.hasByName(styleName)) {
                return ergebnis;
            }
            var styleProps = Lo.qi(XPropertySet.class, pageStylesNA.getByName(styleName));
            if (styleProps == null) {
                return ergebnis;
            }
            var headerIsOnObj = styleProps.getPropertyValue(PageProperties.HEADER_IS_ON);
            if (Boolean.TRUE.equals(headerIsOnObj)) {
                var headerProp = styleProps.getPropertyValue(PageProperties.RIGHTPAGE_HEADER_CONTENT);
                var headerContent = Lo.qi(XHeaderFooterContent.class, headerProp);
                if (headerContent != null) {
                    ergebnis[0] = leseText(headerContent.getLeftText());
                    ergebnis[1] = leseText(headerContent.getCenterText());
                    ergebnis[2] = leseText(headerContent.getRightText());
                }
            }
            var footerIsOnObj = styleProps.getPropertyValue(PageProperties.FOOTER_IS_ON);
            if (Boolean.TRUE.equals(footerIsOnObj)) {
                var footerProp = styleProps.getPropertyValue(PageProperties.RIGHTPAGE_FOOTER_CONTENT);
                var footerContent = Lo.qi(XHeaderFooterContent.class, footerProp);
                if (footerContent != null) {
                    ergebnis[3] = leseText(footerContent.getLeftText());
                    ergebnis[4] = leseText(footerContent.getCenterText());
                    ergebnis[5] = leseText(footerContent.getRightText());
                }
            }
        } catch (Exception e) {
            logger.debug("Kopf-/Fußzeile nicht ermittelbar", e);
        }
        return ergebnis;
    }

    private String leseText(com.sun.star.text.XText text) {
        if (text == null) {
            return null;
        }
        var inhalt = text.getString();
        return (inhalt == null || inhalt.isBlank()) ? null : inhalt;
    }

    /**
     * Extrahiert den anzuzeigenden Wert einer Zelle.
     * <p>
     * Textinhalt wird direkt übernommen. Numerische Werte werden als
     * Ganzzahl oder Dezimalzahl formatiert.
     *
     * @param cell zu lesende Zelle
     * @return Zellinhalt als String oder {@code null} bei leerem Inhalt / Fehler
     */
    private String extrahiereZellwert(XCell cell) {
        try {
            CellContentType type = cell.getType();

            if (CellContentType.TEXT.equals(type)) {
                XText txt = Lo.qi(XText.class, cell);
                return (txt != null) ? txt.getString() : "";
            }

            if (CellContentType.VALUE.equals(type)) {
                double wert = cell.getValue();
                if (Double.isNaN(wert) || Double.isInfinite(wert)) {
                    return null;
                }
                if (wert == Math.rint(wert)) {
                    return String.valueOf((long) wert);
                }
                return String.valueOf(wert);
            }

            if (CellContentType.FORMULA.equals(type)) {
                double wert = cell.getValue();
                if (Double.isNaN(wert) || Double.isInfinite(wert)) {
                    return null;
                }
                // getValue() liefert 0 sowohl für numerische 0 als auch für Text-Ergebnisse.
                // getString() unterscheidet: "" bei leerem Textergebnis, "0" bei numerischer 0,
                // und den eigentlichen Text bei allen anderen Textergebnissen (z.B. Spielernamen).
                if (wert == 0.0) {
                    XText txt = Lo.qi(XText.class, cell);
                    String textErgebnis = (txt != null) ? txt.getString() : "";
                    return textErgebnis.isEmpty() ? null : textErgebnis;
                }
                if (wert == Math.rint(wert)) {
                    return String.valueOf((long) wert);
                }
                return String.valueOf(wert);
            }

            return null;

        } catch (Exception e) {
            logger.debug("Zellwert Fehler", e);
            return null;
        }
    }

    /**
     * Markiert alle Slave-Positionen einer Merge-Gruppe als {@code null} im Gitter.
     * Die Master-Position selbst wird nicht berührt.
     */
    private void markiereSlaves(String[][] gitter, int masterR, int masterC,
            int rowspan, int colspan, int numZeilen, int numSpalten) {
        for (int mr = masterR; mr < masterR + rowspan && mr < numZeilen; mr++) {
            for (int mc = masterC; mc < masterC + colspan && mc < numSpalten; mc++) {
                if (mr != masterR || mc != masterC) {
                    gitter[mr][mc] = null; // Slave
                }
            }
        }
    }

    private StyleModel extrahiereStil(XPropertySet props, int colspan, int rowspan) {
        boolean fett = false;
        boolean kursiv = false;
        String hintergrundfarbe = null;
        String schriftfarbe = null;
        String ausrichtung = "left";
        String vertikaleAusrichtung = "top";
        int rotationGrad = 0;
        boolean zeilenumbruch = false;
        String schriftart = null;
        float schriftgroesse = 0f;
        String linienOben = null;
        String linienUnten = null;
        String linienLinks = null;
        String linienRechts = null;

        // Jede Eigenschaftsgruppe wird separat abgefangen, damit ein Fehler in einer Gruppe
        // die anderen nicht abbricht und der Browser maximal viele Stilinfos erhält.

        try {
            // Schrifteigenschaften
            Object fontWeightObj = props.getPropertyValue("CharWeight");
            fett = fontWeightObj instanceof Float fw && fw >= FontWeight.BOLD;

            Object fontSlantObj = props.getPropertyValue("CharPosture");
            kursiv = fontSlantObj instanceof FontSlant slant && slant == FontSlant.ITALIC;

            Object fontNameObj = props.getPropertyValue("CharFontName");
            if (fontNameObj instanceof String fn && !fn.isBlank()) {
                schriftart = fn;
            }

            Object heightObj = props.getPropertyValue("CharHeight");
            if (heightObj instanceof Number n) {
                schriftgroesse = n.floatValue();
            }
        } catch (Exception e) {
            logger.debug("Schrifteigenschaften nicht ermittelbar", e);
        }

        try {
            // Farben (-1 = automatisch/transparent)
            Object bgObj = props.getPropertyValue("CellBackColor");
            if (bgObj instanceof Integer bg && bg != -1) {
                hintergrundfarbe = String.format("#%06X", 0xFFFFFF & bg);
            }

            Object ccObj = props.getPropertyValue("CharColor");
            if (ccObj instanceof Integer cc && cc != -1) {
                schriftfarbe = String.format("#%06X", 0xFFFFFF & cc);
            }
        } catch (Exception e) {
            logger.debug("Farb-Eigenschaften nicht ermittelbar", e);
        }

        try {
            // Ausrichtung und Layout
            // CellHoriJustify ist kein Java-Enum → equals-Vergleich
            Object hJustObj = props.getPropertyValue("HoriJustify");
            if (hJustObj instanceof CellHoriJustify hJust) {
                if (CellHoriJustify.CENTER.equals(hJust)) {
                    ausrichtung = "center";
                } else if (CellHoriJustify.RIGHT.equals(hJust)) {
                    ausrichtung = "right";
                } else if (CellHoriJustify.BLOCK.equals(hJust)) {
                    ausrichtung = "justify";
                } else {
                    ausrichtung = "left";
                }
            }

            // CellVertJustify2-Konstante als int
            Object vJustObj = props.getPropertyValue("VertJustify");
            if (vJustObj instanceof Integer vJust) {
                vertikaleAusrichtung = switch (vJust) {
                    case 2 -> "middle"; // CENTER
                    case 3, 4 -> "bottom"; // BOTTOM, BLOCK
                    default -> "top";
                };
            }

            // Textrotation (1/100 Grad → Grad)
            Object rotObj = props.getPropertyValue("RotateAngle");
            if (rotObj instanceof Integer winkel) {
                rotationGrad = winkel / 100;
            }

            Object wrapObj = props.getPropertyValue(ICommonProperties.IS_TEXT_WRAPPED);
            zeilenumbruch = Boolean.TRUE.equals(wrapObj);
        } catch (Exception e) {
            logger.debug("Ausrichtungs-Eigenschaften nicht ermittelbar", e);
        }

        try {
            // Rahmenlinien (TableBorder2)
            Object borderObj = props.getPropertyValue("TableBorder2");
            if (borderObj instanceof com.sun.star.table.TableBorder2 tb) {
                linienOben   = linienZuCss(tb.TopLine,    tb.IsTopLineValid);
                linienUnten  = linienZuCss(tb.BottomLine, tb.IsBottomLineValid);
                linienLinks  = linienZuCss(tb.LeftLine,   tb.IsLeftLineValid);
                linienRechts = linienZuCss(tb.RightLine,  tb.IsRightLineValid);
            }
        } catch (Exception e) {
            logger.debug("Rahmen-Eigenschaften nicht ermittelbar", e);
        }

        return new StyleModel(fett, kursiv, hintergrundfarbe, schriftfarbe,
                ausrichtung, vertikaleAusrichtung, colspan, rowspan,
                rotationGrad, zeilenumbruch, schriftart, schriftgroesse,
                linienOben, linienUnten, linienLinks, linienRechts);
    }

    private String linienZuCss(com.sun.star.table.BorderLine2 linie, boolean gueltig) {
        if (!gueltig || linie == null || linie.OuterLineWidth == 0) {
            return null;
        }
        String farbe = linie.Color <= 0
                ? "#000000"
                : String.format("#%06X", 0xFFFFFF & linie.Color);

        if (linie.InnerLineWidth > 0
                || linie.LineStyle == com.sun.star.table.BorderLineStyle.DOUBLE) {
            return "3px" + DOUBLE + farbe;
        }
        if (linie.OuterLineWidth >= 75) {
            return "3px" + SOLID + farbe;
        }
        if (linie.OuterLineWidth >= 50) {
            return "2px" + SOLID + farbe;
        }
        return "1px" + SOLID + farbe;
    }

    private Map<Integer, Integer> ermittleSpaltenBreiten(XSpreadsheet sheet,
            CellRangeAddress bereich, int numSpalten) {
        Map<Integer, Integer> breiten = new LinkedHashMap<>();
        try {
            XColumnRowRange colRowRange = Lo.qi(XColumnRowRange.class, sheet);
            XTableColumns columns = colRowRange.getColumns();
            for (int c = 0; c < numSpalten; c++) {
                int absSpalte = bereich.StartColumn + c;
                try {
                    XPropertySet colProps = Lo.qi(XPropertySet.class, columns.getByIndex(absSpalte));
                    Object brObj = colProps.getPropertyValue("Width");
                    if (brObj instanceof Integer br) {
                        breiten.put(c, br);
                    }
                } catch (Exception e) {
                    logger.debug("Spaltenbreite für Spalte {} (abs={}) nicht ermittelbar", c, absSpalte, e);
                }
            }
        } catch (Exception e) {
            logger.debug("Spaltenbreiten nicht ermittelbar", e);
        }
        return breiten;
    }

    private Map<Integer, Integer> ermittleZeilenHoehen(XSpreadsheet sheet,
            CellRangeAddress bereich, int numZeilen) {
        Map<Integer, Integer> hoehen = new LinkedHashMap<>();
        try {
            XColumnRowRange colRowRange = Lo.qi(XColumnRowRange.class, sheet);
            XTableRows rows = colRowRange.getRows();
            for (int r = 0; r < numZeilen; r++) {
                try {
                    XPropertySet rowProps = Lo.qi(XPropertySet.class,
                            rows.getByIndex(bereich.StartRow + r));
                    Object hObj = rowProps.getPropertyValue("Height");
                    if (hObj instanceof Integer h) {
                        hoehen.put(r, h);
                    }
                } catch (Exception e) {
                    logger.debug("Zeilenhöhe für Zeile {} nicht ermittelbar", r, e);
                }
            }
        } catch (Exception e) {
            logger.debug("Zeilenhöhen nicht ermittelbar", e);
        }
        return hoehen;
    }

    private CellRangeAddress ermittleDruckbereich(XSpreadsheet sheet) {
        try {
            XPrintAreas printAreas = Lo.qi(XPrintAreas.class, sheet);
            if (printAreas != null) {
                CellRangeAddress[] bereiche = printAreas.getPrintAreas();
                if (bereiche != null && bereiche.length > 0) {
                    return begrenzungsrahmen(bereiche);
                }
            }
        } catch (Exception e) {
            logger.debug("Druckbereich nicht ermittelbar, verwende Used Area", e);
        }
        return ermittleUsedArea(sheet);
    }

    /**
     * Berechnet die kleinste Bounding Box über alle Druckbereiche.
     */
    private CellRangeAddress begrenzungsrahmen(CellRangeAddress[] bereiche) {
        var box = new CellRangeAddress();
        box.Sheet = bereiche[0].Sheet;
        box.StartColumn = bereiche[0].StartColumn;
        box.StartRow = bereiche[0].StartRow;
        box.EndColumn = bereiche[0].EndColumn;
        box.EndRow = bereiche[0].EndRow;
        for (int i = 1; i < bereiche.length; i++) {
            box.StartColumn = Math.min(box.StartColumn, bereiche[i].StartColumn);
            box.StartRow = Math.min(box.StartRow, bereiche[i].StartRow);
            box.EndColumn = Math.max(box.EndColumn, bereiche[i].EndColumn);
            box.EndRow = Math.max(box.EndRow, bereiche[i].EndRow);
        }
        return box;
    }

    private CellRangeAddress ermittleUsedArea(XSpreadsheet sheet) {
        try {
            XSheetCellCursor cursor = sheet.createCursor();
            var usedCursor = Lo.qi(XUsedAreaCursor.class, cursor);
            usedCursor.gotoStartOfUsedArea(false);
            usedCursor.gotoEndOfUsedArea(true);
            var addrAble = Lo.qi(XCellRangeAddressable.class, cursor);
            return addrAble.getRangeAddress();
        } catch (Exception e) {
            logger.error("Fehler beim Ermitteln des Used-Area", e);
            return null;
        }
    }

    private List<List<String>> rohGitterZuListe(String[][] gitterRaw, int numZeilen) {
        List<List<String>> gitter = new ArrayList<>(numZeilen);
        for (String[] zeile : gitterRaw) {
            // Arrays.asList erlaubt null-Elemente (null = Merge-Slave, gewollt)
            gitter.add(Collections.unmodifiableList(Arrays.asList(zeile)));
        }
        return gitter;
    }

    private long toMergeKey(int r, int c) {
        return (((long) r) << 32) | (c & 0xffffffffL);
    }

    private static TabelleModel leeresModell() {
        return new TabelleModel(0, 0, List.of(), Map.of(), Map.of(), Map.of(), 0, 0, 0,
                null, null, null, null, null, null);
    }
}
