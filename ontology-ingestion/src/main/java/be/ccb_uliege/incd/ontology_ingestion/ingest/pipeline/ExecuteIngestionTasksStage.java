package be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline;

/**
 * Executes ingestion tasks accumulated in the pipeline context.
 */
public class ExecuteIngestionTasksStage extends IngestionStage {

    @Override
    public void execute(PipelineContext context) {
        for (IngestionTask task : context.getIngestionTasks()) {
            log("Ingesting file: " + task.file());
            context.getSourceIngester().ingest(task.file(), task.mapper(), task.delimiter());
        }
    }
}
