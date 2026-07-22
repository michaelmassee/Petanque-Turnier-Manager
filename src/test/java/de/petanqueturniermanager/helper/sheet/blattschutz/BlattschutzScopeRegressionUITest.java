/*
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet.blattschutz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Regression: Sheet-Schreibvorgänge aus Spieler-DB-Dialogen
 * (siehe {@code SpielerDbDispatcher}) laufen <em>außerhalb</em> eines
 * {@code SheetRunner} und müssen sich daher selbst um einen aktiven
 * {@link BlattschutzManager}-Command-Scope kümmern – über die Convenience-API
 * {@link BlattschutzManager#scopeFuer}.
 *
 * <p>Hintergrund des Bugs: seit der Lazy-Unprotect-Erweiterung von
 * {@code RangeHelper.setDataInRange} wirft {@link BlattschutzManager#ensureUnprotectedInScope}
 * im Turnier-Modus, wenn kein Scope offen ist. Vor dem Fix crashte deshalb
 * jeder Schreibvorgang aus {@code SheetMeldelisteAdapter.schreibeBlock} bzw.
 * den Vorlage-Sheet-Pfaden mit
 * {@code IllegalStateException: Style/CF-Operation außerhalb eines aktiven BlattschutzScopes}.
 */
public class BlattschutzScopeRegressionUITest extends BaseCalcUITest {

    /**
     * Im aktiven Turnier-Modus muss {@code ensureUnprotectedInScope} ohne Scope
     * weiterhin werfen – das ist die Schutz-Invariante, die der Fix nicht
     * aufweichen darf.
     */
    @Test
    void turnierModusAktiv_ohneScope_wirftIllegalStateException() {
        TurnierModus.get().setAktivForTest(true);
        try {
            assertThatThrownBy(() -> BlattschutzManager.get().ensureUnprotectedInScope())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("BlattschutzScopes");
        } finally {
            TurnierModus.get().setAktivForTest(false);
        }
    }

    /**
     * {@link BlattschutzManager#scopeFuer} öffnet im Turnier-Modus einen
     * Command-Scope, in dem {@code ensureUnprotectedInScope} <em>nicht</em>
     * mehr wirft. Nach {@code close()} ist der Scope wieder zu und die
     * Schutz-Invariante greift erneut.
     */
    @Test
    void scopeFuer_turnierModusAktiv_oeffnetScope() {
        TurnierModus.get().setAktivForTest(true);
        try (var _ = BlattschutzManager.get().scopeFuer(TurnierSystem.SCHWEIZER, wkingSpreadsheet)) {
            assertThatCode(() -> BlattschutzManager.get().ensureUnprotectedInScope())
                    .doesNotThrowAnyException();
        } finally {
            assertThatThrownBy(() -> BlattschutzManager.get().ensureUnprotectedInScope())
                    .isInstanceOf(IllegalStateException.class);
            TurnierModus.get().setAktivForTest(false);
        }
    }

    /**
     * Bei deaktiviertem Turnier-Modus liefert {@code scopeFuer} ein no-op:
     * weder wird ein echter Scope geöffnet, noch wirft {@code ensureUnprotectedInScope}.
     */
    @Test
    void scopeFuer_ohneTurnierModus_istNoOp() {
        try (var _ = BlattschutzManager.get().scopeFuer(TurnierSystem.SCHWEIZER, wkingSpreadsheet)) {
            assertThatCode(() -> BlattschutzManager.get().ensureUnprotectedInScope())
                    .doesNotThrowAnyException();
        }
    }

    /**
     * Ohne aktives Turniersystem ({@code TurnierSystem.KEIN}) liefert
     * {@code scopeFuer} ebenfalls ein no-op – relevant für die Vorlage-Pfade
     * im Dispatcher, die auch ohne registriertes Turniersystem laufen.
     */
    @Test
    void scopeFuer_ohneTurnierSystem_istNoOp() {
        TurnierModus.get().setAktivForTest(true);
        try (var _ = BlattschutzManager.get().scopeFuer(TurnierSystem.KEIN, wkingSpreadsheet)) {
            // im Turnier-Modus + KEIN-System bleibt der Scope leer, daher würde
            // ensureUnprotectedInScope wieder werfen – wichtig ist nur, dass
            // scopeFuer selbst nicht wirft und sauber durchläuft.
        } finally {
            TurnierModus.get().setAktivForTest(false);
        }
    }

    @Test
    void setDataInRange_ausserhalbScope_stelltPhysischenBlattschutzWiederHer() throws GenerateException {
        XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);
        XProtectable protectable = Lo.qi(XProtectable.class, sheet);
        protectable.protect("");
        assertThat(protectable.isProtected()).isTrue();

        RangeHelper.from(sheet, doc, RangePosition.from(0, 0))
                .setDataInRange(new RangeData(new Object[][] { { "Fallback" } }));

        assertThat(protectable.isProtected()).isTrue();
    }
}
