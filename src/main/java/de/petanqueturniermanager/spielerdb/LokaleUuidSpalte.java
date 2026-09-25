/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.spielerdb;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Blockweiser Zugriff auf die ausgeblendete Spalte der lokalen PTM-Online-IDs: ein Lesezugriff für alle
 * angefragten Zeilen und höchstens ein Schreibzugriff statt eines UNO-Aufrufs je Zeile. Den Blattschutz behandelt
 * {@link RangeHelper#setDataInRange} selbst.
 */
final class LokaleUuidSpalte {

    private final XSpreadsheet sheet;
    private final XSpreadsheetDocument doc;
    private final int spalte;

    LokaleUuidSpalte(XSpreadsheet sheet, XSpreadsheetDocument doc, int spalte) {
        this.sheet = sheet;
        this.doc = doc;
        this.spalte = spalte;
    }

    /** Vorhandene UUIDs der Zeilen; fehlende werden erzeugt und gemeinsam geschrieben. */
    Map<Integer, String> getOderErzeuge(Collection<Integer> zeilen1Basiert) throws GenerateException {
        Map<Integer, String> ergebnis = new LinkedHashMap<>();
        if (zeilen1Basiert.isEmpty()) {
            return ergebnis;
        }
        Block block = lese(zeilen1Basiert);
        boolean geaendert = false;
        for (int zeile : zeilen1Basiert) {
            String uuid = block.get(zeile);
            if (uuid.isEmpty()) {
                uuid = UUID.randomUUID().toString();
                block.set(zeile, uuid);
                geaendert = true;
            }
            ergebnis.put(zeile, uuid);
        }
        if (geaendert) {
            block.schreibe();
        }
        return ergebnis;
    }

    /** Setzt zuvor gesicherte UUIDs wieder auf ihre Zeilen, in einem Schreibzugriff. */
    void setze(Map<Integer, String> uuidProZeile) throws GenerateException {
        if (uuidProZeile.isEmpty()) {
            return;
        }
        Block block = lese(uuidProZeile.keySet());
        uuidProZeile.forEach(block::set);
        block.schreibe();
    }

    private Block lese(Collection<Integer> zeilen1Basiert) {
        int ersteZeile = zeilen1Basiert.stream().mapToInt(Integer::intValue).min().orElseThrow() - 1;
        int letzteZeile = zeilen1Basiert.stream().mapToInt(Integer::intValue).max().orElseThrow() - 1;
        RangePosition bereich = RangePosition.from(spalte, ersteZeile, spalte, letzteZeile);
        RangeData daten = RangeHelper.from(sheet, doc, bereich).getDataFromRange();
        String[] werte = new String[letzteZeile - ersteZeile + 1];
        for (int i = 0; i < werte.length; i++) {
            werte[i] = i < daten.size() && !daten.get(i).isEmpty()
                    ? StringUtils.strip(StringUtils.defaultString(daten.get(i).get(0).getStringVal()))
                    : "";
        }
        return new Block(bereich, ersteZeile, werte);
    }

    private final class Block {
        private final RangePosition bereich;
        private final int ersteZeile;
        private final String[] werte;

        private Block(RangePosition bereich, int ersteZeile, String[] werte) {
            this.bereich = bereich;
            this.ersteZeile = ersteZeile;
            this.werte = werte;
        }

        String get(int zeile1Basiert) {
            return werte[zeile1Basiert - 1 - ersteZeile];
        }

        void set(int zeile1Basiert, String uuid) {
            werte[zeile1Basiert - 1 - ersteZeile] = uuid;
        }

        void schreibe() throws GenerateException {
            RangeData daten = new RangeData();
            for (String wert : werte) {
                RowData zeile = daten.addNewRow();
                if (wert.isEmpty()) {
                    zeile.newEmpty();
                } else {
                    zeile.newString(wert);
                }
            }
            RangeHelper.from(sheet, doc, bereich).setDataInRange(daten);
        }
    }
}
