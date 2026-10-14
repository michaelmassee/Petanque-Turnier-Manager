/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.kompatibilitaet;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import io.github.g00fy2.versioncompare.Version;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.comp.newrelease.VersionVergleicher;
import de.petanqueturniermanager.exception.AltesDokumentException;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Zentrale Prüfung, ob ein Turnier-Dokument zur installierten Plugin-Version passt, samt Migration älterer
 * Dokumente. Grundlage ist die im Dokument gespeicherte Version
 * ({@link BasePropertiesSpalte#KONFIG_PROP_ERSTELLT_MIT_VERSION}) und die Liste der {@link #SCHRITTE}.
 * <ul>
 * <li>Braucht das Dokument einen nicht migrierbaren Schritt ({@link Status#ZU_ALT}), wird es nicht mehr
 * aktualisiert: Es lässt sich öffnen und ansehen, aber kein Lauf ändert es – auch nicht die gespeicherte
 * Version.</li>
 * <li>Sind alle offenen Schritte migrierbar ({@link Status#MIGRATION_NOETIG}), stellt
 * {@link #stelleAktualisierbarSicher} das Dokument vor dem Lauf um und hebt danach die gespeicherte Version an.</li>
 * <li>Stammt das Dokument von einer neueren Plugin-Version ({@link Status#NEUER_ALS_PLUGIN}), wird die gespeicherte
 * Version nicht heruntergesetzt.</li>
 * </ul>
 */
public final class DokumentKompatibilitaet {

	/**
	 * Alle Migrationsschritte. Ein Dokument durchläuft jeden Schritt, dessen {@link DokumentMigration#abVersion()}
	 * neuer ist als seine Version – mehrere nacheinander, aufsteigend nach Version.
	 */
	static final List<DokumentMigration> SCHRITTE = List.of(new TeamUuidUmstellung());

	/** Ergebnis der Prüfung. */
	public enum Status {
		KOMPATIBEL, MIGRATION_NOETIG, ZU_ALT, NEUER_ALS_PLUGIN
	}

	/**
	 * @param dokumentVersion gespeicherte Version, leer wenn keine Versionsnummer eingetragen ist
	 * @param pluginVersion   installierte Plugin-Version, leer wenn sie nicht ermittelbar ist
	 * @param offeneSchritte  Schritte, die das Dokument braucht, in Reihenfolge
	 */
	public record Ergebnis(Status status, Optional<String> dokumentVersion, Optional<String> pluginVersion,
			List<DokumentMigration> offeneSchritte) {

		public Ergebnis {
			offeneSchritte = List.copyOf(offeneSchritte);
		}

		/** Version, ab der das Dokument nicht mehr aktualisiert wird; nur bei {@link Status#ZU_ALT}. */
		public Optional<String> grenzVersion() {
			return offeneSchritte.stream().filter(schritt -> !schritt.istMigrierbar())
					.map(DokumentMigration::abVersion).reduce((erste, letzte) -> letzte);
		}
	}

	private DokumentKompatibilitaet() {
	}

	/** Prüft das Dokument gegen die installierte Plugin-Version; liest nur. */
	public static Ergebnis pruefe(WorkingSpreadsheet ws) {
		return bewerte(dokumentVersion(ws), pluginVersion(ws), SCHRITTE,
				schritt -> schritt.istNoetigOhneVersion(ws));
	}

	/**
	 * Vor jeder Änderung durch einen Lauf: Ein zu altes Dokument bricht ab, bevor etwas geändert wird; ein
	 * migrierbares wird umgestellt und bekommt danach die aktuelle Plugin-Version.
	 *
	 * @throws AltesDokumentException wenn das Dokument nicht mehr aktualisiert wird
	 */
	public static void stelleAktualisierbarSicher(WorkingSpreadsheet ws) throws GenerateException {
		Ergebnis ergebnis = pruefe(ws);
		switch (ergebnis.status()) {
		case ZU_ALT -> throw new AltesDokumentException(I18n.get("dokument.alt.urspruengliches.plugin",
				ergebnis.grenzVersion().orElse("")));
		case MIGRATION_NOETIG -> {
			migriere(ergebnis.offeneSchritte(), ws);
			DocumentHelper.setDocErstelltMitVersion(ws);
		}
		case KOMPATIBEL, NEUER_ALS_PLUGIN -> {
			// nichts umzustellen
		}
		}
	}

	/** Führt alle Schritte nacheinander aus, aufsteigend nach Version; bricht beim ersten Fehler ab. */
	static void migriere(List<DokumentMigration> schritte, WorkingSpreadsheet ws) throws GenerateException {
		for (DokumentMigration schritt : schritte) {
			schritt.migriere(ws);
		}
	}

	/**
	 * Ob das installierte Plugin seine Version ins Dokument schreiben darf: nicht bei einem zu alten Dokument (es
	 * wird nicht aktualisiert) und nie über eine neuere Version hinweg.
	 */
	public static boolean darfVersionSchreiben(WorkingSpreadsheet ws) {
		Status status = pruefe(ws).status();
		return status != Status.ZU_ALT && status != Status.NEUER_ALS_PLUGIN;
	}

	/** UNO-frei: {@code noetigOhneVersion} wird nur für Dokumente ohne gespeicherte Version gefragt. */
	static Ergebnis bewerte(Optional<String> dokumentVersion, Optional<String> pluginVersion,
			List<DokumentMigration> schritte, Predicate<DokumentMigration> noetigOhneVersion) {
		if (dokumentVersion.isPresent() && pluginVersion.isPresent()
				&& VersionVergleicher.istAelterAls(pluginVersion.get(), dokumentVersion.get())) {
			return new Ergebnis(Status.NEUER_ALS_PLUGIN, dokumentVersion, pluginVersion, List.of());
		}
		List<DokumentMigration> offen = schritte.stream()
				.filter(schritt -> dokumentVersion.map(v -> VersionVergleicher.istAelterAls(v, schritt.abVersion()))
						.orElseGet(() -> noetigOhneVersion.test(schritt)))
				.sorted(Comparator.comparing(schritt -> new Version(schritt.abVersion())))
				.toList();
		Status status = offen.isEmpty() ? Status.KOMPATIBEL
				: offen.stream().allMatch(DokumentMigration::istMigrierbar) ? Status.MIGRATION_NOETIG
						: Status.ZU_ALT;
		return new Ergebnis(status, dokumentVersion, pluginVersion, offen);
	}

	private static Optional<String> dokumentVersion(WorkingSpreadsheet ws) {
		return nurVersionsnummer(Optional.of(new DocumentPropertiesHelper(ws)
				.getStringProperty(BasePropertiesSpalte.KONFIG_PROP_ERSTELLT_MIT_VERSION, "")));
	}

	private static Optional<String> pluginVersion(WorkingSpreadsheet ws) {
		return nurVersionsnummer(ExtensionsHelper.from(ws.getxContext()).versionNummerOptional());
	}

	/** Lässt nur echte Versionsnummern durch, etwa nicht den Eintrag „unbekannt“. */
	private static Optional<String> nurVersionsnummer(Optional<String> angabe) {
		return angabe.map(String::strip).filter(version -> VersionVergleicher.alsVersion(version).isPresent());
	}
}
