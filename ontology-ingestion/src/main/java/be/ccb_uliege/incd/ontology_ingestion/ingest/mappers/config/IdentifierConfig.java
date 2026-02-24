package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

import java.util.List;

/*
 * Configuration for defining an identifier for an OWL individual.
 * 
 * This class represents the configuration for how an identifier should be constructed for an OWL individual.
 * It includes information about the source fields that make up the identifier, a separator string, and whether to use a hash of the fields.
 * 
 * Read-only
 */
@Getter
public class IdentifierConfig {
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> fields;
    private String separator = "_";
    private boolean useHash = false;
}
