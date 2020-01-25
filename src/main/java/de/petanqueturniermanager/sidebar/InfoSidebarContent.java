/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.OnConfigChangedEvent;
import de.petanqueturniermanager.sidebar.fields.LabelPlusTextReadOnly;
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

	private boolean didAddFields = false;
	private LabelPlusTextReadOnly turnierSystemInfoLine;
	private LabelPlusTextReadOnly spielRundeInfoLine;
	private LabelPlusTextReadOnly spielTagInfoLine;

	/**
	 * Jedes Document eigene Instance
	 *
	 * @param context
	 * @param parentWindow
	 */
	public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow) {
		super(workingSpreadsheet, parentWindow);
		addFields(); // kann hier genacht werden weil die felder sich nicht ändern
	}

	@Override
	protected void addFields() {
		if (didAddFields) {
			return;
		}
		didAddFields = true;

		turnierSystemInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Turniersystem :");
		getLayout().addLayout(turnierSystemInfoLine.getLayout(), 1);

		spielRundeInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Spielrunde :");
		getLayout().addLayout(spielRundeInfoLine.getLayout(), 1);

		spielTagInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Spieltag :");
		getLayout().addLayout(spielTagInfoLine.getLayout(), 1);

		TurnierSystem turnierSystemAusDocument = getTurnierSystemAusDocument();
		turnierSystemInfoLine.fieldText(turnierSystemAusDocument.getBezeichnung());
		if (turnierSystemAusDocument != TurnierSystem.KEIN) {
			// TODO aus doc properties lesen
			spielRundeInfoLine.fieldText(1);
			spielTagInfoLine.fieldText(1);
		}
	}

	@Override
	protected void updateFieldContens(ITurnierEvent eventObj) {
		turnierSystemInfoLine.fieldText(getTurnierSystemAusDocument().getBezeichnung());
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
