package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceRecord;

class PipelineContextTest {

    @Test
    void constructorRejectsNullRequiredDependencies() {
        SourceIngester ingester = noOpIngester();
        var graph = PipelineTestSupport.newKnowledgeGraphFacade();

        assertThrows(NullPointerException.class, () -> new PipelineContext(null, ingester, "config", "shapes"));
        assertThrows(NullPointerException.class, () -> new PipelineContext(graph, null, "config", "shapes"));
        assertThrows(NullPointerException.class, () -> new PipelineContext(graph, ingester, null, "shapes"));
        assertThrows(NullPointerException.class, () -> new PipelineContext(graph, ingester, "config", null));
    }

    @Test
    void addTaskStoresTasksInUnmodifiableViewAndRejectsNull() {
        PipelineContext context = newContext();
        IngestionTask task = new IngestionTask(Path.of("events.csv"), record -> { }, ',');

        context.addTask(task);

        assertEquals(1, context.getIngestionTasks().size());
        assertSame(task, context.getIngestionTasks().get(0));
        assertThrows(UnsupportedOperationException.class, () -> context.getIngestionTasks().add(task));
        assertThrows(NullPointerException.class, () -> context.addTask(null));
    }

    @Test
    void setMapperRegistryRejectsNull() {
        PipelineContext context = newContext();

        assertThrows(NullPointerException.class, () -> context.setMapperRegistry(null));
    }

    private static PipelineContext newContext() {
        return new PipelineContext(
                PipelineTestSupport.newKnowledgeGraphFacade(),
                noOpIngester(),
                "config",
                "shapes");
    }

    private static SourceIngester noOpIngester() {
        return (file, mapper, delimiter, progressListener) -> { };
    }
}
