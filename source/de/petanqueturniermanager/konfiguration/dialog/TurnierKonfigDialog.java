/**
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfiguration.dialog;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfiguration.DocumentPropertiesHelper;

/**
 * @author Michael Massee
 *
 */
public class TurnierKonfigDialog {
	private static final Logger logger = LogManager.getLogger(TurnierKonfigDialog.class);

	public static final String PROP_TURNIER_SPIELTAG1_INFO = "Turnier Spieltag1 Spielrunde Info";
	public static final String FIELDNAME_SPIELTAG1_INFO = "spieltag1Info_FieldName";

	final DocumentPropertiesHelper documentPropertiesHelper;
	final WorkingSpreadsheet currentSpreadsheet;

	public TurnierKonfigDialog(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
		documentPropertiesHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		initDefaultProperties();
	}

	private void initDefaultProperties() {
		documentPropertiesHelper.insertStringPropertyIfNotExist(PROP_TURNIER_SPIELTAG1_INFO, "test");
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
		xPSetDialog.setPropertyValue("PositionX", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("PositionY", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("Width", Integer.valueOf(300));
		xPSetDialog.setPropertyValue("Height", Integer.valueOf(100));
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

		// create the label model and set the properties
		{
			String fieldname = "label_spieltag1_info_FieldName";
			Object labelModel = xMultiServiceFactory.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
			XPropertySet xPSetLabel = UnoRuntime.queryInterface(XPropertySet.class, labelModel);
			xPSetLabel.setPropertyValue("PositionX", Integer.valueOf(3));
			xPSetLabel.setPropertyValue("PositionY", Integer.valueOf(10));
			xPSetLabel.setPropertyValue("Width", Integer.valueOf(100));
			xPSetLabel.setPropertyValue("Height", Integer.valueOf(14));
			xPSetLabel.setPropertyValue("Name", fieldname);
			xPSetLabel.setPropertyValue("TabIndex", Short.valueOf((short) 1));
			xPSetLabel.setPropertyValue("Label", PROP_TURNIER_SPIELTAG1_INFO);
			xNameCont.insertByName(fieldname, labelModel);
		}

		{
			String spieltag1Info = documentPropertiesHelper.getStringProperty(PROP_TURNIER_SPIELTAG1_INFO);
			createMultiLineTextArea(xNameCont, xMultiServiceFactory, spieltag1Info, FIELDNAME_SPIELTAG1_INFO, 100, 10, 200, 50);
		}

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
			xPSetCancelButton.setPropertyValue("PushButtonType", Short.valueOf((short) PushButtonType.STANDARD.getValue())); // Short.valueOf((short) 2)); // PushButtonType
			xPSetCancelButton.setPropertyValue("Label", "Speichern");
			xNameCont.insertByName(fieldname, okButtonModel);

			// add an action listener to the button control
			Object objectButton = xControlCont.getControl(fieldname);
			XButton xButton = UnoRuntime.queryInterface(XButton.class, objectButton);
			xButton.addActionListener(new ActionListenerOkBtn(xControlCont, documentPropertiesHelper, xDialog));
		}

		// // create the button model and set the properties
		// Object buttonModel = xMultiServiceFactory.createInstance("com.sun.star.awt.UnoControlButtonModel");
		// XPropertySet xPSetButton = UnoRuntime.queryInterface(XPropertySet.class, buttonModel);
		// xPSetButton.setPropertyValue("PositionX", Integer.valueOf(10));
		// xPSetButton.setPropertyValue("PositionY", Integer.valueOf(70));
		// xPSetButton.setPropertyValue("Width", Integer.valueOf(50));
		// xPSetButton.setPropertyValue("Height", Integer.valueOf(14));
		// xPSetButton.setPropertyValue("Name", _buttonName);
		// xPSetButton.setPropertyValue("TabIndex", Short.valueOf((short) 0));
		// xPSetButton.setPropertyValue("Label", "Click Me");

		// insert the control models into the dialog model
		// xNameCont.insertByName(_buttonName, buttonModel);
		// xNameCont.insertByName(_labelName, labelModel);
		// xNameCont.insertByName(_cancelButtonName, cancelButtonModel);

		// create the dialog control and set the model
		// Object dialog = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", currentSpreadsheet.getxContext());
		// XControl xControl = UnoRuntime.queryInterface(XControl.class, dialog);
		// XControlModel xControlModel = UnoRuntime.queryInterface(XControlModel.class, dialogModel);
		// xControl.setModel(xControlModel);

		// add an action listener to the button control
		// XControlContainer xControlCont = UnoRuntime.queryInterface(XControlContainer.class, dialog);
		// Object objectButton = xControlCont.getControl("Button1");
		// XButton xButton = UnoRuntime.queryInterface(XButton.class, objectButton);
		// xButton.addActionListener(new ActionListenerImpl(xControlCont));

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

	// /**
	// * Create a textarea with given content.
	// * @param content
	// * @param name
	// * @param x
	// * @param y
	// * @param width
	// * @param height
	// * @return a multiline XTextArea
	// * @throws Exception
	// */
	// public XTextComponent addTextArea(String content,String name,int x,int y,int width,int height) throws Exception {

	private void createMultiLineTextArea(XNameContainer xNameCont, XMultiServiceFactory xMultiServiceFactory, String content, String name, int x, int y, int width, int height)
			throws Exception {

		Object labelModel = xMultiServiceFactory.createInstance("com.sun.star.awt.UnoControlEditModel");
		XPropertySet textAreaProperties = UnoRuntime.queryInterface(XPropertySet.class, labelModel);
		textAreaProperties.setPropertyValue("PositionX", new Integer(x));
		textAreaProperties.setPropertyValue("PositionY", new Integer(y));
		textAreaProperties.setPropertyValue("Width", new Integer(width));
		textAreaProperties.setPropertyValue("Height", new Integer(height));
		textAreaProperties.setPropertyValue("Name", name);
		// textAreaProperties.setPropertyValue("TabIndex", new Short((short) tabcount++));
		textAreaProperties.setPropertyValue("MultiLine", new Boolean(true));
		textAreaProperties.setPropertyValue("HScroll", new Boolean(true));
		textAreaProperties.setPropertyValue("VScroll", new Boolean(true));
		textAreaProperties.setPropertyValue("Text", content);

		xNameCont.insertByName(name, labelModel);
		// XControlContainer controlContainer = (XControlContainer) UnoRuntime.queryInterface(XControlContainer.class, oUnoDialog);
		// Object obj = controlContainer.getControl(name);
		// XTextComponent field = UnoRuntime.queryInterface(XTextComponent.class, obj);
		// return field;
		// }

	}

	/**
	 * action listener
	 */
	public class ActionListenerOkBtn implements com.sun.star.awt.XActionListener {

		private XControlContainer xControlCont;
		private DocumentPropertiesHelper documentPropertiesHelper;
		private XDialog xDialog;

		public ActionListenerOkBtn(XControlContainer xControlCont, DocumentPropertiesHelper documentPropertiesHelper, XDialog xDialog) {
			this.xControlCont = xControlCont;
			this.documentPropertiesHelper = documentPropertiesHelper;
			this.xDialog = xDialog;
		}

		@Override
		public void disposing(EventObject eventObject) {
			xControlCont = null;
			documentPropertiesHelper = null;
			xDialog = null;
		}

		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			// save properties
			Object propSpieltag = xControlCont.getControl(FIELDNAME_SPIELTAG1_INFO);
			XTextComponent spielTagfield = UnoRuntime.queryInterface(XTextComponent.class, propSpieltag);
			documentPropertiesHelper.setStringProperty(PROP_TURNIER_SPIELTAG1_INFO, spielTagfield.getText());

			// Close Dialog
			xDialog.endExecute();
		}
	}

}
