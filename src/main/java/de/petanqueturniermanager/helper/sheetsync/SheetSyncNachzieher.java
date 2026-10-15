/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheetsync;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XIndexAccess;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.RueckgaengigSperre;

/**
 * Zentrale Registry aller {@link SheetSyncListener} und Einstieg, um abgeleitete Blätter (Ranglisten, Teilnehmer-
 * und Checkin-Listen) <b>ohne Tab-Wechsel</b> auf den Stand der Eingaben zu bringen.
 * <p>
 * Jeder Leser abgeleiteter Blätter – Webserver, PTM-Online-Live, Verzeichnis-Export, WhatsApp – ruft vor dem Lesen
 * {@link #zieheNach}. Neu aufgebaut wird nur, was laut Signatur veraltet ist, und nur der Datenbereich über die
 * {@code *SheetUpdate}-Klasse: kein Fokuswechsel, kein Eintrag in der Rückgängig-Liste.
 */
public final class SheetSyncNachzieher {

	private static final Logger logger = LogManager.getLogger(SheetSyncNachzieher.class);
	private static final SheetSyncNachzieher INSTANZ = new SheetSyncNachzieher();

	private final List<SheetSyncListener> eintraege = new CopyOnWriteArrayList<>();

	private SheetSyncNachzieher() {
	}

	/** Eigene, leere Registry – für Tests, die nicht von der Plugin-Initialisierung abhängen sollen. */
	static SheetSyncNachzieher eigeneRegistry() {
		return new SheetSyncNachzieher();
	}

	public static SheetSyncNachzieher get() {
		return INSTANZ;
	}

	public void registriere(SheetSyncListener listener) {
		eintraege.add(checkNotNull(listener));
	}

	/**
	 * Prüft ohne zu schreiben, ob ein vorhandenes Sync-Blatt des Dokuments veraltet ist. Nur Lesezugriffe, darf
	 * daher vor dem Start eines SheetRunners laufen (z.&nbsp;B. auf dem Webserver-Debounce-Thread).
	 */
	public boolean istVeraltet(XSpreadsheetDocument xDoc) {
		List<SheetSyncListener> passende = passende(xDoc);
		if (passende.isEmpty()) {
			return false;
		}
		List<XSpreadsheet> blaetter = alleBlaetter(xDoc);
		for (SheetSyncListener listener : passende) {
			if (listener.istVeraltet(xDoc, blaetter)) {
				return true;
			}
		}
		return false;
	}

	/** Wie {@link #zieheNach(WorkingSpreadsheet, NachziehModus, Runnable)}, ohne Vorab-Aktion. */
	public boolean zieheNach(WorkingSpreadsheet ws, NachziehModus modus) throws GenerateException {
		return zieheNach(ws, modus, () -> {
		});
	}

	/**
	 * Zieht alle veralteten Sync-Blätter des Dokuments nach. <b>Muss im laufenden SheetRunner laufen</b> (die
	 * {@code *SheetUpdate}-Arbeit wird direkt ausgeführt, kein verschachtelter Runner).
	 *
	 * @param vorAenderung läuft einmal vor jedem Neuaufbau, z.&nbsp;B. um einen Blattschutz-Scope bedarfsabhängig
	 *                     zu öffnen
	 * @return {@code true}, wenn mindestens ein Blatt neu aufgebaut wurde
	 */
	public boolean zieheNach(WorkingSpreadsheet ws, NachziehModus modus, Runnable vorAenderung)
			throws GenerateException {
		XSpreadsheetDocument xDoc = ws.getWorkingSpreadsheetDocument();
		List<SheetSyncListener> passende = passende(xDoc);
		if (passende.isEmpty()) {
			return false;
		}
		List<XSpreadsheet> blaetter = alleBlaetter(xDoc);
		return RueckgaengigSperre.ohneRueckgaengig(xDoc, () -> {
			boolean aktualisiert = false;
			for (SheetSyncListener listener : passende) {
				aktualisiert |= listener.zieheNach(ws, blaetter, modus, vorAenderung);
			}
			return aktualisiert;
		});
	}

	private List<SheetSyncListener> passende(XSpreadsheetDocument xDoc) {
		TurnierSystem turnierSystem = new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument();
		if (turnierSystem == TurnierSystem.KEIN) {
			return List.of();
		}
		return eintraege.stream().filter(l -> l.getTurnierSystem() == turnierSystem).toList();
	}

	private static List<XSpreadsheet> alleBlaetter(XSpreadsheetDocument xDoc) {
		XIndexAccess sheets = Lo.qi(XIndexAccess.class, xDoc.getSheets());
		List<XSpreadsheet> blaetter = new ArrayList<>();
		if (sheets == null) {
			return blaetter;
		}
		for (int i = 0; i < sheets.getCount(); i++) {
			try {
				XSpreadsheet sheet = Lo.qi(XSpreadsheet.class, sheets.getByIndex(i));
				if (sheet != null) {
					blaetter.add(sheet);
				}
			} catch (IndexOutOfBoundsException | WrappedTargetException e) {
				// Blatt wurde zwischen getCount() und getByIndex() entfernt – beim nächsten Durchlauf erfasst.
				logger.debug("Blatt {} beim Nachziehen nicht lesbar", i, e);
			}
		}
		return blaetter;
	}
}
