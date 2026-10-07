/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Laufzeit-Guard gegen Zebra-Zeilenfarbe als bedingte Formatierung (siehe
 * {@code ZebraNichtPerConditionalFormatKonventionTest}).
 */
class ConditionalFormatHelperZebraGuardTest {

	@Test
	void reineZebraFormelnWerdenErkannt() {
		assertThat(ConditionalFormatHelper.istReineZebraFormel(ConditionalFormatHelper.FORMULA_ISEVEN_ROW)).isTrue();
		assertThat(ConditionalFormatHelper.istReineZebraFormel(ConditionalFormatHelper.FORMULA_ISODD_ROW)).isTrue();
		assertThat(ConditionalFormatHelper.istReineZebraFormel(" iseven( ROW () ) ")).isTrue();
	}

	@Test
	void zusammengesetzteBedingungenBleibenErlaubt() {
		String editierfarbe = "AND(" + ConditionalFormatHelper.FORMULA_ISEVEN_ROW + ";"
				+ GlobalImpl.FORMAT_PTM_BOOLEAN_PROPERTY(EditierbaresZelleFormatHelper.PROPERTY_KEY) + ")";
		assertThat(ConditionalFormatHelper.istReineZebraFormel(editierfarbe)).isFalse();
		assertThat(ConditionalFormatHelper.istReineZebraFormel(ConditionalFormatHelper.FORMULAISODDANDEQUALTOINT_STR(1)))
				.isFalse();
		assertThat(ConditionalFormatHelper.istReineZebraFormel("ISTEXT(A1)")).isFalse();
		assertThat(ConditionalFormatHelper.istReineZebraFormel(null)).isFalse();
	}

	@Test
	void reineZebraFormelWirdUebersprungenOhneDieGenerierungAbzubrechen() {
		ISheet sheet = mock(ISheet.class);
		ConditionalFormatHelper helper = ConditionalFormatHelper.from(sheet, RangePosition.from(0, 0, 0, 10));

		assertThatCode(() -> helper.formula1(ConditionalFormatHelper.FORMULA_ISEVEN_ROW).isFormula()
				.style("Default").applyAndDoReset())
				.as("Eine verbotene Zebra-Regel darf die Sheet-Generierung nicht abbrechen")
				.doesNotThrowAnyException();
		verifyNoInteractions(sheet);
	}
}
