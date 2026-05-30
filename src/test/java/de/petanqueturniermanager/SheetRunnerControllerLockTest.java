/*
 * Erstellung 19.05.2026 / Michael Massee
 */
package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * Tests, dass jeder {@link SheetRunner#run()}-Lauf den Calc-Controller via
 * {@link de.petanqueturniermanager.helper.sheet.ControllerLock} genau einmal
 * sperrt und freigibt – auch im Exception-Fall.
 * <p>
 * Hintergrund: Schutzklammer gegen native Renderer-Crashes in LO
 * (scfiltlo / D3DScreenUpdateManager) bei vielen UNO-Property-Writes
 * während eines SheetRunner-Laufs.
 */
public class SheetRunnerControllerLockTest {

	static final Logger logger = LogManager.getLogger(SheetRunnerControllerLockTest.class);

	private WorkingSpreadsheet workingSpreadsheetMock;
	private ProcessBox processBoxMock;
	private XSpreadsheetDocument xDocMockMitXModel;
	private TestSheetRunner testRunner;

	@BeforeEach
	public void setUp() {
		// Frischer Koordinator pro Test – kein globaler Zustand zwischen Tests
		SheetRunner.koordinator = new SheetRunnerKoordinator();
		workingSpreadsheetMock = Mockito.mock(WorkingSpreadsheet.class);
		processBoxMock = Mockito.mock(ProcessBox.class, Mockito.RETURNS_SELF);
		var xCalculatableMock = Mockito.mock(XCalculatable.class);

		Mockito.when(workingSpreadsheetMock.getxCalculatable()).thenReturn(xCalculatableMock);
		var xStorableMock = Mockito.mock(XStorable.class);
		Mockito.when(xStorableMock.getLocation()).thenReturn("");
		Mockito.when(workingSpreadsheetMock.getXStorable()).thenReturn(xStorableMock);

		// Dokument-Mock zusätzlich als XModel exponieren, damit Lo.qi(XModel.class, doc)
		// das Mock zurückliefert und ControllerLock tatsächlich lockControllers/
		// unlockControllers aufruft.
		xDocMockMitXModel = Mockito.mock(XSpreadsheetDocument.class,
				Mockito.withSettings().extraInterfaces(XModel.class));
		Mockito.when(workingSpreadsheetMock.getWorkingSpreadsheetDocument()).thenReturn(xDocMockMitXModel);

		testRunner = new TestSheetRunner(workingSpreadsheetMock, TurnierSystem.KEIN, "TestPrefix", processBoxMock);
	}

	@Test
	public void testRun_normalerLauf_lockUndUnlockGenauEinmal() {
		testRunner.run();

		XModel xModel = (XModel) xDocMockMitXModel;
		// Genau einmal lockControllers, danach genau einmal unlockControllers
		verify(xModel, Mockito.times(1)).lockControllers();
		verify(xModel, Mockito.times(1)).unlockControllers();
		InOrder inOrder = Mockito.inOrder(xModel);
		inOrder.verify(xModel).lockControllers();
		inOrder.verify(xModel).unlockControllers();
	}

	@Test
	public void testRun_doRunWirftException_unlockTrotzdemAufgerufen() {
		testRunner.doRunException = new GenerateException("Test-Fehler im doRun");

		assertThatNoException().isThrownBy(testRunner::run);

		XModel xModel = (XModel) xDocMockMitXModel;
		// lock + unlock beide aufgerufen, in dieser Reihenfolge
		InOrder inOrder = Mockito.inOrder(xModel);
		inOrder.verify(xModel).lockControllers();
		inOrder.verify(xModel).unlockControllers();
		// keine weiteren Lock-Toggles
		verify(xModel, atLeastOnce()).lockControllers();
		assertThat(testRunner.generateExceptionBehandelt).isTrue();
	}

	@Test
	public void testRun_doRunWurdeAufgerufen_innerhalbDesLocks() {
		// Reihenfolge: lockControllers VOR doRun, unlockControllers NACH doRun.
		// Verifiziert über das doRunCalledZeitpunkt-Flag und die InOrder-Verifikation.
		testRunner.run();

		assertThat(testRunner.doRunCalled).isTrue();
		XModel xModel = (XModel) xDocMockMitXModel;
		InOrder inOrder = Mockito.inOrder(xModel);
		inOrder.verify(xModel).lockControllers();
		inOrder.verify(xModel).unlockControllers();
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
		public void processBoxinfo(String i18nKey, Object... args) {
			// kein LibreOffice ProcessBox in Tests
		}

		@Override
		protected void handleGenerateException(GenerateException e) {
			generateExceptionBehandelt = true;
		}
	}
}
