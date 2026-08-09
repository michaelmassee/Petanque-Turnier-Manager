package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.table.CellRangeAddress;

/**
 * Unit-Tests für {@link TabellenMapper#schneide(CellRangeAddress, CellRangeAddress)}.
 * Reine Arithmetik, kein UNO-Kontext nötig.
 */
class TabellenMapperTest {

    @Test
    void schneide_volleUeberlappung_liefertKleinerenBereich() {
        CellRangeAddress ausgewaehlteSpalte = bereich(0, 2, 1_048_575, 2);
        CellRangeAddress usedArea = bereich(0, 0, 4, 9);

        CellRangeAddress schnitt = TabellenMapper.schneide(ausgewaehlteSpalte, usedArea);

        assertThat(schnitt).isNotNull();
        assertThat(schnitt.StartRow).isZero();
        assertThat(schnitt.EndRow).isEqualTo(4);
        assertThat(schnitt.StartColumn).isEqualTo(2);
        assertThat(schnitt.EndColumn).isEqualTo(2);
    }

    @Test
    void schneide_teilweiseUeberlappung_liefertSchnittmenge() {
        CellRangeAddress a = bereich(0, 0, 5, 5);
        CellRangeAddress b = bereich(3, 3, 10, 10);

        CellRangeAddress schnitt = TabellenMapper.schneide(a, b);

        assertThat(schnitt).isNotNull();
        assertThat(schnitt.StartRow).isEqualTo(3);
        assertThat(schnitt.EndRow).isEqualTo(5);
        assertThat(schnitt.StartColumn).isEqualTo(3);
        assertThat(schnitt.EndColumn).isEqualTo(5);
    }

    @Test
    void schneide_keineUeberlappung_liefertNull() {
        CellRangeAddress a = bereich(0, 0, 2, 2);
        CellRangeAddress b = bereich(10, 10, 12, 12);

        assertThat(TabellenMapper.schneide(a, b)).isNull();
    }

    @Test
    void schneide_bereichVollstaendigEnthalten_liefertKleinerenBereich() {
        CellRangeAddress aussen = bereich(0, 0, 100, 100);
        CellRangeAddress innen = bereich(5, 5, 10, 10);

        CellRangeAddress schnitt = TabellenMapper.schneide(aussen, innen);

        assertThat(schnitt.StartRow).isEqualTo(5);
        assertThat(schnitt.EndRow).isEqualTo(10);
        assertThat(schnitt.StartColumn).isEqualTo(5);
        assertThat(schnitt.EndColumn).isEqualTo(10);
    }

    private static CellRangeAddress bereich(int startRow, int startColumn, int endRow, int endColumn) {
        var range = new CellRangeAddress();
        range.StartRow = startRow;
        range.StartColumn = startColumn;
        range.EndRow = endRow;
        range.EndColumn = endColumn;
        return range;
    }
}
