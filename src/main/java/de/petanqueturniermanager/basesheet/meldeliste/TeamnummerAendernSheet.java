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
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

/**
 * Menüpunkt „Teamnummer ändern“: ändert die angezeigte Nummer einer Meldung, ihre UUID bleibt. Spielrunden, die
 * Teams über die UUID referenzieren, zeigen danach die neue Nummer; ist die neue Nummer vergeben, tauschen beide
 * Meldungen.
 * <p>
 * Nur für Turniersysteme, deren Spielrunden schon die UUID speichern – in den übrigen würde eine geänderte
 * Nummer die Runden auf ein anderes Team zeigen lassen.
 */
public class TeamnummerAendernSheet extends SheetRunner {

	/** Systeme, deren Spielrunden und Ranglisten Teams über die UUID referenzieren (umbau-nr-uuid.md). */
	static final Set<TurnierSystem> UMGESTELLTE_SYSTEME = Set.of(TurnierSystem.SCHWEIZER, TurnierSystem.KO,
			TurnierSystem.FORMULEX, TurnierSystem.KASKADE, TurnierSystem.TRIPTETE, TurnierSystem.JGJ,
			TurnierSystem.LIGA, TurnierSystem.POULE);

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
		melde(MessageBoxTypeEnum.INFO_OK, plan.getauscht()
				? I18n.get("teamnummer.aendern.getauscht", wunsch.alteNr(), wunsch.neueNr())
				: I18n.get("teamnummer.aendern.geaendert", wunsch.alteNr(), wunsch.neueNr()));
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
