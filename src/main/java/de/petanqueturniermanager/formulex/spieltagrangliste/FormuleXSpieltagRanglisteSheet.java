/*
* Erstellung : 18.08.2026 / Michael Massee
**/

package de.petanqueturniermanager.formulex.spieltagrangliste;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

/**
 * Archiviert die Rangliste eines abgeschlossenen Spieltags einer FormuleX-Turnierserie als
 * eigenständiges, unveränderliches Sheet (z.B. "1. Spieltag Rangliste") – analog zu
 * {@link de.petanqueturniermanager.schweizer.spieltagrangliste.SchweizerSpieltagRanglisteSheet}.
 * Wird von {@link de.petanqueturniermanager.formulex.meldeliste.FormuleXListeDelegate#naechsteSpieltag()}
 * unmittelbar VOR dem Wechsel auf den nächsten Spieltag aufgerufen, solange die Konfiguration
 * (aktiver Spieltag, aktive Spielrunde, Meldeliste-Aktiv-Spalte) noch den abzuschließenden
 * Spieltag widerspiegelt.
 * <p>
 * Nutzt die bereits vorhandenen, system-neutralen Namens-/Metadaten-Helper
 * {@link SheetNamen#spieltagRangliste(int)} und {@link SheetMetadataHelper#schluesselSpieltagRangliste(int)}
 * (von Supermelee eingeführt, hier wiederverwendet).
 */
public class FormuleXSpieltagRanglisteSheet extends FormuleXRanglisteSheet {

    private final SpielTagNr spieltag;

    public FormuleXSpieltagRanglisteSheet(WorkingSpreadsheet workingSpreadsheet, SpielTagNr spieltag) {
        super(workingSpreadsheet);
        this.spieltag = checkNotNull(spieltag, "spieltag == null");
    }

    public SpielTagNr getSpieltag() {
        return spieltag;
    }

    @Override
    protected String getRanglistenSheetName() {
        return SheetNamen.spieltagRangliste(spieltag.getNr());
    }

    @Override
    protected String getMetadatenSchluessel() {
        return SheetMetadataHelper.schluesselSpieltagRangliste(spieltag.getNr());
    }

    @Override
    protected String getSpielrundenMetadatenSchluessel(int rundeNr) {
        if (spieltag.getNr() <= 1) {
            return SheetMetadataHelper.schluesselFormuleXSpielrunde(rundeNr);
        }
        return SheetMetadataHelper.schluesselFormuleXSpielrundeSpieltag(spieltag.getNr(), rundeNr);
    }

}
