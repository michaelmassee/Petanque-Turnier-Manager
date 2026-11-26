/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

import de.petanqueturniermanager.ptmonline.auftrag.versand.AuftragsSender;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;

/**
 * Schreibzähler und Auftragspuffer einer Dokumentbindung (T-09, T-19, T-23). Kennt keine Tabelle: gespeichert wird
 * über {@link #eintraege()} und {@link #zaehler()}, geladen über {@link #aus}. Thread-sicher – Aufträge entstehen im
 * Dokument-Kontext, gesendet wird im Hintergrund.
 * <p>
 * Ein Auftrag bleibt {@link Zustand#OFFEN}, bis sein Ergebnis im Dokument angewendet ist. Gesendete, noch nicht
 * angewendete Aufträge werden nicht erneut gesendet; stürzt LibreOffice vorher ab, sind sie nach dem Laden wieder
 * offen und gehen mit derselben Auftrags-ID erneut hinaus – PTM-Online liefert dann die gespeicherte Antwort.
 */
public final class AuftragsBestand {

    /**
     * Höchstzahl erledigter Protokollzeilen (verworfen/abgelehnt/quittiert), die im Puffer stehen bleiben. Ungeklärte
     * Ablehnungen, die eine Meldung sperren ({@link AuftragsArt#sperrtMeldungBisZurKlaerung()}), zählen nicht dazu.
     */
    static final int MAX_PROTOKOLL = 100;

    /** Zustand einer Pufferzeile; angenommene Aufträge verschwinden aus dem Puffer. */
    public enum Zustand {
        OFFEN, VERWORFEN, ABGELEHNT,
        /** Abgelehnt, und die Turnierleitung hat die Ablehnung in der Konfliktliste zur Kenntnis genommen (A-29). */
        QUITTIERT
    }

    /**
     * Eine Pufferzeile.
     *
     * @param grund     Grund bei verworfenen und abgelehnten Aufträgen, sonst leer
     * @param zeitpunkt Entstehung bzw. letzter Zustandswechsel
     */
    public record Eintrag(SyncAuftrag auftrag, Zustand zustand, String grund, Instant zeitpunkt) {

        public Eintrag {
            Objects.requireNonNull(auftrag, "auftrag");
            Objects.requireNonNull(zustand, "zustand");
            grund = grund == null ? "" : grund;
            Objects.requireNonNull(zeitpunkt, "zeitpunkt");
        }
    }

    private final List<Eintrag> eintraege = new ArrayList<>();
    private final Map<String, VersandErgebnis> ergebnisse = new LinkedHashMap<>();
    private final ReentrantLock versandSperre = new ReentrantLock();
    /**
     * Auftrags-IDs offener Aufträge, die PTM-Online erreicht haben können: einmal an den Sender übergeben oder schon
     * beim Laden offen (ihre Vorgeschichte ist unbekannt). Fehlt nur die Antwort, hat PTM-Online sie womöglich
     * ausgeführt; sie dürfen dann nicht mehr stillschweigend entfallen.
     */
    private final Set<String> moeglicherweiseAngekommen = new LinkedHashSet<>();
    /** Flüchtige Zwischenstände der Erfasser je Typ, siehe {@link #zwischenstand}. */
    private final Map<Class<?>, Object> zwischenstaende = new ConcurrentHashMap<>();
    private long zaehler;
    private boolean geaendert;

    private AuftragsBestand(long zaehler, List<Eintrag> eintraege) {
        this.zaehler = zaehler;
        this.eintraege.addAll(eintraege);
        eintraege.stream().filter(eintrag -> eintrag.zustand() == Zustand.OFFEN)
                .forEach(eintrag -> moeglicherweiseAngekommen.add(eintrag.auftrag().auftragsId()));
    }

    /** Leerer Bestand einer neuen Bindung. */
    public static AuftragsBestand leer() {
        return new AuftragsBestand(0, List.of());
    }

    /**
     * Gespeicherter Bestand. Ist ein gespeicherter Auftrag neuer als der gespeicherte Zähler (Zelle manuell
     * verändert), zählt der Auftrag.
     */
    public static AuftragsBestand aus(long zaehler, List<Eintrag> eintraege) {
        long hoechster = eintraege.stream().mapToLong(eintrag -> eintrag.auftrag().zaehler()).max().orElse(0);
        return new AuftragsBestand(Math.max(zaehler, hoechster), eintraege);
    }

    /**
     * Flüchtiger Zwischenstand eines Erfassers, z.&nbsp;B. der zuletzt als Auftrag erfasste Live-Stand. Er gehört zu
     * diesem Puffer, also zu einem Dokument (bzw. Spieltag) – nicht zum Online-Turnier: Eine Kopie desselben Turniers
     * hat ihren eigenen Puffer und darf das Original nicht beeinflussen, etwa wenn PTM-Online ihre Aufträge als
     * Dokumentkopie ablehnt. Wird nicht gespeichert; eine neue Bindung verwirft ihn.
     */
    public <T> T zwischenstand(Class<T> typ, Supplier<T> neu) {
        return typ.cast(zwischenstaende.computeIfAbsent(typ, ignored -> neu.get()));
    }

    /**
     * Sperre des Versands: wer sendet, hält sie. So gehen Hintergrund-Versand und synchroner Versand (manueller
     * Abgleich, Trennen) nie verschränkt hinaus, und der Schreibzähler kommt in steigender Reihenfolge an.
     */
    public ReentrantLock versandSperre() {
        return versandSperre;
    }

    /** Legt einen neuen Auftrag mit neuer Auftrags-ID und dem nächsten Schreibzähler an. */
    public synchronized SyncAuftrag erzeuge(AuftragsArt art, String methode, String pfad, String body, String kontext) {
        SyncAuftrag auftrag = new SyncAuftrag(UUID.randomUUID().toString(), ++zaehler, art, methode, pfad, body,
                kontext);
        eintraege.add(new Eintrag(auftrag, Zustand.OFFEN, "", Instant.now()));
        geaendert = true;
        return auftrag;
    }

    /**
     * Offene, noch nicht gesendete Aufträge in Zählerreihenfolge. Bei Pause nur der Anfang der Reihe, soweit er in
     * der Pause erlaubt ist ({@link AuftragsArt#inPauseErlaubt()}): ein späterer Auftrag darf nie vor einem früheren
     * gesendet werden, sonst lehnt PTM-Online den früheren als veraltet ab.
     */
    public synchronized List<SyncAuftrag> zuSenden(boolean pausiert) {
        List<SyncAuftrag> offen = ungesendeteOffene();
        if (!pausiert) {
            return offen;
        }
        return offen.stream().takeWhile(auftrag -> auftrag.art().inPauseErlaubt()).toList();
    }

    /**
     * Zieht einen offenen, noch nicht gesendeten Auftrag vor alle übrigen ungesendeten, damit ein in der Pause
     * erlaubter Auftrag allein hinausgehen kann. Dazu erhalten die übrigen der Reihe nach neue Zählerstände
     * <em>oberhalb</em> des bisher höchsten; der vorgezogene behält seinen. Ein schon vergebener Zählerstand wird nie an
     * einen anderen Auftrag weitergegeben: Fehlt nur die Antwort, kann PTM-Online ihn bereits verbraucht haben, und
     * ein neuer Auftrag mit diesem Stand würde als Dokumentkopie abgelehnt ({@code document_forked}). Auftrags-ID und
     * Nutzlast der umnummerierten Aufträge bleiben, damit PTM-Online einen bereits ausgeführten an seiner ID erkennt.
     */
    public synchronized void zieheVor(String auftragsId) {
        List<SyncAuftrag> ungesendet = ungesendeteOffene();
        if (ungesendet.stream().noneMatch(auftrag -> auftrag.auftragsId().equals(auftragsId))) {
            return;
        }
        Map<String, Long> neuerZaehler = new LinkedHashMap<>();
        for (SyncAuftrag auftrag : ungesendet) {
            if (!auftrag.auftragsId().equals(auftragsId)) {
                neuerZaehler.put(auftrag.auftragsId(), ++zaehler);
            }
        }
        eintraege.replaceAll(eintrag -> {
            Long zaehlerNeu = neuerZaehler.get(eintrag.auftrag().auftragsId());
            return zaehlerNeu == null ? eintrag
                    : new Eintrag(eintrag.auftrag().mitZaehler(zaehlerNeu), eintrag.zustand(), eintrag.grund(),
                            eintrag.zeitpunkt());
        });
        geaendert = true;
    }

    /**
     * Wiederholt offene, noch nicht beantwortete Aufträge der passenden Arten hinter allen übrigen, z.&nbsp;B. einen
     * wartenden Turnierabschluss nach einer danach erfassten Ranglistenkorrektur. Jeder erhält eine <em>neue</em>
     * Auftrags-ID und einen neuen Zählerstand oberhalb des bisher höchsten; die Nutzlast bleibt. Die neue ID ist nötig:
     * Fehlt nur die Antwort, hat PTM-Online den Auftrag womöglich schon ausgeführt und lieferte unter der alten ID bloß
     * die gespeicherte Antwort – die Wiederholung soll aber den neuen Stand erfassen. Ein noch laufender Versand unter
     * der alten ID ändert daran nichts; sein Ergebnis betrifft keinen Eintrag mehr.
     */
    public synchronized void wiederholeAmEnde(Predicate<AuftragsArt> arten) {
        Map<String, SyncAuftrag> erneuert = new LinkedHashMap<>();
        for (SyncAuftrag auftrag : ungesendeteOffene()) {
            if (arten.test(auftrag.art())) {
                erneuert.put(auftrag.auftragsId(), new SyncAuftrag(UUID.randomUUID().toString(), ++zaehler,
                        auftrag.art(), auftrag.methode(), auftrag.pfad(), auftrag.body(), auftrag.kontext()));
            }
        }
        if (erneuert.isEmpty()) {
            return;
        }
        eintraege.replaceAll(eintrag -> {
            SyncAuftrag neu = erneuert.get(eintrag.auftrag().auftragsId());
            return neu == null ? eintrag : new Eintrag(neu, eintrag.zustand(), eintrag.grund(), Instant.now());
        });
        moeglicherweiseAngekommen.removeAll(erneuert.keySet());
        geaendert = true;
    }

    /**
     * Offene Aufträge, die PTM-Online sicher nie erreicht haben: in dieser Sitzung erzeugt und nie an den Sender
     * übergeben. Nur sie dürfen ersatzlos zurückgenommen werden.
     */
    public synchronized List<SyncAuftrag> nieGesendete() {
        return ungesendeteOffene().stream()
                .filter(auftrag -> !moeglicherweiseAngekommen.contains(auftrag.auftragsId())).toList();
    }

    /**
     * Sender, der jeden Auftrag vor dem Senden als möglicherweise angekommen vermerkt – auch wenn danach die Antwort
     * ausbleibt (Netzfehler, Zeitlimit).
     */
    public AuftragsSender mitVersandVermerk(AuftragsSender sender) {
        return auftrag -> {
            vermerkeVersand(auftrag.auftragsId());
            return sender.sende(auftrag);
        };
    }

    private synchronized void vermerkeVersand(String auftragsId) {
        moeglicherweiseAngekommen.add(auftragsId);
    }

    /** Offene, noch nicht gesendete Aufträge in Zählerreihenfolge. */
    private List<SyncAuftrag> ungesendeteOffene() {
        return eintraege.stream().filter(eintrag -> eintrag.zustand() == Zustand.OFFEN)
                .map(Eintrag::auftrag).filter(auftrag -> !ergebnisse.containsKey(auftrag.auftragsId()))
                .sorted(Comparator.comparingLong(SyncAuftrag::zaehler)).toList();
    }

    /** Merkt das Ergebnis eines gesendeten Auftrags, bis es im Dokument angewendet ist. */
    public synchronized void gesendet(VersandErgebnis ergebnis) {
        ergebnisse.put(ergebnis.auftrag().auftragsId(), ergebnis);
    }

    /** Ergebnisse, die im Dokument noch anzuwenden sind, in Sendereihenfolge. */
    public synchronized List<VersandErgebnis> ergebnisseZumAnwenden() {
        return List.copyOf(ergebnisse.values());
    }

    /**
     * Das Ergebnis ist im Dokument angewendet: ein angenommener Auftrag verschwindet, ein abgelehnter bleibt als
     * Protokollzeile stehen.
     */
    public synchronized void angewendet(VersandErgebnis ergebnis, String grundBeiAblehnung) {
        String id = ergebnis.auftrag().auftragsId();
        ergebnisse.remove(id);
        moeglicherweiseAngekommen.remove(id);
        if (ergebnis.angenommen()) {
            eintraege.removeIf(eintrag -> eintrag.auftrag().auftragsId().equals(id));
        } else {
            ersetzeZustand(id, Zustand.ABGELEHNT, grundBeiAblehnung);
        }
        geaendert = true;
        kuerzeProtokoll();
    }

    /** Markiert einen abgelehnten Auftrag als zur Kenntnis genommen; die Protokollzeile bleibt stehen. */
    public synchronized void quittiere(String auftragsId) {
        boolean abgelehnt = eintraege.stream().anyMatch(eintrag -> eintrag.zustand() == Zustand.ABGELEHNT
                && eintrag.auftrag().auftragsId().equals(auftragsId));
        if (abgelehnt) {
            eintraege.replaceAll(eintrag -> eintrag.auftrag().auftragsId().equals(auftragsId)
                    ? new Eintrag(eintrag.auftrag(), Zustand.QUITTIERT, eintrag.grund(), Instant.now())
                    : eintrag);
            geaendert = true;
        }
    }

    /**
     * Verwirft offene, noch nicht gesendete Aufträge der passenden Arten; sie werden nie mehr gesendet und bleiben als
     * Protokollzeile stehen (KP-05).
     *
     * @return die verworfenen Aufträge
     */
    public synchronized List<SyncAuftrag> verwerfe(Predicate<AuftragsArt> arten, String grund) {
        List<SyncAuftrag> verworfen = eintraege.stream()
                .filter(eintrag -> eintrag.zustand() == Zustand.OFFEN && arten.test(eintrag.auftrag().art())
                        && !ergebnisse.containsKey(eintrag.auftrag().auftragsId()))
                .map(Eintrag::auftrag).toList();
        verworfen.forEach(auftrag -> ersetzeZustand(auftrag.auftragsId(), Zustand.VERWORFEN, grund));
        if (!verworfen.isEmpty()) {
            geaendert = true;
            kuerzeProtokoll();
        }
        return verworfen;
    }

    /**
     * Entfernt offene, noch nicht gesendete Aufträge, die ein neuer Auftrag vollständig ersetzt (z.&nbsp;B. ein
     * älterer Stand derselben Spielrunde). Sie werden nicht protokolliert – ihr Inhalt geht im neueren auf.
     */
    public synchronized void entferneUeberholte(Predicate<SyncAuftrag> ueberholt) {
        boolean entfernt = eintraege.removeIf(eintrag -> eintrag.zustand() == Zustand.OFFEN
                && !ergebnisse.containsKey(eintrag.auftrag().auftragsId()) && ueberholt.test(eintrag.auftrag()));
        geaendert |= entfernt;
    }

    /**
     * Neue Bindung (Verbinden oder Übernehmen): PTM-Online hat den Schreibzähler zurückgesetzt. Offene Aufträge der
     * alten Bindung werden verworfen, der Zähler übernimmt den Stand des Servers. Aufrufer halten die
     * {@link #versandSperre()}, damit kein Versand der alten Bindung mehr hinausgeht.
     *
     * @param anderesTurnier ob die neue Bindung zu einem anderen Online-Turnier gehört: Dann werden auch ungeklärte
     *                       Ablehnungen verworfen – sie betreffen das alte Turnier und sperrten sonst dieselben
     *                       lokalen Meldungen im neuen ({@link AuftragsArt#sperrtMeldungBisZurKlaerung()}).
     */
    public synchronized void neueBindung(long serverZaehler, String grund, boolean anderesTurnier) {
        eintraege.replaceAll(eintrag -> gehoertZurAltenBindung(eintrag, anderesTurnier)
                ? new Eintrag(eintrag.auftrag(), Zustand.VERWORFEN, grund, Instant.now())
                : eintrag);
        ergebnisse.clear();
        moeglicherweiseAngekommen.clear();
        zwischenstaende.clear();
        zaehler = serverZaehler;
        geaendert = true;
        kuerzeProtokoll();
    }

    /**
     * Verbindung getrennt: Der Bestand gilt wie nie angelegt – ohne Aufträge, Protokoll, Zwischenstände und mit Zähler
     * 0, passend zu den geleerten Dokumenteigenschaften. Aufrufer halten die {@link #versandSperre()}. Das Objekt
     * bleibt erhalten, damit ein Hintergrundversand, der es noch hält, nicht mit einem zweiten Bestand konkurriert.
     */
    public synchronized void leeren() {
        eintraege.clear();
        ergebnisse.clear();
        moeglicherweiseAngekommen.clear();
        zwischenstaende.clear();
        zaehler = 0;
        geaendert = false;
    }

    /**
     * Dieses Dokument gilt (das Desktop-Dokument ist maßgeblich): PTM-Online kennt bereits einen höheren Zählerstand
     * dieser Bindung, etwa weil ein älterer, nicht gespeicherter Stand der Datei geöffnet wurde. Offene, noch nicht
     * gesendete Aufträge erhalten der Reihe nach neue Zählerstände oberhalb von Server und Dokument. Auftrags-ID und
     * Nutzlast bleiben, damit PTM-Online einen bereits ausgeführten Auftrag weiterhin an seiner ID erkennt.
     */
    public synchronized void uebernehmeServerZaehler(long serverZaehler) {
        zaehler = Math.max(zaehler, serverZaehler);
        List<String> neuZuNummerieren = ungesendeteOffene().stream().map(SyncAuftrag::auftragsId).toList();
        for (String auftragsId : neuZuNummerieren) {
            long neuerZaehler = ++zaehler;
            eintraege.replaceAll(eintrag -> eintrag.auftrag().auftragsId().equals(auftragsId)
                    ? new Eintrag(eintrag.auftrag().mitZaehler(neuerZaehler), eintrag.zustand(), eintrag.grund(),
                            eintrag.zeitpunkt())
                    : eintrag);
        }
        geaendert = true;
    }

    /** Ob noch Aufträge offen sind (gesendet oder nicht). */
    public synchronized boolean hatOffene() {
        return eintraege.stream().anyMatch(eintrag -> eintrag.zustand() == Zustand.OFFEN);
    }

    /** Ob ein offener Auftrag dieser Art im Puffer steht. */
    public synchronized boolean hatOffenen(AuftragsArt art) {
        return eintraege.stream().anyMatch(eintrag -> eintrag.zustand() == Zustand.OFFEN
                && eintrag.auftrag().art() == art);
    }

    public synchronized long zaehler() {
        return zaehler;
    }

    /** Alle Pufferzeilen in Entstehungsreihenfolge, zum Speichern. */
    public synchronized List<Eintrag> eintraege() {
        return List.copyOf(eintraege);
    }

    /** Ob sich Zähler oder Puffer seit dem letzten {@link #gespeichert()} geändert haben. */
    public synchronized boolean istGeaendert() {
        return geaendert;
    }

    public synchronized void gespeichert() {
        geaendert = false;
    }

    private void ersetzeZustand(String auftragsId, Zustand zustand, String grund) {
        eintraege.replaceAll(eintrag -> eintrag.auftrag().auftragsId().equals(auftragsId)
                ? new Eintrag(eintrag.auftrag(), zustand, grund, Instant.now())
                : eintrag);
    }

    /**
     * Hält den Puffer klein: die ältesten Protokollzeilen fallen weg – offene Aufträge und ungeklärte, sperrende
     * Ablehnungen nie. Ohne diese Ablehnung erzeugte der automatische Abgleich den abgelehnten Auftrag erneut, und die
     * Turnierleitung sähe den Fall nie in der Konfliktliste.
     */
    private void kuerzeProtokoll() {
        long protokoll = eintraege.stream().filter(eintrag -> !bleibtStehen(eintrag)).count();
        for (int i = 0; i < eintraege.size() && protokoll > MAX_PROTOKOLL;) {
            if (bleibtStehen(eintraege.get(i))) {
                i++;
            } else {
                eintraege.remove(i);
                protokoll--;
            }
        }
    }

    private static boolean gehoertZurAltenBindung(Eintrag eintrag, boolean anderesTurnier) {
        return eintrag.zustand() == Zustand.OFFEN || (anderesTurnier && eintrag.zustand() == Zustand.ABGELEHNT);
    }

    private static boolean bleibtStehen(Eintrag eintrag) {
        return eintrag.zustand() == Zustand.OFFEN || (eintrag.zustand() == Zustand.ABGELEHNT
                && eintrag.auftrag().art().sperrtMeldungBisZurKlaerung());
    }
}
