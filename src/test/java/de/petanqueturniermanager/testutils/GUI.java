package de.petanqueturniermanager.testutils;

// GUI.java
// Andrew Davison, ad@fivedots.coe.psu.ac.th, August 2016

/* A growing collection of utility functions to make Office
   easier to use. They are currently divided into the following
   groups:

     * toolbar addition
     * floating frame, message box
     * controller and frame
     * Office container window
     * zooming
     * UI config manager
     * layout manager
     * menu bar
*/
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.UIManager;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.accessibility.XAccessibleContext;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.VclWindowPeerAttribute;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XExtendedToolkit;
import com.sun.star.awt.XMenuBar;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XSystemDependentWindowPeer;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XTopWindow;
import com.sun.star.awt.XUserInputInterception;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XIndexContainer;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XDispatchProviderInterception;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFrames;
import com.sun.star.frame.XFramesSupplier;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.graphic.XGraphic;
import com.sun.star.lang.SystemDependent;
import com.sun.star.lang.XComponent;
import com.sun.star.ui.UIElementType;
import com.sun.star.ui.XImageManager;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;
import com.sun.star.ui.XUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIElement;
import com.sun.star.uno.Exception;
import com.sun.star.view.DocumentZoomType;
import com.sun.star.view.XControlAccess;
import com.sun.star.view.XSelectionSupplier;

public class GUI {
	// view settings zoom constants
	public static final short OPTIMAL = DocumentZoomType.OPTIMAL;
	public static final short PAGE_WIDTH = DocumentZoomType.PAGE_WIDTH;
	public static final short ENTIRE_PAGE = DocumentZoomType.ENTIRE_PAGE;

	public static final String MENU_BAR = "private:resource/menubar/menubar";
	public static final String STATUS_BAR = "private:resource/statusbar/statusbar";
	public static final String FIND_BAR = "private:resource/toolbar/findbar";
	public static final String STANDARD_BAR = "private:resource/toolbar/standardbar";
	public static final String TOOL_BAR = "private:resource/toolbar/toolbar";

	// in "XCU" files in <OFFICE>\officecfg\registry\data\org\openoffice\Office\UI (v.4.3.2.2)
	// add "private:resource/toolbar/" for full resource name
	private static final String[] TOOBAR_NMS = { "3dobjectsbar", "addon_LibreLogo.OfficeToolBar", "alignmentbar",
			"arrowsbar", "arrowshapes", "basicshapes", "bezierobjectbar", "calloutshapes", "changes", "choosemodebar",
			"colorbar", "commentsbar", "commontaskbar", "connectorsbar", "custom_toolbar_1", "datastreams",
			"designobjectbar", "dialogbar", "drawbar", "drawingobjectbar", "drawobjectbar", "drawtextobjectbar",
			"ellipsesbar", "extrusionobjectbar", "findbar", "flowchartshapes", "fontworkobjectbar", "fontworkshapetype",
			"formatobjectbar", "Formatting", "formcontrols", "formcontrolsbar", "formdesign", "formobjectbar",
			"formsfilterbar", "formsnavigationbar", "formtextobjectbar", "frameobjectbar", "fullscreenbar",
			"gluepointsobjectbar", "graffilterbar", "graphicobjectbar", "insertbar", "insertcellsbar",
			"insertcontrolsbar", "insertobjectbar", "linesbar", "macrobar", "masterviewtoolbar", "mediaobjectbar",
			"moreformcontrols", "navigationobjectbar", "numobjectbar", "oleobjectbar", "optimizetablebar", "optionsbar",
			"outlinetoolbar", "positionbar", "previewbar", "previewobjectbar", "queryobjectbar", "rectanglesbar",
			"reportcontrols", "reportobjectbar", "resizebar", "sectionalignmentbar", "sectionshrinkbar",
			"slideviewobjectbar", "slideviewtoolbar", "sqlobjectbar", "standardbar", "starshapes", "symbolshapes",
			"tableobjectbar", "textbar", "textobjectbar", "toolbar", "translationbar", "viewerbar", "zoombar", };

	// ---------------- toolbar addition ------------------

	public static String getToobarResource(String nm) {
		for (String resNm : TOOBAR_NMS)
			if (resNm.contains(nm)) {
				String resource = "private:resource/toolbar/" + resNm;
				System.out.println("Matched " + nm + " to " + resource);
				return resource;
			}

		System.out.println("No matching resource for " + nm);
		return null;
	} // end of getToobarResource()

	public static void addItemToToolbar(XComponent doc, String toolbarName, String itemName, String imFnm)
	/*
	 * Add a user-defined icon and command to the start of the specified toolbar. Based on http://www.oracle.com/technetwork/java/javase/ downloads/menus-toolbars-139540.html
	 * 
	 */
	{
		String cmd = Lo.makeUnoCmd(itemName);
		// System.out.println("Cmd: " + cmd);

		XUIConfigurationManager confMan = GUI.getUIConfigManagerDoc(doc);
		if (confMan == null) {
			System.out.println("Cannot create configuration manager");
			return;
		}

		try {
			// GUI.printUICmds(confMan, toolbarName);

			// add icon image to Office and to toolbar
			XImageManager imageMan = Lo.qi(XImageManager.class, confMan.getImageManager());
			String[] cmds = { cmd };
			XGraphic[] pics = new XGraphic[1];
			pics[0] = Images.loadGraphicFile(imFnm);
			imageMan.insertImages((short) 0, cmds, pics);

			// add item to toolbar
			XIndexAccess settings = confMan.getSettings(toolbarName, true);
			// int numSettings = settings.getCount();

			XIndexContainer conSettings = Lo.qi(XIndexContainer.class, settings);
			PropertyValue[] itemProps = Props.makeBarItem(cmd, itemName);
			conSettings.insertByIndex(0, itemProps); // numSettings
			confMan.replaceSettings(toolbarName, conSettings);
		} catch (java.lang.Exception e) {
			System.out.println(e);
		}
	} // end of addItemToToolbar()

	// --------------------- floating frame, message box ---------------------

	public static XFrame createFloatingFrame(String title, int x, int y, int width, int height)
	// create a floating XFrame at the given position and size
	{
		XToolkit xToolkit = Lo.createInstanceMCF(XToolkit.class, "com.sun.star.awt.Toolkit");
		if (xToolkit == null)
			return null;

		WindowDescriptor desc = new WindowDescriptor();
		desc.Type = WindowClass.TOP;
		desc.WindowServiceName = "modelessdialog";
		desc.ParentIndex = -1;

		desc.Bounds = new Rectangle(x, y, width, height);
		desc.WindowAttributes = WindowAttribute.BORDER + WindowAttribute.MOVEABLE + WindowAttribute.CLOSEABLE
				+ WindowAttribute.SIZEABLE + VclWindowPeerAttribute.CLIPCHILDREN;

		XWindowPeer xWindowPeer = xToolkit.createWindow(desc);
		XWindow window = Lo.qi(XWindow.class, xWindowPeer);

		XFrame xFrame = Lo.createInstanceMCF(XFrame.class, "com.sun.star.frame.Frame");
		if (xFrame == null) {
			System.out.println("Could not create frame");
			return null;
		}

		xFrame.setName(title);
		xFrame.initialize(window);

		XFramesSupplier xFramesSup = Lo.qi(XFramesSupplier.class, Lo.getDesktop());
		XFrames xFrames = xFramesSup.getFrames();
		if (xFrames == null)
			System.out.println("Mo desktop frames found");
		else
			xFrames.append(xFrame);

		window.setVisible(true);
		return xFrame;
	} // end of createFloatingFrame()

	public static void showMessageBox(String title, String message) {
		XToolkit xToolkit = Lo.createInstanceMCF(XToolkit.class, "com.sun.star.awt.Toolkit");
		XWindow xWindow = getWindow();
		if ((xToolkit == null) || (xWindow == null))
			return;

		XWindowPeer xPeer = Lo.qi(XWindowPeer.class, xWindow);

		// initialize message box window description
		WindowDescriptor desc = new WindowDescriptor();
		desc.Type = WindowClass.MODALTOP;
		desc.WindowServiceName = new String("infobox");
		desc.ParentIndex = -1;
		desc.Parent = xPeer;
		desc.Bounds = new Rectangle(0, 0, 300, 200);
		desc.WindowAttributes = WindowAttribute.BORDER | WindowAttribute.MOVEABLE | WindowAttribute.CLOSEABLE;

		XWindowPeer descPeer = xToolkit.createWindow(desc);
		if (descPeer != null) {
			XMessageBox msgBox = Lo.qi(XMessageBox.class, descPeer);
			if (msgBox != null) {
				msgBox.setCaptionText(title);
				msgBox.setMessageText(message);
				msgBox.execute();
			}
		}
	} // end of showMessageBox()

	public static void showJMessageBox(String title, String message) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
	} // end of showJMessageBox()

	public static String getPassword(String title, String inputMsg) {
		JLabel jl = new JLabel(inputMsg);
		JPasswordField jpf = new JPasswordField(24);
		Object[] ob = { jl, jpf };
		int result = JOptionPane.showConfirmDialog(null, ob, title, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION)
			return new String(jpf.getPassword());
		else
			return null;
	} // end of getPassword()

	// -------------------------- controller and frame -------------

	public static XController getCurrentController(Object odoc)
	// was XComponent
	{
		XComponent doc = Lo.qi(XComponent.class, odoc);
		XModel model = Lo.qi(XModel.class, doc);
		if (model == null) {
			System.out.println("Document has no data model");
			return null;
		}
		return model.getCurrentController();
	}

	public static XFrame getFrame(XComponent doc) {
		return getCurrentController(doc).getFrame();
	}

	public static XControlAccess getControlAccess(XComponent doc) {
		return Lo.qi(XControlAccess.class, getCurrentController(doc));
	}

	public static XUserInputInterception getUII(XComponent doc) {
		return Lo.qi(XUserInputInterception.class, getCurrentController(doc));
	}

	public static XSelectionSupplier getSelectionSupplier(Object odoc)
	// was XComponent
	{
		XComponent doc = Lo.qi(XComponent.class, odoc);
		return Lo.qi(XSelectionSupplier.class, getCurrentController(doc));
	}

	public static XDispatchProviderInterception getDPI(XComponent doc) {
		return Lo.qi(XDispatchProviderInterception.class, getFrame(doc));
	}

	// -------------------------- Office container window -------------

	public static XWindow getWindow() {
		XDesktop desktop = Lo.getDesktop();
		XFrame frame = desktop.getCurrentFrame();
		if (frame == null) {
			System.out.println("No current frame");
			return null;
		} else
			return frame.getContainerWindow();
	} // end of getWindow()

	public static XWindow getWindow(XComponent doc) {
		return getCurrentController(doc).getFrame().getContainerWindow();
	}

	public static void setVisible(Object objDoc, boolean isVisible)
	// was XComponent
	{
		XComponent doc = Lo.qi(XComponent.class, objDoc);
		XWindow xWindow = getFrame(doc).getContainerWindow();
		xWindow.setVisible(isVisible);
		xWindow.setFocus();
	} // end of setVisible()

	public static void setVisible(boolean isVisible) {
		XWindow xWindow = getWindow();
		if (xWindow != null) {
			xWindow.setVisible(isVisible);
			xWindow.setFocus();
		}
	} // end of setVisible()

	public static void setSizeWindow(XComponent doc, int width, int height) {
		XWindow xWindow = getWindow(doc);
		// xWindow.setVisible(isVisible);
		Rectangle rect = xWindow.getPosSize();
		xWindow.setPosSize(rect.X, rect.Y, width, height - 30, (short) 15);
	} // end of setSizeWindow()

	public static void setPosSize(XComponent doc, int x, int y, int width, int height) {
		XWindow xWindow = getWindow(doc);
		xWindow.setPosSize(x, y, width, height, PosSize.POSSIZE); // use all values
	} // end of setPosSize()

	public static Rectangle getPosSize(XComponent doc) {
		XWindow xWindow = getWindow(doc);
		return xWindow.getPosSize();
	}

	public static XTopWindow getTopWindow() {
		XExtendedToolkit tk = Lo.createInstanceMCF(XExtendedToolkit.class, "com.sun.star.awt.Toolkit");
		if (tk == null) {
			System.out.println("Toolkit not found");
			return null;
		}
		XTopWindow topWin = tk.getActiveTopWindow();
		if (topWin == null) {
			System.out.println("Could not find top window");
			return null;
		}
		return topWin;
	} // end of getTopWindow();

	public static String getTitleBar() {
		XTopWindow topWin = getTopWindow();
		if (topWin == null)
			return null;
		XAccessible acc = Lo.qi(XAccessible.class, topWin);
		if (acc == null) {
			System.out.println("Top window not accessible");
			return null;
		}
		XAccessibleContext accContext = acc.getAccessibleContext();
		return accContext.getAccessibleName();
	} // end of getTitleBar()

	public static Rectangle getScreenSize()
	// effective screen size (i.e. without Windows' taskbar)
	{ // return java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
		java.awt.GraphicsDevice[] gDevs = ge.getScreenDevices();
		java.awt.GraphicsConfiguration gc = gDevs[0].getDefaultConfiguration();
		// assuming that screen is monitor 0

		java.awt.Rectangle bounds = gc.getBounds();
		java.awt.Insets insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc);

		Rectangle rect = new Rectangle(); // Office Rectangle
		rect.X = bounds.x + insets.left;
		rect.Y = bounds.y + insets.top;
		rect.Height = bounds.height - (insets.top + insets.bottom);
		rect.Width = bounds.width - (insets.left + insets.right);

		return rect;
	} // end of getScreenSize()

	public static void printRect(Rectangle r) {
		System.out.println("Rectangle: (" + r.X + ", " + r.Y + "), " + r.Width + " -- " + r.Height);
	}

	public static int getWindowHandle(XComponent doc)
	/*
	 * if you want to use handles, use the JNAUtils library which allows code to use HWND structures
	 */
	{
		XWindow win = getWindow(doc);
		XSystemDependentWindowPeer winPeer = Lo.qi(XSystemDependentWindowPeer.class, win);
		int handle = (Integer) winPeer.getWindowHandle(new byte[8], SystemDependent.SYSTEM_WIN32);
		// System.out.println("Window handle as integer: " + handle);
		return handle;
	} // end of getWindowHandle()

	public static void setLookFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (java.lang.Exception e) {
			System.out.println("Could not set look and feel");
		}
	} // end of setLookFeel()

	// ----------------------------- zooming --------------------------------

	public static void zoom(XComponent doc, short view)
	// zoom using dispatch
	{
		if (view == OPTIMAL)
			Lo.dispatchCmd("ZoomOptimal");
		else if (view == PAGE_WIDTH)
			Lo.dispatchCmd("ZoomPageWidth");
		if (view == ENTIRE_PAGE)
			Lo.dispatchCmd("ZoomPage");
		else {
			System.out.println("Did not recognize zoom view: " + view + "; using optimal");
			Lo.dispatchCmd("ZoomOptimal");
		}
		Lo.delay(500);
	} // end of zoom()

	public static void zoomValue(XComponent doc, int value)
	// zoom to a specific value using a dispatch
	{
		String[] zoomLabels = { "Zoom.Value", "Zoom.ValueSet", "Zoom.Type" };
		Object[] zoomVals = { (short) value, 28703, 0 };
		/*
		 * Could not get types 1, 2, 3 to work; tried value = -32768, 0, 100 Type = 1 'for Fit Optimal Type = 2 'for Fit Width_and_Height Type = 3 'for Fit Width Type = 0 'for Fit user defined %, set value =
		 * 72 for 72%
		 */

		Lo.dispatchCmd("Zoom", Props.makeProps(zoomLabels, zoomVals));
		Lo.delay(500);
	} // end of zoomValue()

	// ================= UI config manager =========================

	public static XUIConfigurationManager getUIConfigManager(XComponent doc) {
		XModel xModel = Lo.qi(XModel.class, doc);
		XUIConfigurationManagerSupplier xSupplier = Lo.qi(XUIConfigurationManagerSupplier.class, xModel);
		return xSupplier.getUIConfigurationManager();
	} // end of getUIConfigManager()

	public static XUIConfigurationManager getUIConfigManagerDoc(XComponent doc) {
		String docType = Info.docTypeString(doc); // null

		XModuleUIConfigurationManagerSupplier xSupplier = Lo.createInstanceMCF(
				XModuleUIConfigurationManagerSupplier.class, "com.sun.star.ui.ModuleUIConfigurationManagerSupplier");

		XUIConfigurationManager configMan = null;
		try {
			configMan = xSupplier.getUIConfigurationManager(docType);
		} catch (Exception e) {
			System.out.println("Could not create a config manager using \"" + docType + "\"");
		}
		return configMan;
	} // end of getUIConfigManagerDoc()

	public static void printUICmds(XUIConfigurationManager configMan, String uiElemName)
	// print every command used by the toolbar whose resource name is uiElemName
	{
		try {
			XIndexAccess settings = configMan.getSettings(uiElemName, true);
			int numSettings = settings.getCount();
			System.out.println("No. of elements in \"" + uiElemName + "\" toolbar: " + numSettings);

			for (int i = 0; i < numSettings; i++) {
				PropertyValue[] settingProps = Lo.qi(PropertyValue[].class, settings.getByIndex(i));
				// Props.showProps("Settings " + i, settingProps);
				Object val = Props.getValue("CommandURL", settingProps);
				System.out.println(i + ") " + Props.propValueToString(val));
			}
			System.out.println();
		} catch (java.lang.Exception e) {
			System.out.println(e);
		}
	} // end of printUICmds()

	public static void printUICmds(XComponent doc, String uiElemName)
	// print every command used by the toolbar whose resource name is uiElemName
	{
		XUIConfigurationManager configMan = GUI.getUIConfigManagerDoc(doc);
		if (configMan == null)
			System.out.println("Cannot create configuration manager");
		else
			GUI.printUICmds(configMan, uiElemName);
	} // end of printUICmds()

	// ================= layout manager =========================

	public static XLayoutManager getLayoutManager() {
		XDesktop desktop = Lo.getDesktop();
		XFrame frame = desktop.getCurrentFrame();
		if (frame == null) {
			System.out.println("No current frame");
			return null;
		}

		XLayoutManager lm = null;
		try {
			XPropertySet propSet = Lo.qi(XPropertySet.class, frame);
			lm = Lo.qi(XLayoutManager.class, propSet.getPropertyValue("LayoutManager"));
		} catch (Exception e) {
			System.out.println("Could not access layout manager");
		}
		return lm;
	} // end of getLayoutManager()

	public static XLayoutManager getLayoutManager(XComponent doc) {
		XLayoutManager lm = null;
		try {
			XPropertySet propSet = Lo.qi(XPropertySet.class, getFrame(doc));
			lm = Lo.qi(XLayoutManager.class, propSet.getPropertyValue("LayoutManager"));
		} catch (Exception e) {
			System.out.println("Could not access layout manager");
		}
		return lm;
	} // end of getLayoutManager()

	public static void printUIs()
	// print the resource names of every toolbar used by desktop
	{
		printUIs(getLayoutManager());
	}

	public static void printUIs(XComponent doc)
	// print the resource names of every toolbar used by doc
	{
		printUIs(getLayoutManager(doc));
	}

	public static void printUIs(XLayoutManager lm)
	// print the resource names of every toolbar
	{
		if (lm == null)
			System.out.println("No layout manager found");
		else {
			XUIElement[] uiElems = lm.getElements();
			System.out.println("No. of UI Elements: " + uiElems.length);
			for (XUIElement uiElem : uiElems)
				System.out.println("  " + uiElem.getResourceURL() + "; " + getUIElementTypeStr(uiElem.getType()));
			System.out.println();
		}
	} // end of printUIs()

	public static String getUIElementTypeStr(short t) {
		if (t == UIElementType.UNKNOWN)
			return "unknown";
		if (t == UIElementType.MENUBAR)
			return "menubar";
		if (t == UIElementType.POPUPMENU)
			return "popup menu";
		if (t == UIElementType.TOOLBAR)
			return "toolbar";
		if (t == UIElementType.STATUSBAR)
			return "status bar";
		if (t == UIElementType.FLOATINGWINDOW)
			return "floating window";
		if (t == UIElementType.PROGRESSBAR)
			return "progress bar";
		if (t == UIElementType.TOOLPANEL)
			return "tool panel";
		if (t == UIElementType.DOCKINGWINDOW)
			return "docking window";
		if (t == UIElementType.COUNT)
			return "count";

		return "??";
	} // end of getUIElementTypeStr()

	public static void printAllUICommands(XComponent doc)
	// print the commands for every toolbar used by the document
	{
		XUIConfigurationManager confMan = getUIConfigManagerDoc(doc);
		if (confMan == null) {
			System.out.println("No configuration manager found");
			return;
		}
		XLayoutManager lm = getLayoutManager(doc);
		if (lm == null) {
			System.out.println("No layout manager found");
			return;
		}

		XUIElement[] uiElems = lm.getElements();
		System.out.println("No. of UI Elements: " + uiElems.length);
		String uiElemName;
		for (XUIElement uiElem : uiElems) {
			uiElemName = uiElem.getResourceURL();
			System.out.println("--- " + uiElemName + " ---");
			printUICmds(confMan, uiElemName);
		}
	} // end of printAllUICommands()

	public static void showOne(XComponent doc, String showElem)
	// leave only the single specified toolbar visible
	{
		ArrayList<String> showElems = new ArrayList<String>();
		showElems.add(showElem);
		showOnly(doc, showElems);
	} // end of showOne()

	public static void showOnly(XComponent doc, ArrayList<String> showElems)
	// leave only the specified toolbars visible
	{
		XLayoutManager lm = getLayoutManager(doc);
		if (lm == null)
			System.out.println("No layout manager found");
		else {
			XUIElement[] uiElems = lm.getElements();
			hideExcept(lm, uiElems, showElems);
			for (String elemName : showElems) { // these elems are not in lm
				lm.createElement(elemName); // so need to be created & shown
				lm.showElement(elemName);
				System.out.println(elemName + " made visible");
			}
		}
	} // end of showOnly()

	public static void hideExcept(XLayoutManager lm, XUIElement[] uiElems, ArrayList<String> showElems)
	// hide all of uiElems, except ones in showElems;
	// delete any strings that match in showElems
	{
		for (XUIElement uiElem : uiElems) {
			String elemName = uiElem.getResourceURL();
			boolean toHide = true;
			for (int i = 0; i < showElems.size(); i++) {
				if (showElems.get(i).equals(elemName)) {
					showElems.remove(i); // this elem is in lm so remove from showElems
					toHide = false; // since the toolbar is already shown
					break;
				}
			}
			if (toHide) {
				lm.hideElement(elemName);
				System.out.println(elemName + " hidden");
			}
		}
	} // end of hideExcept()

	static void showNone(XComponent doc)
	// make all the toolbars invisible;
	// or use lm.setVisible(false)
	{
		XLayoutManager lm = getLayoutManager(doc);
		if (lm == null)
			System.out.println("No layout manager found");
		else {
			XUIElement[] uiElems = lm.getElements();
			for (XUIElement uiElem : uiElems) {
				String elemName = uiElem.getResourceURL();
				lm.hideElement(elemName);
				System.out.println(elemName + " hidden");
			}
		}
	} // end of showNone()

	// ----------------------- menu bar --------------------

	public static XMenuBar getMenubar(XLayoutManager lm)
	// return a reference to the GUI.MENU_BAR menubar
	{
		if (lm == null) {
			System.out.println("No layout manager available for menu discovery");
			return null;
		}

		XMenuBar bar = null;
		try {
			XUIElement oMenuBar = lm.getElement(GUI.MENU_BAR);
			XPropertySet props = Lo.qi(XPropertySet.class, oMenuBar);
			// Props.showProps("Menu bar", props);

			bar = Lo.qi(XMenuBar.class, props.getPropertyValue("XMenuBar"));
			// the XMenuBar reference is a property of the menubar UI
			if (bar == null)
				System.out.println("Menubar reference not found");
		} catch (Exception e) {
			System.out.println("Could not access menubar");
		}
		return bar;
	} // end of getMenubar()

	public static short getMenuMaxID(XMenuBar bar)
	/*
	 * Scan through the IDs used by all the items in this menubar, and return the largest ID encountered
	 */
	{
		if (bar == null)
			return -1;

		short itemCount = bar.getItemCount();
		System.out.println("No items in menu bar: " + itemCount);
		short maxID = -1;
		for (short i = 0; i < itemCount; i++) {
			short id = bar.getItemId(i);
			if (id > maxID)
				maxID = id;
			// System.out.println(" Name: \"" + bar.getItemText(id) + "\"; ID: " + id);
		}
		// System.out.println("Biggest ID: " + maxID);
		return maxID;
	} // end of getMenuMaxID()

} // end of GUI class
