/*
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties.element;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.Selection;
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

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.pagestyle.HeaderFooterPlatzhalter;
import de.petanqueturniermanager.konfigdialog.dialog.element.UITextAreaProperty;

/**
 * wird im Konfigurations-Dialog von StringConfigElement verwendet um text zu editieren
 *
 * @author Michael Massee
 *
 */
public class TextAreaDialog {

	private static final int DIALOG_HEIGHT = 100;
	private static final int DIALOG_WIDTH = 250;
	private static final int PLATZHALTER_BUTTON_ZEILE_HOEHE = 16;

	final WorkingSpreadsheet currentSpreadsheet;
	UITextAreaProperty uITextAreaProperty;
	private boolean platzhalterButtonsAnzeigen;

	public TextAreaDialog(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
	}

	public void initTextArea(String propName, String label, String defaultVal) {
		initTextArea(propName, label, defaultVal, false);
	}

	/**
	 * @param platzhalterButtonsAnzeigen wenn {@code true}, werden zusätzlich Buttons zum Einfügen der
	 *                                    nativen Kopf-/Fußzeilen-Platzhalter (Datum, Uhrzeit, Seite, ...)
	 *                                    angezeigt (siehe {@link HeaderFooterPlatzhalter})
	 */
	public void initTextArea(String propName, String label, String defaultVal, boolean platzhalterButtonsAnzeigen) {
		this.platzhalterButtonsAnzeigen = platzhalterButtonsAnzeigen;
		uITextAreaProperty = new UITextAreaProperty(propName, label, defaultVal, 60);
		uITextAreaProperty.initDefault(currentSpreadsheet);
	}

	/**
	 * method for creating a dialog at runtime
	 *
	 * @param xContext
	 */
	public void createDialog() throws com.sun.star.uno.Exception {
		DialogAufbau aufbau = baueDialog();
		aufbau.xDialog().execute();
		Lo.qi(XComponent.class, aufbau.dialog()).dispose();
	}

	/**
	 * Baut den Dialog samt aller Controls auf, ohne ihn auszuführen (kein blockierender
	 * {@code execute()}-Aufruf). Trennung von Aufbau und Ausführung, damit die reale
	 * Control-Geometrie in Tests überprüfbar ist (siehe {@code TextAreaDialogLayoutUITest}).
	 */
	DialogAufbau baueDialog() throws com.sun.star.uno.Exception {

		int btnWidth = 50;
		int btnHeight = 14;
		int dialogWidth = platzhalterButtonsAnzeigen ? Math.max(DIALOG_WIDTH, 300) : DIALOG_WIDTH;

		// get the service manager from the component context
		XMultiComponentFactory xMultiComponentFactory = currentSpreadsheet.getxContext().getServiceManager();

		// create the dialog model and set the properties
		Object dialogModel = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel",
				currentSpreadsheet.getxContext());
		XPropertySet xPSetDialog = Lo.qi(XPropertySet.class, dialogModel);

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
		int textAreaPosY = 10;
		uITextAreaProperty.doInsert(dialogModel, xControlCont, textAreaPosY);
		int naechsteY = textAreaPosY + uITextAreaProperty.getHeight();
		// ---------------------------------------------------------------------------------------------------

		if (platzhalterButtonsAnzeigen) {
			naechsteY = erstellePlatzhalterButtons(xMultiServiceFactory, xNameCont, xControlCont, naechsteY, dialogWidth);
		}

		int okCancelPosY = naechsteY + 5;
		int dialogHeight = platzhalterButtonsAnzeigen ? okCancelPosY + btnHeight + 5 : DIALOG_HEIGHT;

		// http://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlDialogModel.html
		xPSetDialog.setPropertyValue("PositionX", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("PositionY", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("Width", Integer.valueOf(dialogWidth));
		xPSetDialog.setPropertyValue("Height", Integer.valueOf(dialogHeight));
		xPSetDialog.setPropertyValue("Moveable", Boolean.TRUE);
		xPSetDialog.setPropertyValue("Sizeable", Boolean.TRUE);
		xPSetDialog.setPropertyValue("Title", "Text bearbeiten");

		// create a Ok button model and set the properties
		{
			String fieldname = "okBtn_FieldName";
			Object okButtonModel = xMultiServiceFactory.createInstance("com.sun.star.awt.UnoControlButtonModel");
			XPropertySet xPSetCancelButton = Lo.qi(XPropertySet.class, okButtonModel);
			xPSetCancelButton.setPropertyValue("PositionX", Integer.valueOf(dialogWidth - btnWidth - 5));
			xPSetCancelButton.setPropertyValue("PositionY", Integer.valueOf(dialogHeight - btnHeight - 5));
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
			xPSetCancelButton.setPropertyValue("PositionX", Integer.valueOf(dialogWidth - (btnWidth * 2) - 10));
			xPSetCancelButton.setPropertyValue("PositionY", Integer.valueOf(dialogHeight - btnHeight - 5));
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

		return new DialogAufbau(dialog, xDialog, xControlCont);
	}

	/**
	 * Fertig aufgebauter, aber noch nicht ausgeführter Dialog.
	 */
	record DialogAufbau(Object dialog, XDialog xDialog, XControlContainer xControlCont) {
	}

	/**
	 * Legt für jeden {@link HeaderFooterPlatzhalter} einen kleinen Button an, der beim Klick
	 * das zugehörige Token an der Cursorposition ins Textfeld einfügt (analog zum nativen
	 * LO-Dialog "Seitenvorlage bearbeiten &gt; Kopf-/Fußzeile").
	 *
	 * @return die nächste freie Y-Position unterhalb der Button-Zeile
	 */
	private int erstellePlatzhalterButtons(XMultiServiceFactory xMultiServiceFactory, XNameContainer xNameCont,
			XControlContainer xControlCont, int posY, int dialogWidth) throws com.sun.star.uno.Exception {

		HeaderFooterPlatzhalter[] platzhalter = HeaderFooterPlatzhalter.values();
		int gap = 2;
		int rand = 5;
		int btnBreite = (dialogWidth - (2 * rand) - ((platzhalter.length - 1) * gap)) / platzhalter.length;

		int posX = rand;
		short tabIndex = 10;
		for (HeaderFooterPlatzhalter einPlatzhalter : platzhalter) {
			String fieldname = "platzhalterBtn_" + einPlatzhalter.name();
			Object buttonModel = xMultiServiceFactory.createInstance("com.sun.star.awt.UnoControlButtonModel");
			XPropertySet xPSetButton = Lo.qi(XPropertySet.class, buttonModel);
			xPSetButton.setPropertyValue("PositionX", Integer.valueOf(posX));
			xPSetButton.setPropertyValue("PositionY", Integer.valueOf(posY));
			xPSetButton.setPropertyValue("Width", Integer.valueOf(btnBreite));
			xPSetButton.setPropertyValue("Height", Integer.valueOf(PLATZHALTER_BUTTON_ZEILE_HOEHE));
			xPSetButton.setPropertyValue("Name", fieldname);
			xPSetButton.setPropertyValue("TabIndex", Short.valueOf(tabIndex++));
			xPSetButton.setPropertyValue("Label", I18n.get(einPlatzhalter.getButtonLabelKey()));
			xNameCont.insertByName(fieldname, buttonModel);

			Object objectButton = xControlCont.getControl(fieldname);
			XButton xButton = Lo.qi(XButton.class, objectButton);
			xButton.addActionListener(new ActionListenerPlatzhalterBtn(einPlatzhalter));

			posX += btnBreite + gap;
		}

		return posY + PLATZHALTER_BUTTON_ZEILE_HOEHE;
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

	/**
	 * Fügt beim Klick das Token des Platzhalters an der aktuellen Cursorposition
	 * im Textfeld ein (ersetzt eine eventuelle Selektion).
	 */
	public class ActionListenerPlatzhalterBtn implements com.sun.star.awt.XActionListener {

		private HeaderFooterPlatzhalter platzhalter;

		public ActionListenerPlatzhalterBtn(HeaderFooterPlatzhalter platzhalter) {
			this.platzhalter = platzhalter;
		}

		@Override
		public void disposing(EventObject eventObject) {
			platzhalter = null;
		}

		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			if (uITextAreaProperty == null || platzhalter == null) {
				return;
			}
			XTextComponent textComponent = uITextAreaProperty.getTextComponent();
			if (textComponent == null) {
				return;
			}
			Selection selection = textComponent.getSelection();
			textComponent.insertText(selection, platzhalter.getToken());
		}
	}

}
