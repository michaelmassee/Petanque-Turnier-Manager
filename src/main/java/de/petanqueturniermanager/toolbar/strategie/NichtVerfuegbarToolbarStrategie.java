/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Fallback-Strategie für Turniersysteme, bei denen eine Toolbar-Aktion
 * nicht sinnvoll verfügbar ist. Zeigt einen Hinweis-Dialog und loggt eine Warnung.
 */
public class NichtVerfuegbarToolbarStrategie implements ITurnierSystemToolbarStrategie {

    private static final Logger logger = LogManager.getLogger(NichtVerfuegbarToolbarStrategie.class);

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        zeigeNichtVerfuegbar(ws, I18n.get("toolbar.weiter.nicht.verfuegbar"));
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        zeigeNichtVerfuegbar(ws, I18n.get("toolbar.vorrunden.rangliste.nicht.verfuegbar"));
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        zeigeNichtVerfuegbar(ws, I18n.get("toolbar.teilnehmer.nicht.verfuegbar"));
    }

    @Override
    public void neuAuslosen(WorkingSpreadsheet ws) throws Exception {
        zeigeNichtVerfuegbar(ws, I18n.get("toolbar.neu.auslosen.nicht.verfuegbar"));
    }

    @Override
    public void abschluss(WorkingSpreadsheet ws) throws Exception {
        zeigeNichtVerfuegbar(ws, I18n.get("toolbar.abschluss.nicht.verfuegbar"));
    }

    @Override
    public void naechsterSpieltag(WorkingSpreadsheet ws) throws Exception {
        zeigeNichtVerfuegbar(ws, I18n.get("toolbar.naechster.spieltag.nicht.verfuegbar"));
    }

    @Override
    public void gesamtrangliste(WorkingSpreadsheet ws) throws Exception {
        zeigeNichtVerfuegbar(ws, I18n.get("toolbar.gesamtrangliste.nicht.verfuegbar"));
    }

    private void zeigeNichtVerfuegbar(WorkingSpreadsheet ws, String nachricht) throws Exception {
        logger.warn("Toolbar-Aktion nicht verfügbar für aktives Turniersystem: {}", nachricht);
        MessageBox.from(ws, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("toolbar.aktion.nicht.verfuegbar.titel"))
                .message(nachricht)
                .show();
    }
}
