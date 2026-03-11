package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Responsible for loading and deserializing a YAML mapper configuration file
 * into a MappersConfig object.
 *
 * This class isolates file I/O and YAML parsing from the mapper creation logic,
 * making both easier to test and reuse independently.
 */
public class MappersConfigLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Loads a MappersConfig from the given YAML file.
     *
     * @param yamlFile the YAML configuration file to read
     * @return the parsed configuration
     * @throws IllegalStateException if the file does not exist
     * @throws IOException           if the file cannot be read or parsed
     */
    public static MappersConfig load(File yamlFile) throws IOException {
        if (!yamlFile.exists()) {
            throw new IllegalStateException(
                "Mapper configuration file not found: " + yamlFile.getAbsolutePath());
        }
        return YAML_MAPPER.readValue(yamlFile, MappersConfig.class);
    }

    /**
     * Convenience overload that accepts a file path string.
     *
     * @param yamlFilePath path to the YAML configuration file
     * @return the parsed configuration
     * @throws IOException if the file cannot be read or parsed
     */
    public static MappersConfig load(String yamlFilePath) throws IOException {
        return load(new File(yamlFilePath));
    }
}
