package be.ccb_uliege.incd.ontology_ingestion.owl;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

/**
 * This class is responsible for loading and managing the properties defined in the ontology.
 * It uses the Loader to access the ontology model and extracts all resources that are of type OWL.ObjectProperty and OWL.DatatypeProperty.
 * It provides methods to retrieve properties by name, create literals with specific datatypes, and add properties to resources while ensuring that duplicate properties are not added.
 * The property names are expected to be the local part of the URI (after the #).
 * The addUniqueDataProperty and addUniqueObjectProperty methods check if the resource already has the specified property before adding it, ensuring that duplicate properties are not created.
 */
public class Properties {

   private final Map<String, Property> properties = new HashMap<>();

   private final Map<String, Property> dataProperties = new HashMap<>();
   private final Loader loader;

   public Properties(Loader loader) {
      this.loader = loader;
      loadProperties();
   }

   public Properties(OntologyFacade facade) {
      this(facade.getLoader());
   }

   private void loadProperties() {
      try {
         loader.getOntologyModel()
               .listResourcesWithProperty(loader.getOntologyModel().getProperty(RDF.type.getURI()),
                     loader.getOntologyModel().getResource(OWL.ObjectProperty.getURI()))
               .forEachRemaining(r -> {
               String localName = r.getLocalName();
               if (localName != null && !localName.isBlank()) {
                  properties.put(localName, loader.getOntologyModel().getProperty(r.getURI()));
               }
               });
         loader.getOntologyModel()
               .listResourcesWithProperty(loader.getOntologyModel().getProperty(RDF.type.getURI()),
                     loader.getOntologyModel().getResource(OWL.DatatypeProperty.getURI()))
               .forEachRemaining(r -> {
               String localName = r.getLocalName();
               if (localName != null && !localName.isBlank()) {
                  dataProperties.put(localName, loader.getOntologyModel().getProperty(r.getURI()));
               }
               });
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

   /**
    * Retrieves an object property by its local name.
    * @param name the local name of the property (the part of the URI after the #)
    * @return the Property representing the object property, or null if no property with the given
    */
   public Property getProperty(String name) {
      Property property = properties.get(name);
      if (property == null) {
         throw new IllegalArgumentException("Object property not found: " + name);
      }
      return property;
   }

   /**
    * Retrieves a data property by its local name.
    * @param name the local name of the property (the part of the URI after the #)
    * @return the Property representing the data property, or null if no property with the given name is found
    * @throws IllegalArgumentException if no data property with the given name is found
    */
   public Property getDataProperty(String name) {
      Property property = dataProperties.get(name);
      if (property == null) {
         throw new IllegalArgumentException("Data property not found: " + name);
      }
      return property;
   }

   /**
    * Creates a typed literal with the given value and RDF datatype.
    * @param value the value of the literal
    * @param datatype the RDFDatatype to use for the literal
    * @return a Literal representing the typed literal
    */
   public Literal createLiteralProperty(Object value, RDFDatatype datatype) {
      return loader.getDataModel().createTypedLiteral(value, datatype);
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
