package de.petanqueturniermanager.helper.sheetsync;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Generischer, systemübergreifender Formatierungs-Runner für Spielplan- und
 * Spielrunden-Sheets. Wird ausschließlich vom
 * {@link SpielplanFormatiererActivationListener} beim Tab-Wechsel gestartet.
 * <p>
 * Aufgaben:
 * <ol>
 *   <li>Zebra-Hintergrundfarben direkt schreiben (kein ConditionalFormat →
 *       HTML-Export-Kompatibilität).</li>
 *   <li>Bedingte Formatierung editierbarer Felder prüfen und setzen, wenn
 *       sie fehlt (z.B. nach Blattschutz-Toggle oder LO-Reload).</li>
 * </ol>
 * Die System-spezifische Konfiguration (Ranges, Farben, Blattschutz) liefert
 * der {@link KonfigSupplier}, der im Factory-Lambda pro System definiert wird.
 */
public final class SpielplanFormatiererSheetRunner extends SheetRunner implements ISheet {

    private static final Logger logger = LogManager.getLogger(SpielplanFormatiererSheetRunner.class);

    /** Liefert die Formatierungs-Konfiguration für das konkrete Sheet. */
    @FunctionalInterface
    public interface KonfigSupplier {
        @Nullable
        SpielplanFormatiererKonfig berechne(ISheet iSheet) throws GenerateException;
    }

    private final XSpreadsheet xSheet;
    private final KonfigSupplier konfigSupplier;

    public SpielplanFormatiererSheetRunner(WorkingSpreadsheet workingSpreadsheet,
            XSpreadsheet xSheet, KonfigSupplier konfigSupplier) {
        super(workingSpreadsheet, TurnierSystem.KEIN);
        this.xSheet = checkNotNull(xSheet);
        this.konfigSupplier = checkNotNull(konfigSupplier);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() {
        return xSheet;
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected void doRun() throws GenerateException {
        SpielplanFormatiererKonfig konfig = konfigSupplier.berechne(this);
        if (konfig == null) return;

        boolean mitBlattschutz = konfig.blattschutzKonfig() != null && TurnierModus.get().istAktiv();
        if (mitBlattschutz) {
            BlattschutzManager.get().beginCommandScope(konfig.blattschutzKonfig(), getWorkingSpreadsheet());
            // SheetRunner.run() ruft endCommandScope() immer im finally – kein manuelles close nötig.
        }

        var ws = getWorkingSpreadsheet();
        var docHelper = new de.petanqueturniermanager.helper.DocumentPropertiesHelper(ws);

        // GenerateException kann nicht direkt durch Runnable propagiert werden →
        // zwischenspeichern und nach dem ohneModifiedFlag-Block weiterwerfen.
        GenerateException[] aufgefangen = {null};
        docHelper.ohneModifiedFlag(() -> {
            try {
                formatiereZebra(konfig);
                if (cfFehlt(konfig)) {
                    setzeEditierbarCF(konfig);
                }
            } catch (GenerateException e) {
                aufgefangen[0] = e;
            }
        });
        if (aufgefangen[0] != null) {
            throw aufgefangen[0];
        }
    }

    private void formatiereZebra(SpielplanFormatiererKonfig konfig) throws GenerateException {
        SheetHelper.faerbeZeilenAbwechselnd(this, konfig.datenRange(),
                konfig.geradeFarbe(), konfig.ungeradeFarbe());
    }

    private boolean cfFehlt(SpielplanFormatiererKonfig konfig) {
        List<RangePosition> editierbar = konfig.editierbareRanges();
        if (editierbar.isEmpty()) return false;
        RangePosition erste = editierbar.get(0);
        try {
            var cell = xSheet.getCellByPosition(erste.getStartSpalte(), erste.getStartZeile());
            XSheetConditionalEntries cf = Lo.qi(XSheetConditionalEntries.class,
                    Lo.qi(XPropertySet.class, cell).getPropertyValue("ConditionalFormat"));
            return cf == null || cf.getCount() == 0;
        } catch (Exception e) {
            logger.warn("CF-Check fehlgeschlagen – überspringe CF-Repair", e);
            return false;
        }
    }

    private void setzeEditierbarCF(SpielplanFormatiererKonfig konfig) throws GenerateException {
        for (RangePosition range : konfig.editierbareRanges()) {
            EditierbaresZelleFormatHelper.anwenden(this, range);
        }
    }

    // ── SheetRunner-Pflichtmethode ────────────────────────────────────────────

    @Override
    @org.jspecify.annotations.Nullable
    protected IKonfigurationSheet getKonfigurationSheet() {
        // Kein KonfigurationSheet nötig – Formatierer erhält seine Konfiguration über den KonfigSupplier.
        return null;
    }

    // ── ISheet-Pflichtmethoden (von SheetRunner nicht bereitgestellt) ─────────

    @Override
    public XComponentContext getxContext() {
        return getWorkingSpreadsheet().getxContext();
    }

    @Override
    public WorkingSpreadsheet getWorkingSpreadsheet() {
        return super.getWorkingSpreadsheet();
    }

    @Override
    public void processBoxinfo(String i18nKey, Object... args) {
        super.processBoxinfo(i18nKey, args);
    }
}
