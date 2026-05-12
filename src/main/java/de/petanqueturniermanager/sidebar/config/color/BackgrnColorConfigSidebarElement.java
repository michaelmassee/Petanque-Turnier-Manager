/*
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config.color;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.farbe.FarbwahlDialog;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.config.ConfigSidebarElement;
import de.petanqueturniermanager.sidebar.fields.LabelPlusBackgrColorAndColorChooser;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public class BackgrnColorConfigSidebarElement implements ConfigSidebarElement {

	static final Logger logger = LogManager.getLogger(BackgrnColorConfigSidebarElement.class);

	LabelPlusBackgrColorAndColorChooser labelPlusBackgrColorAndColorChooser;
	ConfigProperty<?> configProperty;
	WorkingSpreadsheet workingSpreadsheet;
	/** Peer des aufrufenden Fensters (Sidebar oder modaler Konfig-Dialog). Wird als
	 *  Parent für den UNO-ColorPicker benötigt — sonst öffnet er hinter dem Dialog. */
	XWindowPeer parentPeer;

	public BackgrnColorConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<Integer> configProperty, WorkingSpreadsheet workingSpreadsheet) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		this.parentPeer = guiFactoryCreateParam.getWindowPeer();
		var labelText = configProperty.getDescription() != null ? configProperty.getDescription() : configProperty.getKey();
		labelPlusBackgrColorAndColorChooser = LabelPlusBackgrColorAndColorChooser.from(guiFactoryCreateParam).labelText(labelText)
				.helpText(labelText).addXActionListener(btnXActionListener).color(getPropertyValue());
	}

	@Override
	public Layout getLayout() {
		return labelPlusBackgrColorAndColorChooser.getLayout();
	}

	void setPropertyValue(int newVal) {
		if (getPropertyValue() == newVal) {
			return; // nichts zu tun
		}

		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		docPropHelper.setIntProperty(configProperty.getKey(), newVal);
	}

	int getPropertyValue() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		return docPropHelper.getIntProperty(configProperty.getKey(), (Integer) configProperty.getDefaultVal());
	}

	XActionListener btnXActionListener = new XActionListener() {

		@Override
		public void disposing(EventObject arg0) {

			workingSpreadsheet = null;
			configProperty = null;
			labelPlusBackgrColorAndColorChooser = null;
			parentPeer = null;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (workingSpreadsheet == null) {
				return;
			}
			var ergebnis = FarbwahlDialog.waehle(workingSpreadsheet.getxContext(), parentPeer,
					getPropertyValue());
			if (ergebnis.isEmpty()) {
				return;
			}
			int rgbColor = ergebnis.getAsInt();
			setPropertyValue(rgbColor);
			if (labelPlusBackgrColorAndColorChooser != null) {
				labelPlusBackgrColorAndColorChooser.color(rgbColor);
			}
		}
	};

}
