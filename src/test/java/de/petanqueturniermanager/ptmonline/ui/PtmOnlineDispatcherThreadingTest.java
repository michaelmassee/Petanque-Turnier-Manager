/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
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
 */
class PtmOnlineDispatcherThreadingTest {

    private static final String PROCESS_BOX = "de.petanqueturniermanager.helper.msgbox.ProcessBox";

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

        Set<String> direkteProcessBoxAufrufe = new HashSet<>();
        sammleProcessBoxAufrufe(dispatcher, methode.getName(), direkteProcessBoxAufrufe, new HashSet<>());

        assertThat(direkteProcessBoxAufrufe)
                .as("PtmOnlineDispatcher.verbindenImHintergrund()/zeigeAuswahlDialog() läuft auf einem "
                        + "selbst erzeugten Hintergrund-Thread und darf ProcessBox NICHT direkt anfassen "
                        + "(siehe Bug-Fix-Commit ea3000fa / CLAUDE.md Threading-Regel). UI-Feedback muss "
                        + "per LoMainThread.post(...) auf den Main-Thread marshallt werden.")
                .isEmpty();
    }

    /** Verfolgt Methodenaufrufe innerhalb der {@code PtmOnlineDispatcher}-Klasse rekursiv. */
    private static void sammleProcessBoxAufrufe(JavaClass dispatcherClass, String methodenName,
            Set<String> gefunden, Set<String> besucht) {
        if (!besucht.add(methodenName)) {
            return;
        }
        for (JavaMethod methode : dispatcherClass.getMethods()) {
            if (!methode.getName().equals(methodenName)) {
                continue;
            }
            for (JavaMethodCall call : methode.getMethodCallsFromSelf()) {
                var target = call.getTarget();
                if (target.getOwner().getFullName().equals(PROCESS_BOX)) {
                    gefunden.add(target.getName());
                }
                if (target.getOwner().equals(dispatcherClass)) {
                    sammleProcessBoxAufrufe(dispatcherClass, target.getName(), gefunden, besucht);
                }
            }
        }
    }
}
