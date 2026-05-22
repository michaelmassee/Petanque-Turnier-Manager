/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheetsync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;

/**
 * Regressionstest für den LO-Crash beim Blättern zwischen Spieltagen.
 * <p>
 * Hintergrund: {@link SheetSyncListener#pruefeUndStarte} läuft auf dem
 * {@code PTM-SheetSyncDebouncer}-Daemon-Thread. Schrieb der Store dort
 * via {@link DocumentPropertiesHelper#setStringProperty} Hash- und
 * Timestamp-Properties, feuerte das synchron einen
 * {@link de.petanqueturniermanager.comp.turnierevent.TurnierEventType#PropertiesChanged}-Event
 * — die Listener (z.B. {@code BaseSidebarContent}) führten daraufhin auf dem
 * Daemon-Thread VCL-Operationen ({@code window.dispose()}) aus → SIGSEGV.
 * <p>
 * Dieser Test sichert das Verhalten in beide Richtungen ab:
 * <ul>
 *   <li>Alle {@link SheetSyncSignaturStore}-Writes feuern <b>kein</b> TurnierEvent.</li>
 *   <li>Reguläres {@link DocumentPropertiesHelper#setStringProperty} feuert weiterhin
 *       — Sanity-Check, damit der Test nicht stillschweigend false-positive wird.</li>
 * </ul>
 */
class SheetSyncSignaturStoreSilentWriteUITest extends BaseCalcUITest {

    private static final String TEST_SCHLUESSEL = "ranking-test-silent";

    @Test
    void storeWritesFeuernKeineTurnierEvents() {
        XSpreadsheetDocument xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
        EventSammler sammler = new EventSammler();
        PetanqueTurnierMngrSingleton.addTurnierEventListener(sammler);
        try {
            SheetSyncSignaturStore.speichereNachRebuild(xDoc, TEST_SCHLUESSEL, "hash-1", "test");
            assertThat(sammler.events())
                    .as("speichereNachRebuild darf kein TurnierEvent triggern")
                    .isEmpty();

            SheetSyncSignaturStore.aktualisiereVerifyZeit(xDoc, TEST_SCHLUESSEL);
            assertThat(sammler.events())
                    .as("aktualisiereVerifyZeit darf kein TurnierEvent triggern")
                    .isEmpty();

            SheetSyncSignaturStore.markiereRecoveryVersucht(xDoc, TEST_SCHLUESSEL);
            assertThat(sammler.events())
                    .as("markiereRecoveryVersucht darf kein TurnierEvent triggern")
                    .isEmpty();

            // Erneuter Write mit anderem Hash – Cache-Pfad würde sonst falsch-negativ skippen
            SheetSyncSignaturStore.speichereNachRebuild(xDoc, TEST_SCHLUESSEL, "hash-2", "test");
            assertThat(sammler.events())
                    .as("zweiter speichereNachRebuild mit anderem Hash darf weiterhin kein Event feuern")
                    .isEmpty();
        } finally {
            PetanqueTurnierMngrSingleton.removeTurnierEventListener(sammler);
        }
    }

    @Test
    void regulaeresSetStringPropertyFeuertWeiterhin() {
        EventSammler sammler = new EventSammler();
        PetanqueTurnierMngrSingleton.addTurnierEventListener(sammler);
        try {
            docPropHelper.setStringProperty("test.event.sanity", "wert-" + System.nanoTime());
            assertThat(sammler.events())
                    .as("setStringProperty mit echtem Wert-Wechsel muss weiterhin ein Event feuern "
                            + "(Sanity-Check: Listener-Framework funktioniert im Test)")
                    .hasSize(1);
        } finally {
            PetanqueTurnierMngrSingleton.removeTurnierEventListener(sammler);
        }
    }

    private static final class EventSammler implements ITurnierEventListener {
        private final List<ITurnierEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void onPropertiesChanged(ITurnierEvent eventObj) {
            events.add(eventObj);
        }

        List<ITurnierEvent> events() {
            return events;
        }
    }
}
