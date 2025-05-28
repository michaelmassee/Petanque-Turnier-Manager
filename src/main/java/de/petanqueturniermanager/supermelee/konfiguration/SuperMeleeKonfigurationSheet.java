/**
 * Erstellung : 06.05.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.konfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
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
	public SuperMeleeKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
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
	public Integer getSpielRundeHintergrundFarbeGerade() {
		return propertiesSpalte.getSpielRundeHintergrundFarbeGerade();
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeUnGerade() {
		return propertiesSpalte.getSpielRundeHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getSpielRundeHeaderFarbe() {
		return propertiesSpalte.getSpielRundeHeaderFarbe();
	}

	@Override
	public Integer getSpielRundeNeuAuslosenAb() {
		return propertiesSpalte.getSpielRundeNeuAuslosenAb();
	}

	@Override
	public Integer getRanglisteHintergrundFarbeStreichSpieltagGerade() {
		return propertiesSpalte.getRanglisteHintergrundFarbeStreichSpieltagGerade();
	}

	@Override
	public Integer getRanglisteHintergrundFarbeStreichSpieltagUnGerade() {
		return propertiesSpalte.getRanglisteHintergrundFarbeStreichSpieltagUnGerade();
	}

	@Override
	public Integer getNichtGespielteRundePlus() {
		return propertiesSpalte.getNichtGespielteRundePlus();
	}

	@Override
	public Integer getNichtGespielteRundeMinus() {
		return propertiesSpalte.getNichtGespielteRundeMinus();
	}

	@Override
	public SpielrundeSpielbahn getSpielrundeSpielbahn() {
		return propertiesSpalte.getSpielrundeSpielbahn();
	}

	@Override
	public void setSpielrundeSpielbahn(SpielrundeSpielbahn option) {
		propertiesSpalte.setSpielrundeSpielbahn(option);
	}

	@Override
	public Integer getMaxAnzGespielteSpieltage() {
		return propertiesSpalte.getMaxAnzGespielteSpieltage();
	}

	@Override
	public Integer getMaxAnzSpielerInSpalte() {
		return propertiesSpalte.getMaxAnzSpielerInSpalte();
	}

	@Override
	public boolean getSpielrunde1Header() {
		return propertiesSpalte.getSpielrunde1Header();
	}

	@Override
	public SuperMeleeMode getSuperMeleeMode() {
		return propertiesSpalte.getSuperMeleeMode();
	}

	@Override
	public boolean getSpielrundePlan() {
		return propertiesSpalte.getSpielrundePlan();
	}

	@Override
	public boolean getSetzPositionenAktiv() {
		return propertiesSpalte.getSetzPositionenAktiv();
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return this;
	}

	@Override
	public ISuperMeleePropertiesSpalte getPropertiesSpalte() {
		return propertiesSpalte;
	}

	@Override
	public final SpielTagNr getAktiveSpieltag() {
		return getPropertiesSpalte().getAktiveSpieltag();
	}

	@Override
	public final void setAktiveSpieltag(SpielTagNr spieltag) {
		getPropertiesSpalte().setAktiveSpieltag(spieltag);
	}

	@Override
	public final SpielRundeNr getAktiveSpielRunde() {
		return getPropertiesSpalte().getAktiveSpielRunde();
	}

	@Override
	public boolean getGleichePaarungenAktiv() {
		return getPropertiesSpalte().getGleichePaarungenAktiv();
	}

	@Override
	public final void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) {
		getPropertiesSpalte().setAktiveSpielRunde(neueSpielrunde);
	}

	@Override
	public final SuprMleEndranglisteSortMode getSuprMleEndranglisteSortMode() {
		return getPropertiesSpalte().getSuprMleEndranglisteSortMode();
	}

	@Override
	protected void initPageStylesTurnierSystem() {
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

	@Override
	public SpielrundeHintergrundFarbeGeradeStyle getSpielRundeHintergrundFarbeGeradeStyle() {
		return getPropertiesSpalte().getSpielRundeHintergrundFarbeGeradeStyle();
	}

	@Override
	public SpielrundeHintergrundFarbeUnGeradeStyle getSpielRundeHintergrundFarbeUnGeradeStyle() {
		return getPropertiesSpalte().getSpielRundeHintergrundFarbeUnGeradeStyle();
	}

}
