package be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline;

/**
 * A single executable stage in the ingestion pipeline.
 */
public interface IngestionStage {

    /**
     * Executes the stage against the shared pipeline context.
     *
     * @param context shared state exchanged between pipeline stages
     */
    void execute(PipelineContext context);
}
