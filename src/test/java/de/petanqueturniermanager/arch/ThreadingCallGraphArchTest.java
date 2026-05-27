/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.arch;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * Architektur-Konventionstest (Call-Graph) zur Threading-Regel „VCL/UNO-UI nur auf dem LO-Main-Thread"
 * (siehe CLAUDE.md, Abschnitt <em>Threading</em>). Ergänzt den heuristischen
 * {@link de.petanqueturniermanager.sidebar.HintergrundListenerVclKonventionTest Quelltext-Scan} um eine
 * <b>klassenübergreifende</b> Erreichbarkeitsanalyse und schließt damit dessen Helper-Klassen-Lücke
 * (Bug-Fix-Commit {@code 2c2ab188}: Off-Thread-VCL lag in der Hilfsklasse
 * {@code SpieltagToolbarSteuerung}, aufgerufen aus {@code ProtocolHandler.notifyAllListeners}).
 *
 * <h2>Funktionsweise</h2>
 * Ab jeder <b>Off-Thread-Wurzel</b> ({@link #ROOT_INTERFACES} bzw. {@link #notifyAllListenersWurzel})
 * wird der Aufruf-Graph per BFS verfolgt. Jede erreichbare <b>UNO-UI/VCL-Senke</b> ({@link #SINKS})
 * erzeugt einen Verstoß. Das Ergebnis ist in eine {@link FreezingArchRule} gewrappt.
 *
 * <h2>Warum Freeze statt hartes „kein VCL erreichbar"</h2>
 * Ein Spike hat gezeigt: ArchUnit faltet inline-Lambdas (z.B. {@code LoMainThread.post(ctx, () -> …)})
 * in die umschließende Methode — der Marshalling-Schnitt ist im Call-Graph <b>unsichtbar</b>. Eine
 * naive Regel würde korrekt-marshallten Code als Verstoß melden. Die {@link FreezingArchRule} friert
 * den heute bekannten (reviewten) Bestand ein und lässt den Build nur bei <b>NEU</b> hinzukommenden
 * Off-Thread→VCL-Kanten fehlschlagen — genau die Lücke, die {@code 2c2ab188} aufdeckte.
 *
 * <h2>Grenzen (bewusst)</h2>
 * <ul>
 *   <li>Eingefrorene Einträge sind <b>nicht</b> automatisch „ok" — beim (Re-)Freeze ist manuell zu
 *       prüfen, ob der Pfad wirklich über {@code LoMainThread.post}/{@code runOnMain} marshallt.</li>
 *   <li>Methodenreferenzen, Reflection und UNO-Dispatch werden nicht vollständig erfasst → Heuristik,
 *       kein 100%-Beweis. Der Quelltext-Scan-Test bleibt komplementär bestehen.</li>
 * </ul>
 *
 * <h2>Store neu einfrieren</h2>
 * Nach bewusster, reviewter Änderung: einmalig mit {@code -Darchunit.freeze.refreeze=true} laufen
 * (bzw. {@code freeze.refreeze=true} in {@code archunit.properties}), den aktualisierten Store unter
 * {@code config/archunit/frozen-threading-violations/} committen, Flag wieder zurücknehmen.
 */
public class ThreadingCallGraphArchTest {

    /**
     * Interfaces, deren Implementierungen aus Fremd-Threads gerufen werden (siehe CLAUDE.md). Eine
     * Methode gilt als Wurzel, wenn ihr Owner eines dieser Interfaces implementiert und der
     * Methodenname dort deklariert ist.
     */
    private static final Set<String> ROOT_INTERFACES = Set.of(
            "de.petanqueturniermanager.timer.TimerListener",
            "de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener",
            "de.petanqueturniermanager.comp.adapter.IGlobalEventListener");

    /** Off-Thread-Wurzel, die nicht über ein Interface, sondern per Methodenreferenz registriert wird. */
    private static final String NOTIFY_OWNER = "de.petanqueturniermanager.comp.ProtocolHandler";
    private static final String NOTIFY_METHODE = "notifyAllListeners";

    /**
     * Verbotene UNO-UI/VCL-Ziele (Owner-FQN → Methodennamen). Kuratierte Startmenge; jede Erweiterung
     * erfordert ein Re-Freeze. Bewusst auf eindeutige UI-Mutationen beschränkt.
     */
    private static final Map<String, Set<String>> SINKS = Map.of(
            "com.sun.star.frame.XLayoutManager", Set.of("requestElement", "showElement", "hideElement"),
            "com.sun.star.awt.XFixedText", Set.of("setText"),
            "com.sun.star.awt.XWindow", Set.of("setVisible"),
            "com.sun.star.awt.XListBox", Set.of("addItem", "addItems", "selectItemPos"));

    @Test
    void keineNeuenOffThreadVclPfade() {
        JavaClasses classes = new ClassFileImporter().importPackages("de.petanqueturniermanager");

        ArchRule regel = ArchRuleDefinition.methods()
                .should(erreichtKeineVclSenkeAusFremdThread())
                .as("Off-Thread-Listener-Wurzeln dürfen keine UNO-UI/VCL-Senke erreichen "
                        + "(VCL/UNO-UI nur auf dem LO-Main-Thread, siehe CLAUDE.md)");

        FreezingArchRule.freeze(regel).check(classes);
    }

    private static ArchCondition<JavaMethod> erreichtKeineVclSenkeAusFremdThread() {
        return new ArchCondition<>("nicht aus einem Fremd-Thread-Callback eine VCL-Senke erreichen") {
            @Override
            public void check(JavaMethod wurzel, ConditionEvents events) {
                if (!istOffThreadWurzel(wurzel)) {
                    return;
                }
                Set<JavaCodeUnit> besucht = new HashSet<>();
                Deque<JavaCodeUnit> queue = new ArrayDeque<>();
                queue.add(wurzel);
                while (!queue.isEmpty()) {
                    JavaCodeUnit aktuell = queue.poll();
                    if (!besucht.add(aktuell)) {
                        continue;
                    }
                    for (JavaMethodCall call : aktuell.getMethodCallsFromSelf()) {
                        var target = call.getTarget();
                        if (istVclSenke(target.getOwner().getFullName(), target.getName())) {
                            events.add(SimpleConditionEvent.violated(wurzel,
                                    "Off-Thread-Wurzel " + wurzel.getFullName()
                                            + " erreicht UI/VCL-Senke "
                                            + target.getOwner().getFullName() + "." + target.getName()
                                            + " (aufgerufen in " + aktuell.getFullName() + ")"));
                        }
                        target.resolveMember().ifPresent(member -> {
                            if (member instanceof JavaCodeUnit codeUnit) {
                                queue.add(codeUnit);
                            }
                        });
                    }
                }
            }
        };
    }

    private static boolean istOffThreadWurzel(JavaMethod methode) {
        if (methode.getOwner().getFullName().equals(NOTIFY_OWNER)
                && methode.getName().equals(NOTIFY_METHODE)) {
            return true;
        }
        for (JavaClass iface : methode.getOwner().getAllRawInterfaces()) {
            if (ROOT_INTERFACES.contains(iface.getFullName())
                    && iface.tryGetMethod(methode.getName()).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean istVclSenke(String ownerFullName, String methodenName) {
        Set<String> methoden = SINKS.get(ownerFullName);
        return methoden != null && methoden.contains(methodenName);
    }
}
