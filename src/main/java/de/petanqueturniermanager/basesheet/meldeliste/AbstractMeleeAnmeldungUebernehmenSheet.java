/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

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
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
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
 * Teams formen (z.B. ein einzelner Nachzügler), werden die bisherigen Mêlée-Teams nach Rückfrage
 * zusammen mit den neuen Spielern komplett neu ausgelost ({@link MeleeNeuAuslosungPlan}).</li>
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
 * Der Block wird daher ab Spalte&nbsp;1 (direkt hinter der Nr-Spalte) am Stück geschrieben – die
 * laufende Nummer vergibt anschließend die Meldeliste selbst.
 */
public abstract class AbstractMeleeAnmeldungUebernehmenSheet extends SheetRunner
		implements ISheet, MeleeAnmeldungKonstanten {

	/**
	 * Wert der Aktiv-Spalte für „nimmt teil" – in allen betroffenen Meldelisten-Delegates
	 * identisch als {@code AKTIV_WERT_NIMMT_TEIL = 1} definiert. Übernommene Teams bestehen
	 * ausschließlich aus bereits eingecheckten Spielern und gelten damit automatisch als aktiv.
	 */
	protected static final int AKTIV_WERT_NIMMT_TEIL = 1;

	/** Nr-Spalte der Meldeliste: trägt die Team-UUID. */
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
		schreibeTeamsInMeldeliste(teams, teamUuids);
		meldelisteAktualisieren();
		markiereAlsUebernommen(teamUuidProMeleeZeile(teams, teamUuids));
		meldeOffenGebliebene(offeneEingecheckte.size() - tatsaechlichUebernommene.size());
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
		List<Set<String>> spielerJeZeile = leseSpielerJeMeldelisteZeile(ersteFreieZeile);
		Set<String> uebernommeneNamen = alleZeilen.stream().filter(MeleeAnmeldungZeile::uebernommen)
				.map(AbstractMeleeAnmeldungUebernehmenSheet::namensSchluessel).collect(Collectors.toSet());
		List<Integer> meleeTeamIndizes = MeleeNeuAuslosungPlan.meleeTeamZeilen(spielerJeZeile, uebernommeneNamen);
		if (meleeTeamIndizes.isEmpty()) {
			return false;
		}

		// Nur Spieler, die noch in einem Mêlée-Team der Meldeliste stehen – von Hand gelöschte Teams
		// kommen nicht zurück. Die neuen Spieler stehen hinten: bleibt ein Spieler übrig, ist es ein neuer.
		Set<String> namenInMeleeTeams = meleeTeamIndizes.stream().flatMap(i -> spielerJeZeile.get(i).stream())
				.collect(Collectors.toSet());
		List<MeleeAnmeldungZeile> bisherige = alleZeilen.stream().filter(MeleeAnmeldungZeile::uebernommen)
				.filter(z -> namenInMeleeTeams.contains(namensSchluessel(z))).toList();
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(
				alsMeleeSpieler(Stream.concat(bisherige.stream(), neue.stream()).toList()), mode);
		Set<Integer> verteilteZeilen = teams.stream().flatMap(t -> t.spieler().stream())
				.map(MeleeSpieler::zeile).collect(Collectors.toSet());
		List<MeleeAnmeldungZeile> neuVerteilt = neue.stream().filter(z -> verteilteZeilen.contains(z.zeile()))
				.toList();
		if (neuVerteilt.isEmpty()) {
			return false;
		}

		MessageBoxResult antwort = MessageBox.from(getxContext(), MessageBoxTypeEnum.QUESTION_YES_NO)
				.caption(I18n.get("msg.caption.melee.uebernehmen"))
				.message(I18n.get("msg.text.melee.neu.auslosen.frage", neue.size(), meleeTeamIndizes.size()))
				.show();
		if (antwort != MessageBoxResult.YES) {
			return true;
		}

		processBoxinfo("processbox.melee.neu.auslosen", verteilteZeilen.size());
		List<Integer> bisherigeZeilen = meleeTeamIndizes.stream()
				.map(index -> MeldeListeKonstanten.TEAM_MELDELISTE_ERSTE_DATEN_ZEILE + index).toList();
		// Neue Zusammensetzung = neue Team-Identität: alle Teams erhalten eine neue UUID, und jeder Spieler
		// verweist in „Übernommen“ auf sein neues Team.
		List<String> teamUuids = neueTeamUuids(teams.size());
		schreibeNeuAusgeloste(teams, teamUuids, MeleeNeuAuslosungPlan.zeilenZuordnung(bisherigeZeilen,
				teams.size(), ersteFreieZeile));
		meldelisteAktualisieren();
		markiereAlsUebernommen(teamUuidProMeleeZeile(teams, teamUuids));
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

	private void meldeOffenGebliebene(int anzahlOffenGeblieben) {
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
	 * Schreibt die gebildeten Teams als zusammenhängenden Block ans Ende der Meldeliste, die Nr-Zellen mit
	 * {@code PTM.ALG.TNRUUID(0;"<team-uuid>")} – die Nummer vergibt anschließend die Meldeliste.
	 */
	private void schreibeTeamsInMeldeliste(List<MeleeTeam> teams, List<String> teamUuids) throws GenerateException {
		int ersteZeile = naechsteFreieMeldelisteZeile();
		RangeData data = new RangeData();
		SortedMap<Integer, String> nrFormeln = new TreeMap<>();
		for (int i = 0; i < teams.size(); i++) {
			testDoCancelTask();
			fuelleTeamZeile(data.addNewRow(), teams.get(i));
			nrFormeln.put(ersteZeile + i, GlobalImpl.FORMAT_PTM_TNR_UUID(0, teamUuids.get(i)));
		}
		schreibeInMeldeliste(data, ersteZeile);
		schreibeNrFormeln(nrFormeln);
	}

	/**
	 * Schreibt die neu ausgelosten Teams in ihre Zielzeilen und leert überzählige bisherige Mêlée-Zeilen.
	 * Jeder zusammenhängende Zeilenbereich wird am Stück geschrieben; von Hand gemeldete Teams dazwischen
	 * bleiben unberührt.
	 */
	private void schreibeNeuAusgeloste(List<MeleeTeam> teams, List<String> teamUuids, List<Belegung> belegungen)
			throws GenerateException {
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
					fuelleTeamZeile(rowData, teams.get(belegung.teamIndex()));
					nrFormeln.put(zeile, GlobalImpl.FORMAT_PTM_TNR_UUID(0, teamUuids.get(belegung.teamIndex())));
				}
			}
			schreibeInMeldeliste(data, bereich.von());
		}
		// Geleerte Zeilen behalten ihre Nr bis zum Aktualisieren – die Meldeliste entfernt Zeilen ohne Namen.
		schreibeNrFormeln(nrFormeln);
	}

	/** Eine Meldelisten-Zeile ab der Spalte hinter der Nr: [Teamname], je Spieler Vorname, Nachname, [Verein], SP, Aktiv. */
	private void fuelleTeamZeile(RowData zeile, MeleeTeam team) {
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
				zeile.newEmpty();
			}
		}
		if (team.setzPosition() > 0) {
			zeile.newInt(team.setzPosition());
		} else {
			zeile.newEmpty();
		}
		zeile.newInt(AKTIV_WERT_NIMMT_TEIL);
	}

	/** Nr-Zellen mit {@code PTM.ALG.TNRUUID(0;"<team-uuid>")} – die Nummer vergibt anschließend die Meldeliste. */
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
	 * Liest die Spielernamen aller Meldungen in einem Zugriff.
	 *
	 * @return je Meldelisten-Zeile ab der ersten Datenzeile die Namensschlüssel ihrer Spieler
	 */
	private List<Set<String>> leseSpielerJeMeldelisteZeile(int ersteFreieZeile) throws GenerateException {
		int ersteZeile = MeldeListeKonstanten.TEAM_MELDELISTE_ERSTE_DATEN_ZEILE;
		if (ersteFreieZeile <= ersteZeile) {
			return List.of();
		}
		RangePosition bereich = RangePosition.from(MELDELISTE_ERSTE_BLOCK_SPALTE, ersteZeile,
				MELDELISTE_ERSTE_BLOCK_SPALTE + anzahlBlockSpalten() - 1, ersteFreieZeile - 1);
		RangeData data = RangeHelper.from(getMeldeliste().getXSpreadSheet(),
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), bereich).getDataFromRange();
		int ersteSpielerSpalte = getFormationKonfiguration().isMeldeListeTeamnameAnzeigen() ? 1 : 0;
		int spaltenJeSpieler = spaltenJeSpieler();
		List<Set<String>> spielerJeZeile = new ArrayList<>(data.size());
		for (RowData zeile : data) {
			Set<String> spieler = new HashSet<>();
			for (int idx = 0; idx < anzahlSpielerSpalten(); idx++) {
				int spalte = ersteSpielerSpalte + idx * spaltenJeSpieler;
				String vorname = zeile.get(spalte).getStringVal();
				String nachname = zeile.get(spalte + 1).getStringVal();
				if (StringUtils.isNotBlank(vorname) || StringUtils.isNotBlank(nachname)) {
					spieler.add(MeleeNeuAuslosungPlan.namensSchluessel(vorname, nachname));
				}
			}
			spielerJeZeile.add(spieler);
		}
		return spielerJeZeile;
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
	 * Schreibt in „Übernommen“ der neu übernommenen Mêlée-Zeilen den Verweis auf ihr Team. Nur diese Zellen werden
	 * geschrieben (zusammenhängende Zeilen blockweise); früher übernommene behalten ihren Verweis bzw. ihre
	 * Markierung.
	 */
	private void markiereAlsUebernommen(SortedMap<Integer, String> teamUuidProMeleeZeile) throws GenerateException {
		XSpreadsheet meleeSheet = MeleeAnmeldungLeser.findeSheet(getWorkingSpreadsheet(),
				getMeleeMetadatenSchluessel());
		if (meleeSheet == null || teamUuidProMeleeZeile.isEmpty()) {
			return;
		}
		String nrBereich = MeldeListeHelper.nrBereichMeldeliste();
		SortedMap<Integer, String> formeln = new TreeMap<>();
		teamUuidProMeleeZeile.forEach((zeile, teamUuid) -> formeln.put(zeile,
				NrUuidSpalte.formelNrAusUuid(nrBereich, teamUuid)));
		TeamNummernVergabe.schreibeNrFormeln(getSheetHelper(), meleeSheet, SPALTE_UEBERNOMMEN, formeln);
	}

	private void zeigeHinweis(String i18nKey, Object... args) {
		zeigeMeldung(MessageBoxTypeEnum.INFO_OK, i18nKey, args);
	}

	/** Für Fälle, in denen nichts übernommen werden kann – der Menüpunkt bleibt bewusst immer aktiv. */
	private void zeigeWarnung(String i18nKey, Object... args) {
		zeigeMeldung(MessageBoxTypeEnum.WARN_OK, i18nKey, args);
	}

	private void zeigeMeldung(MessageBoxTypeEnum typ, String i18nKey, Object... args) {
		MessageBox.from(getxContext(), typ)
				.caption(I18n.get("msg.caption.melee.uebernehmen"))
				.message(I18n.get(i18nKey, args))
				.show();
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
