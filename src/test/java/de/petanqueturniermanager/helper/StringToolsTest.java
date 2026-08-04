package de.petanqueturniermanager.helper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class StringToolsTest {

    @Test
    public void testBooleanToStringTrue() throws Exception {
        assertThat(StringTools.booleanToString(true)).isEqualTo("J");
    }

    @Test
    public void testBooleanToStringFalse() throws Exception {
        assertThat(StringTools.booleanToString(false)).isEqualTo("N");
    }

    @Test
    public void testStringToBooleanGrossJ() throws Exception {
        assertThat(StringTools.stringToBoolean("J")).isTrue();
    }

    @Test
    public void testStringToBooleanKleinJ() throws Exception {
        assertThat(StringTools.stringToBoolean("j")).isTrue();
    }

    @Test
    public void testStringToBooleanN() throws Exception {
        assertThat(StringTools.stringToBoolean("N")).isFalse();
    }

    @Test
    public void testStringToBooleanTrue() throws Exception {
        // Boolean.parseBoolean Fallback
        assertThat(StringTools.stringToBoolean("true")).isTrue();
    }

    @Test
    public void testStringToBooleanFalse() throws Exception {
        assertThat(StringTools.stringToBoolean("false")).isFalse();
    }

    @Test
    public void testStringToBooleanNull() throws Exception {
        // Boolean.parseBoolean(null) gibt false zurueck
        assertThat(StringTools.stringToBoolean(null)).isFalse();
    }

    @Test
    public void testStringToBooleanLeerstring() throws Exception {
        assertThat(StringTools.stringToBoolean("")).isFalse();
    }

    @Test
    public void testStringToBooleanJInString() throws Exception {
        // enthaelt "J" -> true (containsIgnoreCase)
        assertThat(StringTools.stringToBoolean("Ja")).isTrue();
    }

    @Test
    public void testStringToBooleanBooleanUndJ() throws Exception {
        // Rundflug: booleanToString und zurueck
        String s = StringTools.booleanToString(true);
        assertThat(StringTools.stringToBoolean(s)).isTrue();

        s = StringTools.booleanToString(false);
        assertThat(StringTools.stringToBoolean(s)).isFalse();
    }

    @Test
    public void testIsValidUhrzeitHhMmGueltig() throws Exception {
        assertThat(StringTools.isValidUhrzeitHhMm("00:00")).isTrue();
        assertThat(StringTools.isValidUhrzeitHhMm("09:05")).isTrue();
        assertThat(StringTools.isValidUhrzeitHhMm("23:59")).isTrue();
    }

    @Test
    public void testIsValidUhrzeitHhMmUngueltigesFormat() throws Exception {
        assertThat(StringTools.isValidUhrzeitHhMm("9:05")).isFalse(); // fehlende fuehrende Null
        assertThat(StringTools.isValidUhrzeitHhMm("24:00")).isFalse(); // Stunde ausserhalb 0-23
        assertThat(StringTools.isValidUhrzeitHhMm("12:60")).isFalse(); // Minute ausserhalb 0-59
        assertThat(StringTools.isValidUhrzeitHhMm("abc")).isFalse();
        assertThat(StringTools.isValidUhrzeitHhMm("")).isFalse();
        assertThat(StringTools.isValidUhrzeitHhMm(null)).isFalse();
    }

}
