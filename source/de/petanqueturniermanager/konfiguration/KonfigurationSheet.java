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
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

public class KonfigurationSheet extends SheetRunner implements IPropertiesSpalte, ISheet {
	private static final Logger logger = LogManager.getLogger(KonfigurationSheet.class);

	public static final int PROPERTIESSPALTE = 0;
	public static final int ERSTE_ZEILE_PROPERTIES = 1;
	public static final String SHEETNAME = "Konfiguration";
	public static final String SHEET_COLOR = "6bf442";

	private final PropertiesSpalte propertiesSpalte;

	public KonfigurationSheet(XComponentContext xContext) {
		super(xContext);
		this.propertiesSpalte = new PropertiesSpalte(xContext, PROPERTIESSPALTE, ERSTE_ZEILE_PROPERTIES, this);
		try {
			update();
		} catch (GenerateException e) {
			handleGenerateException(e);
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		getSheetHelper().setActiveSheet(getSheet());
		update();
	}

	void update() throws GenerateException {
		this.propertiesSpalte.updateKonfigBlock();
		this.propertiesSpalte.doFormat();
	}

	@Override
	public SpielTagNr getAktiveSpieltag() throws GenerateException {
		return new SpielTagNr(this.propertiesSpalte.getAktiveSpieltag());
	}

	@Override
	public void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException {
		this.propertiesSpalte.setAktiveSpieltag(spieltag.getNr());
	}

	@Override
	public int getAktiveSpielRunde() throws GenerateException {
		return this.propertiesSpalte.getAktiveSpielRunde();
	}

	@Override
	public void setAktiveSpielRunde(int neueSpielrunde) throws GenerateException {
		this.propertiesSpalte.setAktiveSpielRunde(neueSpielrunde);
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeGerade() throws GenerateException {
		return this.propertiesSpalte.getSpielRundeHintergrundFarbeGerade();
	}

	@Override
	public Integer getSpielRundeHintergrundFarbeUnGerade() throws GenerateException {
		return this.propertiesSpalte.getSpielRundeHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getSpielRundeHeaderFarbe() throws GenerateException {
		return this.propertiesSpalte.getSpielRundeHeaderFarbe();
	}

	@Override
	public Integer getSpielRundeNeuAuslosenAb() throws GenerateException {
		return this.propertiesSpalte.getSpielRundeNeuAuslosenAb();
	}

	@Override
	public Integer getRanglisteHintergrundFarbeGerade() throws GenerateException {
		return this.propertiesSpalte.getRanglisteHintergrundFarbeGerade();
	}

	@Override
	public Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException {
		return this.propertiesSpalte.getRanglisteHintergrundFarbeUnGerade();
	}

	@Override
	public Integer getRanglisteHintergrundFarbe_StreichSpieltag_Gerade() throws GenerateException {
		return this.propertiesSpalte.getRanglisteHintergrundFarbe_StreichSpieltag_Gerade();
	}

	@Override
	public Integer getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade() throws GenerateException {
		return this.propertiesSpalte.getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade();
	}

	@Override
	public Integer getRanglisteHeaderFarbe() throws GenerateException {
		return this.propertiesSpalte.getRanglisteHeaderFarbe();
	}

	@Override
	public Integer getNichtGespielteRundePlus() throws GenerateException {
		return this.propertiesSpalte.getNichtGespielteRundePlus();
	}

	@Override
	public Integer getNichtGespielteRundeMinus() throws GenerateException {
		return this.propertiesSpalte.getNichtGespielteRundeMinus();
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().newIfNotExist(SHEETNAME, DefaultSheetPos.KONFIGURATION, SHEET_COLOR);
	}

	@Override
	public Formation getFormation() throws GenerateException {
		return this.propertiesSpalte.getFormation();

	}

	@Override
	public SpielSystem getSpielSystem() throws GenerateException {
		return this.propertiesSpalte.getSpielSystem();
	}

}
