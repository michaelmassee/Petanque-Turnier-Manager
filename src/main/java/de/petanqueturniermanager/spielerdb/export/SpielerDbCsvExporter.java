package de.petanqueturniermanager.spielerdb.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.spielerdb.LabelDatensatz;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.VereinDatensatz;

/**
 * Schreibt pro Entity eine eigene CSV-Datei in das Zielverzeichnis. UTF-8 mit
 * BOM (für Excel/Calc-Kompatibilität) und Semikolon-Separator (DE-Locale).
 *
 * <p>Spaltenschema (stabil; Re-Import ist auf diese Header angewiesen):
 * <ul>
 *   <li>{@code spieler.csv}: nr;vorname;nachname;vereinNr;vereinName;lizenznr</li>
 *   <li>{@code vereine.csv}: nr;name</li>
 *   <li>{@code labels.csv}: nr;name</li>
 *   <li>{@code spieler_labels.csv}: spielerNr;labelNr</li>
 * </ul>
 */
public final class SpielerDbCsvExporter implements SpielerDbExporter {

    private static final char BOM = '﻿';
    private static final char SEP = ';';
    private static final String NL = "\r\n";

    @Override
    public void export(SpielerDbExportData data, ExportRequest request) throws SpielerDbException {
        Path zielVerzeichnis = request.target();
        try {
            Files.createDirectories(zielVerzeichnis);
        } catch (IOException e) {
            throw new SpielerDbException("CSV-Zielverzeichnis nicht anlegbar: " + zielVerzeichnis, e);
        }
        EnumSet<ExportEntity> entities = request.entities();
        try {
            if (entities.contains(ExportEntity.SPIELER)) {
                schreibeSpieler(data, zielVerzeichnis.resolve("spieler.csv"));
            }
            if (entities.contains(ExportEntity.VEREINE)) {
                schreibeVereine(data, zielVerzeichnis.resolve("vereine.csv"));
            }
            if (entities.contains(ExportEntity.LABELS)) {
                schreibeLabels(data, zielVerzeichnis.resolve("labels.csv"));
            }
            if (entities.contains(ExportEntity.SPIELER) && entities.contains(ExportEntity.LABELS)) {
                schreibeJunction(data, zielVerzeichnis.resolve("spieler_labels.csv"));
            }
        } catch (IOException e) {
            throw new SpielerDbException("CSV-Export fehlgeschlagen: " + zielVerzeichnis, e);
        }
    }

    private static void schreibeSpieler(SpielerDbExportData data, Path datei) throws IOException {
        try (BufferedWriter w = oeffne(datei)) {
            schreibeKopf(w, "nr", "vorname", "nachname", "vereinNr", "vereinName", "lizenznr");
            for (SpielerMitVerein s : data.spieler()) {
                schreibeZeile(w,
                        Integer.toString(s.nr()),
                        s.vorname(),
                        s.nachname(),
                        formatNullableInt(s.vereinNr()),
                        s.vereinName() == null ? "" : s.vereinName(),
                        s.lizenznr() == null ? "" : s.lizenznr());
            }
        }
    }

    private static void schreibeVereine(SpielerDbExportData data, Path datei) throws IOException {
        try (BufferedWriter w = oeffne(datei)) {
            schreibeKopf(w, "nr", "name");
            for (VereinDatensatz v : data.vereine()) {
                schreibeZeile(w, formatNullableInt(v.nr()), v.name());
            }
        }
    }

    private static void schreibeLabels(SpielerDbExportData data, Path datei) throws IOException {
        try (BufferedWriter w = oeffne(datei)) {
            schreibeKopf(w, "nr", "name");
            for (LabelDatensatz l : data.labels()) {
                schreibeZeile(w, formatNullableInt(l.nr()), l.name());
            }
        }
    }

    private static void schreibeJunction(SpielerDbExportData data, Path datei) throws IOException {
        try (BufferedWriter w = oeffne(datei)) {
            schreibeKopf(w, "spielerNr", "labelNr");
            for (SpielerLabelZuordnung z : data.spielerLabels()) {
                schreibeZeile(w, Integer.toString(z.spielerNr()), Integer.toString(z.labelNr()));
            }
        }
    }

    private static BufferedWriter oeffne(Path datei) throws IOException {
        Writer w = Files.newBufferedWriter(datei, StandardCharsets.UTF_8);
        // BOM zuerst — sonst erkennt Excel UTF-8 nicht und stellt Umlaute falsch dar.
        w.write(BOM);
        return new BufferedWriter(w);
    }

    private static void schreibeKopf(BufferedWriter w, String... spalten) throws IOException {
        schreibeZeile(w, spalten);
    }

    private static void schreibeZeile(BufferedWriter w, String... werte) throws IOException {
        for (int i = 0; i < werte.length; i++) {
            if (i > 0) {
                w.write(SEP);
            }
            w.write(maskiere(werte[i]));
        }
        w.write(NL);
    }

    private static String maskiere(String wert) {
        boolean braucheQuotes = wert.indexOf(SEP) >= 0
                || wert.indexOf('"') >= 0
                || wert.indexOf('\n') >= 0
                || wert.indexOf('\r') >= 0;
        if (!braucheQuotes) {
            return wert;
        }
        return "\"" + wert.replace("\"", "\"\"") + "\"";
    }

    private static String formatNullableInt(@Nullable Integer wert) {
        return wert == null ? "" : Integer.toString(wert);
    }
}
