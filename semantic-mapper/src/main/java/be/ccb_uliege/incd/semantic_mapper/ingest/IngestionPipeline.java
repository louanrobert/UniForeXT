package be.ccb_uliege.incd.semantic_mapper.ingest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.ccb_uliege.incd.semantic_mapper.ingest.implementations.csv.CsvIngester;
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

    public void run(PipelineContext context) throws Exception {
        for (IngestionStage stage : stages) {
            try {
                stage.execute(context);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Stage failed: " + stage.getClass().getSimpleName() + " - continuing with next stage", e);
            }
        }
    }

    public static void run(KnowledgeGraphFacade knowledgeGraph) throws Exception {
        IngestionPipeline pipeline = new IngestionPipeline(List.of(
                new LoadMappersStage(),
                new DefineIngestionTasksStage(),
                new ExecuteIngestionTasksStage(),
                new ValidateShaclStage(true)));  // Set to 'false' to continue on validation errors

        PipelineContext context = new PipelineContext(
                knowledgeGraph,
                new CsvIngester(),
                "..\\ingestion-config");

        try {
            pipeline.run(context);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Ingestion pipeline failed: " + e.getMessage(), e);
            throw e;
        }
    }
}
