package de.petanqueturniermanager.sidebar.sheets;

import com.sun.star.awt.XWindow;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.BaseSidebarPanel;

/**
 * Sidebar-Panel, das alle PTM-verwalteten Tabellenblätter auflistet.
 *
 * @author Michael Massee
 */
public class SheetListeSidebarPanel extends BaseSidebarPanel {

    public SheetListeSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
            String resourceUrl, XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, resourceUrl, xSidebar);
    }

    @Override
    protected BaseSidebarContent neuesPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
            XSidebar xSidebar) {
        return new SheetListeSidebarContent(workingSpreadsheet, parentWindow, xSidebar);
    }
}
