package de.petanqueturniermanager.helper.sheetsync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.helper.Lo;

/**
 * Stabile, UNO-proxy-unabhängige Kennungen für Dokument+Sheet.
 * <p>
 * UNO liefert für dasselbe logische Sheet über die Zeit unterschiedliche Proxy-Objekte;
 * ein Identitätsvergleich ({@code ==}) ist daher nicht verlässlich. Der Token kombiniert
 * die stabile Dokument-Identität mit dem (ebenfalls stabilen) Sheet-Namen und dient sowohl
 * dem {@link SheetAktivierungsDispatcher} als Dedup-Schlüssel als auch den Handlern als
 * Fokus-Merker.
 */
final class SheetFokusToken {

    private static final Logger logger = LogManager.getLogger(SheetFokusToken.class);

    private SheetFokusToken() {
    }

    /** {@code identityHashCode(xDoc):sheetName} oder {@code null}, wenn Doc/Sheet fehlen. */
    static String von(XSpreadsheetDocument xDoc, XSpreadsheet xSheet) {
        if (xDoc == null || xSheet == null) {
            return null;
        }
        return System.identityHashCode(xDoc) + ":" + sheetName(xSheet);
    }

    /** Eindeutige Trace-Kennung pro Event-Auslösung (Doc-Identität + monotone Startzeit). */
    static String traceId(XSpreadsheetDocument xDoc, long startNs) {
        return (xDoc == null ? "keinDoc" : Integer.toString(System.identityHashCode(xDoc)))
                + ":" + Long.toUnsignedString(startNs);
    }

    /** Sheet-Name für Logging/Token; nie {@code null}. */
    static String sheetName(XSpreadsheet sheet) {
        if (sheet == null) {
            return "<keinSheet>";
        }
        try {
            XNamed xNamed = Lo.qi(XNamed.class, sheet);
            if (xNamed != null && xNamed.getName() != null) {
                return xNamed.getName();
            }
        } catch (RuntimeException e) {
            logger.trace("Sheet-Name nicht ermittelbar", e);
        }
        return "<unbekannt>";
    }
}
