package be.ccb_uliege.incd.semantic_mapper.owl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import be.ccb_uliege.incd.semantic_mapper.owl.kg.KnowledgeGraphFacade;
import be.ccb_uliege.incd.semantic_mapper.owl.ontology.OntologyFacade;
import lombok.Getter;

@Getter
public class Loader {

   /** The ontology schema model (read-only, loaded from file). */
   private Model ontology;

   /** The model for ingested triples. References the ontology via owl:imports and prefix. */
   private Model knowledgeGraph;

   private static final String ONTOLOGY_PATH_ENV = "ONTOLOGY_FILE_PATH";
   private static final String DEFAULT_ONTOLOGY_PATH = "c:\\Users\\Robert_Louan\\OneDrive - FED BE\\Documents\\TFE\\ontology.rdf";
   private static final String path = System.getenv(ONTOLOGY_PATH_ENV) != null
         ? System.getenv(ONTOLOGY_PATH_ENV)
         : DEFAULT_ONTOLOGY_PATH;
   @Getter private static String base = "http://www.semanticweb.org/robert_louan/ontologies/2026/1/unified-forensics-results#";

   public Loader() {
      // Load the ontology schema from file
      ontology = ModelFactory.createDefaultModel();
      ontology.read(path);

      // Create a separate data model for ingested instances
      knowledgeGraph = ModelFactory.createDefaultModel();
      knowledgeGraph.setNsPrefix("ufr", base);
      knowledgeGraph.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
      knowledgeGraph.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
      knowledgeGraph.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
      knowledgeGraph.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
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

   public void prettyPrint() {
      // Print the size of the ontology model
      System.out.println("Ontology model size: " + getOntologySize() + " statements");
      System.out.println("Data model size: " + getDataSize() + " statements");
      // List all classes (resources of type owl:Class)
      System.out.println("\n--- Classes ---");
      ontology.listResourcesWithProperty(RDF.type, OWL.Class)
            .forEachRemaining(r -> System.out.println(r.getURI()));

      // List all properties
      System.out.println("\n--- Properties ---");
      ontology.listResourcesWithProperty(RDF.type, OWL.ObjectProperty)
            .forEachRemaining(r -> System.out.println(r.getURI()));

      // List all data properties
      System.out.println("\n--- Data Properties ---");
      ontology.listResourcesWithProperty(RDF.type, OWL.DatatypeProperty)
            .forEachRemaining(r -> System.out.println(r.getURI()));
   }
}
