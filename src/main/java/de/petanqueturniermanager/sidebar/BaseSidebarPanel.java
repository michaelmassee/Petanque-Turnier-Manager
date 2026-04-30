/**
 * Erstellung 21.01.2020 / Michael Massee
 * Neu geschrieben 2026-03
 */
package de.petanqueturniermanager.sidebar;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.UIElementType;
import com.sun.star.ui.XSidebar;
import com.sun.star.ui.XUIElement;

import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;

/**
 * Schlanker {@link XUIElement}-Wrapper. Hält ein {@link BaseSidebarContent}.
 * <p>
 * Wenn LibreOffice dieses Panel disposes (z.B. beim Wechsel des Sidebars oder
 * beim Schließen), wird {@link BaseSidebarContent#bereinigen()} aufgerufen, um
 * das Kind-Fenster zu entfernen und Event-Listener abzumelden. Ohne diesen
 * Aufruf würden alte Panel-Instanzen und ihre Fenster sichtbar bleiben
 * (Verdopplungs-Bug).
 * <p>
 * Druckvorschau-Exit: Wenn das Panel während FillToolbar konstruiert wird,
 * registriert sich dieser Wrapper (nicht der Content) im
 * {@link SidebarPanelDelegator}, um {@code onViewCreated} zu empfangen.
 * In {@code onViewCreated} schließt der Content seine Initialisierung ab und
 * übernimmt die vollständige Registrierung; der Wrapper trägt sich danach aus.
 *
 * @author Michael Massee
 */
public abstract class BaseSidebarPanel extends ComponentBase implements XUIElement, IGlobalEventListener {

	private static final Logger logger = LogManager.getLogger(BaseSidebarPanel.class);

	private final String resourceUrl;
	private BaseSidebarContent panel;

	public BaseSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, String resourceUrl,
			XSidebar xSidebar) {
		this.resourceUrl = checkNotNull(resourceUrl);
		panel = neuesPanel(workingSpreadsheet, parentWindow, xSidebar);
		if (PetanqueTurnierMngrSingleton.isDruckvorschauAktiv()) {
			SidebarPanelDelegator.get().registrieren(this);
		}
	}

	/**
	 * Schließt die verzögerte Initialisierung ab: leitet {@code onViewCreated} an
	 * den Content weiter und trägt sich selbst aus dem Delegator aus.
	 */
	@Override
	public void onViewCreated(Object source) {
		if (panel != null) {
			panel.onViewCreated(source);
			SidebarPanelDelegator.get().entfernen(this);
		}
	}

	/**
	 * Wenn LibreOffice das XUIElement disposes, auch den Content bereinigen.
	 * Verhindert, dass alte Fenster (Kinder des parentWindow) sichtbar bleiben.
	 */
	@Override
	public void dispose() {
		logger.debug("BaseSidebarPanel.dispose – bereinige panel");
		SidebarPanelDelegator.get().entfernen(this);
		if (panel != null) {
			panel.bereinigen();
			panel = null;
		}
		super.dispose();
	}

	@Override
	public final XFrame getFrame() {
		return null;
	}

	@Override
	public final Object getRealInterface() {
		return panel;
	}

	@Override
	public final String getResourceURL() {
		return resourceUrl;
	}

	@Override
	public final short getType() {
		return UIElementType.TOOLPANEL;
	}

	protected abstract BaseSidebarContent neuesPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
			XSidebar xSidebar);
}
