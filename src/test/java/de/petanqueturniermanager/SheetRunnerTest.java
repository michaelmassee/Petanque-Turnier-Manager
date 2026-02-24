/**
 * Erstellung 23.02.2026 / Michael Massee
 */
package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sun.star.sheet.XCalculatable;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public class SheetRunnerTest {

	static final Logger logger = LogManager.getLogger(SheetRunnerTest.class);

	private WorkingSpreadsheet workingSpreadsheetMock;
	private ProcessBox processBoxMock;
	private XCalculatable xCalculatableMock;
	private TestSheetRunner testRunner;

	@AfterEach
	public void tearDown() {
		// Statisches isRunning-Flag zurücksetzen, falls ein Test fehlgeschlagen ist
		// ohne den finally-Block von run() zu durchlaufen
		if (SheetRunner.isRunning()) {
			SheetRunner.cancelRunner();
		}
	}

	@BeforeEach
	public void setUp() {
		workingSpreadsheetMock = Mockito.mock(WorkingSpreadsheet.class);
		// RETURNS_SELF: alle fluent-API-Aufrufe (info, fehler, visible, ready, ...) geben das Mock zurück
		processBoxMock = Mockito.mock(ProcessBox.class, Mockito.RETURNS_SELF);
		xCalculatableMock = Mockito.mock(XCalculatable.class);

		Mockito.when(workingSpreadsheetMock.getxCalculatable()).thenReturn(xCalculatableMock);

		testRunner = new TestSheetRunner(workingSpreadsheetMock, TurnierSystem.KEIN, "TestPrefix", processBoxMock);
	}

	// --- Konstruktor / Getter ---

	@Test
	public void testGetTurnierSystem() {
		assertThat(testRunner.getTurnierSystem()).isEqualTo(TurnierSystem.KEIN);
	}

	@Test
	public void testGetTurnierSystem_SUPERMELEE() {
		TestSheetRunner runner = new TestSheetRunner(workingSpreadsheetMock, TurnierSystem.SUPERMELEE, null,
				processBoxMock);
		assertThat(runner.getTurnierSystem()).isEqualTo(TurnierSystem.SUPERMELEE);
	}

	@Test
	public void testGetLogPrefix() {
		assertThat(testRunner.getLogPrefix()).isEqualTo("TestPrefix");
	}

	@Test
	public void testGetLogPrefix_ohnePrefix_null() {
		TestSheetRunner runner = new TestSheetRunner(workingSpreadsheetMock, TurnierSystem.KEIN, null, processBoxMock);
		assertThat(runner.getLogPrefix()).isNull();
	}

	@Test
	public void testGetWorkingSpreadsheet() {
		assertThat(testRunner.getWorkingSpreadsheet()).isSameAs(workingSpreadsheetMock);
	}

	@Test
	public void testKonstruktor_nullWorkingSpreadsheet_wirftException() {
		assertThatThrownBy(() -> new TestSheetRunner(null, TurnierSystem.KEIN, "Test", processBoxMock))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	public void testKonstruktor_nullTurnierSystem_wirftException() {
		assertThatThrownBy(() -> new TestSheetRunner(workingSpreadsheetMock, null, "Test", processBoxMock))
				.isInstanceOf(NullPointerException.class);
	}

	// --- Fluent API ---

	@Test
	public void testBackupDocumentAfterRun_gibtThisZurueck() {
		SheetRunner result = testRunner.backupDocumentAfterRun();
		assertThat(result).isSameAs(testRunner);
	}

	// --- Statische Methoden ---

	@Test
	public void testIsRunning_initiallyFalse() {
		assertThat(SheetRunner.isRunning()).isFalse();
	}

	@Test
	public void testCancelRunner_ohneAktivenRunner_keineException() {
		assertThatNoException().isThrownBy(SheetRunner::cancelRunner);
	}

	@Test
	public void testDoCancelTask_ohneAktivenRunner_keineException() {
		assertThatNoException().isThrownBy(SheetRunner::testDoCancelTask);
	}

	// --- run() ---

	@Test
	public void testRun_ruftDoRunAuf() {
		testRunner.run();
		assertThat(testRunner.doRunCalled).isTrue();
	}

	@Test
	public void testRun_isRunning_nachRun_widerFalse() {
		testRunner.run();
		assertThat(SheetRunner.isRunning()).isFalse();
	}

	@Test
	public void testRun_GenerateException_wirdBehandelt_keinePropagation() {
		testRunner.doRunException = new GenerateException("Test-Fehler");
		assertThatNoException().isThrownBy(testRunner::run);
		assertThat(testRunner.generateExceptionBehandelt).isTrue();
	}

	@Test
	public void testRun_GenerateException_isRunning_wirdZurueckgesetzt() {
		testRunner.doRunException = new GenerateException("Fehler im doRun");
		testRunner.run();
		assertThat(SheetRunner.isRunning()).isFalse();
	}

	// --- Innere konkrete Testklasse ---

	static class TestSheetRunner extends SheetRunner {

		private final ProcessBox processBoxMock;
		boolean doRunCalled = false;
		boolean generateExceptionBehandelt = false;
		GenerateException doRunException = null;

		TestSheetRunner(WorkingSpreadsheet ws, TurnierSystem system, String logPrefix, ProcessBox processBoxMock) {
			super(ws, system, logPrefix);
			this.processBoxMock = processBoxMock;
		}

		@Override
		@SuppressWarnings("deprecation")
		protected IKonfigurationSheet getKonfigurationSheet() {
			return null; // TurnierSystem.KEIN -> updateKonfigurationSheet() wird nicht aufgerufen
		}

		@Override
		public Logger getLogger() {
			return logger;
		}

		@Override
		protected void doRun() throws GenerateException {
			doRunCalled = true;
			if (doRunException != null) {
				throw doRunException;
			}
		}

		@Override
		public ProcessBox processBox() {
			return processBoxMock;
		}

		@Override
		public void processBoxinfo(String infoMsg) {
			// kein LibreOffice ProcessBox in Tests
		}

		/** Überschrieben um LibreOffice MessageBox-Aufrufe in Tests zu vermeiden */
		@Override
		protected void handleGenerateException(GenerateException e) {
			generateExceptionBehandelt = true;
		}
	}
}
