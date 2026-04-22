package be.ccb_uliege.incd.ontology_ingestion.owl;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import lombok.Getter;

/**
 * Facade exposing a simplified API for ontology and data-model interactions.
 * It coordinates low-level schema loading, class lookup/creation, and property handling.
 */
@Getter
public class OntologyFacade {

   private final Loader loader;
   private final Classes classes;
   private final Properties properties;

   public OntologyFacade() {
      this.loader = new Loader();
      this.classes = new Classes(this.loader);
      this.properties = new Properties(this.loader);
   }

   public OntologyFacade(Loader loader) {
      this.loader = loader;
      this.classes = new Classes(this.loader);
      this.properties = new Properties(this.loader);
   }

   public Model getOntologyModel() {
      return loader.getOntologyModel();
   }

   public Model getDataModel() {
      return loader.getDataModel();
   }

   public Resource getClassResource(String className) {
      return classes.getClass(className);
   }

   public Resource createIndividual(String className, String individualName) {
      return classes.createIndividual(className, individualName);
   }

   public Property getObjectProperty(String propertyName) {
      return properties.getProperty(propertyName);
   }

   public Property getDataProperty(String propertyName) {
      return properties.getDataProperty(propertyName);
   }

   public Literal createLiteral(Object value, RDFDatatype datatype) {
      return properties.createLiteralProperty(value, datatype); // TODO: what does literal property mean?
   }

   public void addUniqueDataProperty(Resource resource, String propertyName, Literal value) {
      properties.addUniqueDataProperty(resource, propertyName, value);
   }

   public void addUniqueObjectProperty(Resource resource, String propertyName, Resource value) {
      properties.addUniqueObjectProperty(resource, propertyName, value);
   }
}