package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;

class ExecuteIngestionTasksStageTest {

    @Test
    void executeLogsWhenThereAreNoTasks() {
        ByteArrayOutputStream output = captureStdOut(() -> new ExecuteIngestionTasksStage().execute(newContext(new RecordingIngester())));

        assertTrue(output.toString().contains("No ingestion tasks to execute."));
    }

    @Test
    void executeRunsAllTasksAndRendersKnownAndUnknownProgress() {
        RecordingIngester ingester = new RecordingIngester();
        PipelineContext context = newContext(ingester);
        SourceMapper mapper = record -> { };
        context.addTask(new IngestionTask(Path.of("first.csv"), mapper, ','));
        context.addTask(new IngestionTask(Path.of("second.csv"), mapper, ';'));

        ByteArrayOutputStream output = captureStdOut(() -> new ExecuteIngestionTasksStage().execute(context));

        assertEquals(List.of(Path.of("first.csv"), Path.of("second.csv")), ingester.files);
        assertEquals(List.of(',', ';'), ingester.delimiters);
        assertTrue(output.toString().contains("Ingesting file: first.csv"));
        assertTrue(output.toString().contains("processed: 3 second.csv"));
    }

    @Test
    void executeContinuesWhenIngesterThrows() {
        RecordingIngester ingester = new RecordingIngester();
        ingester.throwOnFirstCall = true;
        PipelineContext context = newContext(ingester);
        SourceMapper mapper = record -> { };
        context.addTask(new IngestionTask(Path.of("first.csv"), mapper, ','));
        context.addTask(new IngestionTask(Path.of("second.csv"), mapper, ','));

        ByteArrayOutputStream output = captureStdOut(() -> new ExecuteIngestionTasksStage().execute(context));

        assertEquals(List.of(Path.of("first.csv"), Path.of("second.csv")), ingester.files);
        assertTrue(output.toString().contains("Error ingesting file 'first.csv'"));
    }

    private static PipelineContext newContext(SourceIngester ingester) {
        return new PipelineContext(
                PipelineTestSupport.newKnowledgeGraphFacade(),
                ingester,
                "config",
                "shapes");
    }

    private static ByteArrayOutputStream captureStdOut(Runnable action) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(output);
        System.setOut(capture);
        System.setErr(capture);
        try {
            action.run();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return output;
    }

    private static class RecordingIngester implements SourceIngester {
        private final List<Path> files = new ArrayList<>();
        private final List<Character> delimiters = new ArrayList<>();
        private boolean throwOnFirstCall;

        @Override
        public void ingest(Path file, SourceMapper mapper, Character delimiter,
                BiConsumer<Integer, Integer> progressListener) {
            files.add(file);
            delimiters.add(delimiter);
            if (throwOnFirstCall && files.size() == 1) {
                throw new IllegalStateException("simulated ingestion failure");
            }
            if (files.size() == 1) {
                progressListener.accept(0, 0);
                progressListener.accept(5, 10);
                progressListener.accept(10, 10);
            } else {
                progressListener.accept(3, -1);
            }
        }
    }
}
