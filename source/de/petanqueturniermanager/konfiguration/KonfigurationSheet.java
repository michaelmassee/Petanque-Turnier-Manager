/**
* Erstellung : 06.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

public class KonfigurationSheet extends SheetRunner implements IPropertiesSpalte, ISheet {
	private static final int MAX_SPIELTAG = 10;

	private static final Logger logger = LogManager.getLogger(KonfigurationSheet.class);

	public static final int PROPERTIESSPALTE = 0;
	public static final int ERSTE_ZEILE_PROPERTIES = 1;
	public static final String SHEETNAME = "Konfiguration";
	public static final String SHEET_COLOR = "6bf442";

	public static final int KONFIG_SPIELTAG_NR = 3;
	public static final int KONFIG_SPIELTAG_KOPFZEILE = KONFIG_SPIELTAG_NR + 1;

	private final PropertiesSpalte propertiesSpalte;

	public KonfigurationSheet(XComponentContext xContext) {
		super(xContext);
		propertiesSpalte = new PropertiesSpalte(xContext, PROPERTIESSPALTE, ERSTE_ZEILE_PROPERTIES, this);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		getSheetHelper().setActiveSheet(getSheet());
		// update wird immer in SheetRunner aufgerufen
		// update();
	}

	// update immer einmal in SheetRunner
	public void update() throws GenerateException {
		processBoxinfo("Update Konfiguration");
		propertiesSpalte.updateKonfigBlock();
		propertiesSpalte.doFormat();
		initSpieltagKonfigSpalten();
		initPageStyles();

		// anzeige in processBoxinfo
		ProcessBox.from().spielTag(getAktiveSpieltag()).spielRunde(getAktiveSpielRunde());
	}

	/**
	 * Page styles anlegen/updaten
	 *
	 * @throws GenerateException
	 */
	private void initPageStyles() throws GenerateException {
		// default page Style
		PageStyleHelper.from(this, PageStyle.PETTURNMNGR).initDefaultFooter().setFooterCenter(getFusszeileMitte()).setFooterLeft(getFusszeileLinks()).create().applytoSheet();

		Position posKopfZeile = Position.from(KONFIG_SPIELTAG_KOPFZEILE, ERSTE_ZEILE_PROPERTIES);
		for (int spieltagCntr = 1; spieltagCntr <= MAX_SPIELTAG; spieltagCntr++) {
			// Kopfzeile Spalte
			String kopfZeile = getSheetHelper().getTextFromCell(getSheet(), posKopfZeile);
			PageStyleHelper.from(this, SpielTagNr.from(spieltagCntr)).initDefaultFooter().setFooterCenter(getFusszeileMitte()).setFooterLeft(getFusszeileLinks())
					.setHeaderCenter(kopfZeile).create();
			posKopfZeile.zeilePlus(2);
		}
	}

	// Spieltag Konfiguration
	private void initSpieltagKonfigSpalten() throws GenerateException {
		// Header
		CellProperties columnPropSpieltag = CellProperties.from().setWidth(1500);
		StringCellValue header = StringCellValue.from(getSheet()).setPos(Position.from(KONFIG_SPIELTAG_NR, ERSTE_ZEILE_PROPERTIES - 1)).centerHoriJustify().centerVertJustify();
		getSheetHelper().setTextInCell(header.setValue("Spieltag").setColumnProperties(columnPropSpieltag));
		CellProperties columnPropKopfZeile = CellProperties.from().setWidth(8000);
		getSheetHelper().setTextInCell(header.setValue("Kopfzeile").spaltePlusEins().setColumnProperties(columnPropKopfZeile));

		// Daten
		StringCellValue nr = StringCellValue.from(getSheet()).setPos(Position.from(KONFIG_SPIELTAG_NR, ERSTE_ZEILE_PROPERTIES)).centerHoriJustify().centerVertJustify()
				.setCharHeight(14);
		StringCellValue kopfZeile = StringCellValue.from(getSheet()).setPos(Position.from(KONFIG_SPIELTAG_KOPFZEILE, ERSTE_ZEILE_PROPERTIES)).centerHoriJustify()
				.centerVertJustify().nichtUeberschreiben();
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
		Position posKopfzeile = Position.from(KONFIG_SPIELTAG_KOPFZEILE, ERSTE_ZEILE_PROPERTIES + (spielTagNr.getNr() - 1));
		return getSheetHelper().getTextFromCell(getSheet(), posKopfzeile);
	}

	@Override
	public SpielTagNr getAktiveSpieltag() throws GenerateException {
		return propertiesSpalte.getAktiveSpieltag();
	}

	@Override
	public void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException {
		propertiesSpalte.setAktiveSpieltag(spieltag);
	}

	@Override
	public SpielRundeNr getAktiveSpielRunde() throws GenerateException {
		return propertiesSpalte.getAktiveSpielRunde();
	}

	@Override
	public void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException {
		propertiesSpalte.setAktiveSpielRunde(neueSpielrunde);
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
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().newIfNotExist(SHEETNAME, DefaultSheetPos.KONFIGURATION, SHEET_COLOR);
	}

	@Override
	public Formation getFormation() throws GenerateException {
		return propertiesSpalte.getFormation();

	}

	@Override
	public SpielSystem getSpielSystem() throws GenerateException {
		return propertiesSpalte.getSpielSystem();
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
	public String getFusszeileLinks() throws GenerateException {
		return propertiesSpalte.getFusszeileLinks();
	}

	@Override
	public String getFusszeileMitte() throws GenerateException {
		return propertiesSpalte.getFusszeileMitte();
	}
}
