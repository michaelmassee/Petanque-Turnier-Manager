/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.Optional;
import java.util.Set;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAendernDialog.Wunsch;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAenderung.Ablehnung;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAenderung.Ergebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJGesamtranglisteSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetUpdate;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadenRanglistenAktualisierer;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheetUpdate;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterGruppenSpalteHelper;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheetUpdate;

/**
 * Menüpunkt „Teamnummer ändern“: ändert die angezeigte Nummer einer Meldung, ihre UUID bleibt. Spielrunden, die
 * Teams über die UUID referenzieren, zeigen danach die neue Nummer; ist die neue Nummer vergeben, tauschen beide
 * Meldungen.
 * <p>
 * Nur für Turniersysteme, deren Spielrunden die UUID speichern ({@link #UMGESTELLTE_SYSTEME}, inzwischen alle).
 */
public class TeamnummerAendernSheet extends SheetRunner {

	/** Systeme, deren Spielrunden und Ranglisten Teams über die UUID referenzieren (umbau-nr-uuid.md). */
	static final Set<TurnierSystem> UMGESTELLTE_SYSTEME = Set.of(TurnierSystem.SCHWEIZER, TurnierSystem.KO,
			TurnierSystem.FORMULEX, TurnierSystem.KASKADE, TurnierSystem.TRIPTETE, TurnierSystem.JGJ,
			TurnierSystem.LIGA, TurnierSystem.POULE, TurnierSystem.SUPERMELEE,
			TurnierSystem.MAASTRICHTER);

	/** Nr-Spalte der Meldeliste (A), wie {@link MeldelisteUuidIndex}. */
	private static final int NR_SPALTE = 0;

	private Wunsch vorgabe;

	public TeamnummerAendernSheet(WorkingSpreadsheet ws) {
		super(ws, new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument(), "TeamnummerAendern");
	}

	/** Ändert ohne Dialog – für Tests und Automatisierung. */
	public TeamnummerAendernSheet mitEingabe(int alteNr, int neueNr) {
		vorgabe = new Wunsch(alteNr, neueNr);
		return this;
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	@Override
	public void doRun() throws GenerateException {
		if (!UMGESTELLTE_SYSTEME.contains(getTurnierSystem())) {
			melde(MessageBoxTypeEnum.WARN_OK, I18n.get("teamnummer.aendern.fehler.system"));
			return;
		}
		XSpreadsheet meldeliste = getSheetHelper().findByName(SheetNamen.meldeliste());
		if (meldeliste == null) {
			melde(MessageBoxTypeEnum.WARN_OK, I18n.get("teamnummer.aendern.fehler.keine_meldeliste"));
			return;
		}
		Optional<Wunsch> wunsch = vorgabe != null ? Optional.of(vorgabe)
				: TeamnummerAendernDialog.zeigen(getWorkingSpreadsheet());
		if (wunsch.isEmpty()) {
			return;
		}
		aendern(meldeliste, wunsch.get());
	}

	private void aendern(XSpreadsheet meldeliste, Wunsch wunsch) throws GenerateException {
		MeldelisteUuidIndex.NrSpalte spalte = MeldelisteUuidIndex.leseNrSpalte(getSheetHelper(), meldeliste,
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument());
		Ergebnis ergebnis = TeamnummerAenderung.plane(spalte.formeln(), spalte.nummern(), wunsch.alteNr(),
				wunsch.neueNr());
		if (ergebnis.ablehnung().isPresent()) {
			melde(MessageBoxTypeEnum.WARN_OK, ablehnungsText(ergebnis.ablehnung().get(), wunsch));
			return;
		}
		TeamnummerAenderung.Plan plan = ergebnis.plan().orElseThrow();
		TeamNummernVergabe.schreibeNrFormeln(getSheetHelper(), meldeliste, NR_SPALTE, plan.formelProZeile());
		getxCalculatable().calculateAll();
		nachziehen(wunsch);
		melde(MessageBoxTypeEnum.INFO_OK, plan.getauscht()
				? I18n.get("teamnummer.aendern.getauscht", wunsch.alteNr(), wunsch.neueNr())
				: I18n.get("teamnummer.aendern.geaendert", wunsch.alteNr(), wunsch.neueNr()));
	}

	/**
	 * Gespeicherte Nummern-Werte auf die neue Nummer bringen: Ranglisten führen Nummern als Werte und werden sofort
	 * aktualisiert, damit auch Leser ohne Tabwechsel (Webserver, Export, PTM-Online-Live) die neue Nummer sehen.
	 * Nicht vorhandene Ranglisten werden nicht angelegt.
	 */
	private void nachziehen(Wunsch wunsch) throws GenerateException {
		WorkingSpreadsheet ws = getWorkingSpreadsheet();
		switch (getTurnierSystem()) {
		case SCHWEIZER -> fallsVorhanden(new SchweizerRanglisteSheetUpdate(ws), SchweizerRanglisteSheetUpdate::doRun);
		case MAASTRICHTER -> MaastrichterGruppenSpalteHelper.nummerAendern(
				new MaastrichterVorrundenRanglisteSheetUpdate(ws), wunsch.alteNr(), wunsch.neueNr());
		case FORMULEX -> fallsVorhanden(new FormuleXRanglisteSheetUpdate(ws), FormuleXRanglisteSheetUpdate::doRun);
		case JGJ -> {
			fallsVorhanden(new JGJRanglisteSheetUpdate(ws), JGJRanglisteSheetUpdate::doRun);
			fallsVorhanden(new JGJGesamtranglisteSheetUpdate(ws), JGJGesamtranglisteSheetUpdate::doRun);
		}
		case LIGA -> fallsVorhanden(new LigaRanglisteSheetUpdate(ws), LigaRanglisteSheetUpdate::doRun);
		case KASKADE -> new KaskadenRanglistenAktualisierer(ws, getSheetHelper()).aktualisiereRanglisten();
		case TRIPTETE -> fallsVorhanden(new TripTeteRanglisteSheetUpdate(ws), TripTeteRanglisteSheetUpdate::doRun);
		case POULE -> fallsVorhanden(new PouleVorrundenRanglisteSheetUpdate(ws),
				PouleVorrundenRanglisteSheetUpdate::doRun);
		case SUPERMELEE -> {
			spieltagRanglistenNeuAufbauen();
			fallsVorhanden(new EndranglisteSheetUpdate(ws), EndranglisteSheetUpdate::doRun);
		}
		default -> {
			// KO: keine Rangliste; der Turnierbaum zeigt die Nummer über die UUID.
		}
		}
	}

	/** Aktualisierung einer Rangliste, typisiert auf ihre {@code *SheetUpdate}-Klasse. */
	@FunctionalInterface
	private interface Aktualisierung<T> {
		void ausfuehren(T rangliste) throws GenerateException;
	}

	private static <T extends ISheet> void fallsVorhanden(T rangliste, Aktualisierung<T> aktualisierung)
			throws GenerateException {
		if (rangliste.getXSpreadSheet() != null) {
			aktualisierung.ausfuehren(rangliste);
		}
	}

	/**
	 * Die Supermelee-Endrangliste liest die Spieltag-Ranglisten über die Spielernummer (Werte). Ohne Neuaufbau
	 * führten ältere Spieltage den Spieler weiter unter der alten Nummer.
	 */
	private void spieltagRanglistenNeuAufbauen() throws GenerateException {
		SpieltagRanglisteSheet spieltagRangliste = new SpieltagRanglisteSheet(getWorkingSpreadsheet());
		int anzRanglisten = spieltagRangliste.countNumberOfRanglisten();
		for (int spieltag = 1; spieltag <= anzRanglisten; spieltag++) {
			SheetRunner.testDoCancelTask();
			spieltagRangliste.generate(SpielTagNr.from(spieltag));
		}
	}

	private static String ablehnungsText(Ablehnung ablehnung, Wunsch wunsch) {
		return switch (ablehnung) {
		case NEUE_NR_UNGUELTIG -> I18n.get("teamnummer.aendern.fehler.neue_ungueltig");
		case GLEICHE_NR -> I18n.get("teamnummer.aendern.fehler.gleich");
		case ALTE_NR_FEHLT -> I18n.get("teamnummer.aendern.fehler.alte_fehlt", wunsch.alteNr());
		case OHNE_UUID -> I18n.get("teamnummer.aendern.fehler.ohne_uuid");
		};
	}

	private void melde(MessageBoxTypeEnum typ, String text) {
		MessageBox.from(getWorkingSpreadsheet().getxContext(), typ)
				.caption(I18n.get("teamnummer.aendern.dialog.titel")).message(text).show();
	}
}
