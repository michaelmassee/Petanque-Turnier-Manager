package de.petanqueturniermanager.webserver;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Löst ein Sheet über einen Metadaten-Schlüssel-Prefix auf.
 * Findet den Eintrag mit der höchsten Nummer, z.B. {@code __PTM_SCHWEIZER_SPIELRUNDE_3__}
 * bei Prefix {@code __PTM_SCHWEIZER_SPIELRUNDE_}.
 * <p>
 * Die Nummer wird aus dem mittleren Teil des Schlüssels extrahiert:
 * {@code prefix + nummer + suffix}.
 */
public class MetadatenPrefixSheetResolver implements SheetResolver {

    private static final Logger logger = LogManager.getLogger(MetadatenPrefixSheetResolver.class);
    private static final Pattern NUMMER_PATTERN = Pattern.compile("^(\\d+)");

    private final String schluesselPrefix;
    private final String schluesselSuffix;
    private final String anzeigeName;

    /** Zuletzt gefundene Nummer – gecacht für {@link #getNummer(XSpreadsheet)}. */
    private volatile int letzteNummer = -1;

    public MetadatenPrefixSheetResolver(String schluesselPrefix, String schluesselSuffix, String anzeigeName) {
        this.schluesselPrefix = schluesselPrefix;
        this.schluesselSuffix = schluesselSuffix;
        this.anzeigeName = anzeigeName;
    }

    @Override
    public Optional<XSpreadsheet> resolve(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return Optional.empty();
        }
        try {
            String[] schluessel = SheetMetadataHelper.getSchluesselMitPrefix(doc, schluesselPrefix);
            int hoesteNr = -1;
            String hoesteSchluessel = null;
            for (String name : schluessel) {
                if (!name.endsWith(schluesselSuffix)) {
                    continue;
                }
                int nr = extrahiereNummer(name);
                if (nr > hoesteNr) {
                    hoesteNr = nr;
                    hoesteSchluessel = name;
                }
            }
            if (hoesteSchluessel == null) {
                letzteNummer = -1;
                return Optional.empty();
            }
            letzteNummer = hoesteNr;
            return SheetMetadataHelper.findeSheet(doc, hoesteSchluessel);
        } catch (Exception e) {
            logger.error("Fehler beim Auflösen via Prefix '{}'", schluesselPrefix, e);
            return Optional.empty();
        }
    }

    @Override
    public String getAnzeigeName() {
        return anzeigeName;
    }

    @Override
    public Optional<Integer> getNummer(XSpreadsheet sheet) {
        return letzteNummer >= 0 ? Optional.of(letzteNummer) : Optional.empty();
    }

    private int extrahiereNummer(String schluessel) {
        String mittelTeil = schluessel.substring(schluesselPrefix.length(),
                schluessel.length() - schluesselSuffix.length());
        Matcher matcher = NUMMER_PATTERN.matcher(mittelTeil);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.debug("Keine gültige Zahl in Schlüssel '{}'", schluessel);
            }
        }
        return -1;
    }
}
