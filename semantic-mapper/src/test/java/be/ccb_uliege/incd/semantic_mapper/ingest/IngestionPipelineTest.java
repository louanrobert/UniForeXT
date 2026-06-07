package be.ccb_uliege.incd.semantic_mapper.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.DefineIngestionTasksStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.ExecuteIngestionTasksStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.IngestionStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.LoadMappersStage;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.PipelineContext;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.PipelineTestSupport;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.ValidateShaclStage;

class IngestionPipelineTest {

    @TempDir
    Path tempDirectory;

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

    @Test
    void runContinuesAfterNonCriticalStageFailure() throws Exception {
        AtomicInteger executedStages = new AtomicInteger();
        IngestionPipeline pipeline = new IngestionPipeline(java.util.List.of(
                new TestStage(() -> {
                    executedStages.incrementAndGet();
                    throw new Exception("recoverable");
                }),
                new TestStage(executedStages::incrementAndGet)));

        pipeline.run(newContext());

        assertEquals(2, executedStages.get());
    }

    @Test
    void runRethrowsIllegalStateExceptionAndStopsPipeline() {
        AtomicInteger executedStages = new AtomicInteger();
        IngestionPipeline pipeline = new IngestionPipeline(java.util.List.of(
                new TestStage(() -> {
                    executedStages.incrementAndGet();
                    throw new IllegalStateException("critical");
                }),
                new TestStage(executedStages::incrementAndGet)));

        assertThrowsWithPipelineLoggingOff(IllegalStateException.class, () -> pipeline.run(newContext()));
        assertEquals(1, executedStages.get());
    }

    @Test
    void staticRunExecutesConfiguredPipelineAndWrapsFailure() throws Exception {
        Path configDirectory = Files.createDirectory(tempDirectory.resolve("config"));
        Path dataFile = Files.writeString(tempDirectory.resolve("events.csv"), "id;name\n1;Alice\n");
        Files.writeString(configDirectory.resolve("mapper.yaml"), "mappers:\n"
                + "  - name: events\n"
                + "    owlClass: Event\n"
                + "    file: " + dataFile.toString().replace('\\', '/') + "\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings: []\n");

        IngestionPipeline.run(
                PipelineTestSupport.newKnowledgeGraphFacade(),
                true,
                configDirectory,
                tempDirectory.resolve("unused-shapes.ttl"));

        assertThrowsWithPipelineLoggingOff(
                Exception.class,
                () -> IngestionPipeline.run(
                        PipelineTestSupport.newKnowledgeGraphFacade(),
                        true,
                        tempDirectory.resolve("missing-config"),
                        tempDirectory.resolve("unused-shapes.ttl")));
    }

    private PipelineContext newContext() {
        return new PipelineContext(
                PipelineTestSupport.newKnowledgeGraphFacade(),
                (file, mapper, delimiter, progressListener) -> { },
                "config",
                "shapes");
    }

    private static <T extends Throwable> T assertThrowsWithPipelineLoggingOff(
            Class<T> expectedType,
            Executable executable) {
        Configurator.setLevel(IngestionPipeline.class.getName(), Level.OFF);
        try {
            return assertThrows(expectedType, executable);
        } finally {
            Configurator.setLevel(IngestionPipeline.class.getName(), Level.ERROR);
        }
    }

    private static class TestStage extends IngestionStage {
        private final ThrowingRunnable action;

        TestStage(ThrowingRunnable action) {
            this.action = action;
        }

        @Override
        public void execute(PipelineContext context) throws Exception {
            action.run();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}