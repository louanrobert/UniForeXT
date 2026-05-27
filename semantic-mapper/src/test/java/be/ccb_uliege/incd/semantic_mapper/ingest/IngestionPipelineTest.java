package be.ccb_uliege.incd.semantic_mapper.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.DefineIngestionTasksStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.ExecuteIngestionTasksStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.LoadMappersStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.ValidateShaclStage;

class IngestionPipelineTest {

    @Test
    void createDefaultStagesIncludesShaclValidationByDefault() {
        var stages = IngestionPipeline.createDefaultStages(false);

        assertEquals(4, stages.size());
        assertTrue(stages.get(0) instanceof LoadMappersStage);
        assertTrue(stages.get(1) instanceof DefineIngestionTasksStage);
        assertTrue(stages.get(2) instanceof ExecuteIngestionTasksStage);
        assertTrue(stages.get(3) instanceof ValidateShaclStage);
    }

    @Test
    void createDefaultStagesSkipsShaclValidationWhenRequested() {
        var stages = IngestionPipeline.createDefaultStages(true);

        assertEquals(3, stages.size());
        assertTrue(stages.get(0) instanceof LoadMappersStage);
        assertTrue(stages.get(1) instanceof DefineIngestionTasksStage);
        assertTrue(stages.get(2) instanceof ExecuteIngestionTasksStage);
        assertFalse(stages.stream().anyMatch(ValidateShaclStage.class::isInstance));
    }
}