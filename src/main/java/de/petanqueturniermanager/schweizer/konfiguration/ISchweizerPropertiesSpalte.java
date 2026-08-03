/*
 * Erstellung 12.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * @author Michael Massee
 *
 */
public interface ISchweizerPropertiesSpalte extends de.petanqueturniermanager.basesheet.konfiguration.IFreispielPropertiesSpalte {

	String getKopfZeileLinks();

	String getKopfZeileMitte();

	String getKopfZeileRechts();

	void setAktiveSpielRunde(SpielRundeNr neueSpielrunde);

	SpielRundeNr getAktiveSpielRunde();

	Integer getSpielRundeHintergrundFarbeGerade();

	SpielrundeHintergrundFarbeGeradeStyle getSpielRundeHintergrundFarbeGeradeStyle();

	Integer getSpielRundeHintergrundFarbeUnGerade();

	SpielrundeHintergrundFarbeUnGeradeStyle getSpielRundeHintergrundFarbeUnGeradeStyle();

	Integer getSpielRundeHeaderFarbe();

	SpielrundeSpielbahn getSpielrundeSpielbahn();

	void setSpielrundeSpielbahn(SpielrundeSpielbahn option);

	Formation getMeldeListeFormation();

	boolean isMeldeListeTeamnameAnzeigen();

	boolean isMeldeListeVereinsnameAnzeigen();

	void setMeldeListeFormation(Formation formation);

	void setMeldeListeTeamnameAnzeigen(boolean anzeigen);

	void setMeldeListeVereinsnameAnzeigen(boolean anzeigen);

	SpielplanTeamAnzeige getSpielplanTeamAnzeige();

	void setSpielplanTeamAnzeige(SpielplanTeamAnzeige anzeige);

	SchweizerRankingModus getRankingModus();

	void setRankingModus(SchweizerRankingModus modus);

	boolean isZeitplanAktiv();

	void setZeitplanAktiv(boolean aktiv);

	int getZeitplanAnzahlBahnen();

	void setZeitplanAnzahlBahnen(int bahnen);

	/** {@code isZeitplanAktiv() && getZeitplanAnzahlBahnen() > 0} — steuert die Durchgang-Aufteilung innerhalb einer Runde. */
	boolean isDurchgangAufteilungWirksam();

	int getZeitplanZeitlimitMinuten();

	void setZeitplanZeitlimitMinuten(int minuten);

	int getZeitplanDurchgangPauseMinuten();

	void setZeitplanDurchgangPauseMinuten(int minuten);

	int getZeitplanRundenPauseMinuten();

	void setZeitplanRundenPauseMinuten(int minuten);

	String getZeitplanTurnierStartzeit();

	void setZeitplanTurnierStartzeit(String hhMm);

}
