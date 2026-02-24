package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

public class ChainsawGenerics {

   public static void addTimestamp(Classes classes, Properties properties, SourceRecord r, Resource detection) {
      if(!r.has("timestamp")) {
         return;
      }
      Literal timestampLiteral = properties.createLiteralProperty(r.get("timestamp"), RDFLangString.rdfLangString);
      detection.addProperty(properties.getDataProperty("hasTimestamp"), timestampLiteral);
   }

   public static void addComputer(Classes classes, Properties properties, SourceRecord r, Resource detection) {
      if(!r.has("Computer")) {
         return;
      }
      // Create or retrieve the Computer individual based on the "Computer" field in
      // the source record
      Resource computer = classes.createIndividual("Computer", r.get("Computer"));
      // The addUniqueDataProperty method ensures we don't create duplicate properties
      Literal computerHostname = properties.createLiteralProperty(r.get("Computer"), RDFLangString.rdfLangString);
      properties.addUniqueDataProperty(computer, "hasHostname", computerHostname);
      detection.addProperty(properties.getProperty("hasComputer"), computer);
   }

   public static void addLogFile(Classes classes, Properties properties, SourceRecord r, Resource detection) {
      if(!r.has("path")) {
         return;
      }
      // Detection -> LogFile -> Path
      Resource logFile = classes.createIndividual("LogFile", r.getHashed("path"));
      Resource path = classes.createIndividual("Path", r.getHashed("path"));
      // Add path content to the Path individual
      Literal pathLiteral = properties.createLiteralProperty(r.get("path"), RDFLangString.rdfLangString);
      properties.addUniqueDataProperty(path, "hasContent", pathLiteral);

      // Add path to LogFile individual
      properties.addUniqueObjectProperty(logFile, "hasPath", path);
      // Add LogFile to Detection individual
      properties.addUniqueObjectProperty(detection, "hasLogFile", logFile);
   }

   // This method adds a name to the detection individual if the "detections" field is present in the source record
   public static void addName(Classes classes, Properties properties, SourceRecord r, Resource detection) {
      if(!r.has("detections")) {
         return;
      }
      detection.addProperty(properties.getDataProperty("hasName"), r.get("detections"), RDFLangString.rdfLangString);
   }

   public static void addUser(Classes classes, Properties properties, SourceRecord r, Resource detection) {
      if(!r.has("User")) {
         return;
      }
      Resource user = classes.createIndividual("User", r.get("User"));
      Literal userLiteral = properties.createLiteralProperty(r.get("User"), RDFLangString.rdfLangString);
      properties.addUniqueDataProperty(user, "hasName", userLiteral);
      detection.addProperty(properties.getProperty("hasUser"), user);
   }

   public static void addTechnique(Classes classes, Properties properties, Resource detection, String technique) {
      if(technique == null) {
         return;
      }
      Literal techniqueLiteral = properties.createLiteralProperty(technique, RDFLangString.rdfLangString);
      detection.addProperty(properties.getDataProperty("hasTechnique"), techniqueLiteral);
   }

   public static Pair<String, String> extractTechnique(String detection) {
      // Example : T1070.009 - Scheduled Task was Deleted
      if(detection.contains(" - ")) {
         String techniqueId = detection.split(" - ")[0].trim();
         String techniqueDescription = detection.split(" - ")[1].trim();
         return new Pair<>(techniqueId, techniqueDescription);
      } else {
         return new Pair<>(detection, null);
      }
   }
}
