package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config;

import java.util.List;

import lombok.Getter;

/*
 * Configuration for mapping a single field from the source data to an OWL property.
 * 
 * This class represents the configuration for how a specific field in the source data should be mapped to an OWL property.
 * It includes information about the source field, the target OWL property, the type of mapping (data property or object property),
 * and additional options such as uniqueness, prefixes, and nested mappings for linked individuals.
 * 
 * Read-only
 */
@Getter
public class FieldMappingConfig {
    private String sourceField;
    private String owlProperty;
    private String type;
    private String prefix;
    private boolean unique = true;
    private String owlClass;
    private String linkProperty;
    private IdentifierConfig identifier;
    private List<FieldMappingConfig> dataProperties;
    private List<FieldMappingConfig> nestedLinks;
}
