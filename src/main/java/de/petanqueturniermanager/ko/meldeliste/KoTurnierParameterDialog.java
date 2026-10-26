/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierStartDialog;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.ko.konfiguration.KoPropertiesSpalte;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter vor dem Anlegen einer neuen
 * K.-O.-Meldeliste.
 * <p>
 * Abschnitte:
 * <ul>
 * <li>Meldeliste: Formation, Zusatzspalten „Teamname“ und „Verein“, Mêlée-Anmeldung</li>
 * <li>Spielbaum: Anzeige der Teams, Spielbahn, Bahnnummer nur in Runde 1, Spiel um Platz 3/4</li>
 * <li>Aufteilung in Gruppen: max. Teams pro Gruppe, Mindestgröße der letzten Gruppe</li>
 * </ul>
 */
public class KoTurnierParameterDialog {

	/** Ergebnis des Dialogs. */
	public record TurnierParameter(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
			Optional<MeleeAnmeldungModus> meleeAnmeldung, TeamAnzeige spielbaumTeamAnzeige, SpielrundeSpielbahn spielbaumSpielbahn,
			boolean spielbaumBahnNurRunde1, boolean spielUmPlatz3, int gruppenGroesse, int minLetzteGruppeGroesse) {
	}

	private static final String LST_SPIELBAUM = "lstSpielbaum";
	private static final String LST_SPIELBAHN = "lstSpielbahn";
	private static final String CB_BAHN_NUR_RUNDE1 = "cbBahnNurRunde1";
	private static final String CB_PLATZ3 = "cbPlatz3";
	private static final String NF_GRUPPEN_GROESSE = "nfGruppenGroesse";
	private static final String NF_MIN_LETZTE_GRUPPE = "nfMinLetzteGruppe";
	private static final int MIN_GRUPPEN_GROESSE = 2;
	private static final int MAX_GRUPPEN_GROESSE = 256;

	private final WorkingSpreadsheet workingSpreadsheet;

	public KoTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	public static KoTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new KoTurnierParameterDialog(workingSpreadsheet);
	}

	/**
	 * Zeigt den Dialog an und gibt das Ergebnis zurück.
	 *
	 * @return Optional mit TurnierParameter bei OK, leer bei Abbrechen
	 */
	public Optional<TurnierParameter> show(Formation defaultFormation, boolean defaultTeamnameAnzeigen,
			boolean defaultVereinsnameAnzeigen, TeamAnzeige defaultSpielbaumTeamAnzeige,
			SpielrundeSpielbahn defaultSpielbahn, boolean defaultSpielUmPlatz3,
			boolean defaultSpielbaumBahnNurRunde1, int defaultGruppenGroesse,
			int defaultMinLetzteGruppeGroesse) throws com.sun.star.uno.Exception {

		TurnierStartDialog dialog = TurnierStartDialog.erstellen(workingSpreadsheet.getxContext(),
				I18n.get("dialog.ko.titel"));
		dialog.meldeliste(defaultFormation, defaultTeamnameAnzeigen, defaultVereinsnameAnzeigen);

		dialog.abschnitt(I18n.get("dialog.gruppe.spielbaum"));
		dialog.auswahl(LST_SPIELBAUM, I18n.get("dialog.label.teams.anzeigen.als"), TeamAnzeige.dialogEintraege(),
				defaultSpielbaumTeamAnzeige.dialogIndex(), I18n.get("dialog.spielbaum.teams.hilfe"));
		dialog.spielbahn(LST_SPIELBAHN, defaultSpielbahn);
		dialog.checkBox(CB_BAHN_NUR_RUNDE1, I18n.get("dialog.label.bahn.nur.runde1"), defaultSpielbaumBahnNurRunde1,
				I18n.get("config.desc.ko.spielbaum.bahn.nur.runde1"));
		dialog.checkBox(CB_PLATZ3, I18n.get("dialog.label.spiel.um.platz3"), defaultSpielUmPlatz3, null);

		dialog.abschnitt(I18n.get("dialog.gruppe.gruppenaufteilung"));
		dialog.zahl(NF_GRUPPEN_GROESSE, I18n.get("dialog.label.max.teams.gruppe"), defaultGruppenGroesse,
				MIN_GRUPPEN_GROESSE, MAX_GRUPPEN_GROESSE, I18n.get("config.desc.ko.gruppen.groesse"));
		dialog.zahl(NF_MIN_LETZTE_GRUPPE, I18n.get("dialog.label.min.letzte.gruppe"), defaultMinLetzteGruppeGroesse,
				MIN_GRUPPEN_GROESSE, MAX_GRUPPEN_GROESSE, I18n.get("config.desc.ko.min.letzte.gruppe.groesse"));

		return dialog.anzeigen(d -> new TurnierParameter(d.formation(), d.teamname(), d.vereinsname(),
				d.meleeAnmeldung(), TeamAnzeige.ausDialogIndex(d.gewaehlt(LST_SPIELBAUM)),
				d.gewaehlteSpielbahn(LST_SPIELBAHN), d.istAngehakt(CB_BAHN_NUR_RUNDE1), d.istAngehakt(CB_PLATZ3),
				KoPropertiesSpalte.normalisiereGruppenGroesse(d.zahlWert(NF_GRUPPEN_GROESSE, defaultGruppenGroesse)),
				KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(
						d.zahlWert(NF_MIN_LETZTE_GRUPPE, defaultMinLetzteGruppeGroesse))));
	}
}
