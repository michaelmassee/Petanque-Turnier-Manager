package de.petanqueturniermanager.sidebar.sheets;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Eintrag in der Baum-Darstellung der Blätterliste.
 * Entweder ein kollabierbarer Gruppen-Kopf, ein Spieltag-Header oder ein einzelnes Blatt.
 */
public sealed interface BlattBaumEintrag
        permits BlattBaumEintrag.GruppenKopf, BlattBaumEintrag.SpieltagKopf, BlattBaumEintrag.BlattKnoten {

    /** Text der in der ListBox angezeigt wird. */
    String anzeigeText();

    /** Gruppen-Header (Turniersystem) mit Auf-/Zuklappen-Pfeil. */
    record GruppenKopf(SheetGruppe gruppe, boolean expandiert) implements BlattBaumEintrag {

        @Override
        public String anzeigeText() {
            return (expandiert ? "▼ " : "▶ ") + gruppe.getAnzeigeBezeichnung();
        }
    }

    /** Spieltag-Untergruppen-Header (nur Supermelee) mit Auf-/Zuklappen-Pfeil. */
    record SpieltagKopf(int spieltagNr, boolean expandiert) implements BlattBaumEintrag {

        @Override
        public String anzeigeText() {
            return (expandiert ? "▼ " : "▶ ") + I18n.get("sidebar.sheets.spieltag.nr", spieltagNr);
        }
    }

    /** Einzelnes Tabellenblatt, eingerückt dargestellt. */
    record BlattKnoten(XSpreadsheet sheet, String anzeigeText, String metadatenSchluessel)
            implements BlattBaumEintrag {
    }
}
