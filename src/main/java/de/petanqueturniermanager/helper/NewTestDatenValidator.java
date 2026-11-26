/*
 * Erstellung 19.06.2022 / Michael Massee
 */
package de.petanqueturniermanager.helper;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.ptmonline.PtmOnlineNeueMeldeliste;

/**
 * Fragt nach, bevor vorhandene Turnierdaten überschrieben werden – beim Anlegen einer neuen Meldeliste und beim
 * Erzeugen von Testdaten.
 *
 * @author michael
 */
public class NewTestDatenValidator {

	private final WorkingSpreadsheet wkSheet;
	private final SheetHelper sheetHelper;
	private final TurnierSystem turnierSystem;
	private String nextLogPrefix;

	private NewTestDatenValidator(WorkingSpreadsheet wkSheet, SheetHelper sheetHelper, TurnierSystem turnierSystem) {
		this.wkSheet = wkSheet;
		this.sheetHelper = sheetHelper;
		this.turnierSystem = turnierSystem;
		nextLogPrefix = null;
	}

	public static NewTestDatenValidator from(WorkingSpreadsheet wkSheet, SheetHelper sheetHelper,
			TurnierSystem turnierSystem) {
		return new NewTestDatenValidator(wkSheet, sheetHelper, turnierSystem);
	}

	/**
	 * @param nextLogPrefix Präfix für die Meldungen in der ProcessBox
	 * @return dieser Validator
	 */
	public NewTestDatenValidator prefix(String nextLogPrefix) {
		this.nextLogPrefix = nextLogPrefix;
		return this;
	}

	/**
	 * Fragt nach, ob ein vorhandenes Turnier überschrieben werden darf. Eine bestehende PTM-Online-Verbindung bleibt
	 * unberührt – sie trennt „Neue Meldeliste“ erst nach dem Parameterdialog.
	 *
	 * @return {@code true}, wenn weitergemacht werden soll
	 */
	public final boolean validate() {
		if (isTurnierVorhanden()) {
			MessageBoxResult result = MessageBox.from(wkSheet.getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption(I18n.get("msg.caption.daten.vorhanden"))
					.message(I18n.get("msg.text.daten.vorhanden")).show();
			if (MessageBoxResult.NO == result) {
				ProcessBox.from().prefix(nextLogPrefix).info(I18n.get("processbox.testdaten.abbruch"));
				return false;
			}
		}
		return true;
	}

	/**
	 * Wie {@link #validate()}, trennt danach aber zusätzlich eine bestehende PTM-Online-Verbindung (nach Rückfrage).
	 * Pflicht für jede Testdaten-Erzeugung, die Blätter ersetzt: Sonst bliebe die Verbindung in den
	 * Dokumenteigenschaften bestehen und der automatische Abgleich legte die Testnamen als Online-Anmeldungen an.
	 *
	 * @return {@code true}, wenn die Testdaten erzeugt werden sollen
	 * @throws GenerateException wenn PTM-Online das Trennen ablehnt
	 */
	public final boolean validateFuerTestdaten() throws GenerateException {
		if (!validate()) {
			return false;
		}
		if (!PtmOnlineNeueMeldeliste.vorbereitenFuerTestdaten(wkSheet)) {
			ProcessBox.from().prefix(nextLogPrefix).info(I18n.get("processbox.testdaten.abbruch"));
			return false;
		}
		return true;
	}

	public final boolean isTurnierVorhanden() {
		TurnierSystem turnierSystemAusDocument = new DocumentPropertiesHelper(wkSheet).getTurnierSystemAusDocument();
		return turnierSystemAusDocument == turnierSystem
				&& sheetHelper.findByName(SheetNamen.meldeliste()) != null;
	}

}
