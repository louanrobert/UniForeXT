package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

/**
 * Executes ingestion tasks accumulated in the pipeline context.
 */
public class ExecuteIngestionTasksStage extends IngestionStage {

    @Override
    public void execute(PipelineContext context) {
        for (IngestionTask task : context.getIngestionTasks()) {
            log("Ingesting file: " + task.file());
            try {
                context.getSourceIngester().ingest(task.file(), task.mapper(), task.delimiter());
            } catch (Exception e) {
                log("Error ingesting file '" + task.file() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
