/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.io.IOException;
import java.util.List;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

public final class KiAssistentService {

    private final KiClient client;
    private final KiOptionen optionen;

    public KiAssistentService(KiOptionen optionen) {
        this(KiClientFactory.erzeugeClient(optionen), optionen);
    }

    /** Ruft die beim konfigurierten Anbieter verfügbaren Modell-IDs ab (z.B. für die Options-Seite). */
    public static List<String> ladeVerfuegbareModelle(KiOptionen optionen) throws IOException, InterruptedException {
        return KiClientFactory.erzeugeClient(optionen).listeModelle();
    }

    KiAssistentService(KiClient client, KiOptionen optionen) {
        this.client = client;
        this.optionen = optionen;
    }

    public KiPlan planeNeuesTurnier(WorkingSpreadsheet ws, String benutzerWunsch) throws Exception {
        String prompt = prompt(ws, benutzerWunsch);
        String antwort = client.erstelleAntwort(prompt);
        KiPlan plan = KiPlanParser.parse(antwort);
        KiActionRegistry.validiere(plan);
        return plan;
    }

    String prompt(WorkingSpreadsheet ws, String benutzerWunsch) {
        return new StringBuilder()
                .append("Du bist der Planer fuer den Petanque-Turnier-Manager (PTM).\n")
                .append("Erzeuge ausschliesslich einen JSON-Plan ohne Markdown.\n\n")
                .append("Erlaubte action types:\n")
                .append("- new_tournament: parameters.system ist eines von SUPERMELEE, LIGA, JGJ, ")
                .append("SCHWEIZER, KO, MAASTRICHTER, POULE, KASKADE, FORMULEX, TRIPTETE.\n")
                .append("- custom_sheet: parameters.name ist der Sheetname, parameters.rows ist ein Array ")
                .append("von Zeilen und Zellen.\n\n")
                .append("Verboten:\n")
                .append("- freie Makros, Basic/Python-Code, unbekannte ptm:-Commands oder UNO-Befehle.\n")
                .append("- automatische Ausfuehrung ohne Bestaetigung.\n")
                .append("- externe Formeln, URLs oder Dateien in custom_sheet-Zellen.\n")
                .append("- Behauptungen ueber PTM-Funktionen ohne Bezug zum lokalen Kontext oder Online-Wissen.\n\n")
                .append("Du bekommst Online-Wissen aus der PTM-Projektseite und dem PTM-Wiki.\n")
                .append("Nutze dieses Wissen, um Fragen zu PTM, Makros, Formeln und Turniersystemen zu ")
                .append("beantworten.\n")
                .append("Wenn der Benutzer nur eine Frage stellt, liefere einen Plan ohne mutierende Aktionen ")
                .append("und schreibe die Antwort in summary/dataPreview.\n")
                .append("Wenn der Benutzer ein Turnier erstellen will, liefere mindestens eine ")
                .append("new_tournament-Aktion.\n\n")
                .append("JSON-Format:\n")
                .append("{\n")
                .append("  \"summary\": \"...\",\n")
                .append("  \"requiresConfirmation\": true,\n")
                .append("  \"actions\": [\n")
                .append("    {\"type\":\"new_tournament\",\"target\":\"SUPERMELEE\",")
                .append("\"parameters\":{\"system\":\"SUPERMELEE\"}},\n")
                .append("    {\"type\":\"custom_sheet\",\"target\":\"Aushang\",")
                .append("\"parameters\":{\"name\":\"Aushang\",\"rows\":[[\"Titel\"],[\"Text\"]]}}\n")
                .append("  ],\n")
                .append("  \"warnings\": [],\n")
                .append("  \"dataPreview\": \"...\"\n")
                .append("}\n\n")
                .append("Aktueller Kontext:\n")
                .append(TurnierKontextSammler.sammle(ws, optionen.vollstaendigenKontextSenden()))
                .append("\n\nPTM Online-Wissen:\n")
                .append(PtmOnlineWissen.laden(optionen))
                .append("\n\nBenutzerwunsch:\n")
                .append(benutzerWunsch == null ? "" : benutzerWunsch.trim())
                .append('\n')
                .toString();
    }
}
