/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

/**
 * Supermelee: jeder Spieltag ist online ein eigenes Turnier. Mit einem neuen Spieltag werden die Verbindungen der
 * bisherigen Spieltage online getrennt und ihre Blätter „PTMOnline Sync“ archiviert; der neue Spieltag muss mit
 * einem neuen Online-Turnier verbunden werden. Scheitert das Trennen online (z.&nbsp;B. kein Netz), wird trotzdem
 * lokal archiviert und gewarnt – PTM-Online blockiert den Turnierbetrieb nie.
 * <p>
 * Läuft synchron im SheetRunner des neuen Spieltags.
 */
public final class PtmOnlineSpieltagWechsel {

    private static final Logger logger = LogManager.getLogger(PtmOnlineSpieltagWechsel.class);

    private PtmOnlineSpieltagWechsel() {}

    /** Trennt alle noch verbundenen Spieltage vor {@code neuerSpieltag} und weist auf das Neu-Verbinden hin. */
    public static void trenneVorherigeSpieltage(WorkingSpreadsheet ws, int neuerSpieltag) throws GenerateException {
        trenneVorherigeSpieltage(ws, neuerSpieltag, new LibreOfficePtmOnlineSpeicher(ws.getxContext()).laden());
    }

    static void trenneVorherigeSpieltage(WorkingSpreadsheet ws, int neuerSpieltag,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config) throws GenerateException {
        List<Integer> getrennt = new ArrayList<>();
        List<String> warnungen = new ArrayList<>();
        for (int spieltag = 1; spieltag < neuerSpieltag; spieltag++) {
            PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(ws, TurnierSystem.SUPERMELEE,
                    spieltag);
            if (mapping.getTournamentId().isEmpty()) {
                continue;
            }
            onlineTrennen(ws, config, mapping, spieltag).ifPresent(warnungen::add);
            mapping.archivieren();
            getrennt.add(spieltag);
        }
        if (!getrennt.isEmpty()) {
            zeigeHinweis(ws, getrennt, neuerSpieltag, warnungen);
        }
    }

    /** @return Warnung, wenn das Online-Turnier nicht getrennt werden konnte */
    private static Optional<String> onlineTrennen(WorkingSpreadsheet ws,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping, int spieltag) {
        if (!config.isConfigured()) {
            return Optional.of(I18n.get("ptmonline.spieltag.trennen.fehlgeschlagen", spieltag,
                    I18n.get("ptmonline.fehler.nicht_konfiguriert")));
        }
        try {
            PtmOnlineTrennung.online(ws, spieltag, config, mapping);
            return Optional.empty();
        } catch (GenerateException e) {
            logger.warn("PTM-Online: Spieltag {} online nicht getrennt", spieltag, e);
            return Optional.of(I18n.get("ptmonline.spieltag.trennen.fehlgeschlagen", spieltag, e.getMessage()));
        } catch (IOException e) {
            logger.warn("PTM-Online: Spieltag {} online nicht getrennt", spieltag, e);
            return Optional.of(I18n.get("ptmonline.spieltag.trennen.fehlgeschlagen", spieltag,
                    PtmOnlineFehlerText.fuer(e)));
        } catch (InterruptedException e) {
            // Abbruch nur des Server-Aufrufs: der neue Spieltag steht bereits, lokal wird trotzdem archiviert.
            logger.warn("PTM-Online: Trennen von Spieltag {} abgebrochen", spieltag, e);
            return Optional.of(I18n.get("ptmonline.spieltag.trennen.fehlgeschlagen", spieltag,
                    I18n.get("msg.text.verarbeitung.abgebrochen")));
        }
    }

    private static void zeigeHinweis(WorkingSpreadsheet ws, List<Integer> getrennt, int neuerSpieltag,
            List<String> warnungen) {
        String spieltage = String.join(", ", getrennt.stream().map(String::valueOf).toList());
        List<String> absaetze = new ArrayList<>();
        absaetze.add(I18n.get("ptmonline.spieltag.getrennt", spieltage, neuerSpieltag));
        absaetze.addAll(warnungen);
        MessageBox.from(ws.getxContext(), warnungen.isEmpty() ? MessageBoxTypeEnum.INFO_OK : MessageBoxTypeEnum.WARN_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(String.join("\n\n", absaetze))
                .show();
    }
}
