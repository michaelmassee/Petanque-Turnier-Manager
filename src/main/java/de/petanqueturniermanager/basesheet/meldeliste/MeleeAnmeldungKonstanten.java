/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

/**
 * Gemeinsame Konstanten des Mêlée-Anmeldung-Sheets: einer schlichten Vorstufe zur Meldeliste, in
 * der lose Einzelspieler erfasst werden, bevor sie per Menü-Kommando zu Teams gemischt und in die
 * eigentliche Meldeliste übernommen werden.
 * <p>
 * Spaltenlayout (fix, unabhängig vom Turniersystem):
 * <pre>
 * Nr | Vorname | Nachname | SP | Eingecheckt | Übernommen
 * </pre>
 */
public interface MeleeAnmeldungKonstanten {

	/** Kopfzeile mit den Spaltenüberschriften. */
	int KOPF_ZEILE = 0;
	/** Erste Zeile mit Anmeldungsdaten. */
	int ERSTE_DATEN_ZEILE = 1;

	int SPALTE_NR = 0;
	int SPALTE_VORNAME = 1;
	int SPALTE_NACHNAME = 2;
	/** Setzposition (SP) – Ganzzahl ≥ 1, leer/0 = kein Setzstatus. */
	int SPALTE_SETZPOSITION = 3;
	int SPALTE_EINGECHECKT = 4;
	int SPALTE_UEBERNOMMEN = 5;
	/** Letzte Spalte des Datenbereichs. */
	int LETZTE_SPALTE = SPALTE_UEBERNOMMEN;

	/**
	 * Sprachneutrale Markierung für die Ja/Nein-Spalten „Eingecheckt" und „Übernommen".
	 * <p>
	 * Bewusst <b>kein</b> übersetzter Text: die Werte werden maschinell zurückgelesen und müssen
	 * nach einem Sprachwechsel des Dokuments weiterhin erkannt werden. Angezeigt wird die
	 * Bedeutung über die (lokalisierten) Spaltenüberschriften.
	 */
	String MARKIERUNG = "X";

	/** Obergrenze für das Einlesen des Datenbereichs (Schutz gegen Endlosschleifen). */
	int MAX_ANZ_ANMELDUNGEN = 999;

	/** Mindestanzahl vorformatierter (leerer) Eingabezeilen. */
	int MIN_ANZ_ZEILEN = 60;

	int NR_SPALTE_WIDTH = 800;
	int NAME_SPALTE_WIDTH = 3500;
	int MARKIERUNG_SPALTE_WIDTH = 1600;
	int SETZPOSITION_SPALTE_WIDTH = 800;
}
