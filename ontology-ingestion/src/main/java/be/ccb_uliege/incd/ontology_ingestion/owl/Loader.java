package be.ccb_uliege.incd.ontology_ingestion.owl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import lombok.Getter;

@Getter
public class Loader {

   private Model model;
   private static final String ONTOLOGY_PATH_ENV = "ONTOLOGY_FILE_PATH";
   private static final String DEFAULT_ONTOLOGY_PATH = "c:\\Users\\Robert_Louan\\OneDrive - FED BE\\Documents\\TFE\\ontology.rdf";
   private static final String path = System.getenv(ONTOLOGY_PATH_ENV) != null
         ? System.getenv(ONTOLOGY_PATH_ENV)
         : DEFAULT_ONTOLOGY_PATH;
   @Getter private static String base = "http://www.semanticweb.org/robert_louan/ontologies/2026/1/unified-forensics-results#";

   public Loader() {
      model = ModelFactory.createDefaultModel();
      model.read(path);
   }

   public long getSize() {
      return model.size();
   }

   public void prettyPrint() {
      // Print the size of the model
      System.out.println("Model size: " + getSize() + " statements");
      // List all classes (resources of type owl:Class)
      System.out.println("\n--- Classes ---");
      model.listResourcesWithProperty(RDF.type, OWL.Class)
            .forEachRemaining(r -> System.out.println(r.getURI()));

      // List all properties
      System.out.println("\n--- Properties ---");
      model.listResourcesWithProperty(RDF.type, OWL.ObjectProperty)
            .forEachRemaining(r -> System.out.println(r.getURI()));

      // List all data properties
      System.out.println("\n--- Data Properties ---");
      model.listResourcesWithProperty(RDF.type, OWL.DatatypeProperty)
            .forEachRemaining(r -> System.out.println(r.getURI()));
   }
}
