/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XCalculatable;

import de.petanqueturniermanager.exception.GenerateException;

class NeuberechnungTest {

	@Test
	void ausserhalbEingabeschonendBleibtSchnittstelleUnveraendert() {
		XCalculatable calc = mock(XCalculatable.class);

		assertThat(Neuberechnung.fuer(calc)).isSameAs(calc);
		assertThat(Neuberechnung.istEingabeschonend()).isFalse();
	}

	@Test
	void eingabeschonendRechnetBeiAutomatikNichtNach() throws GenerateException {
		XCalculatable calc = mock(XCalculatable.class);
		when(calc.isAutomaticCalculationEnabled()).thenReturn(true);

		Neuberechnung.eingabeschonend(() -> {
			XCalculatable schonend = Neuberechnung.fuer(calc);
			schonend.calculate();
			schonend.calculateAll();
		});

		verify(calc, never()).calculate();
		verify(calc, never()).calculateAll();
	}

	@Test
	void eingabeschonendRechnetOhneAutomatikNach() throws GenerateException {
		XCalculatable calc = mock(XCalculatable.class);
		when(calc.isAutomaticCalculationEnabled()).thenReturn(false);

		Neuberechnung.eingabeschonend(() -> {
			XCalculatable schonend = Neuberechnung.fuer(calc);
			schonend.calculate();
			schonend.calculateAll();
		});

		verify(calc).calculate();
		verify(calc).calculateAll();
	}

	@Test
	void eingabeschonendLaesstAutomatikNichtAbschalten() throws GenerateException {
		XCalculatable calc = mock(XCalculatable.class);

		Neuberechnung.eingabeschonend(() -> {
			XCalculatable schonend = Neuberechnung.fuer(calc);
			schonend.enableAutomaticCalculation(false);
			schonend.enableAutomaticCalculation(true);
		});

		verify(calc, never()).enableAutomaticCalculation(false);
		verify(calc).enableAutomaticCalculation(true);
	}

	@Test
	void schalterGiltNurInnerhalbDesLaufs() throws GenerateException {
		AtomicReference<Boolean> innen = new AtomicReference<>();

		Neuberechnung.eingabeschonend(() -> innen.set(Neuberechnung.istEingabeschonend()));

		assertThat(innen.get()).isTrue();
		assertThat(Neuberechnung.istEingabeschonend()).isFalse();
	}

	@Test
	void nurFallsAutomatikAusRechnetNurOhneAutomatik() {
		XCalculatable mitAutomatik = mock(XCalculatable.class);
		when(mitAutomatik.isAutomaticCalculationEnabled()).thenReturn(true);
		XCalculatable ohneAutomatik = mock(XCalculatable.class);

		assertThat(Neuberechnung.nurFallsAutomatikAus(mitAutomatik)).isFalse();
		assertThat(Neuberechnung.nurFallsAutomatikAus(ohneAutomatik)).isTrue();

		verify(mitAutomatik, never()).calculate();
		verify(ohneAutomatik).calculate();
	}
}
