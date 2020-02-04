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
import de.petanqueturniermanager.comp.turnierevent.OnProperiesChangedEvent;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.config.BooleanConfigSidebarElement;
import de.petanqueturniermanager.sidebar.config.IntegerConfigSidebarElement;
import de.petanqueturniermanager.sidebar.fields.LabelPlusTextReadOnly;

/**
 * @author Michael Massee
 *
 * vorlage<br>
 * de.muenchen.allg.itd51.wollmux.sidebar.SeriendruckSidebarContent;
 *
 */
public class InfoSidebarContent extends BaseSidebarContent {

	static final Logger logger = LogManager.getLogger(InfoSidebarContent.class);

	private LabelPlusTextReadOnly turnierSystemInfoLine;
	private LabelPlusTextReadOnly spielRundeInfoLine;
	private LabelPlusTextReadOnly spielTagInfoLine;

	/**
	 * Jedes Document eigene Instance
	 *
	 * @param context
	 * @param parentWindow
	 */
	public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, xSidebar);
	}

	/**
	 * die felder sind immer gleich
	 */
	@Override
	protected void addFields() {
		turnierSystemInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Turniersystem").fieldText(getTurnierSystemAusDocument().getBezeichnung());
		getLayout().addLayout(turnierSystemInfoLine.getLayout(), 1);

		// List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(getCurrentSpreadsheet());
		// if (konfigProperties != null) {
		// konfigProperties.stream().filter(konfigprop -> konfigprop.isInSideBarInfoPanel()).collect(Collectors.toList()).forEach(konfigprop -> addPropToPanel(konfigprop));
		// }

		// spielRundeInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Spielrunde");
		// getLayout().addLayout(spielRundeInfoLine.getLayout(), 1);
		//
		// spielTagInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Spieltag");
		// getLayout().addLayout(spielTagInfoLine.getLayout(), 1);
	}

	/**
	 * Read ONLY Felder
	 *
	 * @param konfigprop
	 * @return
	 */
	private void addPropToPanel(ConfigProperty<?> configProperty) {

		switch (configProperty.getType()) {
		case STRING:
			break;
		case BOOLEAN:
			// create checkbox Readonly
			@SuppressWarnings("unchecked")
			BooleanConfigSidebarElement booleanConfigSidebarElement = new BooleanConfigSidebarElement(getGuiFactoryCreateParam(), (ConfigProperty<Boolean>) configProperty,
					getCurrentSpreadsheet(), true);
			getLayout().addLayout(booleanConfigSidebarElement.getLayout(), 1);
			break;
		case COLOR:
			// create colorpicker
			// TODO ReadOnly
			break;
		case INTEGER:
			@SuppressWarnings("unchecked")
			IntegerConfigSidebarElement integerConfigSidebarElement = new IntegerConfigSidebarElement(getGuiFactoryCreateParam(), (ConfigProperty<Integer>) configProperty,
					getCurrentSpreadsheet());
			getLayout().addLayout(integerConfigSidebarElement.getLayout(), 1);
			break;
		default:
			break;
		}
	}

	@Override
	protected void removeAndAddFields() {
		// wir mussen die felder nicht entfernen, weil die sich nicht ändern
		// nur update
		updateFieldContens();
	}

	private void updateFieldContens() {
		updateFieldContens(new OnProperiesChangedEvent(getCurrentSpreadsheet().getWorkingSpreadsheetDocument()));
	}

	@Override
	protected void updateFieldContens(ITurnierEvent eventObj) {
		turnierSystemInfoLine.fieldText(getTurnierSystemAusDocument().getBezeichnung());
		// TODO Spieltag und Spielrunde aus document properties
		// spielRundeInfoLine.fieldText(((OnConfigChangedEvent) eventObj).getSpielRundeNr().getNr());
		// spielTagInfoLine.fieldText(((OnConfigChangedEvent) eventObj).getSpieltagnr().getNr());
	}

	@Override
	protected void disposing(EventObject event) {
		turnierSystemInfoLine = null;
		spielRundeInfoLine = null;
		spielTagInfoLine = null;
	}

}
