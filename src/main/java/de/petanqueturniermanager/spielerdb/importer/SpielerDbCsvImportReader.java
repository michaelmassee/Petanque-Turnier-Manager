package de.petanqueturniermanager.spielerdb.importer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.export.ExportEntity;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpieler;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpielerLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohVerein;

/**
 * Liest die vier CSV-Dateien aus einem Ordner — exakt symmetrisch zu
 * {@code SpielerDbCsvExporter}. UTF-8 mit optionalem BOM, Semikolon-Separator,
 * Quote-Handling über opencsv.
 *
 * <p>Pro Scope-Entity wird die jeweilige Datei verlangt; fehlt sie, schlägt
 * der Reader hart fehl (mit klarer Pfadangabe). Header-Validierung erfolgt
 * im Validator — der Reader liefert Roh-Strings.
 */
public final class SpielerDbCsvImportReader implements SpielerDbImportReader {

    private static final char SEP = ';';
    private static final char BOM = '﻿';

    @Override
    public ImportRohdaten read(ImportRequest request) throws SpielerDbException {
        Path verzeichnis = request.source();
        if (!Files.isDirectory(verzeichnis)) {
            throw new SpielerDbException("CSV-Quelle ist kein Verzeichnis: " + verzeichnis);
        }
        EnumSet<ExportEntity> entities = request.entities();

        List<RohSpieler> spieler = entities.contains(ExportEntity.SPIELER)
                ? leseSpieler(verzeichnis.resolve("spieler.csv"))
                : List.of();
        List<RohVerein> vereine = entities.contains(ExportEntity.VEREINE)
                ? leseVereine(verzeichnis.resolve("vereine.csv"))
                : List.of();
        List<RohLabel> labels = entities.contains(ExportEntity.LABELS)
                ? leseLabels(verzeichnis.resolve("labels.csv"))
                : List.of();
        List<RohSpielerLabel> junction =
                entities.contains(ExportEntity.SPIELER) && entities.contains(ExportEntity.LABELS)
                        ? leseJunction(verzeichnis.resolve("spieler_labels.csv"))
                        : List.of();

        return new ImportRohdaten(spieler, vereine, labels, junction);
    }

    private static List<RohSpieler> leseSpieler(Path datei) throws SpielerDbException {
        List<String[]> zeilen = leseAlleZeilen(datei);
        validiereHeader(datei, zeilen, "nr", "vorname", "nachname",
                "vereinNr", "vereinName", "lizenznr");
        List<RohSpieler> erg = new ArrayList<>(zeilen.size() - 1);
        for (int i = 1; i < zeilen.size(); i++) {
            String[] z = zeilen.get(i);
            erg.add(new RohSpieler(
                    parseInteger(spalte(z, 0)),
                    spalte(z, 1),
                    spalte(z, 2),
                    parseInteger(spalte(z, 3)),
                    leerAlsNull(spalte(z, 4)),
                    leerAlsNull(spalte(z, 5))));
        }
        return erg;
    }

    private static List<RohVerein> leseVereine(Path datei) throws SpielerDbException {
        List<String[]> zeilen = leseAlleZeilen(datei);
        validiereHeader(datei, zeilen, "nr", "name");
        List<RohVerein> erg = new ArrayList<>(zeilen.size() - 1);
        for (int i = 1; i < zeilen.size(); i++) {
            String[] z = zeilen.get(i);
            erg.add(new RohVerein(parseInteger(spalte(z, 0)), spalte(z, 1)));
        }
        return erg;
    }

    private static List<RohLabel> leseLabels(Path datei) throws SpielerDbException {
        List<String[]> zeilen = leseAlleZeilen(datei);
        validiereHeader(datei, zeilen, "nr", "name");
        List<RohLabel> erg = new ArrayList<>(zeilen.size() - 1);
        for (int i = 1; i < zeilen.size(); i++) {
            String[] z = zeilen.get(i);
            erg.add(new RohLabel(parseInteger(spalte(z, 0)), spalte(z, 1)));
        }
        return erg;
    }

    private static List<RohSpielerLabel> leseJunction(Path datei) throws SpielerDbException {
        List<String[]> zeilen = leseAlleZeilen(datei);
        validiereHeader(datei, zeilen, "spielerNr", "labelNr");
        List<RohSpielerLabel> erg = new ArrayList<>(zeilen.size() - 1);
        for (int i = 1; i < zeilen.size(); i++) {
            String[] z = zeilen.get(i);
            Integer sNr = parseInteger(spalte(z, 0));
            Integer lNr = parseInteger(spalte(z, 1));
            if (sNr == null || lNr == null) {
                throw new SpielerDbException("Junction-Datei " + datei
                        + " enthält leere NR in Zeile " + (i + 1));
            }
            erg.add(new RohSpielerLabel(sNr, lNr));
        }
        return erg;
    }

    private static List<String[]> leseAlleZeilen(Path datei) throws SpielerDbException {
        if (!Files.isRegularFile(datei)) {
            throw new SpielerDbException("Erwartete CSV-Datei fehlt: " + datei);
        }
        try (Reader r = Files.newBufferedReader(datei, StandardCharsets.UTF_8);
                CSVReader csv = new CSVReaderBuilder(r)
                        .withCSVParser(new CSVParserBuilder().withSeparator(SEP).build())
                        .build()) {
            List<String[]> zeilen = csv.readAll();
            if (!zeilen.isEmpty()) {
                String[] erste = zeilen.get(0);
                if (erste.length > 0 && !erste[0].isEmpty() && erste[0].charAt(0) == BOM) {
                    erste[0] = erste[0].substring(1);
                }
            }
            return zeilen;
        } catch (IOException | CsvException e) {
            throw new SpielerDbException("CSV-Lesen fehlgeschlagen: " + datei, e);
        }
    }

    private static void validiereHeader(Path datei, List<String[]> zeilen, String... erwartet)
            throws SpielerDbException {
        if (zeilen.isEmpty()) {
            throw new SpielerDbException("CSV-Datei ist leer: " + datei);
        }
        String[] kopf = zeilen.get(0);
        if (kopf.length < erwartet.length) {
            throw new SpielerDbException("Header in " + datei + " hat zu wenige Spalten: "
                    + kopf.length + " < " + erwartet.length);
        }
        for (int i = 0; i < erwartet.length; i++) {
            if (!erwartet[i].equals(kopf[i])) {
                throw new SpielerDbException("Header-Spalte " + (i + 1) + " in " + datei
                        + " erwartet '" + erwartet[i] + "', gefunden '" + kopf[i] + "'");
            }
        }
    }

    private static String spalte(String[] zeile, int idx) {
        return idx < zeile.length ? zeile[idx] : "";
    }

    @Nullable
    private static Integer parseInteger(String s) {
        String t = s.strip();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static String leerAlsNull(String s) {
        return s.isEmpty() ? null : s;
    }
}
