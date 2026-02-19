package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

public class AntivirusMapper implements SourceMapper {
   private Classes classes;
   private Properties properties;

   public AntivirusMapper(Classes classes, Properties properties) {
      this.classes = classes;
      this.properties = properties;
   }

   @Override public void map(SourceRecord r, Model model) {
      String identifier = r.get("Threat Name") + "_" + r.get("timestamp");
      // Create the detection individual
      Resource detection = classes.createIndividual("Antivirus", identifier);
      // Add properties to the detection individual
      detection.addProperty(properties.getDataProperty("hasName"), r.get("Threat Name"));

      // Generics
      ChainsawGenerics.addTimestamp(classes, properties, r, detection);
      ChainsawGenerics.addComputer(classes, properties, r, detection);
      ChainsawGenerics.addLogFile(classes, properties, r, detection);
      ChainsawGenerics.addUser(classes, properties, r, detection);

      // Specific properties for antivirus detections
      if (r.has("detections")) {
         Literal eventDataLiteral = properties.createLiteralProperty("Antivirus detection: " +  r.get("detections"), RDFLangString.rdfLangString);
         detection.addProperty(properties.getDataProperty("hasDescription"), eventDataLiteral);
      }

      if (r.has("Threat Path")) {
         Literal eventDataLiteral = properties.createLiteralProperty("Threat Path: " + r.get("Threat Path"), RDFLangString.rdfLangString);
         detection.addProperty(properties.getDataProperty("hasExtraInformation"), eventDataLiteral);
      }

      if (r.has("SHA1")) {
         Literal eventDataLiteral = properties.createLiteralProperty("SHA1: " + r.get("SHA1"), RDFLangString.rdfLangString);
         detection.addProperty(properties.getDataProperty("hasExtraInformation"), eventDataLiteral);
      }

      if (r.has("Threat Type")) {
         Literal eventDataLiteral = properties.createLiteralProperty("Threat Type: " + r.get("Threat Type"), RDFLangString.rdfLangString);
         detection.addProperty(properties.getDataProperty("hasExtraInformation"), eventDataLiteral);
      }
   }
   
}
