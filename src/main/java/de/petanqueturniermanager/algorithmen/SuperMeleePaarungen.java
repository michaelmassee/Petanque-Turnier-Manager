/**
 * Erstellung     : 31.08.2017 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.model.MeleeSpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Triplette Teams auffüllen mit Doublette<br>
 * Supermelee <br>
 *
 * @author Michael Massee
 *
 */
@Deprecated(since = "2024-06", forRemoval = true)
public class SuperMeleePaarungen {

	private static final Logger logger = LogManager.getLogger(SuperMeleePaarungen.class);

	private static final int DUMMY_SPIELER_START_NR = 10000;
	private static final int DUMMY_SPIELER_SETZPOS = 999; // damit die nicht im gleichen Team gelost werden
	private static final int MAX_RETRY = 100;

	public MeleeSpielRunde neueSpielrunde(int rndNr, SpielerMeldungen meldungen) throws AlgorithmenException {
		return neueSpielrundeTripletteMode(rndNr, meldungen, false);
	}

	/**
	 * Doublette spielrunde auffüllen mit Triplette
	 *
	 * @param rndNr
	 * @param meldungen
	 * @param nurTriplette
	 * @return
	 * @throws AlgorithmenException
	 */
	public MeleeSpielRunde neueSpielrundeDoubletteMode(int rndNr, SpielerMeldungen meldungen, boolean nurTriplette) throws AlgorithmenException {
		checkNotNull(meldungen, "Meldungen = null");
		SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(meldungen.spieler().size(), SuperMeleeMode.Doublette);

		if (!teamRechner.valideAnzahlSpieler()) {
			return null;
		}

		if (nurTriplette && !teamRechner.isNurTripletteMoeglich()) {
			throw new AlgorithmenException("Keine Triplette Spielrunde möglich");
		}

		MeleeSpielRunde spielRunde;
		if (nurTriplette) {
			spielRunde = generiereNeuSpielrundeMitFesteTeamGroese(rndNr, 3, meldungen);
		} else {
			spielRunde = generiereSpielrundeTripletteMitDummies(rndNr, teamRechner.getAnzDoublette(), meldungen);
		}
		spielRunde.sortiereTeamsNachGroese();
		spielRunde.validateSpielerTeam(null);
		return spielRunde;
	}

	/**
	 * Triplette spielrunde auffüllen mit Doublette
	 *
	 * @param rndNr
	 * @param meldungen
	 * @param nurDoublette
	 * @return
	 * @throws AlgorithmenException
	 */
	public MeleeSpielRunde neueSpielrundeTripletteMode(int rndNr, SpielerMeldungen meldungen, boolean nurDoublette) throws AlgorithmenException {
		checkNotNull(meldungen, "Meldungen = null");

		SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(meldungen.spieler().size(), SuperMeleeMode.Triplette);

		if (!teamRechner.valideAnzahlSpieler()) {
			return null;
		}

		if (nurDoublette && !teamRechner.isNurDoubletteMoeglich()) {
			throw new AlgorithmenException("Keine Doublette Spielrunde möglich");
		}

		MeleeSpielRunde spielRunde;
		if (nurDoublette) {
			spielRunde = generiereNeuSpielrundeMitFesteTeamGroese(rndNr, 2, meldungen);
		} else {
			spielRunde = generiereSpielrundeTripletteMitDummies(rndNr, teamRechner.getAnzDoublette(), meldungen);
		}
		spielRunde.sortiereTeamsNachGroese();
		spielRunde.validateSpielerTeam(null);
		return spielRunde;
	}

	/**
	 * Generiert eine Triplette-Spielrunde, indem Dummy-Spieler für die Doublette-Slots eingefügt
	 * und nach der Generierung wieder entfernt werden.
	 */
	private MeleeSpielRunde generiereSpielrundeTripletteMitDummies(int rndNr, int anzDummies, SpielerMeldungen meldungen)
			throws AlgorithmenException {
		for (int i = 0; i < anzDummies; i++) {
			meldungen.addSpielerWennNichtVorhanden(Spieler.from(DUMMY_SPIELER_START_NR + i).setSetzPos(DUMMY_SPIELER_SETZPOS));
		}
		MeleeSpielRunde spielRunde = generiereNeuSpielrundeMitFesteTeamGroese(rndNr, 3, meldungen);
		for (int i = 0; i < anzDummies; i++) {
			Spieler dummy = Spieler.from(DUMMY_SPIELER_START_NR + i);
			spielRunde.removeSpieler(dummy);
			meldungen.removeSpieler(dummy);
		}
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
	MeleeSpielRunde generiereNeuSpielrundeMitFesteTeamGroese(int rndNr, int teamSize, SpielerMeldungen meldungen) throws AlgorithmenException {

		MeleeSpielRunde spielrunde = null;
		int retryCnt = 1;

		while (retryCnt < MAX_RETRY) {

			spielrunde = new MeleeSpielRunde(rndNr);
			// von alle meldungen die teams löschen
			meldungen.resetTeam();
			meldungen.shuffle();

			try {
				while (meldungen.spielerOhneTeam().size() > 0) {
					findNextTeamInSpielrunde(teamSize, meldungen, spielrunde);
				}
				retryCnt = MAX_RETRY;
			} catch (AlgorithmenException e) {
				retryCnt++;
				if (retryCnt == MAX_RETRY) {
					logger.warn("Maximale Retry-Anzahl ({}) erreicht, Spielrunde konnte nicht generiert werden", MAX_RETRY);
					throw e;
				}
				spielrunde.deleteAllTeams();
			}
		}

		return spielrunde;
	}

	public Team findNextTeamInSpielrunde(int teamSize, SpielerMeldungen meldungen, MeleeSpielRunde spielrunde) throws AlgorithmenException {

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
			throw new AlgorithmenException("Team " + newTeamInRunde.getNr() + " für Spielrunde " + spielrunde.getNr() + " konnte nicht zusammengestelt werden");
		}

		spielrunde.validateSpielerTeam(null);

		return newTeamInRunde;
	}

	private void teamAuffuellen(int teamSize, SpielerMeldungen meldungen, MeleeSpielRunde spielrunde, Team newTeamInRunde) throws AlgorithmenException {
		boolean konnteTauschen = true;
		while (newTeamInRunde.size() != teamSize && meldungen.spielerOhneTeam().size() > 0 && konnteTauschen) {
			// team noch nicht vollständig, versuche zu tauschen mit einem Spieler
			// aus einem anderen Team
			konnteTauschen = false;
			for (Spieler spielerOhneTeam : meldungen.spielerOhneTeam()) {
				Spieler tauschSpieler = kannTauschenMitSpielerOhneTeam(spielerOhneTeam, newTeamInRunde, spielrunde.teams());
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

	public Spieler kannTauschenMitSpielerOhneTeam(Spieler spielerOhneTeam, Team newTeamInRunde, List<Team> alleteams) throws AlgorithmenException {
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
