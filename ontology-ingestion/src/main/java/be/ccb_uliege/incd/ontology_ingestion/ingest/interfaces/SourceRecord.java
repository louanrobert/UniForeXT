package be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces;

/**
 * This interface represents a record from the source data, which can be a CSV row, a JSON object, etc.
 * It provides methods to access the fields of the record, check if a field is present and non-empty, and get hashed values of fields.
 */
public interface SourceRecord {
    String get(String field);
    public String getHashed(String field);
    boolean has(String field);
}
