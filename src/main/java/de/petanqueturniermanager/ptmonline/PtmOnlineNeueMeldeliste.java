/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * „Neue Meldeliste“ in einem mit PTM-Online verbundenen Dokument: Das Anlegen löscht alle Blätter, auch „PTMOnline
 * Sync“. Damit Online nicht weiter eine Bindung an ein Dokument hält, das sie nicht mehr kennt, wird vorher nach
 * Rückfrage ordentlich getrennt (bei Supermêlée jeder verbundene Spieltag). Ist PTM-Online nicht erreichbar, kann auf
 * Nachfrage nur lokal getrennt werden. Online-Turnier und Anmeldungen bleiben erhalten; nach erneutem Verbinden werden
 * die Anmeldungen wieder übernommen.
 */
public final class PtmOnlineNeueMeldeliste {

    private static final Logger logger = LogManager.getLogger(PtmOnlineNeueMeldeliste.class);

    private PtmOnlineNeueMeldeliste() {}

    /**
     * Prüft die Formationssperre und trennt bestehende Verbindungen. Aufzurufen nach dem Parameterdialog, bevor das
     * Dokument geändert wird. Läuft synchron im SheetRunner.
     *
     * @return {@code false}, wenn der Anwender abgebrochen hat – die Meldeliste wird dann nicht angelegt
     * @throws GenerateException wenn die Formation gesperrt ist oder PTM-Online das Trennen ablehnt
     */
    public static boolean vorbereiten(WorkingSpreadsheet ws, TurnierSystem neuesSystem, Formation neueFormation)
            throws GenerateException {
        PtmOnlineFormationsSperre.pruefeAenderung(ws, neuesSystem, neueFormation);
        List<Verbindung> verbindungen = verbindungen(ws);
        if (verbindungen.isEmpty()) {
            return true;
        }
        XComponentContext ctx = ws.getxContext();
        if (!frage(ctx, MessageBoxTypeEnum.WARN_YES_NO, I18n.get("ptmonline.neue_meldeliste.frage.trennen"))) {
            logger.info("PTM-Online: Neue Meldeliste abgebrochen, Verbindung bleibt bestehen");
            return false;
        }
        LibreOfficePtmOnlineSpeicher.Zugangsdaten config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        for (Verbindung verbindung : verbindungen) {
            if (!trenne(ws, config, verbindung)) {
                return false;
            }
        }
        return true;
    }

    /** Verbindungen des Dokuments nach seinem bisherigen Turniersystem – das neue kann ein anderes sein. */
    static List<Verbindung> verbindungen(WorkingSpreadsheet ws) throws GenerateException {
        TurnierSystem bisheriges = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        if (bisheriges == null || bisheriges == TurnierSystem.KEIN) {
            return List.of();
        }
        List<Verbindung> verbindungen = new ArrayList<>();
        for (Integer spieltagNr : spieltage(ws, bisheriges)) {
            PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(ws, bisheriges, spieltagNr);
            if (mapping.getTournamentId().isPresent()) {
                verbindungen.add(new Verbindung(spieltagNr, mapping));
            }
        }
        return verbindungen;
    }

    /** {@code null} steht für die eine Verbindung des Dokuments; bei Supermêlée alle Spieltage mit Sync-Blatt. */
    private static List<Integer> spieltage(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
        if (!turnierSystem.hatMehrereSpielTage()) {
            return Collections.singletonList(null);
        }
        String praefix = SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_SYNC_SPIELTAG_PREFIX;
        return Arrays.stream(SheetMetadataHelper.getSchluesselMitPrefix(ws.getWorkingSpreadsheetDocument(), praefix))
                .map(schluessel -> spieltagNr(schluessel.substring(praefix.length()))).flatMap(Optional::stream)
                .toList();
    }

    private static Optional<Integer> spieltagNr(String rest) {
        String ziffern = rest.replaceFirst(SheetMetadataHelper.SCHLUESSEL_SPIELTAG_RANGLISTE_SUFFIX + "$", "");
        try {
            return Optional.of(Integer.parseInt(ziffern));
        } catch (NumberFormatException e) {
            logger.warn("PTM-Online: unerwarteter Sync-Schlüssel {}", rest, e);
            return Optional.empty();
        }
    }

    /** @return {@code false}, wenn der Anwender das nur lokale Trennen ablehnt */
    private static boolean trenne(WorkingSpreadsheet ws, LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            Verbindung verbindung) throws GenerateException {
        Optional<String> nichtGesendet = trenneOnline(ws, config, verbindung);
        if (nichtGesendet.isPresent() && !frage(ws.getxContext(), MessageBoxTypeEnum.WARN_YES_NO,
                I18n.get("ptmonline.trennen.offline.frage", nichtGesendet.get()))) {
            return false;
        }
        verbindung.mapping().trennen();
        return true;
    }

    /**
     * @return leer, wenn online getrennt; sonst der Grund, warum PTM-Online nicht erreicht wurde
     * @throws GenerateException auch bei Unterbrechung (Stop-Knopf) – die Meldeliste wird dann nicht angelegt
     */
    private static Optional<String> trenneOnline(WorkingSpreadsheet ws,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, Verbindung verbindung) throws GenerateException {
        if (!config.isConfigured()) {
            return Optional.of(I18n.get("ptmonline.fehler.nicht_konfiguriert"));
        }
        try {
            PtmOnlineTrennung.online(ws, verbindung.spieltagNr(), config, verbindung.mapping());
            return Optional.empty();
        } catch (IOException e) {
            logger.warn("PTM-Online: Trennen vor neuer Meldeliste – Server nicht erreichbar", e);
            return Optional.of(PtmOnlineFehlerText.fuer(e));
        } catch (InterruptedException e) {
            // Interrupt-Flag bleibt verbraucht, damit die UNO-Aufräumaufrufe nicht auf einem unterbrochenen Thread laufen.
            logger.debug("PTM-Online: Trennen vor neuer Meldeliste abgebrochen", e);
            throw SheetRunner.verarbeitungAbgebrochen();
        }
    }

    private static boolean frage(XComponentContext ctx, MessageBoxTypeEnum typ, String text) {
        return MessageBox.from(ctx, typ).caption(I18n.get("ptmonline.menu.toplevel")).message(text)
                .show() == MessageBoxResult.YES;
    }

    /** @param spieltagNr {@code null} außer bei Supermêlée */
    record Verbindung(@Nullable Integer spieltagNr, PtmOnlineRegistrationMapping mapping) {}
}
