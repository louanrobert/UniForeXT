package be.ccb_uliege.incd.ontology_ingestion.owl;

import java.util.HashMap;
import java.util.Map;


import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

public class Classes {
   
   private Map<String, Resource> classes = new HashMap<>();
   private Loader loader;

   public Classes(Loader loader) {
      this.loader = loader;
      loadClasses();
   }

   private void loadClasses() {
      try {
      loader.getModel().listResourcesWithProperty(loader.getModel().getProperty(RDF.type.getURI()), loader.getModel().getResource(OWL.Class.getURI()))
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

   public Resource getClass(String name) {
      return classes.get(name);
   }

   public Resource createIndividual(String className, String individualName) {
      Resource cls = getClass(className);
      if (cls == null) {
         throw new IllegalArgumentException("Class not found: " + className);
      }
      individualName = individualName.replaceAll("[^a-zA-Z0-9]", "_");
      Resource individual = loader.getModel().createResource(Loader.getBase() + className + "/" + individualName);
      individual.addProperty(RDF.type, cls);
      return individual;
   }
}
