package de.petanqueturniermanager.webserver;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.FormuleXStatusLeser;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJStatusLeser;
import de.petanqueturniermanager.kaskade.KaskadeStatusLeser;
import de.petanqueturniermanager.ko.KoStatusLeser;
import de.petanqueturniermanager.liga.spielplan.LigaStatusLeser;
import de.petanqueturniermanager.maastrichter.MaastrichterStatusLeser;
import de.petanqueturniermanager.poule.PouleStatusLeser;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerPropertiesSpalte;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Liefert pro Turniersystem einen kurzen Status-Text ("Spielrunde 3", "Turnier beendet", …).
 * Genutzt von Sidebar (InfoSidebarContent) und Startseite-Webserver — die Logik ist hier
 * zentralisiert, damit beide UIs identische Texte zeigen.
 */
public final class TurnierStatusErmittler {

    private TurnierStatusErmittler() {
    }

    public static String ermitteln(WorkingSpreadsheet ws) {
        if (ws == null) {
            return "";
        }
        var docPropHelper = new DocumentPropertiesHelper(ws);
        TurnierSystem system = docPropHelper.getTurnierSystemAusDocument();
        if (system == null) {
            return "";
        }
        return switch (system) {
            case SUPERMELEE -> {
                int spieltag = docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 1);
                var xDoc = ws.getWorkingSpreadsheetDocument();
                String spielrundeSheetName = I18n.get("sheet.name.supermelee.spielrunde.muster", spieltag, 1);
                var spielrundeSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
                        SheetMetadataHelper.schluesselSupermeleeSpielrunde(spieltag, 1),
                        spielrundeSheetName);
                if (spielrundeSheet == null) {
                    yield I18n.get("sidebar.info.meldungen.erfassen");
                }
                int runde = docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, 1);
                yield I18n.get("sidebar.info.supermelee.schritt", spieltag, runde);
            }
            case SCHWEIZER -> {
                int runde = docPropHelper.getIntProperty(SchweizerPropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, 1);
                yield I18n.get("sidebar.info.spielrunde", runde);
            }
            case LIGA -> {
                var status = LigaStatusLeser.von(ws).liesStatus();
                if (!status.spielplanVorhanden()) {
                    yield I18n.get("sidebar.info.meldungen.erfassen");
                }
                if (status.alleGespielt()) {
                    yield I18n.get("sidebar.info.turnier.beendet");
                }
                yield I18n.get("sidebar.info.liga.schritt",
                        status.hrGespielt(), status.hrGesamt(),
                        status.rrGespielt(), status.rrGesamt());
            }
            case JGJ -> {
                var status = JGJStatusLeser.von(ws).liesStatus();
                if (!status.spielplanVorhanden()) {
                    yield I18n.get("sidebar.info.meldungen.erfassen");
                }
                if (status.alleGespielt()) {
                    yield I18n.get("sidebar.info.turnier.beendet");
                }
                if (status.finalrundeVorhanden()) {
                    yield I18n.get("sidebar.jgj.finalrunde");
                }
                yield I18n.get("sidebar.info.jgj.schritt",
                        status.hrGespielt(), status.hrGesamt(),
                        status.rrGespielt(), status.rrGesamt());
            }
            case POULE -> {
                var status = PouleStatusLeser.von(ws).liesStatus();
                if (!status.vorrundeVorhanden()) {
                    yield I18n.get("sidebar.info.meldungen.erfassen");
                }
                if (status.beendet()) {
                    yield I18n.get("sidebar.info.turnier.beendet");
                }
                if (status.koVorhanden()) {
                    yield I18n.get("sidebar.poule.ko");
                }
                yield I18n.get("sidebar.info.poule.vorrunde",
                        status.vorrundeGespielt(), status.vorrundeGesamt());
            }
            case MAASTRICHTER -> {
                var status = MaastrichterStatusLeser.von(ws).liesStatus();
                if (!status.vorrundeVorhanden()) {
                    yield I18n.get("sidebar.info.meldungen.erfassen");
                }
                if (status.beendet()) {
                    yield I18n.get("sidebar.info.turnier.beendet");
                }
                if (status.finalrundeVorhanden()) {
                    yield I18n.get("sidebar.maastrichter.finalrunde");
                }
                yield I18n.get("sidebar.info.maastrichter.vorrunde",
                        status.aktuelleVorrundeNr(),
                        status.vorrundeGespielt(), status.vorrundeGesamt());
            }
            case KASKADE -> {
                var status = KaskadeStatusLeser.von(ws).liesStatus();
                if (!status.rundeVorhanden()) {
                    yield I18n.get("sidebar.info.meldungen.erfassen");
                }
                if (status.beendet()) {
                    yield I18n.get("sidebar.info.turnier.beendet");
                }
                if (status.koPhaseVorhanden()) {
                    yield I18n.get("sidebar.kaskade.ko.felder");
                }
                yield I18n.get("sidebar.info.kaskade.runde",
                        status.aktuelleRundeNr(),
                        status.rundeGespielt(), status.rundeGesamt());
            }
            case KO -> {
                var status = KoStatusLeser.von(ws).liesStatus();
                if (!status.turnierbaumVorhanden()) {
                    yield I18n.get("sidebar.info.meldungen.erfassen");
                }
                if (status.beendet()) {
                    yield I18n.get("sidebar.info.turnier.beendet");
                }
                yield I18n.get("sidebar.info.ko.laeuft");
            }
            case FORMULEX -> {
                var status = FormuleXStatusLeser.von(ws).liesStatus();
                if (!status.spielrundeVorhanden()) {
                    yield I18n.get("sidebar.info.meldungen.erfassen");
                }
                if (status.beendet()) {
                    yield I18n.get("sidebar.info.turnier.beendet");
                }
                yield I18n.get("sidebar.info.formulex.schritt",
                        status.aktuelleRundeNr(), status.anzahlRunden(),
                        status.rundeGespielt(), status.rundeGesamt());
            }
            default -> "";
        };
    }

    /**
     * Lokalisierte Bezeichnung des aktiven Turniersystems oder leer, wenn kein System gesetzt ist.
     */
    public static String turniersystemBezeichnung(WorkingSpreadsheet ws) {
        if (ws == null) {
            return "";
        }
        TurnierSystem system = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        return (system != null) ? system.getBezeichnung() : "";
    }
}
