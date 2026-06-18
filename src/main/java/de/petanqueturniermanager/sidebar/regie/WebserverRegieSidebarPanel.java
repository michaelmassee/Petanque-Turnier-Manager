package de.petanqueturniermanager.sidebar.regie;

import com.sun.star.awt.XWindow;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.BaseSidebarPanel;

public class WebserverRegieSidebarPanel extends BaseSidebarPanel {

    public WebserverRegieSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, String resourceUrl,
            XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, resourceUrl, xSidebar);
    }

    @Override
    protected BaseSidebarContent neuesPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
            XSidebar xSidebar) {
        return new WebserverRegieSidebarContent(workingSpreadsheet, parentWindow, xSidebar);
    }
}
