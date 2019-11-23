/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
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
		processBoxinfo("Update Konfiguration");
		// validate SpielSystem
		validateSpielSystem();
		updateTurnierSystemKonfigBlock();
		doFormat();
		updateTurnierSystemKonfiguration();
		updateTurnierSystemInDocument();
		ProcessBox.from().spielTag(getAktiveSpieltag()).spielRunde(getAktiveSpielRunde()).turnierSystem(getTurnierSystem());
		initPageStyles();
		initPageStylesTurnierSystem();
	}

	protected abstract void initPageStylesTurnierSystem() throws GenerateException;

	private void initPageStyles() throws GenerateException {
		// default page Style
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setFooterCenter(getFusszeileMitte()).setFooterLeft(getFusszeileLinks()).create().applytoSheet();
	}

	private void updateTurnierSystemInDocument() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());
		docPropHelper.insertIntPropertyIfNotExist(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, getTurnierSystem().getId());
	}

	private void validateSpielSystem() throws GenerateException {
		// Property im Document vorhanden ?
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());
		int spielsystem = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM);
		if (spielsystem > -1) {
			TurnierSystem turnierSystemAusDocument = TurnierSystem.findById(spielsystem);
			TurnierSystem turnierSystemAusSheet = getTurnierSystem();
			if (turnierSystemAusDocument != null && turnierSystemAusSheet.getId() != turnierSystemAusDocument.getId()) {
				ProcessBox.from().fehler("Dokument wurde mit Turniersystem " + turnierSystemAusDocument + " erstellt.");
				throw new GenerateException("Turniersystem '" + getTurnierSystem() + "' stimmt nicht mit Dokument '" + turnierSystemAusDocument + "' überein");
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
	public void updateKonfigBlock() throws GenerateException {
		getPropertiesSpalte().updateKonfigBlock();
	}

	@Override
	public void doFormat() throws GenerateException {
		getPropertiesSpalte().doFormat();
	}

	@Override
	public final SpielTagNr getAktiveSpieltag() throws GenerateException {
		return getPropertiesSpalte().getAktiveSpieltag();
	}

	@Override
	public final void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException {
		getPropertiesSpalte().setAktiveSpieltag(spieltag);
	}

	@Override
	public final SpielRundeNr getAktiveSpielRunde() throws GenerateException {
		return getPropertiesSpalte().getAktiveSpielRunde();
	}

	@Override
	public final void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException {
		getPropertiesSpalte().setAktiveSpielRunde(neueSpielrunde);
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
		return NewSheet.from(getWorkingSpreadsheet(), SHEETNAME).pos(DefaultSheetPos.KONFIGURATION).setActiv().tabColor(SHEET_COLOR).useIfExist().hideGrid().create().getSheet();
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	/**
	 * @return the propertiesSpalte
	 */
	protected abstract IPropertiesSpalte getPropertiesSpalte();

	protected abstract void updateTurnierSystemKonfiguration() throws GenerateException;

	protected abstract void updateTurnierSystemKonfigBlock() throws GenerateException;

}
