package be.ccb_uliege.incd.semantic_mapper.ingest.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config.MapperConfigRegistry;
import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config.MappersConfigLoader;
import be.ccb_uliege.incd.semantic_mapper.owl.Loader;
import be.ccb_uliege.incd.semantic_mapper.owl.kg.KnowledgeGraphFacade;
import be.ccb_uliege.incd.semantic_mapper.owl.ontology.OntologyFacade;

class YamlSourceMapperTest {

    private static final String BASE = "http://example.test/ontology#";

    @Test
    void mapAppliesStaticGenericsTransformsValueMapDatesAndLinkedIndividuals() throws Exception {
        MapperConfigRegistry registry = loadYaml("genericMappings:\n"
                + "  common:\n"
                + "    - sourceField: severity\n"
                + "      type: dataProperty\n"
                + "      owlProperty: hasSeverity\n"
                + "      valueTransforms: [trim, lowercase]\n"
                + "      valueMap:\n"
                + "        high: High\n"
                + "      valueMapCaseInsensitive: true\n"
                + "mappers:\n"
                + "  - name: test-mapper\n"
                + "    owlClass: Event\n"
                + "    forensicTool: Chainsaw\n"
                + "    file: C:\\\\analysis\\\\events.csv\n"
                + "    delimiter: ','\n"
                + "    identifier:\n"
                + "      prefix: event-\n"
                + "      fields: [id, host]\n"
                + "      separator: '-'\n"
                + "    generics: [common]\n"
                + "    staticProperties:\n"
                + "      - owlProperty: hasSource\n"
                + "        value: fixture\n"
                + "    fieldMappings:\n"
                + "      - sourceField: timestamp\n"
                + "        type: dataProperty\n"
                + "        owlProperty: hasTimestamp\n"
                + "        dataType: xsd:dateTimeStamp\n"
                + "      - sourceField: missing\n"
                + "        type: dataProperty\n"
                + "        owlProperty: hasMessage\n"
                + "      - type: linkedIndividual\n"
                + "        owlClass: Computer\n"
                + "        linkProperty: hasComputer\n"
                + "        identifier:\n"
                + "          fields: host\n"
                + "        dataProperties:\n"
                + "          - sourceField: host\n"
                + "            type: dataProperty\n"
                + "            owlProperty: hasHostName\n"
                + "            prefix: 'host:'\n"
                + "        nestedLinks:\n"
                + "          - type: linkedIndividual\n"
                + "            owlClass: UserAccount\n"
                + "            linkProperty: hasUser\n"
                + "            identifier:\n"
                + "              fields: [domain, user]\n"
                + "              separator: /\n");
        KnowledgeGraphFacade graph = newKnowledgeGraphFacade();
        YamlSourceMapper mapper = new YamlSourceMapper(
                registry.getMappers().get(0),
                registry.getGenericMappings(),
                graph);

        assertEquals("test-mapper", mapper.getName());
        assertEquals("C:/analysis/events.csv", mapper.getFilePath());
        assertEquals(Character.valueOf(','), mapper.getDelimiter());

        mapper.map(new StubRecord(Map.of(
                "id", "42",
                "host", "WKST01",
                "severity", " HIGH ",
                "timestamp", "2026-06-07 12:30:45 +02:00",
                "domain", "ACME",
                "user", "alice")));

        Model model = graph.getModel();
        Resource event = model.getResource(Loader.getBase() + "Event:event-42-WKST01");
        Resource tool = model.getResource(Loader.getBase() + "ForensicTool:Chainsaw");
        Resource computer = model.getResource(Loader.getBase() + "Computer:WKST01");
        Resource user = model.getResource(Loader.getBase() + "UserAccount:ACME%2Falice");

        assertTrue(model.contains(event, RDF.type, ontologyResource("Event")));
        assertTrue(model.contains(tool, RDF.type, ontologyResource("ForensicTool")));
        assertTrue(model.contains(event, objectProperty("hasForensicTool"), tool));
        assertTrue(model.contains(event, objectProperty("hasComputer"), computer));
        assertTrue(model.contains(computer, objectProperty("isComputerOf"), event));
        assertTrue(model.contains(computer, objectProperty("hasUser"), user));

        assertLiteral(model, event, "hasSource", "fixture", XSDDatatype.XSDstring.getURI());
        assertLiteral(model, event, "hasSeverity", "High", XSDDatatype.XSDstring.getURI());
        assertLiteral(model, event, "hasTimestamp", "2026-06-07T12:30:45+02:00", XSDDatatype.XSDdateTimeStamp.getURI());
        assertLiteral(model, computer, "hasHostName", "host:WKST01", XSDDatatype.XSDstring.getURI());
    }

    @Test
    void mapRejectsUnknownGenericTransformDatatypeAndInvalidHashConfiguration() throws Exception {
        assertMappingThrows("mappers:\n"
                + "  - name: bad-generic\n"
                + "    owlClass: Event\n"
                + "    file: events.csv\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    generics: [missing]\n"
                + "    fieldMappings: []\n", "Unknown generic mapping");

        assertMappingThrows("mappers:\n"
                + "  - name: bad-transform\n"
                + "    owlClass: Event\n"
                + "    file: events.csv\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings:\n"
                + "      - sourceField: severity\n"
                + "        type: dataProperty\n"
                + "        owlProperty: hasSeverity\n"
                + "        valueTransforms: [reverse]\n", "Unknown value transform");

        assertMappingThrows("mappers:\n"
                + "  - name: bad-datatype\n"
                + "    owlClass: Event\n"
                + "    file: events.csv\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings:\n"
                + "      - sourceField: severity\n"
                + "        type: dataProperty\n"
                + "        owlProperty: hasSeverity\n"
                + "        dataType: xsd:doesNotExist\n", "Unknown datatype");

        assertMappingThrows("mappers:\n"
                + "  - name: bad-hash\n"
                + "    owlClass: Event\n"
                + "    file: events.csv\n"
                + "    identifier:\n"
                + "      fields: id\n"
                + "    fieldMappings:\n"
                + "      - type: linkedIndividual\n"
                + "        owlClass: UserAccount\n"
                + "        linkProperty: hasUser\n"
                + "        identifier:\n"
                + "          fields: [domain, user]\n"
                + "          useHash: true\n", "Hashing can only be used");
    }

    private static void assertMappingThrows(String yaml, String expectedMessagePart) throws Exception {
        MapperConfigRegistry registry = loadYaml(yaml);
        YamlSourceMapper mapper = new YamlSourceMapper(
                registry.getMappers().get(0),
                registry.getGenericMappings(),
                newKnowledgeGraphFacade());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.map(new StubRecord(Map.of(
                        "id", "42",
                        "severity", "HIGH",
                        "domain", "ACME",
                        "user", "alice"))));

        assertTrue(exception.getMessage().contains(expectedMessagePart));
    }

    private static MapperConfigRegistry loadYaml(String yaml) throws Exception {
        Path yamlFile = Files.createTempFile("mapper-test", ".yaml");
        try {
            Files.writeString(yamlFile, yaml);
            return MappersConfigLoader.load(yamlFile.toFile());
        } finally {
            Files.deleteIfExists(yamlFile);
        }
    }

    private static KnowledgeGraphFacade newKnowledgeGraphFacade() {
        Loader loader = new Loader(BASE);
        return new KnowledgeGraphFacade(ModelFactory.createDefaultModel(), new OntologyFacade(createOntologyModel()));
    }

    private static Model createOntologyModel() {
        Model ontology = ModelFactory.createDefaultModel();
        List.of("Event", "Computer", "UserAccount", "ForensicTool")
                .forEach(className -> ontology.add(ontologyResource(className), RDF.type, OWL.Class));
        List.of("hasSource", "hasSeverity", "hasTimestamp", "hasMessage", "hasHostName")
                .forEach(propertyName -> ontology.add(dataProperty(propertyName), RDF.type, OWL.DatatypeProperty));
        List.of("hasForensicTool", "hasComputer", "isComputerOf", "hasUser")
                .forEach(propertyName -> ontology.add(objectProperty(propertyName), RDF.type, OWL.ObjectProperty));
        ontology.add(objectProperty("hasComputer"), OWL.inverseOf, objectProperty("isComputerOf"));
        return ontology;
    }

    private static Resource ontologyResource(String localName) {
        return ModelFactory.createDefaultModel().createResource(BASE + localName);
    }

    private static Property dataProperty(String localName) {
        return ModelFactory.createDefaultModel().createProperty(BASE + localName);
    }

    private static Property objectProperty(String localName) {
        return ModelFactory.createDefaultModel().createProperty(BASE + localName);
    }

    private static void assertLiteral(Model model, Resource subject, String propertyName, String expectedValue, String datatypeUri) {
        Literal literal = model.getProperty(subject, dataProperty(propertyName)).getObject().asLiteral();
        assertEquals(expectedValue, literal.getLexicalForm());
        assertEquals(datatypeUri, literal.getDatatypeURI());
    }

    private record StubRecord(Map<String, String> values) implements SourceRecord {
        private StubRecord {
            values = new LinkedHashMap<>(values);
        }

        @Override
        public String get(String field) {
            return values.get(field);
        }

        @Override
        public String getHashed(String field) {
            return "hashed-" + values.get(field);
        }

        @Override
        public boolean has(String field) {
            String value = values.get(field);
            return value != null && !value.isBlank();
        }
    }
}
