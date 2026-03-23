package de.petanqueturniermanager.helper.rangliste;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XNamed;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.XComponentContext;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.Lo;

/**
 * Lauscht auf Sheet-Tab-Wechsel und OS-Fokusereignisse, um eine berechnete Rangliste
 * automatisch neu aufzubauen wenn sie zur Anzeige aktiviert wird.
 * <p>
 * Die Rangliste wird bei jedem Fokus-Erhalt komplett neu aufgebaut – ohne Dirty-Flag-Prüfung.
 * Konkurrierende Rebuilds werden vom {@code SheetRunnerKoordinator} verhindert.
 * <p>
 * Rebuild wird ausgelöst wenn:
 * <ul>
 *   <li>Der Benutzer zum Rangliste-Tab wechselt (XSelectionChangeListener)</li>
 *   <li>Das LibreOffice-Fenster den OS-Fokus erhält und die Rangliste aktiv ist (OnFocus)</li>
 * </ul>
 */
public class RanglisteRefreshListener implements IGlobalEventListener {

	private static final Logger logger = LogManager.getLogger(RanglisteRefreshListener.class);

	private final XComponentContext xContext;
	private final String ranglisteSheetName;
	private final Function<WorkingSpreadsheet, SheetRunner> runnerFactory;

	/** Bereits registrierte Dokumente – verhindert Doppelregistrierung der Listener. */
	private final Set<XSpreadsheetDocument> registriert =
			Collections.newSetFromMap(new WeakHashMap<>());

	/**
	 * @param xContext           UNO-Komponentenkontext
	 * @param ranglisteSheetName Name des Ranglisten-Sheets (z.B. "Rangliste", "Vorrunden-Rangliste")
	 * @param runnerFactory      Erzeugt den zuständigen SheetRunner für den Rebuild
	 */
	public RanglisteRefreshListener(XComponentContext xContext, String ranglisteSheetName,
			Function<WorkingSpreadsheet, SheetRunner> runnerFactory) {
		this.xContext = xContext;
		this.ranglisteSheetName = ranglisteSheetName;
		this.runnerFactory = runnerFactory;
	}

	// ── Dokument-Laden: SelectionChange-Listener registrieren ───────────────

	@Override
	public void onLoadFinished(Object source) {
		registriereListener(source);
	}

	@Override
	public void onNew(Object source) {
		registriereListener(source);
	}

	@Override
	public void onViewCreated(Object source) {
		registriereListener(source);
	}

	private void registriereListener(Object source) {
		try {
			XModel xModel = Lo.qi(XModel.class, source);
			if (xModel == null) return;
			XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
			if (xDoc == null) return;

			synchronized (registriert) {
				if (registriert.contains(xDoc)) return;
				registriert.add(xDoc);
			}

			registriereSelectionChangeListener(xModel, xDoc);

		} catch (Throwable t) {
			logger.error("Fehler beim Registrieren der Listener", t);
		}
	}

	/**
	 * Registriert einen XSelectionChangeListener der bei jedem Tab-Wechsel zur Rangliste
	 * einen vollständigen Neuaufbau auslöst.
	 * <p>
	 * Der Listener feuert bei jeder Selektion-Änderung (Zell-Klick, Pfeiltaste, Tab-Wechsel).
	 * Rebuild wird nur ausgelöst wenn das aktive Sheet von einem anderen Sheet zur Rangliste
	 * wechselt (nicht bei internen Klicks innerhalb der Rangliste).
	 */
	private void registriereSelectionChangeListener(XModel xModel, XSpreadsheetDocument xDoc) {
		XSelectionSupplier selSupplier = Lo.qi(XSelectionSupplier.class, xModel.getCurrentController());
		if (selSupplier == null) return;

		selSupplier.addSelectionChangeListener(new XSelectionChangeListener() {
			private String letztesSheet = null;

			@Override
			public void selectionChanged(EventObject e) {
				try {
					XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());
					if (view == null) return;
					XNamed named = Lo.qi(XNamed.class, view.getActiveSheet());
					if (named == null) return;

					String aktuellesSheet = named.getName();
					String vorherigesSheet = letztesSheet;
					letztesSheet = aktuellesSheet;

					// Kein Sheet-Wechsel (Klick innerhalb desselben Sheets) → nichts zu tun
					if (aktuellesSheet.equals(vorherigesSheet)) return;

					// Rebuild nur wenn User ZUR Rangliste gewechselt hat (nicht von ihr weg)
					boolean istAufRangliste = ranglisteSheetName.equals(aktuellesSheet);
					boolean warAufRangliste = ranglisteSheetName.equals(vorherigesSheet);

					if (istAufRangliste && !warAufRangliste && !SheetRunner.isRunning()) {
						logger.debug("Tab-Wechsel zur Rangliste '{}' → automatischer Neuaufbau", ranglisteSheetName);
						runnerFactory.apply(new WorkingSpreadsheet(xContext, xDoc)).run();
					}
				} catch (Throwable t) {
					logger.error("Fehler im SelectionChangeListener", t);
				}
			}

			@Override
			public void disposing(EventObject e) {
			}
		});

		logger.debug("XSelectionChangeListener für Rangliste '{}' registriert", ranglisteSheetName);
	}

	// ── OnFocus: Fallback-Rebuild für Alt-Tab zurück zum LO-Fenster ──────────

	@Override
	public void onFocus(Object source) {
		try {
			XModel xModel = Lo.qi(XModel.class, source);
			if (xModel == null) return;

			XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
			if (xDoc == null) return;

			XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());
			if (view == null) return;

			XNamed named = Lo.qi(XNamed.class, view.getActiveSheet());
			if (named == null) return;

			if (!ranglisteSheetName.equals(named.getName())) return;
			if (SheetRunner.isRunning()) return;

			logger.debug("OnFocus mit Rangliste '{}' aktiv → automatischer Neuaufbau", ranglisteSheetName);
			runnerFactory.apply(new WorkingSpreadsheet(xContext, xDoc)).run();

		} catch (Throwable t) {
			logger.error("Fehler beim OnFocus-Ranglisten-Refresh", t);
		}
	}
}
