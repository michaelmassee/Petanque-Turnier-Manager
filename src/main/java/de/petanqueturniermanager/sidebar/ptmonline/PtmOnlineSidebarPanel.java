/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.ptmonline;

import com.sun.star.awt.XWindow;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.BaseSidebarPanel;

/**
 * Sidebar-Panel „PTM-Online“ unter „PTM Info“: Zustand der Verbindung des Dokuments mit PTM-Online.
 *
 * @author Michael Massee
 */
public class PtmOnlineSidebarPanel extends BaseSidebarPanel {

    public PtmOnlineSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, String resourceUrl,
            XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, resourceUrl, xSidebar);
    }

    @Override
    protected BaseSidebarContent neuesPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
            XSidebar xSidebar) {
        return new PtmOnlineSidebarContent(workingSpreadsheet, parentWindow, xSidebar);
    }
}
