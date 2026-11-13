/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetNew;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetNew;
import de.petanqueturniermanager.onlinesync.OnlineTurnierAnlage;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetNew;

/**
 * Legt in einem leeren Dokument die Meldeliste für ein Online-Turnier an, ohne Start-Dialog: Turniersystem,
 * Formation und Anmeldeart kommen aus dem Online-Turnier, alles Übrige wie die Vorbelegung des jeweiligen
 * Start-Dialogs. Läuft synchron im aufrufenden SheetRunner – die Meldelisten-Klassen werden nicht gestartet.
 */
public final class PtmOnlineMeldelistenAnlage {

    private PtmOnlineMeldelistenAnlage() {}

    public static void legeAn(WorkingSpreadsheet ws, OnlineTurnierAnlage anlage) throws GenerateException {
        switch (anlage.turnierSystem()) {
            case SUPERMELEE -> new MeldeListeSheet_New(ws).legeAnFuerOnline(anlage);
            case LIGA -> new LigaMeldeListeSheetNew(ws).legeAnFuerOnline(anlage);
            case SCHWEIZER -> new SchweizerMeldeListeSheetNew(ws).legeAnFuerOnline(anlage);
            case MAASTRICHTER -> new MaastrichterMeldeListeSheetNew(ws).legeAnFuerOnline(anlage);
            case JGJ -> new JGJMeldeListeSheet_New(ws).legeAnFuerOnline(anlage);
            case KO -> new KoMeldeListeSheetNew(ws).legeAnFuerOnline(anlage);
            case POULE -> new PouleMeldeListeSheetNew(ws).legeAnFuerOnline(anlage);
            case KASKADE -> new KaskadeMeldeListeSheetNew(ws).legeAnFuerOnline(anlage);
            case FORMULEX -> new FormuleXMeldeListeSheetNew(ws).legeAnFuerOnline(anlage);
            case TRIPTETE -> new TripTeteMeldeListeSheetNew(ws).legeAnFuerOnline();
            case KEIN -> throw new IllegalArgumentException("Kein Turniersystem für die Online-Anlage");
        }
    }
}
