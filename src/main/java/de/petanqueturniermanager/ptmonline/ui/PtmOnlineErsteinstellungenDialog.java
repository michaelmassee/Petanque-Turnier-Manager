/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Fragt beim ersten Verbinden die PTM-Online-Schalter ab ({@link PtmOnlineErsteinstellungen}): je Schalter ein
 * Häkchen, vorbelegt mit dem Stand des Dokuments. Die Ergebnismeldung setzt die Live-Ansicht voraus, das direkte
 * Eintragen die Ergebnismeldung; abhängige Häkchen sind sonst nicht wählbar.
 */
final class PtmOnlineErsteinstellungenDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(PtmOnlineErsteinstellungenDialog.class);
    private static final Duration DIALOG_TIMEOUT = Duration.ofMinutes(30);
    private static final short ANGEHAKT = 1;

    private static final int BREITE = 260;
    private static final int RAND = 8;
    private static final int INHALT_BREITE = BREITE - 2 * RAND;
    private static final int HINWEIS_HOEHE = 30;
    private static final int ZEILE = 15;
    private static final int KAESTCHEN_HOEHE = 12;
    private static final int KNOPF_BREITE = 60;
    private static final int KNOPF_HOEHE = 14;

    private final XWindowPeer parentPeer;
    private final String turnierName;
    private final PtmOnlineErsteinstellungen.Auswahl vorbelegung;
    private final Set<String> vorhandeneSchalter;
    /** Modell je angezeigtem Schalter; Ereignisse und OK laufen auf dem Main-Thread. */
    private final Map<String, XPropertySet> kaestchen = new HashMap<>();
    private PtmOnlineErsteinstellungen.Auswahl auswahl;

    private PtmOnlineErsteinstellungenDialog(WorkingSpreadsheet ws, String turnierName,
            PtmOnlineErsteinstellungen.Auswahl vorbelegung, Set<String> vorhandeneSchalter) {
        super(ws.getxContext());
        this.parentPeer = ws.getContainerWindowPeer();
        this.turnierName = turnierName;
        this.vorbelegung = vorbelegung;
        this.vorhandeneSchalter = Set.copyOf(vorhandeneSchalter);
    }

    /**
     * Zeigt den Dialog auf dem Main-Thread und wartet auf die Antwort. Nur von einem Hintergrund-Thread (SheetRunner)
     * aufrufen, nie vom Main-Thread – sonst wartet dieser auf sich selbst. Im Test ({@link
     * MessageBox#istDialogeUeberspringen()}) entfällt die Frage.
     *
     * @return leer bei Abbrechen
     */
    static Optional<PtmOnlineErsteinstellungen.Auswahl> zeigen(WorkingSpreadsheet ws, String turnierName,
            PtmOnlineErsteinstellungen.Auswahl vorbelegung, Set<String> vorhandeneSchalter) throws GenerateException {
        if (MessageBox.istDialogeUeberspringen()) {
            return Optional.empty();
        }
        var future = new CompletableFuture<Optional<PtmOnlineErsteinstellungen.Auswahl>>();
        LoMainThread.post(ws.getxContext(), () -> {
            try {
                var dialog = new PtmOnlineErsteinstellungenDialog(ws, turnierName, vorbelegung, vorhandeneSchalter);
                dialog.erstelleUndAusfuehren();
                future.complete(Optional.ofNullable(dialog.auswahl));
            } catch (Exception e) {
                logger.error("Fehler im Dialog der PTM-Online-Ersteinstellungen", e);
                future.completeExceptionally(e);
            }
        });
        try {
            return future.get(DIALOG_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            Throwable ursache = e.getCause();
            throw new GenerateException(ursache != null ? ursache.getMessage() : e.getMessage());
        } catch (TimeoutException e) {
            logger.warn("Dialog der PTM-Online-Ersteinstellungen hat nicht rechtzeitig geantwortet", e);
            return Optional.empty();
        }
    }

    @Override
    protected String getTitel() {
        return I18n.get("ptmonline.ersteinstellungen.titel", StringUtils.defaultString(turnierName));
    }

    @Override
    protected int getBreite() {
        return BREITE;
    }

    @Override
    protected int getHoehe() {
        return knopfZeile() + KNOPF_HOEHE + RAND;
    }

    private int knopfZeile() {
        return RAND + HINWEIS_HOEHE + vorhandeneSchalter.size() * ZEILE + RAND;
    }

    @Override
    protected XWindowPeer holeParentPeer() {
        return parentPeer;
    }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF, XNameContainer cont,
            XToolkit xToolkit, XWindowPeer peer, XPropertySet dlgProps, XDialog xDialog)
            throws com.sun.star.uno.Exception {
        label(xMSF, cont, "lblHinweis", I18n.get("ptmonline.ersteinstellungen.hinweis"),
                new Rahmen(RAND, RAND, INHALT_BREITE, HINWEIS_HOEHE), true);
        int y = RAND + HINWEIS_HOEHE;
        for (String schalter : PtmOnlineErsteinstellungen.SCHALTER) {
            if (vorhandeneSchalter.contains(schalter)) {
                kaestchen.put(schalter, checkBox(xMSF, cont, schalter, I18n.get(labelSchluessel(schalter)),
                        new Rahmen(RAND, y, INHALT_BREITE, KAESTCHEN_HOEHE), vorbelegt(schalter)));
                y += ZEILE;
            }
        }
        int knopfY = knopfZeile();
        int abbrechenX = BREITE - RAND - KNOPF_BREITE;
        button(xMSF, cont, "btnOk", I18n.get("dialog.button.ok"),
                new Rahmen(abbrechenX - RAND - KNOPF_BREITE, knopfY, KNOPF_BREITE, KNOPF_HOEHE),
                (short) PushButtonType.OK_value);
        button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.button.abbrechen"),
                new Rahmen(abbrechenX, knopfY, KNOPF_BREITE, KNOPF_HOEHE), (short) PushButtonType.CANCEL_value);

        aktualisiereAbhaengigkeiten();
        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        kaestchen.keySet().forEach(schalter -> beiAenderung(xcc.getControl(schalter)));
    }

    private static String labelSchluessel(String schalter) {
        return switch (schalter) {
            case BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT -> "config.label.ptmonline.live.ansicht";
            case BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG -> "config.label.ptmonline.ergebnismeldung";
            default -> "config.label.ptmonline.ergebnisse.direkt";
        };
    }

    private boolean vorbelegt(String schalter) {
        return switch (schalter) {
            case BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT -> vorbelegung.liveAnsicht();
            case BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG -> vorbelegung.ergebnismeldung();
            default -> vorbelegung.direktEintragen();
        };
    }

    /** Häkchen-Ereignisse kommen vom Dialog selbst und laufen damit bereits auf dem Main-Thread. */
    private void beiAenderung(XControl control) {
        XCheckBox checkBox = control == null ? null : Lo.qi(XCheckBox.class, control);
        if (checkBox == null) {
            return;
        }
        checkBox.addItemListener(new XItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                aktualisiereAbhaengigkeiten();
            }

            @Override
            public void disposing(EventObject e) {
                // nichts zu tun
            }
        });
    }

    /** Ergebnismeldung nur mit Live-Ansicht, direktes Eintragen nur mit Ergebnismeldung. */
    private void aktualisiereAbhaengigkeiten() {
        boolean live = angehaktOderFehlend(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT);
        setzeWaehlbar(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG, live);
        boolean meldung = live && angehaktOderFehlend(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG);
        setzeWaehlbar(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISSE_DIREKT, meldung);
    }

    /** Ein Schalter, den das System nicht kennt, sperrt die abhängigen nicht. */
    private boolean angehaktOderFehlend(String schalter) {
        XPropertySet modell = kaestchen.get(schalter);
        return modell == null || istAngehakt(modell);
    }

    private void setzeWaehlbar(String schalter, boolean waehlbar) {
        XPropertySet modell = kaestchen.get(schalter);
        if (modell == null) {
            return;
        }
        try {
            modell.setPropertyValue("Enabled", Boolean.valueOf(waehlbar));
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("PTM-Online: Häkchen {} nicht umschaltbar", schalter, e);
        }
    }

    private static boolean istAngehakt(XPropertySet modell) {
        try {
            return Short.valueOf(ANGEHAKT).equals(modell.getPropertyValue("State"));
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("PTM-Online: Zustand eines Häkchens nicht lesbar", e);
            return false;
        }
    }

    @Override
    protected void beiOkGeklickt() {
        boolean live = angehaktOderFehlend(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_LIVE_ANSICHT);
        boolean meldung = live && angehaktOderFehlend(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISMELDUNG);
        boolean direkt = meldung && angehaktOderFehlend(BasePropertiesSpalte.KONFIG_PROP_PTMONLINE_ERGEBNISSE_DIREKT);
        auswahl = new PtmOnlineErsteinstellungen.Auswahl(live, meldung, direkt);
    }
}
