package de.petanqueturniermanager.spielerdb.importer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpieler;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpielerLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohVerein;
import de.petanqueturniermanager.spielerdb.importer.ValidierteDaten.ValLabel;
import de.petanqueturniermanager.spielerdb.importer.ValidierteDaten.ValSpieler;
import de.petanqueturniermanager.spielerdb.importer.ValidierteDaten.ValSpielerLabel;
import de.petanqueturniermanager.spielerdb.importer.ValidierteDaten.ValVerein;
import de.petanqueturniermanager.spielerdb.matching.SpielerMatchKeyNormalizer;

/**
 * Prüft Roh-Daten vor der Transaktion. Sammelt harte Fehler in einer
 * {@link SpielerDbValidationException} (alle auf einmal — kein Hin-und-Her).
 * Nicht-fatale Probleme (Datei-interne Duplikate, Junction-Refs auf
 * unbekannte NRs) werden zu {@link ImportWarnung}en.
 *
 * <p>Datei-interne Duplikate werden Last-Wins zusammengeführt — der letzte
 * Datensatz mit demselben Schlüssel überschreibt frühere.
 */
public final class SpielerDbImportValidator {

    public ValidierteDaten validiere(ImportRohdaten roh) throws SpielerDbException {
        List<String> fehler = new ArrayList<>();
        List<ImportWarnung> warnungen = new ArrayList<>();

        List<ValVerein> vereine = validiereVereine(roh.vereine(), fehler, warnungen);
        List<ValLabel> labels = validiereLabels(roh.labels(), fehler, warnungen);
        List<ValSpieler> spieler = validiereSpieler(roh.spieler(), fehler, warnungen);
        List<ValSpielerLabel> junction = validiereJunction(roh, warnungen);

        if (!fehler.isEmpty()) {
            throw new SpielerDbValidationException(fehler);
        }
        return new ValidierteDaten(spieler, vereine, labels, junction, warnungen);
    }

    private static List<ValVerein> validiereVereine(List<RohVerein> roh, List<String> fehler,
            List<ImportWarnung> warnungen) {
        LinkedHashMap<String, ValVerein> dedup = new LinkedHashMap<>();
        for (int i = 0; i < roh.size(); i++) {
            RohVerein v = roh.get(i);
            String name = v.name() == null ? "" : v.name().strip();
            if (name.isEmpty()) {
                fehler.add("Verein-Eintrag " + (i + 1) + ": Name fehlt");
                continue;
            }
            String key = SpielerMatchKeyNormalizer.vereinSchluessel(name);
            ValVerein neu = new ValVerein(v.nr(), name);
            if (dedup.put(key, neu) != null) {
                warnungen.add(new ImportWarnung(
                        "Verein '" + name + "' kommt mehrfach in der Datei vor — letzter Eintrag gewinnt"));
            }
        }
        return new ArrayList<>(dedup.values());
    }

    private static List<ValLabel> validiereLabels(List<RohLabel> roh, List<String> fehler,
            List<ImportWarnung> warnungen) {
        LinkedHashMap<String, ValLabel> dedup = new LinkedHashMap<>();
        for (int i = 0; i < roh.size(); i++) {
            RohLabel l = roh.get(i);
            String name = l.name() == null ? "" : l.name().strip();
            if (name.isEmpty()) {
                fehler.add("Label-Eintrag " + (i + 1) + ": Name fehlt");
                continue;
            }
            String key = SpielerMatchKeyNormalizer.labelSchluessel(name);
            ValLabel neu = new ValLabel(l.nr(), name);
            if (dedup.put(key, neu) != null) {
                warnungen.add(new ImportWarnung(
                        "Label '" + name + "' kommt mehrfach in der Datei vor — letzter Eintrag gewinnt"));
            }
        }
        return new ArrayList<>(dedup.values());
    }

    private static List<ValSpieler> validiereSpieler(List<RohSpieler> roh, List<String> fehler,
            List<ImportWarnung> warnungen) {
        LinkedHashMap<String, ValSpieler> dedup = new LinkedHashMap<>();
        for (int i = 0; i < roh.size(); i++) {
            RohSpieler s = roh.get(i);
            String vorname = s.vorname() == null ? "" : s.vorname().strip();
            String nachname = s.nachname() == null ? "" : s.nachname().strip();
            if (vorname.isEmpty() || nachname.isEmpty()) {
                fehler.add("Spieler-Eintrag " + (i + 1) + ": Vor-/Nachname fehlt");
                continue;
            }
            String key = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinNr(
                    vorname, nachname, s.vereinNr());
            ValSpieler neu = new ValSpieler(s.nr(), vorname, nachname,
                    s.vereinNr(), s.vereinName(), s.lizenznr());
            if (dedup.put(key, neu) != null) {
                warnungen.add(new ImportWarnung(
                        "Spieler '" + vorname + " " + nachname + "' kommt mehrfach in der Datei vor"
                                + " — letzter Eintrag gewinnt"));
            }
        }
        return new ArrayList<>(dedup.values());
    }

    private static List<ValSpielerLabel> validiereJunction(ImportRohdaten roh,
            List<ImportWarnung> warnungen) {
        Set<Integer> bekannteSpielerNrs = new HashSet<>();
        for (RohSpieler s : roh.spieler()) {
            if (s.nr() != null) {
                bekannteSpielerNrs.add(s.nr());
            }
        }
        Set<Integer> bekannteLabelNrs = new HashSet<>();
        for (RohLabel l : roh.labels()) {
            if (l.nr() != null) {
                bekannteLabelNrs.add(l.nr());
            }
        }
        List<ValSpielerLabel> erg = new ArrayList<>(roh.spielerLabels().size());
        for (RohSpielerLabel j : roh.spielerLabels()) {
            if (!bekannteSpielerNrs.contains(j.spielerNr())) {
                warnungen.add(new ImportWarnung("Junction-Eintrag verweist auf unbekannte spielerNr="
                        + j.spielerNr() + " — übersprungen"));
                continue;
            }
            if (!bekannteLabelNrs.contains(j.labelNr())) {
                warnungen.add(new ImportWarnung("Junction-Eintrag verweist auf unbekannte labelNr="
                        + j.labelNr() + " — übersprungen"));
                continue;
            }
            erg.add(new ValSpielerLabel(j.spielerNr(), j.labelNr()));
        }
        return erg;
    }
}
