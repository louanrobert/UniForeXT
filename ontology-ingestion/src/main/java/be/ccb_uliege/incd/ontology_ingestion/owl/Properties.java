package be.ccb_uliege.incd.ontology_ingestion.owl;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

public class Properties {

   private Map<String, Property> properties = new HashMap<>();

   private Map<String, Property> dataProperties = new HashMap<>();
   private Loader loader;

   public Properties(Loader loader) {
      this.loader = loader;
      loadProperties();
   }

   private void loadProperties() {
      try {
         loader.getModel()
               .listResourcesWithProperty(loader.getModel().getProperty(RDF.type.getURI()),
                     loader.getModel().getResource(OWL.ObjectProperty.getURI()))
               .forEachRemaining(
                     r -> properties.put(r.getURI().split("#")[1], loader.getModel().getProperty(r.getURI())));
         loader.getModel()
               .listResourcesWithProperty(loader.getModel().getProperty(RDF.type.getURI()),
                     loader.getModel().getResource(OWL.DatatypeProperty.getURI()))
               .forEachRemaining(
                     r -> dataProperties.put(r.getURI().split("#")[1], loader.getModel().getProperty(r.getURI())));
      } catch (Exception e) {
         e.printStackTrace();
         // If error occurs, likely to be a problem with the ontology file path or
         // format. Ensure the file exists and is a valid RDF file.
         System.err.println("Error loading properties from ontology: " + e.getMessage());
         System.err.println("Ontology file path: " + Loader.getBase());
         System.err.println("Current working directory: " + System.getProperty("user.dir"));
         System.exit(1);
      }
   }

   public Property getProperty(String name) {
      return properties.get(name);
   }

   public Property getDataProperty(String name) {
      Property property = dataProperties.get(name);
      if (property == null) {
         throw new IllegalArgumentException("Data property not found: " + name);
      }
      return property;
   }

   public Literal createLiteralProperty(Object value, RDFDatatype datatype) {
      return loader.getModel().createTypedLiteral(value, datatype);
   }

   /**
    * Adds a data property to a resource only if it doesn't already exist.
    *
    * @param resource     the resource to add the property to
    * @param propertyName the name of the data property
    * @param value        the literal value
    */
   public void addUniqueDataProperty(Resource resource, String propertyName, Literal value) {
      Property property = getDataProperty(propertyName);
      if (!resource.hasProperty(property)) {
         resource.addProperty(property, value);
      }
   }

   /**
    * Adds an object property to a resource only if it doesn't already exist.
    *
    * @param resource     the resource to add the property to
    * @param propertyName the name of the object property
    * @param value        the resource value
    */
   public void addUniqueObjectProperty(Resource resource, String propertyName, Resource value) {
      Property property = getProperty(propertyName);
      if (!resource.hasProperty(property)) {
         resource.addProperty(property, value);
      }
   }
}
