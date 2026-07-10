/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;

final class TurnierKontextSammler {

    private TurnierKontextSammler() {
    }

    static String sammle(WorkingSpreadsheet ws, boolean vollstaendig) {
        StringBuilder out = new StringBuilder();
        var props = new DocumentPropertiesHelper(ws);
        out.append("Aktuelles Turniersystem: ")
                .append(props.getTurnierSystemAusDocument().getBezeichnung())
                .append('\n');
        out.append("Aktuelle Runde: ").append(props.getIntProperty("Spielrunde", 0)).append('\n');
        out.append("Aktueller Spieltag: ").append(props.getIntProperty("Spieltag", 0)).append('\n');
        out.append("Vorhandene Sheets: ").append(sheetNamen(ws)).append('\n');
        if (vollstaendig) {
            out.append("Hinweis: Vollkontext ist aktiv. Erzeuge dennoch nur erlaubte PTM-Actions; ")
                    .append("freie Makros oder unbekannte UNO-Befehle sind verboten.\n");
        }
        return out.toString();
    }

    private static String sheetNamen(WorkingSpreadsheet ws) {
        try {
            XSpreadsheets sheets = ws.getWorkingSpreadsheetDocument().getSheets();
            XNameAccess names = Lo.qi(XNameAccess.class, sheets);
            if (names != null) {
                return String.join(", ", names.getElementNames());
            }
            XIndexAccess index = Lo.qi(XIndexAccess.class, sheets);
            if (index == null) {
                return "";
            }
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < index.getCount(); i++) {
                XSpreadsheet sheet = Lo.qi(XSpreadsheet.class, index.getByIndex(i));
                if (sheet != null) {
                    if (out.length() > 0) {
                        out.append(", ");
                    }
                    out.append(de.petanqueturniermanager.helper.sheet.TurnierSheet.from(sheet, ws).getName());
                }
            }
            return out.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
