/*
 * Erstellung : 15.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierStartDialog;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Modaler Start-Dialog für neue JGJ-Meldelisten.
 * <p>
 * Abschnitte:
 * <ul>
 * <li>Meldeliste: Formation, Zusatzspalten „Teamname“ und „Verein“, Mêlée-Anmeldung</li>
 * <li>Spielplan: Anzeige der Teams (Teamnummer / Spielernamen / Teamname)</li>
 * <li>Spielmodus: Gruppengröße, Rückrunde</li>
 * </ul>
 */
public class JGJStartDialog {

	/** Ergebnis des Dialogs. */
	public record StartParameter(Formation formation,
		boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
		Optional<MeleeAnmeldungModus> meleeAnmeldung,
		TeamAnzeige spielplanTeamAnzeige,
		int gruppengroesse,
		boolean mitRueckrunde) {
	}

	/** Im Dialog vorausgewählte Formation. */
	private static final Formation START_FORMATION = Formation.TETE;

	private static final String LST_SPIELPLAN = "lstSpielplanAnzeige";
	private static final String EDIT_GRUPPENGROESSE = "editGruppengroesse";
	private static final String CB_RUECKRUNDE = "cbRueckrunde";

	private final WorkingSpreadsheet workingSpreadsheet;

	public JGJStartDialog(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	/**
	 * Zeigt den Dialog an und gibt das Ergebnis zurück.
	 *
	 * @return Optional mit StartParameter bei OK, leer bei Abbrechen
	 */
	public Optional<StartParameter> show() throws com.sun.star.uno.Exception {
		TurnierStartDialog dialog = TurnierStartDialog.erstellen(workingSpreadsheet.getxContext(),
				I18n.get("dialog.jgj.title.neue.meldeliste"));
		dialog.meldeliste(START_FORMATION, false, false);

		dialog.abschnitt(I18n.get("dialog.gruppe.spielplan"));
		dialog.auswahl(LST_SPIELPLAN, I18n.get("dialog.label.teams.anzeigen.als"), TeamAnzeige.dialogEintraege(),
				TeamAnzeige.NR.dialogIndex(), I18n.get("dialog.spielplan.teams.hilfe"));

		dialog.abschnitt(I18n.get("dialog.gruppe.spielmodus"));
		dialog.text(EDIT_GRUPPENGROESSE, I18n.get("dialog.jgj.label.gruppengroesse"),
				I18n.get("dialog.jgj.gruppengroesse.hilfe"));
		dialog.checkBox(CB_RUECKRUNDE, I18n.get("dialog.jgj.label.rueckrunde"), false, null);

		return dialog.anzeigen(d -> new StartParameter(d.formation(), d.teamname(), d.vereinsname(),
				d.meleeAnmeldung(), spielplanAnzeige(d.gewaehlt(LST_SPIELPLAN)),
				parseGruppengroesse(d.textWert(EDIT_GRUPPENGROESSE)), d.istAngehakt(CB_RUECKRUNDE)));
	}

	/** Ordnet die Listbox-Reihenfolge zentral der dauerhaft gespeicherten Enum zu. */
	static TeamAnzeige spielplanAnzeige(short auswahl) {
		return TeamAnzeige.ausDialogIndex(auswahl);
	}

	private static int parseGruppengroesse(String text) {
		try {
			return Math.max(0, Integer.parseInt(text.trim()));
		} catch (NumberFormatException e) {
			// leeres oder ungültiges Feld bedeutet „keine Gruppen“
			return 0;
		}
	}

	public static JGJStartDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new JGJStartDialog(workingSpreadsheet);
	}
}
