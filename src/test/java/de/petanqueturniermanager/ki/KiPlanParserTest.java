package de.petanqueturniermanager.ki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KiPlanParserTest {

    @Test
    void parseLiestAktionenUndWarnungen() {
        KiPlan plan = KiPlanParser.parse("""
                ```json
                {
                  "summary": "Liga anlegen",
                  "requiresConfirmation": true,
                  "actions": [
                    {"type":"new_tournament","target":"LIGA","parameters":{"system":"LIGA"}}
                  ],
                  "warnings": ["Pruefen"],
                  "dataPreview": "Preview"
                }
                ```
                """);

        assertThat(plan.summary()).isEqualTo("Liga anlegen");
        assertThat(plan.actions()).hasSize(1);
        assertThat(plan.actions().getFirst().type()).isEqualTo("new_tournament");
        assertThat(plan.warnings()).containsExactly("Pruefen");
        assertThat(plan.dataPreview()).isEqualTo("Preview");
    }

    @Test
    void parseMeldetFehlendenJsonPlan() {
        assertThatThrownBy(() -> KiPlanParser.parse("kein json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON");
    }
}
