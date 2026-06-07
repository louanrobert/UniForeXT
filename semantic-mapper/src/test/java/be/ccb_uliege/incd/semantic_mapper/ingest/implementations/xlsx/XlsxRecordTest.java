package be.ccb_uliege.incd.semantic_mapper.ingest.implementations.xlsx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

class XlsxRecordTest {

    @Test
    void getTrimsValuesAndHashUsesTrimmedValue() {
        XlsxRecord record = new XlsxRecord(Map.of("name", " Alice "));

        assertEquals("Alice", record.get("name"));
        assertEquals(DigestUtils.sha256Hex("Alice"), record.getHashed("name"));
        assertTrue(record.has("name"));
    }

    @Test
    void getRejectsMissingFields() {
        XlsxRecord record = new XlsxRecord(Map.of("name", "Alice"));

        assertThrows(IllegalStateException.class, () -> record.get("missing"));
    }

    @Test
    void constructorRejectsNullValues() {
        Map<String, String> fields = new HashMap<>();
        fields.put("nullValue", null);

        assertThrows(NullPointerException.class, () -> new XlsxRecord(fields));
    }

    @Test
    void hasReturnsFalseForMissingAndBlankValues() {
        XlsxRecord record = new XlsxRecord(Map.of(
                "blank", "   ",
                "value", "x"));

        assertFalse(record.has("missing"));
        assertFalse(record.has("blank"));
        assertTrue(record.has("value"));
    }
}
