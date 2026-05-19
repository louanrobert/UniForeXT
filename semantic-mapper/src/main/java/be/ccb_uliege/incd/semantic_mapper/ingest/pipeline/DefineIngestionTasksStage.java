package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
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

    static List<Path> resolveTaskFiles(String rawPattern) {
        if (!containsWildcard(rawPattern)) {
            return List.of(Path.of(rawPattern)); // safe: no wildcard in string
        }

        // Split on / (we normalised slashes in getFilePath)
        String[] segments = rawPattern.split("/", -1);

        // Find first wildcard segment → everything before it is the search root
        int firstWild = segments.length;
        for (int i = 0; i < segments.length; i++) {
            if (containsWildcard(segments[i])) {
                firstWild = i;
                break;
            }
        }

        // Reconstruct the root string and turn it into a Path (safe: no wildcards)
        String rootStr = String.join("/", Arrays.copyOfRange(segments, 0, firstWild));
        Path searchRoot = rootStr.isEmpty() ? Path.of(".") : Path.of(rootStr);

        if (!Files.isDirectory(searchRoot)) {
            throw new IllegalStateException("Wildcard mapper path root directory not found: " + searchRoot);
        }

        // The glob is everything from the first wildcard segment onward
        String relativeGlob = String.join("/", Arrays.copyOfRange(segments, firstWild, segments.length));
        boolean recursive = relativeGlob.contains("**");
        int maxDepth = segments.length - firstWild; // won't over-walk the tree

        PathMatcher matcher = searchRoot.getFileSystem().getPathMatcher("glob:" + relativeGlob);

        try (Stream<Path> files = recursive
                ? Files.walk(searchRoot)
                : Files.walk(searchRoot, maxDepth)) {

            List<Path> matches = files
                    .filter(Files::isRegularFile)
                    .filter(file -> matcher.matches(searchRoot.relativize(file)))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();

            if (matches.isEmpty()) {
                throw new IllegalStateException("No files match mapper wildcard path: " + rawPattern);
            }
            return matches;

        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve mapper wildcard path: " + rawPattern, e);
        }
    }

    /**
     * Walks the path components and returns the longest prefix path that contains
     * no wildcard segment.
     * E.g. for /data/2024-* /reports/*.csv → returns /data
     */
    private static Path findSearchRoot(Path path) {
        Path current = path.isAbsolute() ? path.getRoot() : Path.of(".");
        for (int i = 0; i < path.getNameCount(); i++) {
            String segment = path.getName(i).toString();
            if (containsWildcard(segment)) {
                break;
            }
            current = current.resolve(segment);
        }
        return current;
    }

    private static boolean containsWildcard(String value) {
        return value.contains("*") || value.contains("?") || value.contains("[") || value.contains("{");
    }
}
