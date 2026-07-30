/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.WhatsAppChatEintrag;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;

public class WhatsAppPostRunner extends SheetRunner {

	private static final Logger logger = LogManager.getLogger(WhatsAppPostRunner.class);
	private static final String PROP_LETZTER_WHATSAPP_CHAT = "WhatsApp Letzter Chat";

	private final WhatsAppAktion aktion;

	public WhatsAppPostRunner(WorkingSpreadsheet ws, TurnierSystem ts, WhatsAppAktion aktion) {
		super(ws, ts, "WhatsApp " + aktion.name());
		this.aktion = aktion;
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	@Override
	protected void doRun() throws GenerateException {
		List<WhatsAppChatEintrag> chats = GlobalProperties.get().getWhatsAppChatEintraege();
		if (chats.isEmpty()) {
			throw new GenerateException(I18n.get("whatsapp.post.fehler.keine.chats"));
		}

		var docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());
		String letzterChatId = docPropHelper.getStringProperty(PROP_LETZTER_WHATSAPP_CHAT, "");
		WhatsAppChatEintrag chat = WhatsAppChatAuswahlDialog
				.zeigen(getWorkingSpreadsheet(), chats, letzterChatId)
				.orElse(null);
		if (chat == null) {
			throw new GenerateException(I18n.get("whatsapp.post.abgebrochen"));
		}
		docPropHelper.setStringProperty(PROP_LETZTER_WHATSAPP_CHAT, chat.id());

		Path tempVerzeichnis = tempVerzeichnis();
		try {
			var bild = new WhatsAppBildExportService(getWorkingSpreadsheet())
					.exportiere(aktion, getTurnierSystem(), tempVerzeichnis);
			String caption = I18n.get("whatsapp.post.caption", aktion.titel(getTurnierSystem()), bild.sheetName());
			WhatsAppBridgeManager.starteOderVerbinde().sendeBild(chat.chatId(), caption, bild.bytes());
			processBox().info(I18n.get("whatsapp.post.erfolg", chat.anzeigeName()));
		} catch (WhatsAppBridgeException e) {
			logger.error("WhatsApp-Post fehlgeschlagen", e);
			throw new GenerateException(e.getMessage());
		} finally {
			try {
				FileUtils.deleteDirectory(tempVerzeichnis.toFile());
			} catch (IOException e) {
				logger.warn("Temporäres WhatsApp-Exportverzeichnis konnte nicht gelöscht werden: {}", tempVerzeichnis, e);
			}
		}
	}

	private static Path tempVerzeichnis() throws GenerateException {
		try {
			return Files.createTempDirectory("ptm-whatsapp-export-");
		} catch (IOException e) {
			throw new GenerateException(e.getMessage());
		}
	}
}
