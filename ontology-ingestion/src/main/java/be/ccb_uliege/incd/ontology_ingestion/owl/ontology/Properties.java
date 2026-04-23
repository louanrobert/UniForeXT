package be.ccb_uliege.incd.ontology_ingestion.owl.ontology;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import be.ccb_uliege.incd.ontology_ingestion.owl.Loader;

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
   private final Model ontology;

   public Properties(Model ontology) {
      this.ontology = ontology;
      loadProperties();
   }

   private void loadProperties() {
      try {
         ontology.listResourcesWithProperty(ontology.getProperty(RDF.type.getURI()),
                     ontology.getResource(OWL.ObjectProperty.getURI()))
               .forEachRemaining(r -> {
               String localName = r.getLocalName();
               if (localName != null && !localName.isBlank()) {
                  properties.put(localName, ontology.getProperty(r.getURI()));
               }
               });
         ontology
               .listResourcesWithProperty(ontology.getProperty(RDF.type.getURI()),
                     ontology.getResource(OWL.DatatypeProperty.getURI()))
               .forEachRemaining(r -> {
               String localName = r.getLocalName();
               if (localName != null && !localName.isBlank()) {
                  dataProperties.put(localName, ontology.getProperty(r.getURI()));
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

}
