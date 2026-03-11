package be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces;

import java.nio.file.Path;

import org.apache.jena.rdf.model.Model;

/**
 * Abstraction for ingesting data from a source file into a Jena Model.
 *
 * Each implementation handles a specific file format (CSV, JSON, XML, …)
 * and delegates the semantic mapping of each record to a SourceMapper.
 *
 */
public interface SourceIngester {

    /**
     * Reads the file at the given path, converts each record using the mapper,
     * and populates the model with the resulting RDF triples.
     *
     * @param file      path to the source file
     * @param mapper    mapper that converts individual records into RDF
     * @param model     the Jena model to populate
     * @param delimiter the field delimiter used in the source file
     */
    void ingest(Path file, SourceMapper mapper, Model model, Character delimiter);
}
