package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config;

import java.util.List;
import java.util.Map;

import lombok.Getter;

/*
 * Configuration for multiple SourceMappers defined in the YAML file.
 * 
 * This class represents the overall configuration for multiple SourceMappers as defined in the YAML configuration file.
 * It includes information about generic mapping groups that can be reused across multiple mappers, and a list of individual mapper configurations.
 * 
 * Read-only
 */
@Getter
public class MappersConfig {
    private Map<String, List<FieldMappingConfig>> genericMappings;
    private List<MapperConfig> mappers;
}
