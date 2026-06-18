package de.petanqueturniermanager.webserver;

/**
 * Konfiguration eines einzelnen Panels in einem Composite View.
 *
 * @param typ                   Anzeigemodus: {@link PanelTyp#BLATT}, {@link PanelTyp#URL},
 *                              {@link PanelTyp#TIMER} oder {@link PanelTyp#STATISCHE_DATEI}
 * @param sheetConfig           Konfigurations-String (nur relevant wenn typ == BLATT)
 * @param resolver              Sheet-Resolver (nur relevant wenn typ == BLATT, sonst {@code null})
 * @param zoom                  Zoom-Faktor in Prozent (10–500, Standard 100)
 * @param sichtbarerTabellenAnteil sichtbarer Anteil der Gesamttabelle in Prozent (10–100, Standard 100)
 * @param horizontalAusrichtung horizontale Ausrichtung des Panel-Inhalts:
 *                              {@code "kein"}, {@code "links"}, {@code "mitte"} oder {@code "rechts"}
 * @param vertikalAusrichtung   vertikale Ausrichtung des Panel-Inhalts:
 *                              {@code "kein"}, {@code "oben"}, {@code "mitte"} oder {@code "unten"}
 * @param blattnameAnzeigen     ob der Blattname als Kopfzeile im Panel angezeigt wird
 * @param externeUrl            externe URL oder lokaler Dateipfad (nur relevant wenn typ == URL oder STATISCHE_DATEI)
 */
public record PanelKonfiguration(
        PanelTyp typ,
        String sheetConfig,
        SheetResolver resolver,
        int zoom,
        int sichtbarerTabellenAnteil,
        String horizontalAusrichtung,
        String vertikalAusrichtung,
        boolean blattnameAnzeigen,
        String externeUrl) {

    public PanelKonfiguration {
        horizontalAusrichtung = PanelAusrichtung.normiereHorizontal(horizontalAusrichtung);
        vertikalAusrichtung   = PanelAusrichtung.normiereVertikal(vertikalAusrichtung);
        if (sichtbarerTabellenAnteil < 10 || sichtbarerTabellenAnteil > 100) {
            sichtbarerTabellenAnteil = 100;
        }
    }

    public PanelKonfiguration(
            PanelTyp typ,
            String sheetConfig,
            SheetResolver resolver,
            int zoom,
            String horizontalAusrichtung,
            String vertikalAusrichtung,
            boolean blattnameAnzeigen,
            String externeUrl) {
        this(typ, sheetConfig, resolver, zoom, 100,
                horizontalAusrichtung, vertikalAusrichtung, blattnameAnzeigen, externeUrl);
    }
}
