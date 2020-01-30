/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.OnConfigChangedEvent;
import de.petanqueturniermanager.sidebar.fields.LabelPlusTextReadOnly;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * @author Michael Massee
 *
 *         vorlage<br>
 *         de.muenchen.allg.itd51.wollmux.sidebar.SeriendruckSidebarContent;
 *
 */
public class InfoSidebarContent extends BaseSidebarContent {

	static final Logger logger = LogManager.getLogger(InfoSidebarContent.class);

	private LabelPlusTextReadOnly turnierSystemInfoLine;
	private LabelPlusTextReadOnly spielRundeInfoLine;
	private LabelPlusTextReadOnly spielTagInfoLine;
	private boolean didAddFields;

	/**
	 * Jedes Document eigene Instance
	 *
	 * @param context
	 * @param parentWindow
	 */
	public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, xSidebar);
	}

	private void addEmptyFields() {
		if (didAddFields) {
			// only once
			return;
		}
		didAddFields = true;
		turnierSystemInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Turniersystem :");
		getLayout().addLayout(turnierSystemInfoLine.getLayout(), 1);

		spielRundeInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Spielrunde :");
		getLayout().addLayout(spielRundeInfoLine.getLayout(), 1);

		spielTagInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Spieltag :");
		getLayout().addLayout(spielTagInfoLine.getLayout(), 1);
	}

	@Override
	protected void addFields() {
		// wird aufgerufen wenn das eigentliche Document on Top ist
		// TODO Spieltag und Spielrunde aus document properties
		addEmptyFields(); // kann hier genacht werden weil die felder sich nicht ändern
		updateFieldContens(new OnConfigChangedEvent(new SpielTagNr(0), new SpielRundeNr(0), getCurrentSpreadsheet()));
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
