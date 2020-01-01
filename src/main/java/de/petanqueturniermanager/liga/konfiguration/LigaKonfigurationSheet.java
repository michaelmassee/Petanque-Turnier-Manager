/**
 * Erstellung 09.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public class LigaKonfigurationSheet extends BaseKonfigurationSheet implements ILigaPropertiesSpalte, IKonfigurationSheet {

	private static final Logger logger = LogManager.getLogger(LigaKonfigurationSheet.class);

	private final LigaPropertiesSpalte propertiesSpalte;

	/**
	 * @param workingSpreadsheet
	 */
	LigaKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.LIGA);
		propertiesSpalte = new LigaPropertiesSpalte(NAME_PROPERTIES_SPALTE, ERSTE_ZEILE_PROPERTIES, this);
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return this;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		// nur Activ setzten
		TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
	}

	@Override
	protected IPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected void updateTurnierSystemKonfiguration() throws GenerateException {
		// nichts, wenn zusatz spalten dann hier
	}

	@Override
	protected void updateTurnierSystemKonfigBlock() throws GenerateException {
		propertiesSpalte.updateKonfigBlock(); // Liga + Allgemeine properties
	}

	@Override
	protected void initPageStylesTurnierSystem() throws GenerateException {
		// default page Style
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setFooterCenter(getFusszeileMitte()).setFooterLeft(getFusszeileLinks())
				.setHeaderLeft(getKopfZeileLinks()).setHeaderCenter(getKopfZeileMitte()).setHeaderRight(getKopfZeileRechts()).create().applytoSheet();
	}

	@Override
	public Integer getSpielPlanHeaderFarbe() throws GenerateException {
		return propertiesSpalte.getSpielPlanHeaderFarbe();
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeUnGerade() throws GenerateException {
		return propertiesSpalte.getSpielPlanHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getSpielPlanHintergrundFarbeGerade() throws GenerateException {
		return propertiesSpalte.getSpielPlanHintergrundFarbeGerade();
	}

	@Override
	public String getKopfZeileLinks() throws GenerateException {
		return propertiesSpalte.getKopfZeileLinks();
	}

	@Override
	public String getKopfZeileMitte() throws GenerateException {
		return propertiesSpalte.getKopfZeileMitte();
	}

	@Override
	public String getKopfZeileRechts() throws GenerateException {
		return propertiesSpalte.getKopfZeileRechts();
	}

}
