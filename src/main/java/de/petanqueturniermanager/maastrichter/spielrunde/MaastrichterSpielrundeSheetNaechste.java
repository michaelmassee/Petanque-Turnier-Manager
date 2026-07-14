/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.spielrunde;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.rangliste.IRanglistenAktualisierer;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellt die nächste Vorrunde im Maastrichter Turnier mit dem Schweizer Algorithmus.
 * Sichtbar heißen die Runden wie normale Spielrunden, die Maastrichter-Identität steckt in den Sheet-Metadaten.
 */
public class MaastrichterSpielrundeSheetNaechste extends SchweizerSpielrundeSheetNaechste {

	public static final String SHEET_BASIS_NAME = "Vorrunde";

	public MaastrichterSpielrundeSheetNaechste(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, SHEET_BASIS_NAME);
	}

	@Override
	protected SchweizerKonfigurationSheet initKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new MaastrichterKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected String getSpielrundeSchluessel(int rundeNr) {
		return SheetMetadataHelper.schluesselMaastrichterVorrunde(rundeNr);
	}

	@Override
	protected String getSheetName(SpielRundeNr nr) {
		return SheetNamen.maastrichterVorrunde(nr.getNr());
	}

	@Override
	protected SchweizerMeldeListeSheetUpdate initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new MaastrichterMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	protected boolean pruefeKannNeueRundeErstellen(int neueSpielrundeNr) {
		int maxVorrunden = ((MaastrichterKonfigurationSheet) getKonfigurationSheet()).getAnzVorrunden();
		if (maxVorrunden > 0 && neueSpielrundeNr > maxVorrunden) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.naechste.runde.nicht.moeglich"))
					.message(I18n.get("msg.text.maastrichter.max.vorrunden.erreicht", maxVorrunden))
					.show();
			return false;
		}
		return true;
	}

	@Override
	protected IRanglistenAktualisierer getRanglistenAktualisierer() {
		return () -> new MaastrichterVorrundenRanglisteSheetUpdate(getWorkingSpreadsheet()).doRun();
	}

	/** Öffentlicher Einstiegspunkt für Testdaten-Generatoren. */
	public void erstelleNaechsteVorrunde() throws GenerateException {
		doRun();
	}

}
