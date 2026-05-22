package be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config;

import java.util.List;

import lombok.Getter;

/*
 * Configuration for a SourceMapper defined in the YAML file.
 * 
 * This class represents the configuration for a single SourceMapper as defined in the YAML configuration file.
 * It includes information about the name of the mapper, the OWL class to instantiate, the source file to read from, how to construct identifiers for individuals,
 * any generic mapping groups to include, static properties to set on all individuals, and specific field mappings for data properties and linked individuals.
 * 
 * Read-only
 */
@Getter
public class MapperConfig {
    private String name;
    private String owlClass;
    private String forensicTool;
    private String file;
    private Character delimiter;
    private IdentifierConfig identifier;
    private List<String> generics;
    private List<StaticPropertyConfig> staticProperties;
    private List<FieldMappingConfig> fieldMappings;
}
