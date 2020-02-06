/**
 * Erstellung 08.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.dialog.mainkonfig;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.AbstractWindowListener;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.config.StringConfigSidebarElement;
import de.petanqueturniermanager.sidebar.fields.LabelPlusTextPlusTextareaBox;
import de.petanqueturniermanager.sidebar.layout.Layout;
import de.petanqueturniermanager.sidebar.layout.VerticalLayout;

/**
 * @author Michael Massee
 */
public class TestKonfigDialog {

    static final Logger logger = LogManager.getLogger(TestKonfigDialog.class);

    private static final int DIALOG_HEIGHT = 400;
    private static final int DIALOG_WIDTH = 250;

    final WorkingSpreadsheet currentSpreadsheet;

    Layout layout;

    public TestKonfigDialog(WorkingSpreadsheet currentSpreadsheet) {
        this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
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
        XPropertySet xPSetDialog = UnoRuntime.queryInterface(XPropertySet.class, dialogModel);
        // http://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/UnoControlDialogModel.html
        xPSetDialog.setPropertyValue("PositionX", Integer.valueOf(50));
        xPSetDialog.setPropertyValue("PositionY", Integer.valueOf(50));
        xPSetDialog.setPropertyValue("Width", Integer.valueOf(DIALOG_WIDTH));
        xPSetDialog.setPropertyValue("Height", Integer.valueOf(DIALOG_HEIGHT));
        xPSetDialog.setPropertyValue("Moveable", Boolean.TRUE);
        xPSetDialog.setPropertyValue("Sizeable", Boolean.TRUE);
        xPSetDialog.setPropertyValue("Title", "Spielrunde Info");

        // get the service manager from the dialog model
        XMultiServiceFactory xMultiServiceFactory = UnoRuntime.queryInterface(XMultiServiceFactory.class, dialogModel);
        XNameContainer xNameCont = UnoRuntime.queryInterface(XNameContainer.class, dialogModel);

        // create the dialog control and set the model
        Object dialog = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialog",
                currentSpreadsheet.getxContext());
        XControl xControl = UnoRuntime.queryInterface(XControl.class, dialog);
        XControlModel xControlModel = UnoRuntime.queryInterface(XControlModel.class, dialogModel);
        xControl.setModel(xControlModel);

        XControlContainer xControlCont = UnoRuntime.queryInterface(XControlContainer.class, dialog);
        XDialog xDialog = UnoRuntime.queryInterface(XDialog.class, dialog);

        // ---------------------------------------------------------------------------------------------------
        int posY = 10;
        // ---------------------------------------------------------------------------------------------------

        // create a Ok button model and set the properties
        {
            String fieldname = "okBtn_FieldName";
            int btnWidth = 50;
            int btnHeight = 14;
            Object okButtonModel = xMultiServiceFactory.createInstance("com.sun.star.awt.UnoControlButtonModel");
            XPropertySet xPSetCancelButton = UnoRuntime.queryInterface(XPropertySet.class, okButtonModel);
            xPSetCancelButton.setPropertyValue("PositionX", Integer.valueOf(DIALOG_WIDTH - btnWidth - 5));
            xPSetCancelButton.setPropertyValue("PositionY", Integer.valueOf(DIALOG_HEIGHT - btnHeight - 5));
            xPSetCancelButton.setPropertyValue("Width", Integer.valueOf(btnWidth));
            xPSetCancelButton.setPropertyValue("Height", Integer.valueOf(btnHeight));
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
        Object toolkit = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.Toolkit",
                currentSpreadsheet.getxContext());
        XToolkit xToolkit = UnoRuntime.queryInterface(XToolkit.class, toolkit);
        XWindow xWindow = UnoRuntime.queryInterface(XWindow.class, xControl);
        xWindow.setVisible(false);
        xControl.createPeer(xToolkit, null);
        XWindowPeer windowPeer = xControl.getPeer();

        // window adapter
        layout = new VerticalLayout(0, 2);
        xWindow.addWindowListener(windowAdapter);

        // Felder hinzufuegen
        // XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer
        GuiFactoryCreateParam guiFactoryCreateParam = new GuiFactoryCreateParam(xMultiComponentFactory,
                currentSpreadsheet.getxContext(), xToolkit, windowPeer);

        int lineHeight = 29;
        int lineWidth = 100;
        Rectangle testRect = new Rectangle(0, 0, lineWidth, lineHeight);

        Map<String, Object> props = new HashMap<>();
        XControl createTextfield = GuiFactory.createTextfield(guiFactoryCreateParam, "test4711", null, testRect, props);
        layout.addControl(createTextfield);
        layout.addLayout(LabelPlusTextPlusTextareaBox.from(guiFactoryCreateParam).fieldText("471111111").getLayout(), 1);

        StringConfigSidebarElement stringConfigSidebarElement = new StringConfigSidebarElement();

        // execute the dialog
        xDialog.execute();

        // dispose the dialog
        XComponent xComponent = UnoRuntime.queryInterface(XComponent.class, dialog);
        xComponent.dispose();
    }

    protected void doLayout(WindowEvent windowEvent) {
        try {
            if (layout != null) {
                // Rectangle posSizeParent = parentWindow.getPosSize();
                // Start offset immer 0,0
                Rectangle posSizeParent = new Rectangle(0, 0, windowEvent.Width, windowEvent.Height);
                layout.layout(posSizeParent);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
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

            // Close Dialog
            xDialog.endExecute();
        }
    }

    private final AbstractWindowListener windowAdapter = new AbstractWindowListener() {
        @Override
        public void windowResized(WindowEvent windowEvent) {
            logger.debug("windowResized");
            doLayout(windowEvent);
        }

        @Override
        public void disposing(EventObject event) {
            logger.debug("disposing");
        }
    };

}
