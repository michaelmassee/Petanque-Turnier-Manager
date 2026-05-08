package de.petanqueturniermanager.spielerdb.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import de.petanqueturniermanager.spielerdb.SpielerDbCsvFormat;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Schreibt die Spieler-Stammdaten in eine flache, single-file CSV mit den
 * Spalten {@code vorname;nachname;verein;lizenznr} — ohne IDs. Vereine sind
 * über den Namen denormalisiert in der Spielerzeile enthalten; Labels sind
 * im Format nicht abgedeckt (für Roundtrips inkl. Labels stehen JSON,
 * Calc-ODS und SQLite-Backup zur Verfügung).
 *
 * <p>Datei-Aufbau:
 * <ol>
 *   <li>UTF-8 BOM (Excel-Kompatibilität)</li>
 *   <li>Format-Marker: {@code # PTM-SpielerDB-CSV;version=1}</li>
 *   <li>Header: {@code vorname;nachname;verein;lizenznr}</li>
 *   <li>Daten-Zeilen, sortiert nach Nachname → Vorname → Verein
 *       (locale-stabiler {@link Collator}, case-insensitiv)</li>
 * </ol>
 *
 * <p>Quoting/Escape erfolgt strikt über opencsv ({@link CSVWriter}) mit
 * Separator {@code ;} und Quote {@code "} — Felder mit Sonderzeichen
 * werden gequotet, eingebettete Anführungszeichen verdoppelt.
 *
 * <p>{@code request.target()} muss eine Datei sein (Endung {@code .csv}).
 * {@link ExportEntity#VEREINE} und {@link ExportEntity#LABELS} im Scope
 * werden ignoriert — die flache CSV deckt nur Spieler+Verein ab.
 */
public final class SpielerDbCsvExporter implements SpielerDbExporter {

    private static final char BOM = '﻿';

    @Override
    public void export(SpielerDbExportData data, ExportRequest request) throws SpielerDbException {
        if (!request.entities().contains(ExportEntity.SPIELER)) {
            throw new SpielerDbException(
                    "Flache CSV exportiert Spieler+Verein — Spieler-Entity muss im Scope sein");
        }
        Path zielDatei = request.target();
        Path elternVerzeichnis = zielDatei.toAbsolutePath().getParent();
        if (elternVerzeichnis != null) {
            try {
                Files.createDirectories(elternVerzeichnis);
            } catch (IOException e) {
                throw new SpielerDbException("CSV-Zielverzeichnis nicht anlegbar: "
                        + elternVerzeichnis, e);
            }
        }

        List<SpielerMitVerein> sortiert = sortiere(data.spieler());

        try (BufferedWriter writer = oeffne(zielDatei);
                ICSVWriter csv = new CSVWriterBuilder(writer)
                        .withSeparator(SpielerDbCsvFormat.SEPARATOR)
                        .withLineEnd(System.lineSeparator())
                        .build()) {
            writer.write(SpielerDbCsvFormat.formatMarkerZeile() + System.lineSeparator());
            csv.writeNext(SpielerDbCsvFormat.HEADER, false);
            for (SpielerMitVerein s : sortiert) {
                csv.writeNext(zuZeile(s), false);
            }
        } catch (IOException e) {
            throw new SpielerDbException("CSV-Export fehlgeschlagen: " + zielDatei, e);
        }
    }

    private static List<SpielerMitVerein> sortiere(List<SpielerMitVerein> spieler) {
        // Collator für stabile, locale-konsistente Reihenfolge (Umlaute richtig).
        Collator collator = Collator.getInstance(Locale.ROOT);
        collator.setStrength(Collator.SECONDARY); // case-insensitiv
        Comparator<String> stringComparator = Comparator.nullsFirst(collator::compare);
        Comparator<SpielerMitVerein> cmp = Comparator
                .<SpielerMitVerein, String>comparing(SpielerMitVerein::nachname, stringComparator)
                .thenComparing(SpielerMitVerein::vorname, stringComparator)
                .thenComparing(SpielerMitVerein::vereinName, stringComparator);
        List<SpielerMitVerein> kopie = new ArrayList<>(spieler);
        kopie.sort(cmp);
        return kopie;
    }

    private static String[] zuZeile(SpielerMitVerein s) {
        return new String[] {
                s.vorname(),
                s.nachname(),
                s.vereinName() == null ? "" : s.vereinName(),
                s.lizenznr() == null ? "" : s.lizenznr()
        };
    }

    private static BufferedWriter oeffne(Path datei) throws IOException {
        Writer w = Files.newBufferedWriter(datei, StandardCharsets.UTF_8);
        // BOM zuerst — sonst erkennt Excel UTF-8 nicht und stellt Umlaute falsch dar.
        w.write(BOM);
        return new BufferedWriter(w);
    }
}
