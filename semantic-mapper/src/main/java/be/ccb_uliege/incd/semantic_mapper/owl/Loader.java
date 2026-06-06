package be.ccb_uliege.incd.semantic_mapper.owl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ccb_uliege.incd.semantic_mapper.owl.kg.KnowledgeGraphFacade;
import be.ccb_uliege.incd.semantic_mapper.owl.ontology.OntologyFacade;
import lombok.Getter;

@Getter
public class Loader {

   private static final Logger LOG = LoggerFactory.getLogger(Loader.class);

   /** The ontology schema model (read-only, loaded from file). */
   private Model ontology;

   /** The model for ingested triples. */
   private Model knowledgeGraph;

   private static final String ONTOLOGY_PATH_ENV = "ONTOLOGY_PATH";
   private static final String DEFAULT_ONTOLOGY_PATH = "../ontology/ontology.rdf";
   private final String ontologyPath;
   @Getter private static String base = "http://www.semanticweb.org/robert_louan/ontologies/2026/1/unified-forensics-results#";

   public Loader() {
      this(null);
   }

   public Loader(String baseIri) {
      String path = System.getenv(ONTOLOGY_PATH_ENV) != null ? System.getenv(ONTOLOGY_PATH_ENV) : DEFAULT_ONTOLOGY_PATH;
      this.ontologyPath = path;
      if (baseIri != null && !baseIri.isBlank()) {
         base = baseIri;
      }

      try {
         // Load the ontology schema from file
         ontology = ModelFactory.createDefaultModel();
         ontology.read(ontologyPath);

         // Create a separate data model for ingested instances
         knowledgeGraph = ModelFactory.createDefaultModel();
         knowledgeGraph.setNsPrefix("ufr", base);
         knowledgeGraph.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
         knowledgeGraph.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
         knowledgeGraph.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
         knowledgeGraph.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
      } catch (Exception e) {
         LOG.error("Failed to load ontology from {}: {}", ontologyPath, e.getMessage(), e);
         throw e;
      }
   }

   public long getOntologySize() {
      return ontology.size();
   }

   public long getDataSize() {
      return knowledgeGraph.size();
   }

   /**
    * Factory method exposing this loader as the ontology facade.
    */
   public OntologyFacade asOntologyFacade() {
      return new OntologyFacade(this.ontology);
   }

   /**
    * Factory method exposing this loader as the knowledge-graph facade.
    */
   public KnowledgeGraphFacade asKnowledgeGraphFacade() {
      return new KnowledgeGraphFacade(this.knowledgeGraph, this.asOntologyFacade());
   }
}
