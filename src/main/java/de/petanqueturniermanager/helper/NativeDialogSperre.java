package de.petanqueturniermanager.helper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Markiert, dass gerade ein natives modales Dialogfenster mit eigener Event-Loop offen ist
 * (z. B. der UNO-ColorPicker via {@link de.petanqueturniermanager.helper.farbe.FarbwahlDialog}).
 *
 * <p>Waehrend ein solcher Dialog offen ist, duerfen Hintergrund-Threads ausserhalb des
 * LO-Main-Threads keine UNO-Zugriffe ausloesen: die verschachtelte native Event-Loop
 * (GTK/glib) des Dialogs kollidiert sonst mit gleichzeitigem UNO-Zugriff aus einem Fremd-Thread –
 * beobachtet als SIGABRT durch Stack-Corruption in {@code ColorDialog::Execute()}
 * (per CoreDump-Analyse verifiziert, korreliert mit aktivem Webserver-Hintergrundbetrieb).
 */
public final class NativeDialogSperre {

    private static final AtomicInteger offeneDialoge = new AtomicInteger(0);

    private NativeDialogSperre() {}

    /** Vor dem Öffnen eines nativen modalen Dialogs aufrufen. */
    public static void eintreten() {
        offeneDialoge.incrementAndGet();
    }

    /** Nach dem Schließen des Dialogs aufrufen (immer im {@code finally}). */
    public static void verlassen() {
        offeneDialoge.decrementAndGet();
    }

    /** Ob aktuell mindestens ein natives modales Dialogfenster offen ist. */
    public static boolean istOffen() {
        return offeneDialoge.get() > 0;
    }
}
