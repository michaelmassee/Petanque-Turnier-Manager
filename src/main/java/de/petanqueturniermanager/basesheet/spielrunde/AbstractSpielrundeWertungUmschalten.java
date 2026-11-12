/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XNamed;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XText;

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
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.NichtGewertetFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RundenWertungHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

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

	/** Titelzelle (verbunden) – in allen Spielrunden-Layouts B1. */
	static final int TITEL_SPALTE = 1;
	static final int TITEL_ZEILE = 0;

	/** Erste Zeile/Spalte der Tabelle inkl. Kopf. */
	private static final int ERSTE_TABELLEN_ZEILE = 0;
	private static final int ERSTE_TABELLEN_SPALTE = 0;

	/** Erste Datenzeile – in allen Spielrunden-Layouts nach zwei Kopfzeilen. */
	private static final int ERSTE_DATEN_ZEILE = 2;

	/** Obergrenze der Suche nach dem Tabellenende. */
	private static final int MAX_DATEN_ZEILEN = 1000;

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
	 * @return letzte Spalte der sichtbaren Spielrunden-Tabelle (Ergebnis Team B bzw. Startzeit)
	 */
	protected abstract int letzteTabellenSpalte() throws GenerateException;

	/**
	 * Spalte, die in jeder Datenzeile belegt ist. Die erste leere Zelle beendet die Tabelle; der
	 * Footer folgt erst nach einer Leerzeile. Standard: Team A (Spalte B).
	 */
	protected int belegteDatenSpalte() {
		return 1;
	}

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
		kennzeichnungAktualisieren(xDoc, rundeSheet, nichtWerten);
		processBoxinfo(nichtWerten ? "spielrunde.wertung.nicht.gewertet.info" : "spielrunde.wertung.wieder.gewertet.info",
				blattName);
		ranglisteNeuAufbauen(schluessel);
	}

	/**
	 * Kennzeichnet die Spielrunde im Blatt: bedingte Formatierung über die Tabelle und Zusatz im
	 * Titel – bzw. entfernt beides wieder.
	 */
	private void kennzeichnungAktualisieren(XSpreadsheetDocument xDoc, XSpreadsheet rundeSheet, boolean nichtWerten)
			throws GenerateException {
		RangePosition tabelle = tabellenBereich(xDoc, rundeSheet);
		String zusatz = I18n.get("spielrunde.wertung.titel.zusatz");
		XText titelZelle = titelZelle(rundeSheet);
		String titel = titelZelle.getString();
		BlattschutzManager.get().ensureUnprotectedInScope();
		if (nichtWerten) {
			NichtGewertetFormatHelper.anwenden(xDoc, rundeSheet, tabelle);
			schreibeTitel(titelZelle, titel, SpielrundeTitelZusatz.mitZusatz(titel, zusatz));
		} else {
			NichtGewertetFormatHelper.entfernen(xDoc, rundeSheet, tabelle);
			schreibeTitel(titelZelle, titel, SpielrundeTitelZusatz.ohneZusatz(titel, zusatz));
		}
	}

	private static void schreibeTitel(XText titelZelle, String bisher, String neu) {
		if (!neu.equals(bisher)) {
			titelZelle.setString(neu);
		}
	}

	private static XText titelZelle(XSpreadsheet rundeSheet) throws GenerateException {
		try {
			return Lo.qi(XText.class, rundeSheet.getCellByPosition(TITEL_SPALTE, TITEL_ZEILE));
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
			throw new GenerateException("Titelzelle der Spielrunde nicht gefunden: " + e.getMessage());
		}
	}

	/**
	 * Tabellenbereich der Spielrunde inkl. Kopfzeilen, bis zur letzten belegten Datenzeile.
	 */
	private RangePosition tabellenBereich(XSpreadsheetDocument xDoc, XSpreadsheet rundeSheet)
			throws GenerateException {
		int spalte = belegteDatenSpalte();
		RangeData daten = RangeHelper.from(rundeSheet, xDoc,
				RangePosition.from(spalte, ERSTE_DATEN_ZEILE, spalte, ERSTE_DATEN_ZEILE + MAX_DATEN_ZEILEN - 1))
				.getDataFromRange();
		int anzahlDatenZeilen = IntStream.range(0, daten.size())
				.filter(idx -> StringUtils.isEmpty(daten.get(idx).get(0).getStringVal()))
				.findFirst()
				.orElse(daten.size());
		int letzteZeile = ERSTE_DATEN_ZEILE + anzahlDatenZeilen - 1;
		return RangePosition.from(ERSTE_TABELLEN_SPALTE, ERSTE_TABELLEN_ZEILE, letzteTabellenSpalte(), letzteZeile);
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
