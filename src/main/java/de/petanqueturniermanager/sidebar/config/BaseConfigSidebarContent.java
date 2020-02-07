/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ImageAlign;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.style.VerticalAlignment;
import com.sun.star.ui.XSidebar;
import com.sun.star.uno.Exception;

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.properties.FarbenDialog;
import de.petanqueturniermanager.konfigdialog.properties.KopfFusszeilenDialog;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.fields.BaseField;
import de.petanqueturniermanager.sidebar.fields.LabelPlusBackgrColorAndColorChooser;
import de.petanqueturniermanager.sidebar.fields.LabelPlusTextPlusTextareaBox;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public abstract class BaseConfigSidebarContent extends BaseSidebarContent {
	static final Logger logger = LogManager.getLogger(BaseConfigSidebarContent.class);

	private static final Predicate<ConfigProperty<?>> KONFIG_PROP_FILTER = konfigprop -> konfigprop.isInSideBar() && !konfigprop.isInSideBarInfoPanel();

	private boolean turnierFields;

	/**
	 * @param workingSpreadsheet
	 * @param parentWindow
	 * @param xSidebar
	 */
	public BaseConfigSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, xSidebar);
	}

	@Override
	protected void disposing(EventObject event) {
	}

	/**
	 * event from menu
	 */
	@Override
	protected void updateFieldContens(ITurnierEvent eventObj) {
		if (!turnierFields) {
			addFields();
		}
	}

	/**
	 * event from new and load
	 */
	@Override
	protected void removeAndAddFields() {
		boolean mustLayout = false;
		if (turnierFields) {
			super.removeAllFieldsAndNewBaseWindow();
			mustLayout = true;
			turnierFields = false;
		}
		addFields(mustLayout);
	}

	@Override
	protected void addFields() {
		addFields(false);
	}

	private void addFields(boolean forceMustLayout) {

		// Turnier vorhanden ?
		TurnierSystem turnierSystemAusDocument = getTurnierSystemAusDocument();
		if (turnierSystemAusDocument == null || turnierSystemAusDocument == TurnierSystem.KEIN) {
			// kein Turnier
			turnierFields = false;
			if (forceMustLayout) {
				requestLayout();
			}
			return;
		}

		logger.debug("addFields");

		List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(getCurrentSpreadsheet());
		if (konfigProperties == null) {
			// kein Turnier vorhanden
			return;
		}

		AddConfigElementsToWindow addConfigElementsToWindow = new AddConfigElementsToWindow(getGuiFactoryCreateParam(), getCurrentSpreadsheet(), getLayout());
		setChangingLayout(true);

		// button panel
		addButtonLine();

		try {
			konfigProperties.stream().filter(KONFIG_PROP_FILTER).filter(getKonfigFieldFilter()).collect(Collectors.toList())
					.forEach(konfigprop -> addConfigElementsToWindow.addPropToPanel(konfigprop));
		} finally {
			setChangingLayout(false);
		}

		// Request layout of the sidebar.
		// Call this method when one of the panels wants to change its size due to late
		// initialization or different content after a context change.
		// Only in InfoPanel
		requestLayout();
		turnierFields = true;
	}

	/**
	 * eine kleine leiste mit icon buttons
	 */
	private void addButtonLine() {
		HorizontalLayout hLayout = new HorizontalLayout();
		// spacer
		XControl spacer = GuiFactory.createLabel(getGuiFactoryCreateParam(), "", BaseField.BASE_RECTANGLE, null);
		hLayout.addControl(spacer, 1);
		newColorBtn(hLayout);
		newHeaderBtn(hLayout);
		getLayout().addLayout(hLayout, 1);
	}

	private void newHeaderBtn(HorizontalLayout hLayout) {
		newBtn(hLayout, LabelPlusTextPlusTextareaBox.btnImage, "Kopf/Fußzeilen", new XActionListener() {
			@Override
			public void disposing(EventObject arg0) {
			}

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					new KopfFusszeilenDialog(getCurrentSpreadsheet()).createDialog();
				} catch (Exception e) {
				}
			}
		});
	}

	private void newColorBtn(HorizontalLayout hLayout) {
		newBtn(hLayout, LabelPlusBackgrColorAndColorChooser.btnImage, "Farben", new XActionListener() {
			@Override
			public void disposing(EventObject arg0) {
			}

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					new FarbenDialog(getCurrentSpreadsheet()).createDialog();
				} catch (Exception e) {
				}
			}
		});
	}

	private void newBtn(HorizontalLayout hLayout, String imageUrl, String helpText, XActionListener xActionListener) {
		Map<String, Object> props = new HashMap<>();
		props.putIfAbsent(GuiFactory.HELP_TEXT, helpText);
		props.putIfAbsent(GuiFactory.VERTICAL_ALIGN, VerticalAlignment.MIDDLE);
		// specifies the horizontal alignment of the text in the control.
		props.putIfAbsent("Align", (short) 1); // 0=left, 1 = center, 2 = right
		props.putIfAbsent("ImageAlign", ImageAlign.RIGHT);
		props.putIfAbsent(GuiFactory.IMAGE_URL, getImageUrlDir() + imageUrl);

		// höhe wird nicht verändert
		Rectangle btnRect = new Rectangle(BaseField.BASE_RECTANGLE.X, BaseField.BASE_RECTANGLE.Y, BaseField.BASE_RECTANGLE.Width, 29);
		XControl btnControl = GuiFactory.createButton(getGuiFactoryCreateParam(), null, xActionListener, btnRect, props);
		// btn = UnoRuntime.queryInterface(XButton.class, btnControl);
		hLayout.addFixedWidthControl(btnControl, 29); // fest 29px breit
	}

	private final String getImageUrlDir() {
		return ExtensionsHelper.from(getCurrentSpreadsheet().getxContext()).getImageUrlDir();
	}

	protected abstract java.util.function.Predicate<ConfigProperty<?>> getKonfigFieldFilter();

}
