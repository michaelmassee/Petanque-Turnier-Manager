package de.petanqueturniermanager.helper.print;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.Point;
import com.sun.star.awt.Size;
import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.drawing.XDrawPage;
import com.sun.star.drawing.XDrawPageSupplier;
import com.sun.star.drawing.XShape;
import com.sun.star.drawing.XShapes;
import com.sun.star.graphic.XGraphic;
import com.sun.star.graphic.XGraphicProvider;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XPrintAreas;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.uno.AnyConverter;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;

/**
 * Fügt das Turnierlogo als Draw-Shape rechts oben in alle Sheets mit Druckbereich ein.<br>
 * Hinweis: Es wird nur der erste Druckbereich eines Sheets berücksichtigt.
 */
public final class TurnierLogoHelper {

    private static final Logger logger = LogManager.getLogger(TurnierLogoHelper.class);

    static final String SHAPE_NAME = "PTM_TurnierLogo";
    private static final int LOGO_BREITE = 4000; // 40mm in 1/100mm
    private static final int LOGO_HOEHE_FALLBACK = 3000; // 30mm in 1/100mm

    private record MarginData(int effektivRechts, int topMargin) {
    }

    private TurnierLogoHelper() {
        // Utility-Klasse – nicht instanziierbar
    }

    /**
     * Aktualisiert das Turnierlogo in allen Sheets des Dokuments.<br>
     * Sheets ohne Druckbereich werden übersprungen.<br>
     * Bei leerer {@code logoUrl} wird ein vorhandenes Logo-Shape entfernt.
     *
     * @param ws       aktives Arbeitsdokument
     * @param logoUrl  URL zur Logo-Datei, oder leer/null zum Entfernen
     */
    public static void logoInAllenSheetsAktualisieren(WorkingSpreadsheet ws, String logoUrl) {
        try {
            var logoUrlLeer = logoUrl == null || logoUrl.isBlank();
            XGraphic graphic = null;
            if (!logoUrlLeer) {
                graphic = ladeGrafik(ws, logoUrl);
                if (graphic == null) {
                    logger.warn("Turnierlogo konnte nicht geladen werden: {}", logoUrl);
                    return;
                }
            }

            XSpreadsheetDocument doc = ws.getWorkingSpreadsheetDocument();
            XSpreadsheets sheets = doc.getSheets();
            var msf = Lo.qi(XMultiServiceFactory.class, doc);
            Map<String, MarginData> marginCache = new HashMap<>();

            for (String sheetName : sheets.getElementNames()) {
                var sheet = Lo.qi(XSpreadsheet.class, sheets.getByName(sheetName));
                verarbeiteSheet(sheet, logoUrl, logoUrlLeer, graphic, ws, msf, marginCache);
            }
        } catch (java.lang.Exception e) {
            logger.error("Fehler beim Aktualisieren des Turnierlogos", e);
        }
    }

    private static void verarbeiteSheet(XSpreadsheet sheet, String logoUrl, boolean logoUrlLeer,
            XGraphic graphic, WorkingSpreadsheet ws, XMultiServiceFactory msf,
            Map<String, MarginData> marginCache) {
        try {
            var vorhandeneShape = sucheLogoShape(sheet);

            // Early exit: kein Logo konfiguriert und kein Shape vorhanden
            if (logoUrlLeer && vorhandeneShape == null) {
                return;
            }

            // Logo entfernen wenn URL leer
            if (logoUrlLeer) {
                entferneShape(sheet, vorhandeneShape);
                return;
            }

            // Nur Sheets mit Druckbereich erhalten ein Logo
            var printAreas = Lo.qi(XPrintAreas.class, sheet).getPrintAreas();
            if (printAreas.length == 0) {
                if (vorhandeneShape != null) {
                    entferneShape(sheet, vorhandeneShape);
                }
                return;
            }

            // Logo unverändert → nichts tun
            if (vorhandeneShape != null) {
                var vorhandeneProps = Lo.qi(XPropertySet.class, vorhandeneShape);
                if (vorhandeneProps != null && logoUrl.equals(vorhandeneProps.getPropertyValue("Description"))) {
                    return;
                }
                entferneShape(sheet, vorhandeneShape);
            }

            // Nur erster Druckbereich (bewusst – mehrere Druckbereiche werden nicht unterstützt)
            var margins = liesMarginData(sheet, ws, marginCache);
            int rechterRand = summeSpaltenbereiten(sheet, printAreas[0].EndColumn);
            int effektivRechts = Math.min(rechterRand, margins.effektivRechts());
            int logoX = Math.max(0, effektivRechts - LOGO_BREITE);
            int logoY = margins.topMargin() / 2;

            einfuegen(sheet, graphic, logoUrl, logoX, logoY, msf);
        } catch (java.lang.Exception e) {
            logger.error("Fehler beim Verarbeiten des Turnierlogos in Sheet", e);
        }
    }

    private static XGraphic ladeGrafik(WorkingSpreadsheet ws, String logoUrl) {
        try {
            var provider = ws.createInstanceMCF(XGraphicProvider.class, "com.sun.star.graphic.GraphicProvider");
            if (provider == null) {
                logger.error("GraphicProvider nicht verfügbar");
                return null;
            }
            var pv = new PropertyValue();
            pv.Name = "URL";
            pv.Value = logoUrl;
            pv.State = PropertyState.DIRECT_VALUE;
            pv.Handle = -1;
            return provider.queryGraphic(new PropertyValue[] { pv });
        } catch (java.lang.Exception e) {
            logger.error("Grafik konnte nicht geladen werden: {}", logoUrl, e);
            return null;
        }
    }

    private static void einfuegen(XSpreadsheet sheet, XGraphic graphic, String logoUrl,
            int logoX, int logoY, XMultiServiceFactory msf) throws java.lang.Exception {
        var supplier = Lo.qi(XDrawPageSupplier.class, sheet);
        XDrawPage drawPage = supplier.getDrawPage();
        if (drawPage == null) {
            return;
        }

        var shapeObj = msf.createInstance("com.sun.star.drawing.GraphicObjectShape");
        var xShape = Lo.qi(XShape.class, shapeObj);
        Lo.qi(XShapes.class, drawPage).add(xShape);

        var props = Lo.qi(XPropertySet.class, xShape);
        props.setPropertyValue("Graphic", graphic);
        props.setPropertyValue("Name", SHAPE_NAME);
        props.setPropertyValue("Description", logoUrl);

        // Proportionale Skalierung – sicherer Zugriff auf Grafik-Metadaten
        int hoehe = LOGO_HOEHE_FALLBACK;
        var gProps = Lo.qi(XPropertySet.class, graphic);
        if (gProps != null) {
            var info = gProps.getPropertySetInfo();
            if (info != null && info.hasPropertyByName("Size100thMM")) {
                var sizeObj = gProps.getPropertyValue("Size100thMM");
                if (sizeObj instanceof Size originalSize && originalSize.Width > 0) {
                    hoehe = (int) (4000L * originalSize.Height / originalSize.Width);
                }
            }
        }
        xShape.setSize(new Size(LOGO_BREITE, hoehe));
        xShape.setPosition(new Point(logoX, logoY));
    }

    private static XShape sucheLogoShape(XSpreadsheet sheet) {
        try {
            var supplier = Lo.qi(XDrawPageSupplier.class, sheet);
            XDrawPage drawPage = supplier.getDrawPage();
            if (drawPage == null) {
                return null;
            }
            for (int i = 0; i < drawPage.getCount(); i++) {
                var obj = drawPage.getByIndex(i);
                var props = Lo.qi(XPropertySet.class, obj);
                if (props != null) {
                    var nameVal = props.getPropertyValue("Name");
                    if (nameVal instanceof String s && SHAPE_NAME.equals(s)) {
                        return Lo.qi(XShape.class, obj);
                    }
                }
            }
        } catch (java.lang.Exception e) {
            logger.error("Fehler beim Suchen des Logo-Shapes", e);
        }
        return null;
    }

    private static void entferneShape(XSpreadsheet sheet, XShape shape) {
        try {
            var supplier = Lo.qi(XDrawPageSupplier.class, sheet);
            XDrawPage drawPage = supplier.getDrawPage();
            if (drawPage == null) {
                return;
            }
            var xShapes = Lo.qi(XShapes.class, drawPage);
            if (xShapes != null) {
                xShapes.remove(shape);
            }
        } catch (java.lang.Exception e) {
            logger.error("Fehler beim Entfernen des Logo-Shapes", e);
        }
    }

    private static MarginData liesMarginData(XSpreadsheet sheet, WorkingSpreadsheet ws,
            Map<String, MarginData> cache) {
        var fallback = new MarginData(20000, 2000);
        try {
            var sheetProps = Lo.qi(XPropertySet.class, sheet);
            var styleName = (String) sheetProps.getPropertyValue("PageStyle");
            if (cache.containsKey(styleName)) {
                return cache.get(styleName);
            }
            var sup = Lo.qi(XStyleFamiliesSupplier.class, ws.getWorkingSpreadsheetDocument());
            var pageStyles = Lo.qi(XNameAccess.class, sup.getStyleFamilies().getByName("PageStyles"));
            var styleProps = Lo.qi(XPropertySet.class, pageStyles.getByName(styleName));
            int pageWidth = AnyConverter.toInt(styleProps.getPropertyValue("Width"));
            int leftMargin = AnyConverter.toInt(styleProps.getPropertyValue("LeftMargin"));
            int rightMargin = AnyConverter.toInt(styleProps.getPropertyValue("RightMargin"));
            int topMargin = AnyConverter.toInt(styleProps.getPropertyValue("TopMargin"));
            var marginData = new MarginData(pageWidth - leftMargin - rightMargin, topMargin);
            cache.put(styleName, marginData);
            return marginData;
        } catch (java.lang.Exception e) {
            logger.error("Fehler beim Lesen der Seitenränder", e);
            return fallback;
        }
    }

    private static int summeSpaltenbereiten(XSpreadsheet sheet, int lastColumn) {
        try {
            var colRowRange = Lo.qi(com.sun.star.table.XColumnRowRange.class, sheet);
            var columns = colRowRange.getColumns();
            int summe = 0;
            for (int col = 0; col <= lastColumn; col++) {
                var colProps = Lo.qi(XPropertySet.class, columns.getByIndex(col));
                if (colProps != null) {
                    summe += AnyConverter.toInt(colProps.getPropertyValue("Width"));
                }
            }
            return summe;
        } catch (java.lang.Exception e) {
            logger.error("Fehler beim Summieren der Spaltenbreiten", e);
            return 15000; // Fallback: 150mm
        }
    }
}
