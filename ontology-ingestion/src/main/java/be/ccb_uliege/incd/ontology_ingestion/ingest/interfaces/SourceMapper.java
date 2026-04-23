package be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces;

import be.ccb_uliege.incd.ontology_ingestion.owl.kg.KnowledgeGraphFacade;

/**
 * This interface defines a mapper that takes a SourceRecord and maps it to a Jena Model.
 * Implementations of this interface will contain the logic to transform the source data into RDF triples.
 */
public interface SourceMapper {

    /**
     * Maps a SourceRecord to RDF triples using the mapper's knowledge graph facade.
     * @param record the source record to map
     * 
     * Implementations should handle any necessary transformations, URI generation, and triple creation based on the content of the SourceRecord.
     */
    void map(SourceRecord record);
}
