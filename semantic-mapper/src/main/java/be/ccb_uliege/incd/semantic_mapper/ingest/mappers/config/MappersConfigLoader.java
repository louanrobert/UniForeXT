package be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern ENVIRONMENT_VARIABLE_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Loads a MappersConfig from the given YAML file.
     *
     * @param yamlFile the YAML configuration file to read
     * @return the parsed configuration
     * @throws IllegalStateException if the file does not exist
     * @throws IOException           if the file cannot be read or parsed
     */
    public static MapperConfigRegistry load(File yamlFile) throws IOException {
        return load(yamlFile, System.getenv());
    }

    /**
     * Loads a MappersConfig from the given YAML file using the provided environment variables.
     *
     * @param yamlFile the YAML configuration file to read
     * @param environment environment variables available for placeholder substitution
     * @return the parsed configuration
     * @throws IllegalStateException if the file does not exist or a placeholder cannot be resolved
     * @throws IOException           if the file cannot be read or parsed
     */
    public static MapperConfigRegistry load(File yamlFile, Map<String, String> environment) throws IOException {
        if (!yamlFile.exists()) {
            throw new IllegalStateException(
                "Mapper configuration file not found: " + yamlFile.getAbsolutePath());
        }
        String yamlContent = Files.readString(yamlFile.toPath(), StandardCharsets.UTF_8);
        String resolvedYamlContent = resolveEnvironmentVariables(yamlContent, environment);
        return YAML_MAPPER.readValue(resolvedYamlContent, MapperConfigRegistry.class);
    }

    /**
     * Convenience overload that accepts a file path string.
     *
     * @param yamlFilePath path to the YAML configuration file
     * @return the parsed configuration
     * @throws IOException if the file cannot be read or parsed
     */
    public static MapperConfigRegistry load(String yamlFilePath) throws IOException {
        return load(new File(yamlFilePath));
    }

    static String resolveEnvironmentVariables(String content, Map<String, String> environment) {
        Matcher matcher = ENVIRONMENT_VARIABLE_PATTERN.matcher(content);
        StringBuffer resolvedContent = new StringBuffer();
        Set<String> missingVariables = new LinkedHashSet<>();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String variableValue = environment.get(variableName);
            if (variableValue == null) {
                missingVariables.add(variableName);
                variableValue = matcher.group(0);
            }
            matcher.appendReplacement(resolvedContent, Matcher.quoteReplacement(variableValue));
        }
        matcher.appendTail(resolvedContent);

        if (!missingVariables.isEmpty()) {
            throw new IllegalStateException(
                "Missing environment variable(s) in mapper configuration: " + String.join(", ", missingVariables));
        }

        return resolvedContent.toString();
    }
}
