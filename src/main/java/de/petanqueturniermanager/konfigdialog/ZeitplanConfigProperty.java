/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog;

/**
 * Konfig-Property für die Rundenzeitplanung (Turnier-Startzeit, Rundenpause, Durchgang-Aufteilung).
 *
 * <p>Reiner Typ-Marker, damit {@code ZeitplanDialog} diese Properties unabhängig von ihrem
 * konkreten {@link ConfigPropertyType} filtern kann. Bewusst generisch benannt (kein
 * Schweizer-Bezug im Namen), obwohl die Properties aktuell nur in {@code SchweizerPropertiesSpalte}
 * registriert sind (und darüber von Maastrichter geerbt werden) — spätere Übertragung auf weitere
 * Turniersysteme soll ohne Umbenennung möglich sein.
 */
public class ZeitplanConfigProperty<V> extends ConfigProperty<V> {

	protected ZeitplanConfigProperty(ConfigPropertyType type, String key) {
		super(type, key);
	}

	@SuppressWarnings("unchecked")
	public static <V> ZeitplanConfigProperty<V> from(ConfigPropertyType type, String key) {
		return (ZeitplanConfigProperty<V>) new ZeitplanConfigProperty<>(type, key);
	}

}
