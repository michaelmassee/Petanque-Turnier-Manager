package de.petanqueturniermanager.spielerdb.importer;

/**
 * Nicht-fataler Hinweis aus der Import-Pipeline. Wird gesammelt und am Ende
 * dem Anwender präsentiert (zusätzlich zu den Counts pro Entity).
 */
public record ImportWarnung(String text) { }
