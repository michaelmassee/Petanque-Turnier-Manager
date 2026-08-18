/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncRebuild;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJGesamtranglisteSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetUpdate;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeGruppenRanglisteSheetUpdate;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheetUpdate;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheetUpdate;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheetUpdate;

/**
 * Baut das Rangliste-Sheet des jeweils aktiven Turniersystems bei Bedarf neu auf, bevor
 * WhatsApp es als Bild versendet – analog zu den {@code aktualisiereExportSheetWennDirty}-Aufrufen
 * in den {@code *ExportInVerzeichnis}-Klassen, damit WhatsApp nicht dieselbe Rangliste
 * fehlend oder veraltet ausliefert wie der Verzeichnis-Export.
 * <p>
 * K.-O. kennt kein aggregiertes Rangliste-Sheet (nur Turnierbaum) und wird daher ausgelassen.
 */
final class WhatsAppRanglisteAktualisierer {

	private static final Logger logger = LogManager.getLogger(WhatsAppRanglisteAktualisierer.class);

	private WhatsAppRanglisteAktualisierer() {
	}

	static void aktualisiereWennNoetig(WorkingSpreadsheet ws, TurnierSystem ts) throws GenerateException {
		var doc = ws.getWorkingSpreadsheetDocument();
		switch (ts) {
		case SCHWEIZER -> SheetSyncRebuild.aktualisiereWennDirty(doc, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerSchweizer),
				() -> new SchweizerRanglisteSheetUpdate(ws).doRun());
		case FORMULEX -> SheetSyncRebuild.aktualisiereWennDirty(doc, SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerFormuleX),
				() -> new FormuleXRanglisteSheetUpdate(ws).doRun());
		case KASKADE -> SheetSyncRebuild.aktualisiereWennDirty(doc, SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerKaskade),
				() -> new KaskadeGruppenRanglisteSheetUpdate(ws).doRun());
		case POULE -> SheetSyncRebuild.aktualisiereWennDirty(doc, SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerPoule),
				() -> new PouleVorrundenRanglisteSheetUpdate(ws).doRun());
		case MAASTRICHTER -> SheetSyncRebuild.aktualisiereWennDirty(doc, SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
				new EingabeSignatur(SignaturQuellen::fuerMaastrichter),
				() -> new MaastrichterVorrundenRanglisteSheetUpdate(ws).doRun());
		case TRIPTETE -> aktualisiereTripTete(ws, doc);
		case JGJ -> aktualisiereJgj(ws, doc);
		case LIGA -> aktualisiereLiga(ws, doc);
		case SUPERMELEE -> aktualisiereSupermelee(ws, doc);
		case KO, KEIN -> logger.debug("Kein Rangliste-Rebuild fuer Turniersystem {}", ts);
		}
	}

	private static void aktualisiereTripTete(WorkingSpreadsheet ws, XSpreadsheetDocument doc)
			throws GenerateException {
		boolean ranglisteFehlt = SheetSyncRebuild.sheetFehlt(doc, SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE);
		SheetSyncRebuild.aktualisiereWennDirty(doc, SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerTripTete), ranglisteFehlt,
				() -> {
					if (ranglisteFehlt) {
						new TripTeteRanglisteSheet(ws).upDateSheet();
					} else {
						new TripTeteRanglisteSheetUpdate(ws).doRun();
					}
				});
	}

	private static void aktualisiereJgj(WorkingSpreadsheet ws, XSpreadsheetDocument doc)
			throws GenerateException {
		boolean abhaengigeAusgabeFehlt = SheetSyncRebuild.sheetFehlt(doc, SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE)
				|| SheetSyncRebuild.sheetFehlt(doc, SheetMetadataHelper.SCHLUESSEL_JGJ_DIREKTVERGLEICH);
		SheetSyncRebuild.aktualisiereWennDirty(doc, SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerJGJ), abhaengigeAusgabeFehlt,
				() -> {
					new JGJRanglisteSheetUpdate(ws).doRun();
					new JGJGesamtranglisteSheetUpdate(ws).doRun();
					new JGJRanglisteDirektvergleichSheet(ws).aktualisieren();
				});
	}

	private static void aktualisiereLiga(WorkingSpreadsheet ws, XSpreadsheetDocument doc)
			throws GenerateException {
		boolean abhaengigeAusgabeFehlt = SheetSyncRebuild.sheetFehlt(doc, SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE)
				|| SheetSyncRebuild.sheetFehlt(doc, SheetMetadataHelper.SCHLUESSEL_LIGA_DIREKTVERGLEICH);
		SheetSyncRebuild.aktualisiereWennDirty(doc, SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerLiga), abhaengigeAusgabeFehlt,
				() -> {
					new LigaRanglisteSheetUpdate(ws).doRun();
					new LigaRanglisteDirektvergleichSheet(ws).aktualisieren();
				});
	}

	private static void aktualisiereSupermelee(WorkingSpreadsheet ws, XSpreadsheetDocument doc)
			throws GenerateException {
		int spieltagNr = new SuperMeleeKonfigurationSheet(ws).getAktiveSpieltag().getNr();
		if (spieltagNr <= 0) {
			logger.debug("Supermelee: kein aktiver Spieltag, Rangliste-Rebuild uebersprungen");
			return;
		}
		String persistenzSchluessel = "SUPERMELEE_SPIELTAG_" + spieltagNr;
		String sheetSchluessel = SheetMetadataHelper.schluesselSpieltagRangliste(spieltagNr);
		SheetSyncRebuild.aktualisiereWennDirty(doc, persistenzSchluessel,
				new EingabeSignatur(xDoc -> SignaturQuellen.fuerSupermeleeSpieltag(xDoc, spieltagNr)),
				SheetSyncRebuild.sheetFehlt(doc, sheetSchluessel),
				() -> new SpieltagRanglisteSheetUpdate(ws, SpielTagNr.from(spieltagNr)).doRun());
	}
}
