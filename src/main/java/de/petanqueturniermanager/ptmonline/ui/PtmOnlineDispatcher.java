/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.RegistrationImportTask;
import de.petanqueturniermanager.ptmonline.ResultExportTask;
import de.petanqueturniermanager.ptmonline.TournamentSyncClient;
import de.petanqueturniermanager.ptmonline.dto.CreateTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.TournamentMetadataDto;
import de.petanqueturniermanager.ptmonline.sheet.PtmOnlineInfoSheet;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/**
 * Bindeglied zwischen {@link de.petanqueturniermanager.comp.ProtocolHandler} und der PTM-Online-
 * REST-Anbindung. Muster analog {@link de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher}:
 * statische Methoden, Fehler werden als {@link MessageBox} gemeldet statt zu crashen. Netzwerk-I/O
 * laeuft off-thread (Dispatch-Aufrufkette darf nicht blockieren); Sheet-/Dokument-Schreibzugriffe
 * werden per {@link LoMainThread#post} zurueck auf den Main-Thread marshalliert.
 */
public final class PtmOnlineDispatcher {

    private static final Logger logger = LogManager.getLogger(PtmOnlineDispatcher.class);

    private PtmOnlineDispatcher() {}

    public static void turnierOnlineAnlegen(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        var zugangsdaten = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!zugangsdaten.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(new DocumentPropertiesHelper(ws));
        Optional<String> vorhandeneId = mapping.getTournamentId();
        if (vorhandeneId.isPresent()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_bereits_angelegt", vorhandeneId.get()));
            return;
        }

        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        Optional<String> onlineTyp = mapOnlineTyp(ts);
        if (onlineTyp.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.turnier.dialog.fehler.system_nicht_unterstuetzt"));
            return;
        }
        String onlineFormation = mapOnlineFormation(MeldelisteZielFactory.fuerAktivesSheet(ws));

        PtmOnlineTurnierAnlegenDialog.Werte werte = zeigeTurnierAnlegenDialog(ctx);
        if (werte == null) {
            return; // Abgebrochen
        }

        CreateTournamentDto dto = new CreateTournamentDto(
                werte.name(), werte.datumIso(), werte.startzeitIso(), werte.ort(), null,
                onlineTyp.get(), onlineFormation, "draft", "private");
        TournamentMetadataDto metadata = new TournamentMetadataDto(
                werte.name(), werte.datumIso(), werte.startzeitIso(), werte.ort(), null,
                onlineTyp.get(), onlineFormation, "draft", 0, null, 0, null, null, null,
                "private", null, false, false);

        Thread worker = new Thread(
                () -> turnierAnlegenImHintergrund(ws, ctx, zugangsdaten.baseUrl(), zugangsdaten.apiKey(), mapping, dto, metadata),
                "PTM-Online-TurnierAnlegen");
        worker.start();
    }

    private static PtmOnlineTurnierAnlegenDialog.Werte zeigeTurnierAnlegenDialog(XComponentContext ctx) {
		return zeigeTurnierAnlegenDialog(ctx, null);
	}

	private static PtmOnlineTurnierAnlegenDialog.Werte zeigeTurnierAnlegenDialog(XComponentContext ctx,
			PtmOnlineTurnierAnlegenDialog.Werte vorbesetzung) {
        ProcessBox pb = ProcessBox.from();
        boolean warSichtbar = pb.istSichtbar();
        if (warSichtbar) {
            pb.hide();
        }
        try {
            return new PtmOnlineTurnierAnlegenDialog(ctx, null, vorbesetzung).zeigen();
        } catch (com.sun.star.uno.Exception | RuntimeException e) {
            logger.error("PTM-Online-Turnier-anlegen-Dialog fehlgeschlagen", e);
            return null;
        } finally {
            if (warSichtbar) {
                pb.visibleWennAutomatisch();
            }
        }
    }

    private static void turnierAnlegenImHintergrund(WorkingSpreadsheet ws,
            XComponentContext ctx, String baseUrl, String apiKey, PtmOnlineRegistrationMapping mapping,
            CreateTournamentDto dto, TournamentMetadataDto metadata) {
        try {
            TournamentSyncClient client = new TournamentSyncClient(baseUrl, apiKey);
            String tournamentId = client.createTournament(dto);
            client.pushTournamentMetadata(tournamentId, metadata);
            LoMainThread.post(ctx, () -> {
                mapping.setTournamentId(tournamentId);
                mapping.setTournamentMetadata(metadata);
                PtmOnlineInfoSheet.aktualisiereBestEffort(ws, baseUrl, mapping);
                MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                        .caption(I18n.get("ptmonline.menu.toplevel"))
                        .message(I18n.get("ptmonline.erfolg.turnier_angelegt", tournamentId))
                        .show();
            });
        } catch (IOException e) {
            logger.error("PTM-Online: Turnier anlegen fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void anmeldungenImportieren(WorkingSpreadsheet ws) {
        RegistrationImportTask.starte(ws);
    }

    public static void ergebnisseExportieren(WorkingSpreadsheet ws) {
        ResultExportTask.starte(ws);
    }

    /** Überträgt die im Dokument gespeicherten, führenden PTM-Online-Eckdaten bewusst nur auf Nutzeraktion. */
    public static void eckdatenNachOnlineUebertragen(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        var zugangsdaten = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(new DocumentPropertiesHelper(ws));
        Optional<String> tournamentId = mapping.getTournamentId();
        Optional<TournamentMetadataDto> metadata = mapping.getTournamentMetadata();
        if (!zugangsdaten.isConfigured() || tournamentId.isEmpty() || metadata.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_angelegt"));
            return;
        }
        new Thread(() -> {
            try {
                new TournamentSyncClient(zugangsdaten.baseUrl(), zugangsdaten.apiKey())
                        .pushTournamentMetadata(tournamentId.get(), metadata.get());
                LoMainThread.post(ctx, () -> {
                    PtmOnlineInfoSheet.aktualisiereBestEffort(ws, zugangsdaten.baseUrl(), mapping);
                    MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK).caption(I18n.get("ptmonline.menu.toplevel"))
                            .message(I18n.get("ptmonline.erfolg.turnier_angelegt", tournamentId.get())).show();
                });
            } catch (IOException e) {
                logger.error("PTM-Online: Eckdatenabgleich fehlgeschlagen", e);
                LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "PTM-Online-EckdatenAbgleich").start();
    }

	/** Ändert die im Dokument gespeicherten Grunddaten; der Online-Abgleich bleibt bewusst explizit. */
	public static void eckdatenBearbeiten(WorkingSpreadsheet ws) {
		XComponentContext ctx = ws.getxContext();
		PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(new DocumentPropertiesHelper(ws));
		Optional<TournamentMetadataDto> bisher = mapping.getTournamentMetadata();
		if (bisher.isEmpty()) {
			zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_angelegt"));
			return;
		}
		TournamentMetadataDto alt = bisher.get();
		PtmOnlineTurnierAnlegenDialog.Werte werte = zeigeTurnierAnlegenDialog(ctx,
				new PtmOnlineTurnierAnlegenDialog.Werte(alt.name(), alt.date(), alt.startTime(), alt.location()));
		if (werte == null) {
			return;
		}
		mapping.setTournamentMetadata(new TournamentMetadataDto(
				werte.name(), werte.datumIso(), werte.startzeitIso(), werte.ort(), alt.description(), alt.type(),
				alt.formation(), alt.status(), alt.maxRegistrations(), alt.registrationDeadline(), alt.entryFeeCents(),
				alt.contactName(), alt.contactEmail(), alt.contactPhone(), alt.visibility(), alt.internalNotes(),
				alt.participantsPublic(), alt.licenseRequired()));
	}

    /** Ordnet das lokale Turniersystem dem passenden {@code type}-Wert der PTM-Online-API zu. */
    private static Optional<String> mapOnlineTyp(TurnierSystem ts) {
        String typ = switch (ts) {
            case SUPERMELEE -> "supermelee";
            case LIGA -> "liga";
            case MAASTRICHTER -> "maastrichter";
            case SCHWEIZER -> "schweizer";
            case JGJ -> "jeder_gegen_jeden";
            case KO -> "ko";
            case POULE -> "poule_ab";
            case KASKADE -> "kaskaden";
            case FORMULEX -> "formule_x";
            case TRIPTETE -> "trip_tete";
            case KEIN -> null;
        };
        return Optional.ofNullable(typ);
    }

    /**
     * Ordnet die lokale Formation der Meldeliste dem passenden {@code formation}-Wert der
     * PTM-Online-API zu (dort nur {@code tete}/{@code doublette}/{@code triplette}). Ist noch keine
     * Meldeliste aktiv oder eine nicht direkt abbildbare Formation (MELEE/NUR_TEAMNAME) gesetzt,
     * wird defensiv "doublette" als haeufigster Fall angenommen — reine Anzeige-Metadaten online,
     * ohne Einfluss auf die lokale Spiellogik.
     */
    private static String mapOnlineFormation(Optional<MeldelisteZiel> ziel) {
        Formation formation = ziel.map(MeldelisteZiel::getFormation).orElse(Formation.DOUBLETTE);
        return switch (formation) {
            case TETE -> "tete";
            case TRIPLETTE -> "triplette";
            case DOUBLETTE, MELEE, NUR_TEAMNAME -> "doublette";
        };
    }

    private static void zeigeNetzwerkFehler(XComponentContext ctx, IOException e) {
        String meldung = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        String key = meldung.contains(" 401") ? "ptmonline.fehler.nicht_freigeschaltet" : "ptmonline.fehler.netzwerk";
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(key.equals("ptmonline.fehler.netzwerk") ? I18n.get(key, meldung) : I18n.get(key))
                .show();
    }

    private static void zeigeFehler(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }
}
