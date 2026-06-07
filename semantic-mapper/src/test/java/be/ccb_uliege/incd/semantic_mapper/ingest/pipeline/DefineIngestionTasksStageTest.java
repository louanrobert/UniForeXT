package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.YamlMapperRegistry;

class DefineIngestionTasksStageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void resolveTaskFilesReturnsConfiguredPathWhenNoWildcard() {
        Path configuredPath = Path.of("data", "wks02-haya.csv");

        List<Path> resolvedPaths = DefineIngestionTasksStage.resolveTaskFiles(configuredPath.toString());

        assertEquals(List.of(configuredPath), resolvedPaths);
    }

    @Test
    void resolveTaskFilesExpandsWildcardFileNameInSortedOrder() throws Exception {
        Path matchingOne = Files.createFile(tempDirectory.resolve("wks02-haya.csv"));
        Path matchingTwo = Files.createFile(tempDirectory.resolve("abc-haya.csv"));
        Files.createFile(tempDirectory.resolve("not-matching.csv"));

        List<Path> resolvedPaths = DefineIngestionTasksStage.resolveTaskFiles(
                tempDirectory.toString().replace('\\', '/') + "/*-haya.csv");

        assertEquals(List.of(matchingTwo, matchingOne), resolvedPaths);
    }

    @Test
    void resolveTaskFilesExpandsRecursiveWildcard() throws Exception {
        Path nestedDirectory = Files.createDirectories(tempDirectory.resolve("nested"));
        Path nestedMatch = Files.createFile(nestedDirectory.resolve("event.csv"));

        List<Path> resolvedPaths = DefineIngestionTasksStage.resolveTaskFiles(
                tempDirectory.toString().replace('\\', '/') + "/**/*.csv");

        assertEquals(List.of(nestedMatch), resolvedPaths);
    }

    @Test
    void resolveTaskFilesThrowsWhenWildcardRootIsMissingOrNoFilesMatch() throws Exception {
        IllegalStateException missingRoot = assertThrows(
                IllegalStateException.class,
                () -> DefineIngestionTasksStage.resolveTaskFiles(
                        tempDirectory.resolve("missing").toString().replace('\\', '/') + "/*.csv"));
        assertTrue(missingRoot.getMessage().contains("root directory not found"));

        IllegalStateException noMatches = assertThrows(
                IllegalStateException.class,
                () -> DefineIngestionTasksStage.resolveTaskFiles(
                        tempDirectory.toString().replace('\\', '/') + "/*.evtx"));
        assertTrue(noMatches.getMessage().contains("No files match mapper wildcard path"));
    }

    @Test
    void executeCreatesTasksForResolvedYamlMappersAndSkipsMissingWildcards() throws Exception {
        Path eventOne = Files.createFile(tempDirectory.resolve("a.csv"));
        Path eventTwo = Files.createFile(tempDirectory.resolve("b.csv"));
        Path mapperDirectory = Files.createDirectory(tempDirectory.resolve("mappers"));
        Files.writeString(mapperDirectory.resolve("events.yaml"), "mappers:\n"
                + "  - name: events\n"
                + "    owlClass: Event\n"
                + "    file: " + tempDirectory.toString().replace('\\', '/') + "/*.csv\n"
                + "    delimiter: ','\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings: []\n"
                + "  - name: missing\n"
                + "    owlClass: Event\n"
                + "    file: " + tempDirectory.toString().replace('\\', '/') + "/*.missing\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings: []\n");
        PipelineContext context = new PipelineContext(
                PipelineTestSupport.newKnowledgeGraphFacade(),
                (file, mapper, delimiter, progressListener) -> { },
                mapperDirectory.toString(),
                "shapes");
        context.setMapperRegistry(YamlMapperRegistry.fromYamlDirectory(mapperDirectory.toString(), context.getKnowledgeGraph()));

        new DefineIngestionTasksStage().execute(context);

        assertEquals(2, context.getIngestionTasks().size());
        assertEquals(eventOne, context.getIngestionTasks().get(0).file());
        assertEquals(eventTwo, context.getIngestionTasks().get(1).file());
        assertEquals(Character.valueOf(','), context.getIngestionTasks().get(0).delimiter());
    }

    @Test
    void executeRequiresMapperRegistry() {
        PipelineContext context = new PipelineContext(
                PipelineTestSupport.newKnowledgeGraphFacade(),
                (file, mapper, delimiter, progressListener) -> { },
                "config",
                "shapes");

        assertThrows(IllegalStateException.class, () -> new DefineIngestionTasksStage().execute(context));
    }
}
