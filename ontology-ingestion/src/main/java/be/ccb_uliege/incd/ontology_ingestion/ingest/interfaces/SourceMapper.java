package be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces;

import org.apache.jena.rdf.model.Model;

/**
 * This interface defines a mapper that takes a SourceRecord and maps it to a Jena Model.
 * Implementations of this interface will contain the logic to transform the source data into RDF triples.
 */
public interface SourceMapper {

    /**
     * Maps a SourceRecord to RDF triples and adds them to the provided Jena Model.
     * @param record the source record to map
     * @param model the Jena Model to which the RDF triples should be added
     * 
     * Implementations should handle any necessary transformations, URI generation, and triple creation based on the content of the SourceRecord.
     */
    void map(SourceRecord record, Model model);
}
