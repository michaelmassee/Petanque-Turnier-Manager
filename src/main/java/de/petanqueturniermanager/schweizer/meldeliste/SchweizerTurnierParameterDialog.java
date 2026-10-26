/*
 * Erstellung : 27.02.2026 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierStartDialog;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter vor dem Anlegen einer neuen
 * Schweizer Meldeliste.
 * <p>
 * Abschnitte:
 * <ul>
 * <li>Meldeliste: Formation, Zusatzspalten „Teamname“ und „Verein“, Mêlée-Anmeldung</li>
 * <li>Spielplan: Anzeige der Teams (Teamnummer / Spielernamen / Teamname)</li>
 * <li>Rangliste: Wertung mit / ohne Buchholz</li>
 * </ul>
 */
public class SchweizerTurnierParameterDialog {

	/** Ergebnis des Dialogs. */
	public record TurnierParameter(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
			Optional<MeleeAnmeldungModus> meleeAnmeldung, TeamAnzeige spielplanTeamAnzeige, SchweizerRankingModus rankingModus) {
	}

	private static final String LST_SPIELPLAN = "lstSpielplan";
	private static final String LST_RANKING = "lstRankingModus";

	private final WorkingSpreadsheet workingSpreadsheet;

	public SchweizerTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	/**
	 * Zeigt den Dialog an und gibt das Ergebnis zurück.
	 *
	 * @param defaultFormation            vorausgewählte Formation
	 * @param defaultTeamnameAnzeigen     vorausgewählter Teamname-Status
	 * @param defaultVereinsnameAnzeigen  vorausgewählter Vereinsname-Status
	 * @param defaultSpielplanTeamAnzeige vorausgewählte Spielplan-Anzeige
	 * @param defaultRankingModus         vorausgewählte Ranglisten-Wertung
	 * @return Optional mit TurnierParameter bei OK, leer bei Abbrechen
	 */
	public Optional<TurnierParameter> show(Formation defaultFormation, boolean defaultTeamnameAnzeigen,
			boolean defaultVereinsnameAnzeigen, TeamAnzeige defaultSpielplanTeamAnzeige,
			SchweizerRankingModus defaultRankingModus) throws com.sun.star.uno.Exception {

		TurnierStartDialog dialog = TurnierStartDialog.erstellen(workingSpreadsheet.getxContext(),
				I18n.get("dialog.schweizer.titel"));
		dialog.meldeliste(defaultFormation, defaultTeamnameAnzeigen, defaultVereinsnameAnzeigen);

		dialog.abschnitt(I18n.get("dialog.gruppe.spielplan"));
		dialog.auswahl(LST_SPIELPLAN, I18n.get("dialog.label.teams.anzeigen.als"), TeamAnzeige.dialogEintraege(),
				defaultSpielplanTeamAnzeige.dialogIndex(), I18n.get("dialog.spielplan.teams.hilfe"));

		dialog.abschnitt(I18n.get("dialog.gruppe.rangliste"));
		dialog.auswahl(LST_RANKING, I18n.get("dialog.label.wertung"),
				new String[] { I18n.get("dialog.schweizer.ranking.mit.buchholz"),
						I18n.get("dialog.schweizer.ranking.ohne.buchholz") },
				defaultRankingModus == SchweizerRankingModus.OHNE_BUCHHOLZ ? 1 : 0);

		return dialog.anzeigen(d -> new TurnierParameter(d.formation(), d.teamname(), d.vereinsname(),
				d.meleeAnmeldung(), TeamAnzeige.ausDialogIndex(d.gewaehlt(LST_SPIELPLAN)),
				d.gewaehlt(LST_RANKING) == 1 ? SchweizerRankingModus.OHNE_BUCHHOLZ
						: SchweizerRankingModus.MIT_BUCHHOLZ));
	}

	public static SchweizerTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerTurnierParameterDialog(workingSpreadsheet);
	}
}
