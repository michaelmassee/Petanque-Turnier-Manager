/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetNew;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetNew;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetNew;

public final class KiActionRegistry {

    static final String ACTION_NEUES_TURNIER = "new_tournament";
    static final String ACTION_CUSTOM_SHEET = "custom_sheet";
    private static final Set<String> ERLAUBTE_ACTIONS = Set.of(ACTION_NEUES_TURNIER, ACTION_CUSTOM_SHEET);

    private KiActionRegistry() {
    }

    public static void validiere(KiPlan plan) {
        for (KiAktion aktion : plan.actions()) {
            if (!ERLAUBTE_ACTIONS.contains(aktion.type())) {
                throw new IllegalArgumentException("Nicht erlaubte KI-Aktion: " + aktion.type());
            }
            if (ACTION_NEUES_TURNIER.equals(aktion.type())) {
                turnierSystem(aktion);
            } else if (ACTION_CUSTOM_SHEET.equals(aktion.type())) {
                CustomSheetDaten.from(aktion);
            }
        }
    }

    public static String beschreibe(KiPlan plan) {
        StringBuilder out = new StringBuilder();
        out.append(plan.summary().isBlank() ? I18n.get("ki.dialog.aktionsplan.default.summary") : plan.summary())
                .append("\n\n");
        int nr = 1;
        for (KiAktion aktion : plan.actions()) {
            out.append(nr++).append(". ");
            if (ACTION_NEUES_TURNIER.equals(aktion.type())) {
                out.append(I18n.get("ki.dialog.aktionsplan.neues.turnier", turnierSystem(aktion).getBezeichnung()));
            } else if (ACTION_CUSTOM_SHEET.equals(aktion.type())) {
                out.append(I18n.get("ki.dialog.aktionsplan.zusatz.sheet", CustomSheetDaten.from(aktion).name()));
            }
            out.append('\n');
        }
        if (!plan.warnings().isEmpty()) {
            out.append('\n').append(I18n.get("ki.dialog.aktionsplan.hinweise")).append('\n');
            plan.warnings().forEach(w -> out.append("- ").append(w).append('\n'));
        }
        if (!plan.dataPreview().isBlank()) {
            out.append('\n').append(I18n.get("ki.dialog.aktionsplan.vorschau")).append('\n').append(plan.dataPreview());
        }
        return out.toString();
    }

    public static void ausfuehrenNachBestaetigung(WorkingSpreadsheet ws, KiPlan plan) {
        validiere(plan);
        if (plan.actions().isEmpty()) {
            MessageBox.from(ws, MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("ki.dialog.antwort.titel"))
                    .message(beschreibe(plan))
                    .show();
            return;
        }
        MessageBoxResult result = MessageBox.from(ws, MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("ki.dialog.aktionsplan.titel"))
                .message(beschreibe(plan) + "\n\n" + I18n.get("ki.dialog.aktionsplan.jetzt.ausfuehren"))
                .show();
        if (result != MessageBoxResult.YES) {
            return;
        }
        for (KiAktion aktion : plan.actions()) {
            if (ACTION_NEUES_TURNIER.equals(aktion.type())) {
                starteNeuesTurnier(ws, turnierSystem(aktion));
            }
        }
        List<CustomSheetDaten> customSheets = plan.actions().stream()
                .filter(a -> ACTION_CUSTOM_SHEET.equals(a.type()))
                .map(CustomSheetDaten::from)
                .toList();
        if (!customSheets.isEmpty()) {
            new Thread(() -> {
                warteAufFreienRunner();
                new KiCustomSheetsRunner(ws, customSheets).start();
            }, "PTM-KI-CustomSheets").start();
        }
    }

    private static void starteNeuesTurnier(WorkingSpreadsheet ws, TurnierSystem system) {
        try {
            SheetRunner runner = switch (system) {
                case SUPERMELEE -> new MeldeListeSheet_New(ws).testKeinAnderesTurnierVorhanden();
                case SCHWEIZER -> new SchweizerMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden();
                case MAASTRICHTER -> new MaastrichterMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden();
                case POULE -> new PouleMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden();
                case LIGA -> new LigaMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden();
                case JGJ -> new JGJMeldeListeSheet_New(ws).testKeinAnderesTurnierVorhanden();
                case KO -> new KoMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden();
                case KASKADE -> new KaskadeMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden();
                case FORMULEX -> new FormuleXMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden();
                case TRIPTETE -> new TripTeteMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden();
                default -> throw new IllegalArgumentException("Turniersystem nicht unterstützt: " + system);
            };
            runner.start();
        } catch (GenerateException e) {
            MessageBox.from(ws, MessageBoxTypeEnum.WARN_OK)
                    .caption(I18n.get("ki.dialog.aktion.abgebrochen.titel"))
                    .message(e.getMessage())
                    .show();
        }
    }

    private static void warteAufFreienRunner() {
        while (SheetRunner.isRunning()) {
            try {
                Thread.sleep(250L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static TurnierSystem turnierSystem(KiAktion aktion) {
        String system = stringParam(aktion.parameters(), "system", aktion.target());
        String norm = system.toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
        return switch (norm) {
            case "SUPERMELEE", "SUPERMELÉE" -> TurnierSystem.SUPERMELEE;
            case "SCHWEIZER", "SCHWEIZERSYSTEM" -> TurnierSystem.SCHWEIZER;
            case "MAASTRICHTER", "MAASTRICHT" -> TurnierSystem.MAASTRICHTER;
            case "POULE", "POULEAB" -> TurnierSystem.POULE;
            case "LIGA" -> TurnierSystem.LIGA;
            case "JGJ", "JEDERGEGENJEDEN" -> TurnierSystem.JGJ;
            case "KO", "KOTURNIER", "KOSYSTEM" -> TurnierSystem.KO;
            case "KASKADE", "KASKADENKO" -> TurnierSystem.KASKADE;
            case "FORMULEX", "FORMULE" -> TurnierSystem.FORMULEX;
            case "TRIPTETE", "TRIPLETE" -> TurnierSystem.TRIPTETE;
            default -> throw new IllegalArgumentException("Unbekanntes Turniersystem: " + system);
        };
    }

    static String stringParam(Map<String, Object> params, String key, String fallback) {
        Object value = params.get(key);
        return value == null ? fallback : String.valueOf(value).trim();
    }
}
