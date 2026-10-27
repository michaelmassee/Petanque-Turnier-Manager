/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner;
import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner.MeleeSpieler;
import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner.MeleeTeam;
import de.petanqueturniermanager.basesheet.konfiguration.BaseKonfigurationSheet;
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
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
			zeigeHinweis("msg.text.melee.nicht.aktiv");
			return;
		}

		MeleeAnmeldungNummerierung.nummerieren(getWorkingSpreadsheet(), getXSpreadSheet());

		List<MeleeAnmeldungZeile> alleZeilen = MeleeAnmeldungLeser.lesen(getWorkingSpreadsheet(),
				getMeleeMetadatenSchluessel());
		List<MeleeAnmeldungZeile> offeneEingecheckte = alleZeilen.stream()
				.filter(MeleeAnmeldungZeile::istOffen)
				.filter(MeleeAnmeldungZeile::eingecheckt)
				.toList();
		if (offeneEingecheckte.size() < 2) {
			zeigeHinweis("msg.text.melee.keine.anmeldungen");
			return;
		}

		MeleeAnmeldungModus modus = getKonfigurationSheet().getMeleeAnmeldungModus();
		if (!modus.passtZu(getFormationKonfiguration().getMeldeListeFormation())) {
			zeigeHinweisMitParameter("msg.text.melee.modus.braucht.triplette", modus.getBezeichnung());
			return;
		}

		List<MeleeTeam> teams = bildeTeams(modus, alsMeleeSpieler(offeneEingecheckte));
		if (teams.isEmpty()) {
			zeigeHinweis("msg.text.melee.keine.anmeldungen");
			return;
		}

		Set<Integer> uebernommeneZeilen = teams.stream().flatMap(t -> t.spieler().stream())
				.map(MeleeSpieler::zeile).collect(Collectors.toSet());
		processBoxinfo("processbox.melee.uebernehmen", uebernommeneZeilen.size());
		List<MeleeAnmeldungZeile> tatsaechlichUebernommene = offeneEingecheckte.stream()
				.filter(z -> uebernommeneZeilen.contains(z.zeile())).toList();

		List<String> teamUuids = teams.stream().map(team -> UUID.randomUUID().toString()).toList();
		schreibeTeamsInMeldeliste(teams, teamUuids);
		meldelisteAktualisieren();
		markiereAlsUebernommen(teamUuidProMeleeZeile(teams, teamUuids));

		int anzahlOffenGeblieben = offeneEingecheckte.size() - tatsaechlichUebernommene.size();
		if (anzahlOffenGeblieben > 0) {
			zeigeHinweisMitParameter("msg.text.melee.spieler.offen.geblieben", anzahlOffenGeblieben);
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

	private static List<MeleeSpieler> alsMeleeSpieler(List<MeleeAnmeldungZeile> zeilen) {
		return zeilen.stream()
				.map(z -> new MeleeSpieler(z.zeile(), z.nr(), z.vorname(), z.nachname(), z.setzPosition()))
				.toList();
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
		Formation formation = getFormationKonfiguration().getMeldeListeFormation();
		boolean teamnameAktiv = getFormationKonfiguration().isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAktiv = getFormationKonfiguration().isMeldeListeVereinsnameAnzeigen();
		int anzSpielerSpalten = formation.getAnzSpieler();

		RangeData data = new RangeData();
		for (MeleeTeam team : teams) {
			testDoCancelTask();
			RowData zeile = data.addNewRow();
			if (teamnameAktiv) {
				// Freier Teamname bleibt leer – bei einer Mêlée-Auslosung gibt es keinen
				// gemeldeten Teamnamen; der Anwender kann ihn nachtragen.
				zeile.newEmpty();
			}
			for (int idx = 0; idx < anzSpielerSpalten; idx++) {
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

		int ersteZeile = naechsteFreieMeldelisteZeile();
		Position start = Position.from(MELDELISTE_ERSTE_BLOCK_SPALTE, ersteZeile);
		XSpreadsheet meldelisteSheet = getMeldeliste().getXSpreadSheet();
		RangeHelper.from(meldelisteSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				data.getRangePosition(start)).setDataInRange(data);

		SortedMap<Integer, String> nrFormeln = new TreeMap<>();
		for (int i = 0; i < teamUuids.size(); i++) {
			nrFormeln.put(ersteZeile + i, GlobalImpl.FORMAT_PTM_TNR_UUID(0, teamUuids.get(i)));
		}
		TeamNummernVergabe.schreibeNrFormeln(getSheetHelper(), meldelisteSheet, MELDELISTE_NR_SPALTE, nrFormeln);
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

	private void zeigeHinweis(String i18nKey) {
		MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
				.caption(I18n.get("msg.caption.melee.uebernehmen"))
				.message(I18n.get(i18nKey))
				.show();
	}

	private void zeigeHinweisMitParameter(String i18nKey, Object... args) {
		MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
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

	/** Named-Range-Schlüssel des Mêlée-Anmeldung-Sheets. */
	protected abstract String getMeleeMetadatenSchluessel();

	/** Die Ziel-Meldeliste (Quelle des Sheets, in das die Teams geschrieben werden). */
	protected abstract ISheet getMeldeliste();

	/** Erste freie Datenzeile der Meldeliste (0-basiert). */
	protected abstract int naechsteFreieMeldelisteZeile() throws GenerateException;

	/** Aktualisiert die Meldeliste (Nummernvergabe, Formatierung, Druckbereich). */
	protected abstract void meldelisteAktualisieren() throws GenerateException;
}
