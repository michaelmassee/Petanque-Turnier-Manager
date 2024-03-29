/**
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import static com.google.common.base.Preconditions.checkNotNull;

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

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.konfigdialog.dialog.element.UITextAreaProperty;

/**
 * wird in sidebar von StringConfigSidebarElement verwendet um text zu editieren
 *
 * @author Michael Massee
 *
 */
public class TextAreaDialog {

	private static final int DIALOG_HEIGHT = 100;
	private static final int DIALOG_WIDTH = 250;
	final WorkingSpreadsheet currentSpreadsheet;
	UITextAreaProperty uITextAreaProperty;

	public TextAreaDialog(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
	}

	public void initTextArea(String propName, String label, String defaultVal) {
		uITextAreaProperty = new UITextAreaProperty(propName, label, defaultVal, 60);
		uITextAreaProperty.initDefault(currentSpreadsheet);
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
		Object dialogModel = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel",
				currentSpreadsheet.getxContext());
		XPropertySet xPSetDialog = Lo.qi(XPropertySet.class, dialogModel);
		// http://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlDialogModel.html
		xPSetDialog.setPropertyValue("PositionX", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("PositionY", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("Width", Integer.valueOf(DIALOG_WIDTH));
		xPSetDialog.setPropertyValue("Height", Integer.valueOf(DIALOG_HEIGHT));
		xPSetDialog.setPropertyValue("Moveable", Boolean.TRUE);
		xPSetDialog.setPropertyValue("Sizeable", Boolean.TRUE);
		xPSetDialog.setPropertyValue("Title", "Text bearbeiten");

		// get the service manager from the dialog model
		XMultiServiceFactory xMultiServiceFactory = Lo.qi(XMultiServiceFactory.class, dialogModel);
		XNameContainer xNameCont = Lo.qi(XNameContainer.class, dialogModel);

		// create the dialog control and set the model
		Object dialog = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialog",
				currentSpreadsheet.getxContext());
		XControl xControl = Lo.qi(XControl.class, dialog);
		XControlModel xControlModel = Lo.qi(XControlModel.class, dialogModel);
		xControl.setModel(xControlModel);

		XControlContainer xControlCont = Lo.qi(XControlContainer.class, dialog);
		XDialog xDialog = Lo.qi(XDialog.class, dialog);

		// ---------------------------------------------------------------------------------------------------
		int posY = 10;
		posY += uITextAreaProperty.doInsert(dialogModel, xControlCont, posY);
		// ---------------------------------------------------------------------------------------------------

		// create a Ok button model and set the properties
		int btnWidth = 50;
		int btnHeight = 14;
		{
			String fieldname = "okBtn_FieldName";
			Object okButtonModel = xMultiServiceFactory.createInstance("com.sun.star.awt.UnoControlButtonModel");
			XPropertySet xPSetCancelButton = Lo.qi(XPropertySet.class, okButtonModel);
			xPSetCancelButton.setPropertyValue("PositionX", Integer.valueOf(DIALOG_WIDTH - btnWidth - 5));
			xPSetCancelButton.setPropertyValue("PositionY", Integer.valueOf(DIALOG_HEIGHT - btnHeight - 5));
			xPSetCancelButton.setPropertyValue("Width", Integer.valueOf(btnWidth));
			xPSetCancelButton.setPropertyValue("Height", Integer.valueOf(btnHeight));
			xPSetCancelButton.setPropertyValue("Name", fieldname);
			xPSetCancelButton.setPropertyValue("TabIndex", Short.valueOf((short) 2));
			xPSetCancelButton.setPropertyValue("PushButtonType",
					Short.valueOf((short) PushButtonType.STANDARD.getValue()));
			xPSetCancelButton.setPropertyValue("Label", "Speichern");
			xNameCont.insertByName(fieldname, okButtonModel);

			// add an action listener to the button control
			Object objectButton = xControlCont.getControl(fieldname);
			XButton xButton = Lo.qi(XButton.class, objectButton);
			xButton.addActionListener(new ActionListenerOkBtn(xDialog));
		}

		// create a Cancel button model and set the properties
		{
			String fieldname = "cancelBtn_FieldName";
			Object cancelButtonModel = xMultiServiceFactory.createInstance("com.sun.star.awt.UnoControlButtonModel");
			XPropertySet xPSetCancelButton = Lo.qi(XPropertySet.class, cancelButtonModel);
			// https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlButtonModel.html
			xPSetCancelButton.setPropertyValue("PositionX", Integer.valueOf(DIALOG_WIDTH - (btnWidth * 2) - 10));
			xPSetCancelButton.setPropertyValue("PositionY", Integer.valueOf(DIALOG_HEIGHT - btnHeight - 5));
			xPSetCancelButton.setPropertyValue("Width", Integer.valueOf(btnWidth));
			xPSetCancelButton.setPropertyValue("Height", Integer.valueOf(btnHeight));
			xPSetCancelButton.setPropertyValue("Name", fieldname);
			xPSetCancelButton.setPropertyValue("TabIndex", Short.valueOf((short) 3));
			xPSetCancelButton.setPropertyValue("PushButtonType",
					Short.valueOf((short) PushButtonType.STANDARD.getValue()));
			xPSetCancelButton.setPropertyValue("DefaultButton", true);
			xPSetCancelButton.setPropertyValue("Label", "Abbruch");
			xNameCont.insertByName(fieldname, cancelButtonModel);

			// add an action listener to the button control
			Object objectButton = xControlCont.getControl(fieldname);
			XButton xButton = Lo.qi(XButton.class, objectButton);
			xButton.addActionListener(new ActionListenerAbbruchBtn(xDialog));
		}

		// create a peer
		Object toolkit = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.Toolkit",
				currentSpreadsheet.getxContext());
		XToolkit xToolkit = Lo.qi(XToolkit.class, toolkit);
		XWindow xWindow = Lo.qi(XWindow.class, xControl);
		xWindow.setVisible(false);
		xControl.createPeer(xToolkit, null);

		// execute the dialog
		xDialog.execute();

		// dispose the dialog
		XComponent xComponent = Lo.qi(XComponent.class, dialog);
		xComponent.dispose();
	}

	/**
	 * action listener
	 */
	public class ActionListenerAbbruchBtn implements com.sun.star.awt.XActionListener {
		private XDialog xDialog;

		public ActionListenerAbbruchBtn(XDialog xDialog) {
			this.xDialog = xDialog;
		}

		@Override
		public void disposing(EventObject eventObject) {
			xDialog = null;
		}

		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			// Close Dialog
			xDialog.endExecute();
		}

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
			if (uITextAreaProperty != null) {
				uITextAreaProperty.save();
			}
			// Close Dialog
			xDialog.endExecute();
		}
	}

}
