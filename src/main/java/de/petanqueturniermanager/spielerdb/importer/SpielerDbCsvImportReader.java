package de.petanqueturniermanager.spielerdb.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import de.petanqueturniermanager.spielerdb.SpielerDbCsvFormat;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpieler;

/**
 * Liest die flache, single-file Spieler-DB-CSV — symmetrisch zum
 * {@link de.petanqueturniermanager.spielerdb.export.SpielerDbCsvExporter}.
 *
 * <p>Akzeptiert UTF-8 mit oder ohne BOM, beliebiges Zeilenende ({@code \n},
 * {@code \r\n}, {@code \r}), Separator {@code ;} und RFC-4180-Quoting (über
 * opencsv).
 *
 * <p>Erwartete Datei-Struktur:
 * <ol>
 *   <li>Optional: Format-Marker-Zeile {@code # PTM-SpielerDB-CSV;version=N}.
 *       Wenn vorhanden, wird die Version geprüft — neuere Versionen führen
 *       zu einem klaren Fehler ("Plugin aktualisieren?"). Fehlt der Marker,
 *       wird tolerant weitergelesen — Header-Validierung greift dann.</li>
 *   <li>Header {@code vorname;nachname;verein;lizenznr}.</li>
 *   <li>Daten-Zeilen.</li>
 * </ol>
 *
 * <p>Pro Daten-Zeile wird ein {@link RohSpieler} erzeugt; alle Werte werden
 * vor der Übergabe an den Validator normalisiert (NFC, Whitespace-Collapse,
 * {@code strip()}). Leere {@code verein}- und {@code lizenznr}-Felder werden
 * zu {@code null} (statt Leer-String). Vereine werden implizit aus
 * {@code RohSpieler.vereinName} abgeleitet — die {@code vereine}-Liste in
 * {@link ImportRohdaten} bleibt leer.
 *
 * <p>{@code request.source()} muss eine Datei sein.
 */
public final class SpielerDbCsvImportReader implements SpielerDbImportReader {

    private static final char BOM = '﻿';

    private static final int IDX_VORNAME = 0;
    private static final int IDX_NACHNAME = 1;
    private static final int IDX_VEREIN = 2;
    private static final int IDX_LIZENZ = 3;

    @Override
    public ImportRohdaten read(ImportRequest request) throws SpielerDbException {
        Path datei = request.source();
        if (!Files.isRegularFile(datei)) {
            throw new SpielerDbException("CSV-Quelle ist keine Datei: " + datei);
        }

        try (Reader r = Files.newBufferedReader(datei, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(r)) {
            int aktuelleZeile = 0;
            String erste = br.readLine();
            aktuelleZeile++;
            if (erste == null) {
                throw new SpielerDbException("CSV-Datei ist leer: " + datei);
            }
            erste = entferneBom(erste);

            String headerZeile;
            if (SpielerDbCsvFormat.istMarkerZeile(erste)) {
                pruefeVersion(datei, erste);
                headerZeile = br.readLine();
                aktuelleZeile++;
            } else {
                // Marker fehlt — tolerant weiterlesen.
                headerZeile = erste;
            }
            if (headerZeile == null) {
                throw new SpielerDbException("CSV-Datei enthält keinen Header: " + datei);
            }

            // Header und Daten-Zeilen über opencsv parsen, damit Quoting konsistent
            // mit dem Exporter behandelt wird (auch im Header — falls jemand mal
            // einen Header mit Sonderzeichen erfindet).
            try (CSVReader csv = new CSVReaderBuilder(new java.io.StringReader(headerZeile))
                    .withCSVParser(new CSVParserBuilder()
                            .withSeparator(SpielerDbCsvFormat.SEPARATOR).build())
                    .build()) {
                String[] header = csv.readNext();
                if (header == null) {
                    throw new SpielerDbException("CSV-Datei enthält keinen Header: " + datei);
                }
                validiereHeader(datei, header);
            } catch (CsvException e) {
                throw new SpielerDbException("CSV-Header nicht parsebar: " + datei, e);
            }

            List<RohSpieler> spieler = leseSpielerZeilen(datei, br, aktuelleZeile);
            return new ImportRohdaten(spieler, List.of(), List.of(), List.of());
        } catch (IOException e) {
            throw new SpielerDbException("CSV-Lesen fehlgeschlagen: " + datei, e);
        }
    }

    private static List<RohSpieler> leseSpielerZeilen(Path datei, BufferedReader br,
            int bereitsGeleseneZeilen) throws SpielerDbException {
        try (CSVReader csv = new CSVReaderBuilder(br)
                .withCSVParser(new CSVParserBuilder()
                        .withSeparator(SpielerDbCsvFormat.SEPARATOR).build())
                .build()) {
            List<RohSpieler> erg = new ArrayList<>();
            int datenZeileInDatei = bereitsGeleseneZeilen;
            String[] zeile;
            while ((zeile = csv.readNext()) != null) {
                datenZeileInDatei++;
                if (zeile.length == 0 || (zeile.length == 1 && zeile[0].isEmpty())) {
                    continue; // Leerzeile überspringen
                }
                String vorname = normalisiere(spalte(zeile, IDX_VORNAME));
                String nachname = normalisiere(spalte(zeile, IDX_NACHNAME));
                String verein = leerAlsNull(normalisiere(spalte(zeile, IDX_VEREIN)));
                String lizenz = leerAlsNull(normalisiere(spalte(zeile, IDX_LIZENZ)));
                erg.add(new RohSpieler(null, vorname, nachname, null, verein, lizenz,
                        datenZeileInDatei));
            }
            return erg;
        } catch (IOException | CsvException e) {
            throw new SpielerDbException("CSV-Daten nicht parsebar: " + datei, e);
        }
    }

    private static void pruefeVersion(Path datei, String markerZeile) throws SpielerDbException {
        Integer version = SpielerDbCsvFormat.leseVersion(markerZeile);
        if (version == null) {
            throw new SpielerDbException("Format-Marker in " + datei
                    + " enthält keine gültige Versionsangabe: " + markerZeile);
        }
        if (version > SpielerDbCsvFormat.VERSION) {
            throw new SpielerDbException("CSV-Datei " + datei + " ist Version " + version
                    + ", dieses Plugin kennt höchstens Version " + SpielerDbCsvFormat.VERSION
                    + " — Plugin aktualisieren?");
        }
    }

    private static void validiereHeader(Path datei, String[] header) throws SpielerDbException {
        String[] erwartet = SpielerDbCsvFormat.HEADER;
        if (header.length < erwartet.length) {
            throw new SpielerDbException("Header in " + datei + " hat zu wenige Spalten: "
                    + header.length + " < " + erwartet.length);
        }
        for (int i = 0; i < erwartet.length; i++) {
            String gefunden = header[i] == null ? "" : header[i].strip();
            if (i == 0) {
                gefunden = entferneBom(gefunden);
            }
            if (!erwartet[i].equalsIgnoreCase(gefunden)) {
                throw new SpielerDbException("Header-Spalte " + (i + 1) + " in " + datei
                        + " erwartet '" + erwartet[i] + "', gefunden '" + gefunden + "'");
            }
        }
    }

    private static String entferneBom(String s) {
        if (!s.isEmpty() && s.charAt(0) == BOM) {
            return s.substring(1);
        }
        return s;
    }

    private static String spalte(String[] zeile, int idx) {
        return idx < zeile.length ? (zeile[idx] == null ? "" : zeile[idx]) : "";
    }

    /**
     * NFC-Normalisierung + Whitespace-Collapse + {@code strip()}. Macht aus
     * "BC  Linden " → "BC Linden", aus "" → "".
     */
    private static String normalisiere(String s) {
        if (s.isEmpty()) {
            return "";
        }
        String nfc = Normalizer.normalize(s, Normalizer.Form.NFC);
        return nfc.replaceAll("\\s+", " ").strip();
    }

    @Nullable
    private static String leerAlsNull(String s) {
        return s.isEmpty() ? null : s;
    }
}
