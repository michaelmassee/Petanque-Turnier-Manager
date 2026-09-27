/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Werte und Formeln eines Blattbereichs, je einmal als Block gelesen. Zugriffe erfolgen mit absoluten
 * (0-basierten) Blattkoordinaten.
 * <p>
 * Team-Zellen enthalten je nach Anzeigemodus die Team-Nr oder den Teamnamen. Namen stehen als Formel
 * {@code …MATCH(<nr>;<Meldeliste>…)} im Blatt (siehe {@code MeldeListeHelper.teamNameFormel}); daraus wird die
 * Nummer gelesen. Weitergereichte Namen (z.&nbsp;B. Sieger im Turnierbaum) werden über die im selben Bereich so
 * aufgelösten Namen zugeordnet; mehrdeutige Namen bleiben unaufgelöst.
 */
final class LiveZellbereich {

    private static final Pattern NR_IN_TEAMNAME_FORMEL = Pattern.compile("MATCH\\(\\s*(\\d+)\\s*[;,]",
            Pattern.CASE_INSENSITIVE);

    private final int ersteSpalte;
    private final int ersteZeile;
    private final RangeData werte;
    private final String[][] formeln;
    private @Nullable Map<String, Integer> nrProName;

    private LiveZellbereich(int ersteSpalte, int ersteZeile, RangeData werte, String[][] formeln) {
        this.ersteSpalte = ersteSpalte;
        this.ersteZeile = ersteZeile;
        this.werte = werte;
        this.formeln = formeln;
    }

    static LiveZellbereich lese(WorkingSpreadsheet ws, XSpreadsheet blatt, int ersteSpalte, int ersteZeile,
            int letzteSpalte, int letzteZeile) {
        RangePosition bereich = RangePosition.from(ersteSpalte, ersteZeile, letzteSpalte, letzteZeile);
        RangeData werte = RangeHelper.from(blatt, ws.getWorkingSpreadsheetDocument(), bereich).getDataFromRange();
        String[][] formeln = new SheetHelper(ws).getFormulaArrayFromRange(blatt, bereich);
        return new LiveZellbereich(ersteSpalte, ersteZeile, werte, formeln);
    }

    int ersteZeile() {
        return ersteZeile;
    }

    int letzteZeile() {
        return ersteZeile + werte.size() - 1;
    }

    /** @return Rohwert der Zelle ({@code Double}, {@code String}), {@code null} außerhalb des Bereichs */
    @Nullable
    Object wert(int spalte, int zeile) {
        int z = zeile - ersteZeile;
        int s = spalte - ersteSpalte;
        if (z < 0 || z >= werte.size() || s < 0) {
            return null;
        }
        RowData row = werte.get(z);
        return s < row.size() ? row.get(s).getData() : null;
    }

    /** @return Text der Zelle (ganze Zahlen ohne Nachkommastellen), {@code null} wenn leer */
    @Nullable
    String text(int spalte, int zeile) {
        Object wert = wert(spalte, zeile);
        String text;
        if (wert instanceof Number zahl) {
            text = BigDecimal.valueOf(zahl.doubleValue()).stripTrailingZeros().toPlainString();
        } else {
            text = wert instanceof String s ? s : null;
        }
        return text == null || text.isBlank() ? null : text.strip();
    }

    /** @return positive ganze Zahl der Zelle (auch als Text), sonst {@code 0} */
    int zahl(int spalte, int zeile) {
        Object wert = wert(spalte, zeile);
        if (wert instanceof Number zahl) {
            return Math.max((int) zahl.doubleValue(), 0);
        }
        String text = text(spalte, zeile);
        return istGanzzahl(text) ? Integer.parseInt(text) : 0;
    }

    private static boolean istGanzzahl(@Nullable String text) {
        return text != null && text.length() < 10 && text.chars().allMatch(Character::isDigit);
    }

    /** @return eingetragene Punkte, {@code null} wenn leer oder keine Zahl */
    @Nullable
    Integer punkte(int spalte, int zeile) {
        String text = text(spalte, zeile);
        return istGanzzahl(text) ? zahl(spalte, zeile) : null;
    }

    /** @return Team-Nr der Zelle (Nummer, Teamnamen-Formel oder bekannter Name), sonst {@code 0} */
    int teamNr(int spalte, int zeile) {
        int nr = zahl(spalte, zeile);
        if (nr > 0) {
            return nr;
        }
        int ausFormel = nrAusFormel(formel(spalte, zeile));
        if (ausFormel > 0) {
            return ausFormel;
        }
        String name = text(spalte, zeile);
        return name == null ? 0 : nrProName().getOrDefault(name, 0);
    }

    private @Nullable String formel(int spalte, int zeile) {
        int z = zeile - ersteZeile;
        int s = spalte - ersteSpalte;
        return z >= 0 && z < formeln.length && s >= 0 && s < formeln[z].length ? formeln[z][s] : null;
    }

    private Map<String, Integer> nrProName() {
        if (nrProName == null) {
            Map<String, Integer> zuordnung = new HashMap<>();
            Set<String> mehrdeutig = new HashSet<>();
            for (int z = 0; z < formeln.length; z++) {
                for (int s = 0; s < formeln[z].length; s++) {
                    int nr = nrAusFormel(formeln[z][s]);
                    String name = text(s + ersteSpalte, z + ersteZeile);
                    Integer bisher = nr > 0 && name != null ? zuordnung.putIfAbsent(name, nr) : null;
                    if (bisher != null && bisher != nr) {
                        mehrdeutig.add(name);
                    }
                }
            }
            mehrdeutig.forEach(zuordnung::remove);
            nrProName = zuordnung;
        }
        return nrProName;
    }

    static int nrAusFormel(@Nullable String formel) {
        if (formel == null || !formel.startsWith("=")) {
            return 0;
        }
        Matcher treffer = NR_IN_TEAMNAME_FORMEL.matcher(formel);
        return treffer.find() ? Integer.parseInt(treffer.group(1)) : 0;
    }
}
