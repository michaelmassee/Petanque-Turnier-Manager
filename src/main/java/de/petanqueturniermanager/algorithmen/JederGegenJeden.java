/**
 * Erstellung 29.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;

/**
 * @author Michael Massee
 *
 */
public class JederGegenJeden {

	// Round Robin Polygon Mode
	// https://nrich.maths.org/1443
	// https://www-i1.informatik.rwth-aachen.de/~algorithmus/algo36.php

	private final List<Team> meldungen;
	private final boolean freiSpiel;
	private final int anzMeldungen;

	public JederGegenJeden(TeamMeldungen meldungen) {
		checkNotNull(meldungen);
		this.meldungen = Collections.unmodifiableList(meldungen.teams());
		anzMeldungen = meldungen.teams().size();
		freiSpiel = IsEvenOrOdd.IsOdd(anzMeldungen);
	}

	public int anzRunden() {
		return (freiSpiel) ? anzMeldungen : anzMeldungen - 1;
	}

	public int letzteMeldungNr() {
		return (freiSpiel) ? anzMeldungen + 1 : anzMeldungen;
	}

	public int anzPaarungen() {
		return letzteMeldungNr() / 2;
	}

	public List<List<TeamPaarung>> generate() {
		List<List<TeamPaarung>> result = new ArrayList<>();

		int anzRndn = anzRunden();
		int letzteMeldungNr = letzteMeldungNr();
		int anzPaarungren = anzPaarungen();

		for (int rundenCntr = 1; rundenCntr <= anzRndn; rundenCntr++) {

			List<TeamPaarung> runde = new ArrayList<>();
			result.add(runde);

			// letzte Teampaarung ist einfach nach schema F
			// wegen heim und gast je nach runde umdrehen, nur wenn kein freispiel
			// bei freispiel nicht drehen sondern rehenfolge bleibt
			if (IsEvenOrOdd.IsOdd(rundenCntr) || freiSpiel) {
				runde.add(newTeamPaarung(rundenCntr - 1, letzteMeldungNr - 1));
			} else {
				runde.add(newTeamPaarung(letzteMeldungNr - 1, rundenCntr - 1));
			}

			// restliche paarungen
			for (int teamPaarungcntr = 1; teamPaarungcntr < anzPaarungren; teamPaarungcntr++) {
				int moduloA = (rundenCntr + teamPaarungcntr) % anzRndn;
				int idxMeldungA = (moduloA < 1) ? anzRndn + moduloA : moduloA;
				int moduloB = (rundenCntr - teamPaarungcntr) % anzRndn;
				int idxMeldungB = (moduloB < 1) ? anzRndn + moduloB : moduloB;

				runde.add(newTeamPaarung(idxMeldungA - 1, idxMeldungB - 1));
			}
		}
		return result;
	}

	private TeamPaarung newTeamPaarung(int idxMeldungA, int idxMeldungB) {
		Team teamA = meldungen.get(idxMeldungA);
		Optional<Team> teamB;

		if (freiSpiel && idxMeldungB >= anzMeldungen) {
			teamB = Optional.empty();
		} else {
			teamB = Optional.of(meldungen.get(idxMeldungB));
		}
		TeamPaarung ret = new TeamPaarung(teamA, teamB);
		return ret;
	}

	/**
	 * @return the freiSpiel
	 */
	public final boolean isFreiSpiel() {
		return freiSpiel;
	}

	/**
	 * @return the anzMeldungen
	 */
	public final int getAnzMeldungen() {
		return anzMeldungen;
	}

}

// @formatter:off
/**

'---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
function mathGenerateRundePlan (pTeams as Integer, pSpieleMatrix)
'---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

Dim anzRunden as Integer
Dim rundenCntr as Integer
Dim spielCntr as Integer

Dim teamA as Integer
Dim teamB as Integer

anzRunden = pTeams -1

	for rundenCntr = 1 to anzRunden

		' erste spiel ist einfach nach schema F
		pSpieleMatrix (rundenCntr,(pTeams /2),1) = rundenCntr
		pSpieleMatrix (rundenCntr,(pTeams /2),2) = pTeams

		for spielCntr = 1 to ((pTeams /2) -1)

	       teamA = (rundenCntr + spielCntr) Mod anzRunden
	       if teamA < 1 then
	       	teamA =  anzRunden + teamA
	       end if

	       teamB = (rundenCntr - spielCntr) Mod anzRunden
	       if teamB < 1 then
	       	teamB =  anzRunden + teamB
	       end if


	       if teamB > teamA then
		       pSpieleMatrix (rundenCntr,spielCntr,1) = teamA
			   pSpieleMatrix (rundenCntr,spielCntr,2) = teamB
		   else
		       pSpieleMatrix (rundenCntr,spielCntr,1) = teamB
			   pSpieleMatrix (rundenCntr,spielCntr,2) = teamA
		   end if

		next spielCntr

	next rundenCntr

End function
 *
 **/

//@formatter:on
