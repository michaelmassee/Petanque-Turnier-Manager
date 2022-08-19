/**
 * Erstellung : 06.05.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.konfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public class SuperMeleeKonfigurationSheet extends BaseKonfigurationSheet
		implements ISuperMeleePropertiesSpalte, IKonfigurationSheet {

	private static final Logger logger = LogManager.getLogger(SuperMeleeKonfigurationSheet.class);

	public static final int MAX_SPIELTAG = 10;

	private final SuperMeleePropertiesSpalte propertiesSpalte;

	// Package weil nur in SuperMeleeSheet verwendet werden darf
	SuperMeleeKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE);
		propertiesSpalte = new SuperMeleePropertiesSpalte(this);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		throw new GenerateException("nicht erlaubt");
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
	public Integer getMaxAnzGespielteSpieltage() throws GenerateException {
		return propertiesSpalte.getMaxAnzGespielteSpieltage();
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
	public boolean getSetzPositionenAktiv() throws GenerateException {
		return propertiesSpalte.getSetzPositionenAktiv();
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return this;
	}

	@Override
	protected ISuperMeleePropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
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
	public boolean getGleichePaarungenAktiv() throws GenerateException {
		return getPropertiesSpalte().getGleichePaarungenAktiv();
	}

	@Override
	public final void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException {
		getPropertiesSpalte().setAktiveSpielRunde(neueSpielrunde);
	}

	@Override
	protected void initPageStylesTurnierSystem() throws GenerateException {
		for (int spieltagCntr = 1; spieltagCntr <= MAX_SPIELTAG; spieltagCntr++) {
			String propNameKey = SuperMeleePropertiesSpalte.PROP_SPIELTAG_KOPFZEILE(spieltagCntr);
			String spielTagKopfzeile = propertiesSpalte.readStringProperty(propNameKey);
			if (spielTagKopfzeile != null) {
				PageStyleHelper.from(this, SpielTagNr.from(spieltagCntr)).initDefaultFooter()
						.setFooterCenter(getFusszeileMitte()).setFooterLeft(getFusszeileLinks())
						.setHeaderCenter(spielTagKopfzeile).create();
			}
		}
	}

}
