package be.ccb_uliege.incd.ontology_ingestion.ingest.implementations.csv;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.csv.CSVRecord;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;

public class CsvRecord implements SourceRecord {

   private final CSVRecord csvRecord;
   public CsvRecord(CSVRecord csvRecord) {
       this.csvRecord = csvRecord;
   }

   @Override public String get(String field) { return csvRecord.get(field).trim(); }
   @Override public String getHashed(String field) { return DigestUtils.sha256Hex(csvRecord.get(field).trim()); }
   @Override public boolean has(String field) {
       if (!csvRecord.isMapped(field)) {
           return false;
       }
       String value = csvRecord.get(field);
       return !value.isEmpty() && !value.isBlank();
   }
}
