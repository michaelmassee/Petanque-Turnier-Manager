package de.petanqueturniermanager.sidebar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.LogUtil;

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
        verteile("onNew", source, IGlobalEventListener::onNew);
    }

    @Override
    public void onLoad(Object source) {
        verteile("onLoad", source, IGlobalEventListener::onLoad);
    }

    @Override
    public void onUnload(Object source) {
        verteile("onUnload", source, IGlobalEventListener::onUnload);
    }

    @Override
    public void onUnfocus(Object source) {
        verteile("onUnfocus", source, IGlobalEventListener::onUnfocus);
    }

    @Override
    public void onViewCreated(Object source) {
        verteile("onViewCreated", source, IGlobalEventListener::onViewCreated);
    }

    @Override
    public void onViewClosed(Object source) {
        verteile("onViewClosed", source, IGlobalEventListener::onViewClosed);
    }

    @Override
    public void onLoadFinished(Object source) {
        verteile("onLoadFinished", source, IGlobalEventListener::onLoadFinished);
    }

    @Override
    public void onFocus(Object source) {
        verteile("onFocus", source, IGlobalEventListener::onFocus);
    }

    private void verteile(String eventName, Object source, BiConsumer<IGlobalEventListener, Object> dispatch) {
        for (var panel : snapshot()) {
            try {
                dispatch.accept(panel, source);
            } catch (Exception e) {
                LogUtil.warn(logger, "Sidebar-Event " + eventName + " an "
                        + panel.getClass().getSimpleName() + " fehlgeschlagen", e);
            } catch (Error e) {
                throw e;
            }
        }
    }

    private List<IGlobalEventListener> snapshot() {
        return new ArrayList<>(panels);
    }
}
