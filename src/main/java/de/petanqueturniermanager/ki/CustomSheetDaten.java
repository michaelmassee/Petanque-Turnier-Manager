/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record CustomSheetDaten(String name, List<List<String>> rows) {

    static final int MAX_ROWS = 200;
    static final int MAX_COLS = 20;

    CustomSheetDaten {
        name = normalisiereName(name);
        rows = begrenze(rows == null ? List.of() : rows);
    }

    @SuppressWarnings("unchecked")
    static CustomSheetDaten from(KiAktion aktion) {
        Map<String, Object> params = aktion.parameters();
        String name = KiActionRegistry.stringParam(params, "name", aktion.target());
        Object rawRows = params.get("rows");
        List<List<String>> rows = new ArrayList<>();
        if (rawRows instanceof List<?> list) {
            for (Object row : list) {
                if (row instanceof List<?> cells) {
                    rows.add(cells.stream().map(c -> c == null ? "" : String.valueOf(c)).toList());
                }
            }
        }
        if (rows.isEmpty()) {
            rows = List.of(List.of(name));
        }
        return new CustomSheetDaten(name, rows);
    }

    private static String normalisiereName(String roh) {
        String name = roh == null || roh.isBlank() ? "KI Zusatz" : roh.trim();
        name = name.replaceAll("[\\\\/*?:\\[\\]]", " ").replaceAll("\\s+", " ").trim();
        if (name.length() > 31) {
            name = name.substring(0, 31).trim();
        }
        return name.isBlank() ? "KI Zusatz" : name;
    }

    private static List<List<String>> begrenze(List<List<String>> rows) {
        List<List<String>> begrenzt = new ArrayList<>();
        for (List<String> row : rows.stream().limit(MAX_ROWS).toList()) {
            List<String> neueZeile = row.stream()
                    .limit(MAX_COLS)
                    .map(CustomSheetDaten::sichereZelle)
                    .toList();
            begrenzt.add(neueZeile);
        }
        return List.copyOf(begrenzt);
    }

    private static String sichereZelle(String wert) {
        if (wert == null) {
            return "";
        }
        String trimmed = wert.length() > 500 ? wert.substring(0, 500) : wert;
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            return "'" + trimmed;
        }
        return trimmed;
    }
}
