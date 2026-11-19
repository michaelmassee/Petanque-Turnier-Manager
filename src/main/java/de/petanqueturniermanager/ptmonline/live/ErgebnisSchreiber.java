/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import org.jspecify.annotations.Nullable;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Trägt ein von der Turnierleitung übernommenes Ergebnis in die beiden Ergebniszellen einer Partie ein, wie es die
 * Turnierleitung von Hand täte. Im Turnierbaum rückt der Sieger dann über die Formeln des Baums vor. Nur im
 * Dokument-Kontext eines SheetRunners mit Blattschutz-Scope.
 */
public final class ErgebnisSchreiber {

    /** Ausgang von {@link #schreibeWennLeer}. */
    public enum DirektEintrag {
        /** Beide Ergebniszellen waren leer, das Ergebnis steht jetzt in der Spielrunde. */
        GESCHRIEBEN,
        /** In der Spielrunde stand bereits genau dieses Ergebnis; nichts geschrieben. */
        BEREITS_GLEICH,
        /** In der Spielrunde steht (zumindest teilweise) ein anderes Ergebnis; nichts geschrieben. */
        ABWEICHEND,
        /** Das Blatt der Partie existiert nicht mehr; nichts geschrieben. */
        BLATT_FEHLT;

        /** Die Meldung gilt als übernommen. */
        public boolean istUebernommen() {
            return this == GESCHRIEBEN || this == BEREITS_GLEICH;
        }
    }

    private ErgebnisSchreiber() {}

    /**
     * Trägt das Ergebnis nur ein, wenn die Partie noch keines hat. Die Turnierleitung tippt parallel Ergebnisse ein;
     * deshalb werden beide Ergebniszellen unmittelbar vor dem Schreiben neu gelesen und eine Eingabe der
     * Turnierleitung nie überschrieben.
     *
     * @param vorAenderung öffnet den Blattschutz-Scope, bevor tatsächlich geschrieben wird
     */
    public static DirektEintrag schreibeWennLeer(WorkingSpreadsheet ws, ErgebnisOrt ort, int punkteA, int punkteB,
            Runnable vorAenderung) throws GenerateException {
        XSpreadsheet blatt = LiveBlattLeser.blatt(ws, ort.blattSchluessel(), ort.blattName());
        if (blatt == null) {
            return DirektEintrag.BLATT_FEHLT;
        }
        DirektEintrag vorher = bewerteZellen(ws, blatt, ort, punkteA, punkteB);
        if (vorher != DirektEintrag.GESCHRIEBEN) {
            return vorher;
        }
        // Das Öffnen des Blattschutz-Scopes dauert; eine in dieser Zeit getippte Eingabe hat Vorrang.
        vorAenderung.run();
        DirektEintrag eintrag = bewerteZellen(ws, blatt, ort, punkteA, punkteB);
        if (eintrag == DirektEintrag.GESCHRIEBEN) {
            schreibeInZellen(ws, blatt, ort, punkteA, punkteB);
        }
        return eintrag;
    }

    private static DirektEintrag bewerteZellen(WorkingSpreadsheet ws, XSpreadsheet blatt, ErgebnisOrt ort,
            int punkteA, int punkteB) {
        return bewerte(text(ws, blatt, ort.punkteA()), text(ws, blatt, ort.punkteB()), punkteA, punkteB);
    }

    /**
     * UNO-frei: Was mit einer Meldung geschieht, wenn in den Ergebniszellen die angegebenen Texte stehen.
     *
     * @param textA Inhalt der linken Ergebniszelle, {@code null} wenn leer
     * @param textB wie {@code textA} für die rechte Ergebniszelle
     * @return {@link DirektEintrag#GESCHRIEBEN}, wenn beide Zellen leer sind und geschrieben werden soll
     */
    static DirektEintrag bewerte(@Nullable String textA, @Nullable String textB, int punkteA, int punkteB) {
        if (textA == null && textB == null) {
            return DirektEintrag.GESCHRIEBEN;
        }
        if (String.valueOf(punkteA).equals(textA) && String.valueOf(punkteB).equals(textB)) {
            return DirektEintrag.BEREITS_GLEICH;
        }
        return DirektEintrag.ABWEICHEND;
    }

    private static @Nullable String text(WorkingSpreadsheet ws, XSpreadsheet blatt, Position zelle) {
        return LiveZellbereich.lese(ws, blatt, zelle.getSpalte(), zelle.getZeile(), zelle.getSpalte(),
                zelle.getZeile()).text(zelle.getSpalte(), zelle.getZeile());
    }

    /** @return {@code false}, wenn das Blatt der Partie nicht mehr existiert */
    public static boolean schreibe(WorkingSpreadsheet ws, ErgebnisOrt ort, int punkteA, int punkteB)
            throws GenerateException {
        XSpreadsheet blatt = LiveBlattLeser.blatt(ws, ort.blattSchluessel(), ort.blattName());
        if (blatt == null) {
            return false;
        }
        schreibeInZellen(ws, blatt, ort, punkteA, punkteB);
        return true;
    }

    private static void schreibeInZellen(WorkingSpreadsheet ws, XSpreadsheet blatt, ErgebnisOrt ort, int punkteA,
            int punkteB) throws GenerateException {
        Position a = ort.punkteA();
        Position b = ort.punkteB();
        if (a.getZeile() == b.getZeile() && b.getSpalte() == a.getSpalte() + 1) {
            schreibe(ws, blatt, a, punkteA, punkteB);
        } else {
            schreibe(ws, blatt, a, punkteA);
            schreibe(ws, blatt, b, punkteB);
        }
    }

    /** Ein Block ab {@code start}, die Punkte nebeneinander. */
    private static void schreibe(WorkingSpreadsheet ws, XSpreadsheet blatt, Position start, int... punkte)
            throws GenerateException {
        RangeData ergebnis = new RangeData();
        RowData zeile = ergebnis.addNewRow();
        for (int wert : punkte) {
            zeile.newInt(wert);
        }
        RangeHelper.from(blatt, ws.getWorkingSpreadsheetDocument(), RangePosition.from(start.getSpalte(),
                start.getZeile(), start.getSpalte() + punkte.length - 1, start.getZeile())).setDataInRange(ergebnis);
    }
}
