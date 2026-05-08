package de.petanqueturniermanager.spielerdb.vorlage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.spielerdb.AbgleichQuelle;
import de.petanqueturniermanager.spielerdb.AbgleichStatusSenke;
import de.petanqueturniermanager.spielerdb.AbgleichStatusSenke.AbgleichStatus;
import de.petanqueturniermanager.spielerdb.AbgleichStatusSenke.ZeilenStatus;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;

/**
 * Liest das flache Vorlage-Sheet (Vorname | Nachname | Verein) als
 * read-only-Quelle für den Spieler-DB-Abgleich und schreibt nach dem Import
 * Status + Fehlerursache pro Zeile zurück (Spalten D / E).
 *
 * <p>Validiert die Header-Zeile strikt (exakt die drei i18n-übersetzten
 * Bezeichnungen Vorname/Nachname/Verein, case-insensitiv getrimmt; Status- und
 * Fehlerursache-Spalten werden tool-seitig gepflegt und nicht validiert).
 * Datenzeilen werden als Block per {@link RangeHelper} gelesen und beim ersten
 * komplett leeren Eintrag beendet.
 */
public final class SheetVorlageAdapter implements AbgleichQuelle, AbgleichStatusSenke {

    private static final Logger logger = LogManager.getLogger(SheetVorlageAdapter.class);

    private final WorkingSpreadsheet ws;

    public SheetVorlageAdapter(WorkingSpreadsheet ws) {
        this.ws = ws;
    }

    @Override
    public String getSystemBezeichnung() {
        return I18n.get("spielerdb.vorlage.bezeichnung");
    }

    @Override
    public List<MeldelisteSpielerDaten> leseAlleSpielerRoh() {
        SheetHelper sh = new SheetHelper(ws);
        String sheetName = SheetNamen.spielerDbVorlage();
        XSpreadsheet sheet = sh.findByName(sheetName);
        if (sheet == null) {
            throw new VorlageNichtVerfuegbarException(
                    I18n.get("spielerdb.vorlage.fehler.fehlt", sheetName));
        }

        validiereHeader(sheet);

        return leseDatenzeilen(sheet);
    }

    private void validiereHeader(XSpreadsheet sheet) {
        String erwarteterVorname  = I18n.get("spielerdb.vorlage.spalte.vorname");
        String erwarteterNachname = I18n.get("spielerdb.vorlage.spalte.nachname");
        String erwarteterVerein   = I18n.get("spielerdb.vorlage.spalte.verein");

        XSpreadsheetDocument doc = ws.getWorkingSpreadsheetDocument();
        RangePosition headerPos = RangePosition.from(
                SpielerDbVorlageSheet.SPALTE_VORNAME, SpielerDbVorlageSheet.HEADER_ZEILE,
                SpielerDbVorlageSheet.SPALTE_VEREIN,  SpielerDbVorlageSheet.HEADER_ZEILE);
        RangeData header = RangeHelper.from(sheet, doc, headerPos).getDataFromRange();

        String gefundenVor = headerWert(header, SpielerDbVorlageSheet.SPALTE_VORNAME);
        String gefundenNach = headerWert(header, SpielerDbVorlageSheet.SPALTE_NACHNAME);
        String gefundenVer  = headerWert(header, SpielerDbVorlageSheet.SPALTE_VEREIN);

        if (!gefundenVor.equalsIgnoreCase(erwarteterVorname.strip())
                || !gefundenNach.equalsIgnoreCase(erwarteterNachname.strip())
                || !gefundenVer.equalsIgnoreCase(erwarteterVerein.strip())) {
            String gefunden = gefundenVor + " | " + gefundenNach + " | " + gefundenVer;
            throw new VorlageNichtVerfuegbarException(
                    I18n.get("spielerdb.vorlage.fehler.header",
                            erwarteterVorname, erwarteterNachname, erwarteterVerein, gefunden));
        }
    }

    private static String headerWert(RangeData header, int spalte) {
        if (header.isEmpty()) {
            return "";
        }
        RowData row = header.get(0);
        if (spalte >= row.size()) {
            return "";
        }
        CellData cell = row.get(spalte);
        String s = cell == null ? null : cell.getStringVal();
        return s == null ? "" : s.strip();
    }

    private List<MeldelisteSpielerDaten> leseDatenzeilen(XSpreadsheet sheet) {
        XSpreadsheetDocument doc = ws.getWorkingSpreadsheetDocument();
        RangePosition datenPos = RangePosition.from(
                SpielerDbVorlageSheet.SPALTE_VORNAME, SpielerDbVorlageSheet.HEADER_ZEILE + 1,
                SpielerDbVorlageSheet.SPALTE_VEREIN,  SpielerDbVorlageSheet.MAX_DATEN_ZEILE);
        RangeData daten = RangeHelper.from(sheet, doc, datenPos).getDataFromRange();

        List<MeldelisteSpielerDaten> result = new ArrayList<>();
        int zeileOffset = SpielerDbVorlageSheet.HEADER_ZEILE + 1;
        for (int i = 0; i < daten.size(); i++) {
            RowData row = daten.get(i);
            String vor  = zellWert(row, SpielerDbVorlageSheet.SPALTE_VORNAME);
            String nach = zellWert(row, SpielerDbVorlageSheet.SPALTE_NACHNAME);
            String ver  = zellWert(row, SpielerDbVorlageSheet.SPALTE_VEREIN);
            if (vor.isEmpty() && nach.isEmpty() && ver.isEmpty()) {
                break;
            }
            if (vor.isEmpty() && nach.isEmpty()) {
                continue;
            }
            String vereinOderNull = ver.isEmpty() ? null : ver;
            int zeile1Basiert = zeileOffset + i + 1;
            result.add(new MeldelisteSpielerDaten(vor, nach, vereinOderNull, zeile1Basiert));
        }
        return result;
    }

    private static String zellWert(RowData row, int spalte) {
        if (row == null || spalte >= row.size()) {
            return "";
        }
        CellData cell = row.get(spalte);
        String s = cell == null ? null : cell.getStringVal();
        return s == null ? "" : s.strip();
    }

    @Override
    public void schreibeStatus(List<ZeilenStatus> eintraege) {
        if (eintraege.isEmpty()) {
            return;
        }
        SheetHelper sh = new SheetHelper(ws);
        XSpreadsheet sheet = sh.findByName(SheetNamen.spielerDbVorlage());
        if (sheet == null) {
            return;
        }

        Map<Integer, ZeilenStatus> byZeile1 = new HashMap<>();
        int minZeile1 = Integer.MAX_VALUE;
        int maxZeile1 = Integer.MIN_VALUE;
        for (ZeilenStatus zs : eintraege) {
            byZeile1.put(zs.zeile1Basiert(), zs);
            minZeile1 = Math.min(minZeile1, zs.zeile1Basiert());
            maxZeile1 = Math.max(maxZeile1, zs.zeile1Basiert());
        }
        // Schreibblock: vom kleinsten gemeldeten Zeilenwert bis zum größten
        // Reservebereich. Zeilen, für die kein Status vorliegt (Lücken zwischen
        // gefüllten Zeilen oder hinter den aktuellen Daten), werden geleert –
        // so verschwinden Stand-Reste eines früheren Laufs zuverlässig.
        int startZeile0 = minZeile1 - 1;
        int endZeile0   = SpielerDbVorlageSheet.MAX_DATEN_ZEILE;

        RangeData rd = new RangeData();
        for (int zeile0 = startZeile0; zeile0 <= endZeile0; zeile0++) {
            int zeile1 = zeile0 + 1;
            ZeilenStatus zs = byZeile1.get(zeile1);
            RowData row = new RowData();
            if (zs == null) {
                row.newString("");
                row.newString("");
            } else {
                row.newString(I18n.get(statusKey(zs.status())));
                row.newString(zs.fehlerursache() == null ? "" : zs.fehlerursache());
            }
            rd.add(row);
        }

        try {
            RangePosition pos = RangePosition.from(
                    SpielerDbVorlageSheet.SPALTE_STATUS, startZeile0,
                    SpielerDbVorlageSheet.SPALTE_FEHLERURSACHE, endZeile0);
            RangeHelper.from(sheet, ws.getWorkingSpreadsheetDocument(), pos)
                    .setDataInRange(rd);
        } catch (GenerateException e) {
            logger.warn("Status-Block-Schreiben in Vorlage fehlgeschlagen", e);
        }
    }

    private static String statusKey(AbgleichStatus status) {
        return switch (status) {
            case NEU    -> "spielerdb.vorlage.status.neu";
            case IN_DB  -> "spielerdb.vorlage.status.in_db";
            case FEHLER -> "spielerdb.vorlage.status.fehler";
            case FEHLT  -> "spielerdb.vorlage.status.fehlt";
        };
    }

    /** Wird vom Dispatcher gefangen und als MessageBox angezeigt. */
    public static final class VorlageNichtVerfuegbarException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public VorlageNichtVerfuegbarException(String message) {
            super(message);
        }
    }
}
