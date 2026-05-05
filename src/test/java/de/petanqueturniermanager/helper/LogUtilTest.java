package de.petanqueturniermanager.helper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class LogUtilTest {

	@Test
	public void errorMitNormalemKontextUndMessage() {
		Logger logger = mock(Logger.class);
		Throwable t = new IllegalStateException("Boom");

		LogUtil.error(logger, "Operation X fehlgeschlagen", t);

		verify(logger).error("{} | {}", "Operation X fehlgeschlagen", "Boom", t);
	}

	@Test
	public void errorFaelltAufExceptionKlassenNamenZurueckBeiNullMessage() {
		Logger logger = mock(Logger.class);
		Throwable t = new NullPointerException();

		LogUtil.error(logger, "Operation X fehlgeschlagen", t);

		verify(logger).error("{} | {}", "Operation X fehlgeschlagen", "NullPointerException", t);
	}

	@Test
	public void errorFaelltAufKontextSentinelZurueckBeiNullKontext() {
		Logger logger = mock(Logger.class);
		Throwable t = new IllegalStateException("Boom");

		LogUtil.error(logger, null, t);

		verify(logger).error("{} | {}", "Kein Kontext", "Boom", t);
	}

	@Test
	public void errorFaelltAufKontextSentinelZurueckBeiBlankKontext() {
		Logger logger = mock(Logger.class);
		Throwable t = new IllegalStateException("Boom");

		LogUtil.error(logger, "   ", t);

		verify(logger).error("{} | {}", "Kein Kontext", "Boom", t);
	}

	@Test
	public void warnNutztWarnLevel() {
		Logger logger = mock(Logger.class);
		Throwable t = new IllegalStateException("Soft");

		LogUtil.warn(logger, "Recoverable Operation", t);

		verify(logger).warn("{} | {}", "Recoverable Operation", "Soft", t);
	}

	@Test
	public void warnFaelltZurueckBeiNullMessageUndNullKontext() {
		Logger logger = mock(Logger.class);
		Throwable t = new NullPointerException();

		LogUtil.warn(logger, null, t);

		verify(logger).warn(eq("{} | {}"), eq("Kein Kontext"), eq("NullPointerException"), any(Throwable.class));
	}
}
