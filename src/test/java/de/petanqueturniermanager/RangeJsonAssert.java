package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Vergleicht eine {@link RangeData}-Instanz gegen eine JSON-Referenzdatei.
 *
 * <p>Reiner String-Vergleich über {@link CellData#getStringVal()}. Wird sowohl
 * von den UI-Tests (via {@code BaseCalcUITest.validateWithJson}) als auch in
 * {@link RangeJsonAssertTest} direkt verwendet.
 */
public final class RangeJsonAssert {

	private RangeJsonAssert() {
	}

	public static void validate(RangeData rangeData, InputStream jsonFile) {

		assertThat(jsonFile).isNotNull();
		assertThat(rangeData).isNotNull().isNotEmpty();

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		RangeData refRangeData = gson.fromJson(
				new BufferedReader(new InputStreamReader(jsonFile, StandardCharsets.UTF_8)),
				RangeData.class);

		assertThat(refRangeData).isNotNull().isNotEmpty();
		assertThat(rangeData).hasSameSizeAs(refRangeData);

		int idx = 0;
		for (RowData data : rangeData) {
			List<String> expected = refRangeData.get(idx).stream()
					.map(CellData::getStringVal)
					.collect(Collectors.toList());
			assertThat(data).extracting(CellData::getStringVal).containsExactlyElementsOf(expected);
			idx++;
		}
	}
}
