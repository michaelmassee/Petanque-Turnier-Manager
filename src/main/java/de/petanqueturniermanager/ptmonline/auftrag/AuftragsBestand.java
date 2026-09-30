/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

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

    /** Höchstzahl erledigter Protokollzeilen (verworfen/abgelehnt), die im Puffer stehen bleiben. */
    static final int MAX_PROTOKOLL = 100;

    /** Zustand einer Pufferzeile; angenommene Aufträge verschwinden aus dem Puffer. */
    public enum Zustand {
        OFFEN, VERWORFEN, ABGELEHNT
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
    private long zaehler;
    private boolean geaendert;

    private AuftragsBestand(long zaehler, List<Eintrag> eintraege) {
        this.zaehler = zaehler;
        this.eintraege.addAll(eintraege);
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
        List<SyncAuftrag> offen = eintraege.stream().filter(eintrag -> eintrag.zustand() == Zustand.OFFEN)
                .map(Eintrag::auftrag).filter(auftrag -> !ergebnisse.containsKey(auftrag.auftragsId()))
                .sorted(Comparator.comparingLong(SyncAuftrag::zaehler)).toList();
        if (!pausiert) {
            return offen;
        }
        return offen.stream().takeWhile(auftrag -> auftrag.art().inPauseErlaubt()).toList();
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
        if (ergebnis.angenommen()) {
            eintraege.removeIf(eintrag -> eintrag.auftrag().auftragsId().equals(id));
        } else {
            ersetzeZustand(id, Zustand.ABGELEHNT, grundBeiAblehnung);
        }
        geaendert = true;
        kuerzeProtokoll();
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
     * alten Bindung werden verworfen, der Zähler übernimmt den Stand des Servers.
     */
    public synchronized void neueBindung(long serverZaehler, String grund) {
        eintraege.replaceAll(eintrag -> eintrag.zustand() == Zustand.OFFEN
                ? new Eintrag(eintrag.auftrag(), Zustand.VERWORFEN, grund, Instant.now())
                : eintrag);
        ergebnisse.clear();
        zaehler = serverZaehler;
        geaendert = true;
        kuerzeProtokoll();
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

    /** Hält den Puffer klein: die ältesten Protokollzeilen fallen weg, offene Aufträge nie. */
    private void kuerzeProtokoll() {
        long protokoll = eintraege.stream().filter(eintrag -> eintrag.zustand() != Zustand.OFFEN).count();
        for (int i = 0; i < eintraege.size() && protokoll > MAX_PROTOKOLL;) {
            if (eintraege.get(i).zustand() == Zustand.OFFEN) {
                i++;
            } else {
                eintraege.remove(i);
                protokoll--;
            }
        }
    }
}
