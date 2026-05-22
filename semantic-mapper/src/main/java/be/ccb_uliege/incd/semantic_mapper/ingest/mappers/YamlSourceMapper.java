package be.ccb_uliege.incd.semantic_mapper.ingest.mappers;

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
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss.SSSSS"));

    private final MapperConfig config;
    private final Map<String, List<FieldMappingConfig>> genericMappings;
    private final KnowledgeGraphFacade knowledgeGraph;
    private final Resource forensicTool;
    private static final Logger LOG = Logger.getLogger(YamlSourceMapper.class.getName());

    public YamlSourceMapper(MapperConfig config, Map<String, List<FieldMappingConfig>> genericMappings, KnowledgeGraphFacade knowledgeGraph) {
        this.config = config;
        this.genericMappings = genericMappings != null ? genericMappings : Collections.emptyMap();
        this.knowledgeGraph = knowledgeGraph;
        this.forensicTool = createForensicTool();
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
    public String getFilePath() {
        String raw = config.getFile();
        // Normalize to forward slashes; resolveTaskFiles handles the rest
        return raw.replace("\\", "/").replaceAll("/+", "/");
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

            addForensicTool(individual);
            applyStaticProperties(individual);
            applyGenerics(r, individual);
            applyFieldMappings(r, individual);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Mapper '" + getName() + "' failed for a record (id=" + r + "): " + e.getMessage(), e);
        }
    }

    /**
     * Builds the identifier for the individual based on the fields specified in the configuration. The identifier is constructed by concatenating the values of the specified fields, separated by the configured separator. If any of the required fields are missing from the SourceRecord, an exception will be thrown.
     */
    private String buildIdentifier(SourceRecord r) {
        return config.getIdentifier().getPrefix() + config.getIdentifier().getFields().stream()
                .map(r::get)
                .collect(Collectors.joining(config.getIdentifier().getSeparator()));
    }

    /**
     * Applies static properties to the individual as defined in the YAML configuration. Static properties are those that have a fixed value specified in the configuration, rather than being derived from the input data. This method iterates over the list of static properties and adds them to the individual in the knowledge graph using the appropriate data type and normalization.
     */
    private void applyStaticProperties(Resource individual) {
        if (config.getStaticProperties() == null) {
            return;
        }
        for (StaticPropertyConfig sp : config.getStaticProperties()) {
            applyStaticProperty(individual, sp);
        }
    }

    /**
     * Applies a single static property to the individual. This involves resolving the data type, normalizing the value if necessary (e.g., for date/time values), and adding the property to the knowledge graph model.
     */
    private void applyStaticProperty(Resource individual, StaticPropertyConfig sp) {
        RDFDatatype dataType = resolveDatatype(sp.getDataType());
        String normalizedValue = normalizeLiteralValue(sp.getValue(), dataType);
        knowledgeGraph.addDataProperty(individual, sp.getOwlProperty(),
                knowledgeGraph.createLiteral(normalizedValue, dataType));
    }

    private Resource createForensicTool() {
        String toolName = config.getForensicTool();
        if (toolName == null || toolName.isBlank()) {
            toolName = config.getName();
        }
        return knowledgeGraph.createIndividual("ForensicTool", toolName);
    }

    private void addForensicTool(Resource individual) {
        knowledgeGraph.addUniqueObjectProperty(individual, "hasForensicTool", forensicTool);
    }

    /**
     * Applies generic mappings to the individual based on the configuration. This method iterates over the list of generic mappings and dispatches each one to the appropriate handler.
     */
    private void applyGenerics(SourceRecord r, Resource individual) {
        if (config.getGenerics() == null) {
            return;
        }
        for (String generic : config.getGenerics()) {
            dispatchGeneric(generic, r, individual);
        }
    }

    /**
     * Applies field mappings to the individual based on the configuration. This method iterates over the list of field mappings and applies each one, which may involve setting data properties or creating linked individuals depending on the mapping type.
     */
    private void applyFieldMappings(SourceRecord r, Resource individual) {
        if (config.getFieldMappings() == null) {
            return;
        }
        for (FieldMappingConfig fm : config.getFieldMappings()) {
            applyFieldMapping(fm, r, individual);
        }
    }

    /**
     * Dispatches a generic mapping by name. This method looks up the list of field mappings associated with the given generic name and applies each of those field mappings to the individual. If the generic name is not found in the configuration, an exception is thrown.
     */
    private void dispatchGeneric(String name, SourceRecord r, Resource individual) {
        List<FieldMappingConfig> mappings = genericMappings.get(name);
        if (mappings == null) {
            throw new IllegalArgumentException("Unknown generic mapping: " + name);
        }
        for (FieldMappingConfig fm : mappings) {
            applyFieldMapping(fm, r, individual);
        }
    }

    /**
     * Applies a field mapping to the individual. The behavior depends on the type of the mapping, which can be either "dataProperty" for setting a data property or "linkedIndividual" for creating a linked individual. This method dispatches to the appropriate handler based on the mapping type.
     */
    private void applyFieldMapping(FieldMappingConfig fm, SourceRecord r, Resource parent) {
        switch (fm.getType()) {
            case "dataProperty" -> applyDataProperty(fm, r, parent);
            case "linkedIndividual" -> applyLinkedIndividual(fm, r, parent);
            default -> throw new IllegalArgumentException("Unknown mapping type: " + fm.getType());
        }
    }

    /**
     * Applies a data property mapping to the individual.
     */
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

    /**
     * Resolves the RDF datatype based on the configured type.
     */
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

    /**
     * Normalizes a literal value based on its datatype. For certain datatypes, such as xsd:dateTimeStamp, this involves parsing the value and reformatting it to a standard representation. For other datatypes, the value is returned as-is.
     */
    private String normalizeLiteralValue(String value, RDFDatatype datatype) {
        if (value == null || datatype == null) {
            return value;
        }
        if (XSDDatatype.XSDdateTimeStamp.getURI().equals(datatype.getURI())) {
            return normalizeDateTimeStamp(value);
        }
        return value;
    }

    /**
     * Normalizes a date/time value to the xsd:dateTimeStamp format. This method attempts to parse the input value using several common date/time formats and converts it to the standard ISO_OFFSET_DATE_TIME format. If the value cannot be parsed as a valid date/time, an exception is thrown.
     */
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

    /**
     * Applies a linked individual mapping to the parent individual. This involves creating a new individual based on the linked individual's class and identifier configuration, applying any data properties and nested links to the linked individual, and then connecting it to the parent individual using the specified object property.
     */
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

    /**
     * Checks if the SourceRecord has all the fields required to construct the identifier for a linked individual. This is used to determine whether the linked individual can be created based on the available data in the SourceRecord.
     */
    private boolean hasAllIdentifierFields(FieldMappingConfig fm, SourceRecord r) {
        return fm.getIdentifier().getFields().stream().allMatch(r::has);
    }

    /**
     * Resolves the identifier for a linked individual based on the configuration. This method supports both hashed identifiers (where the value of a single field is hashed to create the identifier) and combined identifiers (where the values of multiple fields are concatenated with a separator). The appropriate method is called based on the configuration of the identifier in the YAML file.
     */
    private String resolveLinkedIndividualIdentifier(FieldMappingConfig fm, SourceRecord r) {
        if (fm.getIdentifier().isUseHash()) {
            return resolveHashedIdentifier(fm, r);
        }
        if (fm.getIdentifier().getFields().size() == 1) {
            return r.get(fm.getIdentifier().getFields().get(0));
        }
        return resolveCombinedIdentifier(fm, r);
    }

    /**
     * Resolves a hashed identifier for a linked individual. This method assumes that hashing can only be used with a single identifier field, and it retrieves the hashed value of that field from the SourceRecord. If the configuration is invalid (e.g., if multiple fields are specified for hashing), an exception is thrown.
     */
    private String resolveHashedIdentifier(FieldMappingConfig fm, SourceRecord r) {
        if (fm.getIdentifier().getFields().size() > 1) {
            throw new IllegalArgumentException("Hashing can only be used with a single identifier field");
        }
        return r.getHashed(fm.getIdentifier().getFields().get(0));
    }

    /**
     * Resolves a combined identifier for a linked individual. This method concatenates the values of multiple fields with a specified separator.
     */
    private String resolveCombinedIdentifier(FieldMappingConfig fm, SourceRecord r) {
        return fm.getIdentifier().getFields().stream()
                .map(r::get)
                .collect(Collectors.joining(fm.getIdentifier().getSeparator()));
    }

    /**
     * Applies data properties to a linked individual based on the field mappings defined in the configuration. This method iterates over the list of data property mappings for the linked individual and applies each one using the same logic as for top-level individuals.
     */
    private void applyDataPropertiesToLinked(FieldMappingConfig fm, SourceRecord r, Resource linked) {
        if (fm.getDataProperties() != null) {
            for (FieldMappingConfig dp : fm.getDataProperties()) {
                applyFieldMapping(dp, r, linked);
            }
        }
    }

    /**
     * Applies nested links to a linked individual based on the field mappings defined in the configuration. This method iterates over the list of nested link mappings for the linked individual and applies each one, which may involve creating additional linked individuals and connecting them to the current linked individual.
     */
    private void applyNestedLinksToLinked(FieldMappingConfig fm, SourceRecord r, Resource linked) {
        if (fm.getNestedLinks() != null) {
            for (FieldMappingConfig nl : fm.getNestedLinks()) {
                applyFieldMapping(nl, r, linked);
            }
        }
    }
}
