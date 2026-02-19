package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

public class AccountTamperingMapper implements SourceMapper {
   private Classes classes;
   private Properties properties;

   public AccountTamperingMapper(Classes classes, Properties properties) {
      this.classes = classes;
      this.properties = properties;
   }

   @Override public void map(SourceRecord r, Model model) {
      String identifier = r.get("detections") + "_" + r.get("timestamp");
      // Create the detection event individual
      Resource detection = classes.createIndividual("AccountTampering", identifier);
      // Add properties to the detection event
      ChainsawGenerics.addTimestamp(classes, properties, r, detection);
      ChainsawGenerics.addComputer(classes, properties, r, detection);
      ChainsawGenerics.addLogFile(classes, properties, r, detection);
      ChainsawGenerics.addName(classes, properties, r, detection);
      ChainsawGenerics.addUser(classes, properties, r, detection);

      // Add sigma as a description of the detection
      detection.addProperty(properties.getDataProperty("hasDescription"), "Account Tampering",
            RDFLangString.rdfLangString);

      if (r.has("User SID")) {
         Literal eventDataLiteral = properties.createLiteralProperty("User SID: " + r.get("User SID"), RDFLangString.rdfLangString);
         detection.addProperty(properties.getDataProperty("hasExtraInformation"), eventDataLiteral);
      }

      if (r.has("Member SID")) {
         Literal eventDataLiteral = properties.createLiteralProperty("Member SID: " + r.get("Member SID"), RDFLangString.rdfLangString);
         detection.addProperty(properties.getDataProperty("hasExtraInformation"), eventDataLiteral);
      }
   }
}
