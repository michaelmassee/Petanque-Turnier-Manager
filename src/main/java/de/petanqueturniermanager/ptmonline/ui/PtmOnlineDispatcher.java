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

        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        Optional<String> onlineTyp = mapOnlineTyp(ts);
        if (onlineTyp.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.turnier.dialog.fehler.system_nicht_unterstuetzt"));
            return;
        }
        String onlineFormation = mapOnlineFormation(ts, MeldelisteZielFactory.fuerAktivesSheet(ws));

        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(new DocumentPropertiesHelper(ws));
        Optional<String> vorhandeneId = mapping.getTournamentId();
        if (vorhandeneId.isPresent()) {
            pruefeVorhandeneVerknuepfung(ws, ctx, zugangsdaten, mapping, vorhandeneId.get());
            return;
        }

        legeNeuesTurnierAn(ws, ctx, zugangsdaten, mapping, onlineTyp.get(), onlineFormation);
    }

    private static void legeNeuesTurnierAn(WorkingSpreadsheet ws, XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten zugangsdaten, PtmOnlineRegistrationMapping mapping,
            String onlineTyp, String onlineFormation) {
        PtmOnlineTurnierAnlegenDialog.Werte werte = zeigeTurnierAnlegenDialog(ctx);
        if (werte == null) {
            return; // Abgebrochen
        }

        String beschreibung = leerZuNull(werte.beschreibung());
        CreateTournamentDto dto = new CreateTournamentDto(
                werte.name(), werte.datumIso(), werte.startzeitIso(), werte.ort(), beschreibung,
                onlineTyp, onlineFormation, "draft", "private");
        TournamentMetadataDto metadata = new TournamentMetadataDto(
                werte.name(), werte.datumIso(), werte.startzeitIso(), werte.ort(), beschreibung,
                onlineTyp, onlineFormation, "draft", werte.maxAnmeldungen(), null, 0,
                leerZuNull(werte.kontaktName()), leerZuNull(werte.kontaktEmail()), leerZuNull(werte.kontaktTelefon()),
                "private", null, false, false);

        Thread worker = new Thread(
                () -> turnierAnlegenImHintergrund(ws, ctx, zugangsdaten.baseUrl(), zugangsdaten.apiKey(), mapping, dto, metadata),
                "PTM-Online-TurnierAnlegen");
        worker.start();
    }

    /**
     * Eine lokal hinterlegte Turnier-ID kann veraltet sein, wenn das Online-Turnier zwischenzeitlich
     * geloescht wurde. Statt den Nutzer dann dauerhaft mit "bereits angelegt" zu blockieren, wird der
     * Online-Stand geprueft: existiert das Turnier nicht mehr, wird die Verknuepfung entfernt (Nutzer
     * kann "Turnier online anlegen" danach direkt erneut ausfuehren); existiert es noch, werden
     * lokale Metadaten und das "PTM Online"-Info-Sheet aus dem Online-Stand aufgefrischt (kein
     * Duplikat online).
     */
    private static void pruefeVorhandeneVerknuepfung(WorkingSpreadsheet ws, XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten zugangsdaten, PtmOnlineRegistrationMapping mapping,
            String tournamentId) {
        Thread worker = new Thread(() -> {
            try {
                TournamentSyncClient client = new TournamentSyncClient(zugangsdaten.baseUrl(), zugangsdaten.apiKey());
                Optional<TournamentMetadataDto> online = client.fetchTournament(tournamentId);
                if (online.isEmpty()) {
                    LoMainThread.post(ctx, () -> {
                        mapping.clearTournament();
                        MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                                .caption(I18n.get("ptmonline.menu.toplevel"))
                                .message(I18n.get("ptmonline.info.verknuepfung_entfernt", tournamentId))
                                .show();
                    });
                    return;
                }
                LoMainThread.post(ctx, () -> {
                    mapping.setTournamentMetadata(online.get());
                    PtmOnlineInfoSheet.aktualisiereBestEffort(ws, zugangsdaten.baseUrl(), mapping);
                    PtmOnlineInfoSheet.schreibeEckdatenBestEffort(ws, online.get());
                    MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                            .caption(I18n.get("ptmonline.menu.toplevel"))
                            .message(I18n.get("ptmonline.info.bereits_verknuepft", tournamentId))
                            .show();
                });
            } catch (IOException e) {
                logger.error("PTM-Online: Pruefung der vorhandenen Verknuepfung fehlgeschlagen", e);
                LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "PTM-Online-VerknuepfungPruefen");
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
                PtmOnlineInfoSheet.schreibeEckdatenBestEffort(ws, metadata);
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

    /**
     * Überträgt die im Sheet "PTM Online" gepflegten, führenden PTM-Online-Eckdaten bewusst nur auf
     * Nutzeraktion. Der Nutzer kann dort ALLE Felder direkt editieren (siehe {@link PtmOnlineInfoSheet}),
     * daher wird von dort gelesen statt vom zuletzt in DocumentProperties gespeicherten Stand. Die
     * Formation wird zusaetzlich stets frisch aus Turniersystem/Meldeliste neu berechnet (nicht der im
     * Sheet stehende Wert übernommen) — so korrigiert dieser Abgleich auch ein online abweichend
     * hinterlegtes {@code formation} (z.B. Supermelee faelschlich als "doublette").
     */
    public static void eckdatenNachOnlineUebertragen(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        var zugangsdaten = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(new DocumentPropertiesHelper(ws));
        Optional<String> tournamentId = mapping.getTournamentId();
        Optional<TournamentMetadataDto> gespeichert = PtmOnlineInfoSheet.leseEckdaten(ws)
                .or(mapping::getTournamentMetadata);
        if (!zugangsdaten.isConfigured() || tournamentId.isEmpty() || gespeichert.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_angelegt"));
            return;
        }
        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        String onlineFormation = mapOnlineFormation(ts, MeldelisteZielFactory.fuerAktivesSheet(ws));
        TournamentMetadataDto metadata = mitFormation(gespeichert.get(), onlineFormation);
        new Thread(() -> {
            try {
                new TournamentSyncClient(zugangsdaten.baseUrl(), zugangsdaten.apiKey())
                        .pushTournamentMetadata(tournamentId.get(), metadata);
                LoMainThread.post(ctx, () -> {
                    mapping.setTournamentMetadata(metadata);
                    PtmOnlineInfoSheet.aktualisiereBestEffort(ws, zugangsdaten.baseUrl(), mapping);
                    PtmOnlineInfoSheet.schreibeEckdatenBestEffort(ws, metadata);
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

    private static TournamentMetadataDto mitFormation(TournamentMetadataDto alt, String formation) {
        if (alt.formation().equals(formation)) {
            return alt;
        }
        return new TournamentMetadataDto(
                alt.name(), alt.date(), alt.startTime(), alt.location(), alt.description(), alt.type(),
                formation, alt.status(), alt.maxRegistrations(), alt.registrationDeadline(), alt.entryFeeCents(),
                alt.contactName(), alt.contactEmail(), alt.contactPhone(), alt.visibility(), alt.internalNotes(),
                alt.participantsPublic(), alt.licenseRequired());
    }

	/**
	 * Ändert die "wichtigsten" Grunddaten per Dialog; alle übrigen Felder bleiben unangetastet
	 * editierbar direkt im Sheet "PTM Online" (siehe {@link PtmOnlineInfoSheet}). Der Online-Abgleich
	 * bleibt bewusst explizit ({@link #eckdatenNachOnlineUebertragen}).
	 */
	public static void eckdatenBearbeiten(WorkingSpreadsheet ws) {
		XComponentContext ctx = ws.getxContext();
		PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(new DocumentPropertiesHelper(ws));
		if (mapping.getTournamentId().isEmpty()) {
			zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_angelegt"));
			return;
		}
		TournamentMetadataDto alt = PtmOnlineInfoSheet.leseEckdaten(ws).or(mapping::getTournamentMetadata)
				.orElseGet(() -> new TournamentMetadataDto("", "", "", "", null, "", "", "draft", 0, null, 0,
						null, null, null, "private", null, false, false));
		PtmOnlineTurnierAnlegenDialog.Werte werte = zeigeTurnierAnlegenDialog(ctx,
				new PtmOnlineTurnierAnlegenDialog.Werte(alt.name(), alt.date(), alt.startTime(), alt.location(),
						nullZuLeer(alt.description()), nullZuLeer(alt.contactName()), nullZuLeer(alt.contactEmail()),
						nullZuLeer(alt.contactPhone()), alt.maxRegistrations()));
		if (werte == null) {
			return;
		}
		TournamentMetadataDto neu = new TournamentMetadataDto(
				werte.name(), werte.datumIso(), werte.startzeitIso(), werte.ort(), leerZuNull(werte.beschreibung()),
				alt.type(), alt.formation(), alt.status(), werte.maxAnmeldungen(), alt.registrationDeadline(),
				alt.entryFeeCents(), leerZuNull(werte.kontaktName()), leerZuNull(werte.kontaktEmail()),
				leerZuNull(werte.kontaktTelefon()), alt.visibility(), alt.internalNotes(),
				alt.participantsPublic(), alt.licenseRequired());
		mapping.setTournamentMetadata(neu);
		PtmOnlineInfoSheet.schreibeEckdatenBestEffort(ws, neu);
	}

	private static String nullZuLeer(String wert) {
		return wert == null ? "" : wert;
	}

	private static String leerZuNull(String wert) {
		return wert == null || wert.isBlank() ? null : wert;
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
     * PTM-Online-API zu (dort nur {@code tete}/{@code doublette}/{@code triplette}). Supermelee hat
     * lokal die technische Formation {@link Formation#MELEE} (keine feste Teamgroesse), spielt aber
     * immer als Einzelspieler — dort wird die Meldeliste-Formation bewusst ignoriert und immer
     * "tete" gemeldet. Ist ansonsten noch keine Meldeliste aktiv oder eine nicht direkt abbildbare
     * Formation (MELEE/NUR_TEAMNAME) gesetzt, wird defensiv "doublette" als haeufigster Fall
     * angenommen — reine Anzeige-Metadaten online, ohne Einfluss auf die lokale Spiellogik.
     */
    private static String mapOnlineFormation(TurnierSystem ts, Optional<MeldelisteZiel> ziel) {
        if (ts == TurnierSystem.SUPERMELEE) {
            return "tete";
        }
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
