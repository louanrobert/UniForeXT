package be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config;

import java.util.List;
import java.util.Map;

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
    private String dataType = "xsd:string";
    private String prefix;
    private boolean unique = true;
    private List<String> valueTransforms;
    private Map<String, String> valueMap;
    private boolean valueMapCaseInsensitive = false;
    private String owlClass;
    private String linkProperty;
    private IdentifierConfig identifier;
    private List<FieldMappingConfig> dataProperties;
    private List<FieldMappingConfig> nestedLinks;
}
