package de.petanqueturniermanager.webserver;

/**
 * Konfiguration eines einzelnen Panels in einem Composite View.
 *
 * @param typ               Anzeigemodus: {@link PanelTyp#BLATT} oder {@link PanelTyp#URL}
 * @param sheetConfig       Konfigurations-String (nur relevant wenn typ == BLATT)
 * @param resolver          Sheet-Resolver (nur relevant wenn typ == BLATT, sonst {@code null})
 * @param zoom              Zoom-Faktor in Prozent (10–500, Standard 100)
 * @param zentriert         ob der Inhalt dieses Panels horizontal zentriert dargestellt wird
 * @param blattnameAnzeigen ob der Blattname als Kopfzeile im Panel angezeigt wird
 * @param externeUrl        externe URL (nur relevant wenn typ == URL)
 */
public record PanelKonfiguration(
        PanelTyp typ,
        String sheetConfig,
        SheetResolver resolver,
        int zoom,
        boolean zentriert,
        boolean blattnameAnzeigen,
        String externeUrl) {
}
