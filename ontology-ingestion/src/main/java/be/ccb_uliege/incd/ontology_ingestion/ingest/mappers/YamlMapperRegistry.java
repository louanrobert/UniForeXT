package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers;

import java.util.LinkedHashMap;
import java.util.Map;

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
     * Creates a factory by building mappers from the supplied configuration.
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
     * @return a fully initialised YamlMapperFactory
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
