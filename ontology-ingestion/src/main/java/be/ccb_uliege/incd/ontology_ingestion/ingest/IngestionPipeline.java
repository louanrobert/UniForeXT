package be.ccb_uliege.incd.ontology_ingestion.ingest;

import java.util.List;

import be.ccb_uliege.incd.ontology_ingestion.ingest.implementations.csv.CsvIngester;
import be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline.DefineIngestionTasksStage;
import be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline.ExecuteIngestionTasksStage;
import be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline.IngestionStage;
import be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline.LoadMappersStage;
import be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline.PipelineContext;
import be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline.ValidateShaclStage;
import be.ccb_uliege.incd.ontology_ingestion.owl.kg.KnowledgeGraphFacade;

/**
 * This class orchestrates the ingestion process.
 * It defines the files to be ingested, the mappers to use for each file, and
 * executes the ingestion using the correct ingester implementation.
 * 
 * 
 */
public class IngestionPipeline {

    private final List<IngestionStage> stages;

    public IngestionPipeline(List<IngestionStage> stages) {
        this.stages = List.copyOf(stages);
    }

    public void run(PipelineContext context) throws Exception {
        for (IngestionStage stage : stages) {
            stage.execute(context);
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
                "chainsaw-mappers.yaml");

        pipeline.run(context);
    }
}
