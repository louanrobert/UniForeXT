package be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline;

/**
 * A single executable stage in the ingestion pipeline.
 */
public abstract class IngestionStage {
    /**
     * Executes the stage against the shared pipeline context.
     *
     * @param context shared state exchanged between pipeline stages
     */
    abstract public void execute(PipelineContext context) throws Exception;

    /**
     * Helper method to get a consistent log prefix for this stage.
     * Can be used in log messages to identify which stage they come from.
     */
    protected String getLogPrefix() {
        return "[" + getClass().getSimpleName() + "] ";
    }

    protected void log(String message) {
        String logMessage = getLogPrefix() + message;
        System.out.println(logMessage);
    }
}
