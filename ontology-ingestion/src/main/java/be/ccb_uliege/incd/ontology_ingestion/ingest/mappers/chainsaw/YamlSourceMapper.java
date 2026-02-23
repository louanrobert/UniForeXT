package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw;

import java.util.stream.Collectors;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.config.FieldMappingConfig;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.config.MapperConfig;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.config.StaticPropertyConfig;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

public class YamlSourceMapper implements SourceMapper {

    private final MapperConfig config;
    private final Classes classes;
    private final Properties properties;

    public YamlSourceMapper(MapperConfig config, Classes classes, Properties properties) {
        this.config = config;
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
        switch (name) {
            case "addTimestamp" -> ChainsawGenerics.addTimestamp(classes, properties, r, individual);
            case "addComputer" -> ChainsawGenerics.addComputer(classes, properties, r, individual);
            case "addLogFile" -> ChainsawGenerics.addLogFile(classes, properties, r, individual);
            case "addUser" -> ChainsawGenerics.addUser(classes, properties, r, individual);
            case "addName" -> ChainsawGenerics.addName(classes, properties, r, individual);
            default -> throw new IllegalArgumentException("Unknown generic method: " + name);
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
        if (!r.has(fm.getIdentifierField())) {
            return;
        }
        String id = fm.isUseHash()
                ? r.getHashed(fm.getIdentifierField())
                : r.get(fm.getIdentifierField());
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

        // Link to parent
        if (fm.isUnique()) {
            properties.addUniqueObjectProperty(parent, fm.getLinkProperty(), linked);
        } else {
            parent.addProperty(properties.getProperty(fm.getLinkProperty()), linked);
        }
    }
}
