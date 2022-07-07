/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
abstract public class BaseKonfigurationSheet extends SheetRunner implements IPropertiesSpalte, IKonfigurationSheet {

	/**
	 * @param workingSpreadsheet
	 */
	public BaseKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem spielSystem) {
		super(workingSpreadsheet, spielSystem);
	}

	// Wird immer von Sheetrunner aufgerufen
	@Override
	public final void update() throws GenerateException {
		// processBoxinfo("Update Konfiguration");
		// validate SpielSystem
		validateSpielSystem();
		updateTurnierSystemInDocument();
		initPageStyles();
		initPageStylesTurnierSystem();
	}

	protected abstract void initPageStylesTurnierSystem() throws GenerateException;

	private void initPageStyles() throws GenerateException {
		// default page Style footer zeilen
		// sicher gehen das änderungen ankommen
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setFooterCenter(getFusszeileMitte())
				.setFooterLeft(getFusszeileLinks()).create();
	}

	private void updateTurnierSystemInDocument() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, getTurnierSystem().getId());
	}

	private void validateSpielSystem() throws GenerateException {
		// Property im Document vorhanden ?
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());
		int spielsystem = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.KEIN.getId());
		if (spielsystem > 0) { // 0 = Kein
			TurnierSystem turnierSystemAusDocument = TurnierSystem.findById(spielsystem);
			TurnierSystem turnierSystemAusSheet = getTurnierSystem();
			if (turnierSystemAusDocument != null && turnierSystemAusSheet.getId() != turnierSystemAusDocument.getId()) {
				de.petanqueturniermanager.helper.msgbox.ProcessBox.from()
						.fehler("Dokument wurde mit Turniersystem " + turnierSystemAusDocument + " erstellt.");
				throw new GenerateException("Turniersystem '" + getTurnierSystem() + "' stimmt nicht mit Dokument '"
						+ turnierSystemAusDocument + "' überein");
			}
		}
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeGerade() throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHintergrundFarbeGerade();
	}

	@Override
	public final Integer getMeldeListeHintergrundFarbeUnGerade() throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHintergrundFarbeUnGerade();
	}

	@Override
	public final Integer getMeldeListeHeaderFarbe() throws GenerateException {
		return getPropertiesSpalte().getMeldeListeHeaderFarbe();
	}

	@Override
	public final String getFusszeileLinks() throws GenerateException {
		return getPropertiesSpalte().getFusszeileLinks();
	}

	@Override
	public final String getFusszeileMitte() throws GenerateException {
		return getPropertiesSpalte().getFusszeileMitte();
	}

	@Override
	public final boolean zeigeArbeitsSpalten() throws GenerateException {
		return getPropertiesSpalte().zeigeArbeitsSpalten();
	}

	@Override
	public final XSpreadsheet getXSpreadSheet() throws GenerateException {
		throw new GenerateException("nicht erlaubt");
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public final Integer getRanglisteHintergrundFarbeGerade() throws GenerateException {
		return getPropertiesSpalte().getRanglisteHintergrundFarbeGerade();
	}

	@Override
	public final Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException {
		return getPropertiesSpalte().getRanglisteHintergrundFarbeUnGerade();
	}

	@Override
	public final Integer getRanglisteHeaderFarbe() throws GenerateException {
		return getPropertiesSpalte().getRanglisteHeaderFarbe();
	}

	/**
	 * @return the propertiesSpalte
	 */
	protected abstract IPropertiesSpalte getPropertiesSpalte();

}
