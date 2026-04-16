/**
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet.blattschutz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Zentraler Singleton für Sheet-Schutz (Blattschutz) im Turnier-Modus.
 * <p>
 * Orchestriert die korrekte Reihenfolge:
 * <ol>
 *   <li>CellStyles aktualisieren (vor jedem protect – LO-Einschränkung)</li>
 *   <li>Sheet ggf. entsperren (idempotent, verhindert UNO-Exception)</li>
 *   <li>Zellschutz auf editierbaren Bereichen freigeben</li>
 *   <li>Sheet sperren</li>
 * </ol>
 */
public class BlattschutzManager {

    private static final Logger logger = LogManager.getLogger(BlattschutzManager.class);
    private static final BlattschutzManager INSTANCE = new BlattschutzManager();

    private BlattschutzManager() {
    }

    public static BlattschutzManager get() {
        return INSTANCE;
    }

    /**
     * Aktiviert den Blattschutz für alle in der Konfiguration definierten Sheets.
     * <p>
     * Reihenfolge ist zwingend: Styles zuerst → Zellschutz setzen → Sheet sperren.
     *
     * @param konfiguration turnierspezifische Schutz-Konfiguration
     * @param ws            aktuelles Spreadsheet
     */
    public void schuetzen(IBlattschutzKonfiguration konfiguration, WorkingSpreadsheet ws) {
        // Schritt 1: CellStyles sichern BEVOR irgendein Sheet geschützt wird
        konfiguration.zelleStylesAktualisieren(ws);

        // Schritt 2: Schutz-Infos einmalig berechnen
        var infos = konfiguration.berechneSchutzInfos(ws);

        // Schritt 3: Pro Sheet entsperren → Zellschutz → sperren
        for (var info : infos) {
            try {
                entsperreSheetFallsNoetig(info.sheet());
                for (var range : info.editierbareBereich()) {
                    setzeZellSchutzFreigegeben(info.sheet(), range);
                }
                schuetzeSheet(info.sheet());
            } catch (Exception e) {
                logger.warn("Sheet konnte nicht gesperrt werden: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Entfernt den Blattschutz von allen in der Konfiguration definierten Sheets.
     * <p>
     * Cell-Level-Schutz muss nicht zurückgesetzt werden –
     * LibreOffice ignoriert {@code IsLocked} bei ungeschützten Sheets.
     *
     * @param konfiguration turnierspezifische Schutz-Konfiguration
     * @param ws            aktuelles Spreadsheet
     */
    public void entsperren(IBlattschutzKonfiguration konfiguration, WorkingSpreadsheet ws) {
        var infos = konfiguration.berechneSchutzInfos(ws);
        for (var info : infos) {
            try {
                entsperreSheet(info.sheet());
            } catch (Exception e) {
                logger.warn("Sheet konnte nicht entsperrt werden: {}", e.getMessage(), e);
            }
        }
    }

    // -------------------------------------------------------------------------

    private void entsperreSheetFallsNoetig(XSpreadsheet sheet) {
        var xProt = Lo.qi(XProtectable.class, sheet);
        if (xProt.isProtected()) {
            xProt.unprotect("");
        }
    }

    private void schuetzeSheet(XSpreadsheet sheet) {
        Lo.qi(XProtectable.class, sheet).protect("");
    }

    private void entsperreSheet(XSpreadsheet sheet) {
        var xProt = Lo.qi(XProtectable.class, sheet);
        if (xProt.isProtected()) {
            xProt.unprotect("");
        }
    }

    /**
     * Setzt {@code CellProtection.IsLocked = false} auf dem angegebenen Bereich,
     * so dass die Zellen trotz Sheet-Schutz editierbar bleiben.
     * <p>
     * Schreibt bewusst ein neues {@link CellProtection}-Objekt mit allen Flags
     * auf ihren Standardwerten ({@code false}) – außer {@code IsLocked}, das
     * explizit auf {@code false} gesetzt wird. Das Lesen des alten Werts wird
     * vermieden, da {@code getPropertyValue} auf Mehr-Zellen-Bereichen mit
     * ambiguem Inhalt eine Exception werfen kann, die den Schreibvorgang
     * verhindert.
     */
    private void setzeZellSchutzFreigegeben(XSpreadsheet sheet, RangePosition range) {
        try {
            var xRange = sheet.getCellRangeByPosition(
                    range.getStartSpalte(), range.getStartZeile(),
                    range.getEndeSpalte(), range.getEndeZeile());
            var props = Lo.qi(XPropertySet.class, xRange);

            var cp = new CellProtection();
            cp.IsLocked = false;  // Zelle bleibt editierbar trotz Sheet-Schutz
            props.setPropertyValue("CellProtection", cp);

            // Verifikation: LO kann setPropertyValue still ignorieren wenn nLockCount > 0
            var result = (CellProtection) props.getPropertyValue("CellProtection");
            if (result.IsLocked) {
                logger.warn("IsLocked=false konnte nicht gesetzt werden für Bereich {} " +
                        "(LO nLockCount aktiv?) – wird beim nächsten formatDaten() korrigiert.", range);
            }
        } catch (Exception e) {
            logger.error("Zellschutz für Bereich {} konnte nicht gesetzt werden: {}",
                    range, e.getMessage(), e);
        }
    }

}
