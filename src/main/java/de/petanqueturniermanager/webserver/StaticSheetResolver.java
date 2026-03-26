package de.petanqueturniermanager.webserver;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;

/**
 * Löst ein Sheet über seinen exakten Tab-Namen auf.
 * Fallback wenn kein Metadaten-Schlüssel passt.
 */
public class StaticSheetResolver implements SheetResolver {

    private static final Logger logger = LogManager.getLogger(StaticSheetResolver.class);

    private final String sheetName;

    public StaticSheetResolver(String sheetName) {
        this.sheetName = sheetName;
    }

    @Override
    public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
        try {
            var doc = ws.getWorkingSpreadsheetDocument();
            if (doc == null) {
                return Optional.empty();
            }
            XSpreadsheets sheets = doc.getSheets();
            if (!sheets.hasByName(sheetName)) {
                return Optional.empty();
            }
            return Optional.ofNullable(Lo.qi(XSpreadsheet.class, sheets.getByName(sheetName)));
        } catch (NoSuchElementException | WrappedTargetException e) {
            logger.warn("Sheet '{}' nicht gefunden: {}", sheetName, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Fehler beim Auflösen von Sheet '{}'", sheetName, e);
            return Optional.empty();
        }
    }

    @Override
    public String getAnzeigeName() {
        return sheetName;
    }

    @Override
    public Optional<Integer> getNummer(XSpreadsheet sheet) {
        return Optional.empty();
    }
}
