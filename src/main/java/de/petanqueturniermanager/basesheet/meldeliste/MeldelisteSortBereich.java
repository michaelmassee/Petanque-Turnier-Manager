/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Sortier- und Leerbereiche einer Meldeliste müssen die ausgeblendete Spalte mit der lokalen PTM-Online-ID (UUID)
 * einschließen. Sie steht rechts neben den Datenspalten (hinter Aktiv bzw. dem letzten Spieltag); bleibt sie beim
 * Sortieren stehen, hängt jede UUID danach an einer fremden Meldung und damit an einer fremden Online-Anmeldung.
 */
public final class MeldelisteSortBereich {

    /** So viele Spalten rechts vom Bereich werden nach der UUID-Überschrift durchsucht. */
    private static final int MAX_SUCHBREITE = 30;

    private MeldelisteSortBereich() {}

    /**
     * @param bereich     Datenbereich der Meldeliste (ab Nr-Spalte)
     * @param headerZeile Zeile mit der UUID-Überschrift (die Zeile direkt über den Daten)
     * @return {@code bereich}, rechts bis einschließlich der UUID-Spalte erweitert, sofern die Meldeliste eine hat
     */
    public static RangePosition mitLokalerUuidSpalte(ISheet sheet, RangePosition bereich, int headerZeile)
            throws GenerateException {
        if (headerZeile < 0) {
            return bereich; // ohne Überschriftenzeile gibt es auch keine UUID-Spalte
        }
        int ersteSuchSpalte = bereich.getEndeSpalte() + 1;
        RangeData kopfzeile = RangeHelper.from(sheet, RangePosition.from(ersteSuchSpalte, headerZeile,
                ersteSuchSpalte + MAX_SUCHBREITE - 1, headerZeile)).getDataFromRange();
        if (kopfzeile.isEmpty()) {
            return bereich;
        }
        String header = I18n.get("ptmonline.meldeliste.header.lokaleuuid");
        RowData zellen = kopfzeile.getFirst();
        for (int i = 0; i < zellen.size(); i++) {
            if (header.equals(StringUtils.strip(zellen.get(i).getStringVal()))) {
                return RangePosition.from(bereich.getStartSpalte(), bereich.getStartZeile(), ersteSuchSpalte + i,
                        bereich.getEndeZeile());
            }
        }
        return bereich;
    }
}
