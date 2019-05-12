/**
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfiguration.dialog;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * @author Michael Massee
 *
 */
public class TurnierKonfigDialog {
	private static final Logger logger = LogManager.getLogger(TurnierKonfigDialog.class);

	public static final List<UITextAreaProperty> UITEXTAREAPROPERTY_LIST = new ArrayList<>();
	static {
		int lineCntr = 0;
		UITEXTAREAPROPERTY_LIST.add(new UITextAreaProperty("Turnier Spieltag1 Spielrunde Info", "Spieltag 1 Spielrunde Info", "Spieltag 1 Info", lineCntr++));
		UITEXTAREAPROPERTY_LIST.add(new UITextAreaProperty("Turnier Spieltag2 Spielrunde Info", "Spieltag 2 Spielrunde Info", "Spieltag 2 Info", lineCntr++));
	}

	final WorkingSpreadsheet currentSpreadsheet;

	public TurnierKonfigDialog(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
		initDefaultProperties();
	}

	private void initDefaultProperties() {
		for (UIProperty uIProperty : UITEXTAREAPROPERTY_LIST) {
			uIProperty.initDefault(currentSpreadsheet);
		}
	}

	/**
	 * method for creating a dialog at runtime
	 *
	 * @param xContext
	 */
	public void createDialog() throws com.sun.star.uno.Exception {

		// get the service manager from the component context
		XMultiComponentFactory xMultiComponentFactory = currentSpreadsheet.getxContext().getServiceManager();

		// create the dialog model and set the properties
		Object dialogModel = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", currentSpreadsheet.getxContext());
		XPropertySet xPSetDialog = UnoRuntime.queryInterface(XPropertySet.class, dialogModel);
		// http://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlDialogModel.html
		xPSetDialog.setPropertyValue("PositionX", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("PositionY", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("Width", Integer.valueOf(300));
		xPSetDialog.setPropertyValue("Height", Integer.valueOf(400));
		xPSetDialog.setPropertyValue("Moveable", Boolean.TRUE);
		xPSetDialog.setPropertyValue("Sizeable", Boolean.TRUE);
		xPSetDialog.setPropertyValue("Title", "Turnier Konfiguration");

		// get the service manager from the dialog model
		XMultiServiceFactory xMultiServiceFactory = UnoRuntime.queryInterface(XMultiServiceFactory.class, dialogModel);
		XNameContainer xNameCont = UnoRuntime.queryInterface(XNameContainer.class, dialogModel);

		// create the dialog control and set the model
		Object dialog = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", currentSpreadsheet.getxContext());
		XControl xControl = UnoRuntime.queryInterface(XControl.class, dialog);
		XControlModel xControlModel = UnoRuntime.queryInterface(XControlModel.class, dialogModel);
		xControl.setModel(xControlModel);

		XControlContainer xControlCont = UnoRuntime.queryInterface(XControlContainer.class, dialog);
		XDialog xDialog = UnoRuntime.queryInterface(XDialog.class, dialog);

		// ---------------------------------------------------------------------------------------------------
		for (UIProperty uIProperty : UITEXTAREAPROPERTY_LIST) {
			uIProperty.doInsert(dialogModel, xControlCont);
		}
		// ---------------------------------------------------------------------------------------------------

		// create a Ok button model and set the properties
		{
			String fieldname = "okBtn_FieldName";
			Object okButtonModel = xMultiServiceFactory.createInstance("com.sun.star.awt.UnoControlButtonModel");
			XPropertySet xPSetCancelButton = UnoRuntime.queryInterface(XPropertySet.class, okButtonModel);
			xPSetCancelButton.setPropertyValue("PositionX", Integer.valueOf(80));
			xPSetCancelButton.setPropertyValue("PositionY", Integer.valueOf(70));
			xPSetCancelButton.setPropertyValue("Width", Integer.valueOf(50));
			xPSetCancelButton.setPropertyValue("Height", Integer.valueOf(14));
			xPSetCancelButton.setPropertyValue("Name", fieldname);
			xPSetCancelButton.setPropertyValue("TabIndex", Short.valueOf((short) 2));
			xPSetCancelButton.setPropertyValue("PushButtonType", Short.valueOf((short) PushButtonType.STANDARD.getValue()));
			xPSetCancelButton.setPropertyValue("Label", "Speichern");
			xNameCont.insertByName(fieldname, okButtonModel);

			// add an action listener to the button control
			Object objectButton = xControlCont.getControl(fieldname);
			XButton xButton = UnoRuntime.queryInterface(XButton.class, objectButton);
			xButton.addActionListener(new ActionListenerOkBtn(xDialog));
		}

		// create a peer
		Object toolkit = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.ExtToolkit", currentSpreadsheet.getxContext());
		XToolkit xToolkit = UnoRuntime.queryInterface(XToolkit.class, toolkit);
		XWindow xWindow = UnoRuntime.queryInterface(XWindow.class, xControl);
		xWindow.setVisible(false);
		xControl.createPeer(xToolkit, null);

		// execute the dialog
		xDialog.execute();

		// dispose the dialog
		XComponent xComponent = UnoRuntime.queryInterface(XComponent.class, dialog);
		xComponent.dispose();
	}

	/**
	 * action listener
	 */
	public class ActionListenerOkBtn implements com.sun.star.awt.XActionListener {

		private XDialog xDialog;

		public ActionListenerOkBtn(XDialog xDialog) {
			this.xDialog = xDialog;
		}

		@Override
		public void disposing(EventObject eventObject) {
			xDialog = null;
		}

		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			// save properties
			for (UIProperty uIProperty : UITEXTAREAPROPERTY_LIST) {
				uIProperty.save();
			}
			// Close Dialog
			xDialog.endExecute();
		}
	}

}
