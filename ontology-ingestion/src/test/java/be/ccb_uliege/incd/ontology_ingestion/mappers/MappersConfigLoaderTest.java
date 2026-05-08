package be.ccb_uliege.incd.ontology_ingestion.mappers;

import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.MapperConfigRegistry;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.MappersConfigLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class MappersConfigLoaderTest {

    @Test
    void loadValidYamlParsesRegistry() throws IOException {
        String yaml = "genericMappings:\n"
            + "  common:\n"
            + "    - sourceField: id\n"
            + "      owlProperty: ex:id\n"
            + "mappers:\n"
            + "  - name: test-mapper\n"
            + "    owlClass: ex:Class\n"
            + "    file: data.csv\n"
            + "    delimiter: ','\n"
            + "    identifier:\n"
            + "      fields: id\n"
            + "      separator: '-'\n"
            + "      useHash: true\n"
            + "    generics: [common]\n"
            + "    staticProperties:\n"
            + "      - owlProperty: ex:p\n"
            + "        value: v\n"
            + "    fieldMappings:\n"
            + "      - sourceField: name\n"
            + "        owlProperty: ex:name\n";

        File tmp = Files.createTempFile("mappers-test", ".yml").toFile();
        try {
            Files.writeString(tmp.toPath(), yaml);

            MapperConfigRegistry registry = MappersConfigLoader.load(tmp);
            assertNotNull(registry, "Registry should be parsed");
            assertNotNull(registry.getMappers(), "Mappers list should not be null");
            assertEquals(1, registry.getMappers().size(), "There should be one mapper");

            var mapper = registry.getMappers().get(0);
            assertEquals("test-mapper", mapper.getName());
            assertEquals("ex:Class", mapper.getOwlClass());
            assertEquals("data.csv", mapper.getFile().getFileName().toString());
            assertEquals(Character.valueOf(','), mapper.getDelimiter());
            assertNotNull(mapper.getIdentifier());
            assertEquals("-", mapper.getIdentifier().getSeparator());
            assertTrue(mapper.getIdentifier().isUseHash());

            // Generic mapping loaded
            assertNotNull(registry.getGenericMappings());
            assertTrue(registry.getGenericMappings().containsKey("common"));
            assertFalse(registry.getGenericMappings().get("common").isEmpty());

            // Field mapping defaults
            var fm = mapper.getFieldMappings().get(0);
            assertEquals("xsd:string", fm.getDataType());
            assertTrue(fm.isUnique());

        } finally {
            tmp.delete();
        }
    }

    @Test
    void loadMissingFileThrows() {
        File missing = new File("non-existent-file-hopefully.yml");
        assertFalse(missing.exists());
        assertThrows(IllegalStateException.class, () -> MappersConfigLoader.load(missing));
    }
}
