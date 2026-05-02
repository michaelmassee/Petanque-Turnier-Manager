package de.petanqueturniermanager.sidebar.sheets;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.sidebar.PetanqueTurnierManagerPanelFactory;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SupermeleeTurnierTestDaten;

/**
 * Regressionstest für den SIGSEGV-Crash beim Sidebar-Toggle.
 * <p>
 * Reproduziert den Bug: Sidebar aus → wieder ein → Klick in Sheet-Liste → SIGSEGV.
 * Ursache war, dass {@code felderHinzufuegen()} im super()-Konstruktor aufgerufen wurde,
 * bevor die Subklassen-Field-Initializer (insb. {@code itemListener}) liefen.
 * Dadurch wurde {@code addItemListener(null)} an die UNO-Bridge übergeben.
 * <p>
 * Der Test verifiziert, dass nach einem dispose-Zyklus (Toggle-Simulation) ein neues Panel
 * korrekt initialisiert ist: {@code sheetListBox} und {@code itemListener} sind non-null
 * und {@code baumEintraege} enthält Einträge.
 */
@DisplayName("SheetListeSidebar – Sidebar-Toggle-Regression")
public class SheetListeSidebarToggleUITest extends BaseCalcUITest {

    private static final String PANEL_URL =
            PetanqueTurnierManagerPanelFactory.URL_PREFIX + "/SheetListePanel";

    /** Keine echte Sidebar nötig – requestLayout() ist im Test ein No-Op. */
    private static class MinimalXSidebar implements XSidebar {
        @Override
        public void requestLayout() {
            // intentional no-op
        }
    }

    private SheetListeSidebarPanel panel;

    @BeforeEach
    void turnierdatenGenerieren() throws Exception {
        new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
    }

    @AfterEach
    void disposePanel() {
        if (panel != null) {
            Lo.qi(XComponent.class, panel).dispose();
            panel = null;
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Erstes Panel: sheetListBox und baumEintraege sind korrekt initialisiert")
    void erstesPanelIstKorrektInitialisiert() throws Exception {
        panel = neuesPanel();
        var content = content(panel);

        assertThat(sheetListBox(content))
                .as("sheetListBox muss nach Konstruktion non-null sein")
                .isNotNull();
        assertThat(baumEintraege(content))
                .as("baumEintraege müssen nach Turnierdaten-Generierung nicht leer sein")
                .isNotEmpty();
        assertThat(itemListener(content))
                .as("itemListener muss als Field-Initializer vor felderHinzufuegen() ausgeführt worden sein")
                .isNotNull();
    }

    @Test
    @DisplayName("Toggle-Regression: Panel nach dispose() korrekt initialisiert (SIGSEGV-Fix)")
    void panelNachToggle_IstKorrektInitialisiert() throws Exception {
        // Erste Panel-Instanz (entspricht: Sidebar wird geöffnet)
        SheetListeSidebarPanel panel1 = neuesPanel();
        assertThat(sheetListBox(content(panel1))).isNotNull();

        // Dispose (entspricht: Sidebar wird geschlossen / Toggle off)
        Lo.qi(XComponent.class, panel1).dispose();

        // Zweite Panel-Instanz (entspricht: Sidebar wird wieder geöffnet / Toggle on)
        // Vor dem Fix: felderHinzufuegen() lief mit null-itemListener → addItemListener(null)
        // → SIGSEGV beim nächsten Klick in die Liste
        panel = neuesPanel();
        var content2 = content(panel);

        assertThat(sheetListBox(content2))
                .as("sheetListBox des zweiten Panels muss non-null sein")
                .isNotNull();
        assertThat(baumEintraege(content2))
                .as("baumEintraege des zweiten Panels müssen nicht leer sein")
                .isNotEmpty();
        assertThat(itemListener(content2))
                .as("itemListener des zweiten Panels muss non-null sein – war vor Fix null durch falsches Init-Timing")
                .isNotNull();
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private SheetListeSidebarPanel neuesPanel() {
        XModel model = Lo.qi(XModel.class, doc);
        XWindow containerWindow = model.getCurrentController().getFrame().getContainerWindow();
        return new SheetListeSidebarPanel(wkingSpreadsheet, containerWindow, PANEL_URL, new MinimalXSidebar());
    }

    private static SheetListeSidebarContent content(SheetListeSidebarPanel p) {
        return (SheetListeSidebarContent) p.getRealInterface();
    }

    private static XListBox sheetListBox(SheetListeSidebarContent content) throws Exception {
        return privateField(content, "sheetListBox", XListBox.class);
    }

    @SuppressWarnings("unchecked")
    private static List<BlattBaumEintrag> baumEintraege(SheetListeSidebarContent content) throws Exception {
        return privateField(content, "baumEintraege", List.class);
    }

    private static Object itemListener(SheetListeSidebarContent content) throws Exception {
        return privateField(content, "itemListener", Object.class);
    }

    private static <T> T privateField(Object obj, String name, Class<T> type) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(obj));
    }
}