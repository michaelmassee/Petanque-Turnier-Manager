package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.*;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;

import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.SpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.supermelee.TeamRechner;

/*
* Paarungen.java
* Erstellung     : 31.08.2017 / Michael Massee
*
*/
public class TripletteDoublPaarungen {

	static int DOUBL_SPIELER_START_NR = 10000;
	static int DOUBL_SPIELER_SETZPOS = 999; // damit die nicht im gleichen Team gelost werden

	public SpielRunde neueSpielrunde(int rndNr, Meldungen meldungen) throws AlgorithmenException {
		return neueSpielrunde(rndNr, meldungen, false);
	}

	public SpielRunde neueSpielrunde(int rndNr, Meldungen meldungen, boolean nurDoublette) throws AlgorithmenException {
		checkNotNull(meldungen, "Meldungen = null");

		TeamRechner teamRechner = new TeamRechner(meldungen.spieler().size());
		int anzDoubletteOrg = teamRechner.getAnzDoublette();

		if (!teamRechner.valideAnzahlSpieler()) {
			return null;
		}

		if (nurDoublette && !teamRechner.isNurDoubletteMoeglich()) {
			throw new AlgorithmenException("Keine Doublette Spielrunde möglich");
		}

		SpielRunde spielRunde = null;
		if (nurDoublette) {
			spielRunde = generiereNeuSpielrundeMitFesteTeamGroese(rndNr, 2, meldungen);
		} else {
			for (int doublDummyCntr = 0; doublDummyCntr < anzDoubletteOrg; doublDummyCntr++) {
				// dummy spieler einfuegen
				meldungen.addSpielerWennNichtVorhanden(
						Spieler.from(DOUBL_SPIELER_START_NR + doublDummyCntr).setSetzPos(DOUBL_SPIELER_SETZPOS));
			}
			teamRechner = new TeamRechner(meldungen.spieler().size());

			spielRunde = generiereNeuSpielrundeMitFesteTeamGroese(rndNr, 3, meldungen);
			// dummies wieder entfernen
			for (int doublDummyCntr = 0; doublDummyCntr < anzDoubletteOrg; doublDummyCntr++) {
				Spieler spieler = Spieler.from(DOUBL_SPIELER_START_NR + doublDummyCntr);
				spielRunde.removeSpieler(spieler);
				meldungen.removeSpieler(spieler);
			}
		}
		spielRunde.sortiereTeamsNachGroese();
		spielRunde.validateSpielerTeam(null);
		return spielRunde;
	}

	/**
	 * nur dann wenn die anzahl der spieler genau aufgeht
	 *
	 * @param rndNr
	 * @param teamSize
	 * @param meldungen
	 * @return
	 * @throws AlgorithmenException
	 */
	@VisibleForTesting
	SpielRunde generiereNeuSpielrundeMitFesteTeamGroese(int rndNr, int teamSize, Meldungen meldungen)
			throws AlgorithmenException {

		SpielRunde spielrunde = null;
		// Team nextTeam;

		int maxRetry = 100;
		int retryCnt = 1;

		while (retryCnt < maxRetry) {

			spielrunde = new SpielRunde(rndNr);
			// von alle meldungen die teams löschen
			meldungen.resetTeam();
			meldungen.shuffle();

			try {
				while (meldungen.spielerOhneTeam().size() > 0) {
					findNextTeamInSpielrunde(teamSize, meldungen, spielrunde);
				}
				retryCnt = maxRetry;
			} catch (AlgorithmenException e) {
				// retry ?
				retryCnt++;
				if (retryCnt == maxRetry) {
					System.out.println("retry " + retryCnt + "/" + maxRetry);
					throw e;
				}
				spielrunde.deleteAllTeams();
			}
		}

		return spielrunde;
	}

	public Team findNextTeamInSpielrunde(int teamSize, Meldungen meldungen, SpielRunde spielrunde)
			throws AlgorithmenException {

		checkNotNull(meldungen);
		checkNotNull(spielrunde);

		Team newTeamInRunde = spielrunde.newTeam();

		for (Spieler spielerOhneTeam : meldungen.spielerOhneTeam()) {
			// alle spieler ohne team, das neue team hinzufuegen, wenn nicht bereits zusammengespielt
			if (!newTeamInRunde.hatZusammenGespieltMit(spielerOhneTeam)) {
				newTeamInRunde.addSpielerWennNichtVorhanden(spielerOhneTeam);
				if (newTeamInRunde.size() == teamSize) {
					break;
				}
			}
		}

		teamAuffuellen(teamSize, meldungen, spielrunde, newTeamInRunde);

		if (newTeamInRunde.size() != teamSize) {
			newTeamInRunde.removeAlleSpieler();
			throw new AlgorithmenException("Team " + newTeamInRunde.getNr() + " für Spielrunde " + spielrunde.getNr()
					+ " konnte nicht zusammengestelt werden");
		}

		spielrunde.validateSpielerTeam(null);

		return newTeamInRunde;
	}

	private void teamAuffuellen(int teamSize, Meldungen meldungen, SpielRunde spielrunde, Team newTeamInRunde)
			throws AlgorithmenException {
		boolean konnteTauschen = true;
		while (newTeamInRunde.size() != teamSize && meldungen.spielerOhneTeam().size() > 0 && konnteTauschen) {
			// team noch nicht vollständig, versuche zu tauschen mit ein Spieler
			// aus ein andere Team
			konnteTauschen = false;
			for (Spieler spielerOhneTeam : meldungen.spielerOhneTeam()) {
				Spieler tauschSpieler = kannTauschenMitSpielerOhneTeam(spielerOhneTeam, newTeamInRunde,
						spielrunde.teams());
				if (tauschSpieler != null && tauschSpieler.getTeam() != null) {
					// tausch spieler aus sein team raus nehmen
					Team tauschTeam = tauschSpieler.getTeam();
					tauschTeam.removeSpieler(tauschSpieler);
					// neue spieler ins tausch team
					tauschTeam.addSpielerWennNichtVorhanden(spielerOhneTeam);
					// tausch spieler ins team
					newTeamInRunde.addSpielerWennNichtVorhanden(tauschSpieler);
					konnteTauschen = true;
					break;
				}
			}
		}
		spielrunde.validateSpielerTeam(null);
	}

	public Spieler kannTauschenMitSpielerOhneTeam(Spieler spielerOhneTeam, Team newTeamInRunde, List<Team> alleteams)
			throws AlgorithmenException {
		checkNotNull(spielerOhneTeam);
		checkNotNull(newTeamInRunde);
		checkNotNull(alleteams);
		checkArgument(!spielerOhneTeam.isIstInTeam());

		Spieler kannTauschenMit = null;

		for (Team tauschTeam : alleteams) {
			for (Spieler spielerTauschKandidat : tauschTeam.spieler()) {

				// suche ein Spieler der mit den andere mitglieder im Team zusammen spielen kann
				if (!newTeamInRunde.hatZusammenGespieltMit(spielerTauschKandidat)) {
					// prüfen ob wir tauschen können
					boolean kannTauschen = true;
					for (Spieler spielerImTauschTeam : tauschTeam.spieler()) {
						// die restliche Spieler pruefen
						if (spielerTauschKandidat != spielerImTauschTeam) {
							if (spielerImTauschTeam.warImTeamMit(spielerOhneTeam)) {
								kannTauschen = false;
							}
						}
					}
					if (kannTauschen) {
						kannTauschenMit = spielerTauschKandidat;
						break;
					}
				}
			}
			if (kannTauschenMit != null) {
				break;
			}
		}

		return kannTauschenMit;
	}

}
