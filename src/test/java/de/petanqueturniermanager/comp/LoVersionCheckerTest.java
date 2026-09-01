package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.sun.star.uno.XComponentContext;

class LoVersionCheckerTest {

    @Test
    void ermittleBuildVersionLiefertNieNull() {
        // Ob die Ressource build-lo-version.properties auf dem Test-Klassenpfad liegt,
        // hängt davon ab, ob generateLoVersionInfo/processResources vor dem Testlauf
        // gelaufen ist - beides ist zulässig, nur ein Absturz nicht.
        assertThat(LoVersionChecker.ermittleBuildVersion()).isNotBlank();
    }

    @Test
    void pruefungWirftNieBeiFehlendemKontext() {
        XComponentContext context = mock(XComponentContext.class);
        assertThatCode(() -> LoVersionChecker.pruefeUndLoggeKompatibilitaet(context)).doesNotThrowAnyException();
    }
}
