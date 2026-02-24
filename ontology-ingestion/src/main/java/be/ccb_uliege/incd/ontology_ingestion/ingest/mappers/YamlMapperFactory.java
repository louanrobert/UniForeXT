package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.MappersConfig;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

/*
 * Factory to load SourceMappers from YAML configuration file.
 * 
 * This class is reponsible for reading the YAML file, parsing it into configuration objects, and instantiating YamlSourceMapper instances based on that configuration. 
 * It maintains a map of mapper names to SourceMapper instances for easy retrieval.
 */
public class YamlMapperFactory {

    private final Map<String, SourceMapper> mappers;

    public YamlMapperFactory(Classes classes, Properties properties) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        File yamlFile = new File("chainsaw-mappers.yaml");
        try {
            if (!yamlFile.exists()) {
                throw new IllegalStateException("chainsaw-mappers.yaml not found in project root: " + yamlFile.getAbsolutePath());
            }
            MappersConfig config = objectMapper.readValue(yamlFile, MappersConfig.class);
            this.mappers = new LinkedHashMap<>();
            for (var mc : config.getMappers()) {
                YamlSourceMapper ysm = new YamlSourceMapper(mc, config.getGenericMappings(), classes, properties);
                this.mappers.put(ysm.getName(), ysm);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mapper configuration from chainsaw-mappers.yaml", e);
        }
    }

    public SourceMapper getMapper(String name) {
        SourceMapper mapper = mappers.get(name);
        if (mapper == null) {
            throw new IllegalArgumentException("No mapper found with name: " + name);
        }
        return mapper;
    }

    public Map<String, SourceMapper> getMappers() {
        return mappers;
    }
}
