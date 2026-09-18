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
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Importiert online eingegangene, noch nicht lokal vorhandene Anmeldungen (PTM-Online) in die
 * aktive Meldeliste. Nutzt denselben turniersystem-generischen Schreibpfad wie die Spieler-DB-
 * Integration ({@link MeldelisteZiel#schreibeBlock}, {@link MeldelisteZielFactory#starteMeldelisteUpdate}).
 * <p>
 * {@link #fuehreImportDurch} ist die synchrone Kernlogik: sie darf auf jedem Hintergrund-Thread
 * laufen (Sheet-Schreibzugriffe hier sind reine SheetRunner-Background-Thread-Operationen, siehe
 * CLAUDE.md-Threading-Regel) und wird sowohl vom menuegetriggerten {@link #starte} (eigener
 * Worker-Thread, UI-Feedback per {@link LoMainThread#post}) als auch synchron aus einem bereits
 * laufenden {@code SheetRunner} heraus genutzt (Rundenstart-Hook, {@link PtmOnlineSpielrundeSync}).
 */
public final class RegistrationImportTask {

    private static final Logger logger = LogManager.getLogger(RegistrationImportTask.class);

    private RegistrationImportTask() {}

    public static void starte(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        Optional<MeldelisteZiel> zielOpt = MeldelisteZielFactory.fuerAktivesSheet(ws);
        if (zielOpt.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.keine_meldeliste"));
            return;
        }

        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        PtmOnlineRegistrationMapping mapping;
        Optional<String> tournamentId;
        try {
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            mapping = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr);
            tournamentId = mapping.getTournamentId();
        } catch (GenerateException e) {
            zeigeFehler(ctx, e.getMessage());
            return;
        }
        if (tournamentId.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_angelegt"));
            return;
        }

        MeldelisteZiel ziel = zielOpt.get();
        PtmOnlineRegistrationMapping finaleMapping = mapping;
        String finaleTournamentId = tournamentId.get();

        Thread worker = new Thread(
                () -> importiereUndZeigeErgebnis(ws, ctx, config, finaleMapping, finaleTournamentId, ts, ziel),
                "PTM-Online-Import");
        worker.start();
    }

    private static void importiereUndZeigeErgebnis(WorkingSpreadsheet ws, XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping,
            String tournamentId, TurnierSystem ts, MeldelisteZiel ziel) {
        try {
            int anzahl = fuehreImportDurch(ws, config, mapping, tournamentId, ts, ziel);
            String meldung = anzahl == 0
                    ? I18n.get("ptmonline.erfolg.keine_neuen_anmeldungen")
                    : I18n.get("ptmonline.erfolg.anmeldungen_importiert", anzahl);
            LoMainThread.post(ctx, () -> zeigeInfo(ctx, meldung));
        } catch (IOException e) {
            logger.error("PTM-Online: Anmeldungen importieren fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (GenerateException e) {
            logger.error("PTM-Online: Meldungen-Sheet lesen/schreiben fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeFehler(ctx, e.getMessage()));
        }
    }

    /**
     * Holt neue Online-Anmeldungen, schreibt sie in die Meldeliste und aktualisiert das Mapping.
     * Synchron, blockierend (inkl. Warten auf den angestossenen Meldeliste-Update-Runner) - darf
     * NICHT vom LO-Main-Thread aus aufgerufen werden, wenn der Aufrufer selbst schon auf dem
     * Main-Thread laeuft und dort auf einen SheetRunner wartet (Deadlock-Risiko, siehe
     * {@code LoMainThread}-Dokumentation); auf jedem anderen (Hintergrund-)Thread unkritisch.
     *
     * @return Anzahl tatsaechlich importierter Anmeldungen (0, wenn keine neuen vorlagen oder keine
     *         zur Meldeliste-Formation passte).
     */
    public static int fuehreImportDurch(WorkingSpreadsheet ws, LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            PtmOnlineRegistrationMapping mapping, String tournamentId, TurnierSystem ts, MeldelisteZiel ziel)
            throws IOException, InterruptedException, GenerateException {
        TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
        Instant since = mapping.getLastSync().orElse(Instant.EPOCH);
        List<RegistrationDto> alle = client.fetchRegistrations(tournamentId, since);
        List<RegistrationDto> neue = new ArrayList<>();
        for (RegistrationDto reg : alle) {
            if (!mapping.istBereitsImportiert(reg.id())) {
                neue.add(reg);
            }
        }
        if (neue.isEmpty()) {
            return 0;
        }

        List<RegistrationDto> geschrieben = new ArrayList<>();
        for (RegistrationDto reg : neue) {
            List<SpielerMitVerein> spieler = zuSpielerListe(reg, ziel.getFormation());
            if (spieler == null) {
                logger.warn("PTM-Online: Anmeldung {} passt nicht zur Formation {} der Meldeliste, übersprungen",
                        reg.id(), ziel.getFormation());
                continue;
            }
            try {
                ziel.schreibeBlock(spieler);
                geschrieben.add(reg);
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                logger.error("PTM-Online: Anmeldung {} konnte nicht in die Meldeliste geschrieben werden", reg.id(), e);
            }
        }
        if (geschrieben.isEmpty()) {
            return 0;
        }

        SheetRunner runner = MeldelisteZielFactory.starteMeldelisteUpdate(ws, ts);
        if (runner != null) {
            runner.join();
        }

        Instant jetzt = Instant.now();
        for (RegistrationDto reg : geschrieben) {
            String name = reg.firstName() + " " + reg.lastName();
            int zeile = ziel.findeZeileMitName(name);
            if (zeile > 0) {
                mapping.addMapping(zeile, reg.id(), reg.firstName(), reg.lastName());
            } else {
                logger.warn("PTM-Online: Meldeliste-Zeile für importierte Anmeldung {} nicht gefunden", reg.id());
            }
        }
        mapping.setLastSync(jetzt);
        return geschrieben.size();
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
