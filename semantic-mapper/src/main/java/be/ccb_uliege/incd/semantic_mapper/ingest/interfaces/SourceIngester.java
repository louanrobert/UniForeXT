package be.ccb_uliege.incd.semantic_mapper.ingest.interfaces;

import java.nio.file.Path;
import java.util.function.BiConsumer;

/**
 * Abstraction for ingesting data from a source file.
 *
 * Each implementation handles a specific file format (CSV, JSON, XML, etc.)
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
     * @param delimiter optional field delimiter used in the source file (null for
     *                  non-delimited formats)
     */
    default void ingest(Path file, SourceMapper mapper, Character delimiter) {
        ingest(file, mapper, delimiter, null);
    }

    /**
     * Reads the file at the given path, converts each record using the mapper,
     * and delegates each record to the mapper.
     *
     * @param file             path to the source file
     * @param mapper           mapper that converts individual records into RDF
     * @param delimiter        optional field delimiter used in the source file
     *                         (null for non-delimited formats)
     * @param progressListener optional progress callback receiving completed and
     *                         total records
     */
    void ingest(Path file, SourceMapper mapper, Character delimiter, BiConsumer<Integer, Integer> progressListener);
}
