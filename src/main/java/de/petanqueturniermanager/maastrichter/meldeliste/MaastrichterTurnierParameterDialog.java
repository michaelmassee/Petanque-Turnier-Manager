/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierStartDialog;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.ko.konfiguration.KoPropertiesSpalte;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterGruppenModus;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter für ein neues Maastrichter Turnier.
 * <p>
 * Abschnitte:
 * <ul>
 * <li>Meldeliste: Formation, Zusatzspalten „Teamname“ und „Verein“, Mêlée-Anmeldung</li>
 * <li>Vorrunde (Schweizer System): Anzahl Runden, Anzeige der Teams, Wertung</li>
 * <li>Finalgruppen: Einteilung, max. Teams pro Gruppe, Mindestgröße der letzten Gruppe</li>
 * <li>Spielbaum: Anzeige der Teams, Spielbahn, Bahnnummer nur in Runde 1, Spiel um Platz 3/4</li>
 * </ul>
 */
public class MaastrichterTurnierParameterDialog {

	/** Ergebnis des Dialogs. */
	public record TurnierParameter(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
			Optional<MeleeAnmeldungModus> meleeAnmeldung, TeamAnzeige spielplanTeamAnzeige, SchweizerRankingModus rankingModus,
			int anzVorrunden, TeamAnzeige spielbaumTeamAnzeige, SpielrundeSpielbahn spielbaumSpielbahn,
			boolean spielbaumBahnNurRunde1, boolean spielUmPlatz3, int gruppenGroesse, int minLetzteGruppeGroesse,
			MaastrichterGruppenModus gruppenModus) {
	}

	private static final String NF_ANZ_VORRUNDEN = "nfAnzVorrunden";
	private static final String LST_SPIELPLAN = "lstSpielplan";
	private static final String LST_RANKING = "lstRankingModus";
	private static final String LST_GRUPPEN_MODUS = "lstGruppenModus";
	private static final String NF_GRUPPEN_GROESSE = "nfGruppenGroesse";
	private static final String NF_MIN_LETZTE_GRUPPE = "nfMinLetzteGruppe";
	private static final String LST_SPIELBAUM = "lstSpielbaum";
	private static final String LST_SPIELBAHN = "lstSpielbahn";
	private static final String CB_BAHN_NUR_RUNDE1 = "cbBahnNurRunde1";
	private static final String CB_PLATZ3 = "cbSpielUmPlatz3";
	private static final int MIN_VORRUNDEN = 2;
	private static final int MAX_VORRUNDEN = 5;
	private static final int MIN_GRUPPEN_GROESSE = 2;
	private static final int MAX_GRUPPEN_GROESSE = 256;

	private final WorkingSpreadsheet workingSpreadsheet;

	public MaastrichterTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	public Optional<TurnierParameter> show(Formation defaultFormation, boolean defaultTeamnameAnzeigen,
			boolean defaultVereinsnameAnzeigen, TeamAnzeige defaultSpielplanTeamAnzeige,
			SchweizerRankingModus defaultRankingModus, int defaultAnzVorrunden,
			TeamAnzeige defaultSpielbaumTeamAnzeige, SpielrundeSpielbahn defaultSpielbaumSpielbahn,
			boolean defaultSpielbaumBahnNurRunde1, boolean defaultSpielUmPlatz3, int defaultGruppenGroesse,
			int defaultMinLetzteGruppeGroesse,
			MaastrichterGruppenModus defaultGruppenModus)
			throws com.sun.star.uno.Exception {

		TurnierStartDialog dialog = TurnierStartDialog.erstellen(workingSpreadsheet.getxContext(),
				I18n.get("dialog.maastrichter.titel"));
		dialog.meldeliste(defaultFormation, defaultTeamnameAnzeigen, defaultVereinsnameAnzeigen);

		dialog.abschnitt(I18n.get("dialog.gruppe.vorrunde"));
		dialog.zahl(NF_ANZ_VORRUNDEN, I18n.get("dialog.maastrichter.anz.vorrunden.label"), defaultAnzVorrunden,
				MIN_VORRUNDEN, MAX_VORRUNDEN, I18n.get("config.desc.maastrichter.anz.vorrunden"));
		dialog.auswahl(LST_SPIELPLAN, I18n.get("dialog.label.teams.anzeigen.als"), TeamAnzeige.dialogEintraege(),
				defaultSpielplanTeamAnzeige.dialogIndex(), I18n.get("dialog.spielplan.teams.hilfe"));
		dialog.auswahl(LST_RANKING, I18n.get("dialog.label.wertung"),
				new String[] { I18n.get("dialog.schweizer.ranking.mit.buchholz"),
						I18n.get("dialog.schweizer.ranking.ohne.buchholz") },
				defaultRankingModus == SchweizerRankingModus.OHNE_BUCHHOLZ ? 1 : 0);

		dialog.abschnitt(I18n.get("dialog.gruppe.finalgruppen"));
		dialog.auswahl(LST_GRUPPEN_MODUS, I18n.get("dialog.maastrichter.gruppen.modus.label"),
				new String[] { I18n.get("dialog.maastrichter.gruppen.modus.nach.siegen"),
						I18n.get("dialog.maastrichter.gruppen.modus.nach.groesse") },
				defaultGruppenModus == MaastrichterGruppenModus.NACH_GROESSE ? 1 : 0,
				I18n.get("config.desc.maastrichter.gruppen.modus"));
		dialog.zahl(NF_GRUPPEN_GROESSE, I18n.get("dialog.label.max.teams.gruppe"), defaultGruppenGroesse,
				MIN_GRUPPEN_GROESSE, MAX_GRUPPEN_GROESSE, I18n.get("config.desc.ko.gruppen.groesse"));
		dialog.zahl(NF_MIN_LETZTE_GRUPPE, I18n.get("dialog.label.min.letzte.gruppe"), defaultMinLetzteGruppeGroesse,
				MIN_GRUPPEN_GROESSE, MAX_GRUPPEN_GROESSE, I18n.get("config.desc.ko.min.letzte.gruppe.groesse"));

		dialog.abschnitt(I18n.get("dialog.gruppe.spielbaum"));
		dialog.auswahl(LST_SPIELBAUM, I18n.get("dialog.label.teams.anzeigen.als"), TeamAnzeige.dialogEintraege(),
				defaultSpielbaumTeamAnzeige.dialogIndex(), I18n.get("dialog.spielbaum.teams.hilfe"));
		dialog.spielbahn(LST_SPIELBAHN, defaultSpielbaumSpielbahn);
		dialog.checkBox(CB_BAHN_NUR_RUNDE1, I18n.get("dialog.label.bahn.nur.runde1"), defaultSpielbaumBahnNurRunde1,
				I18n.get("config.desc.ko.spielbaum.bahn.nur.runde1"));
		dialog.checkBox(CB_PLATZ3, I18n.get("dialog.label.spiel.um.platz3"), defaultSpielUmPlatz3, null);

		return dialog.anzeigen(d -> new TurnierParameter(d.formation(), d.teamname(), d.vereinsname(),
				d.meleeAnmeldung(), TeamAnzeige.ausDialogIndex(d.gewaehlt(LST_SPIELPLAN)),
				d.gewaehlt(LST_RANKING) == 1 ? SchweizerRankingModus.OHNE_BUCHHOLZ
						: SchweizerRankingModus.MIT_BUCHHOLZ,
				d.zahlWert(NF_ANZ_VORRUNDEN, defaultAnzVorrunden),
				TeamAnzeige.ausDialogIndex(d.gewaehlt(LST_SPIELBAUM)), d.gewaehlteSpielbahn(LST_SPIELBAHN),
				d.istAngehakt(CB_BAHN_NUR_RUNDE1), d.istAngehakt(CB_PLATZ3),
				KoPropertiesSpalte.normalisiereGruppenGroesse(d.zahlWert(NF_GRUPPEN_GROESSE, defaultGruppenGroesse)),
				KoPropertiesSpalte.normalisiereMinLetzteGruppeGroesse(
						d.zahlWert(NF_MIN_LETZTE_GRUPPE, defaultMinLetzteGruppeGroesse)),
				d.gewaehlt(LST_GRUPPEN_MODUS) == 1 ? MaastrichterGruppenModus.NACH_GROESSE
						: MaastrichterGruppenModus.NACH_SIEGEN));
	}

	public static MaastrichterTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new MaastrichterTurnierParameterDialog(workingSpreadsheet);
	}

}
