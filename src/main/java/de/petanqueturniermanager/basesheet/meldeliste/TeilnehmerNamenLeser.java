package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Liest aus einer Meldeliste die Spielernamen und (optional) den freien Teamnamen pro Team-Nr.
 * Block-Lese-Helper für die Teilnehmer-Sheets aller Turniersysteme.
 *
 * Erwartet folgendes Spalten-Layout in der Meldeliste:
 * <ul>
 * <li>Spalte 0: Team-Nr</li>
 * <li>Spalte 1: Teamname (wenn {@code teamnameAktiv}, sonst Vorname Spieler 1)</li>
 * <li>Danach: Vorname/Nachname (+ optional Vereinsname) pro Spieler</li>
 * </ul>
 */
public final class TeilnehmerNamenLeser {

    public record TeilnehmerNamen(Map<Integer, String> spielerNamen, Map<Integer, String> teamnamen) {
    }

    private static final int LESE_BIS_ZEILE_OFFSET = 999;

    private final ISheet sheet;
    private final int ersteDatenZeile;
    private final int anzSpieler;
    private final boolean teamnameAktiv;
    private final boolean vereinsnameAktiv;

    private TeilnehmerNamenLeser(ISheet sheet, int ersteDatenZeile, int anzSpieler,
            boolean teamnameAktiv, boolean vereinsnameAktiv) {
        this.sheet = checkNotNull(sheet);
        this.ersteDatenZeile = ersteDatenZeile;
        this.anzSpieler = anzSpieler;
        this.teamnameAktiv = teamnameAktiv;
        this.vereinsnameAktiv = vereinsnameAktiv;
    }

    public static TeilnehmerNamenLeser from(ISheet meldelisteSheet, int ersteDatenZeile,
            Formation formation, boolean teamnameAktiv, boolean vereinsnameAktiv) {
        return new TeilnehmerNamenLeser(meldelisteSheet, ersteDatenZeile, formation.getAnzSpieler(),
                teamnameAktiv, vereinsnameAktiv);
    }

    public TeilnehmerNamen lesen() throws GenerateException {
        Map<Integer, String> spielerNamen = new HashMap<>();
        Map<Integer, String> teamnamen = new HashMap<>();

        XSpreadsheet xSheet = sheet.getXSpreadSheet();
        if (xSheet == null) {
            return new TeilnehmerNamen(spielerNamen, teamnamen);
        }

        int ersterSpielerOffset = teamnameAktiv ? 2 : 1;
        int spaltenProSpieler = vereinsnameAktiv ? 3 : 2;
        int maxSpalte = ersterSpielerOffset + anzSpieler * spaltenProSpieler - 1;

        var xDoc = sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        RangeData data = RangeHelper.from(xSheet, xDoc,
                RangePosition.from(0, ersteDatenZeile, maxSpalte, ersteDatenZeile + LESE_BIS_ZEILE_OFFSET))
                .getDataFromRange();

        for (RowData row : data) {
            if (row.isEmpty()) {
                break;
            }
            int nr = row.get(0).getIntVal(0);
            if (nr <= 0) {
                break;
            }
            spielerNamen.put(nr, bauSpielerNamenZusammen(row, ersterSpielerOffset, spaltenProSpieler));
            if (teamnameAktiv && row.size() > 1) {
                String teamname = row.get(1).getStringVal();
                teamnamen.put(nr, teamname != null ? teamname : "");
            }
        }
        return new TeilnehmerNamen(spielerNamen, teamnamen);
    }

    private String bauSpielerNamenZusammen(RowData row, int ersterSpielerOffset, int spaltenProSpieler) {
        StringBuilder sb = new StringBuilder();
        for (int s = 0; s < anzSpieler; s++) {
            int vorSpalte = ersterSpielerOffset + s * spaltenProSpieler;
            int nachSpalte = vorSpalte + 1;
            String vorname = vorSpalte < row.size() ? row.get(vorSpalte).getStringVal() : "";
            String nachname = nachSpalte < row.size() ? row.get(nachSpalte).getStringVal() : "";
            String spielerName = baueSpielerName(vorname, nachname);
            if (!spielerName.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" / ");
                }
                sb.append(spielerName);
            }
        }
        return sb.toString();
    }

    private static String baueSpielerName(String vorname, String nachname) {
        String vn = vorname != null ? vorname.trim() : "";
        String nn = nachname != null ? nachname.trim() : "";
        if (vn.isEmpty() && nn.isEmpty()) {
            return "";
        }
        if (vn.isEmpty()) {
            return nn;
        }
        if (nn.isEmpty()) {
            return vn;
        }
        return vn + " " + nn;
    }
}
