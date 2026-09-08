/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IFormationKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungLeser;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungZeile;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;

/**
 * Zentrale Auskunft darüber, ob die Mêlée-Anmeldung (Vorab-Liste loser Einzelspieler, die per
 * Menü-Kommando zu Teams gemischt und in die Meldeliste übernommen werden) im aktuellen Dokument
 * <b>möglich</b> und <b>aktiviert</b> ist.
 * <p>
 * Möglich ist sie nur bei Turniersystemen mit konfigurierbarer Meldeliste-Formation
 * ({@link IFormationKonfiguration}) und dort nur bei den Formationen
 * {@link Formation#DOUBLETTE} und {@link Formation#TRIPLETTE}: bei
 * {@link Formation#TETE} ist jeder Einzelspieler bereits ein eigenständiges Team, bei
 * {@link Formation#NUR_TEAMNAME} werden ganze Teams am Stück gemeldet – eine Vorstufe aus losen
 * Einzelspielern ergibt in beiden Fällen fachlich keinen Sinn.
 * <p>
 * Die Auflösung erfolgt bewusst über das im Dokument hinterlegte Turniersystem (nicht fokus-basiert),
 * damit bei mehreren offenen Turnier-Dokumenten immer die Konfiguration des <b>gemeinten</b>
 * Dokuments gelesen wird.
 */
public final class MeleeAnmeldungKonfiguration {

	private static final Logger logger = LogManager.getLogger(MeleeAnmeldungKonfiguration.class);

	/**
	 * Fabriken je Turniersystem mit konfigurierbarer Meldeliste-Formation. Systeme ohne
	 * konfigurierbare Formation (Liga, Supermelee, Trip-Tête) haben bewusst keinen Eintrag – für sie
	 * gibt es keine Mêlée-Anmeldung. Maastrichter erweitert das Schweizer Konfigurations-Sheet
	 * (identische Formation-Optionen) und ist deshalb hier gleichwertig gelistet.
	 */
	private static final Map<TurnierSystem, Function<WorkingSpreadsheet, BaseKonfigurationSheet>> FABRIKEN =
			Map.of(
					TurnierSystem.SCHWEIZER, SchweizerKonfigurationSheet::new,
					TurnierSystem.JGJ, JGJKonfigurationSheet::new,
					TurnierSystem.KO, KoKonfigurationSheet::new,
					TurnierSystem.KASKADE, KaskadeKonfigurationSheet::new,
					TurnierSystem.POULE, PouleKonfigurationSheet::new,
					TurnierSystem.FORMULEX, FormuleXKonfigurationSheet::new,
					TurnierSystem.MAASTRICHTER, MaastrichterKonfigurationSheet::new);

	/** Named-Range-Schlüssel des Mêlée-Anmeldung-Sheets je Turniersystem. */
	private static final Map<TurnierSystem, String> METADATEN_SCHLUESSEL = Map.of(
			TurnierSystem.SCHWEIZER, SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELEE_ANMELDUNG,
			TurnierSystem.JGJ, SheetMetadataHelper.SCHLUESSEL_JGJ_MELEE_ANMELDUNG,
			TurnierSystem.KO, SheetMetadataHelper.SCHLUESSEL_KO_MELEE_ANMELDUNG,
			TurnierSystem.KASKADE, SheetMetadataHelper.SCHLUESSEL_KASKADE_MELEE_ANMELDUNG,
			TurnierSystem.POULE, SheetMetadataHelper.SCHLUESSEL_POULE_MELEE_ANMELDUNG,
			TurnierSystem.FORMULEX, SheetMetadataHelper.SCHLUESSEL_FORMULEX_MELEE_ANMELDUNG,
			TurnierSystem.MAASTRICHTER, SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_MELEE_ANMELDUNG);

	private MeleeAnmeldungKonfiguration() {
	}

	/**
	 * Liefert die noch nicht übernommenen Mêlée-Anmeldungen des Dokuments.
	 *
	 * @param ws aktuelles Dokument
	 * @return offene Anmeldungen, oder eine leere Liste wenn die Mêlée-Anmeldung nicht aktiv ist
	 */
	public static List<MeleeAnmeldungZeile> offeneAnmeldungen(WorkingSpreadsheet ws) {
		if (!istAktiv(ws)) {
			return List.of();
		}
		TurnierSystem turnierSystem = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
		String schluessel = METADATEN_SCHLUESSEL.get(turnierSystem);
		if (schluessel == null) {
			return List.of();
		}
		return MeleeAnmeldungLeser.lesen(ws, schluessel).stream()
				.filter(MeleeAnmeldungZeile::istOffen)
				.toList();
	}

	/**
	 * @param ws aktuelles Dokument
	 * @return das Konfigurations-Sheet des Turniersystems, oder {@code null} wenn das System keine
	 *         konfigurierbare Meldeliste-Formation kennt bzw. kein Turnier vorhanden ist
	 */
	public static BaseKonfigurationSheet konfiguration(WorkingSpreadsheet ws) {
		if (ws == null || ws.getWorkingSpreadsheetDocument() == null) {
			return null;
		}
		try {
			TurnierSystem turnierSystem = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
			Function<WorkingSpreadsheet, BaseKonfigurationSheet> fabrik = FABRIKEN.get(turnierSystem);
			return fabrik == null ? null : fabrik.apply(ws);
		} catch (RuntimeException e) {
			logger.warn("Konfiguration für die Mêlée-Anmeldung konnte nicht ermittelt werden", e);
			return null;
		}
	}

	/**
	 * @param ws aktuelles Dokument
	 * @return {@code true} wenn die eingestellte Formation eine Mêlée-Anmeldung fachlich zulässt
	 */
	public static boolean istMoeglich(WorkingSpreadsheet ws) {
		return istMoeglich(konfiguration(ws));
	}

	/**
	 * @param konfigurationSheet Konfigurations-Sheet, darf {@code null} sein
	 * @return {@code true} wenn die eingestellte Formation eine Mêlée-Anmeldung fachlich zulässt
	 */
	public static boolean istMoeglich(BaseKonfigurationSheet konfigurationSheet) {
		if (!(konfigurationSheet instanceof IFormationKonfiguration formationKonfiguration)) {
			return false;
		}
		Formation formation = formationKonfiguration.getMeldeListeFormation();
		return formation == Formation.DOUBLETTE || formation == Formation.TRIPLETTE;
	}

	/**
	 * @param ws aktuelles Dokument
	 * @return {@code true} wenn die Mêlée-Anmeldung möglich <b>und</b> in der Turnier-Konfiguration
	 *         eingeschaltet ist
	 */
	public static boolean istAktiv(WorkingSpreadsheet ws) {
		return istAktiv(konfiguration(ws));
	}

	/**
	 * @param konfigurationSheet Konfigurations-Sheet, darf {@code null} sein
	 * @return {@code true} wenn die Mêlée-Anmeldung möglich <b>und</b> eingeschaltet ist
	 */
	public static boolean istAktiv(BaseKonfigurationSheet konfigurationSheet) {
		return istMoeglich(konfigurationSheet) && konfigurationSheet.isMeleeAnmeldungAktiv();
	}
}
