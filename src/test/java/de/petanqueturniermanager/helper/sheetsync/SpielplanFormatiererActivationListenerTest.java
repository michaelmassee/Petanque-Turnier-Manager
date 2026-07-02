package de.petanqueturniermanager.helper.sheetsync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;

class SpielplanFormatiererActivationListenerTest {

    @Test
    void merkeNeuenFokus_blockiertDoppeltenFokusAufSelbemSheet() {
        var listener = new SpielplanFormatiererActivationListener(
                Mockito.mock(XComponentContext.class),
                (xDoc, sheet) -> true,
                (ws, sheet) -> Mockito.mock(SheetRunner.class),
                "TEST");
        var xDoc = Mockito.mock(XSpreadsheetDocument.class);
        var xSheet = Mockito.mock(XSpreadsheet.class, Mockito.withSettings().extraInterfaces(com.sun.star.container.XNamed.class));
        Mockito.when(((com.sun.star.container.XNamed) xSheet).getName()).thenReturn("Spielplan");

        assertThat(listener.merkeNeuenFokus(xDoc, xSheet)).isTrue();
        assertThat(listener.merkeNeuenFokus(xDoc, xSheet)).isFalse();

        listener.aufFokusVerloren();

        assertThat(listener.merkeNeuenFokus(xDoc, xSheet)).isTrue();
    }

    @Test
    void fokusToken_bleibtStabilBeiNeuenUnoProxiesFuerDasselbeSheet() {
        var xDoc = Mockito.mock(XSpreadsheetDocument.class);
        var proxy1 = sheetProxy("Spielplan");
        var proxy2 = sheetProxy("Spielplan");

        assertThat(proxy1).isNotSameAs(proxy2);
        assertThat(SheetFokusToken.von(xDoc, proxy1))
                .isEqualTo(SheetFokusToken.von(xDoc, proxy2));
    }

    private static XSpreadsheet sheetProxy(String name) {
        var xSheet = Mockito.mock(XSpreadsheet.class,
                Mockito.withSettings().extraInterfaces(com.sun.star.container.XNamed.class));
        Mockito.when(((com.sun.star.container.XNamed) xSheet).getName()).thenReturn(name);
        return xSheet;
    }
}
