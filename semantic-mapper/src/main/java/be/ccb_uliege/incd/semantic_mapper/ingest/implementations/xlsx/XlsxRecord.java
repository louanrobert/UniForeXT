package be.ccb_uliege.incd.semantic_mapper.ingest.implementations.xlsx;

import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceRecord;

/**
 * SourceRecord implementation backed by one XLSX row.
 */
public class XlsxRecord implements SourceRecord {

    private final Map<String, String> fields;

    public XlsxRecord(Map<String, String> fields) {
        this.fields = Map.copyOf(fields);
    }

    @Override
    public String get(String field) throws IllegalStateException, IllegalArgumentException {
        if (!fields.containsKey(field)) {
            throw new IllegalStateException("Field not found in XLSX row: " + field);
        }

        String value = fields.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Field value is null: " + field);
        }

        return value.trim();
    }

    @Override
    public String getHashed(String field) throws IllegalStateException, IllegalArgumentException {
        return DigestUtils.sha256Hex(this.get(field));
    }

    @Override
    public boolean has(String field) {
        if (!fields.containsKey(field)) {
            return false;
        }

        String value = fields.get(field);
        return value != null && !value.isBlank();
    }
}
