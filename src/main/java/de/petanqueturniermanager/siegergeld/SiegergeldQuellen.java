/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import java.util.Optional;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetUpdate;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetUpdate;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;

final class SiegergeldQuellen {

	private SiegergeldQuellen() {
	}

	static Optional<SiegergeldQuelle> fuer(WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
		return switch (turnierSystem) {
		case SUPERMELEE -> Optional.of(new RanglistenSiegergeldQuelle(ws,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE, "A",
				EndranglisteSheet.ERSTE_DATEN_ZEILE, EndranglisteSheet.SPIELER_NR_SPALTE,
				1, EndranglisteSheet.RANGLISTE_SPALTE, SheetNamen.LEGACY_ENDRANGLISTE,
				() -> new MeldeListeSheet_Update(ws).getAktiveMeldungen().size()));
		case LIGA -> Optional.of(new RanglistenSiegergeldQuelle(ws,
				SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE, "A",
				2, 0, 1, LigaRanglisteSheet.RANGLISTE_SPALTE,
				SheetNamen.LEGACY_RANGLISTE,
				() -> new LigaMeldeListeSheetUpdate(ws).getAktiveMeldungen().size()));
		case SCHWEIZER -> Optional.of(new RanglistenSiegergeldQuelle(ws,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE, "A",
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE, SchweizerRanglisteSheet.TEAM_NR_SPALTE,
				SchweizerRanglisteSheet.TEAM_NAME_SPALTE, SchweizerRanglisteSheet.PLATZ_SPALTE,
				SheetNamen.LEGACY_RANGLISTE,
				() -> new SchweizerMeldeListeSheetUpdate(ws).getAktiveMeldungen().size()));
		case FORMULEX -> Optional.of(new RanglistenSiegergeldQuelle(ws,
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, "A",
				FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE, FormuleXRanglisteSheet.TEAM_NR_SPALTE,
				FormuleXRanglisteSheet.TEAM_NAME_SPALTE, FormuleXRanglisteSheet.PLATZ_SPALTE,
				SheetNamen.LEGACY_FORMULEX_RANGLISTE,
				() -> new FormuleXMeldeListeSheetUpdate(ws).getAktiveMeldungen().size()));
		case TRIPTETE -> Optional.of(new RanglistenSiegergeldQuelle(ws,
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE, "A",
				TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE, TripTeteRanglisteSheet.TEAM_NR_SPALTE,
				TripTeteRanglisteSheet.NAME_SPALTE, TripTeteRanglisteSheet.RANG_SPALTE,
				SheetNamen.LEGACY_RANGLISTE,
				() -> new TripTeteMeldeListeSheetUpdate(ws).getAktiveMeldungen().size()));
		case JGJ -> Optional.of(new JgjSiegergeldQuelle(ws));
		case MAASTRICHTER -> Optional.of(new MaastrichterSiegergeldQuelle(ws));
		default -> Optional.empty();
		};
	}
}
