package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw;

import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

public class SigmaMapper implements SourceMapper {
   private Classes classes;
   private Properties properties;

   public SigmaMapper(Classes classes, Properties properties) {
      this.classes = classes;
      this.properties = properties;
   }

   @Override
   public void map(SourceRecord r, Model model) {
      // Identifier
      String identifier = r.get("detections") + "_" + r.get("timestamp");
      // Create the detection event individual
      Resource detection = classes.createIndividual("Sigma", identifier);
      // Add sigma as a description of the detection
      detection.addProperty(properties.getDataProperty("hasDescription"), "Sigma detection",
            RDFLangString.rdfLangString);

      ChainsawGenerics.addName(classes, properties, r, detection);
      ChainsawGenerics.addTimestamp(classes, properties, r, detection);
      ChainsawGenerics.addLogFile(classes, properties, r, detection);
      ChainsawGenerics.addComputer(classes, properties, r, detection);

      if (r.has("Event Data")) {
         Literal eventDataLiteral = properties.createLiteralProperty(r.get("Event Data"), RDFLangString.rdfLangString);
         detection.addProperty(properties.getDataProperty("hasExtraInformation"), eventDataLiteral);
      }

   }

}
