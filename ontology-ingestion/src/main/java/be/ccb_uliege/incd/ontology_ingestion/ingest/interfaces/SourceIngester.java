package be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces;

import java.nio.file.Path;

/**
 * Abstraction for ingesting data from a source file.
 *
 * Each implementation handles a specific file format (CSV, JSON, XML, …)
 * and delegates the semantic mapping of each record to a SourceMapper.
 *
 */
public interface SourceIngester {

    /**
     * Reads the file at the given path, converts each record using the mapper,
    * and delegates each record to the mapper.
     *
     * @param file      path to the source file
     * @param mapper    mapper that converts individual records into RDF
     * @param delimiter the field delimiter used in the source file
     */
    void ingest(Path file, SourceMapper mapper, Character delimiter);
}
