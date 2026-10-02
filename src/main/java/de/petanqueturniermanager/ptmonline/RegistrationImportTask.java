/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.onlinesync.sheet.NeueZuordnung;
import de.petanqueturniermanager.onlinesync.sheet.ZuordnungsZusatz;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.AnmeldungsAbruf;
import de.petanqueturniermanager.ptmonline.dto.KonfliktListeDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.PersonDto;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeleeAnmeldungZiel;
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
    /** Aktiv-Wert „nimmt teil“ (Check-in); bei Supermêlée der Eintrag in der Spalte des Spieltags. */
    private static final int AKTIV_EINGECHECKT = 1;
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
     * @param moeglichIdentisch Online-Bezeichnungen der Anmeldungen, zu denen eine gleichnamige, noch nicht
     *                        verknüpfte Meldelistenzeile steht; die Turnierleitung entscheidet (KP-06 a2)
     */
    public record ImportErgebnis(int importiert, List<String> namensgleich, List<String> nichtZuordenbar,
            List<String> nachStart, List<String> unvollstaendig, List<String> moeglichIdentisch) {

        public ImportErgebnis {
            namensgleich = List.copyOf(namensgleich);
            nichtZuordenbar = List.copyOf(nichtZuordenbar);
            nachStart = List.copyOf(nachStart);
            unvollstaendig = List.copyOf(unvollstaendig);
            moeglichIdentisch = List.copyOf(moeglichIdentisch);
        }

        public ImportErgebnis(int importiert, List<String> namensgleich, List<String> nichtZuordenbar,
                List<String> nachStart) {
            this(importiert, namensgleich, nichtZuordenbar, nachStart, List.of(), List.of());
        }

        public ImportErgebnis(int importiert, List<String> namensgleich, List<String> nichtZuordenbar) {
            this(importiert, namensgleich, nichtZuordenbar, List.of(), List.of(), List.of());
        }

        /**
         * Nach dem Turnierstart eingegangene Anmeldungen zählen nicht: sie stehen in der Konfliktliste, die der
         * Abgleich aus allen Online-Anmeldungen bildet. Offene „möglicherweise identisch“-Fälle zählen, damit die
         * Anmeldung beim nächsten Abgleich wieder geliefert wird.
         */
        public boolean vollstaendig() {
            return namensgleich.isEmpty() && nichtZuordenbar.isEmpty() && moeglichIdentisch.isEmpty();
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
            if (!unvollstaendig.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.team_unvollstaendig", String.join(", ", unvollstaendig)));
            }
            if (!moeglichIdentisch.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.anmeldungen_moeglich_identisch",
                        String.join(", ", moeglichIdentisch)));
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
     * @param online           online stornierte oder auf die Warteliste gesetzte und daher lokal ausgeschlossene
     *                         Meldungen (KP-14) sowie die Konfliktliste des Online-Turniers (KP-06)
     */
    public record AbgleichErgebnis(ImportErgebnis importErgebnis, int onlineAngelegt, List<String> onlineAbgelehnt,
            List<String> nichtAngelegt, int ueberKapazitaet, OnlineBefunde online, List<KonfliktFall> offeneFaelle,
            boolean schliessenAnbieten) {

        public AbgleichErgebnis {
            onlineAbgelehnt = List.copyOf(onlineAbgelehnt);
            nichtAngelegt = List.copyOf(nichtAngelegt);
            offeneFaelle = List.copyOf(offeneFaelle);
        }

        public AbgleichErgebnis(ImportErgebnis importErgebnis, int onlineAngelegt, List<String> onlineAbgelehnt,
                List<String> nichtAngelegt, int ueberKapazitaet) {
            this(importErgebnis, onlineAngelegt, onlineAbgelehnt, nichtAngelegt, ueberKapazitaet, OnlineBefunde.KEINE,
                    List.of(), false);
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
            hinweise.addAll(online.hinweise());
            if (!offeneFaelle.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.konfliktliste", offeneFaelle.size(),
                        SheetNamen.ptmOnlineKonflikte()));
            }
            return hinweise;
        }
    }

    /**
     * Was der Abgleich online vorgefunden hat und die Turnierleitung wissen muss.
     *
     * @param entfernt            lokale Bezeichnungen online stornierter oder wartender, noch nicht eingecheckter und
     *                            daher entfernter Meldungen
     * @param ausgeschlossen      lokale Bezeichnungen online stornierter oder wartender, lokal ausgeschlossener Meldungen
     * @param wiederBestaetigt    lokale Bezeichnungen wieder bestätigter, nicht mehr ausgeschlossener Meldungen
     * @param kontoKonflikte      je Doppelbelegung eines Kontos die Bezeichnungen der betroffenen Anmeldungen
     * @param moeglicheDubletten  je möglicher Dublette die Bezeichnungen der betroffenen Anmeldungen (nur Hinweis)
     */
    public record OnlineBefunde(List<String> entfernt, List<String> ausgeschlossen, List<String> wiederBestaetigt,
            List<String> kontoKonflikte, List<String> moeglicheDubletten, List<String> onlineUebernommen,
            List<NamensKonflikt> namensKonflikte) {

        static final OnlineBefunde KEINE = new OnlineBefunde(List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());

        public OnlineBefunde {
            entfernt = List.copyOf(entfernt);
            ausgeschlossen = List.copyOf(ausgeschlossen);
            wiederBestaetigt = List.copyOf(wiederBestaetigt);
            kontoKonflikte = List.copyOf(kontoKonflikte);
            moeglicheDubletten = List.copyOf(moeglicheDubletten);
            onlineUebernommen = List.copyOf(onlineUebernommen);
            namensKonflikte = List.copyOf(namensKonflikte);
        }

        public List<String> hinweise() {
            List<String> hinweise = new ArrayList<>();
            if (!entfernt.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.online_entfernt", String.join(", ", entfernt)));
            }
            if (!ausgeschlossen.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.online_ausgeschlossen", String.join(", ", ausgeschlossen)));
            }
            if (!wiederBestaetigt.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.online_wieder_bestaetigt", String.join(", ", wiederBestaetigt)));
            }
            if (!kontoKonflikte.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.konto_konflikt", String.join("; ", kontoKonflikte)));
            }
            if (!moeglicheDubletten.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.moegliche_dublette", String.join("; ", moeglicheDubletten)));
            }
            if (!onlineUebernommen.isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.online_namen_uebernommen", String.join("; ", onlineUebernommen)));
            }
            namensKonflikte.forEach(konflikt -> hinweise.add(konflikt.hinweis()));
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
        // Entscheidungen der vorigen Konfliktliste (A-29) wirken in diesem Lauf; die Liste wird am Ende neu gebildet.
        KonfliktSammlung sammlung = KonfliktSammlung.mit(mapping.konfliktListe().leseEntscheidungen());
        wendeLokaleEntscheidungenAn(ziel, mapping, bestand, tournamentId, sammlung);
        ImportLauf importLauf = fuehreImportDurch(config, mapping, tournamentId, ziel, aktualisierung, fortschritt,
                sammlung);
        fortschritt.pruefeAbbruch();
        fortschritt.status(I18n.get("ptmonline.fortschritt.lokale_meldungen_anlegen"));
        NeuanlageErgebnis neuanlage = neueLokaleMeldungenAnlegen(config, mapping, bestand, tournamentId, ziel,
                sammlung);
        fortschritt.pruefeAbbruch();
        fortschritt.status(I18n.get("ptmonline.fortschritt.details_aktualisieren"));
        Bezeichnungsabgleich bezeichnungen = aktualisiereBezeichnungen(config, mapping, bestand, tournamentId, ziel,
                aktualisierung, sammlung);
        meldeNichtUebertragene(sammlung, importLauf.ergebnis());
        if (!sammlung.protokoll().isEmpty()) {
            PtmOnlineAuftraege.entscheidungen(bestand, tournamentId, sammlung.protokoll());
            sendeSynchron(config, mapping, bestand);
        }
        // Zuletzt: auch Ablehnungen dieses Laufs und des Hintergrundversands (A-29).
        PtmOnlineAuftraege.abgelehnteFaelle(bestand).forEach(abgelehnt -> sammlung.melde(abgelehnt.fall()));
        PtmOnlineAuftraege.speichern(bestand, mapping);
        mapping.konfliktListe().schreibe(sammlung.zeilen(), zeitpunktText(Instant.now()));
        OnlineBefunde online = new OnlineBefunde(importLauf.status().entfernt(), importLauf.status().ausgeschlossen(),
                importLauf.status().wiederBestaetigt(), bezeichnungen.kontoKonflikte(),
                bezeichnungen.moeglicheDubletten(), bezeichnungen.onlineUebernommen(), bezeichnungen.namensKonflikte());
        boolean schliessenAnbieten = schliessenAnbieten(bezeichnungen.stand(), LocalDate.now(),
                ziel.getMeldelisteStatus().checkin() > 0);
        return new AbgleichErgebnis(importLauf.ergebnis(), neuanlage.angelegt(), neuanlage.abgelehnt(),
                neuanlage.nichtAngelegt(), bezeichnungen.ueberKapazitaet(), online, sammlung.faelle(),
                schliessenAnbieten);
    }

    /**
     * Vorbeugung (KP-05): Beim Abgleich vor dem Check-in bietet PTM an, die Online-Anmeldung zu schließen, damit bis
     * zum Rundenstart keine Anmeldungen mehr eingehen. Angeboten wird es, solange sie online noch offen ist und der
     * Check-in ansteht – am Turniertag oder sobald lokal eingecheckt wird.
     */
    static boolean schliessenAnbieten(@Nullable SyncStandDto stand, LocalDate heute, boolean lokalEingecheckt) {
        if (stand == null || !stand.istAnmeldungOffen()) {
            return false;
        }
        if (lokalEingecheckt) {
            return true;
        }
        try {
            return stand.date() != null && !LocalDate.parse(stand.date()).isAfter(heute);
        } catch (DateTimeParseException e) {
            logger.warn("PTM-Online: Turniertag {} nicht lesbar", stand.date(), e);
            return false;
        }
    }

    /**
     * Schließt die Online-Anmeldung als gezählter Auftrag und sendet ihn sofort.
     *
     * @return ob PTM-Online das Schließen angenommen hat
     */
    public static boolean schliesseOnlineAnmeldung(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, AuftragsBestand bestand, String tournamentId)
            throws IOException, InterruptedException, GenerateException {
        PtmOnlineAuftraege.anmeldungSchliessen(bestand, tournamentId);
        return sendeSynchron(config, mapping, bestand).abgelehnt(AuftragsArt.ANMELDUNG_SCHLIESSEN).isEmpty();
    }

    private static String zeitpunktText(Instant zeitpunkt) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
                .format(zeitpunkt);
    }

    /**
     * Besetzung, die nicht abgeglichen werden konnte (E-16, KP-16, KP-20): beidseitig unterschiedlich geändert oder
     * online geändert, aber lokal nicht übernehmbar. Keine Seite gewinnt still; beide bleiben unverändert und die
     * zuletzt abgeglichene Besetzung bleibt stehen, bis die Turnierleitung eine Variante bestätigt.
     *
     * @param lokaleUuid        lokale Meldung
     * @param lokal             lokale Besetzung
     * @param online            Online-Besetzung
     * @param onlineVorgewaehlt Online-Stand vorausgewählt, solange die Meldung nicht eingecheckt ist und das Turnier
     *                          nicht läuft; danach der lokale
     * @param grund             übersetzter Grund, warum der Online-Stand nicht übernommen werden konnte, sonst
     *                          {@code null}
     */
    public record NamensKonflikt(String lokaleUuid, String lokal, String online, boolean onlineVorgewaehlt,
            @Nullable String grund) {

        public String hinweis() {
            if (grund != null) {
                return grund;
            }
            return I18n.get("ptmonline.hinweis.namenskonflikt", lokal, online,
                    I18n.get(onlineVorgewaehlt ? "ptmonline.konflikt.vorauswahl.online"
                            : "ptmonline.konflikt.vorauswahl.lokal"));
        }
    }

    /** Ergebnis des Imports samt der übernommenen Online-Statuswechsel. */
    private record ImportLauf(ImportErgebnis ergebnis, StatusUebernahme status) {}

    /**
     * Online stornierte/wartende Meldungen, die entfernt (noch nicht eingecheckt) oder ausgeschlossen (eingecheckt)
     * wurden, und wieder bestätigte, die zurückgeholt wurden.
     */
    private record StatusUebernahme(List<String> entfernt, List<String> ausgeschlossen, List<String> wiederBestaetigt) {}

    /**
     * @param ueberKapazitaet    zugeordnete Anmeldungen über der Online-Kapazität
     * @param kontoKonflikte     Bezeichnungen je Doppelbelegung eines Kontos
     * @param moeglicheDubletten Bezeichnungen je möglicher Dublette
     */
    private record Bezeichnungsabgleich(int ueberKapazitaet, List<String> kontoKonflikte,
            List<String> moeglicheDubletten, List<String> onlineUebernommen, List<NamensKonflikt> namensKonflikte,
            @Nullable SyncStandDto stand) {}

    /**
     * Prüfung vor dem Turnierstart (erste Spielrunde): liest nur und liefert die bestätigten Online-Anmeldungen, die
     * noch in keiner Meldelistenzeile stehen. Steht eine namensgleiche, noch nicht verknüpfte Zeile in der Meldeliste,
     * wird nichts automatisch verknüpft – die Anmeldung erscheint mit dem Hinweis „möglicherweise identisch“, die
     * Turnierleitung entscheidet im manuellen Abgleich. {@code lastSync} bleibt unverändert. Kurzes Zeitlimit
     * ({@link #TIMEOUT_VOR_TURNIERSTART}): bei schlechtem Netz fragt der Rundenstart ausdrücklich nach.
     *
     * Zusätzlich aktualisiert die Prüfung den Vermerk der Doppelbelegungen eines Kontos (KP-06 b), damit die
     * Auslosung und {@link #lokaleAusschlussgruende} den aktuellen Stand kennen.
     *
     * @return fehlende Anmeldungen, leer wenn die Meldeliste vollständig ist
     */
    public static Vorabcheck pruefeVorTurnierstart(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = TournamentSyncClient.mitKurzemTimeout(config.baseUrl(), config.apiKey(),
                TIMEOUT_VOR_TURNIERSTART);
        AnmeldungsAbruf abruf = client.fetchAbgleich(tournamentId, mapping.getLastSync().orElse(Instant.EPOCH));
        List<RegistrationDto> registrations = abruf.registrations();
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
        aktualisiereKontoKonflikte(mapping, abruf.konflikte());
        return new Vorabcheck(fehlend);
    }

    /**
     * Ergebnis der Prüfung vor dem Turnierstart.
     *
     * @param fehlend bestätigte Online-Anmeldungen, die noch in keiner Meldelistenzeile stehen
     */
    public record Vorabcheck(List<String> fehlend) {

        public Vorabcheck {
            fehlend = List.copyOf(fehlend);
        }
    }

    /**
     * Setzt den Vermerk {@link ZuordnungsVermerke#KONTO_KONFLIKT} an allen zugeordneten Meldungen, die online in einer
     * Doppelbelegung stehen, und entfernt ihn – samt Freigabe – an allen anderen (KP-06 b).
     */
    static void aktualisiereKontoKonflikte(PtmOnlineRegistrationMapping mapping, KonfliktListeDto konflikte)
            throws GenerateException {
        Map<String, String> uuidProOnlineId = new LinkedHashMap<>();
        Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
        onlineIds.forEach((uuid, onlineId) -> uuidProOnlineId.putIfAbsent(onlineId, uuid));
        Set<String> betroffen = konflikte.accountConflicts().stream()
                .flatMap(konflikt -> konflikt.registrationIds().stream()).map(uuidProOnlineId::get)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, ZuordnungsZusatz> zusaetze = mapping.getZusaetzeProUuid();
        Map<String, ZuordnungsZusatz> neueVermerke = new LinkedHashMap<>();
        for (String uuid : onlineIds.keySet()) {
            ZuordnungsZusatz zusatz = zusaetze.get(uuid);
            ZuordnungsVermerke bisher = ZuordnungsVermerke.lese(zusatz == null ? null : zusatz.vermerk());
            ZuordnungsVermerke neu = betroffen.contains(uuid) ? bisher.mit(ZuordnungsVermerke.KONTO_KONFLIKT)
                    : bisher.ohne(ZuordnungsVermerke.KONTO_KONFLIKT).ohne(ZuordnungsVermerke.KONTO_FREIGEGEBEN);
            if (!neu.equals(bisher)) {
                neueVermerke.put(uuid, ZuordnungsZusatz.nurVermerk(neu.alsText()));
            }
        }
        mapping.setZusaetze(neueVermerke);
    }

    /**
     * Meldelistenzeilen, die nicht ausgelost werden (P-30, P-42), mit Grund: Doppelbelegung eines Kontos, solange
     * nicht als verschiedene Personen aufgelöst, und Teams mit weniger Personen als die Formation. Liest nur.
     *
     * @return Grund (Art und Bezeichnung) je 1-basierter Zeile
     */
    public static Map<Integer, String> gesperrteZeilen(PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel)
            throws GenerateException {
        Map<Integer, String> gesperrt = new LinkedHashMap<>();
        Map<String, Integer> zeileProUuid = zeileProLokalerUuid(ziel);
        Map<String, String> namen = mapping.getBezeichnungenProUuid();
        for (Map.Entry<String, ZuordnungsZusatz> zusatz : mapping.getZusaetzeProUuid().entrySet()) {
            ZuordnungsVermerke vermerke = ZuordnungsVermerke.lese(zusatz.getValue().vermerk());
            Integer zeile = zeileProUuid.get(zusatz.getKey());
            if (zeile != null && vermerke.hat(ZuordnungsVermerke.KONTO_KONFLIKT)
                    && !vermerke.hat(ZuordnungsVermerke.KONTO_FREIGEGEBEN)) {
                gesperrt.put(zeile, KonfliktArt.KONTO_KONFLIKT.anzeige() + ": "
                        + namen.getOrDefault(zusatz.getKey(), zusatz.getKey()));
            }
        }
        int formationsStaerke = ziel.getFormation().getAnzSpieler();
        if (formationsStaerke > 1) {
            spielerProZeile(ziel).forEach((zeile, spieler) -> {
                if (spieler.size() < formationsStaerke) {
                    gesperrt.putIfAbsent(zeile, KonfliktArt.UNVOLLSTAENDIG.anzeige() + ": " + bezeichnung(spieler));
                }
            });
        }
        return gesperrt;
    }

    /**
     * Ausschlussgründe, die das Dokument kennt (A-29, KP-06, KP-14, E-20, KP-18): online stornierte oder wartende
     * Meldungen, die eingecheckt waren, eingecheckte gesperrte Meldungen ({@link #gesperrteZeilen}) und – bei
     * Mêlée-Anmeldung – eingecheckte Spieler, die noch in keinem Team sind (P-53). Liest nur; Doppelbelegungen kennt
     * es aus dem letzten Abgleich bzw. Vorabcheck.
     */
    public static List<String> lokaleAusschlussgruende(PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel)
            throws GenerateException {
        List<String> gruende = new ArrayList<>();
        Map<String, String> namen = mapping.getBezeichnungenProUuid();
        mapping.getZusaetzeProUuid().forEach((uuid, zusatz) -> {
            ZuordnungsVermerke vermerke = ZuordnungsVermerke.lese(zusatz.vermerk());
            if (vermerke.hat(ZuordnungsVermerke.AUSGESCHLOSSEN) && !vermerke.hat(ZuordnungsVermerke.LOKAL_ENTFERNT)
                    && vermerke.zahl(ZuordnungsVermerke.AUSGESCHLOSSEN).orElse(-1) == 1) {
                gruende.add(KonfliktArt.ONLINE_AUSGESCHLOSSEN.anzeige() + ": " + namen.getOrDefault(uuid, uuid));
            }
        });
        if (ziel instanceof MeleeAnmeldungZiel melee) {
            List<String> ohneTeam = melee.leseMeleeZeilen().stream()
                    .filter(zeile -> zeile.eingecheckt() && !zeile.uebernommen())
                    .map(MeleeAnmeldungZeile::anzeigeName).toList();
            if (!ohneTeam.isEmpty()) {
                gruende.add(I18n.get("ptmonline.vorabcheck.melee_ohne_team", ohneTeam.size(),
                        String.join(", ", ohneTeam)));
            }
        } else {
            gesperrteZeilen(mapping, ziel).forEach((zeile, grund) -> {
                if (ziel.getAktivWertAusZeile(zeile) == 1) {
                    gruende.add(grund);
                }
            });
        }
        return gruende;
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
    private static ImportLauf fuehreImportDurch(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, MeldelisteZiel ziel,
            MeldelistenAktualisierung aktualisierung, AbgleichFortschritt fortschritt, KonfliktSammlung sammlung)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
        Instant abgleichStart = Instant.now();
        // Mit Entscheidungen alle Anmeldungen holen: eine nach dem Start eingegangene liefert der since-Abruf sonst
        // nicht mehr, obwohl die Turnierleitung sie übernehmen will.
        Instant since = sammlung.hatEntscheidungen() ? Instant.EPOCH : mapping.getLastSync().orElse(Instant.EPOCH);
        fortschritt.status(I18n.get("ptmonline.fortschritt.anmeldungen_abrufen"));
        List<RegistrationDto> alle = client.fetchRegistrations(tournamentId, since);
        fortschritt.status(I18n.get("ptmonline.fortschritt.anmeldungen_abgerufen", alle.size()));
        fortschritt.pruefeAbbruch();
        StatusUebernahme status = uebernehmeOnlineStatus(ziel, mapping, alle);
        if (!status.entfernt().isEmpty() && !ziel.istSpielerpool()) {
            // Entfernte Meldungen hinterlassen leere Zeilen; erst das Aktualisieren schließt die Lücken, sonst sieht der
            // folgende Import die Zeilen dahinter nicht.
            aktualisierung.aktualisieren();
        }
        ImportErgebnis ergebnis = uebernehmeAnmeldungen(alle, mapping, ziel, aktualisierung, fortschritt, sammlung);
        if (ergebnis.vollstaendig()) {
            mapping.setLastSync(abgleichStart);
        }
        return new ImportLauf(ergebnis, status);
    }

    /**
     * Ordnet bestätigte, noch nicht importierte Anmeldungen der Meldeliste zu:
     * <ul>
     * <li>keine Zeile mit derselben Besetzung: neue, inaktive Zeile;</li>
     * <li>genau eine noch nicht online verknüpfte Zeile: „möglicherweise identisch“ (KP-06 a2). Ohne Entscheidung
     * wird weder importiert noch die Zeile online angelegt; „verknüpfen“ ordnet die Zeile der Anmeldung zu,
     * „getrennt“ lässt die Zeile normal online anlegen. Im Spielerpool (Supermêlée) ist der gleiche Name derselbe
     * Spieler: die Zeile wird ohne Rückfrage verknüpft, der Spieler behält seine Spieler-Nr über die Spieltage;</li>
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
        return uebernehmeAnmeldungen(registrations, mapping, ziel, aktualisierung, fortschritt,
                KonfliktSammlung.ohneEntscheidungen());
    }

    static ImportErgebnis uebernehmeAnmeldungen(List<RegistrationDto> registrations,
            PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel, MeldelistenAktualisierung aktualisierung,
            AbgleichFortschritt fortschritt, KonfliktSammlung sammlung) throws GenerateException, InterruptedException {
        List<RegistrationDto> neue = new ArrayList<>();
        List<String> nachStart = new ArrayList<>();
        Set<String> importierte = mapping.getImportierteOnlineIds();
        for (RegistrationDto reg : registrations) {
            if (!istImportierbar(reg) || importierte.contains(reg.id())) {
                continue;
            }
            if (!reg.istNachTurnierstartEingegangen()) {
                neue.add(reg);
            } else if (sammlung.entscheidung(nachStartFall(reg)).isPresent()) {
                neue.add(reg);
                sammlung.angewendet(Entscheidung.UEBERNEHMEN, null, reg.id());
            } else {
                nachStart.add(onlineBezeichnung(reg));
            }
        }

        if (neue.isEmpty()) {
            return new ImportErgebnis(0, List.of(), List.of(), nachStart);
        }

        List<GeschriebeneAnmeldung> geschrieben = new ArrayList<>();
        Set<Integer> geschriebeneZeilen = new HashSet<>();
        List<String> namensgleich = new ArrayList<>();
        List<String> nichtZuordenbar = new ArrayList<>();
        List<String> unvollstaendig = new ArrayList<>();
        List<String> moeglichIdentisch = new ArrayList<>();
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
            if (spieler.size() < ziel.getFormation().getAnzSpieler()) {
                // Formée-Team mit weniger Personen als die Formation: wird importiert und vor Ort vervollständigt (E-20).
                unvollstaendig.add(onlineBezeichnung(reg));
            }
            String besetzung = besetzungsSchluessel(spieler.stream().map(s -> OnlineSpielerName.schluessel(s.vorname(), s.nachname())));
            List<Integer> gleicheZeilen = vorhandeneZeilen.getOrDefault(besetzung, List.of());
            List<Integer> freieZeilen = nichtVerknuepfteZeilen(mapping, ziel, gleicheZeilen, geschriebeneZeilen);
            if (freieZeilen.size() == 1) {
                int zeile = freieZeilen.getFirst();
                if (ziel.istSpielerpool()) {
                    verknuepfeBestehendeZeile(mapping, ziel, reg, zeile);
                    fortschritt.status(I18n.get("ptmonline.fortschritt.meldung_verknuepft", onlineBezeichnung(reg)));
                    continue;
                }
                String uuid = lokaleUuid(ziel, zeile);
                KonfliktFall fall = new KonfliktFall(KonfliktArt.MOEGLICH_IDENTISCH, uuid, List.of(reg.id()),
                        lokaleBezeichnung(ziel, zeile), onlineBezeichnung(reg),
                        I18n.get("ptmonline.konflikt.hinweis.moeglich_identisch"),
                        List.of(Entscheidung.VERKNUEPFEN, Entscheidung.GETRENNT));
                Optional<Entscheidung> entscheidung = sammlung.entscheidung(fall);
                if (entscheidung.isEmpty()) {
                    sammlung.melde(fall);
                    sammlung.halteZurueck(uuid);
                    moeglichIdentisch.add(onlineBezeichnung(reg));
                } else if (entscheidung.get() == Entscheidung.VERKNUEPFEN) {
                    verknuepfeBestehendeZeile(mapping, ziel, reg, zeile);
                    sammlung.angewendet(Entscheidung.VERKNUEPFEN, uuid, reg.id());
                    fortschritt.status(I18n.get("ptmonline.fortschritt.meldung_verknuepft", onlineBezeichnung(reg)));
                } else {
                    // Bewusst getrennt: die Zeile wird online angelegt; die gleichnamige Anmeldung bleibt, bis die
                    // Turnierleitung einen der Namen unterscheidbar macht.
                    sammlung.angewendet(Entscheidung.GETRENNT, uuid, reg.id());
                    namensgleich.add(onlineBezeichnung(reg));
                }
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
            return new ImportErgebnis(0, namensgleich, nichtZuordenbar, nachStart, unvollstaendig, moeglichIdentisch);
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
        return new ImportErgebnis(zuordnungen.size(), namensgleich, nichtZuordenbar, nachStart, unvollstaendig,
                moeglichIdentisch);
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
        mapping.setZusaetze(Map.of(uuid, new ZuordnungsZusatz(
                AbgeglicheneBesetzung.ausOnline(reg.personen()).alsText(), "")));
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
        mapping.setZusaetze(Map.of(uuid,
                ZuordnungsZusatz.nurBesetzung(AbgeglicheneBesetzung.ausOnline(reg.personen()).alsText())));
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
     * Bei Spieltagen (Supermêlée) gehören nur die am aktiven Spieltag gemeldeten Zeilen zu dessen Online-Turnier;
     * der übrige Spielerpool bleibt rein lokal.
     */
    private static NeuanlageErgebnis neueLokaleMeldungenAnlegen(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, AuftragsBestand bestand, String tournamentId, MeldelisteZiel ziel,
            KonfliktSammlung sammlung) throws IOException, InterruptedException, GenerateException {
        Map<Integer, List<MeldelisteSpielerDaten>> proZeile = spielerProZeile(ziel);
        Map<Integer, String> uuidProZeile = lokaleUuids(ziel, proZeile.keySet());
        Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
        Optional<Set<Integer>> spieltagZeilen = ziel.zeilenDesAktivenSpieltags();
        for (Map.Entry<Integer, List<MeldelisteSpielerDaten>> eintrag : proZeile.entrySet()) {
            String uuid = uuidProZeile.get(eintrag.getKey());
            if (uuid == null || onlineIds.containsKey(uuid) || sammlung.istZurueckgehalten(uuid)
                    || spieltagZeilen.filter(zeilen -> !zeilen.contains(eintrag.getKey())).isPresent()) {
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

    /** Richtung des Besetzungsabgleichs einer zugeordneten Meldung (E-16). */
    enum BesetzungsRichtung {
        /** Beide Seiten gleich: nichts zu tun. */
        GLEICH,
        /** Nur lokal geändert: lokaler Stand geht online. */
        NACH_ONLINE,
        /** Nur online geändert: Online-Stand wird lokal übernommen. */
        NACH_LOKAL,
        /** Beidseitig unterschiedlich geändert: Konflikt, keine Seite gewinnt still. */
        KONFLIKT
    }

    /**
     * Vergleicht lokale und Online-Besetzung mit der zuletzt abgeglichenen (E-16). Ohne gespeicherten Stand (ältere
     * Zuordnung) gilt der Online-Stand als zuletzt abgeglichen – das Dokument bleibt dann wie bisher Master.
     */
    static BesetzungsRichtung richtung(AbgeglicheneBesetzung lokal, AbgeglicheneBesetzung online,
            AbgeglicheneBesetzung zuletzt) {
        String l = lokal.namensSchluessel();
        String o = online.namensSchluessel();
        String z = zuletzt.istLeer() ? o : zuletzt.namensSchluessel();
        if (l.equals(o)) {
            return BesetzungsRichtung.GLEICH;
        }
        if (o.equals(z)) {
            return BesetzungsRichtung.NACH_ONLINE;
        }
        if (l.equals(z)) {
            return BesetzungsRichtung.NACH_LOKAL;
        }
        return BesetzungsRichtung.KONFLIKT;
    }

    /**
     * Gleicht die Besetzung zugeordneter Meldungen in beide Richtungen ab (KP-16, KP-20): nur lokal geändert geht per
     * Änderungsauftrag online (samt Benutzer-IDs des aktuellen Online-Stands, T-17), nur online geändert wird lokal
     * übernommen, beidseitig unterschiedlich ist ein Konflikt ohne Datenänderung. Danach werden Anzeige,
     * Ausführungsrevisionen und – außer bei Konflikten – die zuletzt abgeglichene Besetzung aktualisiert.
     */
    private static Bezeichnungsabgleich aktualisiereBezeichnungen(LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, AuftragsBestand bestand, String tournamentId, MeldelisteZiel ziel,
            MeldelistenAktualisierung aktualisierung, KonfliktSammlung sammlung)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = gebundenerClient(config, mapping);
        AnmeldungsAbruf abruf = client.fetchAbgleich(tournamentId, null);
        Map<String, RegistrationDto> remoteProId = abruf.registrations().stream()
                .collect(Collectors.toMap(RegistrationDto::id, registration -> registration));
        Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
        Map<String, ZuordnungsZusatz> zusaetze = mapping.getZusaetzeProUuid();

        List<String> onlineUebernommen = new ArrayList<>();
        List<NamensKonflikt> namensKonflikte = new ArrayList<>();
        Set<String> lokalBehalten = new HashSet<>();
        if (uebernehmeOnlineBesetzungen(ziel, onlineIds, zusaetze, remoteProId, abruf.turnierLaeuft(), sammlung,
                new BesetzungsBefunde(onlineUebernommen, namensKonflikte, lokalBehalten))) {
            // Geänderte Namen können die Reihenfolge der Meldeliste ändern; die UUID wandert mit der Zeile.
            aktualisierung.aktualisieren();
        }
        Set<String> konfliktUuids = namensKonflikte.stream().map(NamensKonflikt::lokaleUuid)
                .collect(Collectors.toSet());

        Map<Integer, String> bezeichnungProZeile = lokaleBezeichnungen(ziel);
        Map<Integer, String> uuidProZeile = lokaleUuids(ziel, bezeichnungProZeile.keySet());
        Map<Integer, List<MeldelisteSpielerDaten>> spielerProZeile = spielerProZeile(ziel);
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
            List<AbgeglicheneBesetzung.Person> lokal = personen(spielerProZeile.get(zeile));
            AbgeglicheneBesetzung online = AbgeglicheneBesetzung.ausOnline(remote.personen());
            boolean nurLokalGeaendert = !konfliktUuids.contains(uuid) && richtung(AbgeglicheneBesetzung.von(lokal),
                    online, zuletzt(zusaetze, uuid)) == BesetzungsRichtung.NACH_ONLINE;
            if (nurLokalGeaendert || lokalBehalten.contains(uuid)) {
                // Benutzer-IDs aus dem aktuellen Online-Stand: Hat sich online ein Konto gelöst, wäre eine gemerkte ID
                // veraltet und PTM-Online würde den ganzen Auftrag als fremde ID ablehnen (T-17).
                PtmOnlineAuftraege.aenderung(bestand, tournamentId, uuid, onlineId,
                        zuOnlineAnmeldung(spielerProZeile.get(zeile)), online.fuerUebertragung(lokal),
                        executionRevision(remote), bezeichnungProZeile.get(zeile));
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
        // Der Online-Stand nach dem Abgleich ist die zuletzt abgeglichene Besetzung – samt neuer Benutzer-IDs, z. B.
        // nachdem online eine Slot-E-Mail eingetragen wurde (P-47). Ein offener Konflikt behält den alten Stand.
        Map<String, ZuordnungsZusatz> besetzungen = new LinkedHashMap<>();
        registrationProUuid.forEach((uuid, registration) -> {
            if (!konfliktUuids.contains(uuid)) {
                besetzungen.put(uuid, ZuordnungsZusatz.nurBesetzung(
                        AbgeglicheneBesetzung.ausOnline(registration.personen()).alsText()));
            }
        });
        mapping.setZusaetze(besetzungen);
        int ueberKapazitaet = (int) registrationProUuid.values().stream().filter(RegistrationDto::istUeberKapazitaet)
                .count();
        Map<String, String> bezeichnungProOnlineId = new LinkedHashMap<>();
        remoteProId.forEach((onlineId, registration) -> bezeichnungProOnlineId.put(onlineId,
                Optional.ofNullable(uuidProOnlineId.get(onlineId)).map(bezeichnungProUuid::get)
                        .orElseGet(() -> onlineBezeichnung(registration))));
        sammleOffeneFaelle(ziel, mapping, abruf, bezeichnungProOnlineId, spielerProZeile, uuidProZeile, sammlung);
        return new Bezeichnungsabgleich(ueberKapazitaet,
                abruf.konflikte().accountConflicts().stream()
                        .map(konflikt -> bezeichnungen(konflikt.registrationIds(), bezeichnungProOnlineId)).toList(),
                abruf.konflikte().possibleDuplicates().stream()
                        .map(dublette -> bezeichnungen(dublette.registrationIds(), bezeichnungProOnlineId)).toList(),
                onlineUebernommen, namensKonflikte, abruf.stand());
    }

    private static AbgeglicheneBesetzung zuletzt(Map<String, ZuordnungsZusatz> zusaetze, String uuid) {
        ZuordnungsZusatz zusatz = zusaetze.get(uuid);
        return zusatz == null ? AbgeglicheneBesetzung.leer() : AbgeglicheneBesetzung.lese(zusatz.besetzung());
    }

    /**
     * Ergebnisse von {@link #uebernehmeOnlineBesetzungen}.
     *
     * @param lokalBehalten lokale UUIDs, deren Besetzung die Turnierleitung im Konflikt behalten hat; sie geht online
     */
    private record BesetzungsBefunde(List<String> onlineUebernommen, List<NamensKonflikt> namensKonflikte,
            Set<String> lokalBehalten) {}

    /**
     * Übernimmt nur online geänderte Besetzungen in die Meldeliste und sammelt beidseitige Konflikte (KP-16, KP-20).
     * Eine Entscheidung der Turnierleitung löst einen Konflikt: „Online übernehmen“ schreibt den Online-Stand in die
     * Meldeliste, „lokal behalten“ überträgt den lokalen.
     *
     * @return ob Namen in der Meldeliste geändert wurden
     */
    private static boolean uebernehmeOnlineBesetzungen(MeldelisteZiel ziel, Map<String, String> onlineIds,
            Map<String, ZuordnungsZusatz> zusaetze, Map<String, RegistrationDto> remoteProId, boolean turnierLaeuft,
            KonfliktSammlung sammlung, BesetzungsBefunde befunde) throws GenerateException {
        Map<Integer, List<MeldelisteSpielerDaten>> spielerProZeile = spielerProZeile(ziel);
        Map<Integer, String> uuidProZeile = lokaleUuids(ziel, spielerProZeile.keySet());
        boolean geaendert = false;
        for (Map.Entry<Integer, String> eintrag : uuidProZeile.entrySet()) {
            int zeile = eintrag.getKey();
            String uuid = eintrag.getValue();
            RegistrationDto remote = Optional.ofNullable(onlineIds.get(uuid)).map(remoteProId::get).orElse(null);
            if (remote == null) {
                continue;
            }
            String lokalText = bezeichnung(spielerProZeile.get(zeile));
            AbgeglicheneBesetzung lokal = AbgeglicheneBesetzung.von(personen(spielerProZeile.get(zeile)));
            AbgeglicheneBesetzung online = AbgeglicheneBesetzung.ausOnline(remote.personen());
            BesetzungsRichtung richtung = richtung(lokal, online, zuletzt(zusaetze, uuid));
            if (richtung == BesetzungsRichtung.KONFLIKT) {
                NamensKonflikt konflikt = new NamensKonflikt(uuid, lokalText, onlineBezeichnung(remote),
                        !turnierLaeuft && ziel.getAktivWertAusZeile(zeile) != 1, null);
                KonfliktFall fall = namensFall(konflikt, remote.id(),
                        List.of(Entscheidung.ONLINE_UEBERNEHMEN, Entscheidung.LOKAL_BEHALTEN));
                Optional<Entscheidung> entscheidung = sammlung.entscheidung(fall);
                if (entscheidung.isEmpty()) {
                    befunde.namensKonflikte().add(konflikt);
                    sammlung.melde(fall);
                    continue;
                }
                sammlung.angewendet(entscheidung.get(), uuid, remote.id());
                if (entscheidung.get() == Entscheidung.LOKAL_BEHALTEN) {
                    befunde.lokalBehalten().add(uuid);
                    continue;
                }
            } else if (richtung != BesetzungsRichtung.NACH_LOKAL) {
                // Gleich: nichts zu tun. Nach online: übernimmt der Änderungsauftrag im Hauptlauf.
                continue;
            }
            geaendert |= uebernehmeOnlineBesetzung(ziel, zeile, uuid, lokalText, remote, sammlung, befunde);
        }
        return geaendert;
    }

    /**
     * Schreibt die Online-Besetzung in die Meldelistenzeile. Lässt sie sich nicht übernehmen (z. B. Mêlée-Spieler
     * schon im Team), ist das ein Konflikt, den nur „lokal behalten“ löst.
     *
     * @return ob die Zeile geändert wurde
     */
    private static boolean uebernehmeOnlineBesetzung(MeldelisteZiel ziel, int zeile, String uuid, String lokalText,
            RegistrationDto remote, KonfliktSammlung sammlung, BesetzungsBefunde befunde) {
        List<SpielerMitVerein> spieler = zuSpielerListe(remote, ziel.getFormation());
        try {
            if (spieler == null) {
                throw new MeldelisteZiel.MeldelisteSchreibException(
                        I18n.get("ptmonline.hinweis.anmeldungen_nicht_zuordenbar", onlineBezeichnung(remote)));
            }
            ziel.ersetzeSpielerNamen(zeile, spieler);
            befunde.onlineUebernommen().add(lokalText + " → " + onlineBezeichnung(remote));
            return true;
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            logger.warn("PTM-Online: Online-Besetzung für Zeile {} nicht übernommen", zeile, e);
            NamensKonflikt konflikt = new NamensKonflikt(uuid, lokalText, onlineBezeichnung(remote), false,
                    e.getMessage());
            KonfliktFall fall = namensFall(konflikt, remote.id(), List.of(Entscheidung.LOKAL_BEHALTEN));
            if (sammlung.entscheidung(fall).isPresent()) {
                sammlung.angewendet(Entscheidung.LOKAL_BEHALTEN, uuid, remote.id());
                befunde.lokalBehalten().add(uuid);
            } else {
                befunde.namensKonflikte().add(konflikt);
                sammlung.melde(fall);
            }
            return false;
        }
    }

    private static KonfliktFall namensFall(NamensKonflikt konflikt, String onlineId, List<Entscheidung> optionen) {
        return new KonfliktFall(KonfliktArt.NAMENSKONFLIKT, konflikt.lokaleUuid(), List.of(onlineId),
                konflikt.lokal(), konflikt.online(), konflikt.hinweis(), optionen);
    }

    private static KonfliktFall nachStartFall(RegistrationDto registration) {
        return new KonfliktFall(KonfliktArt.NACH_START, null, List.of(registration.id()), "",
                onlineBezeichnung(registration), I18n.get("ptmonline.konflikt.hinweis.nach_start"),
                List.of(Entscheidung.UEBERNEHMEN));
    }

    // ── Konfliktliste (A-29) ────────────────────────────────────────────────

    /**
     * Zustand einer zugeordneten Meldung, über den die Turnierleitung entscheidet: online ausgeschlossen (KP-14) oder
     * lokal fehlend (KP-15).
     */
    private record LokalerFall(KonfliktFall fall, String uuid, String onlineId, @Nullable Integer zeile,
            ZuordnungsVermerke vermerke) {}

    /** Online ausgeschlossene und lokal fehlende zugeordnete Meldungen, nach dem aktuellen Stand des Dokuments. */
    private static List<LokalerFall> lokaleFaelle(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping)
            throws GenerateException {
        Map<String, ZuordnungsZusatz> zusaetze = mapping.getZusaetzeProUuid();
        Map<String, Integer> zeileProUuid = zeileProLokalerUuid(ziel);
        Map<String, String> namen = mapping.getBezeichnungenProUuid();
        Map<String, String> rohStatus = mapping.getRohStatusProUuid();
        List<LokalerFall> faelle = new ArrayList<>();
        for (Map.Entry<String, String> zuordnung : mapping.getOnlineIdsProUuid().entrySet()) {
            String uuid = zuordnung.getKey();
            String onlineId = zuordnung.getValue();
            ZuordnungsZusatz zusatz = zusaetze.get(uuid);
            ZuordnungsVermerke vermerke = ZuordnungsVermerke.lese(zusatz == null ? null : zusatz.vermerk());
            if (vermerke.hat(ZuordnungsVermerke.LOKAL_ENTFERNT)) {
                continue;
            }
            Integer zeile = zeileProUuid.get(uuid);
            String status = rohStatus.get(uuid);
            String name = namen.getOrDefault(uuid, "");
            if (zeile == null) {
                List<Entscheidung> optionen = OnlineAnmeldeStatus.istStorniert(status)
                        ? List.of(Entscheidung.NUR_LOKAL_ENTFERNEN)
                        : List.of(Entscheidung.NUR_LOKAL_ENTFERNEN, Entscheidung.ONLINE_STORNIEREN);
                faelle.add(new LokalerFall(new KonfliktFall(KonfliktArt.LOKAL_FEHLEND, uuid, List.of(onlineId), name,
                        OnlineAnmeldeStatus.anzeige(status), I18n.get("ptmonline.konflikt.hinweis.lokal_fehlend"),
                        optionen), uuid, onlineId, null, vermerke));
            } else if (vermerke.hat(ZuordnungsVermerke.AUSGESCHLOSSEN)) {
                faelle.add(new LokalerFall(new KonfliktFall(KonfliktArt.ONLINE_AUSGESCHLOSSEN, uuid,
                        List.of(onlineId), name, OnlineAnmeldeStatus.anzeige(status),
                        I18n.get("ptmonline.konflikt.hinweis.online_ausgeschlossen"),
                        List.of(Entscheidung.BEHALTEN)), uuid, onlineId, zeile, vermerke));
            }
        }
        return faelle;
    }

    /**
     * Wendet die Entscheidungen zu online ausgeschlossenen und lokal fehlenden Meldungen an: „bewusst behalten“ hebt
     * den Ausschluss auf und stellt den alten Check-in-Zustand her (KP-14), „nur lokal entfernen“ und „online
     * stornieren“ vermerken die Meldung als lokal entfernt, Letzteres mit einem Storno-Auftrag (KP-15).
     */
    private static void wendeLokaleEntscheidungenAn(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            AuftragsBestand bestand, String tournamentId, KonfliktSammlung sammlung) throws GenerateException {
        if (!sammlung.hatEntscheidungen()) {
            return;
        }
        for (PtmOnlineAuftraege.AbgelehnterFall abgelehnt : PtmOnlineAuftraege.abgelehnteFaelle(bestand)) {
            if (sammlung.entscheidung(abgelehnt.fall()).isPresent()) {
                bestand.quittiere(abgelehnt.auftragsId());
                sammlung.angewendet(Entscheidung.ZUR_KENNTNIS, abgelehnt.fall().lokaleUuid(), null);
            }
        }
        Map<String, Integer> revisionen = mapping.getExecutionRevisionenProUuid();
        Map<String, ZuordnungsZusatz> neueVermerke = new LinkedHashMap<>();
        for (LokalerFall lokal : lokaleFaelle(ziel, mapping)) {
            Optional<Entscheidung> entscheidung = sammlung.entscheidung(lokal.fall());
            if (entscheidung.isEmpty()) {
                continue;
            }
            ZuordnungsVermerke vermerke = switch (entscheidung.get()) {
                case BEHALTEN -> {
                    stelleAktivWertWiederHer(ziel, lokal);
                    yield lokal.vermerke().ohne(ZuordnungsVermerke.AUSGESCHLOSSEN).mit(ZuordnungsVermerke.BEHALTEN);
                }
                case ONLINE_STORNIEREN -> {
                    PtmOnlineAuftraege.storno(bestand, tournamentId, lokal.uuid(), lokal.onlineId(),
                            revisionen.getOrDefault(lokal.uuid(), 1));
                    yield lokal.vermerke().mit(ZuordnungsVermerke.LOKAL_ENTFERNT);
                }
                default -> lokal.vermerke().mit(ZuordnungsVermerke.LOKAL_ENTFERNT);
            };
            neueVermerke.put(lokal.uuid(), ZuordnungsZusatz.nurVermerk(vermerke.alsText()));
            sammlung.angewendet(entscheidung.get(), lokal.uuid(), lokal.onlineId());
        }
        mapping.setZusaetze(neueVermerke);
    }

    private static void stelleAktivWertWiederHer(MeldelisteZiel ziel, LokalerFall lokal) throws GenerateException {
        if (lokal.zeile() == null) {
            return;
        }
        try {
            ziel.stelleAktivWertWiederHer(lokal.zeile(),
                    lokal.vermerke().zahl(ZuordnungsVermerke.AUSGESCHLOSSEN).orElse(-1));
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    /**
     * Ergänzt die Konfliktliste um die Fälle, die sich aus dem Stand nach dem Abgleich ergeben: online ausgeschlossen,
     * lokal fehlend, nach dem Start eingegangen, Doppelbelegungen, mögliche Dubletten und unvollständige Teams.
     */
    private static void sammleOffeneFaelle(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            AnmeldungsAbruf abruf, Map<String, String> bezeichnungProOnlineId,
            Map<Integer, List<MeldelisteSpielerDaten>> spielerProZeile, Map<Integer, String> uuidProZeile,
            KonfliktSammlung sammlung) throws GenerateException {
        lokaleFaelle(ziel, mapping).forEach(lokal -> sammlung.melde(lokal.fall()));
        Set<String> importierte = mapping.getImportierteOnlineIds();
        abruf.registrations().stream()
                .filter(registration -> istImportierbar(registration) && registration.istNachTurnierstartEingegangen()
                        && !importierte.contains(registration.id()))
                .forEach(registration -> sammlung.melde(nachStartFall(registration)));
        aktualisiereKontoKonflikte(mapping, abruf.konflikte());
        Map<String, String> uuidProOnlineId = new LinkedHashMap<>();
        mapping.getOnlineIdsProUuid().forEach((uuid, onlineId) -> uuidProOnlineId.putIfAbsent(onlineId, uuid));
        Map<String, ZuordnungsZusatz> freigaben = new LinkedHashMap<>();
        for (KonfliktListeDto.KontoKonflikt konflikt : abruf.konflikte().accountConflicts()) {
            KonfliktFall fall = new KonfliktFall(KonfliktArt.KONTO_KONFLIKT, null, konflikt.registrationIds(), "",
                    bezeichnungen(konflikt.registrationIds(), bezeichnungProOnlineId),
                    I18n.get("ptmonline.konflikt.hinweis.konto_konflikt"),
                    List.of(Entscheidung.VERSCHIEDENE_PERSONEN));
            if (sammlung.entscheidung(fall).isEmpty()) {
                sammlung.melde(fall);
                continue;
            }
            // Verschiedene Personen (KP-06 c): die beteiligten Meldungen sind wieder auslosbar.
            Map<String, ZuordnungsZusatz> zusaetze = mapping.getZusaetzeProUuid();
            for (String onlineId : konflikt.registrationIds()) {
                String uuid = uuidProOnlineId.get(onlineId);
                if (uuid != null) {
                    ZuordnungsZusatz zusatz = zusaetze.get(uuid);
                    freigaben.put(uuid, ZuordnungsZusatz.nurVermerk(ZuordnungsVermerke
                            .lese(zusatz == null ? null : zusatz.vermerk()).mit(ZuordnungsVermerke.KONTO_FREIGEGEBEN)
                            .alsText()));
                }
                sammlung.angewendet(Entscheidung.VERSCHIEDENE_PERSONEN, uuid, onlineId);
            }
        }
        mapping.setZusaetze(freigaben);
        abruf.konflikte().possibleDuplicates().forEach(dublette -> sammlung.melde(new KonfliktFall(
                KonfliktArt.MOEGLICHE_DUBLETTE, null, dublette.registrationIds(), "",
                bezeichnungen(dublette.registrationIds(), bezeichnungProOnlineId),
                I18n.get("ptmonline.konflikt.hinweis.moegliche_dublette"), List.of())));
        int formationsStaerke = ziel.getFormation().getAnzSpieler();
        if (formationsStaerke > 1) {
            Map<String, String> onlineIds = mapping.getOnlineIdsProUuid();
            spielerProZeile.forEach((zeile, spieler) -> {
                String uuid = uuidProZeile.get(zeile);
                if (uuid != null && spieler.size() < formationsStaerke) {
                    String onlineId = onlineIds.get(uuid);
                    sammlung.melde(new KonfliktFall(KonfliktArt.UNVOLLSTAENDIG, uuid,
                            onlineId == null ? List.of() : List.of(onlineId), bezeichnung(spieler), "",
                            I18n.get("ptmonline.konflikt.hinweis.unvollstaendig", spieler.size(), formationsStaerke),
                            List.of()));
                }
            });
        }
    }

    /**
     * Online-Anmeldungen, die dieser Abgleich nicht übernehmen konnte. Lokale Meldungen, deren Online-Anlage
     * abgelehnt wurde, erscheinen als abgelehnte Aufträge.
     */
    private static void meldeNichtUebertragene(KonfliktSammlung sammlung, ImportErgebnis ergebnis) {
        ergebnis.namensgleich().forEach(name -> sammlung.melde(nichtUebertragen("", name,
                "ptmonline.konflikt.hinweis.namensgleich")));
        ergebnis.nichtZuordenbar().forEach(name -> sammlung.melde(nichtUebertragen("", name,
                "ptmonline.konflikt.hinweis.nicht_zuordenbar")));
    }

    private static KonfliktFall nichtUebertragen(String lokal, String online, String hinweisSchluessel) {
        return new KonfliktFall(KonfliktArt.NICHT_UEBERTRAGEN, null, List.of(), lokal, online,
                I18n.get(hinweisSchluessel), List.of());
    }

    private static String bezeichnungen(List<String> onlineIds, Map<String, String> bezeichnungProOnlineId) {
        return onlineIds.stream().map(onlineId -> bezeichnungProOnlineId.getOrDefault(onlineId, onlineId))
                .collect(Collectors.joining(" / "));
    }

    /** Personen einer Meldelistenzeile in Slot-Reihenfolge. */
    private static List<AbgeglicheneBesetzung.Person> personen(List<MeldelisteSpielerDaten> spieler) {
        return spieler.stream().map(daten -> new AbgeglicheneBesetzung.Person(daten.vorname(), daten.nachname(), null))
                .toList();
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

    /**
     * Online stornierte oder auf die Warteliste gesetzte, bereits zugeordnete Meldungen (E-14, KP-14):
     * <ul>
     * <li>Noch nicht eingecheckt: Die Meldung wird aus der Meldeliste entfernt und ihre Zuordnung gelöscht. Meldet sich
     * dasselbe Team neu an oder rückt es nach, wird es wie jede neue Anmeldung übernommen – ohne Doppeleintrag. Bei
     * Supermêlée (Spielerpool) bleibt der Spieler im Pool, nur die Zuordnung zum Spieltag entfällt.</li>
     * <li>Eingecheckt (das Team steht vor Ort): Die Meldung bleibt, wird als abgemeldet markiert, nicht ausgelost und
     * erscheint in der Konfliktliste. Der Aktiv-Wert davor wird im Vermerk gemerkt: Wird die Anmeldung online wieder
     * bestätigt, ist der lokale Check-in-Zustand wieder genau der alte. Behält die Turnierleitung eine Meldung bewusst,
     * wird sie nicht erneut ausgeschlossen.</li>
     * </ul>
     */
    private static StatusUebernahme uebernehmeOnlineStatus(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            List<RegistrationDto> registrations) throws GenerateException {
        Map<String, String> uuidProOnlineId = new LinkedHashMap<>();
        mapping.getOnlineIdsProUuid().forEach((uuid, onlineId) -> uuidProOnlineId.putIfAbsent(onlineId, uuid));
        List<RegistrationDto> zugeordnete = registrations.stream()
                .filter(registration -> uuidProOnlineId.containsKey(registration.id())).toList();
        if (zugeordnete.isEmpty()) {
            return new StatusUebernahme(List.of(), List.of(), List.of());
        }
        Map<String, ZuordnungsZusatz> zusaetze = mapping.getZusaetzeProUuid();
        Map<String, Integer> zeileProUuid = zeileProLokalerUuid(ziel);
        Map<String, ZuordnungsZusatz> neueVermerke = new LinkedHashMap<>();
        List<String> ausgeschlossen = new ArrayList<>();
        List<String> wiederBestaetigt = new ArrayList<>();
        Map<String, Integer> zuEntfernen = new LinkedHashMap<>();
        List<String> entfernt = new ArrayList<>();
        for (RegistrationDto registration : zugeordnete) {
            String uuid = uuidProOnlineId.get(registration.id());
            Integer zeile = zeileProUuid.get(uuid);
            if (zeile == null) {
                continue;
            }
            ZuordnungsZusatz zusatz = zusaetze.get(uuid);
            ZuordnungsVermerke bisher = ZuordnungsVermerke.lese(zusatz == null ? null : zusatz.vermerk());
            ZuordnungsVermerke vermerke = bisher;
            try {
                if (istOnlineAusgeschlossen(registration)) {
                    if (!bisher.hat(ZuordnungsVermerke.AUSGESCHLOSSEN) && !bisher.hat(ZuordnungsVermerke.BEHALTEN)) {
                        int vorher = ziel.getAktivWertAusZeile(zeile);
                        if (vorher != AKTIV_EINGECHECKT) {
                            zuEntfernen.put(uuid, zeile);
                            entfernt.add(lokaleBezeichnung(ziel, zeile) + " (" + onlineStatus(registration) + ")");
                            continue;
                        }
                        ziel.markiereAlsAbgemeldet(zeile);
                        vermerke = bisher.mit(ZuordnungsVermerke.AUSGESCHLOSSEN, Integer.toString(vorher));
                        ausgeschlossen.add(lokaleBezeichnung(ziel, zeile) + " (" + onlineStatus(registration) + ")");
                    }
                } else if (OnlineAnmeldeStatus.istBestaetigt(registration.status())) {
                    if (bisher.hat(ZuordnungsVermerke.AUSGESCHLOSSEN)) {
                        ziel.stelleAktivWertWiederHer(zeile, bisher.zahl(ZuordnungsVermerke.AUSGESCHLOSSEN).orElse(-1));
                        wiederBestaetigt.add(lokaleBezeichnung(ziel, zeile));
                    }
                    vermerke = bisher.ohne(ZuordnungsVermerke.AUSGESCHLOSSEN).ohne(ZuordnungsVermerke.BEHALTEN);
                }
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                throw new GenerateException(e.getMessage());
            }
            if (!vermerke.equals(bisher)) {
                neueVermerke.put(uuid, ZuordnungsZusatz.nurVermerk(vermerke.alsText()));
                mapping.setBezeichnung(uuid, lokaleBezeichnung(ziel, zeile), onlineStatus(registration));
                mapping.setOnlineDetails(uuid, registration);
            }
        }
        mapping.setZusaetze(neueVermerke);
        entferneMeldungen(ziel, mapping, zuEntfernen);
        return new StatusUebernahme(entfernt, ausgeschlossen, wiederBestaetigt);
    }

    /**
     * Entfernt die Meldungen aus der Liste – im Supermêlée-Spielerpool bleiben die Spieler stehen – und danach ihre
     * Zuordnungen.
     */
    private static void entferneMeldungen(MeldelisteZiel ziel, PtmOnlineRegistrationMapping mapping,
            Map<String, Integer> zeileProUuid) throws GenerateException {
        if (zeileProUuid.isEmpty()) {
            return;
        }
        if (!ziel.istSpielerpool()) {
            try {
                for (int zeile : zeileProUuid.values()) {
                    ziel.entferneMeldung(zeile);
                }
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                throw new GenerateException(e.getMessage());
            }
        }
        mapping.entferneZuordnungen(zeileProUuid.keySet());
        logger.info("PTM-Online: {} online stornierte bzw. wartende, nicht eingecheckte Meldungen entfernt",
                zeileProUuid.size());
    }

    /** Online storniert oder auf der Warteliste: lokal von der Auslosung ausgeschlossen (E-14). */
    static boolean istOnlineAusgeschlossen(RegistrationDto registration) {
        return OnlineAnmeldeStatus.istStorniert(registration.status())
                || OnlineAnmeldeStatus.WARTELISTE.apiWert().equals(registration.status());
    }

    /** 1-basierte Meldelistenzeile je vorhandener lokaler UUID; Zeilen ohne UUID fehlen. */
    private static Map<String, Integer> zeileProLokalerUuid(MeldelisteZiel ziel) throws GenerateException {
        List<Integer> zeilen = ziel.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::zeile1Basiert).distinct()
                .toList();
        Map<String, Integer> ergebnis = new LinkedHashMap<>();
        try {
            ziel.leseLokaleUuids(zeilen).forEach((zeile, uuid) -> ergebnis.putIfAbsent(uuid, zeile));
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
        return ergebnis;
    }

    private static String onlineStatus(RegistrationDto registration) {
        return OnlineAnmeldeStatus.anzeige(registration.status());
    }

    /**
     * Baut die Spielerliste aus den Personen-Slots der Anmeldung. Liefert {@code null}, wenn die Personenzahl nicht zur
     * Meldeliste passt: genau eine Person bei Einzelanmeldung, sonst 2 bis zur Formationsstärke (E-20). Ein Formée-Team
     * mit weniger Personen als die Formation ist zulässig und wird unvollständig importiert.
     */
    static @Nullable List<SpielerMitVerein> zuSpielerListe(RegistrationDto reg, Formation formation) {
        List<PersonDto> personen = reg.personen().stream()
                .filter(person -> !istLeer(person.firstName()) && !istLeer(person.lastName())).toList();
        int hoechstens = Math.max(1, formation.getAnzSpieler());
        int mindestens = hoechstens == 1 ? 1 : 2;
        if (personen.size() < mindestens || personen.size() > hoechstens) {
            return null;
        }
        return personen.stream()
                .map(person -> neuerSpieler(person.firstName(), person.lastName(), reg.club(), person.licenseNr()))
                .toList();
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
