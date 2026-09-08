/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
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
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Übernimmt die offenen, eingecheckten Mêlée-Anmeldungen als Teams in die Meldeliste.
 * <p>
 * Ablauf:
 * <ol>
 * <li>Mêlée-Anmeldung-Sheet lesen, auf <b>offen</b> (noch nicht übernommen) und <b>eingecheckt</b>
 * filtern – nicht erschienene Spieler bleiben unangetastet stehen.</li>
 * <li>Teams über den {@link MeleeAnmeldungTeamBildner} mischen (Team-Modus aus der
 * Konfiguration, Setzpositionen werden beachtet).</li>
 * <li>Teams blockweise ans Ende der Meldeliste schreiben (Teamname, Spielernamen, Setzposition,
 * Aktiv-Kennzeichen) und die Meldeliste aktualisieren.</li>
 * <li>Die übernommenen Mêlée-Zeilen als „Übernommen" markieren – die Zeilen bleiben erhalten und
 * werden dadurch kein zweites Mal übernommen.</li>
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

		processBoxinfo("processbox.melee.uebernehmen", offeneEingecheckte.size());
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(
				alsMeleeSpieler(offeneEingecheckte), getKonfigurationSheet().getMeleeTeamModus());
		if (teams.isEmpty()) {
			zeigeHinweis("msg.text.melee.keine.anmeldungen");
			return;
		}
		int anzSpielerSpalten = getFormationKonfiguration().getMeldeListeFormation().getAnzSpieler();
		if (teams.stream().anyMatch(team -> team.spieler().size() > anzSpielerSpalten)) {
			zeigeHinweis("msg.text.melee.formation.zu.klein");
			return;
		}

		schreibeTeamsInMeldeliste(teams);
		meldelisteAktualisieren();
		markiereAlsUebernommen(alleZeilen, offeneEingecheckte);
	}

	private static List<MeleeSpieler> alsMeleeSpieler(List<MeleeAnmeldungZeile> zeilen) {
		return zeilen.stream()
				.map(z -> new MeleeSpieler(z.nr(), z.vorname(), z.nachname(), z.setzPosition()))
				.toList();
	}

	/**
	 * Schreibt die gebildeten Teams als zusammenhängenden Block ans Ende der Meldeliste.
	 */
	private void schreibeTeamsInMeldeliste(List<MeleeTeam> teams) throws GenerateException {
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

		Position start = Position.from(MELDELISTE_ERSTE_BLOCK_SPALTE, naechsteFreieMeldelisteZeile());
		XSpreadsheet meldelisteSheet = getMeldeliste().getXSpreadSheet();
		RangeHelper.from(meldelisteSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				data.getRangePosition(start)).setDataInRange(data);
	}

	/**
	 * Markiert die übernommenen Zeilen im Mêlée-Anmeldung-Sheet. Geschrieben wird der komplette
	 * Zeilenbereich von der ersten bis zur letzten übernommenen Zeile am Stück. Dazwischen liegende
	 * Zeilen behalten ihren bisherigen Zustand: bereits früher übernommene bleiben markiert, noch
	 * nicht eingecheckte bleiben leer.
	 */
	private void markiereAlsUebernommen(List<MeleeAnmeldungZeile> alleZeilen,
			List<MeleeAnmeldungZeile> neuUebernommene) throws GenerateException {
		XSpreadsheet meleeSheet = MeleeAnmeldungLeser.findeSheet(getWorkingSpreadsheet(),
				getMeleeMetadatenSchluessel());
		if (meleeSheet == null) {
			return;
		}
		int ersteZeile = neuUebernommene.stream().mapToInt(MeleeAnmeldungZeile::zeile).min().orElseThrow();
		int letzteZeile = neuUebernommene.stream().mapToInt(MeleeAnmeldungZeile::zeile).max().orElseThrow();
		Set<Integer> neuMarkiert = neuUebernommene.stream().map(MeleeAnmeldungZeile::zeile)
				.collect(Collectors.toSet());
		Set<Integer> bereitsMarkiert = alleZeilen.stream().filter(MeleeAnmeldungZeile::uebernommen)
				.map(MeleeAnmeldungZeile::zeile).collect(Collectors.toSet());

		RangeData data = new RangeData();
		for (int zeile = ersteZeile; zeile <= letzteZeile; zeile++) {
			RowData rowData = data.addNewRow();
			if (neuMarkiert.contains(zeile) || bereitsMarkiert.contains(zeile)) {
				rowData.newString(MARKIERUNG);
			} else {
				rowData.newEmpty();
			}
		}
		RangePosition bereich = data.getRangePosition(Position.from(SPALTE_UEBERNOMMEN, ersteZeile));
		RangeHelper.from(meleeSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), bereich)
				.setDataInRange(data);
	}

	private void zeigeHinweis(String i18nKey) {
		MessageBox.from(getxContext(), MessageBoxTypeEnum.INFO_OK)
				.caption(I18n.get("msg.caption.melee.uebernehmen"))
				.message(I18n.get(i18nKey))
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
