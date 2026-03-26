package de.petanqueturniermanager.webserver;

import java.util.Optional;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Löst ein Sheet über einen exakten {@link SheetMetadataHelper}-Schlüssel auf.
 * Sprachunabhängig und überlebt Umbenennung des Sheets.
 */
public class MetadatenSheetResolver implements SheetResolver {

    private final String metadatenSchluessel;
    private final String anzeigeName;

    public MetadatenSheetResolver(String metadatenSchluessel, String anzeigeName) {
        this.metadatenSchluessel = metadatenSchluessel;
        this.anzeigeName = anzeigeName;
    }

    @Override
    public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return Optional.empty();
        }
        return SheetMetadataHelper.findeSheet(doc, metadatenSchluessel);
    }

    @Override
    public String getAnzeigeName() {
        return anzeigeName;
    }

    @Override
    public Optional<Integer> getNummer(XSpreadsheet sheet) {
        return Optional.empty();
    }
}
