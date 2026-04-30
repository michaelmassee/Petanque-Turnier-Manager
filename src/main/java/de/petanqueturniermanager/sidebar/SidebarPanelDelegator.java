package de.petanqueturniermanager.sidebar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;

/**
 * Delegiert globale LO-Events an aktive Sidebar-Panels.
 * <p>
 * Panels registrieren sich hier statt direkt im globalen Event-Bus
 * ({@code PetanqueTurnierMngrSingleton}). Dadurch bleibt der globale Bus frei von
 * kurzlebigen Panel-Instanzen: {@code bereinigen()} modifiziert nur diese lokale
 * Panel-Liste — nie die globale {@code listeners}-Liste während deren Iteration.
 */
public class SidebarPanelDelegator implements IGlobalEventListener {

    private static final Logger logger = LogManager.getLogger(SidebarPanelDelegator.class);
    private static final SidebarPanelDelegator INSTANCE = new SidebarPanelDelegator();

    private final List<IGlobalEventListener> panels = Collections.synchronizedList(new ArrayList<>());

    private SidebarPanelDelegator() {
    }

    public static SidebarPanelDelegator get() {
        return INSTANCE;
    }

    public void registrieren(IGlobalEventListener panel) {
        panels.add(panel);
        logger.debug("Panel registriert: {}, gesamt={}", panel.getClass().getSimpleName(), panels.size());
    }

    public void entfernen(IGlobalEventListener panel) {
        panels.remove(panel);
        logger.debug("Panel entfernt: {}, gesamt={}", panel.getClass().getSimpleName(), panels.size());
    }

    @Override
    public void onNew(Object source) {
        for (var panel : snapshot()) {
            try {
                panel.onNew(source);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onLoad(Object source) {
        for (var panel : snapshot()) {
            try {
                panel.onLoad(source);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onUnload(Object source) {
        for (var panel : snapshot()) {
            try {
                panel.onUnload(source);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onUnfocus(Object source) {
        for (var panel : snapshot()) {
            try {
                panel.onUnfocus(source);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onViewCreated(Object source) {
        for (var panel : snapshot()) {
            try {
                panel.onViewCreated(source);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onViewClosed(Object source) {
        for (var panel : snapshot()) {
            try {
                panel.onViewClosed(source);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onLoadFinished(Object source) {
        for (var panel : snapshot()) {
            try {
                panel.onLoadFinished(source);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void onFocus(Object source) {
        for (var panel : snapshot()) {
            try {
                panel.onFocus(source);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private List<IGlobalEventListener> snapshot() {
        return new ArrayList<>(panels);
    }
}
