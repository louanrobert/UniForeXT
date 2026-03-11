package be.ccb_uliege.incd.ontology_ingestion.ingest.implementations.csv;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.csv.CSVRecord;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;

/**
 * Wrapper around SourceRecord for CSV inputs. This class corresponds to a
 * single record (line) in a CSV file.
 * 
 * Provides methods to get field values, check if a field is present and
 * non-empty, and get hashed values (SHA-256).
 * 
 * Uses Apache Commons CSV to read records.
 * 
 */
public class CsvRecord implements SourceRecord {

    // The underlying CSVRecord from Apache Commons CSV
    private final CSVRecord csvRecord;

    // Constructor takes a CSVRecord
    public CsvRecord(CSVRecord csvRecord) {
        this.csvRecord = csvRecord;
    }

    /**
     * Returns the value of a field in the CSV record, trimmed of whitespace.
     * If the field is not present, this will throw an exception (as per CSVRecord
     * behavior).
     * 
     * @param field the name of the field to retrieve
     * @return the trimmed value of the field
     * @throws IllegalStateException    if the field is not mapped in the CSV record
     * @throws IllegalArgumentException if the field value is null
     */
    @Override
    public String get(String field) throws IllegalStateException, IllegalArgumentException {
        return csvRecord.get(field).trim();
    }

    /**
     * Returns the SHA-256 hash of a field value in the CSV record, trimmed of
     * whitespace.
     * If the field is not present, this will throw an exception (as per CSVRecord
     * behavior).
     * 
     * @param field the name of the field to retrieve and hash
     * @return the SHA-256 hash of the trimmed field value
     * @throws IllegalStateException    if the field is not mapped in the CSV record
     * @throws IllegalArgumentException if the field value is null
     */
    @Override
    public String getHashed(String field) throws IllegalStateException, IllegalArgumentException {
        return DigestUtils.sha256Hex(this.get(field));
    }

    /**
     * Checks if a field is present in the CSV record and has a non-empty, non-blank
     * value.
     * Returns false if the field is not mapped, or if the value is empty or blank.
     * 
     * @param field the name of the field to check
     * @return true if the field is mapped and has a non-empty, non-blank value;
     *         false otherwise
     */
    @Override
    public boolean has(String field) {
        if (!csvRecord.isMapped(field)) {
            return false;
        }
        String value = csvRecord.get(field);
        return !value.isEmpty() && !value.isBlank();
    }
}
