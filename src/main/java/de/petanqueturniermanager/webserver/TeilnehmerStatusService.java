package de.petanqueturniermanager.webserver;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Stateless-Service, der die aktuellen Teilnehmerzahlen (angemeldet, aktiv) sowie optional die
 * zugehörigen Anzeige-Namen aus dem Meldelisten-Sheet des Dokuments ermittelt. Die Zählung
 * ({@link #ermitteln(WorkingSpreadsheet)}) bleibt bewusst leichtgewichtig — kein Tabellen-Mapping,
 * keine Diff-Engine — damit der Live-Push der Turnier-Startseite vom Heavy-Refresh-Pfad der
 * Composite-Views entkoppelt bleibt. Die Namensermittlung ({@link #ermittelnNamen(WorkingSpreadsheet)})
 * wird nur bei aktiviertem Checkin-Listen-Feature aufgerufen und nutzt für die Formatierung
 * turniersystem-spezifische {@code *KonfigurationSheet}-Klassen (leichte Property-Delegation, kein
 * Sheet-Vollaufbau) über {@link TeilnehmerNamenLeser}, damit Namen identisch wie in der Checkin-Liste
 * dargestellt werden.
 *
 * <p><b>Angemeldet</b>: Zeilen, in denen eine echte Namensspalte nicht leer ist.
 * Die Spalten werden über die Header gesucht, weil Teamname/Verein/Formation je nach
 * Turniersystem die festen Spalten B/C verschieben können.
 *
 * <p><b>Aktiv</b>: Wert {@code 1} in der „Aktiv"-Spalte. Spalte wird über den Header in
 * Zeile&nbsp;2 (zweite Header-Zeile) lokalisiert:
 * <ul>
 *   <li>Supermelee: Spalte mit Header {@code column.header.spieltag.nr} für den
 *       aktuell eingestellten Spieltag (aus {@link SuperMeleeKonfigurationSheet}) —
 *       Supermelee hat pro Spieltag eine eigene Status-Spalte.</li>
 *   <li>Alle anderen Systeme: Spalte mit Header {@code column.header.aktiv}.</li>
 * </ul>
 * Findet keine Aktiv-Spalte zugeordnet werden, wird {@code aktiv = angemeldet} als
 * Fallback geliefert.
 */
public final class TeilnehmerStatusService {

    private static final Logger logger = LogManager.getLogger(TeilnehmerStatusService.class);

    /** Anzahl Leerzeilen in Folge, nach der die Zählung abbricht. */
    private static final int MAX_LEERE_ZEILEN_IN_FOLGE = 10;
    /** Hartes Limit gegen Endlosschleifen bei beschädigten Sheets. */
    private static final int MAX_ZEILEN = 10_000;
    /** Maximale Anzahl Spalten, die beim Header-Scan durchsucht werden. */
    private static final int MAX_HEADER_SPALTEN = 200;
    /** Wert in der Aktiv-Spalte, der „nimmt teil" bedeutet (SpielrundeGespielt.JA / AKTIV_WERT_NIMMT_TEIL). */
    private static final int WERT_AKTIV = 1;

    public record TeilnehmerStatus(int angemeldet, int aktiv) {}

    /** Anzeige-Namen (identisch formatiert wie in der Checkin-Liste), aufgeteilt nach Aktiv-Status. */
    public record TeilnehmerNamenListen(List<String> angemeldetNichtEingecheckt, List<String> eingecheckt) {}

    record Zaehlschema(int ersteDatenZeile, List<Integer> namensSpalten) {}

    /** Formation + Anzeige-Optionen, wie sie das jeweilige *KonfigurationSheet für die Meldeliste liefert. */
    private record NamenKonfiguration(Formation formation, boolean teamnameAktiv, boolean vereinsnameAktiv) {}

    @FunctionalInterface
    interface ZellTextLeser {
        String text(int spalte, int zeile);
    }

    private TeilnehmerStatusService() {}

    /**
     * Ermittelt die aktuellen Teilnehmerzahlen aus dem Meldelisten-Sheet des übergebenen Dokuments.
     * Liefert {@code (0, 0)} wenn kein Turniersystem erkannt wurde, kein Meldelisten-Sheet
     * existiert oder beim Lesen ein Fehler auftritt.
     */
    public static TeilnehmerStatus ermitteln(WorkingSpreadsheet ws) {
        if (ws == null || ws.getWorkingSpreadsheetDocument() == null) {
            return new TeilnehmerStatus(0, 0);
        }
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            if (ts == null || ts == TurnierSystem.KEIN) {
                return new TeilnehmerStatus(0, 0);
            }
            SheetHelper sh = new SheetHelper(ws);
            XSpreadsheet sheet = sh.findByName(SheetNamen.meldeliste());
            if (sheet == null) {
                return new TeilnehmerStatus(0, 0);
            }
            int aktivSpalte = ermittleAktivSpalte(sh, sheet, ts, ws);
            Zaehlschema schema = zaehlschema((spalte, zeile) ->
                    sh.getTextFromCell(sheet, Position.from(spalte, zeile)));
            return zaehlen(sh, sheet, aktivSpalte, schema);
        } catch (RuntimeException e) {
            logger.warn("Teilnehmerzahl konnte nicht ermittelt werden", e);
            return new TeilnehmerStatus(0, 0);
        }
    }

    /**
     * Ermittelt die Anzeige-Namen aller Angemeldeten aus dem Meldelisten-Sheet, aufgeteilt in
     * „noch nicht eingecheckt" und „eingecheckt". Nutzt für die Namensbildung dieselbe Klasse
     * ({@link TeilnehmerNamenLeser}) wie die bestehenden Checkin-Listen, damit die Formatierung
     * (Vorname Nachname, mehrere Spieler mit „ / " verbunden) identisch ist.
     *
     * <p>Nur für den Live-Push der Turnier-Startseite gedacht (optionales Feature) – liefert bei
     * fehlendem Turniersystem/Sheet oder Lesefehler leere Listen.
     */
    public static TeilnehmerNamenListen ermittelnNamen(WorkingSpreadsheet ws) {
        if (ws == null || ws.getWorkingSpreadsheetDocument() == null) {
            return new TeilnehmerNamenListen(List.of(), List.of());
        }
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            if (ts == null || ts == TurnierSystem.KEIN) {
                return new TeilnehmerNamenListen(List.of(), List.of());
            }
            SheetHelper sh = new SheetHelper(ws);
            XSpreadsheet sheet = sh.findByName(SheetNamen.meldeliste());
            if (sheet == null) {
                return new TeilnehmerNamenListen(List.of(), List.of());
            }
            int aktivSpalte = ermittleAktivSpalte(sh, sheet, ts, ws);
            Zaehlschema schema = zaehlschema((spalte, zeile) ->
                    sh.getTextFromCell(sheet, Position.from(spalte, zeile)));
            var namenKonfiguration = namenKonfiguration(ts, ws);
            if (namenKonfiguration == null) {
                return new TeilnehmerNamenListen(List.of(), List.of());
            }
            var meldelisteAnsicht = new MeldelisteSheetAnsicht(ws, sheet);
            var teilnehmerNamen = TeilnehmerNamenLeser
                    .from(meldelisteAnsicht, schema.ersteDatenZeile(), namenKonfiguration.formation(),
                            namenKonfiguration.teamnameAktiv(), namenKonfiguration.vereinsnameAktiv())
                    .lesen();
            return namenListen(sh, sheet, aktivSpalte, schema, teilnehmerNamen);
        } catch (RuntimeException | GenerateException e) {
            logger.warn("Teilnehmerlisten konnten nicht ermittelt werden", e);
            return new TeilnehmerNamenListen(List.of(), List.of());
        }
    }

    /** Ein Listeneintrag vor der Sortierung: Anzeige-Name + Sortierschlüssel (Nachname Spieler 1). */
    private record NamenEintrag(int nr, String name, String sortName) {}

    private static TeilnehmerNamenListen namenListen(SheetHelper sh, XSpreadsheet sheet, int aktivSpalte,
            Zaehlschema schema, TeilnehmerNamenLeser.TeilnehmerNamen teilnehmerNamen) {
        Map<Integer, String> spielerNamen = teilnehmerNamen.spielerNamen();
        Map<Integer, String> sortNamen = teilnehmerNamen.sortNamen();
        List<NamenEintrag> nichtEingecheckt = new ArrayList<>();
        List<NamenEintrag> eingecheckt = new ArrayList<>();
        int leereZeilenInFolge = 0;
        for (int zeile = schema.ersteDatenZeile(); zeile < MAX_ZEILEN; zeile++) {
            if (!zeileHatNamen(sh, sheet, schema.namensSpalten(), zeile)) {
                if (++leereZeilenInFolge >= MAX_LEERE_ZEILEN_IN_FOLGE) {
                    break;
                }
                continue;
            }
            leereZeilenInFolge = 0;
            int nr = sh.getIntFromCell(sheet, Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, zeile));
            String name = spielerNamen.getOrDefault(nr, "");
            if (name.isBlank()) {
                continue;
            }
            var eintrag = new NamenEintrag(nr, name, sortNamen.getOrDefault(nr, ""));
            boolean istEingecheckt = aktivSpalte >= 0
                    && sh.getIntFromCell(sheet, Position.from(aktivSpalte, zeile)) == WERT_AKTIV;
            (istEingecheckt ? eingecheckt : nichtEingecheckt).add(eintrag);
        }
        return new TeilnehmerNamenListen(namenSortiert(nichtEingecheckt), namenSortiert(eingecheckt));
    }

    /**
     * Sortiert nach Nachname Spieler 1, locale-korrekt (Deutsch, Umlaute, case-insensitiv) – identisches
     * Vergleichsmuster wie {@link de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerListeSortModus#NAME}
     * in der Checkin-Liste, damit die Startseite dieselbe Reihenfolge zeigt.
     */
    private static List<String> namenSortiert(List<NamenEintrag> eintraege) {
        Collator collator = Collator.getInstance(Locale.GERMAN);
        collator.setStrength(Collator.SECONDARY);
        var sortiert = new ArrayList<>(eintraege);
        sortiert.sort(Comparator.<NamenEintrag, String>comparing(NamenEintrag::sortName, collator)
                .thenComparingInt(NamenEintrag::nr));
        return sortiert.stream().map(NamenEintrag::name).toList();
    }

    /**
     * Liefert Formation/Anzeige-Optionen für die Namensbildung pro Turniersystem. Bei KO, Schweizer,
     * JGJ, FormuleX, Kaskade, Maastrichter, Poule und TripTete kommen sie aus dem jeweiligen
     * {@code *KonfigurationSheet} (leichte Delegation an bereits instanzierte PropertiesSpalte-Objekte,
     * kein Sheet-Vollaufbau). Liga und SuperMelee kennen keine einstellbare Formation – dort ist die
     * Meldeliste fest auf Einzelspieler ohne Teamname/Verein-Spalte ausgelegt.
     */
    private static NamenKonfiguration namenKonfiguration(TurnierSystem ts, WorkingSpreadsheet ws) {
        return switch (ts) {
            case KO -> {
                var k = new KoKonfigurationSheet(ws);
                yield new NamenKonfiguration(k.getMeldeListeFormation(), k.isMeldeListeTeamnameAnzeigen(),
                        k.isMeldeListeVereinsnameAnzeigen());
            }
            case SCHWEIZER -> {
                var k = new SchweizerKonfigurationSheet(ws);
                yield new NamenKonfiguration(k.getMeldeListeFormation(), k.isMeldeListeTeamnameAnzeigen(),
                        k.isMeldeListeVereinsnameAnzeigen());
            }
            case JGJ -> {
                var k = new JGJKonfigurationSheet(ws);
                yield new NamenKonfiguration(k.getMeldeListeFormation(), k.isMeldeListeTeamnameAnzeigen(),
                        k.isMeldeListeVereinsnameAnzeigen());
            }
            case FORMULEX -> {
                var k = new FormuleXKonfigurationSheet(ws);
                yield new NamenKonfiguration(k.getMeldeListeFormation(), k.isMeldeListeTeamnameAnzeigen(),
                        k.isMeldeListeVereinsnameAnzeigen());
            }
            case KASKADE -> {
                var k = new KaskadeKonfigurationSheet(ws);
                yield new NamenKonfiguration(k.getMeldeListeFormation(), k.isMeldeListeTeamnameAnzeigen(),
                        k.isMeldeListeVereinsnameAnzeigen());
            }
            case MAASTRICHTER -> {
                var k = new MaastrichterKonfigurationSheet(ws);
                yield new NamenKonfiguration(k.getMeldeListeFormation(), k.isMeldeListeTeamnameAnzeigen(),
                        k.isMeldeListeVereinsnameAnzeigen());
            }
            case POULE -> {
                var k = new PouleKonfigurationSheet(ws);
                yield new NamenKonfiguration(k.getMeldeListeFormation(), k.isMeldeListeTeamnameAnzeigen(),
                        k.isMeldeListeVereinsnameAnzeigen());
            }
            case TRIPTETE -> {
                var k = new TripTeteKonfigurationSheet(ws);
                yield new NamenKonfiguration(Formation.TRIPLETTE, k.isMeldeListeTeamnameAnzeigen(),
                        k.isMeldeListeVereinsnameAnzeigen());
            }
            case LIGA -> new NamenKonfiguration(Formation.TETE, false, false);
            case SUPERMELEE -> new NamenKonfiguration(Formation.MELEE, false, false);
            case KEIN -> null;
        };
    }

    /**
     * Minimale, rein lesende {@link ISheet}-Sicht auf ein bereits bekanntes Meldelisten-Sheet – für
     * {@link TeilnehmerNamenLeser}, das ein {@code ISheet} erwartet. Bewusst kein SheetRunner/Vollaufbau:
     * {@link #getXSpreadSheet()} liefert direkt das übergebene Sheet, ohne Validierung oder Erzeugung
     * auszulösen (im Gegensatz zu {@code *KonfigurationSheet.getXSpreadSheet()}, das für Fremdaufrufe
     * absichtlich eine {@link GenerateException} wirft).
     */
    private static final class MeldelisteSheetAnsicht implements ISheet {

        private final WorkingSpreadsheet ws;
        private final XSpreadsheet xSheet;

        MeldelisteSheetAnsicht(WorkingSpreadsheet ws, XSpreadsheet xSheet) {
            this.ws = ws;
            this.xSheet = xSheet;
        }

        @Override
        public SheetHelper getSheetHelper() {
            return new SheetHelper(ws);
        }

        @Override
        public XSpreadsheet getXSpreadSheet() {
            return xSheet;
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public XComponentContext getxContext() {
            return ws.getxContext();
        }

        @Override
        public WorkingSpreadsheet getWorkingSpreadsheet() {
            return ws;
        }

        @Override
        public void processBoxinfo(String i18nKey, Object... args) {
            // Kein UI im Live-Push-Pfad der Startseite.
        }

        @Override
        public TurnierSheet getTurnierSheet() {
            throw new UnsupportedOperationException("Nicht unterstützt für die read-only Meldelisten-Sicht");
        }
    }

    /**
     * Sucht die Aktiv-Spalte (für Supermelee die Spalte des aktuell eingestellten Spieltags,
     * sonst die als „Aktiv" beschriftete Spalte) über die Header-Zeile.
     *
     * @return Spaltenindex oder {@code -1} falls nicht gefunden — in dem Fall fällt die
     *         Zählung auf {@code aktiv = angemeldet} zurück.
     */
    private static int ermittleAktivSpalte(SheetHelper sh, XSpreadsheet sheet, TurnierSystem ts,
            WorkingSpreadsheet ws) {
        String gesuchterHeader = gesuchterHeader(ts, ws);
        if (gesuchterHeader == null || gesuchterHeader.isBlank()) {
            return -1;
        }
        for (int spalte = 0; spalte < MAX_HEADER_SPALTEN; spalte++) {
            String headerText = sh.getTextFromCell(sheet,
                    Position.from(spalte, MeldeListeKonstanten.ZWEITE_HEADER_ZEILE));
            if (headerText != null && headerText.equals(gesuchterHeader)) {
                return spalte;
            }
        }
        return -1;
    }

    private static String gesuchterHeader(TurnierSystem ts, WorkingSpreadsheet ws) {
        if (ts == TurnierSystem.SUPERMELEE) {
            try {
                SpielTagNr spieltagNr = new SuperMeleeKonfigurationSheet(ws).getAktiveSpieltag();
                return I18n.get("column.header.spieltag.nr", spieltagNr.getNr());
            } catch (RuntimeException e) {
                logger.debug("Aktiver Spieltag (Supermelee) konnte nicht ermittelt werden: {}", e.getMessage());
                return null;
            }
        }
        return I18n.get("column.header.aktiv");
    }

    static Zaehlschema zaehlschema(ZellTextLeser zellen) {
        String vorname = I18n.get("column.header.vorname");
        String nachname = I18n.get("column.header.nachname");
        String name = I18n.get("column.header.name");

        Zaehlschema dreizeilig = zaehlschemaAusHeaderZeile(zellen, MeldeListeKonstanten.ERSTE_DATEN_ZEILE,
                vorname, nachname, name);
        if (!dreizeilig.namensSpalten().isEmpty()) {
            return dreizeilig;
        }

        Zaehlschema zweizeilig = zaehlschemaAusHeaderZeile(zellen, MeldeListeKonstanten.ZWEITE_HEADER_ZEILE,
                vorname, nachname, name);
        if (!zweizeilig.namensSpalten().isEmpty()) {
            return zweizeilig;
        }

        return new Zaehlschema(MeldeListeKonstanten.ERSTE_DATEN_ZEILE,
                List.of(MeldeListeKonstanten.SPIELER_NR_SPALTE + 1,
                        MeldeListeKonstanten.SPIELER_NR_SPALTE + 2));
    }

    private static Zaehlschema zaehlschemaAusHeaderZeile(ZellTextLeser zellen, int headerZeile,
            String... namensHeader) {
        List<Integer> spalten = new ArrayList<>();
        for (int spalte = 0; spalte < MAX_HEADER_SPALTEN; spalte++) {
            String headerText = zellen.text(spalte, headerZeile);
            if (istNamensHeader(headerText, namensHeader)) {
                spalten.add(spalte);
            }
        }
        return new Zaehlschema(headerZeile + 1, List.copyOf(spalten));
    }

    private static boolean istNamensHeader(String headerText, String... namensHeader) {
        if (headerText == null || headerText.isBlank()) {
            return false;
        }
        for (String header : namensHeader) {
            if (headerText.equals(header)) {
                return true;
            }
        }
        return false;
    }

    private static TeilnehmerStatus zaehlen(SheetHelper sh, XSpreadsheet sheet, int aktivSpalte,
            Zaehlschema schema) {
        int angemeldet = 0;
        int aktiv = 0;
        int leereZeilenInFolge = 0;
        for (int zeile = schema.ersteDatenZeile(); zeile < MAX_ZEILEN; zeile++) {
            if (zeileHatNamen(sh, sheet, schema.namensSpalten(), zeile)) {
                leereZeilenInFolge = 0;
                angemeldet++;
                if (aktivSpalte >= 0
                        && sh.getIntFromCell(sheet, Position.from(aktivSpalte, zeile)) == WERT_AKTIV) {
                    aktiv++;
                }
            } else if (++leereZeilenInFolge >= MAX_LEERE_ZEILEN_IN_FOLGE) {
                break;
            }
        }
        // Fallback wenn Aktiv-Spalte nicht gefunden: Aktiv = Angemeldet (verhindert „0"-Anzeige bei
        // ungewöhnlichem Header-Layout / Sprachwechsel im Dokument).
        return new TeilnehmerStatus(angemeldet, aktivSpalte >= 0 ? aktiv : angemeldet);
    }

    private static boolean zeileHatNamen(SheetHelper sh, XSpreadsheet sheet, List<Integer> namensSpalten,
            int zeile) {
        for (int spalte : namensSpalten) {
            if (!istLeer(sh.getTextFromCell(sheet, Position.from(spalte, zeile)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean istLeer(String text) {
        return text == null || text.isBlank();
    }
}
