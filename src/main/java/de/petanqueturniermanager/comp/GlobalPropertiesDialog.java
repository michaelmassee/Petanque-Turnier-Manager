package de.petanqueturniermanager.comp;

import java.awt.*;

import javax.swing.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;

/**
 * Modaler Swing-Dialog zur Bearbeitung der Plugin-Konfiguration (GlobalProperties).
 */
public class GlobalPropertiesDialog {

	private static final Logger logger = LogManager.getLogger(GlobalPropertiesDialog.class);

	private final XComponentContext xContext;

	public GlobalPropertiesDialog(XComponentContext xContext) {
		this.xContext = xContext;
	}

	public void zeigen() {
		SwingUtilities.invokeLater(this::oeffnen);
	}

	private void positionierenImLibreOfficeFenster(JDialog dialog) {
		try {
			XFrame frame = DocumentHelper.getCurrentFrame(xContext);
			if (frame != null) {
				XWindow containerWindow = frame.getContainerWindow();
				Rectangle pos = containerWindow.getPosSize();
				dialog.setLocation(pos.X + 50, pos.Y + 50);
				return;
			}
		} catch (Exception e) {
			logger.warn("Fensterpositionierung fehlgeschlagen: {}", e.getMessage());
		}
		dialog.setLocationByPlatform(true);
	}

	private void oeffnen() {
		GlobalProperties gp = GlobalProperties.get();

		JDialog dialog = new JDialog((Frame) null, "Plugin Konfiguration", true);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setLayout(new BorderLayout(8, 8));
		dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// ---- Felder ----
		JPanel felderPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		int zeile = 0;

		// Autosave
		JCheckBox autosaveBox = new JCheckBox("Autosave nach jeder Aktion", gp.isAutoSave());
		gbc.gridx = 0; gbc.gridy = zeile++; gbc.gridwidth = 2;
		felderPanel.add(autosaveBox, gbc);

		// Backup
		JCheckBox backupBox = new JCheckBox("Backup vor wichtigen Generierungen", gp.isCreateBackup());
		gbc.gridy = zeile++;
		felderPanel.add(backupBox, gbc);

		// New Version Check immer true
		JCheckBox newVersionBox = new JCheckBox("Neue-Version-Prüfung immer aktiv (Entwicklungsmodus)", gp.isNewVersionCheckImmerTrue());
		gbc.gridy = zeile++;
		felderPanel.add(newVersionBox, gbc);

		// Log-Level
		gbc.gridwidth = 1;
		gbc.gridy = zeile;
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.NONE;
		felderPanel.add(new JLabel("Log-Level:"), gbc);

		String[] logLevels = { "", "info", "debug" };
		JComboBox<String> logLevelBox = new JComboBox<>(logLevels);
		String aktuellerLevel = gp.getLogLevel().toLowerCase();
		logLevelBox.setSelectedItem(aktuellerLevel.isBlank() ? "" : aktuellerLevel);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		felderPanel.add(logLevelBox, gbc);

		dialog.add(felderPanel, BorderLayout.CENTER);

		// ---- Buttons ----
		JButton okBtn = new JButton("OK");
		JButton abbrechenBtn = new JButton("Abbrechen");

		okBtn.addActionListener(e -> {
			try {
				String gewaehlterLevel = (String) logLevelBox.getSelectedItem();
				gp.speichern(
						autosaveBox.isSelected(),
						backupBox.isSelected(),
						newVersionBox.isSelected(),
						gewaehlterLevel
				);
				// UI-Komponenten sofort aktualisieren (z.B. ProcessBox-Versionslabel)
				NewReleaseChecker.callbacksAusloesen();
				logger.info("Plugin-Konfiguration gespeichert");
			} catch (Exception ex) {
				logger.error("Fehler beim Speichern der Plugin-Konfiguration: {}", ex.getMessage(), ex);
			}
			dialog.dispose();
		});

		abbrechenBtn.addActionListener(e -> dialog.dispose());

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btnPanel.add(abbrechenBtn);
		btnPanel.add(okBtn);
		dialog.add(btnPanel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
		positionierenImLibreOfficeFenster(dialog);
		dialog.setVisible(true);
	}
}
