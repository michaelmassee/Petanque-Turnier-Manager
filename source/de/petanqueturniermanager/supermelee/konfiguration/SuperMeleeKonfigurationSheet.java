/**
* Erstellung : 06.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.konfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;

import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public class SuperMeleeKonfigurationSheet extends BaseKonfigurationSheet implements ISuperMeleePropertiesSpalte, IKonfigurationSheet {

	private static final Logger logger = LogManager.getLogger(SuperMeleeKonfigurationSheet.class);

	private static final int MAX_SPIELTAG = 10;
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
		getSheet();
	}

	// update immer einmal in SheetRunner
	// wird von #BaseKonfigurationSheet.update() verwendet
	@Override
	protected void updateTurnierSystemKonfiguration() throws GenerateException {
		initSpieltagKonfigSpalten();
	}

	// Spieltag Konfiguration
	private void initSpieltagKonfigSpalten() throws GenerateException {
		// Header
		ColumnProperties columnPropSpieltag = ColumnProperties.from().setWidth(1500);
		StringCellValue header = StringCellValue.from(getSheet()).setPos(Position.from(KONFIG_SPIELTAG_NR_SPALTE, ERSTE_ZEILE_PROPERTIES - 1)).centerHoriJustify()
				.centerVertJustify().setCharWeight(FontWeight.BOLD).setBorder(BasePropertiesSpalte.HEADER_BORDER).setCellBackColor(BasePropertiesSpalte.HEADER_BACK_COLOR);
		getSheetHelper().setTextInCell(header.setValue("Spieltag").setColumnProperties(columnPropSpieltag));
		ColumnProperties columnPropKopfZeile = ColumnProperties.from().setWidth(8000);
		getSheetHelper().setTextInCell(header.setValue("Kopfzeile").spaltePlusEins().setColumnProperties(columnPropKopfZeile));

		// Daten
		StringCellValue nr = StringCellValue.from(getSheet()).setPos(Position.from(KONFIG_SPIELTAG_NR_SPALTE, ERSTE_ZEILE_PROPERTIES)).centerHoriJustify().centerVertJustify()
				.setCharHeight(14).setBorder(BorderFactory.from().allThin().toBorder());
		StringCellValue kopfZeile = StringCellValue.from(getSheet()).setPos(Position.from(KONFIG_SPIELTAG_KOPFZEILE_SPALTE, ERSTE_ZEILE_PROPERTIES)).centerHoriJustify()
				.centerVertJustify().nichtUeberschreiben().setBorder(BorderFactory.from().allThin().toBorder());
		for (int spieltagCntr = 1; spieltagCntr <= MAX_SPIELTAG; spieltagCntr++) {
			getSheetHelper().setTextInCell(nr.setValue("" + spieltagCntr).setEndPosMergeZeilePlus(1));
			nr.zeilePlus(2);
			// Kopfzeile Spalte
			getSheetHelper().setTextInCell(kopfZeile.setValue(spieltagCntr + ". Spieltag").setEndPosMergeZeilePlus(1));
			kopfZeile.zeilePlus(2);
		}
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
		return getSheetHelper().getTextFromCell(getSheet(), posKopfzeile);
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
	public Integer getRanglisteHintergrundFarbeGerade() throws GenerateException {
		return propertiesSpalte.getRanglisteHintergrundFarbeGerade();
	}

	@Override
	public Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException {
		return propertiesSpalte.getRanglisteHintergrundFarbeUnGerade();
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
	public Integer getRanglisteHeaderFarbe() throws GenerateException {
		return propertiesSpalte.getRanglisteHeaderFarbe();
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
		Position posKopfZeile = Position.from(KONFIG_SPIELTAG_KOPFZEILE_SPALTE, ERSTE_ZEILE_PROPERTIES);
		for (int spieltagCntr = 1; spieltagCntr <= MAX_SPIELTAG; spieltagCntr++) {
			// Kopfzeile Spalte
			String kopfZeile = getSheetHelper().getTextFromCell(getSheet(), posKopfZeile);
			PageStyleHelper.from(this, SpielTagNr.from(spieltagCntr)).initDefaultFooter().setFooterCenter(getFusszeileMitte()).setFooterLeft(getFusszeileLinks())
					.setHeaderCenter(kopfZeile).create();
			posKopfZeile.zeilePlus(2);
		}
	}

}
