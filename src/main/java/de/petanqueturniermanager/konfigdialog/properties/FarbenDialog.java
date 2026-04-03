/**
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.Rectangle;

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.config.AddConfigElementsToWindow;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;

/**
 * @author Michael Massee
 */
public class FarbenDialog extends BasePropertiesDialog {

	static final Logger logger = LogManager.getLogger(FarbenDialog.class);

	public FarbenDialog(WorkingSpreadsheet currentSpreadsheet) {
		super(currentSpreadsheet);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return konfigprop -> konfigprop.getType() == ConfigPropertyType.COLOR && !konfigprop.isTabFarbe();
	}

	@Override
	protected String getTitle() {
		return I18n.get("dialog.title.farben");
	}

	@Override
	protected void erstelleNachHauptFelder(GuiFactoryCreateParam param,
			AddConfigElementsToWindow addConfigElementsToWindow) throws com.sun.star.uno.Exception {
		List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(currentSpreadsheet);
		if (konfigProperties == null) {
			return;
		}
		var tabFarbProps = konfigProperties.stream()
				.filter(p -> p.getType() == ConfigPropertyType.COLOR && p.isTabFarbe())
				.collect(Collectors.toList());
		if (tabFarbProps.isEmpty()) {
			return;
		}
		var trennlinie = GuiFactory.createHLine(param, new Rectangle(0, 0, 190, 4), null);
		layout.addLayout(new ControlLayout(trennlinie), 1);
		tabFarbProps.forEach(addConfigElementsToWindow::addPropToPanel);
	}

}
