/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import java.util.HashSet;
import java.util.Set;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;

/**
 * Liefert die für die Plan-Berechnung ({@code KaskadenKoRundenPlaner}) zu verwendende Teamanzahl:
 * fixiert ab Runde 1 ({@link KaskadeKonfigurationSheet#getAnzahlTeamsRunde1()}), damit
 * Folgerunden UND der Abschluss (Gruppenrangliste, KO-Felder) dieselbe Gruppenstruktur wie die
 * bereits geschriebenen Runden-Sheets verwenden - unabhängig von zwischenzeitlichen Ausstiegen.
 * <p>
 * Migrations-Fallback: Dateien, die vor Einführung dieser Property mit bereits gespielter Runde 1
 * gespeichert wurden, haben die Property noch nicht gesetzt (Default 0). In diesem Fall wird die
 * tatsächliche Runde-1-Teamanzahl aus dem bereits geschriebenen Runde-1-Sheet rekonstruiert (jede
 * Team-Nr taucht dort genau einmal auf) und dauerhaft persistiert, statt mit gesamtTeams=0 gegen
 * {@code KaskadenKoRundenPlaner.berechne()} zu laufen (IllegalArgumentException).
 */
final class KaskadePlanGroesse {

    private KaskadePlanGroesse() {
        // Hilfsklasse – kein Instanz-Konstruktor
    }

    static int ermittleFixierteTeamAnzahl(WorkingSpreadsheet workingSpreadsheet,
            KaskadeKonfigurationSheet konfigurationSheet, int aktuelleAktiveTeamAnzahl) throws GenerateException {
        int anzahl = konfigurationSheet.getAnzahlTeamsRunde1();
        if (anzahl >= 2) {
            return anzahl;
        }
        anzahl = ermittleAusRunde1Sheet(workingSpreadsheet);
        if (anzahl >= 2) {
            konfigurationSheet.setAnzahlTeamsRunde1(anzahl);
            return anzahl;
        }
        // Kein (rekonstruierbares) Runde-1-Sheet vorhanden: letzter Fallback auf die aktuelle
        // aktive Teamzahl (Vor-Fix-Semantik).
        return aktuelleAktiveTeamAnzahl;
    }

    private static int ermittleAusRunde1Sheet(WorkingSpreadsheet workingSpreadsheet) throws GenerateException {
        var runde1Sheet = SheetMetadataHelper.findeSheetUndHeile(
                workingSpreadsheet.getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.schluesselKaskadenRunde(1), SheetNamen.kaskadenRunde(1));
        if (runde1Sheet == null) {
            return 0;
        }

        var readRange = RangePosition.from(KaskadeSpielrundeSheet.SPIEL_NR_SPALTE,
                KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE, KaskadeSpielrundeSheet.TEAM_B_SPALTE,
                KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE + 999);
        var daten = RangeHelper.from(runde1Sheet, workingSpreadsheet.getWorkingSpreadsheetDocument(), readRange)
                .getDataFromRange();

        Set<Integer> teamNrn = new HashSet<>();
        for (var zeile : daten) {
            if (zeile.isEmpty() || zeile.get(0).getIntVal(0) <= 0) {
                break; // Ende der Spieldaten
            }
            int teamA = zeile.size() > 1 ? zeile.get(1).getIntVal(0) : 0;
            int teamB = zeile.size() > 2 ? zeile.get(2).getIntVal(0) : 0;
            if (teamA > 0) {
                teamNrn.add(teamA);
            }
            if (teamB > 0) {
                teamNrn.add(teamB);
            }
        }
        return teamNrn.size();
    }
}
