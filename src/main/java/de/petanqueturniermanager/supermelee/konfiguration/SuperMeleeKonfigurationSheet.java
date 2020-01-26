/**
* Erstellung : 06.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.konfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public class SuperMeleeKonfigurationSheet extends BaseKonfigurationSheet implements ISuperMeleePropertiesSpalte, IKonfigurationSheet {

	private static final Logger logger = LogManager.getLogger(SuperMeleeKonfigurationSheet.class);

	public static final int MAX_SPIELTAG = 10;
	private static final int KONFIG_SPIELTAG_NR_SPALTE = NAME_PROPERTIES_SPALTE + 3;
	private static final int KONFIG_SPIELTAG_KOPFZEILE_SPALTE = KONFIG_SPIELTAG_NR_SPALTE + 1;

	private final SuperMeleePropertiesSpalte propertiesSpalte;

	// Package weil nur in SuperMeleeSheet verwendet werden darf
	SuperMeleeKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE);
		propertiesSpalte = new SuperMeleePropertiesSpalte(NAME_PROPERTIES_SPALTE, ERSTE_ZEILE_PROPERTIES, this);
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

	// update immer einmal in SheetRunner
	// wird von #BaseKonfigurationSheet.update() verwendet
	@Override
	protected void updateTurnierSystemKonfiguration() throws GenerateException {
	}

	/**
	 *
	 * @param spielTagNr
	 * @return null wenn not found
	 * @throws GenerateException
	 */

	public String getKopfZeile(SpielTagNr spielTagNr) throws GenerateException {
		checkNotNull(spielTagNr);
		Position posKopfzeile = Position.from(KONFIG_SPIELTAG_KOPFZEILE_SPALTE, ERSTE_ZEILE_PROPERTIES + (spielTagNr.getNr() - 1));
		return getSheetHelper().getTextFromCell(getXSpreadSheet(), posKopfzeile);
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeGerade() throws GenerateException {
		return propertiesSpalte.getSpielRundeHintergrundFarbeGerade();
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeUnGerade() throws GenerateException {
		return propertiesSpalte.getSpielRundeHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getSpielRundeHeaderFarbe() throws GenerateException {
		return propertiesSpalte.getSpielRundeHeaderFarbe();
	}

	@Override
	public Integer getSpielRundeNeuAuslosenAb() throws GenerateException {
		return propertiesSpalte.getSpielRundeNeuAuslosenAb();
	}

	@Override
	public Integer getRanglisteHintergrundFarbe_StreichSpieltag_Gerade() throws GenerateException {
		return propertiesSpalte.getRanglisteHintergrundFarbe_StreichSpieltag_Gerade();
	}

	@Override
	public Integer getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade() throws GenerateException {
		return propertiesSpalte.getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade();
	}

	@Override
	public Integer getNichtGespielteRundePlus() throws GenerateException {
		return propertiesSpalte.getNichtGespielteRundePlus();
	}

	@Override
	public Integer getNichtGespielteRundeMinus() throws GenerateException {
		return propertiesSpalte.getNichtGespielteRundeMinus();
	}

	@Override
	public String getSpielrundeSpielbahn() throws GenerateException {
		return propertiesSpalte.getSpielrundeSpielbahn();
	}

	@Override
	public Integer getAnzGespielteSpieltage() throws GenerateException {
		return propertiesSpalte.getAnzGespielteSpieltage();
	}

	@Override
	public boolean getSpielrunde1Header() throws GenerateException {
		return propertiesSpalte.getSpielrunde1Header();
	}

	@Override
	public SuperMeleeMode getSuperMeleeMode() throws GenerateException {
		return propertiesSpalte.getSuperMeleeMode();
	}

	@Override
	public boolean getSpielrundePlan() throws GenerateException {
		return propertiesSpalte.getSpielrundePlan();
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return this;
	}

	@Override
	protected IPropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	protected void updateTurnierSystemKonfigBlock() throws GenerateException {
		propertiesSpalte.updateKonfigBlock(); // SuperMelee + Allgemeine properties
	}

	@Override
	protected void initPageStylesTurnierSystem() throws GenerateException {
		for (int spieltagCntr = 1; spieltagCntr <= MAX_SPIELTAG; spieltagCntr++) {
			String propNameKey = SuperMeleePropertiesSpalte.PROP_SPIELTAG_KOPFZEILE(spieltagCntr);
			String spielTagKopfzeile = propertiesSpalte.readStringProperty(propNameKey);
			if (spielTagKopfzeile != null) {
				PageStyleHelper.from(this, SpielTagNr.from(spieltagCntr)).initDefaultFooter().setFooterCenter(getFusszeileMitte()).setFooterLeft(getFusszeileLinks())
						.setHeaderCenter(spielTagKopfzeile).create();
			}
		}
	}

}
