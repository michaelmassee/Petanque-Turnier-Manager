/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner;
import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner.MeleeSpieler;
import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner.MeleeTeam;
import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeNeuAuslosungPlan.Belegung;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeNeuAuslosungPlan.Bereich;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Übernimmt die offenen, eingecheckten Mêlée-Anmeldungen als Teams in die Meldeliste.
 * <p>
 * Ablauf:
 * <ol>
 * <li>Nr-Spalte des Mêlée-Anmeldung-Sheets lückenlos durchnummerieren ({@link MeleeAnmeldungNummerierung})
 * – deckt z.B. per Copy-Paste eingefügte Zeilen ab, die noch keine Nummer haben.</li>
 * <li>Mêlée-Anmeldung-Sheet lesen, auf <b>offen</b> (noch nicht übernommen) und <b>eingecheckt</b>
 * filtern – nicht erschienene Spieler bleiben unangetastet stehen.</li>
 * <li>Teams über den {@link MeleeAnmeldungTeamBildner} mischen (Setzpositionen werden beachtet),
 * je nach {@link MeleeAnmeldungModus}: {@link MeleeAnmeldungModus#EINFACH} in der Größe der
 * Meldeliste-Formation, die „ohne Freilos"-Modi als gemischte Doubletten/Triplettes mit gerader
 * Teamzahl (nur bei Triplette-Meldeliste, bei Doubletten bleibt Spieler 3 leer). Nicht aufteilbare
 * überzählige Spieler bleiben offen stehen.</li>
 * <li>„Ohne Freilos" und noch keine Spielrunde: Lassen sich die neuen Spieler nicht vollständig zu eigenen
 * Teams formen (z.B. ein einzelner Nachzügler), werden die bisherigen aktiven Mêlée-Teams nach Rückfrage
 * zusammen mit den neuen Spielern komplett neu ausgelost ({@link MeleeNeuAuslosungPlan}). In der Meldeliste
 * nachgetragene Vereine bleiben dabei beim jeweiligen Spieler. Übernommene Spieler ohne Check-in fallen dabei
 * heraus und ihre Anmeldung wird wieder offen.</li>
 * <li>Teams blockweise ans Ende der Meldeliste schreiben (Teamname, Spielernamen, Setzposition,
 * Aktiv-Kennzeichen), jedes mit einer neuen Team-UUID in der Nr-Zelle, und die Meldeliste
 * aktualisieren (die Nummernvergabe behält die UUID).</li>
 * <li>In den übernommenen Mêlée-Zeilen „Übernommen" auf das Team verweisen lassen: die Formel trägt die
 * Team-UUID und zeigt die aktuelle Teamnummer. Wird das Team gelöscht, wird die Zelle leer und der
 * Spieler ist wieder offen. Die Spieler behalten ihre eigene Mêlée-UUID (Nr-Zelle der Anmeldung).</li>
 * </ol>
 * <p>
 * Das Spaltenlayout der Ziel-Meldeliste ist über alle betroffenen Turniersysteme identisch:
 * <pre>
 * Nr | [Teamname] | (Vorname | Nachname | [Verein]) je Spieler | SP | Aktiv
 * </pre>
 * Der Block wird daher ab Spalte&nbsp;1 (direkt hinter der Nr-Spalte) am Stück geschrieben, die Nr-Zellen danach als
 * {@code PTM.ALG.TNRUUID(nr;"<team-uuid>")}. Die Nummern vergibt die Übernahme selbst, in der Reihenfolge der Teams
 * (Doubletten vor Triplettes wie im Supermêlée): Die Meldeliste sortiert neue Zeilen vor ihrer eigenen
 * Nummernvergabe nach Namen, die Reihenfolge ginge sonst verloren. Eine Nr-Zelle mit Nummer und UUID lässt die
 * Nummernvergabe unverändert.
 */
public abstract class AbstractMeleeAnmeldungUebernehmenSheet extends SheetRunner
		implements ISheet, MeleeAnmeldungKonstanten {

	/**
	 * Wert der Aktiv-Spalte für „nimmt teil" – in allen betroffenen Meldelisten-Delegates
	 * identisch als {@code AKTIV_WERT_NIMMT_TEIL = 1} definiert. Übernommene Teams bestehen
	 * ausschließlich aus bereits eingecheckten Spielern und gelten damit automatisch als aktiv.
	 */
	protected static final int AKTIV_WERT_NIMMT_TEIL = 1;

	private static final Logger logger = LogManager.getLogger(AbstractMeleeAnmeldungUebernehmenSheet.class);

	/** Nr-Spalte der Meldeliste: trägt Teamnummer und Team-UUID. */
	private static final int MELDELISTE_NR_SPALTE = 0;

	/** Erste Spalte des Datenblocks in der Meldeliste (direkt hinter der Nr-Spalte). */
	private static final int MELDELISTE_ERSTE_BLOCK_SPALTE = 1;

	protected AbstractMeleeAnmeldungUebernehmenSheet(WorkingSpreadsheet workingSpreadsheet,
			TurnierSystem turnierSystem, String logPrefix) {
		super(workingSpreadsheet, turnierSystem, logPrefix);
	}

	@Override
	protected void doRun() throws GenerateException {
		uebernehmen();
	}

	/**
	 * Führt die Übernahme synchron aus. Der öffentliche Einstiegspunkt erlaubt
	 * UI-Regressionstests, die zwei aufeinanderfolgende Übernahmen prüfen.
	 */
	public void uebernehmen() throws GenerateException {
		if (!MeleeAnmeldungKonfiguration.istAktiv(getKonfigurationSheet())) {
			zeigeWarnung("msg.text.melee.nicht.aktiv");
			return;
		}

		MeleeAnmeldungNummerierung.nummerieren(getWorkingSpreadsheet(), getXSpreadSheet());

		List<MeleeAnmeldungZeile> alleZeilen = MeleeAnmeldungLeser.lesen(getWorkingSpreadsheet(),
				getMeleeMetadatenSchluessel());
		List<MeleeAnmeldungZeile> offeneEingecheckte = alleZeilen.stream()
				.filter(MeleeAnmeldungZeile::istOffen)
				.filter(MeleeAnmeldungZeile::eingecheckt)
				.toList();
		if (offeneEingecheckte.isEmpty()) {
			zeigeWarnung("msg.text.melee.keine.anmeldungen");
			return;
		}

		if (!namenEindeutig(alleZeilen)) {
			return;
		}

		MeleeAnmeldungModus modus = getKonfigurationSheet().getMeleeAnmeldungModus();
		if (!modus.passtZu(getFormationKonfiguration().getMeldeListeFormation())) {
			zeigeWarnung("msg.text.melee.modus.braucht.triplette", modus.getBezeichnung());
			return;
		}

		Optional<SuperMeleeMode> superMeleeMode = modus.superMeleeMode();
		boolean turnierGestartet = istTurnierGestartet();
		if (superMeleeMode.isPresent() && !turnierGestartet
				&& !alleOhneFreilosVerteilbar(offeneEingecheckte.size())
				&& neuAuslosen(superMeleeMode.get(), alleZeilen, offeneEingecheckte)) {
			return;
		}

		List<MeleeTeam> teams = bildeTeams(modus, alsMeleeSpieler(offeneEingecheckte));
		if (teams.isEmpty()) {
			zeigeWarnung(superMeleeMode.isPresent() && turnierGestartet
					? "msg.text.melee.zu.wenige.spieler.turnier.gestartet"
					: "msg.text.melee.zu.wenige.spieler", offeneEingecheckte.size());
			return;
		}

		Set<Integer> uebernommeneZeilen = teams.stream().flatMap(t -> t.spieler().stream())
				.map(MeleeSpieler::zeile).collect(Collectors.toSet());
		processBoxinfo("processbox.melee.uebernehmen", uebernommeneZeilen.size());
		List<MeleeAnmeldungZeile> tatsaechlichUebernommene = offeneEingecheckte.stream()
				.filter(z -> uebernommeneZeilen.contains(z.zeile())).toList();

		List<String> teamUuids = neueTeamUuids(teams.size());
		List<Integer> nummern = MeleeNeuAuslosungPlan.teamNummern(List.of(), teams.size(),
				leseMeldeliste(naechsteFreieMeldelisteZeile()).hoechsteNr());
		schreibeTeamsInMeldeliste(teams, nummern, teamUuids);
		meldelisteAktualisieren();
		markiereAlsUebernommen(teamUuidProMeleeZeile(teams, teamUuids), List.of());
		meldeOffenGebliebene(offeneEingecheckte.size() - tatsaechlichUebernommene.size());
	}

	/**
	 * Prüft vor der Übernahme auf doppelte Namen: mehrfach eingecheckt im Mêlée-Blatt oder schon von Hand in der
	 * Meldeliste. Betroffene Namen werden in beiden Blättern rot markiert, die Übernahme bricht mit einer Meldung
	 * ab. Bewusst keine Live-Regel im Mêlée-Blatt – die würde jede Eingabe bremsen. Die Markierung eines früheren
	 * Laufs wird im Mêlée-Blatt hier zurückgesetzt, in der Meldeliste bei deren Aktualisieren.
	 */
	private boolean namenEindeutig(List<MeleeAnmeldungZeile> alleZeilen) throws GenerateException {
		XSpreadsheet meleeSheet = MeleeAnmeldungLeser.findeSheet(getWorkingSpreadsheet(), getMeleeMetadatenSchluessel());
		int letzteZeile = alleZeilen.stream().mapToInt(MeleeAnmeldungZeile::zeile).max().orElse(ERSTE_DATEN_ZEILE);
		if (meleeSheet != null) {
			setzeNamensSchrift(meleeSheet, RangePosition.from(SPALTE_VORNAME, ERSTE_DATEN_ZEILE, SPALTE_NACHNAME,
					letzteZeile), ColorHelper.CHAR_COLOR_BLACK);
		}
		MeldelisteInhalt meldeliste = leseMeldeliste(naechsteFreieMeldelisteZeile());
		MeleeDoppelteNamen.Ergebnis ergebnis = MeleeDoppelteNamen.pruefe(alleZeilen,
				meldeliste.spielerJeZeile().stream().flatMap(Set::stream).collect(Collectors.toSet()));
		if (ergebnis.istFehlerfrei()) {
			return true;
		}

		Set<Integer> fehlerZeilen = new TreeSet<>(ergebnis.doppelteMeleeZeilen());
		fehlerZeilen.addAll(ergebnis.meldelisteKonflikte());
		if (meleeSheet != null) {
			for (int zeile : fehlerZeilen) {
				setzeNamensSchrift(meleeSheet, RangePosition.from(SPALTE_VORNAME, zeile, SPALTE_NACHNAME, zeile),
						ColorHelper.CHAR_COLOR_RED);
			}
		}
		XSpreadsheet meldelisteSheet = getMeldeliste().getXSpreadSheet();
		for (SpielerZelle zelle : meldeliste.spielerZellen()) {
			if (ergebnis.meldelisteSchluessel().contains(zelle.schluessel())) {
				setzeNamensSchrift(meldelisteSheet, RangePosition.from(zelle.vornameSpalte(), zelle.zeile(),
						zelle.vornameSpalte() + 1, zelle.zeile()), ColorHelper.CHAR_COLOR_RED);
			}
		}

		String inMeldeliste = I18n.get("melee.doppelt.in.meldeliste");
		String namen = alleZeilen.stream().filter(z -> fehlerZeilen.contains(z.zeile()))
				.map(z -> "• " + z.anzeigeName()
						+ (ergebnis.meldelisteKonflikte().contains(z.zeile()) ? " (" + inMeldeliste + ")" : ""))
				.distinct().collect(Collectors.joining("\n"));
		zeigeWarnung("msg.text.melee.doppelte.namen", namen);
		return false;
	}

	private void setzeNamensSchrift(XSpreadsheet sheet, RangePosition bereich, int farbe) throws GenerateException {
		BlattschutzManager.get().ensureUnprotectedInScope();
		getSheetHelper().setPropertiesInRange(sheet, bereich, CellProperties.from().setCharColor(farbe));
	}

	private static boolean alleOhneFreilosVerteilbar(int anzSpieler) {
		return MeleeAnmeldungTeamBildner.anzahlVerwendbarOhneFreilos(anzSpieler) == anzSpieler;
	}

	/**
	 * Lost die bisherigen Mêlée-Teams der Meldeliste zusammen mit den neuen Spielern komplett neu aus.
	 *
	 * @return {@code true} wenn der Fall hier abschließend behandelt wurde (neu ausgelost oder vom
	 *         Anwender abgebrochen); {@code false} wenn ein Neu-Auslosen nichts bringt – dann greift die
	 *         normale Übernahme
	 */
	private boolean neuAuslosen(SuperMeleeMode mode, List<MeleeAnmeldungZeile> alleZeilen,
			List<MeleeAnmeldungZeile> neue) throws GenerateException {
		int ersteFreieZeile = naechsteFreieMeldelisteZeile();
		MeldelisteInhalt meldeliste = leseMeldeliste(ersteFreieZeile);
		List<Set<String>> spielerJeZeile = meldeliste.spielerJeZeile();
		Set<String> uebernommeneNamen = alleZeilen.stream().filter(MeleeAnmeldungZeile::uebernommen)
				.map(AbstractMeleeAnmeldungUebernehmenSheet::namensSchluessel).collect(Collectors.toSet());
		List<Integer> meleeTeamIndizes = MeleeNeuAuslosungPlan.meleeTeamZeilen(spielerJeZeile, meldeliste.aktiv(),
				uebernommeneNamen);
		if (meleeTeamIndizes.isEmpty()) {
			return false;
		}

		// Nur Spieler, die noch in einem Mêlée-Team der Meldeliste stehen – von Hand gelöschte Teams
		// kommen nicht zurück. Die neuen Spieler stehen hinten: bleibt ein Spieler übrig, ist es ein neuer.
		// Wem der Check-in entzogen wurde, fällt heraus und wird wieder offen.
		Set<String> namenInMeleeTeams = meleeTeamIndizes.stream().flatMap(i -> spielerJeZeile.get(i).stream())
				.collect(Collectors.toSet());
		Map<Boolean, List<MeleeAnmeldungZeile>> bisherigeNachCheckin = alleZeilen.stream()
				.filter(MeleeAnmeldungZeile::uebernommen)
				.filter(z -> namenInMeleeTeams.contains(namensSchluessel(z)))
				.collect(Collectors.partitioningBy(MeleeAnmeldungZeile::eingecheckt));
		List<MeleeAnmeldungZeile> bisherige = bisherigeNachCheckin.get(true);
		List<MeleeAnmeldungZeile> ausgecheckte = bisherigeNachCheckin.get(false);
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(
				alsMeleeSpieler(Stream.concat(bisherige.stream(), neue.stream()).toList()), mode);
		Set<Integer> verteilteZeilen = teams.stream().flatMap(t -> t.spieler().stream())
				.map(MeleeSpieler::zeile).collect(Collectors.toSet());
		List<MeleeAnmeldungZeile> neuVerteilt = neue.stream().filter(z -> verteilteZeilen.contains(z.zeile()))
				.toList();
		if (neuVerteilt.isEmpty()) {
			return false;
		}

		MessageBoxResult antwort = zeigeAufHauptthread(MessageBox.from(getxContext(), MessageBoxTypeEnum.QUESTION_YES_NO)
				.caption(I18n.get("msg.caption.melee.uebernehmen"))
				.message(I18n.get("msg.text.melee.neu.auslosen.frage", neue.size(), meleeTeamIndizes.size())));
		if (antwort != MessageBoxResult.YES) {
			return true;
		}

		processBoxinfo("processbox.melee.neu.auslosen", verteilteZeilen.size());
		List<Integer> bisherigeZeilen = meleeTeamIndizes.stream()
				.map(index -> MeldeListeKonstanten.TEAM_MELDELISTE_ERSTE_DATEN_ZEILE + index).toList();
		// Neue Zusammensetzung = neue Team-Identität: alle Teams erhalten eine neue UUID, und jeder Spieler
		// verweist in „Übernommen“ auf sein neues Team. Die bisherigen Nummern werden weiterverwendet.
		List<String> teamUuids = neueTeamUuids(teams.size());
		List<Integer> nummern = MeleeNeuAuslosungPlan.teamNummern(
				meleeTeamIndizes.stream().map(meldeliste.nummern()::get).toList(), teams.size(), meldeliste.hoechsteNr());
		schreibeNeuAusgeloste(new NeueTeams(teams, nummern, teamUuids), MeleeNeuAuslosungPlan.zeilenZuordnung(
				bisherigeZeilen, teams.size(), ersteFreieZeile), meldeliste.vereinJeSpieler());
		meldelisteAktualisieren();
		markiereAlsUebernommen(teamUuidProMeleeZeile(teams, teamUuids), ausgecheckte);
		meldeOffenGebliebene(neue.size() - neuVerteilt.size());
		return true;
	}

	/**
	 * Ob für dieses Turnier schon eine Spielrunde (bzw. ein Spielplan/Turnierbaum) angelegt ist – dann
	 * dürfen die bisherigen Teams nicht mehr verändert werden.
	 */
	private boolean istTurnierGestartet() {
		XSpreadsheetDocument xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		return getSpielrundenSchluesselPraefixe().stream()
				.flatMap(praefix -> Arrays.stream(SheetMetadataHelper.getSchluesselMitPrefix(xDoc, praefix)))
				.anyMatch(schluessel -> SheetMetadataHelper.findeSheet(xDoc, schluessel).isPresent());
	}

	private void meldeOffenGebliebene(int anzahlOffenGeblieben) throws GenerateException {
		if (anzahlOffenGeblieben > 0) {
			zeigeHinweis("msg.text.melee.spieler.offen.geblieben", anzahlOffenGeblieben);
		}
	}

	/**
	 * Mischt die Spieler je nach Modus zu Teams fester Größe (Meldeliste-Formation) oder zu
	 * gemischten Doubletten/Triplettes ohne Freilos.
	 */
	private List<MeleeTeam> bildeTeams(MeleeAnmeldungModus modus, List<MeleeSpieler> spieler) {
		Optional<SuperMeleeMode> superMeleeMode = modus.superMeleeMode();
		if (superMeleeMode.isPresent()) {
			return MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(spieler, superMeleeMode.get());
		}
		int teamGroesse = getFormationKonfiguration().getMeldeListeFormation().getAnzSpieler();
		return MeleeAnmeldungTeamBildner.bildeTeams(spieler, teamGroesse);
	}

	private static String namensSchluessel(MeleeAnmeldungZeile zeile) {
		return MeleeNeuAuslosungPlan.namensSchluessel(zeile.vorname(), zeile.nachname());
	}

	private static List<MeleeSpieler> alsMeleeSpieler(List<MeleeAnmeldungZeile> zeilen) {
		return zeilen.stream()
				.map(z -> new MeleeSpieler(z.zeile(), z.nr(), z.vorname(), z.nachname(), z.setzPosition()))
				.toList();
	}

	private static List<String> neueTeamUuids(int anzahl) {
		return Stream.generate(() -> UUID.randomUUID().toString()).limit(anzahl).toList();
	}

	private static SortedMap<Integer, String> teamUuidProMeleeZeile(List<MeleeTeam> teams, List<String> teamUuids) {
		SortedMap<Integer, String> ergebnis = new TreeMap<>();
		for (int i = 0; i < teams.size(); i++) {
			String teamUuid = teamUuids.get(i);
			teams.get(i).spieler().forEach(spieler -> ergebnis.put(spieler.zeile(), teamUuid));
		}
		return ergebnis;
	}

	/**
	 * Die geschriebenen Teams mit ihrer Nummer und Team-UUID, jeweils am selben Index.
	 *
	 * @param teams     gebildete Teams
	 * @param nummern   Teamnummer je Team
	 * @param teamUuids Team-UUID je Team
	 */
	private record NeueTeams(List<MeleeTeam> teams, List<Integer> nummern, List<String> teamUuids) {

		String nrFormel(int teamIndex) {
			return GlobalImpl.FORMAT_PTM_TNR_UUID(nummern.get(teamIndex), teamUuids.get(teamIndex));
		}
	}

	/**
	 * Schreibt die gebildeten Teams als zusammenhängenden Block ans Ende der Meldeliste, die Nr-Zellen mit
	 * {@code PTM.ALG.TNRUUID(nr;"<team-uuid>")}, nummeriert hinter der höchsten vorhandenen Nummer.
	 */
	private void schreibeTeamsInMeldeliste(List<MeleeTeam> teams, List<Integer> nummern, List<String> teamUuids)
			throws GenerateException {
		NeueTeams neueTeams = new NeueTeams(teams, nummern, teamUuids);
		int ersteZeile = naechsteFreieMeldelisteZeile();
		RangeData data = new RangeData();
		SortedMap<Integer, String> nrFormeln = new TreeMap<>();
		for (int i = 0; i < teams.size(); i++) {
			testDoCancelTask();
			fuelleTeamZeile(data.addNewRow(), teams.get(i), Map.of());
			nrFormeln.put(ersteZeile + i, neueTeams.nrFormel(i));
		}
		schreibeInMeldeliste(data, ersteZeile);
		schreibeNrFormeln(nrFormeln);
	}

	/**
	 * Schreibt die neu ausgelosten Teams in ihre Zielzeilen und leert überzählige bisherige Mêlée-Zeilen.
	 * Jeder zusammenhängende Zeilenbereich wird am Stück geschrieben; von Hand gemeldete Teams dazwischen
	 * bleiben unberührt.
	 *
	 * @param vereinJeSpieler bisheriger Verein je Namensschlüssel – geht beim Neu-Auslosen nicht verloren
	 */
	private void schreibeNeuAusgeloste(NeueTeams neueTeams, List<Belegung> belegungen,
			Map<String, String> vereinJeSpieler) throws GenerateException {
		int anzBlockSpalten = anzahlBlockSpalten();
		SortedMap<Integer, String> nrFormeln = new TreeMap<>();
		int index = 0;
		for (Bereich bereich : MeleeNeuAuslosungPlan.zusammenhaengendeBereiche(belegungen)) {
			testDoCancelTask();
			RangeData data = new RangeData();
			for (int zeile = bereich.von(); zeile <= bereich.bis(); zeile++, index++) {
				Belegung belegung = belegungen.get(index);
				RowData rowData = data.addNewRow();
				if (belegung.istLeeren()) {
					for (int spalte = 0; spalte < anzBlockSpalten; spalte++) {
						rowData.newEmpty();
					}
				} else {
					fuelleTeamZeile(rowData, neueTeams.teams().get(belegung.teamIndex()), vereinJeSpieler);
					nrFormeln.put(zeile, neueTeams.nrFormel(belegung.teamIndex()));
				}
			}
			schreibeInMeldeliste(data, bereich.von());
		}
		// Geleerte Zeilen behalten ihre Nr bis zum Aktualisieren – die Meldeliste entfernt Zeilen ohne Namen.
		schreibeNrFormeln(nrFormeln);
	}

	/**
	 * Eine Meldelisten-Zeile ab der Spalte hinter der Nr: [Teamname], je Spieler Vorname, Nachname, [Verein], SP,
	 * Aktiv.
	 *
	 * @param vereinJeSpieler bekannter Verein je Namensschlüssel; Spieler ohne Eintrag bekommen einen leeren Verein
	 */
	private void fuelleTeamZeile(RowData zeile, MeleeTeam team, Map<String, String> vereinJeSpieler) {
		if (getFormationKonfiguration().isMeldeListeTeamnameAnzeigen()) {
			// Freier Teamname bleibt leer – bei einer Mêlée-Auslosung gibt es keinen
			// gemeldeten Teamnamen; der Anwender kann ihn nachtragen.
			zeile.newEmpty();
		}
		boolean vereinsnameAktiv = getFormationKonfiguration().isMeldeListeVereinsnameAnzeigen();
		for (int idx = 0; idx < anzahlSpielerSpalten(); idx++) {
			MeleeSpieler spieler = idx < team.spieler().size() ? team.spieler().get(idx) : null;
			zeile.newString(spieler != null ? spieler.vorname() : "");
			zeile.newString(spieler != null ? spieler.nachname() : "");
			if (vereinsnameAktiv) {
				zeile.newString(spieler != null
						? vereinJeSpieler.getOrDefault(
								MeleeNeuAuslosungPlan.namensSchluessel(spieler.vorname(), spieler.nachname()), "")
						: "");
			}
		}
		if (team.setzPosition() > 0) {
			zeile.newInt(team.setzPosition());
		} else {
			zeile.newEmpty();
		}
		zeile.newInt(AKTIV_WERT_NIMMT_TEIL);
	}

	/** Nr-Zellen mit {@code PTM.ALG.TNRUUID(nr;"<team-uuid>")}. */
	private void schreibeNrFormeln(SortedMap<Integer, String> nrFormeln) throws GenerateException {
		TeamNummernVergabe.schreibeNrFormeln(getSheetHelper(), getMeldeliste().getXSpreadSheet(), MELDELISTE_NR_SPALTE,
				nrFormeln);
	}

	private void schreibeInMeldeliste(RangeData data, int ersteZeile) throws GenerateException {
		XSpreadsheet meldelisteSheet = getMeldeliste().getXSpreadSheet();
		RangeHelper.from(meldelisteSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				data.getRangePosition(Position.from(MELDELISTE_ERSTE_BLOCK_SPALTE, ersteZeile))).setDataInRange(data);
	}

	/**
	 * Inhalt der Meldeliste ab der ersten Datenzeile.
	 *
	 * @param nummern         Teamnummer je Zeile (0 wenn leer)
	 * @param spielerJeZeile  Namensschlüssel der Spieler je Zeile
	 * @param aktiv           je Zeile, ob das Team teilnimmt
	 * @param spielerZellen   Position jedes Spielers (für die Fehlermarkierung)
	 * @param vereinJeSpieler eingetragener Verein je Namensschlüssel (nur Spieler mit Verein)
	 */
	private record MeldelisteInhalt(List<Integer> nummern, List<Set<String>> spielerJeZeile, List<Boolean> aktiv,
			List<SpielerZelle> spielerZellen, Map<String, String> vereinJeSpieler) {

		int hoechsteNr() {
			return nummern.stream().mapToInt(Integer::intValue).max().orElse(0);
		}
	}

	/**
	 * Ein Spieler der Meldeliste.
	 *
	 * @param zeile         Blattzeile
	 * @param vornameSpalte Spalte des Vornamens (der Nachname steht rechts daneben)
	 * @param schluessel    Namensschlüssel
	 */
	private record SpielerZelle(int zeile, int vornameSpalte, String schluessel) {
	}

	/** Liest Nummern und Spielernamen aller Meldungen in einem Zugriff. */
	private MeldelisteInhalt leseMeldeliste(int ersteFreieZeile) throws GenerateException {
		int ersteZeile = MeldeListeKonstanten.TEAM_MELDELISTE_ERSTE_DATEN_ZEILE;
		if (ersteFreieZeile <= ersteZeile) {
			return new MeldelisteInhalt(List.of(), List.of(), List.of(), List.of(), Map.of());
		}
		RangePosition bereich = RangePosition.from(MELDELISTE_NR_SPALTE, ersteZeile,
				MELDELISTE_ERSTE_BLOCK_SPALTE + anzahlBlockSpalten() - 1, ersteFreieZeile - 1);
		RangeData data = RangeHelper.from(getMeldeliste().getXSpreadSheet(),
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), bereich).getDataFromRange();
		int ersteSpielerSpalte = MELDELISTE_ERSTE_BLOCK_SPALTE
				+ (getFormationKonfiguration().isMeldeListeTeamnameAnzeigen() ? 1 : 0);
		int spaltenJeSpieler = spaltenJeSpieler();
		boolean vereinsnameAktiv = getFormationKonfiguration().isMeldeListeVereinsnameAnzeigen();
		int aktivSpalte = MELDELISTE_ERSTE_BLOCK_SPALTE + anzahlBlockSpalten() - 1;
		List<Integer> nummern = new ArrayList<>(data.size());
		List<Set<String>> spielerJeZeile = new ArrayList<>(data.size());
		List<Boolean> aktiv = new ArrayList<>(data.size());
		List<SpielerZelle> spielerZellen = new ArrayList<>();
		Map<String, String> vereinJeSpieler = new HashMap<>();
		int blattZeile = ersteZeile;
		for (RowData zeile : data) {
			nummern.add(zeile.get(MELDELISTE_NR_SPALTE).getIntVal(0));
			aktiv.add(zeile.get(aktivSpalte).getIntVal(0) == AKTIV_WERT_NIMMT_TEIL);
			Set<String> spieler = new HashSet<>();
			for (int idx = 0; idx < anzahlSpielerSpalten(); idx++) {
				int spalte = ersteSpielerSpalte + idx * spaltenJeSpieler;
				String vorname = zeile.get(spalte).getStringVal();
				String nachname = zeile.get(spalte + 1).getStringVal();
				if (StringUtils.isNotBlank(vorname) || StringUtils.isNotBlank(nachname)) {
					String schluessel = MeleeNeuAuslosungPlan.namensSchluessel(vorname, nachname);
					spieler.add(schluessel);
					spielerZellen.add(new SpielerZelle(blattZeile, spalte, schluessel));
					if (vereinsnameAktiv && StringUtils.isNotBlank(zeile.get(spalte + 2).getStringVal())) {
						vereinJeSpieler.put(schluessel, zeile.get(spalte + 2).getStringVal());
					}
				}
			}
			spielerJeZeile.add(spieler);
			blattZeile++;
		}
		return new MeldelisteInhalt(nummern, spielerJeZeile, aktiv, spielerZellen, vereinJeSpieler);
	}

	private int anzahlSpielerSpalten() {
		return getFormationKonfiguration().getMeldeListeFormation().getAnzSpieler();
	}

	private int spaltenJeSpieler() {
		return getFormationKonfiguration().isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
	}

	/** Breite des Datenblocks hinter der Nr-Spalte: [Teamname], Spielerspalten, SP, Aktiv. */
	private int anzahlBlockSpalten() {
		int teamname = getFormationKonfiguration().isMeldeListeTeamnameAnzeigen() ? 1 : 0;
		return teamname + anzahlSpielerSpalten() * spaltenJeSpieler() + 2;
	}

	/**
	 * Schreibt in „Übernommen“ der neu übernommenen Mêlée-Zeilen den Verweis auf ihr Team und leert die Zellen der
	 * aus den Teams genommenen Spieler. Nur diese Zellen werden geschrieben (zusammenhängende Zeilen blockweise);
	 * früher übernommene behalten ihren Verweis bzw. ihre Markierung.
	 *
	 * @param entfernte übernommene Spieler, die nicht mehr in der Meldeliste stehen – wieder offen
	 */
	private void markiereAlsUebernommen(SortedMap<Integer, String> teamUuidProMeleeZeile,
			List<MeleeAnmeldungZeile> entfernte) throws GenerateException {
		XSpreadsheet meleeSheet = MeleeAnmeldungLeser.findeSheet(getWorkingSpreadsheet(),
				getMeleeMetadatenSchluessel());
		if (meleeSheet == null) {
			return;
		}
		String nrBereich = MeldeListeHelper.nrBereichMeldeliste();
		SortedMap<Integer, String> formeln = new TreeMap<>();
		teamUuidProMeleeZeile.forEach((zeile, teamUuid) -> formeln.put(zeile,
				NrUuidSpalte.formelNrAusUuid(nrBereich, teamUuid)));
		entfernte.forEach(zeile -> formeln.put(zeile.zeile(), ""));
		TeamNummernVergabe.schreibeNrFormeln(getSheetHelper(), meleeSheet, SPALTE_UEBERNOMMEN, formeln);
	}

	private void zeigeHinweis(String i18nKey, Object... args) throws GenerateException {
		zeigeMeldung(MessageBoxTypeEnum.INFO_OK, i18nKey, args);
	}

	/** Für Fälle, in denen nichts übernommen werden kann – der Menüpunkt bleibt bewusst immer aktiv. */
	private void zeigeWarnung(String i18nKey, Object... args) throws GenerateException {
		zeigeMeldung(MessageBoxTypeEnum.WARN_OK, i18nKey, args);
	}

	private void zeigeMeldung(MessageBoxTypeEnum typ, String i18nKey, Object... args) throws GenerateException {
		zeigeAufHauptthread(MessageBox.from(getxContext(), typ)
				.caption(I18n.get("msg.caption.melee.uebernehmen"))
				.message(I18n.get(i18nKey, args)));
	}

	/**
	 * Zeigt den Dialog auf dem LO-Hauptthread und wartet auf die Antwort. Die Übernahme läuft im
	 * SheetRunner-Thread; ein dort ausgeführter modaler Dialog (Rückfrage zum Neu-Auslosen) ließ nach dem
	 * anschließenden Wechsel zur Meldeliste Tabreiter und Seitenleiste auf dem Mêlée-Blatt stehen.
	 * <p>
	 * Darf nie vom Hauptthread aus aufgerufen werden – das Warten blockierte dann genau den Thread, der den
	 * Dialog ausführen soll. {@link #uebernehmen()} läuft im SheetRunner-Thread bzw. im Testthread.
	 */
	private MessageBoxResult zeigeAufHauptthread(MessageBox dialog) throws GenerateException {
		CompletableFuture<MessageBoxResult> antwort = new CompletableFuture<>();
		LoMainThread.post(getxContext(), () -> {
			try {
				antwort.complete(dialog.show());
			} catch (RuntimeException e) {
				antwort.completeExceptionally(e);
			}
		});
		try {
			return antwort.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new GenerateException(e.getMessage());
		} catch (ExecutionException e) {
			logger.error("Dialog der Mêlée-Übernahme fehlgeschlagen", e);
			throw new GenerateException(String.valueOf(e.getCause()));
		}
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				getMeleeMetadatenSchluessel(), SheetNamen.meleeAnmeldung());
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	// ── system-spezifische Hooks ─────────────────────────────────────────────

	@Override
	protected abstract BaseKonfigurationSheet getKonfigurationSheet();

	/**
	 * Dieselbe Konfiguration wie {@link #getKonfigurationSheet()}, jedoch als
	 * {@link IFormationKonfiguration} – für Formation und Anzeige-Optionen der Meldeliste.
	 */
	protected abstract IFormationKonfiguration getFormationKonfiguration();

	/**
	 * Named-Range-Schlüssel(-Präfixe) der Blätter, die den Turnierbeginn anzeigen (Spielrunde, Spielplan,
	 * Turnierbaum …). Existiert eines davon, werden bisherige Teams nicht mehr neu ausgelost.
	 */
	protected abstract List<String> getSpielrundenSchluesselPraefixe();

	/** Named-Range-Schlüssel des Mêlée-Anmeldung-Sheets. */
	protected abstract String getMeleeMetadatenSchluessel();

	/** Die Ziel-Meldeliste (Quelle des Sheets, in das die Teams geschrieben werden). */
	protected abstract ISheet getMeldeliste();

	/** Erste freie Datenzeile der Meldeliste (0-basiert). */
	protected abstract int naechsteFreieMeldelisteZeile() throws GenerateException;

	/** Aktualisiert die Meldeliste (Nummernvergabe, Formatierung, Druckbereich). */
	protected abstract void meldelisteAktualisieren() throws GenerateException;
}
