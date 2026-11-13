/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Synchroner Versand angehalten, weil PTM-Online schon neuere Aufträge dieses Dokuments kennt
 * ({@code document_forked}). Der manuelle Abgleich fragt dann, ob der Stand dieses Dokuments gelten soll
 * ({@link PtmOnlineDokumentstand}); alle anderen Aufrufer melden den Text.
 */
final class PtmOnlineDokumentGeforktException extends GenerateException {

    private static final long serialVersionUID = 1L;

    PtmOnlineDokumentGeforktException() {
        super(I18n.get("ptmonline.fehler.dokument_geforkt"));
    }
}
