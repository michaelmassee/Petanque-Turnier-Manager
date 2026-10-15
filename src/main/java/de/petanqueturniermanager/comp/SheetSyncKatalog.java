/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXCheckinListeSheetUpdate;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXTeilnehmerSheetUpdate;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncAusgabe;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncListener;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJCheckinListeSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJTeilnehmerSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJGesamtranglisteSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetUpdate;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeCheckinListeSheetUpdate;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeTeilnehmerSheetUpdate;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeGruppenRanglisteSheetUpdate;
import de.petanqueturniermanager.ko.meldeliste.KoCheckinListeSheetUpdate;
import de.petanqueturniermanager.ko.meldeliste.KoTeilnehmerSheetUpdate;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetUpdate;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheetUpdate;
import de.petanqueturniermanager.liga.spielplan.LigaTermineProTeilnehmerSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterCheckinListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterTeilnehmerSheetUpdate;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.poule.meldeliste.PouleCheckinListeSheetUpdate;
import de.petanqueturniermanager.poule.meldeliste.PouleTeilnehmerSheetUpdate;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerCheckinListeSheetUpdate;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerTeilnehmerSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.SupermeleeTeilnehmerSheetUpdate;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheetUpdate;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteCheckinListeSheetUpdate;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteTeilnehmerSheetUpdate;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheetUpdate;

/**
 * Alle Sheet-Synchronisationen (abgeleitete Blätter je Turniersystem) an einer Stelle. Jeder Eintrag wird beim
 * {@code SheetAktivierungsDispatcher} (Tab-Wechsel) und beim {@code SheetSyncNachzieher} (Webserver,
 * PTM-Online-Live, Export, WhatsApp) angemeldet.
 * <p>
 * Regeln siehe {@code turniersysteme/RANGLISTE_LISTENER.md}: ausschließlich {@code *SheetUpdate}-Klassen, nie
 * Vollaufbau-Klassen. Blatt anlegende Aktionen (Direktvergleich, Erstaufbau) nur als Export-Ausgabe.
 */
final class SheetSyncKatalog {

	/**
	 * @param mitPropertyTrigger zusätzlich auf {@code PropertiesChanged} reagieren: nötig für Listen, deren
	 *                           Signatur einen Konfig-Zusatzkontext enthält (Sortiermodus der Teilnehmer-/Checkin-
	 *                           Liste). Ein Wechsel im Konfig-Dialog löst sonst keinen Tab-Wechsel aus.
	 */
	record Eintrag(SheetSyncListener listener, boolean mitPropertyTrigger) {
	}

	private final XComponentContext context;
	private final List<Eintrag> eintraege = new ArrayList<>();

	private SheetSyncKatalog(XComponentContext context) {
		this.context = context;
	}

	static List<Eintrag> erstelle(XComponentContext context) {
		SheetSyncKatalog katalog = new SheetSyncKatalog(context);
		katalog.ranglisten();
		katalog.supermelee();
		katalog.teilnehmerlisten();
		katalog.checkinListen();
		return List.copyOf(katalog.eintraege);
	}

	private void ranglisten() {
		ohnePropertyTrigger(mitErstaufbau(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE, TurnierSystem.SCHWEIZER,
				new EingabeSignatur(SignaturQuellen::fuerSchweizer),
				(ws, ignored) -> new SchweizerRanglisteSheetUpdate(ws)),
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerSchweizer),
				ws -> () -> new SchweizerRanglisteSheetUpdate(ws).doRun()));
		ohnePropertyTrigger(mitErstaufbau(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDEN_RANGLISTE, TurnierSystem.MAASTRICHTER,
				new EingabeSignatur(SignaturQuellen::fuerMaastrichter),
				(ws, ignored) -> new MaastrichterVorrundenRanglisteSheetUpdate(ws)),
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDEN_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerMaastrichter),
				ws -> () -> new MaastrichterVorrundenRanglisteSheetUpdate(ws).doRun()));
		ohnePropertyTrigger(mitErstaufbau(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE, TurnierSystem.POULE,
				new EingabeSignatur(SignaturQuellen::fuerPoule),
				(ws, ignored) -> new PouleVorrundenRanglisteSheetUpdate(ws)),
				SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerPoule),
				ws -> () -> new PouleVorrundenRanglisteSheetUpdate(ws).doRun()));
		ohnePropertyTrigger(mitErstaufbau(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, TurnierSystem.FORMULEX,
				new EingabeSignatur(SignaturQuellen::fuerFormuleX),
				(ws, ignored) -> new FormuleXRanglisteSheetUpdate(ws)),
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerFormuleX),
				ws -> () -> new FormuleXRanglisteSheetUpdate(ws).doRun()));
		ohnePropertyTrigger(mitErstaufbau(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE, TurnierSystem.KASKADE,
				new EingabeSignatur(SignaturQuellen::fuerKaskade),
				(ws, ignored) -> new KaskadeGruppenRanglisteSheetUpdate(ws)),
				SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerKaskade),
				ws -> () -> new KaskadeGruppenRanglisteSheetUpdate(ws).doRun()));
		jgj();
		liga();
		tripTete();
	}

	/** JGJ: Rangliste samt Direktvergleich, Gesamtrangliste mit eigenem Hash. */
	private void jgj() {
		ohnePropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE, TurnierSystem.JGJ,
				new EingabeSignatur(SignaturQuellen::fuerJGJ),
				(ws, ignored) -> new JGJRanglisteSheetUpdate(ws))
				.mitExportAusgaben(ws -> List.of(new SheetSyncAusgabe(SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE,
						new EingabeSignatur(SignaturQuellen::fuerJGJ),
						SheetSyncAusgabe.fehltEines(ws.getWorkingSpreadsheetDocument(),
								SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE,
								SheetMetadataHelper.SCHLUESSEL_JGJ_DIREKTVERGLEICH),
						() -> {
							new JGJRanglisteSheetUpdate(ws).doRun();
							new JGJRanglisteDirektvergleichSheet(ws).aktualisieren();
						}))));
		ohnePropertyTrigger(mitErstaufbau(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_JGJ_GESAMTRANGLISTE, TurnierSystem.JGJ,
				new EingabeSignatur(SignaturQuellen::fuerJGJ),
				(ws, ignored) -> new JGJGesamtranglisteSheetUpdate(ws)),
				SheetMetadataHelper.SCHLUESSEL_JGJ_GESAMTRANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerJGJ),
				ws -> () -> new JGJGesamtranglisteSheetUpdate(ws).doRun()));
	}

	/** Liga: Rangliste samt Direktvergleich; Termine pro Teilnehmer als Blattgruppe mit gemeinsamem Hash. */
	private void liga() {
		ohnePropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE, TurnierSystem.LIGA,
				new EingabeSignatur(SignaturQuellen::fuerLiga),
				(ws, ignored) -> new LigaRanglisteSheetUpdate(ws))
				.mitExportAusgaben(ws -> List.of(new SheetSyncAusgabe(SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE,
						new EingabeSignatur(SignaturQuellen::fuerLiga),
						SheetSyncAusgabe.fehltEines(ws.getWorkingSpreadsheetDocument(),
								SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE,
								SheetMetadataHelper.SCHLUESSEL_LIGA_DIREKTVERGLEICH),
						() -> {
							new LigaRanglisteSheetUpdate(ws).doRun();
							new LigaRanglisteDirektvergleichSheet(ws).aktualisieren();
						}))));
		// Termine pro Teilnehmer: das Update löscht und erzeugt die Blätter neu – nicht im Hintergrund.
		ohnePropertyTrigger(SheetSyncListener.fuerSchluesselPraefix(context,
				SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER,
				SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER_PREFIX,
				TurnierSystem.LIGA,
				new EingabeSignatur(SignaturQuellen::fuerLigaTermineProTeilnehmer),
				(ws, ignored) -> new LigaTermineProTeilnehmerSheet(ws))
				.nichtImHintergrund()
				.mitExportAusgaben(SheetSyncKatalog::ligaTermineAusgabe));
	}

	/** Ein Termin-Blatt je Team; fehlt eines, werden alle neu erzeugt. */
	private static List<SheetSyncAusgabe> ligaTermineAusgabe(WorkingSpreadsheet ws) throws GenerateException {
		LigaMeldeListeSheetUpdate meldeliste = new LigaMeldeListeSheetUpdate(ws);
		meldeliste.upDateSheet();
		TeamMeldungen meldungen = meldeliste.getAlleMeldungen();
		long anzahlTerminBlaetter = Arrays.stream(SheetMetadataHelper.getSchluesselMitPrefix(
				ws.getWorkingSpreadsheetDocument(), SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER_PREFIX))
				.filter(s -> s.endsWith(SheetMetadataHelper.SCHLUESSEL_SUFFIX)).count();
		return List.of(new SheetSyncAusgabe(SheetMetadataHelper.SCHLUESSEL_LIGA_TERMINE_PRO_TEILNEHMER,
				new EingabeSignatur(SignaturQuellen::fuerLigaTermineProTeilnehmer),
				anzahlTerminBlaetter < meldungen.teams().size(),
				() -> new LigaTermineProTeilnehmerSheet(ws).generate(meldungen)));
	}

	/** Trip-Tête: das Update kennt keinen Erstaufbau, eine fehlende Rangliste baut die Vollaufbau-Klasse. */
	private void tripTete() {
		ohnePropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE, TurnierSystem.TRIPTETE,
				new EingabeSignatur(SignaturQuellen::fuerTripTete),
				(ws, ignored) -> new TripTeteRanglisteSheetUpdate(ws))
				.mitExportAusgaben(ws -> {
					boolean fehlt = SheetSyncAusgabe.fehltEines(ws.getWorkingSpreadsheetDocument(),
							SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE);
					return List.of(new SheetSyncAusgabe(SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE,
							new EingabeSignatur(SignaturQuellen::fuerTripTete), fehlt,
							() -> {
								if (fehlt) {
									new TripTeteRanglisteSheet(ws).upDateSheet();
								} else {
									new TripTeteRanglisteSheetUpdate(ws).doRun();
								}
							}));
				}));
	}

	/** Supermelee: Spieltag-Ranglisten je Nr (Export: aktiver Spieltag notfalls anlegen) und Endrangliste. */
	private void supermelee() {
		ohnePropertyTrigger(SheetSyncListener.fuerSpieltagRangliste(context, TurnierSystem.SUPERMELEE,
				SheetSyncKatalog::supermeleeSpieltagSignatur,
				(ws, xSheet) -> new SpieltagRanglisteSheetUpdate(ws,
						SheetMetadataHelper.findeSpieltagNr(ws.getWorkingSpreadsheetDocument(), xSheet)
								.orElse(null)))
				.mitExportAusgaben(ws -> {
					int spieltagNr = new SuperMeleeKonfigurationSheet(ws).getAktiveSpieltag().getNr();
					if (spieltagNr <= 0) {
						return List.of();
					}
					return List.of(new SheetSyncAusgabe("SUPERMELEE_SPIELTAG_" + spieltagNr,
							supermeleeSpieltagSignatur(spieltagNr),
							SheetSyncAusgabe.fehltEines(ws.getWorkingSpreadsheetDocument(),
									SheetMetadataHelper.schluesselSpieltagRangliste(spieltagNr)),
							() -> new SpieltagRanglisteSheetUpdate(ws, SpielTagNr.from(spieltagNr)).doRun()));
				}));
		ohnePropertyTrigger(mitErstaufbau(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE, TurnierSystem.SUPERMELEE,
				new EingabeSignatur(SignaturQuellen::fuerSupermeleeEnd),
				(ws, ignored) -> new EndranglisteSheetUpdate(ws)),
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE,
				new EingabeSignatur(SignaturQuellen::fuerSupermeleeEnd),
				ws -> () -> new EndranglisteSheetUpdate(ws).doRun()));
		mitPropertyTrigger(SheetSyncListener.fuerSpieltagTeilnehmer(context, TurnierSystem.SUPERMELEE,
				spieltagNr -> new EingabeSignatur(
						xDoc -> SignaturQuellen.fuerSupermeleeTeilnehmer(xDoc, spieltagNr),
						SignaturQuellen::teilnehmerSortKontext),
				(ws, xSheet) -> {
					SupermeleeTeilnehmerSheetUpdate update = new SupermeleeTeilnehmerSheetUpdate(ws);
					SheetMetadataHelper.findeTeilnehmerSpieltagNr(ws.getWorkingSpreadsheetDocument(), xSheet)
							.ifPresent(update::setSpielTagNr);
					return update;
				}));
	}

	private static EingabeSignatur supermeleeSpieltagSignatur(int spieltagNr) {
		return new EingabeSignatur(xDoc -> SignaturQuellen.fuerSupermeleeSpieltag(xDoc, spieltagNr));
	}

	/**
	 * Teilnehmerlisten der Einzel-Sheet-Systeme: alle unter dem generischen {@code SCHLUESSEL_TEILNEHMER}; die
	 * Eindeutigkeit ergibt sich aus dem Turniersystem-Gate.
	 */
	private void teilnehmerlisten() {
		teilnehmer(TurnierSystem.SCHWEIZER, new EingabeSignatur(SignaturQuellen::fuerSchweizerTeilnehmer,
				SignaturQuellen::teilnehmerSortKontext), (ws, ignored) -> new SchweizerTeilnehmerSheetUpdate(ws));
		teilnehmer(TurnierSystem.JGJ, new EingabeSignatur(SignaturQuellen::fuerJGJTeilnehmer,
				SignaturQuellen::teilnehmerSortKontext), (ws, ignored) -> new JGJTeilnehmerSheetUpdate(ws));
		teilnehmer(TurnierSystem.KO, new EingabeSignatur(SignaturQuellen::fuerKoTeilnehmer,
				SignaturQuellen::teilnehmerSortKontext), (ws, ignored) -> new KoTeilnehmerSheetUpdate(ws));
		teilnehmer(TurnierSystem.MAASTRICHTER, new EingabeSignatur(SignaturQuellen::fuerMaastrichterTeilnehmer,
				SignaturQuellen::teilnehmerSortKontext), (ws, ignored) -> new MaastrichterTeilnehmerSheetUpdate(ws));
		teilnehmer(TurnierSystem.KASKADE, new EingabeSignatur(SignaturQuellen::fuerKaskadeTeilnehmer,
				SignaturQuellen::teilnehmerSortKontext), (ws, ignored) -> new KaskadeTeilnehmerSheetUpdate(ws));
		teilnehmer(TurnierSystem.FORMULEX, new EingabeSignatur(SignaturQuellen::fuerFormuleXTeilnehmer,
				SignaturQuellen::teilnehmerSortKontext), (ws, ignored) -> new FormuleXTeilnehmerSheetUpdate(ws));
		teilnehmer(TurnierSystem.POULE, new EingabeSignatur(SignaturQuellen::fuerPouleTeilnehmer,
				SignaturQuellen::teilnehmerSortKontext), (ws, ignored) -> new PouleTeilnehmerSheetUpdate(ws));
		teilnehmer(TurnierSystem.TRIPTETE, new EingabeSignatur(SignaturQuellen::fuerTripTeteTeilnehmer,
				SignaturQuellen::teilnehmerSortKontext), (ws, ignored) -> new TripTeteTeilnehmerSheetUpdate(ws));
	}

	private void teilnehmer(TurnierSystem system, EingabeSignatur signatur,
			BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> update) {
		mitPropertyTrigger(SheetSyncListener.fuerSchluessel(context, SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				system, signatur, update));
	}

	/**
	 * Checkin-Listen: Eingabe-Quelle ist (wie bei den Teilnehmerlisten) ausschließlich die Meldeliste. Jedes System
	 * hat einen eigenen Checkin-Schlüssel.
	 */
	private void checkinListen() {
		mitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_POULE_CHECKIN_LISTE, TurnierSystem.POULE,
				new EingabeSignatur(SignaturQuellen::fuerPouleTeilnehmer, SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new PouleCheckinListeSheetUpdate(ws)));
		mitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_CHECKIN_LISTE, TurnierSystem.SCHWEIZER,
				new EingabeSignatur(SignaturQuellen::fuerSchweizerTeilnehmer, SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new SchweizerCheckinListeSheetUpdate(ws)));
		mitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_KO_CHECKIN_LISTE, TurnierSystem.KO,
				new EingabeSignatur(SignaturQuellen::fuerKoTeilnehmer, SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new KoCheckinListeSheetUpdate(ws)));
		mitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_CHECKIN_LISTE, TurnierSystem.MAASTRICHTER,
				new EingabeSignatur(SignaturQuellen::fuerMaastrichterTeilnehmer, SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new MaastrichterCheckinListeSheetUpdate(ws)));
		mitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_KASKADE_CHECKIN_LISTE, TurnierSystem.KASKADE,
				new EingabeSignatur(SignaturQuellen::fuerKaskadeTeilnehmer, SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new KaskadeCheckinListeSheetUpdate(ws)));
		mitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_CHECKIN_LISTE, TurnierSystem.FORMULEX,
				new EingabeSignatur(SignaturQuellen::fuerFormuleXTeilnehmer, SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new FormuleXCheckinListeSheetUpdate(ws)));
		mitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_JGJ_CHECKIN_LISTE, TurnierSystem.JGJ,
				new EingabeSignatur(SignaturQuellen::fuerJGJTeilnehmer, SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new JGJCheckinListeSheetUpdate(ws)));
		mitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_CHECKIN_LISTE, TurnierSystem.TRIPTETE,
				new EingabeSignatur(SignaturQuellen::fuerTripTeteTeilnehmer, SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new TripTeteCheckinListeSheetUpdate(ws)));
		// Supermelee-Anmeldungen (Checkin-Liste) sind spieltag-variabel – eigener Schlüssel je Nr.
		mitPropertyTrigger(SheetSyncListener.fuerSpieltagSheet(context, TurnierSystem.SUPERMELEE,
				SheetMetadataHelper::findeAnmeldungenSpieltagNr, "SUPERMELEE_ANMELDUNGEN_",
				spieltagNr -> new EingabeSignatur(
						xDoc -> SignaturQuellen.fuerSupermeleeTeilnehmer(xDoc, spieltagNr),
						SignaturQuellen::checkinSortKontext),
				(ws, xSheet) -> {
					AnmeldungenSheetUpdate update = new AnmeldungenSheetUpdate(ws);
					SheetMetadataHelper.findeAnmeldungenSpieltagNr(ws.getWorkingSpreadsheetDocument(), xSheet)
							.ifPresent(update::setSpielTag);
					return update;
				}));
	}

	/** Aktion, die ein Ausgabe-Blatt aktualisiert oder – falls es fehlt – über das Update anlegt. */
	@FunctionalInterface
	private interface ErstaufbauAktion {
		SheetSyncAusgabe.Aktion fuer(WorkingSpreadsheet ws);
	}

	/** Export-Ausgabe für ein einzelnes, per Schlüssel identifiziertes Blatt, dessen Update fehlende anlegt. */
	private static SheetSyncListener mitErstaufbau(SheetSyncListener listener, String schluessel,
			EingabeSignatur signatur, ErstaufbauAktion aktion) {
		return listener.mitExportAusgaben(ws -> {
			XSpreadsheetDocument xDoc = ws.getWorkingSpreadsheetDocument();
			return List.of(new SheetSyncAusgabe(schluessel, signatur, SheetSyncAusgabe.fehltEines(xDoc, schluessel),
					aktion.fuer(ws)));
		});
	}

	private void ohnePropertyTrigger(SheetSyncListener listener) {
		eintraege.add(new Eintrag(listener, false));
	}

	private void mitPropertyTrigger(SheetSyncListener listener) {
		eintraege.add(new Eintrag(listener, true));
	}
}
