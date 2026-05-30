package de.petanqueturniermanager.sidebar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Architektur-Konventionstest zur Threading-Regel „VCL/UNO-UI nur auf dem LO-Main-Thread"
 * (siehe CLAUDE.md, Abschnitt <em>Threading</em>).
 * <p>
 * Hintergrund: Listener-Callbacks, die aus Fremd-Threads feuern
 * ({@code SheetRunner.benachrichtigeListener()} → Worker-Thread,
 * {@code TimerManager.emittiere()} → {@code PTM-Timer}-Thread,
 * {@code WebServerManager}/{@code ReleaseUpdateService}-StatusListener), dürfen UNO-UI-Controls
 * NICHT direkt anfassen – das blockiert unter Windows die SolarMutex (Freeze) oder crasht (SIGSEGV).
 * Die UI-Arbeit muss über {@code LoMainThread.post(...)} (bzw. {@code ProcessBox.runOnMain}) auf den
 * Main-Thread marshallen.
 * <p>
 * Dieser Test ist eine Heuristik auf Quelltext-Ebene (kein Call-Graph): er markiert jede Klasse, die
 * <ol>
 *   <li>einen Background-Thread-Listener registriert/implementiert UND</li>
 *   <li>eindeutige VCL-Control-APIs referenziert,</li>
 * </ol>
 * aber {@code LoMainThread}/{@code runOnMain} NICHT erwähnt. Lagert eine Klasse ihre UI-Mutation in
 * eine Hilfsklasse aus, erkennt der Scan das nicht – die Regel bleibt trotzdem in CLAUDE.md bindend.
 * Mögliche Verschärfung (Call-Graph via ArchUnit) ist als Ausbau vorgemerkt.
 */
public class HintergrundListenerVclKonventionTest {

    /** Markierungen, die eine Klasse als Träger eines (potenziell) Fremd-Thread-Callbacks ausweisen. */
    private static final Pattern BACKGROUND_LISTENER = Pattern.compile(
            "SheetRunner\\.addStateChangeListener\\("
                    + "|TimerManager\\.get\\(\\)\\.addListener\\("
                    + "|implements\\s+[\\w,\\s.]*\\bTimerListener\\b"
                    + "|WebServerManager\\.get\\(\\)\\.addStatusListener\\("
                    + "|ReleaseUpdateService\\.get\\(\\)\\.addStatusListener\\(");

    /**
     * Eindeutig UI-/VCL-Control-bezogene Tokens. Bewusst eng gewählt (keine generischen Treffer wie
     * {@code setPropertyValue} oder {@code .dispose()}, die auch Dokument-/Service-Lifecycle betreffen),
     * damit der Test keine Fehlalarme produziert.
     */
    private static final Pattern VCL_CONTROL_TOKEN = Pattern.compile(
            "showElement\\(|requestElement\\(|XFixedText|XListBox|\\bXControl\\b|XWindowPeer"
                    + "|\\.selectItemPos\\(|\\.addItem\\(|XLayoutManager|XFixedHyperlink|\\bXButton\\b");

    /** Marshalling auf den Main-Thread. */
    private static final Pattern MARSHALLING = Pattern.compile("LoMainThread|runOnMain");

    @Test
    void hintergrundListenerMitUiArbeitMarshallenAufMainThread() throws IOException {
        List<String> verstoesse = new ArrayList<>();
        Path quellWurzel = Paths.get("src/main/java");
        try (Stream<Path> dateien = Files.walk(quellWurzel)) {
            dateien.filter(p -> p.toString().endsWith(".java")).forEach(datei -> {
                String inhalt = liesDatei(datei);
                boolean istBackgroundListener = BACKGROUND_LISTENER.matcher(inhalt).find();
                boolean fasstUiAn = VCL_CONTROL_TOKEN.matcher(inhalt).find();
                boolean marshallt = MARSHALLING.matcher(inhalt).find();
                if (istBackgroundListener && fasstUiAn && !marshallt) {
                    verstoesse.add(quellWurzel.relativize(datei).toString());
                }
            });
        }

        assertThat(verstoesse)
                .as("Klassen mit Fremd-Thread-Listener UND direkter VCL-Control-Nutzung ohne "
                        + "LoMainThread/runOnMain (siehe CLAUDE.md → Threading: VCL/UNO-UI NUR auf dem "
                        + "LO-Main-Thread). UI-Arbeit aus dem Callback per LoMainThread.post(...) auf den "
                        + "Main-Thread verschieben.")
                .isEmpty();
    }

    private static String liesDatei(Path datei) {
        try {
            return Files.readString(datei);
        } catch (IOException e) {
            throw new IllegalStateException("Fehler beim Lesen von " + datei, e);
        }
    }
}
