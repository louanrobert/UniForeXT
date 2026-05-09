package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class DefineIngestionTasksStageTest {

    @Test
    void resolveTaskFilesReturnsConfiguredPathWhenNoWildcard() {
        Path configuredPath = Path.of("data", "wks02-haya.csv");

        List<Path> resolvedPaths = DefineIngestionTasksStage.resolveTaskFiles(configuredPath);

        assertEquals(1, resolvedPaths.size());
        assertEquals(configuredPath, resolvedPaths.get(0));
    }

    @Test
    void resolveTaskFilesExpandsWildcardFileName() throws IOException {
        Path tempDirectory = Files.createTempDirectory("ingestion-wildcard");
        try {
            Path matchingOne = Files.createFile(tempDirectory.resolve("wks02-haya.csv"));
            Path matchingTwo = Files.createFile(tempDirectory.resolve("abc-haya.csv"));
            Files.createFile(tempDirectory.resolve("not-matching.csv"));

            List<Path> resolvedPaths = DefineIngestionTasksStage.resolveTaskFiles(tempDirectory.resolve("*-haya.csv"));

            assertEquals(List.of(matchingTwo, matchingOne), resolvedPaths);
        } finally {
            deleteRecursively(tempDirectory);
        }
    }

    @Test
    void resolveTaskFilesThrowsWhenWildcardHasNoMatch() throws IOException {
        Path tempDirectory = Files.createTempDirectory("ingestion-wildcard-empty");
        try {
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> DefineIngestionTasksStage.resolveTaskFiles(tempDirectory.resolve("*-haya.csv")));

            assertEquals(true, exception.getMessage().contains("No files match mapper wildcard path"));
        } finally {
            deleteRecursively(tempDirectory);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            files.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }
}
