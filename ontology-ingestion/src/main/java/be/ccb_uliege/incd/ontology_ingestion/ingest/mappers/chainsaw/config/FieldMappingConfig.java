package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.config;

import java.util.List;

public class FieldMappingConfig {
    private String sourceField;
    private String owlProperty;
    private String type;
    private String prefix;
    private boolean unique;
    private String owlClass;
    private String linkProperty;
    private String identifierField;
    private boolean useHash;
    private List<FieldMappingConfig> dataProperties;
    private List<FieldMappingConfig> nestedLinks;

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getOwlProperty() {
        return owlProperty;
    }

    public void setOwlProperty(String owlProperty) {
        this.owlProperty = owlProperty;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public String getOwlClass() {
        return owlClass;
    }

    public void setOwlClass(String owlClass) {
        this.owlClass = owlClass;
    }

    public String getLinkProperty() {
        return linkProperty;
    }

    public void setLinkProperty(String linkProperty) {
        this.linkProperty = linkProperty;
    }

    public String getIdentifierField() {
        return identifierField;
    }

    public void setIdentifierField(String identifierField) {
        this.identifierField = identifierField;
    }

    public boolean isUseHash() {
        return useHash;
    }

    public void setUseHash(boolean useHash) {
        this.useHash = useHash;
    }

    public List<FieldMappingConfig> getDataProperties() {
        return dataProperties;
    }

    public void setDataProperties(List<FieldMappingConfig> dataProperties) {
        this.dataProperties = dataProperties;
    }

    public List<FieldMappingConfig> getNestedLinks() {
        return nestedLinks;
    }

    public void setNestedLinks(List<FieldMappingConfig> nestedLinks) {
        this.nestedLinks = nestedLinks;
    }
}
