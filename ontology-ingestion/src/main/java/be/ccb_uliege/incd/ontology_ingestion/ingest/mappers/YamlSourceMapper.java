package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers;

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

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.FieldMappingConfig;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.MapperConfig;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.StaticPropertyConfig;
import be.ccb_uliege.incd.ontology_ingestion.owl.kg.KnowledgeGraphFacade;

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

    public YamlSourceMapper(MapperConfig config, Map<String, List<FieldMappingConfig>> genericMappings, KnowledgeGraphFacade knowledgeGraph) {
        this.config = config;
        this.genericMappings = genericMappings != null ? genericMappings : Collections.emptyMap();
        this.knowledgeGraph = knowledgeGraph;
    }

    public String getName() {
        return config.getName();
    }

    @Override
    public void map(SourceRecord r) {
        // Build identifier by joining resolved fields with separator
        String identifier = config.getIdentifier().getFields().stream()
                .map(r::get)
                .collect(Collectors.joining(config.getIdentifier().getSeparator()));

        // Create the OWL individual
        Resource individual = knowledgeGraph.createIndividual(config.getOwlClass(), identifier);

        // Apply static properties unconditionally
        if (config.getStaticProperties() != null) {
            for (StaticPropertyConfig sp : config.getStaticProperties()) {
                RDFDatatype dataType = resolveDatatype(sp.getDataType());
                String normalizedValue = normalizeLiteralValue(sp.getValue(), dataType);
                knowledgeGraph.addDataProperty(individual, sp.getOwlProperty(),
                        knowledgeGraph.createLiteral(normalizedValue, dataType));
            }
        }

        // Dispatch generics
        if (config.getGenerics() != null) {
            for (String generic : config.getGenerics()) {
                dispatchGeneric(generic, r, individual);
            }
        }

        // Apply field mappings
        if (config.getFieldMappings() != null) {
            for (FieldMappingConfig fm : config.getFieldMappings()) {
                applyFieldMapping(fm, r, individual);
            }
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

        // Already valid dateTimeStamp lexical form.
        try {
            return OffsetDateTime.parse(value).format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return ZonedDateTime.parse(value).toOffsetDateTime().format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Instant.parse(value).atOffset(ZoneOffset.UTC).format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }

        for (DateTimeFormatter formatter : LOCAL_DATE_TIME_INPUT_FORMATTERS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
                return dateTime.atOffset(ZoneOffset.UTC).format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return localDate.atStartOfDay().atOffset(ZoneOffset.UTC).format(OUTPUT_DATE_TIME_STAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }

        throw new IllegalArgumentException("Could not normalize xsd:dateTimeStamp value: " + rawValue);
    }

    private void applyLinkedIndividual(FieldMappingConfig fm, SourceRecord r, Resource parent) {
        for (String field : fm.getIdentifier().getFields()) {
            if (!r.has(field)) {
                return; // Skip if any identifier field is missing
            }
        }
        String id = null;
        if (fm.getIdentifier().isUseHash()) { // Can only use one field if hashing
            id = r.getHashed(fm.getIdentifier().getFields().get(0));
            if (fm.getIdentifier().getFields().size() > 1) {
                throw new IllegalArgumentException("Hashing can only be used with a single identifier field");
            }
        } else if (fm.getIdentifier().getFields().size() == 1) {
            id = r.get(fm.getIdentifier().getFields().get(0));
        } else {
            id = fm.getIdentifier().getFields().stream()
                    .map(r::get)
                    .collect(Collectors.joining(fm.getIdentifier().getSeparator()));
        }

        Resource linked = knowledgeGraph.createIndividual(fm.getOwlClass(), id);

        // Apply data properties on the linked individual
        if (fm.getDataProperties() != null) {
            for (FieldMappingConfig dp : fm.getDataProperties()) {
                applyFieldMapping(dp, r, linked);
            }
        }

        // Apply nested links recursively
        if (fm.getNestedLinks() != null) {
            for (FieldMappingConfig nl : fm.getNestedLinks()) {
                applyFieldMapping(nl, r, linked);
            }
        }

        knowledgeGraph.addUniqueObjectProperty(parent, fm.getLinkProperty(), linked);
    }
}
