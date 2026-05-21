package be.ccb_uliege.incd.semantic_mapper.owl.ontology;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Facade exposing the ontology schema model and vocabulary helpers.
 * It coordinates low-level schema loading, class lookup, and property handling.
 */
public class OntologyFacade {
   private final Classes classes;
   private final Properties properties;
   private final Model ontology;

   public OntologyFacade(Model model) {
      this.ontology = model;
      this.classes = new Classes(this.ontology);
      this.properties = new Properties(this.ontology);
   }

   public Model getOntologyModel() {
      return this.ontology;
   }

   public Resource getClassResource(String className) {
      return classes.getClass(className);
   }

   public Property getObjectProperty(String propertyName) {
      return properties.getProperty(propertyName);
   }

   public Property getDataProperty(String propertyName) {
      return properties.getDataProperty(propertyName);
   }

   /**
    * Retrieves the inverse property name for a given object property.
    * @param propertyName the local name of the property
    * @return the local name of the inverse property, or null if no inverse is defined
    */
   public String getInversePropertyName(String propertyName) {
      return properties.getInversePropertyName(propertyName);
   }
}