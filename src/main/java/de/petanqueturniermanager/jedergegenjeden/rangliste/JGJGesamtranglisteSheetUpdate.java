package de.petanqueturniermanager.jedergegenjeden.rangliste;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Aktualisiert die JGJ-Gesamtrangliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link JGJGesamtranglisteSheet} (vollständiger Neuaufbau mit
 * {@code NewSheet.forceCreate()}) schreibt diese Klasse nur den Datenbereich neu.
 * Header, Spaltenbreiten und Metadaten bleiben unverändert. Wird vom
 * SheetSync-Listener beim Tab-Wechsel ausgelöst.
 * <p>
 * Fallback: Wenn das Sheet noch nicht existiert, wird automatisch der
 * vollständige Erstaufbau über {@link JGJGesamtranglisteSheet#upDateSheet()}
 * ausgelöst. Bei nur einer Gruppe ist die Gesamtrangliste nicht sinnvoll –
 * das Update bricht dann still ab.
 */
public class JGJGesamtranglisteSheetUpdate extends JGJGesamtranglisteSheet {

	private static final Logger logger = LogManager.getLogger(JGJGesamtranglisteSheetUpdate.class);

	public JGJGesamtranglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public void doRun() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			logger.debug("GesamtranglisteUpdate: Sheet nicht vorhanden – vollständiger Erstaufbau");
			upDateSheet();
			return;
		}

		logger.debug("GesamtranglisteUpdate START – Thread='{}'", Thread.currentThread().getName());
		processBoxinfo("processbox.rangliste.aktualisieren");

		var meldeListe = new JGJMeldeListeSheet_Update(getWorkingSpreadsheet());
		TeamMeldungen aktiveMeldungen = meldeListe.getAktiveMeldungen();
		if (aktiveMeldungen == null || aktiveMeldungen.size() == 0) {
			processBoxinfo("processbox.abbruch");
			return;
		}

		List<TeamMeldungen> gruppen = ermittleGruppen(aktiveMeldungen);
		if (gruppen.size() < 2) {
			logger.debug("GesamtranglisteUpdate: nur eine Gruppe – kein Update");
			processBoxinfo("processbox.abbruch");
			return;
		}

		insertHeader(sheet);
		berechnungUndSchreiben(sheet, meldeListe, aktiveMeldungen, gruppen);

		logger.debug("GesamtranglisteUpdate ENDE – Thread='{}'", Thread.currentThread().getName());
	}
}
