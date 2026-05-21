package be.ccb_uliege.incd.semantic_mapper.ingest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.ccb_uliege.incd.semantic_mapper.ingest.implementations.MultiFormatIngester;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.DefineIngestionTasksStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.ExecuteIngestionTasksStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.IngestionStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.LoadMappersStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.PipelineContext;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.ValidateShaclStage;
import be.ccb_uliege.incd.semantic_mapper.owl.kg.KnowledgeGraphFacade;

/**
 * This class orchestrates the ingestion process.
 * It defines the files to be ingested, the mappers to use for each file, and
 * executes the ingestion using the correct ingester implementation.
 * 
 * 
 */
public class IngestionPipeline {

    private final List<IngestionStage> stages;
    private static final Logger LOG = Logger.getLogger(IngestionPipeline.class.getName());

    public IngestionPipeline(List<IngestionStage> stages) {
        this.stages = List.copyOf(stages);
    }

    /**
     * Executes the ingestion pipeline by iterating through each stage and executing it with the provided PipelineContext. If any stage throws an exception, it is caught and logged as a warning, but the pipeline continues to execute the remaining stages. This allows for a more resilient ingestion process where one failing stage does not necessarily halt the entire pipeline. However, it is important to note that if a critical stage fails (e.g., loading mappers), subsequent stages may not function correctly, so careful consideration should be given to which stages are allowed to fail without stopping the pipeline.
     */
    public void run(PipelineContext context) throws Exception {
        for (IngestionStage stage : stages) {
            try {
                stage.execute(context);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Stage failed: " + stage.getClass().getSimpleName() + " - continuing with next stage", e);
            }
        }
    }

    /**
     * Static method to run the entire ingestion pipeline. This method initializes the pipeline with the necessary stages, creates a PipelineContext with the provided KnowledgeGraphFacade and a MultiFormatIngester, and then executes the pipeline. If any stage throws an exception, it is logged and re-thrown to indicate that the ingestion process failed.
     */
    public static void run(KnowledgeGraphFacade knowledgeGraph) throws Exception {
        IngestionPipeline pipeline = new IngestionPipeline(List.of(
                new LoadMappersStage(),
                new DefineIngestionTasksStage(),
                new ExecuteIngestionTasksStage(),
                new ValidateShaclStage(true)));  // Set to 'false' to continue on validation errors

        PipelineContext context = new PipelineContext(
                knowledgeGraph,
            new MultiFormatIngester(),
                "..\\ingestion-config");

        try {
            pipeline.run(context);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Ingestion pipeline failed: " + e.getMessage(), e);
            throw e;
        }
    }
}
