package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoadMappersStageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void executeLoadsMapperRegistryFromConfigDirectory() throws Exception {
        Files.writeString(tempDirectory.resolve("mapper.yaml"), "mappers:\n"
                + "  - name: events\n"
                + "    owlClass: Event\n"
                + "    file: events.csv\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings: []\n");
        PipelineContext context = new PipelineContext(
                PipelineTestSupport.newKnowledgeGraphFacade(),
                (file, mapper, delimiter, progressListener) -> { },
                tempDirectory.toString(),
                "shapes");

        new LoadMappersStage().execute(context);

        assertNotNull(context.getMapperRegistry());
        assertTrue(context.getMapperRegistry().getMappers().containsKey("events"));
    }

    @Test
    void executePropagatesMissingConfigDirectory() {
        PipelineContext context = new PipelineContext(
                PipelineTestSupport.newKnowledgeGraphFacade(),
                (file, mapper, delimiter, progressListener) -> { },
                tempDirectory.resolve("missing").toString(),
                "shapes");

        assertThrows(IllegalArgumentException.class, () -> new LoadMappersStage().execute(context));
    }
}
