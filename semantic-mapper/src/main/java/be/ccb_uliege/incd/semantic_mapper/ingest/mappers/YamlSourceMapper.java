package be.ccb_uliege.incd.semantic_mapper.ingest.mappers;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.XSD;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config.FieldMappingConfig;
import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config.MapperConfig;
import be.ccb_uliege.incd.semantic_mapper.ingest.mappers.config.StaticPropertyConfig;
import be.ccb_uliege.incd.semantic_mapper.owl.kg.KnowledgeGraphFacade;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * SourceMapper implementation that creates OWL individuals based on YAML configuration.
 * 
 * This class is responsible for taking a SourceRecord, extracting values based on the provided MapperConfig, and using those values to create and populate an OWL individual in the Jena Model.
 * It supports static properties, field mappings for data properties and linked individuals, and the use of generic mapping groups for reusable field mapping configurations.
 */
public class YamlSourceMapper implements SourceMapper {

    private static final String XSD_PREFIX = "xsd:";
    private static final DateTimeFormatter OUTPUT_DATE_TIME_STAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final List<DateTimeFormatter> LOCAL_DATE_TIME_INPUT_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

    private final MapperConfig config;
    private final Map<String, List<FieldMappingConfig>> genericMappings;
    private final KnowledgeGraphFacade knowledgeGraph;
    private static final Logger LOG = Logger.getLogger(YamlSourceMapper.class.getName());

    public YamlSourceMapper(MapperConfig config, Map<String, List<FieldMappingConfig>> genericMappings, KnowledgeGraphFacade knowledgeGraph) {
        this.config = config;
        this.genericMappings = genericMappings != null ? genericMappings : Collections.emptyMap();
        this.knowledgeGraph = knowledgeGraph;
    }

    /**
     * Returns the name of this mapper, as defined in the YAML configuration. This is used for logging and error messages to identify which mapper is being executed.
     */
    public String getName() {
        return config.getName();
    }

    /**
     * Resolves the file path from the MapperConfig, supporting wildcard patterns to match files in a directory.
     * If the file path does not contain a wildcard, it is returned as-is.
     */
    public Path getFilePath() throws IOException {
        String raw = config.getFile(); // String, not Path
        
        // Normalize slashes
        String normalized = raw.replace("\\", "/").replaceAll("/+", "/");
        
        int lastSlash = normalized.lastIndexOf('/');
        String dirPart      = normalized.substring(0, lastSlash);
        String filenamePart = normalized.substring(lastSlash + 1);

        if (!filenamePart.contains("*")) {
            return Path.of(normalized); // safe — no wildcard
        }

        Path dir = Path.of(dirPart); // safe — no wildcard in dir
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filenamePart)) {
            for (Path match : stream) {
                return match; // first match
            }
        }

        throw new IOException("No file matching '" + filenamePart + "' found in: " + dirPart);
    }

    /**
     * Returns the delimiter character for parsing the source file, as defined in the YAML configuration. This is used by the SourceIngester to correctly parse the input data according to the specified format.
     */
    public Character getDelimiter() {
        return config.getDelimiter();
    }

    /**
     * Maps a SourceRecord to an individual in the knowledge graph according to the configuration defined in the YAML file. This includes constructing the individual's identifier, setting static properties, applying generic mapping groups, and processing field mappings for both data properties and linked individuals.
     */
    @Override
    public void map(SourceRecord r) {
        try {
            String identifier = buildIdentifier(r);
            Resource individual = knowledgeGraph.createIndividual(config.getOwlClass(), identifier);

            applyStaticProperties(individual);
            applyGenerics(r, individual);
            applyFieldMappings(r, individual);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Mapper '" + getName() + "' failed for a record (id=" + r + "): " + e.getMessage(), e);
        }
    }

    private String buildIdentifier(SourceRecord r) {
        return config.getIdentifier().getFields().stream()
                .map(r::get)
                .collect(Collectors.joining(config.getIdentifier().getSeparator()));
    }

    private void applyStaticProperties(Resource individual) {
        if (config.getStaticProperties() == null) {
            return;
        }
        for (StaticPropertyConfig sp : config.getStaticProperties()) {
            applyStaticProperty(individual, sp);
        }
    }

    private void applyStaticProperty(Resource individual, StaticPropertyConfig sp) {
        RDFDatatype dataType = resolveDatatype(sp.getDataType());
        String normalizedValue = normalizeLiteralValue(sp.getValue(), dataType);
        knowledgeGraph.addDataProperty(individual, sp.getOwlProperty(),
                knowledgeGraph.createLiteral(normalizedValue, dataType));
    }

    private void applyGenerics(SourceRecord r, Resource individual) {
        if (config.getGenerics() == null) {
            return;
        }
        for (String generic : config.getGenerics()) {
            dispatchGeneric(generic, r, individual);
        }
    }

    private void applyFieldMappings(SourceRecord r, Resource individual) {
        if (config.getFieldMappings() == null) {
            return;
        }
        for (FieldMappingConfig fm : config.getFieldMappings()) {
            applyFieldMapping(fm, r, individual);
        }
    }

    private void dispatchGeneric(String name, SourceRecord r, Resource individual) {
        List<FieldMappingConfig> mappings = genericMappings.get(name);
        if (mappings == null) {
            throw new IllegalArgumentException("Unknown generic mapping: " + name);
        }
        for (FieldMappingConfig fm : mappings) {
            applyFieldMapping(fm, r, individual);
        }
    }

    private void applyFieldMapping(FieldMappingConfig fm, SourceRecord r, Resource parent) {
        switch (fm.getType()) {
            case "dataProperty" -> applyDataProperty(fm, r, parent);
            case "linkedIndividual" -> applyLinkedIndividual(fm, r, parent);
            default -> throw new IllegalArgumentException("Unknown mapping type: " + fm.getType());
        }
    }

    private void applyDataProperty(FieldMappingConfig fm, SourceRecord r, Resource parent) {
        if (!r.has(fm.getSourceField())) {
            return;
        }
        String value = fm.getPrefix() != null
                ? fm.getPrefix() + r.get(fm.getSourceField())
                : r.get(fm.getSourceField());
        RDFDatatype dataType = resolveDatatype(fm.getDataType());
        String normalizedValue = normalizeLiteralValue(value, dataType);

        if (fm.isUnique()) {
            knowledgeGraph.addUniqueDataProperty(parent, fm.getOwlProperty(),
                knowledgeGraph.createLiteral(normalizedValue, dataType));
        } else {
            knowledgeGraph.addDataProperty(parent, fm.getOwlProperty(),
                knowledgeGraph.createLiteral(normalizedValue, dataType));
        }
    }

    private RDFDatatype resolveDatatype(String configuredType) {
        if (configuredType == null || configuredType.isBlank()) {
            return XSDDatatype.XSDstring;
        }

        String normalizedType = configuredType.trim();
        String datatypeUri = normalizedType.startsWith(XSD_PREFIX)
                ? XSD.getURI() + normalizedType.substring(XSD_PREFIX.length())
                : normalizedType;

        RDFDatatype datatype = TypeMapper.getInstance().getTypeByName(datatypeUri);
        if (datatype == null) {
            throw new IllegalArgumentException("Unknown datatype: " + configuredType);
        }
        return datatype;
    }

    private String normalizeLiteralValue(String value, RDFDatatype datatype) {
        if (value == null || datatype == null) {
            return value;
        }
        if (XSDDatatype.XSDdateTimeStamp.getURI().equals(datatype.getURI())) {
            return normalizeDateTimeStamp(value);
        }
        return value;
    }

    private String normalizeDateTimeStamp(String rawValue) {
        String value = rawValue.trim();
        if (value.isEmpty()) {
            return value;
        }

        String result = tryParseOffsetDateTime(value);
        if (result != null) return result;

        result = tryParseZonedDateTime(value);
        if (result != null) return result;

        result = tryParseInstant(value);
        if (result != null) return result;

        result = tryParseLocalDateTime(value);
        if (result != null) return result;

        result = tryParseLocalDate(value);
        if (result != null) return result;

        throw new IllegalArgumentException("Could not normalize xsd:dateTimeStamp value: " + rawValue);
    }

    private String tryParseOffsetDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String tryParseZonedDateTime(String value) {
        try {
            return ZonedDateTime.parse(value).toOffsetDateTime().format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String tryParseInstant(String value) {
        try {
            return Instant.parse(value).atOffset(ZoneOffset.UTC).format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String tryParseLocalDateTime(String value) {
        for (DateTimeFormatter formatter : LOCAL_DATE_TIME_INPUT_FORMATTERS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
                return dateTime.atOffset(ZoneOffset.UTC).format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String tryParseLocalDate(String value) {
        try {
            LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return localDate.atStartOfDay().atOffset(ZoneOffset.UTC).format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private void applyLinkedIndividual(FieldMappingConfig fm, SourceRecord r, Resource parent) {
        if (!hasAllIdentifierFields(fm, r)) {
            return;
        }
        
        String id = resolveLinkedIndividualIdentifier(fm, r);
        Resource linked = knowledgeGraph.createIndividual(fm.getOwlClass(), id);
        
        applyDataPropertiesToLinked(fm, r, linked);
        applyNestedLinksToLinked(fm, r, linked);
        
        knowledgeGraph.addUniqueObjectProperty(parent, fm.getLinkProperty(), linked);
    }

    private boolean hasAllIdentifierFields(FieldMappingConfig fm, SourceRecord r) {
        return fm.getIdentifier().getFields().stream().allMatch(r::has);
    }

    private String resolveLinkedIndividualIdentifier(FieldMappingConfig fm, SourceRecord r) {
        if (fm.getIdentifier().isUseHash()) {
            return resolveHashedIdentifier(fm, r);
        }
        if (fm.getIdentifier().getFields().size() == 1) {
            return r.get(fm.getIdentifier().getFields().get(0));
        }
        return resolveCombinedIdentifier(fm, r);
    }

    private String resolveHashedIdentifier(FieldMappingConfig fm, SourceRecord r) {
        if (fm.getIdentifier().getFields().size() > 1) {
            throw new IllegalArgumentException("Hashing can only be used with a single identifier field");
        }
        return r.getHashed(fm.getIdentifier().getFields().get(0));
    }

    private String resolveCombinedIdentifier(FieldMappingConfig fm, SourceRecord r) {
        return fm.getIdentifier().getFields().stream()
                .map(r::get)
                .collect(Collectors.joining(fm.getIdentifier().getSeparator()));
    }

    private void applyDataPropertiesToLinked(FieldMappingConfig fm, SourceRecord r, Resource linked) {
        if (fm.getDataProperties() != null) {
            for (FieldMappingConfig dp : fm.getDataProperties()) {
                applyFieldMapping(dp, r, linked);
            }
        }
    }

    private void applyNestedLinksToLinked(FieldMappingConfig fm, SourceRecord r, Resource linked) {
        if (fm.getNestedLinks() != null) {
            for (FieldMappingConfig nl : fm.getNestedLinks()) {
                applyFieldMapping(nl, r, linked);
            }
        }
    }
}
