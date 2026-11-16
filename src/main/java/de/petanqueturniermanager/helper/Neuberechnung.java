/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper;

import com.sun.star.sheet.XCalculatable;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Neuberechnung des Calc-Dokuments, ohne eine laufende Zelleingabe des Anwenders abzubrechen.
 * <p>
 * <b>Problem:</b> {@link XCalculatable#calculate()} und {@link XCalculatable#calculateAll()} schließen in
 * LibreOffice eine gerade laufende Zelleingabe ab ({@code ScDocShell::DoRecalc}/{@code DoHardRecalc} →
 * {@code ScTabView::UpdateInputLine()} → {@code ScModule::InputEnterHandler()}, siehe
 * {@code sc/source/ui/docshell/docsh4.cxx}). Läuft das aus einem Hintergrund-Thread, während der Anwender
 * zügig „13“ in eine Ergebniszelle tippt, wird die „1“ sofort übernommen und die „3“ beginnt eine neue Eingabe,
 * die die Zelle überschreibt – die erste Ziffer „verschwindet“.
 * <p>
 * <b>Lösung:</b> Bei eingeschalteter automatischer Berechnung ist ein expliziter Aufruf überflüssig: Calc
 * rechnet veraltete Formelzellen beim Lesen selbst nach ({@code ScFormulaCell::MaybeInterpret}), auch beim
 * Sortieren. Hintergrundläufe (Sheet-Synchronisation, Webserver, PTM-Online) laufen daher
 * {@linkplain #eingabeschonend(Aktion) eingabeschonend}: Die Automatik bleibt an, explizite Neuberechnungen
 * entfallen.
 */
public final class Neuberechnung {

	private static final ThreadLocal<Boolean> EINGABESCHONEND = ThreadLocal.withInitial(() -> Boolean.FALSE);

	private Neuberechnung() {
	}

	@FunctionalInterface
	public interface Aktion {
		void ausfuehren() throws GenerateException;
	}

	/**
	 * Führt die Aktion so aus, dass alle über {@link #fuer(XCalculatable)} bezogenen Berechnungs-Schnittstellen
	 * dieses Threads die Zelleingabe des Anwenders nicht abbrechen. Verschachtelte Aufrufe sind erlaubt.
	 */
	public static void eingabeschonend(Aktion aktion) throws GenerateException {
		boolean vorher = istEingabeschonend();
		EINGABESCHONEND.set(Boolean.TRUE);
		try {
			aktion.ausfuehren();
		} finally {
			EINGABESCHONEND.set(vorher);
		}
	}

	/** Ob der aktuelle Thread gerade {@linkplain #eingabeschonend(Aktion) eingabeschonend} läuft. */
	public static boolean istEingabeschonend() {
		return EINGABESCHONEND.get();
	}

	/**
	 * Liefert die Berechnungs-Schnittstelle für den aktuellen Thread: unverändert, oder – innerhalb von
	 * {@link #eingabeschonend(Aktion)} – eine Hülle, die die Zelleingabe des Anwenders nicht abbricht.
	 */
	public static XCalculatable fuer(XCalculatable calc) {
		if (calc == null || !istEingabeschonend()) {
			return calc;
		}
		return new EingabeschonendeBerechnung(calc);
	}

	/**
	 * Rechnet nur nach, wenn die automatische Berechnung aus ist; sonst erledigt Calc das beim Lesen selbst.
	 *
	 * @return {@code true}, wenn nachgerechnet wurde
	 */
	public static boolean nurFallsAutomatikAus(XCalculatable calc) {
		if (calc == null || calc.isAutomaticCalculationEnabled()) {
			return false;
		}
		calc.calculate();
		return true;
	}

	/**
	 * Lässt die automatische Berechnung eingeschaltet und führt explizite Neuberechnungen nur aus, wenn die
	 * Automatik (z.&nbsp;B. vom Anwender) ausgeschaltet ist.
	 */
	private static final class EingabeschonendeBerechnung implements XCalculatable {

		private final XCalculatable calc;

		EingabeschonendeBerechnung(XCalculatable calc) {
			this.calc = calc;
		}

		@Override
		public void calculate() {
			nurFallsAutomatikAus(calc);
		}

		@Override
		public void calculateAll() {
			if (!calc.isAutomaticCalculationEnabled()) {
				calc.calculateAll();
			}
		}

		@Override
		public boolean isAutomaticCalculationEnabled() {
			return calc.isAutomaticCalculationEnabled();
		}

		@Override
		public void enableAutomaticCalculation(boolean einschalten) {
			// Abschalten zur Beschleunigung entfällt: danach wäre ein calculate() nötig, das die Eingabe abbricht.
			if (einschalten) {
				calc.enableAutomaticCalculation(true);
			}
		}
	}
}
