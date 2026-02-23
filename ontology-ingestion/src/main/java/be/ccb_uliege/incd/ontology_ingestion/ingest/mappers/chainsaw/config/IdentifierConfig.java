package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.config;

import java.util.List;

public class IdentifierConfig {
    private List<String> fields;
    private String separator;

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
