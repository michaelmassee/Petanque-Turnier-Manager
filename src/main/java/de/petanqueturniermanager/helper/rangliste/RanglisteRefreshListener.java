package de.petanqueturniermanager.helper.rangliste;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.XComponentContext;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Lauscht auf Sheet-Tab-Wechsel und OS-Fokusereignisse, um eine berechnete Rangliste
 * automatisch neu aufzubauen wenn sie zur Anzeige aktiviert wird.
 * <p>
 * Die Rangliste wird bei jedem Fokus-Erhalt komplett neu aufgebaut – ohne Dirty-Flag-Prüfung.
 * Konkurrierende Rebuilds werden vom {@code SheetRunnerKoordinator} verhindert.
 * <p>
 * Die Sheet-Identifikation erfolgt über Named Ranges ({@link SheetMetadataHelper}), nicht
 * über den Sheet-Namen. Dadurch bleibt der Refresh stabil auch wenn der Benutzer einen
 * Sheet-Tab umbenennt oder mehrere Sheets denselben Namen haben.
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
	private final BiPredicate<XSpreadsheetDocument, XSpreadsheet> ranglisteMatch;
	private final TurnierSystem erwartesTurnierSystem;
	private final BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory;

	/** Bereits registrierte Dokumente – verhindert Doppelregistrierung der Listener. */
	private final Set<XSpreadsheetDocument> registriert =
			Collections.newSetFromMap(new WeakHashMap<>());

	// ── Factory-Methoden ────────────────────────────────────────────────────

	/**
	 * Erzeugt einen Listener für Sheets mit festem Named-Range-Schlüssel.
	 * Nutzt intern {@link SheetMetadataHelper#istRegistriertesSheet}.
	 *
	 * @param namedRangeKey         Schlüssel des benannten Bereichs, z.B.
	 *                              {@link SheetMetadataHelper#SCHLUESSEL_SCHWEIZER_RANGLISTE}
	 */
	public static RanglisteRefreshListener fuerSchluessel(
			XComponentContext xContext,
			String namedRangeKey,
			TurnierSystem erwartesTurnierSystem,
			BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
		return new RanglisteRefreshListener(xContext,
				(xDoc, sheet) -> SheetMetadataHelper.istRegistriertesSheet(xDoc, sheet, namedRangeKey),
				erwartesTurnierSystem, runnerFactory);
	}

	/**
	 * Erzeugt einen Listener für Supermelee-Spieltag-Ranglisten (dynamische Schlüssel).
	 * Nutzt intern {@link SheetMetadataHelper#findeSpieltagNr}.
	 */
	public static RanglisteRefreshListener fuerSpieltagRangliste(
			XComponentContext xContext,
			TurnierSystem erwartesTurnierSystem,
			BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
		return new RanglisteRefreshListener(xContext,
				(xDoc, sheet) -> SheetMetadataHelper.findeSpieltagNr(xDoc, sheet).isPresent(),
				erwartesTurnierSystem, runnerFactory);
	}

	// ── Konstruktor ──────────────────────────────────────────────────────────

	/**
	 * @param xContext              UNO-Komponentenkontext
	 * @param ranglisteMatch        Prüft ob ein Sheet die gesuchte Rangliste ist.
	 *                              Bekommt Dokument + aktives Sheet übergeben.
	 * @param erwartesTurnierSystem Nur Dokumente dieses Turniersystems werden berücksichtigt
	 * @param runnerFactory         Erzeugt den zuständigen SheetRunner; erhält WorkingSpreadsheet
	 *                              und das aktive XSpreadsheet (für Spieltag-Nr o.ä.)
	 */
	RanglisteRefreshListener(XComponentContext xContext,
			BiPredicate<XSpreadsheetDocument, XSpreadsheet> ranglisteMatch,
			TurnierSystem erwartesTurnierSystem,
			BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
		this.xContext = xContext;
		this.ranglisteMatch = ranglisteMatch;
		this.erwartesTurnierSystem = erwartesTurnierSystem;
		this.runnerFactory = runnerFactory;
	}

	/** Prüft ob das Dokument das erwartete Turniersystem hat. */
	private boolean istPassendesDokument(XSpreadsheetDocument xDoc) {
		return new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument() == erwartesTurnierSystem;
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
	 * Rebuild wird nur ausgelöst wenn das aktive Sheet von einem anderen Sheet zur Rangliste
	 * wechselt (nicht bei internen Klicks innerhalb der Rangliste).
	 */
	private void registriereSelectionChangeListener(XModel xModel, XSpreadsheetDocument xDoc) {
		XSelectionSupplier selSupplier = Lo.qi(XSelectionSupplier.class, xModel.getCurrentController());
		if (selSupplier == null) return;

		selSupplier.addSelectionChangeListener(new XSelectionChangeListener() {
			private XSpreadsheet letztesSheet = null;

			@Override
			public void selectionChanged(EventObject e) {
				try {
					XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());
					if (view == null) return;

					XSpreadsheet aktuellesSheet = view.getActiveSheet();
					if (aktuellesSheet == null) return;

					XSpreadsheet vorherigesSheet = letztesSheet;
					letztesSheet = aktuellesSheet;

					// Kein Sheet-Wechsel (Klick innerhalb desselben Sheets) → nichts zu tun
					if (aktuellesSheet == vorherigesSheet) return;

					// Rebuild nur wenn User ZUR Rangliste gewechselt hat (nicht von ihr weg)
					boolean istAufRangliste = ranglisteMatch.test(xDoc, aktuellesSheet);
					boolean warAufRangliste = vorherigesSheet != null && ranglisteMatch.test(xDoc, vorherigesSheet);

					logger.trace("selectionChanged: istAufRangliste={}, warAufRangliste={}, isRunning={}, Thread='{}'",
							istAufRangliste, warAufRangliste, SheetRunner.isRunning(),
							Thread.currentThread().getName());

					if (istAufRangliste && !warAufRangliste && !SheetRunner.isRunning()
							&& istPassendesDokument(xDoc)) {
						// Flag erst hier konsumieren (wenn isRunning()==false), damit ein
						// synchrones Ereignis das Flag nicht vorzeitig verbraucht und das
						// asynchrone Ereignis danach unkontrolliert durchrutscht.
						if (SheetRunner.consumeSelectionChangeSuppression()) {
							logger.trace("selectionChanged: Unterdrückt – ausgelöst durch setActiveSheet() des Runners");
							return;
						}
						logger.warn("selectionChanged: REBUILD getriggert – Thread='{}', isRunning={}",
								Thread.currentThread().getName(), SheetRunner.isRunning());
						runnerFactory.apply(new WorkingSpreadsheet(xContext, xDoc), aktuellesSheet).run();
					}
				} catch (Throwable t) {
					logger.error("Fehler im SelectionChangeListener", t);
				}
			}

			@Override
			public void disposing(EventObject e) {
			}
		});

		logger.debug("XSelectionChangeListener für Rangliste registriert");
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

			XSpreadsheet aktuellesSheet = view.getActiveSheet();
			if (aktuellesSheet == null) return;

			if (!ranglisteMatch.test(xDoc, aktuellesSheet)) return;
			if (SheetRunner.isRunning()) return;
			if (!istPassendesDokument(xDoc)) return;
			if (SheetRunner.consumeSelectionChangeSuppression()) {
				logger.debug("onFocus: Unterdrückt – ausgelöst durch setActiveSheet() des Runners");
				return;
			}

			logger.debug("OnFocus mit Rangliste aktiv → automatischer Neuaufbau");
			runnerFactory.apply(new WorkingSpreadsheet(xContext, xDoc), aktuellesSheet).run();

		} catch (Throwable t) {
			logger.error("Fehler beim OnFocus-Ranglisten-Refresh", t);
		}
	}
}
