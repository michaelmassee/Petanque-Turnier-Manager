package de.petanqueturniermanager.helper.sheetsync;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.DisposedException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Berechnet eine kanonische SHA-256-Signatur über die semantisch relevanten
 * Eingangsdaten eines synchronisierbaren Sheets (Meldeliste, Spielrunden, Vorranglisten,
 * Teilnehmerlisten, …).
 * <p>
 * Identisches Sheet-Set + identische Whitelist-Inhalte → identischer Hash. Änderungen
 * an Hilfsspalten, Formatierung, Formeln außerhalb der Whitelist beeinflussen den Hash
 * nicht (siehe {@link SignaturQuelle#relevanteSpalten}).
 * <p>
 * Die Engine ist <b>generisch</b>: pro System wird sie über einen
 * {@link Function Function&lt;XSpreadsheetDocument, List&lt;SignaturQuelle&gt;&gt;}
 * konfiguriert, der die aktuellen Quellen liefert (Spielrunden-Anzahl variabel).
 * <p>
 * Sheet-Identität läuft ausschließlich über den
 * {@link SheetMetadataHelper Named-Range-Schlüssel} – nie über Sheet-Index oder -Name.
 */
public final class EingabeSignatur {

    private static final Logger logger = LogManager.getLogger(EingabeSignatur.class);

    /** Zusatz-Kontext, der keiner Sheet-Quelle entspricht (z.B. Default-Konstante). */
    private static final Function<XSpreadsheetDocument, String> KEIN_ZUSATZ_KONTEXT = xDoc -> "";

    private final Function<XSpreadsheetDocument, List<SignaturQuelle>> quellenLieferant;
    private final Function<XSpreadsheetDocument, String> zusatzKontextLieferant;

    /**
     * @param quellenLieferant liefert die aktuelle Quellen-Liste; wird bei jedem
     *                         {@link #berechne} aufgerufen, damit dynamische Quellen
     *                         (z.B. variable Spielrunden-Anzahl) berücksichtigt werden.
     */
    public EingabeSignatur(
            Function<XSpreadsheetDocument, List<SignaturQuelle>> quellenLieferant) {
        this(quellenLieferant, KEIN_ZUSATZ_KONTEXT);
    }

    /**
     * @param quellenLieferant      liefert die aktuelle Quellen-Liste (siehe oben).
     * @param zusatzKontextLieferant liefert eine zusätzliche, nicht aus Sheet-Zellen ableitbare
     *                              Zeichenkette, die in den Hash einfließt (z.B. eine
     *                              Sortier-Konfiguration aus den Dokument-Properties). Ändert sich
     *                              dieser Wert, ändert sich der Hash – und der Sheet-Sync wird
     *                              ausgelöst, obwohl die Eingabe-Sheets unverändert sind. Liefert
     *                              die Funktion einen leeren String, bleibt der Hash unverändert
     *                              gegenüber der Variante ohne Zusatz-Kontext.
     */
    public EingabeSignatur(
            Function<XSpreadsheetDocument, List<SignaturQuelle>> quellenLieferant,
            Function<XSpreadsheetDocument, String> zusatzKontextLieferant) {
        this.quellenLieferant = checkNotNull(quellenLieferant);
        this.zusatzKontextLieferant = checkNotNull(zusatzKontextLieferant);
    }

    /**
     * Berechnet das Signatur-Ergebnis für das angegebene Dokument.
     *
     * @param xDoc    Spreadsheet-Dokument
     * @param versuch aktueller Versuch (1-basiert) – wird in {@link SignaturErgebnis.TransientFehler}
     *                weitergereicht.
     */
    public SignaturErgebnis berechne(XSpreadsheetDocument xDoc, int versuch) {
        checkNotNull(xDoc);
        List<SignaturQuelle> quellen;
        try {
            quellen = quellenLieferant.apply(xDoc);
        } catch (DisposedException e) {
            return new SignaturErgebnis.TransientFehler("Dokument verworfen", e, versuch);
        } catch (RuntimeException e) {
            logger.warn("Quellen-Lieferant warf RuntimeException", e);
            return new SignaturErgebnis.PermanenterFehler("Quellen-Lieferant fehlerhaft", e);
        }
        if (quellen == null || quellen.isEmpty()) {
            return new SignaturErgebnis.PermanenterFehler("Quellen-Lieferant lieferte keine Quellen", null);
        }

        List<SignaturQuelle> sortiert = new ArrayList<>(quellen);
        sortiert.sort(Comparator.comparing(SignaturQuelle::stabileId));

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return new SignaturErgebnis.PermanenterFehler("SHA-256 nicht verfügbar", e);
        }

        for (SignaturQuelle quelle : sortiert) {
            SignaturErgebnis teilErgebnis = serialisiereQuelle(xDoc, quelle, digest, versuch);
            if (teilErgebnis != null) {
                return teilErgebnis;
            }
        }

        String zusatzKontext;
        try {
            zusatzKontext = zusatzKontextLieferant.apply(xDoc);
        } catch (DisposedException e) {
            return new SignaturErgebnis.TransientFehler("Zusatz-Kontext verworfen", e, versuch);
        } catch (RuntimeException e) {
            logger.warn("Zusatz-Kontext-Lieferant warf RuntimeException", e);
            return new SignaturErgebnis.PermanenterFehler("Zusatz-Kontext-Lieferant fehlerhaft", e);
        }
        if (zusatzKontext != null && !zusatzKontext.isEmpty()) {
            digest.update(("CTX=" + zusatzKontext + "\n").getBytes(StandardCharsets.UTF_8));
        }

        String hex = HexFormat.of().formatHex(digest.digest());
        return new SignaturErgebnis.Ok(hex);
    }

    /**
     * Verarbeitet eine einzelne Quelle. Liefert {@code null} bei Erfolg (Daten in Digest
     * eingespeist) oder ein nicht-Ok-Ergebnis (das den Gesamt-Hash kurzschließt).
     */
    private SignaturErgebnis serialisiereQuelle(XSpreadsheetDocument xDoc, SignaturQuelle quelle,
            MessageDigest digest, int versuch) {
        Optional<XSpreadsheet> sheetOpt;
        try {
            sheetOpt = SheetMetadataHelper.findeSheet(xDoc, quelle.sheetNamedRangeSchluessel());
        } catch (DisposedException e) {
            return new SignaturErgebnis.TransientFehler(
                    "Sheet-Lookup verworfen für '" + quelle.stabileId() + "'", e, versuch);
        } catch (RuntimeException e) {
            return new SignaturErgebnis.TransientFehler(
                    "Sheet-Lookup-Fehler für '" + quelle.stabileId() + "'", e, versuch);
        }

        if (sheetOpt.isEmpty()) {
            return new SignaturErgebnis.SheetFehlt(quelle.stabileId(), quelle.erwartet());
        }

        RangeData daten;
        try {
            RangePosition rangePos = RangePosition.from(
                    quelle.ersteRelevanteSpalte(),
                    quelle.ersteZeile(),
                    quelle.letzteRelevanteSpalte(),
                    quelle.ersteZeile() + quelle.maxAnzahlZeilen() - 1);
            daten = RangeHelper.from(sheetOpt.get(), xDoc, rangePos).getDataFromRange();
        } catch (DisposedException e) {
            return new SignaturErgebnis.TransientFehler(
                    "Bulk-Read verworfen für '" + quelle.stabileId() + "'", e, versuch);
        } catch (RuntimeException e) {
            return new SignaturErgebnis.TransientFehler(
                    "Bulk-Read-Fehler für '" + quelle.stabileId() + "'", e, versuch);
        }

        kanonisiereInDigest(quelle, daten, digest);
        return null;
    }

    /**
     * Serialisiert eine Quelle deterministisch in den Digest:
     * <ul>
     *   <li>Header pro Quelle: {@code "Q="+stabileId+";N="+anzNichtleereZeilen+"\n"}.</li>
     *   <li>Pro nicht-leere Datenzeile: {@code "R="+absoluteZeile+"\n"}.</li>
     *   <li>Pro Zelle in Whitelist mit nicht-leerem Wert:
     *       {@code "C="+absSpalte+";V="+normalisierterWert+"\n"}.</li>
     * </ul>
     * Leere Zellen und Zellen außerhalb der Whitelist werden vollständig ausgelassen –
     * dadurch ist die Reihenfolge der Iteration stabil und nur tatsächliche Änderungen
     * an relevanten Werten verändern den Hash.
     */
    private void kanonisiereInDigest(SignaturQuelle quelle, RangeData daten, MessageDigest digest) {
        int ersteAbsoluteSpalte = quelle.ersteRelevanteSpalte();
        int anzNichtleere = 0;
        StringBuilder zeilen = new StringBuilder(256);

        for (int zeilenIdx = 0; zeilenIdx < daten.size(); zeilenIdx++) {
            RowData zeile = daten.get(zeilenIdx);
            StringBuilder zellen = new StringBuilder(64);
            boolean zeileHatInhalt = false;
            for (int cellIdx = 0; cellIdx < zeile.size(); cellIdx++) {
                int absSpalte = ersteAbsoluteSpalte + cellIdx;
                if (!quelle.relevanteSpalten().contains(absSpalte)) {
                    continue;
                }
                Object rohwert = zeile.get(cellIdx).getData();
                String normiert = normalisiereWert(rohwert);
                if (normiert == null) {
                    continue;
                }
                zellen.append("C=").append(absSpalte).append(";V=").append(normiert).append('\n');
                zeileHatInhalt = true;
            }
            if (zeileHatInhalt) {
                int absoluteZeile = quelle.ersteZeile() + zeilenIdx;
                zeilen.append("R=").append(absoluteZeile).append('\n').append(zellen);
                anzNichtleere++;
            }
        }

        digest.update(("Q=" + quelle.stabileId() + ";N=" + anzNichtleere + "\n")
                .getBytes(StandardCharsets.UTF_8));
        digest.update(zeilen.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Normalisiert den Rohwert einer Zelle für die kanonische Serialisierung.
     * <p>
     * Zahlen werden locale-unabhängig formatiert ({@code Double.toString} bzw.
     * ganzzahlige Darstellung), Strings werden getrimmt. Leere/null Werte liefern
     * {@code null} – sie werden im Digest vollständig ausgelassen.
     */
    static String normalisiereWert(Object rohwert) {
        if (rohwert == null) {
            return null;
        }
        if (rohwert instanceof Number num) {
            double d = num.doubleValue();
            if (d == 0.0d && !(rohwert instanceof Double)) {
                // Integer 0 → bleibt "0"
                return Long.toString(num.longValue());
            }
            if (Double.isFinite(d) && d == Math.floor(d) && !Double.isInfinite(d)
                    && Math.abs(d) < 1e15) {
                return Long.toString((long) d);
            }
            return Double.toString(d);
        }
        String s = rohwert.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
