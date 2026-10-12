/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.petanqueturniermanager.addins.GlobalImpl;

/**
 * Inhalt einer Nr-Zelle: lokale UUID und – falls die Meldung mit PTM-Online verbunden ist – die Online-ID der
 * Anmeldung. Damit stehen alle Identitäten einer Meldung an einer Stelle und wandern beim Sortieren mit.
 * <ul>
 * <li>Meldeliste (Team): {@code PTM.ALG.TNRUUID(nr;"uuid";"online-id")}</li>
 * <li>Mêlée-Anmeldung (Spieler): {@code PTM.ALG.SPLRUUID(nr;"uuid";"online-id")}</li>
 * </ul>
 * Die Online-ID ist optional. Die angezeigte Nummer gehört nicht zum Wert: Sie ändert sich, die Identitäten
 * nicht. UNO-frei.
 */
public record NrZellFormel(Art art, String uuid, Optional<String> onlineId) {

	/** Welche Liste die Zelle trägt. */
	public enum Art {
		TEAM(GlobalImpl.PTM_TNR_UUID, "ptmtnruuid"), SPIELER(GlobalImpl.PTM_SPLR_UUID, "ptmsplruuid");

		private final String funktion;
		private final String programmatischerName;

		Art(String funktion, String programmatischerName) {
			this.funktion = funktion;
			this.programmatischerName = programmatischerName;
		}

		public String funktion() {
			return funktion;
		}

		/** {@code getFormula} liefert Add-in-Funktionen je nach Kontext unter dem programmatischen Namen. */
		boolean erkennt(String formel) {
			String klein = formel.toLowerCase(Locale.ROOT);
			return klein.contains(funktion.toLowerCase(Locale.ROOT)) || klein.contains(programmatischerName);
		}
	}

	private static final Pattern TEXT_LITERAL = Pattern.compile("\"([^\"]*)\"");

	public NrZellFormel {
		onlineId = onlineId.filter(id -> !id.isBlank());
	}

	public static NrZellFormel team(String uuid) {
		return new NrZellFormel(Art.TEAM, uuid, Optional.empty());
	}

	public static NrZellFormel spieler(String uuid) {
		return new NrZellFormel(Art.SPIELER, uuid, Optional.empty());
	}

	/**
	 * Liest eine Nr-Zelle. Die Art ergibt sich aus dem Funktionsnamen; fehlt er (z.&nbsp;B. weil der Text verändert
	 * wurde), gilt {@code standardArt}. Erstes Text-Literal ist die lokale UUID, das zweite die Online-ID.
	 *
	 * @return leer, wenn die Zelle keine lokale UUID trägt (reine Zahl, leer, Text)
	 */
	public static Optional<NrZellFormel> lese(String formel, Art standardArt) {
		Optional<String> uuid = TeamUuidFormel.findeUuid(formel);
		if (uuid.isEmpty()) {
			return Optional.empty();
		}
		Art art = Art.SPIELER.erkennt(formel) ? Art.SPIELER : Art.TEAM.erkennt(formel) ? Art.TEAM : standardArt;
		List<String> literale = textLiterale(formel);
		Optional<String> onlineId = literale.size() > 1 ? Optional.of(literale.get(1)) : Optional.empty();
		return Optional.of(new NrZellFormel(art, uuid.get(), onlineId));
	}

	/** @return Online-ID aus dem Formeltext einer Nr-Zelle */
	public static Optional<String> onlineIdAus(String formel) {
		return lese(formel, Art.TEAM).flatMap(NrZellFormel::onlineId);
	}

	public NrZellFormel mitOnlineId(Optional<String> neueOnlineId) {
		return new NrZellFormel(art, uuid, neueOnlineId);
	}

	public NrZellFormel mitUuid(String neueUuid) {
		return new NrZellFormel(art, neueUuid, onlineId);
	}

	/** @return Formel für die Nr-Zelle, die {@code nr} anzeigt */
	public String formel(int nr) {
		StringBuilder formel = new StringBuilder(art.funktion()).append('(').append(nr).append(";\"").append(uuid)
				.append('"');
		onlineId.ifPresent(id -> formel.append(";\"").append(id.replace("\"", "")).append('"'));
		return formel.append(')').toString();
	}

	private static List<String> textLiterale(String formel) {
		List<String> literale = new ArrayList<>();
		Matcher treffer = TEXT_LITERAL.matcher(formel);
		while (treffer.find()) {
			literale.add(treffer.group(1));
		}
		return literale;
	}
}
