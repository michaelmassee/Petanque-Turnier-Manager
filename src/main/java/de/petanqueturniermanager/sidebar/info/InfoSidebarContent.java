/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.info;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.config.BooleanConfigSidebarElement;
import de.petanqueturniermanager.sidebar.config.IntegerConfigSidebarElement;
import de.petanqueturniermanager.sidebar.fields.LabelPlusTextReadOnly;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public class InfoSidebarContent extends BaseSidebarContent {

	private static final Logger logger = LogManager.getLogger(InfoSidebarContent.class);

	public static final Predicate<ConfigProperty<?>> INFO_PANEL_PROP_FILTER = konfigprop -> konfigprop.isInSideBarInfoPanel();

	private LabelPlusTextReadOnly turnierSystemInfoLine;

	private boolean turnierFields;
	private boolean didAddGlobalFields;

	/**
	 * Jedes Document eigene Instance
	 *
	 * @param context
	 * @param parentWindow
	 */
	public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, xSidebar);
	}

	// nur einmale hinzu fügen
	private void addGlobalFields() {
		if (didAddGlobalFields) {
			return;
		}
		didAddGlobalFields = true;
		setChangingLayout(true);
		try {
			turnierSystemInfoLine = LabelPlusTextReadOnly.from(getGuiFactoryCreateParam()).labelText("Turniersystem").fieldText(getTurnierSystemAusDocument().getBezeichnung());
			getLayout().addLayout(turnierSystemInfoLine.getLayout(), 1);
		} finally {
			setChangingLayout(false);
		}
	}

	// private void updateFieldContens() {
	// updateFieldContens(new OnProperiesChangedEvent(getCurrentSpreadsheet().getWorkingSpreadsheetDocument()));
	// }

	@Override
	protected void updateFieldContens(ITurnierEvent eventObj) {
		if (!turnierFields) {
			addFields();
		}
		turnierSystemInfoLine.fieldText(getTurnierSystemAusDocument().getBezeichnung());
	}

	@Override
	protected void removeAndAddFields() {
		boolean mustLayout = false;
		if (turnierFields) {
			super.removeAllFieldsAndNewBaseWindow();
			mustLayout = true;
			turnierFields = false;
			didAddGlobalFields = false;
		}
		addFields(mustLayout);
	}

	@Override
	protected void addFields() {
		addFields(false);
	}

	// TODO Doppelte code entfernen
	private void addFields(boolean mustLayout) {

		addGlobalFields();
		// Turnier vorhanden ?
		TurnierSystem turnierSystemAusDocument = getTurnierSystemAusDocument();
		if (turnierSystemAusDocument == null || turnierSystemAusDocument == TurnierSystem.KEIN) {
			// kein Turnier
			turnierFields = false;
			if (mustLayout) {
				requestLayout();
			}
			return;
		}

		logger.debug("addFields");

		List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(getCurrentSpreadsheet());
		if (konfigProperties == null) {
			// kein Turnier vorhanden
			return;
		}

		setChangingLayout(true);
		try {
			konfigProperties.stream().filter(INFO_PANEL_PROP_FILTER).collect(Collectors.toList()).forEach(konfigprop -> addPropToPanel(konfigprop));
		} finally {
			setChangingLayout(false);
		}

		// Request layout of the sidebar.
		// Call this method when one of the panels wants to change its size due to late
		// initialization or different content after a context change.
		requestLayout();
		turnierFields = true;
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
					getCurrentSpreadsheet(), true);
			getLayout().addLayout(integerConfigSidebarElement.getLayout(), 1);
			break;
		default:
			break;
		}
	}

	@Override
	protected void disposing(EventObject event) {
		turnierSystemInfoLine = null;
	}

}
