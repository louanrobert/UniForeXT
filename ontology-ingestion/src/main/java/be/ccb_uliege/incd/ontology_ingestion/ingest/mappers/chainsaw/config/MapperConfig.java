package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.config;

import java.util.List;

public class MapperConfig {
    private String name;
    private String owlClass;
    private IdentifierConfig identifier;
    private List<String> generics;
    private List<StaticPropertyConfig> staticProperties;
    private List<FieldMappingConfig> fieldMappings;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwlClass() {
        return owlClass;
    }

    public void setOwlClass(String owlClass) {
        this.owlClass = owlClass;
    }

    public IdentifierConfig getIdentifier() {
        return identifier;
    }

    public void setIdentifier(IdentifierConfig identifier) {
        this.identifier = identifier;
    }

    public List<String> getGenerics() {
        return generics;
    }

    public void setGenerics(List<String> generics) {
        this.generics = generics;
    }

    public List<StaticPropertyConfig> getStaticProperties() {
        return staticProperties;
    }

    public void setStaticProperties(List<StaticPropertyConfig> staticProperties) {
        this.staticProperties = staticProperties;
    }

    public List<FieldMappingConfig> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<FieldMappingConfig> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }
}
