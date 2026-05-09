package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.YamlSourceMapper;
import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.YamlMapperRegistry;

/**
 * Defines file-to-mapper ingestion tasks.
 */
public class DefineIngestionTasksStage extends IngestionStage {

    @Override
    public void execute(PipelineContext context) throws Exception {
        YamlMapperRegistry mapperRegistry = context.getMapperRegistry();
        if (mapperRegistry == null) {
            throw new IllegalStateException("Mapper registry must be loaded before defining ingestion tasks");
        }

        for (var mapper : mapperRegistry.getMappers().values()) {
            if (!(mapper instanceof YamlSourceMapper yamlSourceMapper)) {
                log("Skipping unsupported mapper implementation: " + mapper.getClass().getName());
                continue;
            }

            try {
                for (Path filePath : resolveTaskFiles(yamlSourceMapper.getFilePath())) {
                    IngestionTask task = new IngestionTask(
                            filePath,
                            yamlSourceMapper,
                            yamlSourceMapper.getDelimiter());
                    context.addTask(task);
                }
            } catch (Exception e) {
                log("Skipping mapper '" + yamlSourceMapper.getName() + "' due to error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    static List<Path> resolveTaskFiles(Path configuredPath) {
        String fileName = configuredPath.getFileName() == null ? configuredPath.toString() : configuredPath.getFileName().toString();
        if (!containsWildcard(fileName)) {
            return List.of(configuredPath);
        }

        Path parentDirectory = configuredPath.getParent() == null ? Path.of(".") : configuredPath.getParent();
        if (!Files.isDirectory(parentDirectory)) {
            throw new IllegalStateException("Wildcard mapper path parent directory not found: " + parentDirectory);
        }

        PathMatcher fileNameMatcher = parentDirectory.getFileSystem().getPathMatcher("glob:" + fileName);
        try (Stream<Path> files = Files.list(parentDirectory)) {
            List<Path> matches = files
                    .filter(Files::isRegularFile)
                    .filter(file -> fileNameMatcher.matches(file.getFileName()))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();

            if (matches.isEmpty()) {
                throw new IllegalStateException("No files match mapper wildcard path: " + configuredPath);
            }
            return matches;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve mapper wildcard path: " + configuredPath, e);
        }
    }

    private static boolean containsWildcard(String value) {
        return value.contains("*") || value.contains("?") || value.contains("[") || value.contains("{");
    }
}
