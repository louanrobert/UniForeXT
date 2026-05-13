package be.ccb_uliege.incd.semantic_mapper.owl.kg;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import be.ccb_uliege.incd.semantic_mapper.owl.Loader;
import be.ccb_uliege.incd.semantic_mapper.owl.ontology.OntologyFacade;

/**
 * Facade exposing knowledge-graph operations on top of the ontology schema.
 * It coordinates individual creation and data-model mutations.
 */
public class KnowledgeGraphFacade {

   private final OntologyFacade ontologyFacade;
   private final Model knowledgeGraph;


   public KnowledgeGraphFacade(Model knowledgeGraph, OntologyFacade ontologyFacade) {
      this.knowledgeGraph = knowledgeGraph;
      this.ontologyFacade = ontologyFacade;
   }

   public Model getDataModel() {
      return this.knowledgeGraph;
   }

   public Resource createIndividual(String className, String individualName) {
      Resource cls = ontologyFacade.getClassResource(className);
      if (cls == null) {
         throw new IllegalArgumentException("Class not found: " + className);
      }
      String safeIndividualName = individualName.replaceAll("[^a-zA-Z0-9]", "_");
      String safeClassName = className.replaceAll("[^a-zA-Z0-9]", "_");
      Resource individual = knowledgeGraph.createResource(Loader.getBase() + safeClassName + "-" + safeIndividualName);
      individual.addProperty(org.apache.jena.vocabulary.RDF.type, cls);
      return individual;
   }

   public Literal createLiteral(Object value, RDFDatatype datatype) {
      return knowledgeGraph.createTypedLiteral(value, datatype);
   }

   public Property getObjectProperty(String propertyName) {
      return ontologyFacade.getObjectProperty(propertyName);
   }

   public Property getDataProperty(String propertyName) {
      return ontologyFacade.getDataProperty(propertyName);
   }

   public void addDataProperty(Resource resource, String propertyName, Literal value) {
      resource.addProperty(getDataProperty(propertyName), value);
   }

   public void addObjectProperty(Resource resource, String propertyName, Resource value) {
      resource.addProperty(getObjectProperty(propertyName), value);
      addInverseProperty(propertyName, resource, value);
   }

   public void addUniqueDataProperty(Resource resource, String propertyName, Literal value) {
      Property property = getDataProperty(propertyName);
      if (!resource.hasProperty(property)) {
         resource.addProperty(property, value);
      }
   }

   public void addUniqueObjectProperty(Resource resource, String propertyName, Resource value) {
      Property property = getObjectProperty(propertyName);
      if (!resource.hasProperty(property, value)) {
         resource.addProperty(property, value);
         addInverseProperty(propertyName, resource, value);
      }
   }

   /**
    * Adds the inverse property to the object if an inverse is defined in the ontology.
    * For example, if hasComputer is added, isComputerOf will be added in the reverse direction.
    * @param propertyName the name of the forward property
    * @param resource the subject resource
    * @param value the object resource
    */
   private void addInverseProperty(String propertyName, Resource resource, Resource value) {
      String inversePropertyName = ontologyFacade.getInversePropertyName(propertyName);
      if (inversePropertyName != null) {
         try {
            Property inverseProperty = getObjectProperty(inversePropertyName);
            value.addProperty(inverseProperty, resource);
         } catch (IllegalArgumentException e) {
            // Inverse property not found in ontology, skip
         }
      }
   }

   public Model getModel() {
        return this.knowledgeGraph;
    }

   public Model getOntologyModel() {
      return this.ontologyFacade.getOntologyModel();
   }
}