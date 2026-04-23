package be.ccb_uliege.incd.ontology_ingestion.owl.ontology;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import be.ccb_uliege.incd.ontology_ingestion.owl.Loader;

/**
 * This class is responsible for loading and managing the classes defined in the ontology.
 * It uses the Loader to access the ontology model and extracts all resources that are of type OWL.Class.
 * It provides methods to retrieve a class by name and to create individuals of a given class.
 * The class names are expected to be the local part of the URI (after the #), and individuals are created with URIs based on the class name and individual name.
 */
public class Classes {
   
   private final Map<String, Resource> classes = new HashMap<>();
   private final Model ontology;

   public Classes(Model ontology) {
      this.ontology = ontology;
      loadClasses();
   }

   /**
    * Loads all classes from the ontology model and stores them in a map for easy retrieval.
    * The method looks for all resources that have an RDF type of OWL.Class and extracts their local name to use as the key in the map.
    * If any error occurs during loading (e.g., file not found, invalid RDF), it prints the stack trace and exits the program with an error message.
    */
   private void loadClasses() {
      try {
      ontology.listResourcesWithProperty(ontology.getProperty(RDF.type.getURI()), ontology.getResource(OWL.Class.getURI()))
            .forEachRemaining(r -> {
               String localName = r.getLocalName();
               if (localName != null && !localName.isBlank()) {
                  classes.put(localName, r);
               }
            });
      } catch (Exception e) {
         e.printStackTrace();
         // If error occurs, likely to be a problem with the ontology file path or format. Ensure the file exists and is a valid RDF file.
         System.err.println("Error loading classes from ontology: " + e.getMessage());
         System.err.println("Ontology file path: " + Loader.getBase());
         System.err.println("Current working directory: " + System.getProperty("user.dir"));
         System.exit(1);
      }
   }

   /**
    * Retrieves a class resource by its local name.
    * @param name the local name of the class (the part of the URI after the #)
    * @return the Resource representing the class, or null if no class with the given name is found
    */
   public Resource getClass(String name) {
      return classes.get(name);
   }

}
