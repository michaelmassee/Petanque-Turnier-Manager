/**
* Erstellung : 06.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

public class KonfigurationSheet extends SheetRunner implements IPropertiesSpalte, ISheet {
	private static final Logger logger = LogManager.getLogger(KonfigurationSheet.class);

	public static final int PROPERTIESSPALTE = 0;
	public static final int ERSTE_ZEILE_PROPERTIES = 1;
	public static final String SHEETNAME = "Konfiguration";
	public static final short SHEET_POS = (short) 99;
	public static final String SHEET_COLOR = "6bf442";

	private final PropertiesSpalte propertiesSpalte;

	public KonfigurationSheet(XComponentContext xContext) {
		super(xContext);
		this.propertiesSpalte = new PropertiesSpalte(xContext, PROPERTIESSPALTE, ERSTE_ZEILE_PROPERTIES, this);
		update();
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		getSheetHelper().setActiveSheet(getSheet());
		update();
	}

	void update() {
		this.propertiesSpalte.updateKonfigBlock();
		this.propertiesSpalte.doFormat();
	}

	@Override
	public int getAktuelleSpieltag() {
		return this.propertiesSpalte.getAktuelleSpieltag();
	}

	@Override
	public void setAktuelleSpieltag(int spieltag) {
		this.propertiesSpalte.setAktuelleSpieltag(spieltag);
	}

	@Override
	public int getAktuelleSpielRunde() {
		return this.propertiesSpalte.getAktuelleSpielRunde();
	}

	@Override
	public void setAktuelleSpielRunde(int neueSpielrunde) {
		this.propertiesSpalte.setAktuelleSpielRunde(neueSpielrunde);
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeGerade() {
		return this.propertiesSpalte.getSpielRundeHintergrundFarbeGerade();
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeUnGerade() {
		return this.propertiesSpalte.getSpielRundeHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getSpielRundeHeaderFarbe() {
		return this.propertiesSpalte.getSpielRundeHeaderFarbe();
	}

	@Override
	public Integer getSpielRundeNeuAuslosenAb() {
		return this.propertiesSpalte.getSpielRundeNeuAuslosenAb();
	}

	@Override
	public Integer getRanglisteHintergrundFarbeGerade() {
		return this.propertiesSpalte.getRanglisteHintergrundFarbeGerade();
	}

	@Override
	public Integer getRanglisteHintergrundFarbeUnGerade() {
		return this.propertiesSpalte.getRanglisteHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getRanglisteHeaderFarbe() {
		return this.propertiesSpalte.getRanglisteHeaderFarbe();
	}

	@Override
	public Integer getNichtGespielteRundePlus() {
		return this.propertiesSpalte.getNichtGespielteRundePlus();
	}

	@Override
	public Integer getNichtGespielteRundeMinus() {
		return this.propertiesSpalte.getNichtGespielteRundeMinus();
	}

	@Override
	public XSpreadsheet getSheet() {
		return getSheetHelper().newIfNotExist(SHEETNAME, SHEET_POS, SHEET_COLOR);
	}

	@Override
	public Formation getFormation() {
		return this.propertiesSpalte.getFormation();

	}

	@Override
	public SpielSystem getSpielSystem() {
		return this.propertiesSpalte.getSpielSystem();
	}
}
