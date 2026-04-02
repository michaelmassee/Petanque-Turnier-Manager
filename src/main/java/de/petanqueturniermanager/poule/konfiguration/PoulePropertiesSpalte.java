/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.konfiguration;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;

/**
 * Konfigurationsspalte für das Poule-A/B-Turniersystem.
 * Verwaltet Formation, Teamname- und Vereinsname-Einstellung sowie Grundfarben.
 */
public class PoulePropertiesSpalte extends BasePropertiesSpalte implements IPoulePropertiesSpalte {

    public static final List<ConfigProperty<?>> KONFIG_PROPERTIES = new ArrayList<>();

    static {
        ADDBaseProp(KONFIG_PROPERTIES, false); // Rangliste-Farben noch nicht benötigt
    }

    private static final String KONFIG_PROP_MELDELISTE_FORMATION   = "Meldeliste Formation";
    private static final String KONFIG_PROP_MELDELISTE_TEAMNAME    = "Meldeliste Teamname";
    private static final String KONFIG_PROP_MELDELISTE_VEREINSNAME = "Meldeliste Vereinsname";
    private static final String KONFIG_PROP_SPIELPLAN_MIT_BAHN = "Spielplan Bahnspalte";

    static {
        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_FORMATION)
                .setDefaultVal(Formation.TRIPLETTE.name())
                .setDescription("config.desc.meldeliste.formation"))
                .addAuswahl(Formation.TETE.name(), Formation.TETE.getBezeichnung())
                .addAuswahl(Formation.DOUBLETTE.name(), Formation.DOUBLETTE.getBezeichnung())
                .addAuswahl(Formation.TRIPLETTE.name(), Formation.TRIPLETTE.getBezeichnung()).inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_TEAMNAME)
                .setDefaultVal("J").setDescription("config.desc.meldeliste.teamname"))
                .addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_MELDELISTE_VEREINSNAME)
                .setDefaultVal("N").setDescription("config.desc.schweizer.vereinsname"))
                .addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());

        KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELPLAN_MIT_BAHN)
                .setDefaultVal("N").setDescription("config.desc.poule.spielplan.mit.bahnspalte"))
                .addAuswahl("J", "Ja").addAuswahl("N", "Nein").inSideBar());
    }

    protected PoulePropertiesSpalte(ISheet sheet) {
        super(sheet);
    }

    @Override
    protected List<ConfigProperty<?>> getKonfigProperties() {
        return KONFIG_PROPERTIES;
    }

    @Override
    public Formation getMeldeListeFormation() {
        var val = readStringProperty(KONFIG_PROP_MELDELISTE_FORMATION);
        var formation = Formation.valueOf(val);
        return formation != null ? formation : Formation.TRIPLETTE;
    }

    @Override
    public boolean isMeldeListeTeamnameAnzeigen() {
        return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME));
    }

    @Override
    public boolean isMeldeListeVereinsnameAnzeigen() {
        return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME));
    }

    @Override
    public void setMeldeListeFormation(Formation formation) {
        setStringProperty(KONFIG_PROP_MELDELISTE_FORMATION, formation.name());
    }

    @Override
    public void setMeldeListeTeamnameAnzeigen(boolean anzeigen) {
        setStringProperty(KONFIG_PROP_MELDELISTE_TEAMNAME, anzeigen ? "J" : "N");
    }

    @Override
    public void setMeldeListeVereinsnameAnzeigen(boolean anzeigen) {
        setStringProperty(KONFIG_PROP_MELDELISTE_VEREINSNAME, anzeigen ? "J" : "N");
    }

    @Override
    public boolean isSpielplanMitBahnspalte() {
        return "J".equalsIgnoreCase(readStringProperty(KONFIG_PROP_SPIELPLAN_MIT_BAHN));
    }

    @Override
    public void setSpielplanMitBahnspalte(boolean mitBahnspalte) {
        setStringProperty(KONFIG_PROP_SPIELPLAN_MIT_BAHN, mitBahnspalte ? "J" : "N");
    }

}
