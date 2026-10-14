/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.kompatibilitaet;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

/**
 * Weist beim Öffnen eines Turnier-Dokuments einmal darauf hin, wenn es nicht zur installierten Plugin-Version passt
 * ({@link DokumentKompatibilitaet}). Das Dokument öffnet normal; was damit geschieht, entscheidet der Anwender.
 */
public final class DokumentKompatibilitaetsHinweis implements IGlobalEventListener {

	private static final Logger logger = LogManager.getLogger(DokumentKompatibilitaetsHinweis.class);

	private final XComponentContext context;

	public DokumentKompatibilitaetsHinweis(XComponentContext context) {
		this.context = context;
	}

	@Override
	public void onLoad(Object source) {
		XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, source);
		if (xDoc == null || new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument() == TurnierSystem.KEIN) {
			return;
		}
		DokumentKompatibilitaet.Ergebnis ergebnis = DokumentKompatibilitaet.pruefe(new WorkingSpreadsheet(context, xDoc));
		hinweisText(ergebnis).ifPresent(text -> {
			logger.warn("Turnier-Dokument nicht kompatibel: {}", ergebnis);
			// Nach dem Lade-Event anzeigen: kein modaler Dialog mitten in der Event-Verteilung.
			LoMainThread.post(context, new Anzeige(context, text)::zeigen);
		});
	}

	static Optional<String> hinweisText(DokumentKompatibilitaet.Ergebnis ergebnis) {
		String dokumentVersion = ergebnis.dokumentVersion().orElse(I18n.get("version.unbekannt"));
		String pluginVersion = ergebnis.pluginVersion().orElse(I18n.get("version.unbekannt"));
		return switch (ergebnis.status()) {
		case ZU_ALT -> Optional.of(I18n.get("dokument.kompatibilitaet.zu_alt", dokumentVersion,
				ergebnis.grenzVersion().orElse("")));
		case NEUER_ALS_PLUGIN -> Optional.of(I18n.get("dokument.kompatibilitaet.neuer_als_plugin", dokumentVersion,
				pluginVersion));
		case KOMPATIBEL, MIGRATION_NOETIG -> Optional.empty();
		};
	}

	private record Anzeige(XComponentContext context, String text) {
		void zeigen() {
			MessageBox.from(context, MessageBoxTypeEnum.WARN_OK)
					.caption(I18n.get("dokument.kompatibilitaet.titel")).message(text).show();
		}
	}
}
