/**
 * Erstellung 27.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.forme.korunde;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamRangliste;

/**
 * Hilfsklasse zum Einlesen der Vorrunden-Paarungen für die KO-Runden-Berechnung.
 *
 * <p>Diese Klasse verwaltet ein <em>temporäres Rechenhilfsblatt</em> ("VorRunden") in LibreOffice
 * Calc. Das Sheet dient als Zwischenpuffer zwischen zwei Berechnungsschritten: Die vom User
 * eingetragenen Paarungen aus dem permanenten {@link VorrundenSheet} werden gelesen und daraus
 * Java-Objektgraphen ({@link de.petanqueturniermanager.model.TeamRangliste} mit Gegner-Listen)
 * aufgebaut. So kann {@link KoGruppeABSheet} beim Generieren der KO-Paarungen
 * Wiederholungspaarungen vermeiden.
 *
 * <p><b>Warum ein echtes Calc-Sheet als Puffer?</b><br>
 * Die LibreOffice-UNO-API bietet keine direkte Möglichkeit, strukturierte Zwischenergebnisse
 * im Java-Heap zu halten und gleichzeitig mit anderen Sheet-Operationen zu koordinieren.
 * Das temporäre Sheet übernimmt daher die Rolle des Arbeitsspeichers zwischen den
 * Berechnungsschritten (Cadrage → Rangliste → KO-Paarungen).
 *
 * <p><b>Temporär bedeutet:</b> Das Sheet besitzt keinen Metadata-Schlüssel und ist kein
 * offizielles Turnier-Sheet. Es wird bei jedem Lauf via {@code forceCreate()} überschrieben
 * und hat keinen dauerhaften Wert für den User.
 *
 * @author Michael Massee
 * @see VorrundenSheet
 * @see CadrageSheet
 * @see KoGruppeABSheet
 */
public class Vorrunden {

	private static final String VORRUNDEN_SHEET = "VorRunden";
	private static final String RNDHEADER = "Rnd";

	private final WeakRefHelper<ISheet> parentSheet;

	public Vorrunden(ISheet parentSheet) {
		this.parentSheet = new WeakRefHelper<>(parentSheet);
	}

	public XSpreadsheet getSheet() throws GenerateException {
		XSpreadsheet vorRunden = getSheetHelper().findByName(VORRUNDEN_SHEET);

		if (null != vorRunden) {
			getSheetHelper().setActiveSheet(vorRunden);
		} else {
			vorRunden = NewSheet.temporary(parentSheet.get(), VORRUNDEN_SHEET).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create().getSheet();
		}

		return vorRunden;
	}

	public void vorRundenEinlesen(TeamRangliste rangliste) throws GenerateException {

		int headerZeile = 0;
		int ersteTeamNrZeile = 1;

		XSpreadsheet vorRunden = getSheet();

		processBoxinfo("processbox.vorrunden.einlesen");

		ImmutableList<Team> teamList = rangliste.getTeamListe();

		Position headerPos = Position.from(0, headerZeile);
		for (int spalteCnt = 0; spalteCnt < 20; spalteCnt += 2) {
			SheetRunner.testDoCancelTask();
			headerPos.spalte(spalteCnt);
			String header = getSheetHelper().getTextFromCell(vorRunden, headerPos);
			if (StringUtils.isNotEmpty(header) && header.regionMatches(true, 0, RNDHEADER, 0, RNDHEADER.length())) {
				for (int zeileCntr = ersteTeamNrZeile; zeileCntr < 999; zeileCntr++) {
					Integer cellNumTeamA = getSheetHelper().getIntFromCell(vorRunden, Position.from(spalteCnt, zeileCntr));
					if (cellNumTeamA > 0) {
						var teamA = Team.from(cellNumTeamA);
						var teamAInListe = teamList.stream().filter(teamA::equals).findFirst().orElse(null);
						if (teamAInListe != null) {
							Integer cellNumTeamB = getSheetHelper().getIntFromCell(vorRunden, Position.from(spalteCnt + 1, zeileCntr));
							if (cellNumTeamB > 0) {
								var teamB = Team.from(cellNumTeamB);
								teamList.stream().filter(teamB::equals).findFirst()
										.ifPresent(teamBInListe -> teamBInListe.addGegner(teamAInListe));
							}
						}
					} else {
						break;
					}
				}
			} else {
				break;
			}
		}
	}

	/** Gibt eine Prozessbox-Meldung über das übergeordnete Sheet aus. */
	private void processBoxinfo(String i18nKey, Object... args) {
		parentSheet.get().processBoxinfo(i18nKey, args);
	}

	/** Gibt den {@link SheetHelper} des übergeordneten Sheets zurück. */
	private SheetHelper getSheetHelper() throws GenerateException {
		return parentSheet.get().getSheetHelper();
	}

}
