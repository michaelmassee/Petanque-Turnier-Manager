package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontSlant;
import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XPrintAreas;
import com.sun.star.sheet.XSheetCellCursor;
import com.sun.star.sheet.XSheetCellRange;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XUsedAreaCursor;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.table.XColumnRowRange;
import com.sun.star.table.XTableColumns;
import com.sun.star.table.XTableRows;
import com.sun.star.text.XText;

import de.petanqueturniermanager.helper.Lo;

/**
 * Mappt ein {@link XSpreadsheet} auf ein {@link TabelleModel}.
 * <p>
 * Verwendet den definierten Druckbereich des Sheets; Fallback: Used-Area.
 * <p>
 * Merge-Logik: Master-Zellen erhalten colspan/rowspan via {@code createCursorByRange} +
 * {@code collapseToMergedArea}. Slave-Positionen werden im Gitter als {@code null} markiert.
 * <p>
 * <strong>Muss im SheetRunner-Thread oder UNO-Event-Thread aufgerufen werden.</strong>
 */
public class TabellenMapper {

    private static final Logger logger = LogManager.getLogger(TabellenMapper.class);

    private static final String SOLID  = " solid ";
    private static final String DOUBLE = " double ";

    /**
     * Mappt das übergebene Sheet vollständig in ein {@link TabelleModel}.
     *
     * @param sheet anzuzeigendes Sheet
     * @return TabelleModel mit Grid, Zellen, Spaltenbreiten und Zeilenhöhen
     */
    public TabelleModel map(XSpreadsheet sheet) {
        try {
            var bereich = ermittleDruckbereich(sheet);
            if (bereich == null) {
                return leeresModell();
            }
            return mapBereich(sheet, bereich);
        } catch (Exception e) {
            logger.error("Fehler beim Mappen des Sheets auf TabelleModel", e);
            return leeresModell();
        }
    }

    private TabelleModel mapBereich(XSpreadsheet sheet, CellRangeAddress bereich) {
        int numZeilen = bereich.EndRow - bereich.StartRow + 1;
        int numSpalten = bereich.EndColumn - bereich.StartColumn + 1;

        // Gitter: null = Merge-Slave (kein <td>), "" = noch nicht verarbeitet
        var gitterRaw = new String[numZeilen][numSpalten];
        for (var zeile : gitterRaw) {
            java.util.Arrays.fill(zeile, "");
        }

        Map<String, ZelleModel> zellenMap = new LinkedHashMap<>();
        XCellRange cellRange = Lo.qi(XCellRange.class, sheet);

        for (int r = 0; r < numZeilen; r++) {
            for (int c = 0; c < numSpalten; c++) {
                if (gitterRaw[r][c] == null) {
                    continue; // Merge-Slave – von einer Master-Zelle bereits markiert
                }

                int absRow = bereich.StartRow + r;
                int absCol = bereich.StartColumn + c;

                try {
                    XCell cell = cellRange.getCellByPosition(absCol, absRow);
                    XPropertySet props = Lo.qi(XPropertySet.class, cell);

                    int colspan = 1;
                    int rowspan = 1;

                    // Merge-Erkennung via Cursor: zuverlässiger als IsMerged-Property
                    try {
                        var einzelZelle = Lo.qi(XSheetCellRange.class,
                                sheet.getCellRangeByPosition(absCol, absRow, absCol, absRow));
                        var mergeKursor = sheet.createCursorByRange(einzelZelle);
                        mergeKursor.collapseToMergedArea();
                        var mergeAdresse = Lo.qi(XCellRangeAddressable.class, mergeKursor).getRangeAddress();
                        boolean istMaster = mergeAdresse.StartRow == absRow
                                && mergeAdresse.StartColumn == absCol
                                && (mergeAdresse.EndRow > absRow || mergeAdresse.EndColumn > absCol);
                        if (istMaster) {
                            colspan = mergeAdresse.EndColumn - absCol + 1;
                            rowspan = mergeAdresse.EndRow - absRow + 1;
                            markiereSlaves(gitterRaw, r, c, rowspan, colspan, numZeilen, numSpalten);
                        }
                    } catch (Exception e) {
                        logger.debug("Merge-Erkennung für Zelle [{},{}]: {}", r, c, e.getMessage());
                    }

                    String id = TabelleModel.zelleId(r, c);
                    gitterRaw[r][c] = id;

                    XText cellText = Lo.qi(XText.class, cell);
                    String wert = (cellText != null) ? cellText.getString() : "";

                    zellenMap.put(id, new ZelleModel(id, wert, extrahiereStil(props, colspan, rowspan)));

                } catch (Exception e) {
                    logger.debug("Fehler beim Mappen von Zelle [{},{}]: {}", r, c, e.getMessage());
                    gitterRaw[r][c] = TabelleModel.zelleId(r, c);
                }
            }
        }

        return new TabelleModel(
                numZeilen, numSpalten,
                rohGitterZuListe(gitterRaw, numZeilen, numSpalten),
                zellenMap,
                ermittleSpaltenBreiten(sheet, bereich, numSpalten),
                ermittleZeilenHoehen(sheet, bereich, numZeilen),
                bereich.StartRow,
                bereich.StartColumn);
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

        try {
            // Fettschrift
            Object fontWeightObj = props.getPropertyValue("CharWeight");
            fett = fontWeightObj instanceof Float fw && fw >= FontWeight.BOLD;

            // Kursiv
            Object fontSlantObj = props.getPropertyValue("CharPosture");
            kursiv = fontSlantObj instanceof FontSlant slant && slant == FontSlant.ITALIC;

            // Hintergrundfarbe (-1 = automatisch)
            Object bgObj = props.getPropertyValue("CellBackColor");
            if (bgObj instanceof Integer bg && bg != -1) {
                hintergrundfarbe = String.format("#%06X", 0xFFFFFF & bg);
            }

            // Schriftfarbe (-1 = automatisch)
            Object ccObj = props.getPropertyValue("CharColor");
            if (ccObj instanceof Integer cc && cc != -1) {
                schriftfarbe = String.format("#%06X", 0xFFFFFF & cc);
            }

            // Horizontale Ausrichtung (CellHoriJustify ist kein Java-Enum → equals-Vergleich)
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

            // Vertikale Ausrichtung (CellVertJustify2-Konstante als int)
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

            // Zeilenumbruch
            Object wrapObj = props.getPropertyValue("IsTextWrapped");
            zeilenumbruch = Boolean.TRUE.equals(wrapObj);

            // Schriftart
            Object fontNameObj = props.getPropertyValue("CharFontName");
            if (fontNameObj instanceof String fn && !fn.isBlank()) {
                schriftart = fn;
            }

            // Schriftgröße (CharHeight in Punkt)
            Object heightObj = props.getPropertyValue("CharHeight");
            if (heightObj instanceof Number n) {
                schriftgroesse = n.floatValue();
            }

            // Rahmenlinien (TableBorder2)
            Object borderObj = props.getPropertyValue("TableBorder2");
            if (borderObj instanceof com.sun.star.table.TableBorder2 tb) {
                linienOben   = linienZuCss(tb.TopLine,    tb.IsTopLineValid);
                linienUnten  = linienZuCss(tb.BottomLine, tb.IsBottomLineValid);
                linienLinks  = linienZuCss(tb.LeftLine,   tb.IsLeftLineValid);
                linienRechts = linienZuCss(tb.RightLine,  tb.IsRightLineValid);
            }

        } catch (Exception e) {
            logger.debug("Fehler beim Extrahieren des Zell-Stils: {}", e.getMessage());
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
                try {
                    XPropertySet colProps = Lo.qi(XPropertySet.class,
                            columns.getByIndex(bereich.StartColumn + c));
                    Object brObj = colProps.getPropertyValue("Width");
                    if (brObj instanceof Integer br) {
                        breiten.put(c, br);
                    }
                } catch (Exception e) {
                    logger.debug("Spaltenbreite für Spalte {} nicht ermittelbar", c);
                }
            }
        } catch (Exception e) {
            logger.debug("Spaltenbreiten nicht ermittelbar: {}", e.getMessage());
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
                    logger.debug("Zeilenhöhe für Zeile {} nicht ermittelbar", r);
                }
            }
        } catch (Exception e) {
            logger.debug("Zeilenhöhen nicht ermittelbar: {}", e.getMessage());
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
            logger.debug("Druckbereich nicht ermittelbar, verwende Used Area: {}", e.getMessage());
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

    private List<List<String>> rohGitterZuListe(String[][] gitterRaw, int numZeilen, int numSpalten) {
        List<List<String>> gitter = new ArrayList<>(numZeilen);
        for (int r = 0; r < numZeilen; r++) {
            List<String> zeile = new ArrayList<>(numSpalten);
            for (int c = 0; c < numSpalten; c++) {
                zeile.add(gitterRaw[r][c]); // null für Slaves ist gewollt
            }
            gitter.add(Collections.unmodifiableList(zeile));
        }
        return gitter;
    }

    private static TabelleModel leeresModell() {
        return new TabelleModel(0, 0, List.of(), Map.of(), Map.of(), Map.of(), 0, 0);
    }
}
