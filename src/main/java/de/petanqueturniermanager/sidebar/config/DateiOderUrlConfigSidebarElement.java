/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XControl;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.fields.BaseField;

/**
 * Sidebar-Element für die Logo-URL Konfiguration.
 * <p>
 * Erweitert {@link StringConfigSidebarElement} um einen zweiten Button „Datei…",
 * der einen System-Dateiauswahldialog öffnet und den gewählten Dateipfad
 * als {@code file://…}-URL in das Textfeld schreibt.
 * <p>
 * Neben dem bestehenden Bearbeiten-Button des Elternelements wird ein weiterer
 * Button direkt in das horizontale Layout eingefügt.
 */
public class DateiOderUrlConfigSidebarElement extends StringConfigSidebarElement {

    private static final Logger logger = LogManager.getLogger(DateiOderUrlConfigSidebarElement.class);

    public DateiOderUrlConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam,
            ConfigProperty<String> configProperty,
            WorkingSpreadsheet workingSpreadsheet) {
        super(guiFactoryCreateParam, configProperty, workingSpreadsheet);
        fuegeZweitenButtonHinzu(guiFactoryCreateParam);
    }

    // ── Zweiten Button zum Layout hinzufügen ───────────────────────────────────

    private void fuegeZweitenButtonHinzu(GuiFactoryCreateParam guiFactoryCreateParam) {
        if (labelPlusTextPlusTextareaBox == null) {
            return;
        }
        Map<String, Object> props = new HashMap<>();
        props.put(GuiFactory.HELP_TEXT, I18n.get("konfig.logo.datei.tooltip"));
        Rectangle btnRect = new Rectangle(BaseField.BASE_RECTANGLE.X, BaseField.BASE_RECTANGLE.Y,
                BaseField.BASE_RECTANGLE.Width, 29);
        XControl btnControl = GuiFactory.createButton(
                guiFactoryCreateParam,
                I18n.get("konfig.logo.datei.auswaehlen.kurz"),
                dateiAuswaehlenListener,
                btnRect,
                props);
        labelPlusTextPlusTextareaBox.getLayout().addFixedWidthControl(btnControl, 29);
    }

    // ── Dateiauswahl-Listener ──────────────────────────────────────────────────

    private final XActionListener dateiAuswaehlenListener = new XActionListener() {

        @Override
        public void disposing(EventObject arg0) {
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!ObjectUtils.allNotNull(labelPlusTextPlusTextareaBox)) {
                return;
            }
            try {
                oeffneDateiAuswahl();
            } catch (Exception e) {
                logger.error("Fehler bei Dateiauswahl", e);
            }
        }
    };

    private void oeffneDateiAuswahl() {
        var chooser = new JFileChooser();
        chooser.setDialogTitle(I18n.get("konfig.logo.datei.auswaehlen"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                I18n.get("konfig.logo.datei.filter.bilder"), "png", "jpg", "jpeg", "gif", "svg", "webp"));

        // Aktuellen Wert als Startverzeichnis setzen, wenn es eine file://-URL ist
        var aktuellerWert = getPropertyValue();
        if (aktuellerWert.startsWith("file://")) {
            try {
                var aktuelleFile = new java.io.File(new java.net.URI(aktuellerWert));
                chooser.setCurrentDirectory(aktuelleFile.getParentFile());
            } catch (java.net.URISyntaxException ignored) {
                // Ignorieren – Standardverzeichnis wird verwendet
            }
        }

        int ergebnis = chooser.showOpenDialog(null);
        if (ergebnis == JFileChooser.APPROVE_OPTION) {
            var datei = chooser.getSelectedFile();
            if (datei != null && datei.exists()) {
                String fileUrl = datei.toURI().toString(); // z.B. file:///home/user/logo.png
                labelPlusTextPlusTextareaBox.fieldText(fileUrl);
                setPropertyValue(fileUrl);
            }
        }
    }
}
