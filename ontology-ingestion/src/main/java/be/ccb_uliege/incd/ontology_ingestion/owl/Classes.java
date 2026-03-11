package be.ccb_uliege.incd.ontology_ingestion.owl;

import java.util.HashMap;
import java.util.Map;


import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

/**
 * This class is responsible for loading and managing the classes defined in the ontology.
 * It uses the Loader to access the ontology model and extracts all resources that are of type OWL.Class.
 * It provides methods to retrieve a class by name and to create individuals of a given class.
 * The class names are expected to be the local part of the URI (after the #), and individuals are created with URIs based on the class name and individual name.
 */
public class Classes {
   
   private Map<String, Resource> classes = new HashMap<>();
   private Loader loader;

   public Classes(Loader loader) {
      this.loader = loader;
      loadClasses();
   }

   /**
    * Loads all classes from the ontology model and stores them in a map for easy retrieval.
    * The method looks for all resources that have an RDF type of OWL.Class and extracts their local name to use as the key in the map.
    * If any error occurs during loading (e.g., file not found, invalid RDF), it prints the stack trace and exits the program with an error message.
    */
   private void loadClasses() {
      try {
      loader.getOntologyModel().listResourcesWithProperty(loader.getOntologyModel().getProperty(RDF.type.getURI()), loader.getOntologyModel().getResource(OWL.Class.getURI()))
            .forEachRemaining(r -> classes.put(r.getURI().split("#")[1], r));
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

   /**
    * Creates an individual of a given class with a specified name.
    * The method first retrieves the class resource using the getClass method. If the class is not found, it throws an IllegalArgumentException.
    * It then creates a new resource for the individual with a URI based on the class name and individual name (special characters in the individual name are replaced with underscores).
    * Finally, it adds a triple to the model indicating that the individual is of the specified class type and returns the created individual resource.
    * 
    * @param className the local name of the class to which the individual belongs
    * @param individualName the name of the individual to create (will be sanitized to form a valid URI)
    * @return the Resource representing the created individual
    * @throws IllegalArgumentException if the specified class name does not exist in the ontology
    */
   public Resource createIndividual(String className, String individualName) throws IllegalArgumentException {
      Resource cls = getClass(className);
      if (cls == null) {
         throw new IllegalArgumentException("Class not found: " + className);
      }
      individualName = individualName.replaceAll("[^a-zA-Z0-9]", "_");
      Resource individual = loader.getDataModel().createResource(Loader.getBase() + className + "-" + individualName);
      individual.addProperty(RDF.type, cls);
      return individual;
   }
}
