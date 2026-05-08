package de.petanqueturniermanager.spielerdb.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

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
 *
 * <p>Implizite Verein-Synthese: Hat ein {@link RohSpieler} nur einen
 * {@code vereinName} ohne {@code vereinNr} und gibt es keinen passenden
 * {@link RohVerein}, wird ein synthetischer {@link ValVerein} mit einer
 * Sentinel-Negativ-NR angelegt. Der Importer legt diesen Verein dann beim
 * Apply automatisch an. Sentinel-NRs sind ausschließlich validator-intern;
 * nach dem Verein-Insert führt das Mapping zur echten DB-NR.
 */
public final class SpielerDbImportValidator {

    public ValidierteDaten validiere(ImportRohdaten roh) throws SpielerDbException {
        List<String> fehler = new ArrayList<>();
        List<ImportWarnung> warnungen = new ArrayList<>();

        List<ValVerein> vereine = validiereVereine(roh.vereine(), fehler, warnungen);
        List<ValLabel> labels = validiereLabels(roh.labels(), fehler, warnungen);

        Map<String, Integer> vereinNameZuAltNr = sammleVereinNamen(vereine);
        ergaenzeImpliziteVereine(roh.spieler(), vereine, vereinNameZuAltNr);

        List<ValSpieler> spieler = validiereSpieler(roh.spieler(), vereinNameZuAltNr,
                fehler, warnungen);
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

    private static Map<String, Integer> sammleVereinNamen(List<ValVerein> vereine) {
        Map<String, Integer> erg = new HashMap<>();
        for (ValVerein v : vereine) {
            if (v.altNr() != null) {
                erg.put(SpielerMatchKeyNormalizer.vereinSchluessel(v.name()), v.altNr());
            }
        }
        return erg;
    }

    /**
     * Für jeden RohSpieler mit {@code vereinName} aber ohne {@code vereinNr}
     * (und ohne passenden expliziten RohVerein) wird ein synthetischer
     * {@link ValVerein} mit Sentinel-Negativ-NR angelegt. Die NR ist
     * ausschließlich validator-intern und wird vom Importer beim Apply auf
     * eine echte DB-NR gemappt.
     */
    private static void ergaenzeImpliziteVereine(List<RohSpieler> spieler,
            List<ValVerein> vereine, Map<String, Integer> nameZuAltNr) {
        int sentinel = -1;
        // Schon vergebene negative AltNrs (z.B. aus expliziten RohVereinen mit nr=null
        // wären in vereine schon mit altNr=null vermerkt — diese betreffen den Sentinel
        // nicht). Wir starten frisch bei -1, da Sentinel-NRs nur dann benutzt werden,
        // wenn der explizite RohVerein keine altNr hatte; das stört das Mapping nicht.
        Set<Integer> bereitsBenutzteSentinels = new HashSet<>();
        for (RohSpieler s : spieler) {
            if (s.vereinNr() != null) {
                continue;
            }
            String vereinName = s.vereinName();
            if (vereinName == null) {
                continue;
            }
            String getrimmt = vereinName.strip();
            if (getrimmt.isEmpty()) {
                continue;
            }
            String key = SpielerMatchKeyNormalizer.vereinSchluessel(getrimmt);
            if (nameZuAltNr.containsKey(key)) {
                continue;
            }
            while (bereitsBenutzteSentinels.contains(sentinel)) {
                sentinel--;
            }
            int altNr = sentinel--;
            bereitsBenutzteSentinels.add(altNr);
            vereine.add(new ValVerein(altNr, getrimmt));
            nameZuAltNr.put(key, altNr);
        }
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

    private static List<ValSpieler> validiereSpieler(List<RohSpieler> roh,
            Map<String, Integer> vereinNameZuAltNr, List<String> fehler,
            List<ImportWarnung> warnungen) {
        LinkedHashMap<String, ValSpieler> dedup = new LinkedHashMap<>();
        for (int i = 0; i < roh.size(); i++) {
            RohSpieler s = roh.get(i);
            String vorname = s.vorname() == null ? "" : s.vorname().strip();
            String nachname = s.nachname() == null ? "" : s.nachname().strip();
            String herkunft = herkunftsLabel(s, i);
            if (vorname.isEmpty() || nachname.isEmpty()) {
                fehler.add(herkunft + ": Vor-/Nachname fehlt");
                continue;
            }
            Integer effektiveVereinNr = effektiveVereinNr(s, vereinNameZuAltNr);
            String key = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinNr(
                    vorname, nachname, effektiveVereinNr);
            ValSpieler neu = new ValSpieler(s.nr(), vorname, nachname,
                    effektiveVereinNr, s.vereinName(), s.lizenznr(), s.quellZeile());
            if (dedup.put(key, neu) != null) {
                String praefix = s.quellZeile() != null ? "Zeile " + s.quellZeile() + ": " : "";
                warnungen.add(new ImportWarnung(praefix
                        + "Spieler '" + vorname + " " + nachname + "' kommt mehrfach in der Datei vor"
                        + " — letzter Eintrag gewinnt"));
            }
        }
        return new ArrayList<>(dedup.values());
    }

    @Nullable
    private static Integer effektiveVereinNr(RohSpieler s, Map<String, Integer> nameZuAltNr) {
        if (s.vereinNr() != null) {
            return s.vereinNr();
        }
        String name = s.vereinName();
        if (name == null) {
            return null;
        }
        String getrimmt = name.strip();
        if (getrimmt.isEmpty()) {
            return null;
        }
        return nameZuAltNr.get(SpielerMatchKeyNormalizer.vereinSchluessel(getrimmt));
    }

    private static String herkunftsLabel(RohSpieler s, int idx) {
        return s.quellZeile() != null
                ? "Zeile " + s.quellZeile()
                : "Spieler-Eintrag " + (idx + 1);
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
