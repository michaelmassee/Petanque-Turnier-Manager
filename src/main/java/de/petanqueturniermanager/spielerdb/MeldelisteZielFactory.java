package de.petanqueturniermanager.spielerdb;

import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.AbstractMeleeAnmeldungSheet;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetUpdate;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetUpdate;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erkennt anhand des Dokumenten-Turniersystems das passende Konfigurations-Sheet
 * und baut einen {@link MeldelisteZiel}-Adapter, der auf das Standard-Meldeliste-Sheet
 * schreibt. Liefert {@link Optional#empty()} wenn das Dokument kein bekanntes
 * (unterstütztes) Turniersystem hat oder kein Meldeliste-Sheet existiert.
 *
 * <p>Layout-Werte (Formation, Teamname-/Vereinsnamen-Anzeige) werden über die
 * system-spezifische {@code *KonfigurationSheet}-Klasse gelesen — damit greifen
 * deren Konfig-Defaults korrekt, und gleichzeitig persistiert
 * {@code BasePropertiesSpalte.readStringProperty} fehlende Werte einmalig in die
 * UserDefinedProperties (siehe {@code initStringPropertyIfAbsent}).
 */
public final class MeldelisteZielFactory {

    private static final Logger logger = LogManager.getLogger(MeldelisteZielFactory.class);

    private MeldelisteZielFactory() {}

    /**
     * Startet den system-passenden "Meldeliste aktualisieren"-Lauf als {@link SheetRunner}
     * (asynchron, eigener Thread, eigener Blattschutz-Scope) — vergibt dabei u.&nbsp;a. die
     * Team-Nr neu geschriebener Bloecke. Gemeinsam genutzt von der Spieler-DB- und der
     * PTM-Online-Integration, die beide per {@link MeldelisteZiel#schreibeBlock} neue Zeilen
     * anlegen und anschliessend diesen Lauf brauchen. LIGA/KEIN sind nicht erreichbar, da
     * {@link #fuerAktivesSheet} dafuer kein Ziel liefert; der Zweig bleibt defensiv ohne Aktion.
     *
     * @return der gestartete {@link SheetRunner}, oder {@code null} wenn für {@code ts} kein
     *         Update-Lauf existiert (LIGA/KEIN) oder das Starten fehlschlug.
     */
    public static @Nullable SheetRunner starteMeldelisteUpdate(WorkingSpreadsheet ws, TurnierSystem ts) {
        try {
            SheetRunner runner = switch (ts) {
                case SUPERMELEE -> new MeldeListeSheet_Update(ws).testTurnierSystem(TurnierSystem.SUPERMELEE);
                case SCHWEIZER -> new SchweizerMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.SCHWEIZER);
                case FORMULEX -> new FormuleXMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.FORMULEX);
                case KO -> new KoMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.KO).backUpDocument();
                case JGJ -> new JGJMeldeListeSheet_Update(ws).testTurnierSystem(TurnierSystem.JGJ).backUpDocument();
                case MAASTRICHTER -> new MaastrichterMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.MAASTRICHTER).backUpDocument();
                case KASKADE -> new KaskadeMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.KASKADE).backUpDocument();
                case POULE -> new PouleMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.POULE).backUpDocument();
                case TRIPTETE -> new TripTeteMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.TRIPTETE).backUpDocument();
                case LIGA, KEIN -> null;
            };
            if (runner != null) {
                runner.start();
            }
            return runner;
        } catch (GenerateException e) {
            logger.error("Meldeliste-Aktualisieren nach Übernahme neuer Meldungen fehlgeschlagen", e);
            return null;
        }
    }

    /**
     * Führt den system-passenden "Meldeliste aktualisieren"-Lauf synchron im aufrufenden Thread aus
     * (gleiche Arbeit wie {@code doRun()} des Update-Runners). Für Aufrufer, die selbst schon als
     * {@link SheetRunner} laufen und daher keinen zweiten Runner starten können.
     */
    public static void aktualisiereMeldelisteSynchron(WorkingSpreadsheet ws, TurnierSystem ts)
            throws GenerateException {
        switch (ts) {
            case SUPERMELEE -> new MeldeListeSheet_Update(ws).aktualisiereFuerAktivenSpieltag();
            case SCHWEIZER -> new SchweizerMeldeListeSheetUpdate(ws).vollstaendigAktualisieren();
            case FORMULEX -> new FormuleXMeldeListeSheetUpdate(ws).vollstaendigAktualisieren();
            case KO -> new KoMeldeListeSheetUpdate(ws).aktualisiereMeldeliste();
            case JGJ -> new JGJMeldeListeSheet_Update(ws).upDateSheet();
            case MAASTRICHTER -> new MaastrichterMeldeListeSheetUpdate(ws).vollstaendigAktualisieren();
            case KASKADE -> new KaskadeMeldeListeSheetUpdate(ws).vollstaendigAktualisieren();
            case POULE -> new PouleMeldeListeSheetUpdate(ws).vollstaendigAktualisieren();
            case TRIPTETE -> new TripTeteMeldeListeSheetUpdate(ws).upDateSheet();
            case LIGA, KEIN -> throw new GenerateException("Meldeliste konnte nicht aktualisiert werden");
        }
    }

    /**
     * Sync-Ziel für PTM-Online: bei aktiver Mêlée-Anmeldung das Mêlée-Anmeldung-Sheet (ein Online-Mêlée-Turnier
     * nimmt nur Einzelspieler an), sonst die Meldeliste wie {@link #fuerAktivesSheet}. Das Mêlée-Ziel entsteht
     * auch, wenn das Sheet noch fehlt; {@link #aktualisiereZielSynchron} legt es an.
     */
    public static Optional<MeldelisteZiel> fuerPtmOnline(WorkingSpreadsheet ws) {
        if (MeleeAnmeldungKonfiguration.istAktiv(ws)) {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            Optional<String> schluessel = MeleeAnmeldungKonfiguration.metadatenSchluessel(ts);
            if (schluessel.isPresent()) {
                return Optional.of(new MeleeAnmeldungZiel(ws, ts, schluessel.get()));
            }
        }
        return fuerAktivesSheet(ws);
    }

    /**
     * Synchrones Aktualisieren des Ziels: beim Mêlée-Ziel datenerhaltender Neuaufbau des Mêlée-Anmeldung-Sheets
     * (legt es bei Bedarf an), sonst {@link #aktualisiereMeldelisteSynchron}.
     */
    public static void aktualisiereZielSynchron(WorkingSpreadsheet ws, TurnierSystem ts, MeldelisteZiel ziel)
            throws GenerateException {
        if (!(ziel instanceof MeleeAnmeldungZiel)) {
            aktualisiereMeldelisteSynchron(ws, ts);
            return;
        }
        AbstractMeleeAnmeldungSheet meleeSheet = MeleeAnmeldungKonfiguration.meleeAnmeldungSheet(ws)
                .orElseThrow(() -> new GenerateException("Mêlée-Anmeldung konnte nicht aktualisiert werden"));
        meleeSheet.generate();
    }

    public static Optional<MeldelisteZiel> fuerAktivesSheet(WorkingSpreadsheet ws) {
        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        if (ts == null || ts == TurnierSystem.KEIN) {
            return Optional.empty();
        }
        Optional<MeldelisteLayout> layoutOpt = leseLayout(ts, ws);
        if (layoutOpt.isEmpty() || layoutOpt.get().formation() == Formation.NUR_TEAMNAME) {
            return Optional.empty();
        }
        MeldelisteLayout l = layoutOpt.get();
        return SheetMeldelisteAdapter.fuer(ws, SheetNamen.meldeliste(), ts,
                l.formation(), l.teamnameAktiv(), l.vereinsnameAktiv());
    }

    /**
     * Ergänzt beim normalen Meldelisten-Refresh genau einmal eine lokale UUID für jede belegte
     * Zeile. Die UUID ist die einzige stabile Identität für die PTM-Online-Zuordnung.
     */
    public static void erstelleLokalePtmOnlineUuids(WorkingSpreadsheet ws) throws GenerateException {
        Optional<MeldelisteZiel> ziel = fuerAktivesSheet(ws);
        if (ziel.isEmpty()) {
            return;
        }
        try {
            ziel.get().getOderErzeugeLokaleUuids(belegteZeilen(ziel.get()));
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    /** 1-basierte Sheet-Zeilen mit mindestens einem eingetragenen Spieler, ohne Duplikate. */
    private static Set<Integer> belegteZeilen(MeldelisteZiel ziel) {
        return new LinkedHashSet<>(ziel.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::zeile1Basiert)
                .toList());
    }

    /**
     * Sichert die vorhandenen UUIDs nach lokaler Spieler-/Teamnummer und räumt danach ihre Spalte – für Umbauten
     * dynamischer Meldelisten, bei denen die UUID-Spalte wandert (Supermelee: neuer Spieltag belegt sie). Legt keine
     * neuen UUIDs an; das übernimmt {@link #erstelleLokalePtmOnlineUuids} nach dem Umbau.
     */
    public static Map<Integer, String> sichereUndEntferneLokalePtmOnlineUuids(WorkingSpreadsheet ws)
            throws GenerateException {
        Map<Integer, String> ergebnis = new LinkedHashMap<>();
        Optional<MeldelisteZiel> ziel = fuerAktivesSheet(ws);
        if (ziel.isEmpty()) {
            return ergebnis;
        }
        try {
            Map<Integer, Integer> nrProZeile = new LinkedHashMap<>();
            for (int zeile : belegteZeilen(ziel.get())) {
                int nr = ziel.get().getTeamNrAusZeile(zeile);
                if (nr > 0) {
                    nrProZeile.put(zeile, nr);
                }
            }
            ziel.get().leseLokaleUuids(nrProZeile.keySet())
                    .forEach((zeile, uuid) -> ergebnis.put(nrProZeile.get(zeile), uuid));
            ziel.get().entferneLokaleUuids();
            return ergebnis;
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    /** Stellt beim dynamischen Spaltenumbau gesicherte UUIDs wieder an ihren Spieler-/Teamnummern her. */
    public static void stelleLokalePtmOnlineUuidsWiederher(WorkingSpreadsheet ws, Map<Integer, String> uuids)
            throws GenerateException {
        if (uuids.isEmpty()) {
            return;
        }
        Optional<MeldelisteZiel> ziel = fuerAktivesSheet(ws);
        if (ziel.isEmpty()) {
            return;
        }
        try {
            Map<Integer, String> uuidProZeile = new LinkedHashMap<>();
            for (int zeile : belegteZeilen(ziel.get())) {
                String uuid = uuids.get(ziel.get().getTeamNrAusZeile(zeile));
                if (uuid != null) {
                    uuidProZeile.put(zeile, uuid);
                }
            }
            ziel.get().setzeLokaleUuids(uuidProZeile);
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }

    /**
     * Liefert das Turniersystem des aktiven Dokuments, sofern es überhaupt eines hat,
     * aber die Spieler-DB-Übernahme nicht unterstützt (z.&nbsp;B. Liga). Für Dokumente
     * ohne Turniersystem (Stammdaten o.&nbsp;ä.) und für unterstützte Systeme wird
     * {@link Optional#empty()} zurückgegeben.
     */
    public static Optional<TurnierSystem> nichtUnterstuetztesSystem(WorkingSpreadsheet ws) {
        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        if (ts == null || ts == TurnierSystem.KEIN) {
            return Optional.empty();
        }
        return switch (ts) {
            case LIGA -> Optional.of(ts);
            default -> Optional.empty();
        };
    }

    /**
     * Liefert das Turniersystem des aktiven Dokuments, sofern die Meldeliste-Formation
     * {@link Formation#NUR_TEAMNAME} konfiguriert ist – dort gibt es keine einzelnen
     * Spieler, die Spieler-DB-Übernahme ist daher sinnlos. Für alle anderen Fälle
     * (inkl. Liga, das über {@link #nichtUnterstuetztesSystem} separat gemeldet wird)
     * liefert die Methode {@link Optional#empty()}.
     */
    public static Optional<TurnierSystem> formationNichtUnterstuetzt(WorkingSpreadsheet ws) {
        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        if (ts == null || ts == TurnierSystem.KEIN || ts == TurnierSystem.LIGA) {
            return Optional.empty();
        }
        Optional<MeldelisteLayout> layoutOpt = leseLayout(ts, ws);
        if (layoutOpt.isPresent() && layoutOpt.get().formation() == Formation.NUR_TEAMNAME) {
            return Optional.of(ts);
        }
        return Optional.empty();
    }

    /**
     * Layout-Triplet aus dem system-spezifischen KonfigurationSheet.
     */
    private record MeldelisteLayout(Formation formation, boolean teamnameAktiv, boolean vereinsnameAktiv) {}

    /**
     * Liest das Meldeliste-Layout aus dem zum Turniersystem passenden Konfigurations-Sheet.
     * Supermelee hat keine konfigurierbare Formation — dort gilt immer
     * {@link Formation#MELEE} und es gibt keine Teamname-/Vereinsname-Spalten.
     * Für Systeme ohne Spieler-DB-Übernahme-Unterstützung (Liga)
     * liefert die Methode {@link Optional#empty()}.
     */
    private static Optional<MeldelisteLayout> leseLayout(TurnierSystem ts, WorkingSpreadsheet ws) {
        try {
            return switch (ts) {
                case SUPERMELEE -> Optional.of(new MeldelisteLayout(Formation.MELEE, false, false));
                case KO -> {
                    KoKonfigurationSheet k = new KoKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case SCHWEIZER -> {
                    SchweizerKonfigurationSheet k = new SchweizerKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case MAASTRICHTER -> {
                    MaastrichterKonfigurationSheet k = new MaastrichterKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case JGJ -> {
                    JGJKonfigurationSheet k = new JGJKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case POULE -> {
                    PouleKonfigurationSheet k = new PouleKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case KASKADE -> {
                    KaskadeKonfigurationSheet k = new KaskadeKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case FORMULEX -> {
                    FormuleXKonfigurationSheet k = new FormuleXKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(k.getMeldeListeFormation(),
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case TRIPTETE -> {
                    TripTeteKonfigurationSheet k = new TripTeteKonfigurationSheet(ws);
                    yield Optional.of(new MeldelisteLayout(Formation.TRIPLETTE,
                            k.isMeldeListeTeamnameAnzeigen(), k.isMeldeListeVereinsnameAnzeigen()));
                }
                case LIGA, KEIN -> Optional.empty();
            };
        } catch (RuntimeException e) {
            logger.warn("Konfig-Layout für {} konnte nicht gelesen werden", ts, e);
            return Optional.empty();
        }
    }
}
