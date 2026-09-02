/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.sheet.PtmOnlineInfoSheet;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Importiert online eingegangene, noch nicht lokal vorhandene Anmeldungen (PTM-Online) in die
 * aktive Meldeliste. Nutzt denselben turniersystem-generischen Schreibpfad wie die Spieler-DB-
 * Integration ({@link MeldelisteZiel#schreibeBlock}, {@link MeldelisteZielFactory#starteMeldelisteUpdate}).
 */
public final class RegistrationImportTask {

    private static final Logger logger = LogManager.getLogger(RegistrationImportTask.class);

    /** Online-Anmeldung zusammen mit der beim Schreiben eindeutig bestimmten Meldelistenzeile. */
    private record GeschriebeneAnmeldung(RegistrationDto anmeldung, int meldelistenZeile) {
    }

    private RegistrationImportTask() {}

    public static void starte(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        var zugangsdaten = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!zugangsdaten.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        Optional<MeldelisteZiel> zielOpt = MeldelisteZielFactory.fuerAktivesSheet(ws);
        if (zielOpt.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.keine_meldeliste"));
            return;
        }

        DocumentPropertiesHelper docProps = new DocumentPropertiesHelper(ws);
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(docProps);
        Optional<String> tournamentId = mapping.getTournamentId();
        if (tournamentId.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_angelegt"));
            return;
        }

        TurnierSystem ts = docProps.getTurnierSystemAusDocument();
        MeldelisteZiel ziel = zielOpt.get();

        Thread worker = new Thread(
                () -> importiereImHintergrund(
                        ws, ctx, zugangsdaten.baseUrl(), zugangsdaten.apiKey(), mapping, tournamentId.get(), ts, ziel),
                "PTM-Online-Import");
        worker.start();
    }

    private static void importiereImHintergrund(WorkingSpreadsheet ws, XComponentContext ctx, String baseUrl, String apiKey,
            PtmOnlineRegistrationMapping mapping, String tournamentId, TurnierSystem ts, MeldelisteZiel ziel) {
        List<RegistrationDto> neue;
        // Der nächste Abruf beginnt an diesem Zeitpunkt. Er liegt bewusst VOR dem HTTP-Request,
        // damit eine während des Abrufs eingegangene Anmeldung nicht zwischen zwei Syncs verloren geht.
        Instant abgleichStart = Instant.now();
        try {
            TournamentSyncClient client = new TournamentSyncClient(baseUrl, apiKey);
            Instant since = mapping.getLastSync().orElse(Instant.EPOCH);
            List<RegistrationDto> alle = client.fetchRegistrations(tournamentId, since);
            neue = alle.stream().filter(r -> !mapping.istBereitsImportiert(r.id())).toList();
        } catch (IOException e) {
            logger.error("PTM-Online: Anmeldungen abrufen fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (neue.isEmpty()) {
            LoMainThread.post(ctx, () -> {
                mapping.setLastSync(abgleichStart);
                PtmOnlineInfoSheet.aktualisiereBestEffort(ws, baseUrl, mapping);
                zeigeInfo(ctx, I18n.get("ptmonline.erfolg.keine_neuen_anmeldungen"));
            });
            return;
        }

        LoMainThread.post(ctx, () -> schreibeUndAktualisiere(ws, ctx, baseUrl, mapping, ts, ziel, neue, abgleichStart));
    }

    /** Laeuft auf dem Main-Thread: schreibt neue Bloecke, stoesst den Update-Lauf an. */
    private static void schreibeUndAktualisiere(WorkingSpreadsheet ws, XComponentContext ctx, String baseUrl,
            PtmOnlineRegistrationMapping mapping, TurnierSystem ts, MeldelisteZiel ziel, List<RegistrationDto> neue,
            Instant abgleichStart) {
        List<GeschriebeneAnmeldung> geschrieben = new ArrayList<>();
        for (RegistrationDto reg : neue) {
            List<SpielerMitVerein> spieler = zuSpielerListe(reg, ziel.getFormation());
            if (spieler == null) {
                logger.warn("PTM-Online: Anmeldung {} passt nicht zur Formation {} der Meldeliste, übersprungen",
                        reg.id(), ziel.getFormation());
                continue;
            }
            try {
                int meldelistenZeile = ziel.schreibeBlockUndLiefereZeile(spieler);
                geschrieben.add(new GeschriebeneAnmeldung(reg, meldelistenZeile));
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                logger.error("PTM-Online: Anmeldung {} konnte nicht in die Meldeliste geschrieben werden", reg.id(), e);
            }
        }

        if (geschrieben.isEmpty()) {
            zeigeInfo(ctx, I18n.get("ptmonline.erfolg.keine_neuen_anmeldungen"));
            return;
        }

        SheetRunner runner = MeldelisteZielFactory.starteMeldelisteUpdate(ws, ts);
        Thread abschluss = new Thread(
                () -> warteAufAbschlussUndAktualisiereMapping(ws, ctx, baseUrl, mapping, ziel, geschrieben, neue, abgleichStart, runner),
                "PTM-Online-ImportAbschluss");
        abschluss.start();
    }

    /**
     * Wartet (auf einem eigenen Hintergrund-Thread, NICHT dem Main-Thread) auf das Ende des
     * Meldeliste-Aktualisieren-Laufs, der erst die Team-Nr vergibt — analog zum
     * start()+join()-Muster fuer synchrone Abhaengigkeit von einem SheetRunner-Ergebnis.
     * Ein join() auf dem Main-Thread waere hier riskant, falls der Runner intern selbst
     * per LoMainThread.post zurueckmarshalliert (Deadlock-Gefahr).
     */
    private static void warteAufAbschlussUndAktualisiereMapping(WorkingSpreadsheet ws, XComponentContext ctx, String baseUrl,
            PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel, List<GeschriebeneAnmeldung> geschrieben,
            List<RegistrationDto> neue, Instant abgleichStart, @Nullable SheetRunner runner) {
        if (runner != null) {
            try {
                runner.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        LoMainThread.post(ctx, () -> aktualisiereMappingUndZeigeErfolg(
                ws, ctx, baseUrl, mapping, ziel, geschrieben, neue, abgleichStart));
    }

    private static void aktualisiereMappingUndZeigeErfolg(WorkingSpreadsheet ws, XComponentContext ctx, String baseUrl,
            PtmOnlineRegistrationMapping mapping, MeldelisteZiel ziel, List<GeschriebeneAnmeldung> geschrieben,
            List<RegistrationDto> neue, Instant abgleichStart) {
        int erfolgreichGemappt = 0;
        for (GeschriebeneAnmeldung geschriebenAnmeldung : geschrieben) {
            RegistrationDto reg = geschriebenAnmeldung.anmeldung();
            int teamNr = ziel.getTeamNrAusZeile(geschriebenAnmeldung.meldelistenZeile());
            if (teamNr > 0) {
                mapping.addMapping(teamNr, reg.id());
                erfolgreichGemappt++;
            } else {
                logger.warn("PTM-Online: Team-Nr. für importierte Anmeldung {} nicht gefunden", reg.id());
            }
        }
        if (erfolgreichGemappt == neue.size()) {
            mapping.setLastSync(abgleichStart);
        } else {
            logger.warn("PTM-Online: Sync-Fortschritt bleibt unverändert, damit {} nicht importierte Anmeldungen erneut abgerufen werden",
                    neue.size() - erfolgreichGemappt);
        }
        PtmOnlineInfoSheet.aktualisiereBestEffort(ws, baseUrl, mapping);
        zeigeInfo(ctx, I18n.get("ptmonline.erfolg.anmeldungen_importiert", geschrieben.size()));
    }

    /**
     * Baut die Spielerliste passend zur Meldeliste-Formation. Liefert {@code null}, wenn die
     * Registrierung fuer die geforderte Spielerzahl nicht genug ausgefuellte Namen mitbringt
     * (defensiv statt Absturz — z.B. Doublette-Meldeliste, aber Anmeldung ohne Partnername).
     */
    private static @Nullable List<SpielerMitVerein> zuSpielerListe(RegistrationDto reg, Formation formation) {
        if (istLeer(reg.firstName()) || istLeer(reg.lastName())) {
            return null;
        }
        List<SpielerMitVerein> spieler = new ArrayList<>();
        spieler.add(neuerSpieler(reg.firstName(), reg.lastName(), reg.club(), reg.licenseNr()));

        if (formation.getAnzSpieler() >= 2) {
            if (istLeer(reg.partnerFirstName()) || istLeer(reg.partnerLastName())) {
                return null;
            }
            spieler.add(neuerSpieler(reg.partnerFirstName(), reg.partnerLastName(), reg.club(), null));
        }
        if (formation.getAnzSpieler() >= 3) {
            if (istLeer(reg.partner2FirstName()) || istLeer(reg.partner2LastName())) {
                return null;
            }
            spieler.add(neuerSpieler(reg.partner2FirstName(), reg.partner2LastName(), reg.club(), null));
        }
        return spieler;
    }

    private static boolean istLeer(@Nullable String wert) {
        return wert == null || wert.isBlank();
    }

    /** {@code nr=0}: Online-Anmeldungen haben keinen Bezug zu einem lokalen Spieler-DB-Datensatz. */
    private static SpielerMitVerein neuerSpieler(String vorname, String nachname, @Nullable String vereinName, @Nullable String lizenznr) {
        return new SpielerMitVerein(0, vorname, nachname, null, vereinName, List.of(), List.of(), lizenznr);
    }

    private static void zeigeNetzwerkFehler(XComponentContext ctx, IOException e) {
        String meldung = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        boolean nichtFreigeschaltet = meldung.contains(" 401");
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(nichtFreigeschaltet
                        ? I18n.get("ptmonline.fehler.nicht_freigeschaltet")
                        : I18n.get("ptmonline.fehler.netzwerk", meldung))
                .show();
    }

    private static void zeigeFehler(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }

    private static void zeigeInfo(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(meldung)
                .show();
    }
}
