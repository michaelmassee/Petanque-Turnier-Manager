/**
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config.color;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;

import javax.swing.JColorChooser;
import javax.swing.JFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
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

	public BackgrnColorConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<Integer> configProperty, WorkingSpreadsheet workingSpreadsheet) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		labelPlusBackgrColorAndColorChooser = LabelPlusBackgrColorAndColorChooser.from(guiFactoryCreateParam).labelText(configProperty.getKey())
				.helpText(configProperty.getDescription()).addXActionListener(btnXActionListener).color(getPropertyValue());
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
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// btn Klicked
			try {
				Color color = new Color(getPropertyValue());
				JFrame frame = ProcessBox.from().moveInsideTopWindow().toFront().getFrame();
				Color newColor = JColorChooser.showDialog(frame, configProperty.getKey(), color);
				if (newColor != null) {
					// farbe ausgewaehlt
					int red = newColor.getRed();
					int green = newColor.getGreen();
					int blue = newColor.getBlue();
					String hex = String.format("%02x%02x%02x", red, green, blue);
					int rgbColor = Integer.valueOf(hex, 16);
					setPropertyValue(rgbColor);
					labelPlusBackgrColorAndColorChooser.color(rgbColor);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	};

}
