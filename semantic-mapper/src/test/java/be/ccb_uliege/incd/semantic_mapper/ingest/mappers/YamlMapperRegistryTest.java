package be.ccb_uliege.incd.semantic_mapper.ingest.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config.MapperConfigRegistry;
import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config.MappersConfigLoader;
import be.ccb_uliege.incd.semantic_mapper.ingest.pipeline.PipelineTestSupport;

class YamlMapperRegistryTest {

    @TempDir
    Path tempDirectory;

    @Test
    void constructorBuildsMappersAndGetMapperReturnsByName() throws Exception {
        MapperConfigRegistry config = loadYaml("mappers:\n"
                + "  - name: events\n"
                + "    owlClass: Event\n"
                + "    file: events.csv\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings: []\n");

        YamlMapperRegistry registry = new YamlMapperRegistry(config, PipelineTestSupport.newKnowledgeGraphFacade());

        SourceMapper mapper = registry.getMapper("events");
        assertSame(mapper, registry.getMappers().get("events"));
        assertThrows(IllegalArgumentException.class, () -> registry.getMapper("missing"));
    }

    @Test
    void fromYamlFileLoadsSingleConfigurationAndWrapsFailures() throws Exception {
        Path mapperFile = writeYaml("events.yaml", "mappers:\n"
                + "  - name: events\n"
                + "    owlClass: Event\n"
                + "    file: events.csv\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings: []\n");

        YamlMapperRegistry registry = YamlMapperRegistry.fromYamlFile(
                mapperFile.toString(),
                PipelineTestSupport.newKnowledgeGraphFacade());

        assertTrue(registry.getMappers().containsKey("events"));
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> YamlMapperRegistry.fromYamlFile(
                        tempDirectory.resolve("missing.yaml").toString(),
                        PipelineTestSupport.newKnowledgeGraphFacade()));
        assertTrue(exception.getMessage().contains("Failed to load mapper configuration"));
    }

    @Test
    void fromYamlDirectoryLoadsYamlAndYmlFilesInSortedOrderOnly() throws Exception {
        Files.writeString(tempDirectory.resolve("ignored.txt"), "not yaml");
        writeYaml("b.yml", "mappers:\n"
                + "  - name: second\n"
                + "    owlClass: Event\n"
                + "    file: second.csv\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings: []\n");
        writeYaml("a.yaml", "mappers:\n"
                + "  - name: first\n"
                + "    owlClass: Event\n"
                + "    file: first.csv\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings: []\n");

        YamlMapperRegistry registry = YamlMapperRegistry.fromYamlDirectory(
                tempDirectory.toString(),
                PipelineTestSupport.newKnowledgeGraphFacade());

        assertEquals("[first, second]", registry.getMappers().keySet().toString());
    }

    @Test
    void fromYamlDirectoryRejectsInvalidDirectoryEmptyDirectoryAndUnreadableYaml() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> YamlMapperRegistry.fromYamlDirectory(
                        tempDirectory.resolve("missing").toString(),
                        PipelineTestSupport.newKnowledgeGraphFacade()));

        Path emptyDirectory = Files.createDirectory(tempDirectory.resolve("empty"));
        assertThrows(
                IllegalStateException.class,
                () -> YamlMapperRegistry.fromYamlDirectory(
                        emptyDirectory.toString(),
                        PipelineTestSupport.newKnowledgeGraphFacade()));

        Path badDirectory = Files.createDirectory(tempDirectory.resolve("bad"));
        Files.writeString(badDirectory.resolve("bad.yaml"), "mappers: [not-valid-for-this-model");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> YamlMapperRegistry.fromYamlDirectory(
                        badDirectory.toString(),
                        PipelineTestSupport.newKnowledgeGraphFacade()));
        assertTrue(exception.getMessage().contains("Failed to load mapper configuration"));
    }

    private MapperConfigRegistry loadYaml(String yaml) throws Exception {
        Path yamlFile = writeYaml("mapper.yaml", yaml);
        return MappersConfigLoader.load(yamlFile.toFile());
    }

    private Path writeYaml(String fileName, String yaml) throws Exception {
        Path yamlFile = tempDirectory.resolve(fileName);
        Files.writeString(yamlFile, yaml);
        return yamlFile;
    }
}
