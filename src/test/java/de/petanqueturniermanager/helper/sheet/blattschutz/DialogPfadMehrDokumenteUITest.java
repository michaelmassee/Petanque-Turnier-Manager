/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.blattschutz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XTopWindow;
import com.sun.star.container.XIndexAccess;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.DokumentKontext;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Regression: Dialog-Befehle (z.B. Spieler-DB „Spieler in Meldeliste übernehmen") laufen ohne
 * {@code SheetRunner}. Ohne Dokumentkontext galt der Turniermodus als aktiv, sobald irgendein Dokument in ihm
 * war – das Schreiben in ein zweites Dokument ohne Turniermodus scheiterte mit „Style/CF-Operation außerhalb
 * eines aktiven BlattschutzScopes". Der {@code ProtocolHandler} setzt für Dialog-Befehle daher den
 * Dokumentkontext des Frames. Zwei Dokumente offen, Fokus auf dem Dokument im Turniermodus.
 */
class DialogPfadMehrDokumenteUITest extends BaseCalcUITest {

	private XSpreadsheetDocument zweitesDokument;
	private WorkingSpreadsheet zweitesWs;

	@AfterEach
	void raeumeAuf() {
		TurnierModus.get().wiederherstellenAlleElemente(wkingSpreadsheet);
		warteAufLoLeerlauf();
		if (zweitesDokument != null) {
			OfficeDocumentHelper.closeDoc(zweitesDokument);
			zweitesDokument = null;
		}
	}

	@Test
	void schreibenInDokumentOhneTurniermodusGelingtMitDessenKontext() throws Exception {
		zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
		assertThat(zweitesDokument).as("zweites Dokument konnte nicht erstellt werden").isNotNull();
		zweitesWs = new WorkingSpreadsheet(starter.getxComponentContext(), zweitesDokument);
		TurnierModus.get().aktivieren(wkingSpreadsheet);
		warteAufLoLeerlauf();
		fokussiere(doc);
		warteAufLoLeerlauf();
		XSpreadsheet blattB = Lo.qi(XSpreadsheet.class,
				Lo.qi(XIndexAccess.class, zweitesDokument.getSheets()).getByIndex(0));

		assertThatThrownBy(() -> schreibeImDialogPfad(blattB))
				.as("ohne Kontext entscheidet der Turniermodus von Dokument A").isInstanceOf(IllegalStateException.class);

		DokumentKontext.mitKontextAufruf(zweitesDokument, () -> {
			schreibeImDialogPfad(blattB);
			return null;
		});
		assertThat(blattB.getCellByPosition(0, 0).getFormula()).isEqualTo("Anna");
	}

	/** Wie der Spieler-DB-Dispatcher: Blattschutz-Scope für das Zieldokument, dann blockweise schreiben. */
	private void schreibeImDialogPfad(XSpreadsheet blatt) throws Exception {
		RangeData data = new RangeData();
		data.addNewRow().newString("Anna");
		try (var _ = BlattschutzManager.get().scopeFuer(TurnierSystem.SCHWEIZER, zweitesWs)) {
			RangeHelper.from(blatt, zweitesDokument, data.getRangePosition(Position.from(0, 0))).setDataInRange(data);
		}
	}

	private static void fokussiere(XSpreadsheetDocument dokument) {
		XFrame frame = Lo.qi(XModel.class, dokument).getCurrentController().getFrame();
		frame.activate();
		XTopWindow topWindow = Lo.qi(XTopWindow.class, frame.getContainerWindow());
		if (topWindow != null) {
			topWindow.toFront();
		}
	}
}
