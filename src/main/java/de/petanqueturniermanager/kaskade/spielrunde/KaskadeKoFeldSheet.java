/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.kaskaden.KaskadenFeldBelegung;
import de.petanqueturniermanager.algorithmen.kaskaden.KaskadenKoRundenPlaner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKoBracketKonfigAdapter;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetUpdate;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellt für jedes Kaskaden-Endfeld (A, B, C, D …) einen visuellen KO-Turnierbaum.<br>
 * <br>
 * Die Darstellung delegiert vollständig an {@link KoTurnierbaumSheet#erstelleGruppenBrackets},
 * sodass die gleiche Bracket-Ansicht wie beim KO-System, Maastrichter KO und Poule KO entsteht
 * (Rundenköpfe, Cadrage-Spalte, IF-Formeln für automatische Siegerberechnung, Unicode-Konnektoren).
 *
 * @author Michael Massee
 */
public class KaskadeKoFeldSheet extends SheetRunner implements ISheet {

    private static final Logger LOGGER = LogManager.getLogger(KaskadeKoFeldSheet.class);

    private final KaskadeKonfigurationSheet konfigurationSheet;
    private final KaskadeMeldeListeSheetUpdate meldeListe;

    /** Das zuletzt erstellte Feld-Sheet (für getXSpreadSheet). */
    private String letzterBezeichner;

    /** Wird für Tests gesetzt, damit forceCreate bei KaskadeGruppenRanglisteSheet aktiviert wird. */
    private volatile boolean forceOk;

    public KaskadeKoFeldSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, TurnierSystem.KASKADE, "Kaskaden-KO-Feld");
        konfigurationSheet = new KaskadeKonfigurationSheet(workingSpreadsheet);
        meldeListe = new KaskadeMeldeListeSheetUpdate(workingSpreadsheet);
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        if (letzterBezeichner == null) {
            return null;
        }
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.schluesselKaskadenFeld(letzterBezeichner),
                SheetNamen.kaskadenFeld(letzterBezeichner));
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    public KaskadeKonfigurationSheet getKonfigurationSheet() {
        return konfigurationSheet;
    }

    @Override
    protected void doRun() throws GenerateException {
        getxCalculatable().enableAutomaticCalculation(false);
        processBoxinfo("processbox.kaskade.ko.felder.erstellen");

        meldeListe.upDateSheet();
        int gesamtTeams = meldeListe.getMeldungenSortiertNachSetzposition().size();

        boolean freispielGewonnen = konfigurationSheet.getFreispielPunktePlus()
                > konfigurationSheet.getFreispielPunkteMinus();
        var plan = KaskadenKoRundenPlaner.berechne(
                gesamtTeams, konfigurationSheet.getAnzahlKaskaden(), freispielGewonnen);

        var belegungen = new KaskadeRundenErgebnisLeser(getWorkingSpreadsheet()).ladeFeldBelegungen(plan);

        // Gruppenrangliste immer zuerst erstellen (Voraussetzung für KO-Felder)
        var gruppenRangliste = new KaskadeGruppenRanglisteSheet(getWorkingSpreadsheet());
        gruppenRangliste.setForceOk(isForceOk());
        gruppenRangliste.doRun();

        var konfig = new KaskadeKoBracketKonfigAdapter(konfigurationSheet);
        List<KoTurnierbaumSheet.GruppenBracketAuftrag> bracketAuftraege = new ArrayList<>();
        short sheetPos = DefaultSheetPos.KASKADE_FELDER;
        for (var belegung : belegungen) {
            SheetRunner.testDoCancelTask();
            var auftrag = feldBracketAuftrag(belegung, sheetPos);
            if (auftrag != null) {
                bracketAuftraege.add(auftrag);
                sheetPos++;
            }
        }
        new KoTurnierbaumSheet(getWorkingSpreadsheet()).erstelleGruppenBrackets(bracketAuftraege, konfig);
    }

    // ---------------------------------------------------------------
    // Feld-Sheet erstellen
    // ---------------------------------------------------------------

    private KoTurnierbaumSheet.GruppenBracketAuftrag feldBracketAuftrag(KaskadenFeldBelegung belegung,
            short sheetPos) throws GenerateException {
        var feld = belegung.feld();
        if (feld.gesamtTeams() < 2) {
            LOGGER.info("Feld {} hat weniger als 2 Teams ({}), wird übersprungen.",
                    feld.bezeichner(), feld.gesamtTeams());
            return null;
        }

        processBoxinfo("processbox.kaskade.ko.plan.erstellen", feld.bezeichner());
        letzterBezeichner = feld.bezeichner();

        var gruppeTeams = feldBelegungZuTeamMeldungen(belegung.teamNrs());
        var sheetName   = SheetNamen.kaskadenFeld(feld.bezeichner());
        var schluessel  = SheetMetadataHelper.schluesselKaskadenFeld(feld.bezeichner());
        return new KoTurnierbaumSheet.GruppenBracketAuftrag(gruppeTeams, sheetName, sheetPos, schluessel, null);
    }

    private static TeamMeldungen feldBelegungZuTeamMeldungen(List<Integer> teamNrs) {
        var meldungen = new TeamMeldungen();
        teamNrs.forEach(nr -> meldungen.addTeamWennNichtVorhanden(Team.from(nr)));
        return meldungen;
    }

    public boolean isForceOk() {
        return forceOk;
    }

    public void setForceOk(boolean forceOk) {
        this.forceOk = forceOk;
    }
}
