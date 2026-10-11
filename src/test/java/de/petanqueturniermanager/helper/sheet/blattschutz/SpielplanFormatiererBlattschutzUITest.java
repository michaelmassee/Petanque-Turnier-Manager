/*
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet.blattschutz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheetsync.SpielplanFormatiererKonfig;
import de.petanqueturniermanager.helper.sheetsync.SpielplanFormatiererSheetRunner;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Regression: Der {@link SpielplanFormatiererSheetRunner} läuft bei jedem Tab-Wechsel. Ist das
 * Blatt bereits korrekt formatiert und geschützt, darf er im Turniermodus keinen
 * Protect/Unprotect-Zyklus über alle Turnierblätter auslösen. Ein ungeschützt vorgefundenes
 * Blatt muss er dagegen weiterhin wieder schützen.
 */
class SpielplanFormatiererBlattschutzUITest extends BaseCalcUITest {

    private static final int GERADE_FARBE = 0xE0E0E0;
    private static final int UNGERADE_FARBE = 0xFFFFFF;
    private static final RangePosition DATEN_RANGE = RangePosition.from(0, 0, 3, 9);

    private XSpreadsheet sheet;
    private XProtectable protectable;
    private IBlattschutzKonfiguration blattschutzKonfig;

    @BeforeEach
    void vorbereiten() throws GenerateException {
        sheet = sheetHlp.getSheetByIdx(0);
        protectable = Lo.qi(XProtectable.class, sheet);
        blattschutzKonfig = mock(IBlattschutzKonfiguration.class);
        when(blattschutzKonfig.berechneSchutzInfos(any())).thenReturn(List.of());
        BlattschutzManager.get().resetCallCounters();
    }

    @AfterEach
    void aufraeumen() {
        TurnierModus.get().setAktivForTest(false);
        BlattschutzManager.get().resetScopeForTest();
        BlattschutzManager.get().resetCallCounters();
        protectable.unprotect("");
    }

    @Test
    void korrektFormatiertUndGeschuetzt_imTurniermodus_keinProtectZyklus() throws Exception {
        SheetHelper.faerbeZeilenAbwechselnd(neuerRunner(), DATEN_RANGE, GERADE_FARBE, UNGERADE_FARBE);
        protectable.protect("");
        TurnierModus.get().setAktivForTest(true);

        neuerRunner().run();

        assertThat(BlattschutzManager.get().getProtectCallCount()).isZero();
        assertThat(BlattschutzManager.get().getUnprotectCallCount()).isZero();
    }

    @Test
    void korrektFormatiertAberUngeschuetzt_imTurniermodus_schuetztErneut() throws Exception {
        SheetHelper.faerbeZeilenAbwechselnd(neuerRunner(), DATEN_RANGE, GERADE_FARBE, UNGERADE_FARBE);
        TurnierModus.get().setAktivForTest(true);

        neuerRunner().run();

        assertThat(BlattschutzManager.get().getProtectCallCount()).isEqualTo(1);
    }

    @Test
    void abweichendeZebraFarbe_wirdErkanntUndRepariert() throws Exception {
        ISheet iSheet = neuerRunner();
        SheetHelper.faerbeZeilenAbwechselnd(iSheet, DATEN_RANGE, GERADE_FARBE, UNGERADE_FARBE);
        Lo.qi(XPropertySet.class, sheet.getCellByPosition(2, 5)).setPropertyValue("CellBackColor", 0xFF0000);
        assertThat(SheetHelper.brauchtZebraReparatur(iSheet, DATEN_RANGE, GERADE_FARBE, UNGERADE_FARBE))
                .isTrue();

        neuerRunner().run();

        assertThat(SheetHelper.brauchtZebraReparatur(iSheet, DATEN_RANGE, GERADE_FARBE, UNGERADE_FARBE))
                .isFalse();
        assertThat(BlattschutzManager.get().getProtectCallCount()).isZero();
    }

    /** Der Runner ist zugleich das {@link ISheet} des Zielblatts – so auch ohne Lauf für die Helper nutzbar. */
    private SpielplanFormatiererSheetRunner neuerRunner() {
        return new SpielplanFormatiererSheetRunner(wkingSpreadsheet, sheet,
                iSheet -> new SpielplanFormatiererKonfig(DATEN_RANGE, List.of(),
                        GERADE_FARBE, UNGERADE_FARBE, blattschutzKonfig));
    }
}
