package de.petanqueturniermanager.jedergegenjeden.spielplan;

import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.jedergegenjeden.finalrunde.JGJFinalrundeSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJGesamtranglisteSheet;

/**
 * JGJ-Beispielturnier mit 8 Triplette-Teams, aufgeteilt in zwei Gruppen à 4 Teams.
 * Erzeugt zusätzlich zur (gruppierten) Einzel-Rangliste die gruppenübergreifende
 * {@link JGJGesamtranglisteSheet} und zwei vollständig ausgefüllte KO-Finalrunden
 * à 4 Teams inklusive Spiel um Platz 3.
 * Entspricht dem Menüeintrag
 * {@code ptm:jgj_testdaten_turnier_triplette_2gruppen}.
 */
public class JGJTriplette2Gruppen4TurnierTestDaten extends JGJTurnierTestDaten {

    private static final int ANZ_TEAMS = 8;
    private static final int GRUPPENGROESSE = 4;
    private static final String SCORE_DATA_PREFIX = "PTM_EDIT:";

    public JGJTriplette2Gruppen4TurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected Formation getFormation() {
        return Formation.TRIPLETTE;
    }

    @Override
    protected int getAnzTeams() {
        return ANZ_TEAMS;
    }

    @Override
    protected int getGruppengroesse() {
        return GRUPPENGROESSE;
    }

    @Override
    protected void doRun() throws GenerateException {
        super.doRun();

        // Zusätzlich die gruppenübergreifende Gesamtrangliste erzeugen und aktiv setzen.
        processBoxinfo("processbox.erstelle.rangliste");
        var gesamtrangliste = new JGJGesamtranglisteSheet(getWorkingSpreadsheet());
        gesamtrangliste.upDateSheet();

        var konfig = new JGJKonfigurationSheet(getWorkingSpreadsheet());
        konfig.setGruppenGroesse(4);
        konfig.setSpielbaumSpielUmPlatz3(true);
        new JGJFinalrundeSheet(getWorkingSpreadsheet()).doRun();
        ergebnisseInFinalrundenEintragen();
        TurnierSheet.from(gesamtrangliste.getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
    }

    private void ergebnisseInFinalrundenEintragen() throws GenerateException {
        for (String gruppe : new String[] { "A", "B" }) {
            fuelleLeereScorePaare(SheetMetadataHelper.schluesselJgjFinalrunde(gruppe));
        }
        calculateAll();
    }

    private void fuelleLeereScorePaare(String finalrundenSchluessel) throws GenerateException {
        var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        XSpreadsheet sheet = SheetMetadataHelper.findeSheet(xDoc, finalrundenSchluessel).orElse(null);
        if (sheet == null) {
            return;
        }
        String encoded = SheetMetadataHelper.leseScoreText(
                xDoc, SheetMetadataHelper.scoreSchluessel(finalrundenSchluessel));
        if (encoded == null || encoded.isBlank()) {
            return;
        }
        String daten = encoded.startsWith(SCORE_DATA_PREFIX)
                ? encoded.substring(SCORE_DATA_PREFIX.length())
                : encoded;
        String[] tokens = daten.split("\\|");
        for (int i = 0; i + 1 < tokens.length; i += 2) {
            int[] posA = parsePosition(tokens[i]);
            int[] posB = parsePosition(tokens[i + 1]);
            if (posA == null || posB == null) {
                continue;
            }
            XCell zelleA;
            XCell zelleB;
            try {
                zelleA = sheet.getCellByPosition(posA[0], posA[1]);
                zelleB = sheet.getCellByPosition(posB[0], posB[1]);
            } catch (com.sun.star.lang.IndexOutOfBoundsException e) {
                throw new GenerateException("JGJ-Finalrunden-Score-Zelle ungueltig: " + e.getMessage());
            }
            if (zelleA.getType() != CellContentType.EMPTY || zelleB.getType() != CellContentType.EMPTY) {
                continue;
            }
            boolean aGewinnt = RandomSource.nextInt(2) == 0;
            int verliererPunkte = RandomSource.nextInt(13);
            zelleA.setValue(aGewinnt ? 13 : verliererPunkte);
            zelleB.setValue(aGewinnt ? verliererPunkte : 13);
        }
    }

    private int[] parsePosition(String token) {
        String[] teile = token.split(",");
        if (teile.length != 2) {
            return null;
        }
        try {
            return new int[] {
                    Integer.parseInt(teile[0].trim()),
                    Integer.parseInt(teile[1].trim())
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void calculateAll() {
        XCalculatable xCal = Lo.qi(XCalculatable.class, getWorkingSpreadsheet().getWorkingSpreadsheetDocument());
        if (xCal != null) {
            xCal.calculateAll();
        }
    }
}
