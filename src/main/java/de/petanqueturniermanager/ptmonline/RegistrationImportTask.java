/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.time.Duration;
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
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
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
    /** Die Prüfung vor dem Turnierstart hält die Auslosung auf – bei schlechtem Netz nicht länger als so. */
    static final Duration TIMEOUT_VOR_TURNIERSTART = Duration.ofSeconds(8);

    private RegistrationImportTask() {}

    @FunctionalInterface
    public interface MeldelistenAktualisierung {
        void aktualisieren() throws GenerateException, InterruptedException;
    }

    /**
     * In die Meldeliste geschriebene Anmeldung. Die Zeile gilt nur bis zum Aktualisieren der Meldeliste, das
     * sortiert und Zeilen verschiebt – danach wird sie über die (eindeutige) Besetzung neu ermittelt.
     */
    private record GeschriebeneAnmeldung(RegistrationDto registration, String besetzung) {}

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
     * @param nachStart       Online-Bezeichnungen der Anmeldungen, die nach dem lokalen Turnierstart eingegangen sind;
     *                        sie werden nie automatisch übernommen (KP-05)
     */
    public record ImportErgebnis(int importiert, List<String> namensgleich, List<String> nichtZuordenbar,
            List<String> nachStart) {

        public ImportErgebnis {
            namensgleich = List.copyOf(namensgleich);
            nichtZuordenbar = List.copyOf(nichtZuordenbar);
            nachStart = List.copyOf(nachStart);
        }

        public ImportErgebnis(int importiert, List<String> namensgleich, List<String> nichtZuordenbar) {
            this(importiert, namensgleich, nichtZuordenbar, List.of());
        }

        /** Nach dem Turnierstart eingegangene Anmeldungen zählen nicht: sie warten auf die Entscheidung der Leitung. */
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
            if (!nachStart.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.anmeldungen_nach_start", String.join(", ", nachStart)));
            }
            return hinweise;
        }
    }

    /**
     * @param onlineAbgelehnt  lokale Bezeichnungen der Meldungen, die PTM-Online beim Anlegen abgelehnt hat, weil
     *                         ein Spieler oder der Teamname dort bereits angemeldet ist
     * @param nichtAngelegt    lokale Bezeichnungen der Meldungen, die wegen des Turnierstarts nicht mehr online
     *                         angelegt wurden; sie bleiben rein lokal (E-13)
     * @param ueberKapazitaet  Anzahl der Nachmeldungen der Turnierleitung über die Online-Kapazität hinaus (T-24)
     */
    public record AbgleichErgebnis(ImportErgebnis importErgebnis, int onlineAngelegt, List<String> onlineAbgelehnt,
            List<String> nichtAngelegt, int ueberKapazitaet) {

        public AbgleichErgebnis {
            onlineAbgelehnt = List.copyOf(onlineAbgelehnt);
            nichtAngelegt = List.copyOf(nichtAngelegt);
        }

        public List<String> hinweise() {
            List<String> hinweise = new ArrayList<>(importErgebnis.hinweise());
            if (!onlineAbgelehnt.isEmpty()) {
                hinweise.add(onlineAbgelehntHinweis(onlineAbgelehnt));
            }
            if (!nichtAngelegt.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.nach_start_lokal", String.join(", ", nichtAngelegt)));
            }
            if (ueberKapazitaet > 0) {
                hinweise.add(I18n.get("ptmonline.hinweis.ueber_kapazitaet", ueberKapazitaet));
            }
            return hinweise;
        }
    }

    private record NeuanlageErgebnis(int angelegt, List<String> abgelehnt, List<String> nichtAngelegt) {}

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
        Integer spieltagNr;
        try {
            spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
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

        new PtmOnlineAbgleichSheetRunner(ws, ts, spieltagNr, config, mapping, tournamentId.get(), zielOpt.get())
                .start();
    }

    /**
     * Manueller Abgleich: importiert neue Online-Meldungen und legt neue lokale Meldungen online an. Anlage und
     * Namenskorrektur sind gezählte Aufträge (T-23, T-24); sie werden im Puffer gespeichert und in Zählerreihenfolge
     * gesendet – zusammen mit noch offenen Aufträgen aus dem Hintergrund.
     */
    public static AbgleichErgebnis fuehreAbgleichDurch(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, AuftragsBestand bestand, String tournamentId, MeldelisteZiel ziel,
            MeldelistenAktualisierung aktualisierung, AbgleichFortschritt fortschritt)
            throws IOException, InterruptedException, GenerateException {
        mapping.sicherstellen();
        ImportErgebnis importErgebnis = fuehreImportDurch(config, mapping, tournamentId, ziel, aktualisierung,
                fortschritt);
        fortschritt.pruefeAbbruch();
        fortschritt.status(I18n.get("ptmonline.fortschritt.lokale_meldungen_anlegen"));
        NeuanlageErgebnis neuanlage = neueLokaleMeldungenAnlegen(config, mapping, bestand, tournamentId, ziel);
        fortschritt.pruefeAbbruch();
        fortschritt.status(I18n.get("ptmonline.fortschritt.details_aktualisieren"));
        int ueberKapazitaet = aktualisiereBezeichnungen(config, mapping, bestand, tournamentId, ziel);
        return new AbgleichErgebnis(importErgebnis, neuanlage.angelegt(), neuanlage.abgelehnt(),
                neuanlage.nichtAngelegt(), ueberKapazitaet);
    }

    /**
     * Prüfung vor dem Turnierstart (erste Spielrunde): liest nur und liefert die bestätigten Online-Anmeldungen, die
     * noch in keiner Meldelistenzeile stehen. Steht eine namensgleiche, noch nicht verknüpfte Zeile in der Meldeliste,
     * wird nichts automatisch verknüpft – die Anmeldung erscheint mit dem Hinweis „möglicherweise identisch“, die
     * Turnierleitung entscheidet im manuellen Abgleich. {@code lastSync} bleibt unverändert. Kurzes Zeitlimit
     * ({@link #TIMEOUT_VOR_TURNIERSTART}): bei schlechtem Netz fragt der Rundenstart ausdrücklich nach.
     *
     * @return Online-Bezeichnungen der fehlenden Anmeldungen, leer wenn die Meldeliste vollständig ist.
     */
    public static List<String> pruefeVorTurnierstart(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = TournamentSyncClient.mitKurzemTimeout(config.baseUrl(), config.apiKey(),
                TIMEOUT_VOR_TURNIERSTART);
        List<RegistrationDto> registrations = client.fetchRegistrations(tournamentId,
                mapping.getLastSync().orElse(Instant.EPOCH));
        Map<String, List<Integer>> vorhandeneZeilen = vorhandeneZeilenNachBesetzung(ziel);
        List<String> fehlend = new ArrayList<>();
        for (RegistrationDto reg : registrations) {
            if (!istImportierbar(reg) || mapping.istBereitsImportiert(reg.id())) {
                continue;
            }
            List<Integer> gleicheZeilen = vorhandeneZeilen.getOrDefault(besetzungsSchluessel(reg), List.of());
            List<Integer> freieZeilen = nichtVerknuepfteZeilen(mapping, ziel, gleicheZeilen, Set.of());
            fehlend.add(freieZeilen.isEmpty() ? onlineBezeichnung(reg)
                    : I18n.get("ptmonline.hinweis.moeglicherweise_identisch", onlineBezeichnung(reg)));
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

    /** Gleicher, reihenfolgeunabhängiger Schlüssel für die aus dem Dokument gebaute Anmeldung. */
    private static String besetzungsSchluessel(NeueOnlineAnmeldung anmeldung) {
        return besetzungsSchluessel(Stream.of(
                        new String[] { anmeldung.firstName(), anmeldung.lastName() },
                        new String[] { anmeldung.partnerFirstName(), anmeldung.partnerLastName() },
                        new String[] { anmeldung.partner2FirstName(), anmeldung.partner2LastName() })
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
        List<String> nachStart = new ArrayList<>();
        Set<String> importierte = mapping.getImportierteOnlineIds();
        for (RegistrationDto reg : registrations) {
            if (!istImportierbar(reg) || importierte.contains(reg.id())) {
                continue;
            }
            if (reg.istNachTurnierstartEingegangen()) {
                nachStart.add(onlineBezeichnung(reg));
            } else {
                neue.add(reg);
            }
        }

        if (neue.isEmpty()) {
            return new ImportErgebnis(0, List.of(), List.of(), nachStart);
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
                    geschrieben.add(new GeschriebeneAnmeldung(reg, besetzung));
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
            return new ImportErgebnis(0, namensgleich, nichtZuordenbar, nachStart);
        }

        fortschritt.status(I18n.get("ptmonline.fortschritt.meldeliste_aktualisieren"));
        aktualisierung.aktualisieren();

        Map<RegistrationDto, Integer> zeileProAnmeldung = zeilenNachAktualisierung(ziel, geschrieben, nichtZuordenbar);
        Map<Integer, String> uuidProZeile;
        try {
            uuidProZeile = ziel.getOderErzeugeLokaleUuids(zeileProAnmeldung.values());
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
        Map<Integer, String> bezeichnungProZeile = lokaleBezeichnungen(ziel);
        List<NeueZuordnung> zuordnungen = new ArrayList<>();
        for (Map.Entry<RegistrationDto, Integer> eintrag : zeileProAnmeldung.entrySet()) {
            RegistrationDto reg = eintrag.getKey();
            int zeile = eintrag.getValue();
            String uuid = uuidProZeile.get(zeile);
            try {
                zuordnungen.add(PtmOnlineRegistrationMapping.neueZuordnung(uuid, ziel.formelTeamNrAusLokalerUuid(uuid),
                        bezeichnungProZeile.getOrDefault(zeile, ""), reg));
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                logger.warn("PTM-Online: Nummernformel für importierte Anmeldung {} nicht ermittelt", reg.id(), e);
                nichtZuordenbar.add(onlineBezeichnung(reg));
            }
        }
        mapping.addMappings(zuordnungen);
        return new ImportErgebnis(zuordnungen.size(), namensgleich, nichtZuordenbar, nachStart);
    }

    /**
     * Ermittelt die Zeilen der geschriebenen Anmeldungen nach dem Aktualisieren der Meldeliste neu. Die beim
     * Schreiben gelieferte Zeile ist dann veraltet, weil das Aktualisieren sortiert – sie zu verwenden, verknüpft
     * Online-Anmeldungen mit fremden Meldelistenzeilen. Geschrieben wird nur eine Besetzung, die noch in keiner
     * Zeile stand; ist sie trotzdem nicht mehr eindeutig auffindbar, wird die Anmeldung nicht zugeordnet.
     */
    private static Map<RegistrationDto, Integer> zeilenNachAktualisierung(MeldelisteZiel ziel,
            List<GeschriebeneAnmeldung> geschrieben, List<String> nichtZuordenbar) {
        Map<String, List<Integer>> zeilenNachBesetzung = vorhandeneZeilenNachBesetzung(ziel);
        Map<RegistrationDto, Integer> zeileProAnmeldung = new LinkedHashMap<>();
        for (GeschriebeneAnmeldung geschriebene : geschrieben) {
            List<Integer> zeilen = zeilenNachBesetzung.getOrDefault(geschriebene.besetzung(), List.of());
            if (zeilen.size() == 1) {
                zeileProAnmeldung.put(geschriebene.registration(), zeilen.getFirst());
            } else {
                logger.error("PTM-Online: Anmeldung {} steht nach dem Aktualisieren in {} Meldelistenzeilen",
                        geschriebene.registration().id(), zeilen.size());
                nichtZuordenbar.add(onlineBezeichnung(geschriebene.registration()));
            }
        }
        return zeileProAnmeldung;
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

    /**
     * Legt alle lokalen Meldungen ohne Online-Zuordnung als Aufträge an (Teilnahme inaktiv – den Check-in meldet die
     * Check-in-Erkennung) und sendet sie. Nach dem Turnierstart lehnt PTM-Online ab; die Meldungen bleiben lokal.
     */
    private static NeuanlageErgebnis neueLokaleMeldungenAnlegen(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, AuftragsBestand bestand, String tournamentId, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        Map<Integer, List<MeldelisteSpielerDaten>> proZeile = spielerProZeile(ziel);
        Map<Integer, String> uuidProZeile = lokaleUuids(ziel, proZeile.keySet());
        Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
        for (Map.Entry<Integer, List<MeldelisteSpielerDaten>> eintrag : proZeile.entrySet()) {
            String uuid = uuidProZeile.get(eintrag.getKey());
            if (uuid == null || onlineIds.containsKey(uuid)) {
                continue;
            }
            PtmOnlineAuftraege.anlage(bestand, tournamentId, uuid, zuOnlineAnmeldung(eintrag.getValue()),
                    OnlineTeilnahme.INAKTIV, null, formelTeamNr(ziel, uuid), bezeichnung(eintrag.getValue()));
        }
        PtmOnlineAuftraege.Anwendung anwendung = sendeSynchron(config, mapping, bestand);
        return new NeuanlageErgebnis(anwendung.angelegt(), anwendung.abgelehnt(), anwendung.nachStart());
    }

    /**
     * Sendet alle offenen Aufträge synchron und wendet die Ergebnisse an. Bricht der Versand ab, wird das wie ein
     * Verbindungsfehler gemeldet; die Aufträge bleiben gespeichert und gehen beim nächsten Versuch mit derselben
     * Auftrags-ID hinaus.
     */
    private static PtmOnlineAuftraege.Anwendung sendeSynchron(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, AuftragsBestand bestand)
            throws IOException, InterruptedException, GenerateException {
        PtmOnlineAuftraege.SynchronerVersand versand = PtmOnlineAuftraege.sendeSynchron(bestand, mapping,
                gebundenerClient(config, mapping), false);
        switch (versand.stopp()) {
            case FERTIG -> {
                return versand.anwendung();
            }
            case DOKUMENT_GEFORKT -> throw new GenerateException(I18n.get("ptmonline.fehler.dokument_geforkt"));
            case BINDUNG_ABGELOEST -> throw new GenerateException(I18n.get("ptmonline.fehler.bindung_abgeloest"));
            case TURNIER_GELOESCHT -> throw new GenerateException(I18n.get("ptmonline.turnier.geloescht"));
            case NETZ, SERVERFEHLER, NICHT_BERECHTIGT -> throw new IOException(
                    I18n.get("ptmonline.fehler.auftraege_nicht_gesendet", versand.stopp()));
            default -> throw new IllegalStateException("Unbekannter Versandstopp " + versand.stopp());
        }
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

    /**
     * Gleicht die Namen zugeordneter Meldungen ab: weicht die Besetzung online ab, überträgt ein Änderungsauftrag den
     * lokalen Stand (das Dokument ist Master) und vermerkt dabei online die lokale UUID (T-21). Danach werden Anzeige
     * und Ausführungsrevisionen des Sync-Blatts aktualisiert.
     *
     * @return Anzahl der zugeordneten Anmeldungen, die online über der Kapazität liegen
     */
    private static int aktualisiereBezeichnungen(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, AuftragsBestand bestand, String tournamentId, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = gebundenerClient(config, mapping);
        Map<String, RegistrationDto> remoteProId = client.fetchRegistrations(tournamentId, null).stream()
                .collect(Collectors.toMap(RegistrationDto::id, registration -> registration));
        Map<Integer, String> bezeichnungProZeile = lokaleBezeichnungen(ziel);
        Map<Integer, String> uuidProZeile = lokaleUuids(ziel, bezeichnungProZeile.keySet());
        Map<Integer, List<MeldelisteSpielerDaten>> spielerProZeile = spielerProZeile(ziel);
        Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
        Map<String, String> bezeichnungProUuid = new LinkedHashMap<>();
        Map<String, RegistrationDto> registrationProUuid = new LinkedHashMap<>();
        Map<String, String> uuidProOnlineId = new LinkedHashMap<>();
        Map<String, String> geaenderteZeilen = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> eintrag : uuidProZeile.entrySet()) {
            int zeile = eintrag.getKey();
            String uuid = eintrag.getValue();
            String onlineId = onlineIds.get(uuid);
            if (onlineId == null) {
                continue;
            }
            String bereitsZugeordnet = uuidProOnlineId.putIfAbsent(onlineId, uuid);
            if (bereitsZugeordnet != null && !bereitsZugeordnet.equals(uuid)) {
                throw new GenerateException(I18n.get("ptmonline.fehler.doppelte_online_zuordnung", onlineId));
            }
            bezeichnungProUuid.put(uuid, bezeichnungProZeile.get(zeile));
            RegistrationDto remote = remoteProId.get(onlineId);
            if (remote == null) {
                throw new GenerateException(I18n.get("ptmonline.fehler.online_zuordnung_fehlend", onlineId));
            }
            registrationProUuid.put(uuid, remote);
            NeueOnlineAnmeldung lokal = zuOnlineAnmeldung(spielerProZeile.get(zeile));
            if (!besetzungsSchluessel(lokal).equals(besetzungsSchluessel(remote))) {
                PtmOnlineAuftraege.aenderung(bestand, tournamentId, uuid, onlineId, lokal, executionRevision(remote),
                        bezeichnungProZeile.get(zeile));
                geaenderteZeilen.put(uuid, bezeichnungProZeile.get(zeile));
            }
        }
        if (!geaenderteZeilen.isEmpty()) {
            Map<String, RegistrationDto> geaendert = sendeSynchron(config, mapping, bestand).geaendert();
            for (Map.Entry<String, String> zeile : geaenderteZeilen.entrySet()) {
                RegistrationDto remote = geaendert.get(zeile.getKey());
                if (remote == null || !onlineIds.get(zeile.getKey()).equals(remote.id())) {
                    throw new GenerateException(I18n.get("ptmonline.fehler.online_namen_abweichend", zeile.getValue()));
                }
                registrationProUuid.put(zeile.getKey(), remote);
            }
        }
        mapping.aktualisiereAnzeigen(bezeichnungProUuid, registrationProUuid);
        Map<String, Integer> revisionen = registrationProUuid.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        eintrag -> executionRevision(eintrag.getValue()), (links, rechts) -> rechts,
                        LinkedHashMap::new));
        mapping.setExecutionRevisionen(revisionen);
        return (int) registrationProUuid.values().stream().filter(RegistrationDto::istUeberKapazitaet).count();
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
