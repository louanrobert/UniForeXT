package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import java.nio.file.Path;
import java.util.List;

/**
 * Executes ingestion tasks accumulated in the pipeline context.
 */
public class ExecuteIngestionTasksStage extends IngestionStage {

    private int lastRenderedLength;

    @Override
    public void execute(PipelineContext context) {
        List<IngestionTask> tasks = context.getIngestionTasks();

        if (tasks.isEmpty()) {
            log("No ingestion tasks to execute.");
            return;
        }

        for (IngestionTask task : tasks) {
            log("Ingesting file: " + task.file().getFileName());
            lastRenderedLength = 0;
            try {
                context.getSourceIngester().ingest(
                        task.file(),
                        task.mapper(),
                        task.delimiter(),
                        (completedRecords, totalRecords) -> renderProgress(task.file(), completedRecords,
                                totalRecords));
            } catch (Exception e) {
                log("Error ingesting file '" + task.file() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void renderProgress(Path currentFile, int completedRecords, int totalRecords) {
        final int barWidth = 28;
        int safeTotal = Math.max(totalRecords, 1); // no division by zero
        // never negative
        int filledWidth = Math.min(completedRecords * barWidth / safeTotal, barWidth);
        int percent = completedRecords * 100 / safeTotal;

        String message = String.format("[%s%s] %3d%% (%d/%d) %s",
                "#".repeat(filledWidth),
                "-".repeat(barWidth - filledWidth),
                percent, completedRecords, totalRecords,
                currentFile.getFileName());

        // Pad with spaces to clear leftover characters from previous render
        int padding = Math.max(0, lastRenderedLength - message.length());
        System.out.print("\r" + message + " ".repeat(padding));
        System.out.flush();
        lastRenderedLength = message.length();

        // Clear the line when complete to avoid leftover progress bar
        if (completedRecords >= totalRecords) {
            System.out.print("\r" + " ".repeat(lastRenderedLength) + "\r");
            System.out.flush();
            lastRenderedLength = 0;
        }
    }
}
