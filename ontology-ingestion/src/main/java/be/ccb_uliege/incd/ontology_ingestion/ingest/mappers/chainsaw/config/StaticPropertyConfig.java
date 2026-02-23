package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.config;

public class StaticPropertyConfig {
    private String owlProperty;
    private String value;

    public String getOwlProperty() {
        return owlProperty;
    }

    public void setOwlProperty(String owlProperty) {
        this.owlProperty = owlProperty;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
