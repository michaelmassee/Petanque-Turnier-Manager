/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.OnConfigChangedEvent;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 * vorlage<br>
 * de.muenchen.allg.itd51.wollmux.sidebar.SeriendruckSidebarContent;
 *
 */
public class InfoSidebarContent extends BaseSidebarContent {

	static final Logger logger = LogManager.getLogger(InfoSidebarContent.class);

	private InfoLine turnierSystemInfoLine;
	private InfoLine spielRundeInfoLine;
	private InfoLine spielTagInfoLine;

	/**
	 * Jedes Document eigene Instance
	 *
	 * @param context
	 * @param parentWindow
	 */
	public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow) {
		super(workingSpreadsheet, parentWindow);

	}

	@Override
	protected void addFields() {
		turnierSystemInfoLine = InfoLine.from(getxMCF(), getCurrentSpreadsheet(), getToolkit(), getWindowPeer()).labelText("Turniersystem :");
		layout.addLayout(turnierSystemInfoLine.getLayout(), 1);

		spielRundeInfoLine = InfoLine.from(getxMCF(), getCurrentSpreadsheet(), getToolkit(), getWindowPeer()).labelText("Spielrunde :");
		layout.addLayout(spielRundeInfoLine.getLayout(), 1);

		spielTagInfoLine = InfoLine.from(getxMCF(), getCurrentSpreadsheet(), getToolkit(), getWindowPeer()).labelText("Spieltag :");
		layout.addLayout(spielTagInfoLine.getLayout(), 1);
	}

	@Override
	protected void initFields() {
		TurnierSystem turnierSystemAusDocument = TurnierSystem.KEIN;
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getCurrentSpreadsheet());
		int spielsystem = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM);
		if (spielsystem > -1) {
			turnierSystemAusDocument = TurnierSystem.findById(spielsystem);
		}
		turnierSystemInfoLine.fieldText(turnierSystemAusDocument.getBezeichnung());
		if (turnierSystemAusDocument != TurnierSystem.KEIN) {
			// TODO wenn Turnier vorhanden Konfig lesen ?
			spielRundeInfoLine.fieldText(1);
			spielTagInfoLine.fieldText(1);
		}
	}

	@Override
	protected void updateFields(ITurnierEvent eventObj) {
		turnierSystemInfoLine.fieldText(((OnConfigChangedEvent) eventObj).getTurnierSystem().getBezeichnung());
		spielRundeInfoLine.fieldText(((OnConfigChangedEvent) eventObj).getSpielRundeNr().getNr());
		spielTagInfoLine.fieldText(((OnConfigChangedEvent) eventObj).getSpieltagnr().getNr());
	}

	@Override
	protected void disposing(EventObject event) {
		turnierSystemInfoLine = null;
		spielRundeInfoLine = null;
		spielTagInfoLine = null;
	}

}
