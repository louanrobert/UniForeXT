package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import java.nio.file.Path;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;

/**
 * Immutable task definition for ingesting one source file.
 */
public record IngestionTask(Path file, SourceMapper mapper, Character delimiter) {
}
