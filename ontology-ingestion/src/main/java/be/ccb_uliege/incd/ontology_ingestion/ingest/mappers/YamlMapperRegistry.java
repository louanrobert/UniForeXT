package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.MapperConfig;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.MapperConfigRegistry;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.MappersConfigLoader;
import be.ccb_uliege.incd.ontology_ingestion.owl.kg.KnowledgeGraphFacade;

// TODO: rename to SourceMapperRegistry because it is not a factory in the traditional sense of creating new instances on demand, but rather a registry of pre-built mappers.

/**
 * Factory that creates SourceMapper instances from a MappersConfig.
 *
 * This class is only responsible for building mapper instances from an
 * already-parsed configuration object.
 * Configuration loading is handled separately by MappersConfigLoader.
 */
public class YamlMapperRegistry {

    private final Map<String, SourceMapper> mappers = new LinkedHashMap<>();

    /**
     * Private constructor for building an empty registry.
     * Used by fromYamlDirectory to accumulate mappers from multiple files.
     */
    private YamlMapperRegistry() {
    }

    /**
     * Creates a registry by building mappers from the supplied configuration.
     *
     * @param config     the parsed mapper configuration
     * @param knowledgeGraph facade used for graph manipulation
     */
    public YamlMapperRegistry(MapperConfigRegistry config, KnowledgeGraphFacade knowledgeGraph) {
        for (MapperConfig mc : config.getMappers()) {
            YamlSourceMapper ysm = new YamlSourceMapper(mc, config.getGenericMappings(), knowledgeGraph);
            this.mappers.put(ysm.getName(), ysm);
        }
    }

    /**
     * Convenience factory that loads the configuration from a YAML file path
     * and builds the mappers in one step.
     *
     * @param yamlFilePath path to the YAML configuration file
     * @param knowledgeGraph facade used for graph manipulation
     * @return a fully initialised YamlMapperRegistry
     */
    public static YamlMapperRegistry fromYamlFile(String yamlFilePath, KnowledgeGraphFacade knowledgeGraph) {
        try {
            MapperConfigRegistry config = MappersConfigLoader.load(yamlFilePath);
            return new YamlMapperRegistry(config, knowledgeGraph);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mapper configuration from " + yamlFilePath, e);
        }
    }

    /**
     * Loads all YAML mapper configurations from a directory.
     * Each YAML file's generics are isolated and not shared across files.
     * All mappers from all files are accumulated into a single registry.
     *
     * @param yamlDirectoryPath path to the directory containing YAML configuration files
     * @param knowledgeGraph facade used for graph manipulation
     * @return a fully initialised YamlMapperRegistry with mappers from all YAML files
     */
    public static YamlMapperRegistry fromYamlDirectory(String yamlDirectoryPath, KnowledgeGraphFacade knowledgeGraph) {
        File directory = new File(yamlDirectoryPath);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Mapper configuration path is not a directory: " + yamlDirectoryPath);
        }

        YamlMapperRegistry combinedRegistry = new YamlMapperRegistry();
        
        try (Stream<Path> paths = Files.list(directory.toPath())) {
            List<Path> yamlFiles = paths
                    .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .sorted()
                    .toList();

            if (yamlFiles.isEmpty()) {
                throw new IllegalStateException("No YAML files found in directory: " + yamlDirectoryPath);
            }

            for (Path yamlFile : yamlFiles) {
                try {
                    MapperConfigRegistry config = MappersConfigLoader.load(yamlFile.toFile());
                    // Load mappers from this file with its own isolated generics
                    for (MapperConfig mc : config.getMappers()) {
                        YamlSourceMapper ysm = new YamlSourceMapper(mc, config.getGenericMappings(), knowledgeGraph);
                        combinedRegistry.mappers.put(ysm.getName(), ysm);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load mapper configuration from " + yamlFile, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read mapper configuration directory: " + yamlDirectoryPath, e);
        }

        return combinedRegistry;
    }

    /**
     * Retrieves a SourceMapper by name.
     * If no mapper with the given name exists, an IllegalArgumentException is
     * thrown.
     * 
     * @param name the name of the mapper to retrieve
     * @return the SourceMapper instance with the specified name
     * @throws IllegalArgumentException if no mapper with the given name is found
     */
    public SourceMapper getMapper(String name) throws IllegalArgumentException {
        SourceMapper mapper = mappers.get(name);
        if (mapper == null) {
            throw new IllegalArgumentException("No mapper found with name: " + name);
        }
        return mapper;
    }

    /**
     * Returns the mappers map, which contains all the SourceMapper instances
     * created by this factory, keyed by their names.
     * This allows external code to see what mappers are available without being
     * able to modify the factory's internal state.
     * 
     * @return an unmodifiable map of mapper names to SourceMapper instances
     */
    public Map<String, SourceMapper> getMappers() {
        return mappers;
    }
}
