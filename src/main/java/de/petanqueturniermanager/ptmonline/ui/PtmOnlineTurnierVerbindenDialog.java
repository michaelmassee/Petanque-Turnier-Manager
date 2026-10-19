/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.OnlineVerbindbarkeit;

/**
 * Auswahldialog "Mit Online-Turnier verbinden": zeigt alle zum aktiven API-Key gehörenden PTM-Online-Turniere in zwei
 * Listen – oben die verbindbaren zur Auswahl, unten die übrigen mit dem Grund, warum sie nicht passen.
 */
final class PtmOnlineTurnierVerbindenDialog extends AbstractUnoDialog {

	private static final Logger logger = LogManager.getLogger(PtmOnlineTurnierVerbindenDialog.class);
	private static final Duration DIALOG_TIMEOUT = Duration.ofMinutes(30);

	private final XWindowPeer parentPeer;
	private final List<OnlineTournamentDto> verbindbare;
	private final List<OnlineVerbindbarkeit.Bewertung> nichtVerbindbare;

	private XControlContainer xcc;
	private XDialog xDialog;
	/** Online-Turnier, mit dem dieses Dokument bereits verbunden ist; {@code null} wenn nicht verbunden. */
	private final String eigeneTurnierId;
	private OnlineTournamentDto ausgewaehlt;

	private PtmOnlineTurnierVerbindenDialog(XComponentContext ctx, XWindowPeer parentPeer,
			List<OnlineVerbindbarkeit.Bewertung> bewertungen, String eigeneTurnierId) {
		super(ctx);
		this.parentPeer = parentPeer;
		this.verbindbare = bewertungen.stream().filter(OnlineVerbindbarkeit.Bewertung::istVerbindbar)
				.map(OnlineVerbindbarkeit.Bewertung::turnier).toList();
		this.nichtVerbindbare = bewertungen.stream().filter(bewertung -> !bewertung.istVerbindbar()).toList();
		this.eigeneTurnierId = eigeneTurnierId;
	}

	/**
	 * @param bewertungen     alle Online-Turniere des API-Keys mit ihrer Verbindbarkeit
	 * @param eigeneTurnierId Online-Turnier, mit dem dieses Dokument bereits verbunden ist, sonst {@code null} –
	 *                        für den Verbindungsstatus je Listeneintrag
	 */
	static Optional<OnlineTournamentDto> zeigen(XComponentContext ctx, XWindowPeer parentPeer,
			List<OnlineVerbindbarkeit.Bewertung> bewertungen, String eigeneTurnierId) throws GenerateException {
		logger.info("PtmOnlineTurnierVerbindenDialog.zeigen(): poste auf Main-Thread (Thread={}, {} Turniere)",
				Thread.currentThread().getName(), bewertungen.size());
		var future = new CompletableFuture<Optional<OnlineTournamentDto>>();
		LoMainThread.post(ctx, () -> {
			logger.info("PtmOnlineTurnierVerbindenDialog: Main-Thread-Callback laeuft (Thread={})",
					Thread.currentThread().getName());
			try {
				var dialog = new PtmOnlineTurnierVerbindenDialog(ctx, parentPeer, bewertungen, eigeneTurnierId);
				dialog.erstelleUndAusfuehren();
				logger.info("PtmOnlineTurnierVerbindenDialog: erstelleUndAusfuehren() zurueck, ausgewaehlt={}",
						dialog.ausgewaehlt != null);
				future.complete(Optional.ofNullable(dialog.ausgewaehlt));
			} catch (Exception e) {
				logger.error("Fehler im PTM-Online-Verbinden-Dialog", e);
				future.completeExceptionally(e);
			}
		});
		try {
			return future.get(DIALOG_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			throw new GenerateException(cause != null ? cause.getMessage() : e.getMessage());
		} catch (TimeoutException e) {
			logger.warn("PTM-Online-Verbinden-Dialog hat nicht rechtzeitig geantwortet", e);
			throw new GenerateException(I18n.get("ptmonline.turnier.verbinden.dialog.fehler.timeout"));
		}
	}

	@Override
	protected String getTitel() {
		return I18n.get("ptmonline.turnier.verbinden.dialog.titel");
	}

	@Override
	protected int getBreite() {
		return 320;
	}

	@Override
	protected int getHoehe() {
		return 236;
	}

	@Override
	protected XWindowPeer holeParentPeer() {
		return parentPeer;
	}

	@Override
	protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
			XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
			XPropertySet dlgProps, XDialog dialog) throws com.sun.star.uno.Exception {
		this.xDialog = dialog;
		this.xcc = Lo.qi(XControlContainer.class, dialog);

		label(xMSF, cont, "lblHinweis",
				verbindbare.isEmpty()
						? I18n.get("ptmonline.turnier.verbinden.dialog.keine.turniere")
						: I18n.get("ptmonline.turnier.verbinden.dialog.hinweis"),
				8, 6, 304, 20);
		label(xMSF, cont, "lblVerbindbar", I18n.get("ptmonline.turnier.verbinden.dialog.liste.verbindbar"),
				8, 26, 304, 10);
		listBox(xMSF, cont, "lstTurniere", verbindbarItems(), false, 8, 37, 304, 74);
		label(xMSF, cont, "lblNichtVerbindbar", I18n.get("ptmonline.turnier.verbinden.dialog.liste.nicht_verbindbar"),
				8, 117, 304, 10);
		listBox(xMSF, cont, "lstNichtVerbindbar", nichtVerbindbarItems(), true, 8, 128, 304, 74);

		button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), 90, 212, 55, 14, (short) PushButtonType.STANDARD_value);
		button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), 155, 212, 75, 14, (short) PushButtonType.CANCEL_value);
		if (verbindbare.isEmpty()) {
			Lo.qi(XPropertySet.class, cont.getByName("btnOk")).setPropertyValue("Enabled", Boolean.FALSE);
		}
		registriereOkButton();
	}

	private void registriereOkButton() {
		var okCtrl = xcc.getControl("btnOk");
		if (okCtrl == null) {
			return;
		}
		var btn = Lo.qi(XButton.class, okCtrl);
		if (btn == null) {
			return;
		}
		btn.addActionListener(new XActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				beimOkGeklickt();
			}

			@Override
			public void disposing(EventObject e) {
				// nichts zu tun
			}
		});
	}

	private void beimOkGeklickt() {
		if (!verbindbare.isEmpty()) {
			int index = listBoxIndex("lstTurniere");
			ausgewaehlt = verbindbare.get(Math.max(0, Math.min(verbindbare.size() - 1, index)));
		}
		xDialog.endExecute();
	}

	private int listBoxIndex(String name) {
		var ctrl = xcc.getControl(name);
		XListBox listBox = ctrl == null ? null : Lo.qi(XListBox.class, ctrl);
		return listBox == null ? 0 : listBox.getSelectedItemPos();
	}

	private String[] verbindbarItems() {
		return verbindbare.stream()
				.map(t -> kopf(t) + "  (" + StringUtils.defaultString(t.status) + ")  —  " + verbindungsStatus(t))
				.toArray(String[]::new);
	}

	private String[] nichtVerbindbarItems() {
		return nichtVerbindbare.stream()
				.map(bewertung -> kopf(bewertung.turnier()) + "  —  " + bewertung.hinderungsgrund().orElse(""))
				.toArray(String[]::new);
	}

	private static String kopf(OnlineTournamentDto turnier) {
		return StringUtils.defaultString(turnier.name) + "  —  " + StringUtils.defaultString(turnier.date);
	}

	/** Online und Dokument sind immer 1:1 verbunden – die Liste zeigt, wer ein Turnier gerade hält. */
	private String verbindungsStatus(OnlineTournamentDto turnier) {
		if (turnier.id != null && turnier.id.equals(eigeneTurnierId)) {
			return I18n.get("ptmonline.turnier.verbinden.dialog.status.dieses_dokument");
		}
		return turnier.documentManaged ? I18n.get("ptmonline.turnier.verbinden.dialog.status.anderes_dokument")
				: I18n.get("ptmonline.turnier.verbinden.dialog.status.nicht_verbunden");
	}

	private static void label(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String text, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", text);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		props.setPropertyValue("MultiLine", Boolean.TRUE);
		cont.insertByName(name, model);
	}

	/**
	 * @param nurAnzeige {@code true}: ohne Auswahl, nur zum Lesen (die nicht verbindbaren Turniere)
	 */
	private static void listBox(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String[] items, boolean nurAnzeige, int x, int y, int w, int h)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		props.setPropertyValue("StringItemList", items);
		props.setPropertyValue("MultiSelection", Boolean.FALSE);
		props.setPropertyValue("Dropdown", Boolean.FALSE);
		props.setPropertyValue("ReadOnly", nurAnzeige);
		if (items.length > 0 && !nurAnzeige) {
			props.setPropertyValue("SelectedItems", new short[] { 0 });
		}
		cont.insertByName(name, model);
	}

	private static void button(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String text, int x, int y, int w, int h, short pushButtonType)
			throws com.sun.star.uno.Exception {
		var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
		var props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", text);
		props.setPropertyValue("PositionX", x);
		props.setPropertyValue("PositionY", y);
		props.setPropertyValue("Width", w);
		props.setPropertyValue("Height", h);
		props.setPropertyValue("PushButtonType", pushButtonType);
		cont.insertByName(name, model);
	}
}
