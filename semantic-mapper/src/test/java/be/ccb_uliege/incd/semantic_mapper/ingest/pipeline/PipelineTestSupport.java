package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import be.ccb_uliege.incd.semantic_mapper.owl.kg.KnowledgeGraphFacade;
import be.ccb_uliege.incd.semantic_mapper.owl.ontology.OntologyFacade;

public final class PipelineTestSupport {

    public static final String BASE = "http://example.test/ontology#";

    private PipelineTestSupport() {
    }

    public static KnowledgeGraphFacade newKnowledgeGraphFacade() {
        Model ontology = ModelFactory.createDefaultModel();
        ontology.add(ontology.createResource(BASE + "Event"), RDF.type, OWL.Class);
        ontology.add(ontology.createResource(BASE + "ForensicTool"), RDF.type, OWL.Class);
        ontology.add(ontology.createProperty(BASE + "hasForensicTool"), RDF.type, OWL.ObjectProperty);
        return new KnowledgeGraphFacade(ModelFactory.createDefaultModel(), new OntologyFacade(ontology));
    }
}
