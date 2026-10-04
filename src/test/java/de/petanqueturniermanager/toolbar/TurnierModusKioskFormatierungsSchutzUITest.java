package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStatusListener;
import com.sun.star.util.URL;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.Lo;

class TurnierModusKioskFormatierungsSchutzUITest extends BaseCalcUITest {

    @Test
    void formatierungenLoeschenIstImKioskModusDeaktiviert() throws Exception {
        var turnierModus = TurnierModus.get();
        turnierModus.aktivieren(wkingSpreadsheet);
        try {
            assertThat(turnierModus.istAktiv()).isTrue();
            var frame = Lo.qi(XModel.class, doc).getCurrentController().getFrame();
            var dispatchProvider = Lo.qi(XDispatchProvider.class, frame);
            var resetAttributes = url(".uno:ResetAttributes");
            var dispatch = dispatchProvider.queryDispatch(resetAttributes, "_self", 0);
            assertThat(dispatch).as("Der Kiosk-Filter liefert einen eigenen Dispatch").isNotNull();
            final boolean[] enabled = {true};
            dispatch.addStatusListener(new XStatusListener() {
                @Override
                public void statusChanged(com.sun.star.frame.FeatureStateEvent event) {
                    enabled[0] = event.IsEnabled;
                }

                @Override
                public void disposing(com.sun.star.lang.EventObject event) {
                    // nichts zu tun
                }
            }, resetAttributes);

            assertThat(enabled[0]).as("Formatierungen löschen ist im Kioskmodus deaktiviert").isFalse();
        } finally {
            turnierModus.wiederherstellenAlleElemente(wkingSpreadsheet);
        }
    }

    private URL url(String complete) {
        var url = new URL();
        url.Complete = complete;
        return url;
    }
}
