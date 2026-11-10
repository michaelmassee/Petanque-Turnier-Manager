/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.RundenWertungHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

/**
 * Schaltet für das aktive Spielrunden-Blatt um, ob die Runde gewertet wird.
 * <p>
 * Eine nicht gewertete Runde bleibt als Blatt mit ihrer Nummer erhalten, zählt aber weder in
 * der Rangliste noch in der Paarungshistorie der nächsten Auslosung (siehe
 * {@link RundenWertungHelper}). Der Tab wird grau gefärbt, die Rangliste neu aufgebaut.
 * <p>
 * Je Turniersystem liefert eine Unterklasse das Schlüssel-Präfix der Spielrunden-Blätter und den
 * Neuaufbau der zugehörigen Rangliste.
 */
public abstract class AbstractSpielrundeWertungUmschalten extends SheetRunner {

	private static final Logger logger = LogManager.getLogger(AbstractSpielrundeWertungUmschalten.class);

	/** Tab-Farbe einer nicht gewerteten Spielrunde (grau). */
	public static final int TAB_FARBE_NICHT_GEWERTET = 0x9e9e9e;

	protected AbstractSpielrundeWertungUmschalten(WorkingSpreadsheet workingSpreadsheet, TurnierSystem turnierSystem) {
		super(workingSpreadsheet, turnierSystem);
	}

	@Override
	protected abstract BaseKonfigurationSheet getKonfigurationSheet();

	/**
	 * @return Präfix der Identitäts-Schlüssel der Spielrunden-Blätter dieses Systems,
	 *         z.B. {@link SheetMetadataHelper#SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX}
	 */
	protected abstract String getRundenSchluesselPraefix();

	/**
	 * Baut die Rangliste neu auf, in die die Spielrunde mit dem Schlüssel {@code rundenSchluessel}
	 * eingeht – synchron, ohne eigenen SheetRunner (kein SheetRunner im SheetRunner).
	 */
	protected abstract void ranglisteNeuAufbauen(String rundenSchluessel) throws GenerateException;

	/**
	 * Filter für Schlüssel, die zwar das Präfix tragen, aber kein Spielrunden-Blatt bezeichnen
	 * (z.B. Supermêlée-Spielrundenplan).
	 */
	protected boolean istRundenSchluessel(String schluessel) {
		return true;
	}

	/**
	 * Prüft systemspezifische Voraussetzungen. Liefert die Begründung, falls nicht umgeschaltet
	 * werden darf.
	 */
	protected Optional<String> sperrGrund() {
		return Optional.empty();
	}

	@Override
	public void doRun() throws GenerateException {
		XSpreadsheetDocument xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		XSpreadsheet rundeSheet = getWorkingSpreadsheet().getWorkingSpreadsheetView().getActiveSheet();
		Optional<String> rundenSchluessel = Optional.ofNullable(rundeSheet)
				.flatMap(sheet -> SheetMetadataHelper.findeSchluesselMitPrefix(xDoc, sheet, getRundenSchluesselPraefix()))
				.filter(this::istRundenSchluessel);
		if (rundenSchluessel.isEmpty()) {
			zeigeHinweis(I18n.get("spielrunde.wertung.kein.spielrundenblatt"));
			return;
		}
		Optional<String> sperrGrund = sperrGrund();
		if (sperrGrund.isPresent()) {
			zeigeHinweis(sperrGrund.get());
			return;
		}

		String schluessel = rundenSchluessel.get();
		String blattName = Lo.qi(XNamed.class, rundeSheet).getName();
		boolean nichtWerten = !RundenWertungHelper.istNichtGewertet(xDoc, rundeSheet, schluessel);
		if (!istBestaetigt(blattName, nichtWerten)) {
			logger.debug("Wertung der Spielrunde '{}' umschalten vom Benutzer abgebrochen", blattName);
			return;
		}

		RundenWertungHelper.setzeNichtGewertet(xDoc, rundeSheet, schluessel, nichtWerten);
		TurnierSheet.from(rundeSheet, getWorkingSpreadsheet())
				.tabColor(nichtWerten ? TAB_FARBE_NICHT_GEWERTET : getKonfigurationSheet().getSpielrundeTabFarbe());
		processBoxinfo(nichtWerten ? "spielrunde.wertung.nicht.gewertet.info" : "spielrunde.wertung.wieder.gewertet.info",
				blattName);
		ranglisteNeuAufbauen(schluessel);
	}

	/**
	 * Baut die Rangliste mit dem Identitäts-Schlüssel {@code ranglisteSchluessel} nur dann neu auf,
	 * wenn sie bereits existiert – das Umschalten soll keine Rangliste erzwingen.
	 */
	protected final void wennVorhanden(String ranglisteSchluessel, RanglisteAufbau aufbau) throws GenerateException {
		if (SheetMetadataHelper.findeSheet(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), ranglisteSchluessel)
				.isPresent()) {
			aufbau.aufbauen();
		}
	}

	/** Neuaufbau einer Rangliste. */
	@FunctionalInterface
	protected interface RanglisteAufbau {
		void aufbauen() throws GenerateException;
	}

	private boolean istBestaetigt(String blattName, boolean nichtWerten) {
		String frage = nichtWerten
				? I18n.get("spielrunde.wertung.frage.nicht.werten", blattName)
				: I18n.get("spielrunde.wertung.frage.wieder.werten", blattName);
		MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
				.caption(I18n.get("spielrunde.wertung.caption"))
				.message(frage)
				.show();
		return result == MessageBoxResult.YES;
	}

	private void zeigeHinweis(String nachricht) {
		MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
				.caption(I18n.get("spielrunde.wertung.caption"))
				.message(nachricht)
				.show();
	}
}
