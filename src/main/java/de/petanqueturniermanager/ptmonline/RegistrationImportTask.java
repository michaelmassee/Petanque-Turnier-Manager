/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.onlinesync.sheet.NeueZuordnung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Importiert online eingegangene, bestaetigte und noch nicht lokal vorhandene Anmeldungen
 * (PTM-Online) in die aktive Meldeliste. Neue Zeilen bleiben in der Aktiv-Spalte leer (inaktiv):
 * der Anmeldestatus sagt nichts über die Teilnahme aus, die erst mit dem Check-in gesetzt wird.
 * Nutzt denselben turniersystem-generischen Schreibpfad wie die Spieler-DB-Integration
 * ({@link MeldelisteZiel#schreibeBlock}, {@link MeldelisteZielFactory#starteMeldelisteUpdate}).
 * Bei aktiver Mêlée-Anmeldung ist das Ziel stattdessen das Mêlée-Anmeldung-Sheet
 * ({@link MeldelisteZielFactory#fuerPtmOnline}): Online-Mêlée-Turniere nehmen nur Einzelspieler an.
 * <p>
 * Übernommen wird nur beim manuellen Abgleich ({@link PtmOnlineAbgleichSheetRunner}: ProcessBox, Abbruch).
 * Der Rundenstart ({@link PtmOnlineSpielrundeSync}) importiert nicht, sondern fragt vor dem Turnierstart
 * nach, wenn online noch Meldungen fehlen ({@link #pruefeVorTurnierstart}).
 */
public final class RegistrationImportTask {

    private static final Logger logger = LogManager.getLogger(RegistrationImportTask.class);

    private RegistrationImportTask() {}

    @FunctionalInterface
    public interface MeldelistenAktualisierung {
        void aktualisieren() throws GenerateException, InterruptedException;
    }

    private record GeschriebeneAnmeldung(RegistrationDto registration, int zeile1Basiert) {}

    /**
     * Nicht übernommene Anmeldungen werden beim nächsten Abgleich erneut versucht ({@code lastSync}
     * bleibt dann stehen).
     *
     * @param importiert      als neue Meldelistenzeile übernommene Anmeldungen
     * @param namensgleich    Online-Bezeichnungen der Anmeldungen, deren Name bereits in einer anderen,
     *                        schon verknüpften Meldelistenzeile steht (doppelte Namen blockieren das
     *                        Aktualisieren der Meldeliste)
     * @param nichtZuordenbar Online-Bezeichnungen der Anmeldungen, die nicht zur Formation passen, zu
     *                        mehreren Zeilen passen oder nicht geschrieben werden konnten
     */
    public record ImportErgebnis(int importiert, List<String> namensgleich, List<String> nichtZuordenbar) {

        public ImportErgebnis {
            namensgleich = List.copyOf(namensgleich);
            nichtZuordenbar = List.copyOf(nichtZuordenbar);
        }

        public boolean vollstaendig() {
            return namensgleich.isEmpty() && nichtZuordenbar.isEmpty();
        }

        /** Benutzerhinweise zu nicht übernommenen Anmeldungen, leer wenn alles übernommen wurde. */
        public List<String> hinweise() {
            List<String> hinweise = new ArrayList<>();
            if (!namensgleich.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.anmeldungen_namensgleich", String.join(", ", namensgleich)));
            }
            if (!nichtZuordenbar.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.anmeldungen_nicht_zuordenbar",
                        String.join(", ", nichtZuordenbar)));
            }
            return hinweise;
        }
    }

    /**
     * @param onlineAbgelehnt lokale Bezeichnungen der Meldungen, die PTM-Online beim Anlegen abgelehnt hat, weil
     *                        ein Spieler oder der Teamname dort bereits angemeldet ist
     */
    public record AbgleichErgebnis(ImportErgebnis importErgebnis, int onlineAngelegt, List<String> onlineAbgelehnt) {

        public AbgleichErgebnis {
            onlineAbgelehnt = List.copyOf(onlineAbgelehnt);
        }

        public List<String> hinweise() {
            List<String> hinweise = new ArrayList<>(importErgebnis.hinweise());
            if (!onlineAbgelehnt.isEmpty()) {
                hinweise.add(onlineAbgelehntHinweis(onlineAbgelehnt));
            }
            return hinweise;
        }
    }

    private record NeuanlageErgebnis(int angelegt, List<String> abgelehnt) {}

    /** Benutzerhinweis zu lokalen Meldungen, die PTM-Online als bereits angemeldet abgelehnt hat. */
    public static String onlineAbgelehntHinweis(List<String> lokaleBezeichnungen) {
        return I18n.get("ptmonline.hinweis.online_abgelehnt", String.join(", ", lokaleBezeichnungen));
    }

    public static void starte(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        Optional<MeldelisteZiel> zielOpt = MeldelisteZielFactory.fuerPtmOnline(ws);
        if (zielOpt.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.keine_meldeliste"));
            return;
        }

        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        PtmOnlineRegistrationMapping mapping;
        Optional<String> tournamentId;
        boolean pausiert;
        try {
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            mapping = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr);
            tournamentId = mapping.getTournamentId();
            pausiert = mapping.istPausiert();
        } catch (GenerateException e) {
            zeigeFehler(ctx, e.getMessage());
            return;
        }
        if (tournamentId.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_angelegt"));
            return;
        }
        if (pausiert) {
            MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("ptmonline.menu.toplevel"))
                    .message(I18n.get("ptmonline.hinweis.sync_pausiert"))
                    .show();
            return;
        }

        new PtmOnlineAbgleichSheetRunner(ws, ts, config, mapping, tournamentId.get(), zielOpt.get()).start();
    }

    /** Manueller Abgleich: importiert neue Online-Meldungen und legt neue lokale Meldungen online an. */
    public static AbgleichErgebnis fuehreAbgleichDurch(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel,
            MeldelistenAktualisierung aktualisierung, AbgleichFortschritt fortschritt)
            throws IOException, InterruptedException, GenerateException {
        mapping.sicherstellen();
        ImportErgebnis importErgebnis = fuehreImportDurch(config, mapping, tournamentId, ziel, aktualisierung,
                fortschritt);
        fortschritt.pruefeAbbruch();
        fortschritt.status(I18n.get("ptmonline.fortschritt.lokale_meldungen_anlegen"));
        NeuanlageErgebnis neuanlage = neueLokaleMeldungenAnlegen(config, mapping, tournamentId, ziel, fortschritt);
        fortschritt.pruefeAbbruch();
        fortschritt.status(I18n.get("ptmonline.fortschritt.details_aktualisieren"));
        aktualisiereBezeichnungen(config, mapping, tournamentId, ziel);
        return new AbgleichErgebnis(importErgebnis, neuanlage.angelegt(), neuanlage.abgelehnt());
    }

    /**
     * Prüfung vor dem Turnierstart (erste Spielrunde): importiert nichts, sondern liefert die bestätigten
     * Online-Anmeldungen, die noch in keiner Meldelistenzeile stehen. Namensgleiche, vor Ort erfasste und
     * noch nicht verknüpfte Zeilen werden dabei verknüpft – sonst legt der Rundenstart sie online erneut an
     * und PTM-Online lehnt sie als doppelte Spieler ab. {@code lastSync} bleibt unverändert, damit ein
     * späterer manueller Abgleich die fehlenden Anmeldungen weiterhin abruft.
     *
     * @return Online-Bezeichnungen der fehlenden Anmeldungen, leer wenn die Meldeliste vollständig ist.
     */
    public static List<String> pruefeVorTurnierstart(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
        List<RegistrationDto> registrations = client.fetchRegistrations(tournamentId,
                mapping.getLastSync().orElse(Instant.EPOCH));
        Map<String, List<Integer>> vorhandeneZeilen = vorhandeneZeilenNachBesetzung(ziel);
        List<String> fehlend = new ArrayList<>();
        for (RegistrationDto reg : registrations) {
            if (!istImportierbar(reg) || mapping.istBereitsImportiert(reg.id())) {
                continue;
            }
            List<Integer> gleicheZeilen = vorhandeneZeilen.getOrDefault(besetzungsSchluessel(reg), List.of());
            if (gleicheZeilen.isEmpty()) {
                fehlend.add(onlineBezeichnung(reg));
                continue;
            }
            List<Integer> freieZeilen = nichtVerknuepfteZeilen(mapping, ziel, gleicheZeilen, Set.of());
            if (freieZeilen.size() == 1) {
                verknuepfeBestehendeZeile(mapping, ziel, reg, freieZeilen.getFirst());
            }
        }
        return fehlend;
    }

    /** Besetzung einer Online-Anmeldung aus allen angegebenen Spielernamen, unabhängig von der Formation. */
    private static String besetzungsSchluessel(RegistrationDto reg) {
        return besetzungsSchluessel(Stream.of(
                        new String[] { reg.firstName(), reg.lastName() },
                        new String[] { reg.partnerFirstName(), reg.partnerLastName() },
                        new String[] { reg.partner2FirstName(), reg.partner2LastName() })
                .filter(name -> !istLeer(name[0]) || !istLeer(name[1]))
                .map(name -> OnlineSpielerName.schluessel(name[0], name[1])));
    }

    /**
     * Holt neue Online-Anmeldungen, schreibt sie in die Meldeliste und aktualisiert das Mapping.
     * Synchron und blockierend; läuft innerhalb des {@link PtmOnlineAbgleichSheetRunner}, nie auf dem
     * LO-Main-Thread.
     */
    private static ImportErgebnis fuehreImportDurch(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel,
            MeldelistenAktualisierung aktualisierung, AbgleichFortschritt fortschritt)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
        Instant abgleichStart = Instant.now();
        Instant since = mapping.getLastSync().orElse(Instant.EPOCH);
        fortschritt.status(I18n.get("ptmonline.fortschritt.anmeldungen_abrufen"));
        List<RegistrationDto> alle = client.fetchRegistrations(tournamentId, since);
        fortschritt.status(I18n.get("ptmonline.fortschritt.anmeldungen_abgerufen", alle.size()));
        fortschritt.pruefeAbbruch();
        uebernehmeOnlineStornierungen(ziel, mapping, alle);
        ImportErgebnis ergebnis = uebernehmeAnmeldungen(alle, mapping, ziel, aktualisierung, fortschritt);
        if (ergebnis.vollstaendig()) {
            mapping.setLastSync(abgleichStart);
        }
        return ergebnis;
    }

    /**
     * Ordnet bestätigte, noch nicht importierte Anmeldungen der Meldeliste zu:
     * <ul>
     * <li>keine Zeile mit derselben Besetzung: neue, inaktive Zeile;</li>
     * <li>genau eine noch nicht online verknüpfte Zeile: wird verknüpft (vor Ort erfasst und
     * zusätzlich online gemeldet);</li>
     * <li>genau eine namensgleiche Zeile hängt an einer online stornierten Anmeldung: Neuanmeldung
     * derselben Person, die Zeile wird auf die neue Anmeldung umgehängt und ihre Abmeldung aufgehoben;</li>
     * <li>alle namensgleichen Zeilen anderweitig verknüpft (oder in diesem Lauf neu geschrieben): nicht
     * übernommen, da doppelte Namen das Aktualisieren der Meldeliste blockieren. Macht die
     * Turnierleitung die lokale Zeile unterscheidbar, wird die Anmeldung beim nächsten Abgleich
     * übernommen.</li>
     * </ul>
     */
    static ImportErgebnis uebernehmeAnmeldungen(List<RegistrationDto> registrations,
            PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel, MeldelistenAktualisierung aktualisierung,
            AbgleichFortschritt fortschritt) throws GenerateException, InterruptedException {
        List<RegistrationDto> neue = new ArrayList<>();
        Set<String> importierte = mapping.getImportierteOnlineIds();
        for (RegistrationDto reg : registrations) {
            if (istImportierbar(reg) && !importierte.contains(reg.id())) {
                neue.add(reg);
            }
        }

        if (neue.isEmpty()) {
            return new ImportErgebnis(0, List.of(), List.of());
        }

        List<GeschriebeneAnmeldung> geschrieben = new ArrayList<>();
        Set<Integer> geschriebeneZeilen = new HashSet<>();
        List<String> namensgleich = new ArrayList<>();
        List<String> nichtZuordenbar = new ArrayList<>();
        Map<String, List<Integer>> vorhandeneZeilen = vorhandeneZeilenNachBesetzung(ziel);
        for (RegistrationDto reg : neue) {
            fortschritt.pruefeAbbruch();
            List<SpielerMitVerein> spieler = zuSpielerListe(reg, ziel.getFormation());
            if (spieler == null) {
                logger.warn("PTM-Online: Anmeldung {} passt nicht zur Formation {} der Meldeliste, übersprungen",
                        reg.id(), ziel.getFormation());
                nichtZuordenbar.add(onlineBezeichnung(reg));
                continue;
            }
            String besetzung = besetzungsSchluessel(spieler.stream().map(s -> OnlineSpielerName.schluessel(s.vorname(), s.nachname())));
            List<Integer> gleicheZeilen = vorhandeneZeilen.getOrDefault(besetzung, List.of());
            List<Integer> freieZeilen = nichtVerknuepfteZeilen(mapping, ziel, gleicheZeilen, geschriebeneZeilen);
            if (freieZeilen.size() == 1) {
                verknuepfeBestehendeZeile(mapping, ziel, reg, freieZeilen.getFirst());
                fortschritt.status(I18n.get("ptmonline.fortschritt.meldung_verknuepft", onlineBezeichnung(reg)));
                continue;
            }
            if (freieZeilen.size() > 1) {
                logger.warn("PTM-Online: Anmeldung {} passt zu mehreren lokalen Meldelistenzeilen; nicht importiert", reg.id());
                nichtZuordenbar.add(onlineBezeichnung(reg));
                continue;
            }
            List<Integer> stornierteZeilen = onlineStornierteZeilen(mapping, ziel, gleicheZeilen, geschriebeneZeilen);
            if (stornierteZeilen.size() == 1) {
                verknuepfeNachOnlineStorno(mapping, ziel, reg, stornierteZeilen.getFirst());
                fortschritt.status(I18n.get("ptmonline.fortschritt.meldung_verknuepft", onlineBezeichnung(reg)));
                continue;
            }
            if (!gleicheZeilen.isEmpty()) {
                logger.warn("PTM-Online: Name der Anmeldung {} steht bereits in einer verknüpften Meldelistenzeile; "
                        + "nicht importiert", reg.id());
                namensgleich.add(onlineBezeichnung(reg));
                continue;
            }
            try {
                int zeile = ziel.schreibeBlockUndLiefereZeile(spieler, MeldelisteZiel.NeueMeldungTeilnahme.INAKTIV);
                if (zeile <= 0) {
                    logger.error("PTM-Online: Anmeldung {} lieferte keine eindeutige Meldeliste-Zeile", reg.id());
                    nichtZuordenbar.add(onlineBezeichnung(reg));
                } else {
                    geschrieben.add(new GeschriebeneAnmeldung(reg, zeile));
                    fortschritt.status(I18n.get("ptmonline.fortschritt.meldung_uebernommen", onlineBezeichnung(reg)));
                    geschriebeneZeilen.add(zeile);
                    uebernehmeSetzposition(ziel, reg, zeile);
                    vorhandeneZeilen.computeIfAbsent(besetzung, ignored -> new ArrayList<>()).add(zeile);
                }
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                logger.error("PTM-Online: Anmeldung {} konnte nicht in die Meldeliste geschrieben werden", reg.id(), e);
                nichtZuordenbar.add(onlineBezeichnung(reg));
            }
        }
        if (geschrieben.isEmpty()) {
            return new ImportErgebnis(0, namensgleich, nichtZuordenbar);
        }

        fortschritt.status(I18n.get("ptmonline.fortschritt.meldeliste_aktualisieren"));
        aktualisierung.aktualisieren();

        Map<Integer, String> uuidProZeile;
        try {
            uuidProZeile = ziel.getOderErzeugeLokaleUuids(
                    geschrieben.stream().map(GeschriebeneAnmeldung::zeile1Basiert).toList());
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
        Map<Integer, String> bezeichnungProZeile = lokaleBezeichnungen(ziel);
        List<NeueZuordnung> zuordnungen = new ArrayList<>();
        for (GeschriebeneAnmeldung geschriebene : geschrieben) {
            RegistrationDto reg = geschriebene.registration();
            String uuid = uuidProZeile.get(geschriebene.zeile1Basiert());
            try {
                zuordnungen.add(PtmOnlineRegistrationMapping.neueZuordnung(uuid, ziel.formelTeamNrAusLokalerUuid(uuid),
                        bezeichnungProZeile.getOrDefault(geschriebene.zeile1Basiert(), ""), reg));
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                logger.warn("PTM-Online: Nummernformel für importierte Anmeldung {} nicht ermittelt", reg.id(), e);
                nichtZuordenbar.add(onlineBezeichnung(reg));
            }
        }
        mapping.addMappings(zuordnungen);
        return new ImportErgebnis(zuordnungen.size(), namensgleich, nichtZuordenbar);
    }

    /**
     * Nur bestaetigte Anmeldungen werden lokal in die Meldeliste uebernommen. Offene, Warteliste- und
     * stornierte Anmeldungen bleiben online; wird eine offene Anmeldung spaeter bestaetigt, liefert der
     * {@code since}-Abruf (Filter auf {@code updated_at}) sie beim naechsten Abgleich erneut.
     */
    static boolean istImportierbar(RegistrationDto registration) {
        return OnlineAnmeldeStatus.istBestaetigt(registration.status());
    }

    /**
     * Filtert die Zeilen heraus, die bereits einer Online-Anmeldung zugeordnet sind oder in diesem Lauf
     * für eine andere Anmeldung neu geschrieben wurden (deren Zuordnung folgt erst nach dem Aktualisieren).
     */
    private static List<Integer> nichtVerknuepfteZeilen(PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel,
            List<Integer> zeilen, Set<Integer> geschriebeneZeilen) throws GenerateException {
        List<Integer> freie = new ArrayList<>();
        for (int zeile : zeilen) {
            if (!geschriebeneZeilen.contains(zeile) && mapping.getOnlineId(lokaleUuid(ziel, zeile)).isEmpty()) {
                freie.add(zeile);
            }
        }
        return freie;
    }

    /** Namensgleiche Zeilen, deren zugeordnete Online-Anmeldung inzwischen storniert ist. */
    private static List<Integer> onlineStornierteZeilen(PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel,
            List<Integer> zeilen, Set<Integer> geschriebeneZeilen) throws GenerateException {
        List<Integer> stornierte = new ArrayList<>();
        for (int zeile : zeilen) {
            if (!geschriebeneZeilen.contains(zeile) && mapping.istOnlineStorniert(lokaleUuid(ziel, zeile))) {
                stornierte.add(zeile);
            }
        }
        return stornierte;
    }

    /**
     * Neuanmeldung nach Online-Storno: die bisherige Zeile wird der neuen Anmeldung zugeordnet und von
     * „abgemeldet“ wieder auf inaktiv gesetzt, statt eine zweite, namensgleiche Zeile anzulegen.
     */
    private static void verknuepfeNachOnlineStorno(PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel,
            RegistrationDto reg, int zeile) throws GenerateException {
        String uuid = lokaleUuid(ziel, zeile);
        try {
            ziel.hebeAbmeldungAuf(zeile);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException("Abmeldung konnte nicht aufgehoben werden: " + e.getMessage());
        }
        mapping.ersetzeOnlineId(uuid, reg.id(), executionRevision(reg));
        mapping.setBezeichnung(uuid, lokaleBezeichnung(ziel, zeile), onlineStatus(reg));
        mapping.setOnlineDetails(uuid, reg);
        uebernehmeSetzposition(ziel, reg, zeile);
        logger.info("PTM-Online: Neuanmeldung {} nach Storno mit bestehender Meldelistenzeile {} verknüpft", reg.id(), zeile);
    }

    private static void verknuepfeBestehendeZeile(PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel,
            RegistrationDto reg, int zeile) throws GenerateException {
        String uuid = lokaleUuid(ziel, zeile);
        try {
            mapping.addMapping(uuid, reg.id(), ziel.formelTeamNrAusLokalerUuid(uuid), executionRevision(reg),
                    lokaleBezeichnung(ziel, zeile), onlineStatus(reg));
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException("Lokale vorhandene Anmeldung konnte nicht verknüpft werden: " + e.getMessage());
        }
        mapping.setOnlineDetails(uuid, reg);
        uebernehmeSetzposition(ziel, reg, zeile);
    }

    /**
     * Online gepflegte Setzposition übernehmen, sofern die Zeile noch keine hat. Scheitert das, bleibt die
     * Anmeldung trotzdem übernommen.
     */
    private static void uebernehmeSetzposition(MeldelisteZiel ziel, RegistrationDto reg, int zeile1Basiert) {
        if (reg.seedingPosition() == null) {
            return;
        }
        try {
            ziel.uebernehmeOnlineSetzposition(zeile1Basiert, reg.seedingPosition());
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            logger.warn("PTM-Online: Setzposition der Anmeldung {} nicht übernommen", reg.id(), e);
        }
    }

    private static String lokaleUuid(MeldelisteZiel ziel, int zeile1Basiert) throws GenerateException {
        try {
            return ziel.getOderErzeugeLokaleUuid(zeile1Basiert);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    /**
     * Meldelistenzeilen gruppiert nach ihrer Besetzung (alle Spielernamen, reihenfolgeunabhängig, verglichen
     * nach der PTM-Online-Regel {@link OnlineSpielerName}).
     */
    private static Map<String, List<Integer>> vorhandeneZeilenNachBesetzung(MeldelisteZiel ziel) {
        Map<Integer, List<MeldelisteSpielerDaten>> proZeile = ziel.leseAlleSpielerRoh().stream()
                .collect(Collectors.groupingBy(MeldelisteSpielerDaten::zeile1Basiert, LinkedHashMap::new,
                        Collectors.toList()));
        Map<String, List<Integer>> nachBesetzung = new LinkedHashMap<>();
        proZeile.forEach((zeile, spieler) -> nachBesetzung
                .computeIfAbsent(besetzungsSchluessel(spieler.stream()
                        .map(s -> OnlineSpielerName.schluessel(s.vorname(), s.nachname()))), ignored -> new ArrayList<>())
                .add(zeile));
        return nachBesetzung;
    }

    private static String besetzungsSchluessel(Stream<String> nameSchluessel) {
        return nameSchluessel.sorted().collect(Collectors.joining("\u0000"));
    }

    private static int executionRevision(RegistrationDto registration) {
        return registration.executionRevision() == null ? 1 : Math.max(1, registration.executionRevision());
    }

    private static NeuanlageErgebnis neueLokaleMeldungenAnlegen(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel, AbgleichFortschritt fortschritt)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = gebundenerClient(config, mapping);
        Map<Integer, List<MeldelisteSpielerDaten>> proZeile = spielerProZeile(ziel);
        Map<Integer, String> uuidProZeile = lokaleUuids(ziel, proZeile.keySet());
        Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
        List<String> abgelehnt = new ArrayList<>();
        List<NeueZuordnung> zuordnungen = new ArrayList<>();
        try {
            for (Map.Entry<Integer, List<MeldelisteSpielerDaten>> eintrag : proZeile.entrySet()) {
                String uuid = uuidProZeile.get(eintrag.getKey());
                if (uuid == null || onlineIds.containsKey(uuid)) {
                    continue;
                }
                fortschritt.pruefeAbbruch();
                String bezeichnung = bezeichnung(eintrag.getValue());
                RegistrationDto remote;
                try {
                    remote = client.upsertRegistration(tournamentId, uuid, zuOnlineAnmeldung(eintrag.getValue()));
                } catch (PtmOnlineHttpException e) {
                    if (!e.istBereitsAngemeldet()) {
                        throw e;
                    }
                    logger.warn("PTM-Online: lokale Meldung {} online abgelehnt (bereits angemeldet)", uuid, e);
                    abgelehnt.add(bezeichnung);
                    continue;
                }
                zuordnungen.add(PtmOnlineRegistrationMapping.neueZuordnung(uuid, formelTeamNr(ziel, uuid), bezeichnung,
                        remote));
                fortschritt.status(I18n.get("ptmonline.fortschritt.meldung_online_angelegt", bezeichnung));
            }
        } finally {
            // Auch nach Abbruch oder Netzfehler: bereits online angelegte Meldungen lokal zuordnen.
            mapping.addMappings(zuordnungen);
        }
        return new NeuanlageErgebnis(zuordnungen.size(), abgelehnt);
    }

    private static TournamentSyncClient gebundenerClient(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping) throws GenerateException {
        String documentId = mapping.getSyncDocumentId()
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig")));
        String leaseToken = mapping.getLeaseToken()
                .orElseThrow(() -> new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig")));
        return new TournamentSyncClient(config.baseUrl(), config.apiKey(), documentId, leaseToken);
    }

    private static Map<Integer, List<MeldelisteSpielerDaten>> spielerProZeile(MeldelisteZiel ziel) {
        return ziel.leseAlleSpielerRoh().stream().collect(Collectors.groupingBy(MeldelisteSpielerDaten::zeile1Basiert,
                LinkedHashMap::new, Collectors.toList()));
    }

    /** Lokale Bezeichnung („Vorname Nachname / …“) je 1-basierter Sheet-Zeile, aus einem Lesezugriff. */
    private static Map<Integer, String> lokaleBezeichnungen(MeldelisteZiel ziel) {
        Map<Integer, String> ergebnis = new LinkedHashMap<>();
        spielerProZeile(ziel).forEach((zeile, spieler) -> ergebnis.put(zeile, bezeichnung(spieler)));
        return ergebnis;
    }

    private static String bezeichnung(List<MeldelisteSpielerDaten> spieler) {
        return spieler.stream().map(RegistrationImportTask::spielerBezeichnung).filter(name -> !name.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private static Map<Integer, String> lokaleUuids(MeldelisteZiel ziel, Collection<Integer> zeilen1Basiert)
            throws GenerateException {
        try {
            return ziel.getOderErzeugeLokaleUuids(zeilen1Basiert);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    private static String formelTeamNr(MeldelisteZiel ziel, String uuid) throws GenerateException {
        try {
            return ziel.formelTeamNrAusLokalerUuid(uuid);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    private static de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung zuOnlineAnmeldung(
            List<MeldelisteSpielerDaten> spieler) {
        MeldelisteSpielerDaten erster = spieler.get(0);
        MeldelisteSpielerDaten zweiter = spieler.size() > 1 ? spieler.get(1) : null;
        MeldelisteSpielerDaten dritter = spieler.size() > 2 ? spieler.get(2) : null;
        return new de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung(
                erster.vorname(), erster.nachname(), erster.vereinName(), null,
                zweiter == null ? null : zweiter.vorname(), zweiter == null ? null : zweiter.nachname(),
                dritter == null ? null : dritter.vorname(), dritter == null ? null : dritter.nachname(),
                null, true, true, List.of(), List.of());
    }

    private static void aktualisiereBezeichnungen(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        Map<String, RegistrationDto> remoteProId = gebundenerClient(config, mapping)
                .fetchRegistrations(tournamentId, null).stream()
                .collect(Collectors.toMap(RegistrationDto::id, registration -> registration));
        Map<Integer, String> bezeichnungProZeile = lokaleBezeichnungen(ziel);
        Map<Integer, String> uuidProZeile = lokaleUuids(ziel, bezeichnungProZeile.keySet());
        Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
        Map<String, String> bezeichnungProUuid = new LinkedHashMap<>();
        Map<String, RegistrationDto> registrationProUuid = new LinkedHashMap<>();
        uuidProZeile.forEach((zeile, uuid) -> {
            String onlineId = onlineIds.get(uuid);
            if (onlineId == null) {
                return;
            }
            bezeichnungProUuid.put(uuid, bezeichnungProZeile.get(zeile));
            RegistrationDto remote = remoteProId.get(onlineId);
            if (remote != null) {
                registrationProUuid.put(uuid, remote);
            }
        });
        mapping.aktualisiereAnzeigen(bezeichnungProUuid, registrationProUuid);
    }

    private static String lokaleBezeichnung(MeldelisteZiel ziel, int zeile1Basiert) {
        return ziel.leseAlleSpielerRoh().stream()
                .filter(spieler -> spieler.zeile1Basiert() == zeile1Basiert)
                .map(RegistrationImportTask::spielerBezeichnung)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private static String onlineBezeichnung(RegistrationDto registration) {
        return Stream.of(
                name(registration.firstName(), registration.lastName()),
                name(registration.partnerFirstName(), registration.partnerLastName()),
                name(registration.partner2FirstName(), registration.partner2LastName()))
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private static String spielerBezeichnung(MeldelisteSpielerDaten spieler) {
        return name(spieler.vorname(), spieler.nachname());
    }

    private static String name(String vorname, String nachname) {
        return (String.valueOf(vorname == null ? "" : vorname).strip() + " "
                + String.valueOf(nachname == null ? "" : nachname).strip()).strip();
    }

    private static void uebernehmeOnlineStornierungen(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            List<RegistrationDto> registrations) throws GenerateException {
        for (RegistrationDto registration : registrations) {
            if (!OnlineAnmeldeStatus.istStorniert(registration.status())) {
                continue;
            }
            Optional<String> lokaleUuid = mapping.getLokaleUuid(registration.id());
            if (lokaleUuid.isEmpty()) {
                continue;
            }
            for (MeldelisteSpielerDaten spieler : ziel.leseAlleSpielerRoh()) {
                try {
                    if (lokaleUuid.get().equals(ziel.getOderErzeugeLokaleUuid(spieler.zeile1Basiert()))) {
                        ziel.markiereAlsAbgemeldet(spieler.zeile1Basiert());
                        mapping.setBezeichnung(lokaleUuid.get(), lokaleBezeichnung(ziel, spieler.zeile1Basiert()),
                                onlineStatus(registration));
                        mapping.setOnlineDetails(lokaleUuid.get(), registration);
                        break;
                    }
                } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                    throw new GenerateException(e.getMessage());
                }
            }
        }
    }

    private static String onlineStatus(RegistrationDto registration) {
        return OnlineAnmeldeStatus.anzeige(registration.status());
    }

    /**
     * Baut die Spielerliste passend zur Meldeliste-Formation. Liefert {@code null}, wenn die
     * Registrierung fuer die geforderte Spielerzahl nicht genug ausgefuellte Namen mitbringt
     * (defensiv statt Absturz — z.B. Doublette-Meldeliste, aber Anmeldung ohne Partnername).
     */
    private static @Nullable List<SpielerMitVerein> zuSpielerListe(RegistrationDto reg, Formation formation) {
        if (istLeer(reg.firstName()) || istLeer(reg.lastName())) {
            return null;
        }
        List<SpielerMitVerein> spieler = new ArrayList<>();
        spieler.add(neuerSpieler(reg.firstName(), reg.lastName(), reg.club(), reg.licenseNr()));

        if (formation.getAnzSpieler() >= 2) {
            if (istLeer(reg.partnerFirstName()) || istLeer(reg.partnerLastName())) {
                return null;
            }
            spieler.add(neuerSpieler(reg.partnerFirstName(), reg.partnerLastName(), reg.club(), null));
        }
        if (formation.getAnzSpieler() >= 3) {
            if (istLeer(reg.partner2FirstName()) || istLeer(reg.partner2LastName())) {
                return null;
            }
            spieler.add(neuerSpieler(reg.partner2FirstName(), reg.partner2LastName(), reg.club(), null));
        }
        return spieler;
    }

    private static boolean istLeer(@Nullable String wert) {
        return wert == null || wert.isBlank();
    }

    /**
     * {@code nr=0}: Online-Anmeldungen haben keinen Bezug zu einem lokalen Spieler-DB-Datensatz. Namen
     * werden getrimmt, damit Leerzeichen aus dem Online-Formular weder in der Meldeliste landen noch
     * den Namensabgleich mit bestehenden Zeilen verhindern.
     */
    private static SpielerMitVerein neuerSpieler(String vorname, String nachname, @Nullable String vereinName,
            @Nullable String lizenznr) {
        return new SpielerMitVerein(0, vorname.strip(), nachname.strip(), null,
                vereinName == null ? null : vereinName.strip(), List.of(), List.of(), lizenznr);
    }

    private static void zeigeFehler(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }

}
