/**
 * Erstellung 07.02.2020 / Michael Massee
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

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.AbstractWindowListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.config.AddConfigElementsToWindow;
import de.petanqueturniermanager.sidebar.layout.Layout;
import de.petanqueturniermanager.sidebar.layout.VerticalLayout;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
abstract class BasePropertiesDialog {

	private static final Logger logger = LogManager.getLogger(BasePropertiesDialog.class);

	private static final int DIALOG_MIN_HEIGHT = 30;
	private static final int DIALOG_MAX_HEIGHT = 600;
	private static final int DIALOG_WIDTH = 200;
	private static final int BORDER = 5;
	WorkingSpreadsheet currentSpreadsheet;
	Layout layout;

	public BasePropertiesDialog(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
	}

	/**
	 * method for creating a dialog at runtime
	 *
	 * @param xContext
	 */
	public final void createDialog() throws com.sun.star.uno.Exception {

		TurnierSystem turnierSystemAusDocument = new DocumentPropertiesHelper(currentSpreadsheet)
				.getTurnierSystemAusDocument();
		if (turnierSystemAusDocument == TurnierSystem.KEIN) {
			MessageBox.from(currentSpreadsheet.getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption("Kein Turnier vorhanden").message("Kein Turnier vorhanden").show();
			return;
		}

		ProcessBox.from().hide(); // sonst überlapt

		// get the service manager from the component context
		XMultiComponentFactory xMultiComponentFactory = currentSpreadsheet.getxContext().getServiceManager();

		// create the dialog model and set the properties
		Object dialogModel = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel",
				currentSpreadsheet.getxContext());
		XPropertySet xPSetDialog = Lo.qi(XPropertySet.class, dialogModel);
		// http://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlDialogModel.html
		xPSetDialog.setPropertyValue("PositionX", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("PositionY", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("Width", Integer.valueOf(DIALOG_WIDTH));
		xPSetDialog.setPropertyValue("Height", Integer.valueOf(DIALOG_MIN_HEIGHT));
		xPSetDialog.setPropertyValue("Moveable", Boolean.TRUE);
		xPSetDialog.setPropertyValue("Sizeable", Boolean.TRUE);

		String title = turnierSystemAusDocument.getBezeichnung() + "  " + getTitle();
		xPSetDialog.setPropertyValue("Title", title);

		// create the dialog control and set the model
		Object dialog = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialog",
				currentSpreadsheet.getxContext());
		XControl xControl = Lo.qi(XControl.class, dialog);
		XControlModel xControlModel = Lo.qi(XControlModel.class, dialogModel);
		xControl.setModel(xControlModel);

		XDialog xDialog = Lo.qi(XDialog.class, dialog);

		// create a peer
		Object toolkit = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.Toolkit",
				currentSpreadsheet.getxContext());
		XToolkit xToolkit = Lo.qi(XToolkit.class, toolkit);
		XWindow xWindow = Lo.qi(XWindow.class, xControl);
		xWindow.setVisible(false);
		xControl.createPeer(xToolkit, null);
		XWindowPeer windowPeer = xControl.getPeer();

		// window adapter
		int margin = 2;
		layout = new VerticalLayout(0, margin);
		xWindow.addWindowListener(windowAdapter);

		// Felder hinzufuegen
		GuiFactoryCreateParam guiFactoryCreateParam = new GuiFactoryCreateParam(xMultiComponentFactory,
				currentSpreadsheet.getxContext(), xToolkit, windowPeer);
		List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(currentSpreadsheet);

		AddConfigElementsToWindow addConfigElementsToWindow = new AddConfigElementsToWindow(guiFactoryCreateParam,
				currentSpreadsheet, layout);
		AtomicInteger anzElementen = new AtomicInteger(0);
		if (konfigProperties != null) {
			konfigProperties.stream().filter(getKonfigFieldFilter()).forEach(konfigprop -> {
				addConfigElementsToWindow.addPropToPanel(konfigprop);
				anzElementen.addAndGet(1);
			});
		}

		// set height
		int dialogHeight = Math.min(Math.max(layout.getHeight() / 2, DIALOG_MIN_HEIGHT), DIALOG_MAX_HEIGHT);
		xPSetDialog.setPropertyValue("Height", Integer.valueOf(dialogHeight));

		// execute the dialog
		xDialog.execute();

		// dispose the dialog
		XComponent xComponent = Lo.qi(XComponent.class, dialog);
		xComponent.dispose();
	}

	/**
	 * @return
	 */
	protected abstract String getTitle();

	protected final void doLayout(WindowEvent windowEvent) {
		try {
			if (layout != null) {
				Rectangle posSizeParent = new Rectangle(BORDER, BORDER, windowEvent.Width - (BORDER * 2),
						windowEvent.Height - (BORDER * 2));
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
			layout = null;
		}
	};

	protected abstract java.util.function.Predicate<ConfigProperty<?>> getKonfigFieldFilter();

}
