/**
 * Erstellung 10.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

/**
 * @author Michael Massee
 *
 */
public interface IMeldung<T> {

	int getNr();

	int getSetzPos();

	T setSetzPos(int setzPos);

	boolean isHatteFreilos();

	T setHatteFreilos(boolean hatteFreilos);

}
