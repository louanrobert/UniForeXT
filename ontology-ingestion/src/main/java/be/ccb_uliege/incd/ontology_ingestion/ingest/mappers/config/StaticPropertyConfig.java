package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config;

import lombok.Getter;

/* 
 * Configuration for a static property to be set on all individuals created by a SourceMapper.
 * 
 * This class represents the configuration for a static property that should be set on all individuals created by a specific SourceMapper.
 * It includes information about the OWL property to set and the value to set it to.
 * 
 * Read-only
 */
@Getter
public class StaticPropertyConfig {
    private String owlProperty; // 
    private String value;
    private String dataType = "xsd:string";
}
