/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;

/**
 * Vertrag der Rundenzeitplanung (Turnier-Startzeit, Zeitlimit, Pausen, Durchgang-Aufteilung) für
 * Turniersysteme mit Zeitplan-Unterstützung. Analog zu {@link IFreispielPropertiesSpalte}: reiner
 * Signatur-Vertrag ohne Default-Implementierung, damit sowohl {@code *PropertiesSpalte}-Klassen
 * (Implementierung über {@code readIntProperty}/{@code writeIntProperty}/...) als auch
 * {@code *KonfigurationSheet}-Delegatklassen (Implementierung durch Delegation an ihre
 * {@code propertiesSpalte}) dasselbe Interface erfüllen können. Zentralisiert zumindest die
 * Property-Schlüssel-Konstanten als Single Source of Truth über beide Turniersysteme hinweg.
 */
public interface IZeitplanPropertiesSpalte {

	String KONFIG_PROP_ZEITPLAN_AKTIV = "Zeitplan aktiv";
	String KONFIG_PROP_ZEITPLAN_ANZAHL_BAHNEN = "Zeitplan Anzahl Bahnen";
	String KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN = "Durchgang Zeitlimit (Minuten)";
	String KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN = "Durchgang Pause (Minuten)";
	String KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN = "Runden Pause (Minuten)";
	String KONFIG_PROP_ZEITPLAN_TURNIER_STARTZEIT = "Turnier Startzeit";

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

	/**
	 * Wird von der Durchgang-Aufteilung benötigt (Bahn-Nummerierung nur in den Modi X/N sinnvoll,
	 * siehe {@code IZeitplanSpielrundeSheet.bahnNummerierungProDurchgang}).
	 */
	SpielrundeSpielbahn getSpielrundeSpielbahn();

}
