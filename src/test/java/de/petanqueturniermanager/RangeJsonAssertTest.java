package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Vollständige Branch-Coverage für {@link RangeJsonAssert#validate}.
 *
 * <p>Kernregression: Vor Commit d5bfc093 hat die Methode {@code refRangeData}
 * gegen sich selbst verglichen. {@link #contentMismatch_failsAssertion()}
 * sichert ab, dass abweichender Inhalt jetzt zuverlässig fehlschlägt.
 */
class RangeJsonAssertTest {

	private static InputStream json(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

	private static RangeData range(String[]... rows) {
		RangeData rd = new RangeData();
		for (String[] r : rows) {
			RowData row = new RowData();
			for (String c : r) {
				row.add(new CellData(c));
			}
			rd.add(row);
		}
		return rd;
	}

	@Test
	@DisplayName("Happy Path: identische Daten und Referenz")
	void identicalData_passes() {
		RangeData data = range(new String[] { "A", "B" }, new String[] { "1", "2" });
		String ref = "[[{\"data\":\"A\"},{\"data\":\"B\"}],[{\"data\":1.0},{\"data\":2.0}]]";

		assertThatCode(() -> RangeJsonAssert.validate(data, json(ref))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Regression: abweichender Zellinhalt MUSS fehlschlagen (Bug aus d5bfc093)")
	void contentMismatch_failsAssertion() {
		RangeData data = range(new String[] { "A", "B" }, new String[] { "1", "FALSCH" });
		String ref = "[[{\"data\":\"A\"},{\"data\":\"B\"}],[{\"data\":\"1\"},{\"data\":\"2\"}]]";

		assertThatThrownBy(() -> RangeJsonAssert.validate(data, json(ref)))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	void jsonFileNull_failsAssertion() {
		RangeData data = range(new String[] { "A" });

		assertThatThrownBy(() -> RangeJsonAssert.validate(data, null))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	void rangeDataNull_failsAssertion() {
		assertThatThrownBy(() -> RangeJsonAssert.validate(null, json("[[{\"data\":\"A\"}]]")))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	void rangeDataEmpty_failsAssertion() {
		assertThatThrownBy(() -> RangeJsonAssert.validate(new RangeData(), json("[[{\"data\":\"A\"}]]")))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	@DisplayName("JSON-Inhalt 'null' deserialisiert zu null-Referenz → fail")
	void refDataParsedToNull_failsAssertion() {
		RangeData data = range(new String[] { "A" });

		assertThatThrownBy(() -> RangeJsonAssert.validate(data, json("null")))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	@DisplayName("Leeres Referenz-Array → fail (Referenz darf nicht leer sein)")
	void refDataEmpty_failsAssertion() {
		RangeData data = range(new String[] { "A" });

		assertThatThrownBy(() -> RangeJsonAssert.validate(data, json("[]")))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	void sizeMismatch_failsAssertion() {
		RangeData data = range(new String[] { "A" }, new String[] { "B" });
		String ref = "[[{\"data\":\"A\"}]]";

		assertThatThrownBy(() -> RangeJsonAssert.validate(data, json(ref)))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	@DisplayName("Numerische Zellen werden über getStringVal verglichen")
	void numericCells_compareEqualAsString() {
		RangeData data = new RangeData();
		RowData row = new RowData();
		row.add(new CellData(Integer.valueOf(42)));
		row.add(new CellData(Double.valueOf(7.0)));
		data.add(row);
		String ref = "[[{\"data\":42.0},{\"data\":7.0}]]";

		assertThatCode(() -> RangeJsonAssert.validate(data, json(ref))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("null-Zellen auf beiden Seiten gelten als gleich")
	void nullCells_compareEqual() {
		RangeData data = new RangeData();
		RowData row = new RowData();
		row.add(new CellData(null));
		data.add(row);
		String ref = "[[{}]]";

		assertThatCode(() -> RangeJsonAssert.validate(data, json(ref))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Mismatch in mittlerer Zeile schlägt fehl (Iteration läuft komplett durch)")
	void mismatchInLaterRow_failsAssertion() {
		RangeData data = range(
				new String[] { "ok" },
				new String[] { "ok" },
				new String[] { "FALSCH" });
		String ref = "[[{\"data\":\"ok\"}],[{\"data\":\"ok\"}],[{\"data\":\"richtig\"}]]";

		assertThatThrownBy(() -> RangeJsonAssert.validate(data, json(ref)))
				.isInstanceOf(AssertionError.class);
	}
}
