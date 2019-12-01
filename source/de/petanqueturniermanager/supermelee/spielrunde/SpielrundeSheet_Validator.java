package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;

// prüfen alle spielrunden für ein spieltag auf doppelte paarungen
public class SpielrundeSheet_Validator extends AbstractSpielrundeSheet {

	private static final Logger logger = LogManager.getLogger(SpielrundeSheet_Validator.class);

	private final MeldeListeSheet_New meldeliste;

	public SpielrundeSheet_Validator(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		meldeliste = new MeldeListeSheet_New(workingSpreadsheet);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {

		// TODO + Prüfen, mögliche fehler ???
		// Junit test für klasse Team
		// Flag für Team in Spieler ??? + weakref zum Team ? ist problem ? anders lösen
		// immer nur mit eine !!! globale liste von spieler instancen arbeiten ???? prüfen !

		SpielTagNr spielTagNr = getKonfigurationSheet().getAktiveSpieltag();
		validateSpieltag(spielTagNr);
	}

	public void validateSpieltag(SpielTagNr spielTagNr) throws GenerateException {
		processBoxinfo("validateSpieltag " + spielTagNr.getNr());

		meldeliste.setSpielTag(spielTagNr);
		List<Spielrunde> spielrunden = new ArrayList<>();

		for (int spielrunde = 1; spielrunde < 99; spielrunde++) {
			SpielRundeNr spielRundeNr = SpielRundeNr.from(spielrunde);
			List<Team> teams = spielrundeEinlesen(spielTagNr, spielRundeNr);

			if (teams != null && teams.size() > 0) {
				spielrunden.add(new Spielrunde(spielTagNr, spielRundeNr, teams));
			} else {
				break;
			}
		}

		SpielerMeldungen alleMeldungen = meldeliste.getAlleMeldungen();
		processBoxinfo("Meldungen " + alleMeldungen.size());
		processBoxinfo("Spielrunden " + spielrunden.size());

		validateDoppelteTeams(alleMeldungen, spielrunden);
		processBoxinfo("Fertig, kein fehler gefunden");
	}

	/**
	 * prüfen ob spieler berits zusammen gespielt haben
	 *
	 * @throws GenerateException
	 */

	private void validateDoppelteTeams(SpielerMeldungen alleMeldungen, List<Spielrunde> spielrunden) throws GenerateException {
		int fehlerCntr = 0;
		int spielrundeCntr = 0;
		for (Spielrunde spielrunde : spielrunden) {
			spielrundeCntr++;
			int teamCntr = 0;
			for (Team team : spielrunde.teamlist) {
				teamCntr++;
				for (Spieler spielerToValidate : team.spieler()) {
					Spieler spielerAusMeldung = alleMeldungen.findSpielerByNr(spielerToValidate.getNr()); // Achtung: gleiche nummer, aber nicht gleiche object !
					if (spielerAusMeldung == null) {
						throw new GenerateException("Spieler darf nicht null sein");
					}

					int warbereitsimTeamMitNr = validateWarImTeam(spielerAusMeldung, spielerToValidate, team);
					if (warbereitsimTeamMitNr > 0) {
						ProcessBox.from().fehler("Doppelte Auslosung gefunden in Spieltag: " + meldeliste.getSpielTag().getNr() + ", Spielrunde: " + spielrundeCntr + ", TeamNr: "
								+ teamCntr + ", Spieler: " + spielerToValidate.getNr() + " hat bereits zusammen gespielt mit " + warbereitsimTeamMitNr);
						fehlerCntr++;
					}
					for (Spieler spielerausTeam : team.spieler()) {
						if (!spielerToValidate.equals(spielerausTeam)) {
							spielerAusMeldung.addWarImTeamMitWennNichtVorhanden(spielerausTeam);
						}
					}
				}
			}
		}

		if (fehlerCntr > 0) {
			throw new GenerateException(fehlerCntr + " Doppelte Auslosung(en) gefunden !!!");
		}

	}

	private int validateWarImTeam(Spieler spielerAusMeldung, Spieler spielerToValidate, Team team) {
		for (Spieler spielerausTeam : team.spieler()) {
			if (!spielerToValidate.equals(spielerausTeam)) { // nicht sich selbst == gleiche nummer
				if (spielerAusMeldung.warImTeamMit(spielerausTeam)) {
					return spielerausTeam.getNr();
				}
			}
		}
		return 0;
	}

	private List<Team> spielrundeEinlesen(SpielTagNr spielTagNr, SpielRundeNr spielRundeNr) throws GenerateException {
		String sheetNamen = getSheetName(spielTagNr, spielRundeNr);

		List<Team> teamList = new ArrayList<>();
		XSpreadsheet spieltag = getSheetHelper().findByName(sheetNamen);

		if (spieltag == null) {
			return teamList;
		}

		getSheetHelper().setActiveSheet(spieltag);

		// spieler einlesen
		Position posSpielrNr = Position.from(ERSTE_SPIELERNR_SPALTE, ERSTE_DATEN_ZEILE);

		try {
			int teamCntr = 1;
			HashSet<Integer> spielrNrSet = new HashSet<>();
			for (int zeileCntr = 0; zeileCntr < 999; zeileCntr++) {
				Integer paarungCntr = getSheetHelper().getIntFromCell(spieltag, Position.from(posSpielrNr).spaltePlus(-1));

				if (paarungCntr < 1) {
					break;
				}
				// Team A = 3 spalten
				Team teamA = new Team(teamCntr++);
				for (int spalteOffsetTeamA = 0; spalteOffsetTeamA < 3; spalteOffsetTeamA++) {
					nextSpielerInTeam(spieltag, posSpielrNr, spielrNrSet, teamA);
				}
				teamList.add(teamA);

				// Team B = 3 spalten
				Team teamB = new Team(teamCntr++);
				for (int spalteOffsetTeamB = 0; spalteOffsetTeamB < 3; spalteOffsetTeamB++) {
					nextSpielerInTeam(spieltag, posSpielrNr, spielrNrSet, teamB);
				}

				teamList.add(teamB);

				posSpielrNr.spalte(ERSTE_SPIELERNR_SPALTE).zeilePlusEins(); // nächste zeile
			}
		} catch (AlgorithmenException e) {
			logger.error(e.getMessage(), e);
			throw new GenerateException(e.getMessage());
		}
		return teamList;
	}

	private void nextSpielerInTeam(XSpreadsheet spieltag, Position posSpielrNr, HashSet<Integer> spielrNrSet, Team team) throws GenerateException, AlgorithmenException {

		Integer spielerNrAusSheet = getSheetHelper().getIntFromCell(spieltag, posSpielrNr);
		if (spielerNrAusSheet > 0) {
			if (spielrNrSet.contains(spielerNrAusSheet)) {
				throw new GenerateException("spieler " + spielerNrAusSheet + " ist doppelt");
			}
			spielrNrSet.add(spielerNrAusSheet);
			team.addSpielerWennNichtVorhanden(Spieler.from(spielerNrAusSheet));
		}
		posSpielrNr.spaltePlusEins();

	}
}

class Spielrunde {

	final SpielTagNr spielTagNr;
	final SpielRundeNr spielRundeNr;
	final List<Team> teamlist;

	public Spielrunde(SpielTagNr spielTagNr, SpielRundeNr spielRundeNr, List<Team> teamlist) {
		this.spielTagNr = checkNotNull(spielTagNr, "spielTagNr==null");
		this.spielRundeNr = checkNotNull(spielRundeNr, "spielRundeNr==null");
		this.teamlist = checkNotNull(teamlist, "teamlist==null");
	}

}
