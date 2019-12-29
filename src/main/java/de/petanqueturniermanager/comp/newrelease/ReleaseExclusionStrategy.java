/**
 * Erstellung 29.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import org.kohsuke.github.GHRelease;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * @author Michael Massee
 *
 */
public class ReleaseExclusionStrategy implements ExclusionStrategy {

	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		return false;
	}

	@Override
	public boolean shouldSkipClass(Class<?> clazz) {
		Package package1 = clazz.getPackage();
		boolean okayPackage = package1 != null && clazz.getPackage().getName().startsWith("java.lang");
		return !(clazz.isAssignableFrom(GHRelease.class) || okayPackage || clazz.isPrimitive());
	}
}
