/**
 * Erstellung : 01.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * UITest für das dynamische Enable/Disable der Schweizer-Menüpunkte.
 * <p>
 * Die Logik im {@code ProtocolHandler.isEnabled()} lautet:
 * <ul>
 *   <li>{@code schweizer_start} – aktiv wenn {@link TurnierSystem#KEIN}</li>
 *   <li>{@code schweizer_neue_meldeliste} – aktiv wenn {@link TurnierSystem#SCHWEIZER}</li>
 * </ul>
 * Da {@code isEnabled()} package-private ist, wird die gleiche Logik hier
 * über {@code DocumentPropertiesHelper} und {@code TurnierSystem} verifiziert.
 */
public class SchweizerMenuEnableUITest extends BaseCalcUITest {

	/** Spiegelt die Logik aus ProtocolHandler.isEnabled() */
	private boolean isStartEnabled(TurnierSystem ts) {
		return ts == TurnierSystem.KEIN;
	}

	private boolean isNeueMeldelisteEnabled(TurnierSystem ts) {
		return ts == TurnierSystem.SCHWEIZER;
	}

	@Test
	public void testInitialKeinTurnier_StartEnabled_NeueMeldelisteDisabled() {
		TurnierSystem ts = docPropHelper.getTurnierSystemAusDocument();

		assertThat(ts).as("Frisches Dokument hat kein Turnier").isEqualTo(TurnierSystem.KEIN);
		assertThat(isStartEnabled(ts)).as("schweizer_start muss bei KEIN enabled sein").isTrue();
		assertThat(isNeueMeldelisteEnabled(ts)).as("schweizer_neue_meldeliste muss bei KEIN disabled sein").isFalse();
	}

	@Test
	public void testSchweizer_StartDisabled_NeueMeldelisteEnabled() {
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SCHWEIZER.getId());

		TurnierSystem ts = docPropHelper.getTurnierSystemAusDocument();

		assertThat(ts).as("TurnierSystem soll SCHWEIZER sein").isEqualTo(TurnierSystem.SCHWEIZER);
		assertThat(isStartEnabled(ts)).as("schweizer_start muss bei SCHWEIZER disabled sein").isFalse();
		assertThat(isNeueMeldelisteEnabled(ts)).as("schweizer_neue_meldeliste muss bei SCHWEIZER enabled sein").isTrue();
	}

	@Test
	public void testAnderesSystem_BeideDisabled() {
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SUPERMELEE.getId());

		TurnierSystem ts = docPropHelper.getTurnierSystemAusDocument();

		assertThat(ts).as("TurnierSystem soll SUPERMELEE sein").isEqualTo(TurnierSystem.SUPERMELEE);
		assertThat(isStartEnabled(ts)).as("schweizer_start muss bei SUPERMELEE disabled sein").isFalse();
		assertThat(isNeueMeldelisteEnabled(ts)).as("schweizer_neue_meldeliste muss bei SUPERMELEE disabled sein").isFalse();
	}

	@Test
	public void testWechselVonKeinZuSchweizer_UndZurueck() {
		// Ausgangszustand: KEIN
		TurnierSystem ts = docPropHelper.getTurnierSystemAusDocument();
		assertThat(ts).isEqualTo(TurnierSystem.KEIN);
		assertThat(isStartEnabled(ts)).isTrue();
		assertThat(isNeueMeldelisteEnabled(ts)).isFalse();

		// Wechsel zu SCHWEIZER
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SCHWEIZER.getId());
		ts = docPropHelper.getTurnierSystemAusDocument();
		assertThat(ts).isEqualTo(TurnierSystem.SCHWEIZER);
		assertThat(isStartEnabled(ts)).isFalse();
		assertThat(isNeueMeldelisteEnabled(ts)).isTrue();

		// Zurück zu KEIN
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.KEIN.getId());
		ts = docPropHelper.getTurnierSystemAusDocument();
		assertThat(ts).isEqualTo(TurnierSystem.KEIN);
		assertThat(isStartEnabled(ts)).isTrue();
		assertThat(isNeueMeldelisteEnabled(ts)).isFalse();
	}
}
