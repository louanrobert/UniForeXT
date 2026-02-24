package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.FieldMappingConfig;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.MapperConfig;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.config.StaticPropertyConfig;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

/*
 * SourceMapper implementation that creates OWL individuals based on YAML configuration.
 * 
 * This class is responsible for taking a SourceRecord, extracting values based on the provided MapperConfig, and using those values to create and populate an OWL individual in the Jena Model.
 * It supports static properties, field mappings for data properties and linked individuals, and the use of generic mapping groups for reusable field mapping configurations.
 */
public class YamlSourceMapper implements SourceMapper {

    private final MapperConfig config;
    private final Map<String, List<FieldMappingConfig>> genericMappings;
    private final Classes classes;
    private final Properties properties;

    public YamlSourceMapper(MapperConfig config, Map<String, List<FieldMappingConfig>> genericMappings, Classes classes, Properties properties) {
        this.config = config;
        this.genericMappings = genericMappings != null ? genericMappings : Collections.emptyMap();
        this.classes = classes;
        this.properties = properties;
    }

    public String getName() {
        return config.getName();
    }

    @Override
    public void map(SourceRecord r, Model model) {
        // Build identifier by joining resolved fields with separator
        String identifier = config.getIdentifier().getFields().stream()
                .map(r::get)
                .collect(Collectors.joining(config.getIdentifier().getSeparator()));

        // Create the OWL individual
        Resource individual = classes.createIndividual(config.getOwlClass(), identifier);

        // Apply static properties unconditionally
        if (config.getStaticProperties() != null) {
            for (StaticPropertyConfig sp : config.getStaticProperties()) {
                Literal lit = properties.createLiteralProperty(sp.getValue(), RDFLangString.rdfLangString);
                individual.addProperty(properties.getDataProperty(sp.getOwlProperty()), lit);
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
        Literal lit = properties.createLiteralProperty(value, RDFLangString.rdfLangString);

        if (fm.isUnique()) {
            properties.addUniqueDataProperty(parent, fm.getOwlProperty(), lit);
        } else {
            parent.addProperty(properties.getDataProperty(fm.getOwlProperty()), lit);
        }
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

        Resource linked = classes.createIndividual(fm.getOwlClass(), id);

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

        properties.addUniqueObjectProperty(parent, fm.getLinkProperty(), linked);
    }
}
