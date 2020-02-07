/**
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.AbstractWindowListener;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.config.StringConfigSidebarElement;
import de.petanqueturniermanager.sidebar.config.headerfooter.HeaderFooterSidebarContent;
import de.petanqueturniermanager.sidebar.layout.Layout;
import de.petanqueturniermanager.sidebar.layout.VerticalLayout;

/**
 * @author Michael Massee
 */
public class KopfFusszeilenDialog {

	static final Logger logger = LogManager.getLogger(KopfFusszeilenDialog.class);

	private static final int DIALOG_MIN_HEIGHT = 30;
	private static final int DIALOG_MAX_HEIGHT = 600;
	private static final int DIALOG_WIDTH = 250;
	private static final int BORDER = 5; // abstand Zum Rand

	WorkingSpreadsheet currentSpreadsheet;

	Layout layout;

	public KopfFusszeilenDialog(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
	}

	/**
	 * method for creating a dialog at runtime
	 *
	 * @param xContext
	 */
	public void createDialog() throws com.sun.star.uno.Exception {

		ProcessBox.from().hide(); // sonst überlapt

		// get the service manager from the component context
		XMultiComponentFactory xMultiComponentFactory = currentSpreadsheet.getxContext().getServiceManager();

		// create the dialog model and set the properties
		Object dialogModel = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", currentSpreadsheet.getxContext());
		XPropertySet xPSetDialog = UnoRuntime.queryInterface(XPropertySet.class, dialogModel);
		// http://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlDialogModel.html
		xPSetDialog.setPropertyValue("PositionX", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("PositionY", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("Width", Integer.valueOf(DIALOG_WIDTH));
		xPSetDialog.setPropertyValue("Height", Integer.valueOf(DIALOG_MIN_HEIGHT));
		xPSetDialog.setPropertyValue("Moveable", Boolean.TRUE);
		xPSetDialog.setPropertyValue("Sizeable", Boolean.TRUE);
		xPSetDialog.setPropertyValue("Title", "Kopf/Fusszeilen");

		// create the dialog control and set the model
		Object dialog = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", currentSpreadsheet.getxContext());
		XControl xControl = UnoRuntime.queryInterface(XControl.class, dialog);
		XControlModel xControlModel = UnoRuntime.queryInterface(XControlModel.class, dialogModel);
		xControl.setModel(xControlModel);

		XDialog xDialog = UnoRuntime.queryInterface(XDialog.class, dialog);

		// create a peer
		Object toolkit = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.Toolkit", currentSpreadsheet.getxContext());
		XToolkit xToolkit = UnoRuntime.queryInterface(XToolkit.class, toolkit);
		XWindow xWindow = UnoRuntime.queryInterface(XWindow.class, xControl);
		xWindow.setVisible(false);
		xControl.createPeer(xToolkit, null);
		XWindowPeer windowPeer = xControl.getPeer();

		// window adapter
		int margin = 2;
		layout = new VerticalLayout(0, margin);
		xWindow.addWindowListener(windowAdapter);

		// Felder hinzufuegen
		GuiFactoryCreateParam guiFactoryCreateParam = new GuiFactoryCreateParam(xMultiComponentFactory, currentSpreadsheet.getxContext(), xToolkit, windowPeer);
		List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(currentSpreadsheet);

		AtomicInteger anzElementen = new AtomicInteger(0);
		if (konfigProperties != null) {
			konfigProperties.stream().filter(HeaderFooterSidebarContent.HEADERFOOTER_FILTER).forEach(konfigprop -> {
				StringConfigSidebarElement stringConfigSidebarElement = new StringConfigSidebarElement(guiFactoryCreateParam, (ConfigProperty<String>) konfigprop,
						currentSpreadsheet);
				layout.addLayout(stringConfigSidebarElement.getLayout(), 1);
				anzElementen.addAndGet(1);
			});
		}

		// set height
		int dialogHeight = Math.min(Math.max(layout.getHeight() / 2, DIALOG_MIN_HEIGHT), DIALOG_MAX_HEIGHT);
		xPSetDialog.setPropertyValue("Height", Integer.valueOf(dialogHeight));

		// execute the dialog
		xDialog.execute();

		// dispose the dialog
		XComponent xComponent = UnoRuntime.queryInterface(XComponent.class, dialog);
		xComponent.dispose();
	}

	protected void doLayout(WindowEvent windowEvent) {
		try {
			if (layout != null) {
				Rectangle posSizeParent = new Rectangle(BORDER, BORDER, windowEvent.Width - (BORDER * 2), windowEvent.Height - (BORDER * 2));
				layout.layout(posSizeParent);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private final AbstractWindowListener windowAdapter = new AbstractWindowListener() {
		@Override
		public void windowResized(WindowEvent windowEvent) {
			doLayout(windowEvent);
		}

		@Override
		public void disposing(EventObject event) {
			currentSpreadsheet = null;
		}
	};

}
