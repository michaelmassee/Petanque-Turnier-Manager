/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/**
 * Regressionstest für den Freeze-Bug aus Commit {@code ea3000fa}: {@code turnierVerbinden()} lädt
 * die Online-Turnierliste per Netzwerk-Aufruf auf einem eigenen Hintergrund-Thread ("PTM-Online-
 * Verbinden") und zeigt danach den Auswahldialog. Dabei rief {@code zeigeAuswahlDialog()} direkt
 * {@code ProcessBox.hide()}/{@code istSichtbar()}/{@code visibleWennAutomatisch()} auf — VCL-/UNO-
 * UI-Zugriffe, die laut CLAUDE.md ("Threading: VCL/UNO-UI NUR auf dem LO-Main-Thread") nur auf dem
 * Main-Thread laufen dürfen. Symptom: "LibreOffice Calc antwortet nicht" beim Öffnen des Dialogs.
 * <p>
 * Der generische Call-Graph-Test {@link de.petanqueturniermanager.arch.ThreadingCallGraphArchTest}
 * deckt diese Klasse bewusst NICHT ab: ein pauschaler Off-Thread-Wurzel-Marker für jede Methode, die
 * {@code Thread.start()} aufruft, würde in diesem Projekt zu massivem Rauschen führen (fast jede
 * {@code SheetRunner}-Nutzung und jeder {@code MessageBox.show()}-Aufruf hängt indirekt von
 * {@code ProcessBox} ab, das intern aber bereits per {@code runOnMain(...)} korrekt marshallt —
 * nur {@code istSichtbar()} tut das NICHT). Dieser Test bleibt daher bewusst eng auf die konkrete
 * Off-Thread-Methode dieser Klasse beschränkt.
 * <p>
 * {@link #postZielMethodenRuehrenThreadJoinNichtAn()} sichert einen zweiten, verwandten Bug ab: die
 * per {@code LoMainThread.post(...)} auf den Main-Thread marshallten Methoden riefen (vor dem Fix)
 * transitiv {@code SheetRunner.start()}+{@code .join()} auf — der Main-Thread haelt beim Ausfuehren
 * des Callbacks bereits den SolarMutex, das {@code join()} auf den neu gestarteten Sheet-Thread
 * deadlockte. Der reale Aufrufpfad verlaesst dabei {@code PtmOnlineDispatcher} (ueber
 * {@code PtmOnlineRegistrationMapping} bis hinein in {@code PtmOnlineSyncSheet}, wo
 * {@code SheetRunner.start()+.join()} tatsaechlich passiert)
 * — die Traversal MUSS daher klassenuebergreifend erfolgen (siehe {@link #erreichbar}, analog zum
 * BFS-Muster in {@code ThreadingCallGraphArchTest}, dort ueber {@code JavaCodeUnit}+
 * {@code target.resolveMember()}).
 * <p>
 * Ein Versuch, das als generelles projektweites Call-Graph-Gate (jede Methode, die
 * {@code LoMainThread.post} aufruft, als Wurzel) umzusetzen, wurde verworfen: die BFS ist
 * ordnungs-unempfindlich und markierte auch den bereits gefixten Code als Verstoss, weil das
 * (sichere) {@code join()} im selben Methodenkoerper VOR dem {@code post(...)}-Aufruf liegt, nicht
 * darin. Dieser Test prueft daher gezielt nur die tatsaechlichen Post-Ziel-Methoden.
 * <p>
 * Seit Verbinden und Trennen komplett in einem SheetRunner laufen ({@link PtmOnlineVerbindenRunner},
 * {@link PtmOnlineVerbindungsRunner}), meldet der Runner den Erfolg selbst; der Dispatcher postet nur noch
 * Fehlermeldungen.
 */
class PtmOnlineDispatcherThreadingTest {

    private static final String PROCESS_BOX = "de.petanqueturniermanager.helper.msgbox.ProcessBox";
    private static final String THREAD_FQN = "java.lang.Thread";
    private static final String JOIN_METHODE = "join";

    /**
     * Methoden, die aktuell als {@code () -> methodeName(...)}-Ziel an {@code LoMainThread.post(...)}
     * uebergeben werden - laufen also auf dem LO-Main-Thread. Bei Erweiterung von
     * {@code PtmOnlineDispatcher} um weitere {@code LoMainThread.post}-Aufrufe hier ergaenzen.
     */
    private static final Set<String> POST_ZIEL_METHODEN = Set.of("zeigeFehler", "zeigeNetzwerkFehler");

    @Test
    void verbindenImHintergrundRuehrtProcessBoxNichtDirektAn() {
        JavaClasses classes = new ClassFileImporter().importPackages("de.petanqueturniermanager.ptmonline");
        JavaClass dispatcher = classes.get(PtmOnlineDispatcher.class);

        JavaMethod methode = dispatcher.getMethods().stream()
                .filter(m -> m.getName().equals("verbindenImHintergrund") || m.getName().equals("zeigeAuswahlDialog"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "PtmOnlineDispatcher.verbindenImHintergrund()/zeigeAuswahlDialog() nicht gefunden - "
                                + "Methode umbenannt? Diesen Test dann auf den neuen Namen anpassen."));

        Set<String> direkteProcessBoxAufrufe = erreichbar(methode,
                target -> target.getOwner().getFullName().equals(PROCESS_BOX));

        assertThat(direkteProcessBoxAufrufe)
                .as("PtmOnlineDispatcher.verbindenImHintergrund()/zeigeAuswahlDialog() läuft auf einem "
                        + "selbst erzeugten Hintergrund-Thread und darf ProcessBox NICHT direkt anfassen "
                        + "(siehe Bug-Fix-Commit ea3000fa / CLAUDE.md Threading-Regel). UI-Feedback muss "
                        + "per LoMainThread.post(...) auf den Main-Thread marshallt werden.")
                .isEmpty();
    }

    @Test
    void postZielMethodenRuehrenThreadJoinNichtAn() {
        JavaClasses classes = new ClassFileImporter().importPackages("de.petanqueturniermanager");
        JavaClass dispatcher = classes.get(PtmOnlineDispatcher.class);

        Set<String> gefunden = new HashSet<>();
        for (String zielMethode : POST_ZIEL_METHODEN) {
            JavaMethod methode = dispatcher.getMethods().stream()
                    .filter(m -> m.getName().equals(zielMethode))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "PtmOnlineDispatcher." + zielMethode + "() nicht gefunden - Methode umbenannt? "
                                    + "POST_ZIEL_METHODEN anpassen."));
            gefunden.addAll(erreichbar(methode,
                    target -> target.getName().equals(JOIN_METHODE) && target.getOwner().isAssignableTo(THREAD_FQN)));
        }

        assertThat(gefunden)
                .as("Die per LoMainThread.post(...) auf den Main-Thread marshallten Methoden ("
                        + POST_ZIEL_METHODEN + ") duerfen kein Thread.join()/SheetRunner.join() erreichen - "
                        + "der Main-Thread haelt beim Ausfuehren des Callbacks bereits den SolarMutex "
                        + "(Deadlock-Risiko, siehe Klassen-Javadoc).")
                .isEmpty();
    }

    /**
     * Klassenübergreifende BFS über den Aufruf-Graph ab {@code wurzel} (Muster analog
     * {@code ThreadingCallGraphArchTest.erreichtKeineVclSenkeAusFremdThread}): jeder Treffer auf
     * {@code istTreffer} wird gesammelt, jeder aufgelöste Aufruf-Ziel-{@link JavaCodeUnit} wird
     * unabhängig von seiner Owner-Klasse weiterverfolgt.
     */
    private static Set<String> erreichbar(JavaCodeUnit wurzel, Predicate<MethodCallTarget> istTreffer) {
        Set<String> gefunden = new HashSet<>();
        Set<JavaCodeUnit> besucht = new HashSet<>();
        Deque<JavaCodeUnit> queue = new ArrayDeque<>();
        queue.add(wurzel);
        while (!queue.isEmpty()) {
            JavaCodeUnit aktuell = queue.poll();
            if (!besucht.add(aktuell)) {
                continue;
            }
            for (JavaMethodCall call : aktuell.getMethodCallsFromSelf()) {
                MethodCallTarget target = call.getTarget();
                if (istTreffer.test(target)) {
                    gefunden.add(target.getOwner().getSimpleName() + "." + target.getName());
                }
                target.resolveMember().ifPresent(queue::add);
            }
        }
        return gefunden;
    }
}
