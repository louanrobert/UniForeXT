package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceRecord;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

public class IndicatorRemovalMapper implements SourceMapper {
   private Classes classes;
   private Properties properties;

   public IndicatorRemovalMapper(Classes classes, Properties properties) {
      this.classes = classes;
      this.properties = properties;
   }

   @Override public void map(SourceRecord r, Model model) {
      // Check if a technique is present in detections
      Pair<String, String> extracted = ChainsawGenerics.extractTechnique(r.get("detections"));

      String techniqueId = null, techniqueDescription = null;
      if (extracted.getRight() != null) { // A technique was successfully extracted
         techniqueId = extracted.getLeft();
         techniqueDescription = extracted.getRight();
      } else { // No technique could be extracted, use the original detections value
         techniqueDescription = extracted.getLeft();
      }

      String identifier = techniqueDescription + "_" + r.get("timestamp");
      Resource indicatorRemoval = classes.createIndividual("IndicatorRemoval", identifier);

      if(techniqueId != null) {
         ChainsawGenerics.addTechnique(classes, properties, indicatorRemoval, techniqueId);
      }
      ChainsawGenerics.addName(classes, properties, r, indicatorRemoval);
      ChainsawGenerics.addTimestamp(classes, properties, r, indicatorRemoval);
      ChainsawGenerics.addLogFile(classes, properties, r, indicatorRemoval);
      ChainsawGenerics.addComputer(classes, properties, r, indicatorRemoval);
      ChainsawGenerics.addUser(classes, properties, r, indicatorRemoval);

      //! Scheduled Task Name column
      // T1070.009 - Scheduled Task was Deleted is the only possible value for detections in the Indicator Removal category
      //* 1. Create ScheduledTaskDeletion_configuration_event
      // Remove / from the task name to avoid issues with individual creation (as it is used as an identifier)
      Resource scheduledTaskDeletionConfigEvent = classes.createIndividual("ScheduledTaskDeletion_configuration_event", r.get("Scheduled Task Name").replace("/", ""));
      
      // Add the scheduled task name as a property of the scheduledTaskDeletionConfigEvent individual
      Literal taskNameLiteral = properties.createLiteralProperty(r.get("Scheduled Task Name"), RDFLangString.rdfLangString);
      properties.addUniqueDataProperty(scheduledTaskDeletionConfigEvent, "hasName", taskNameLiteral);
      // Add the timestamp as a property of the scheduledTaskDeletionConfigEvent individual
      Literal timestampLiteral = properties.createLiteralProperty(r.get("timestamp"), RDFLangString.rdfLangString);
      properties.addUniqueDataProperty(scheduledTaskDeletionConfigEvent, "hasTimestamp", timestampLiteral);
      // Add computer as a property of the scheduledTaskDeletionConfigEvent individual
      if (r.has("Computer")) {
         Resource computer = classes.createIndividual("Computer", r.get("Computer"));
         Literal computerHostname = properties.createLiteralProperty(r.get("Computer"), RDFLangString.rdfLangString);
         properties.addUniqueDataProperty(computer, "hasHostname", computerHostname);
         properties.addUniqueObjectProperty(scheduledTaskDeletionConfigEvent, "hasComputer", computer);
      }
      // Add user as a property of the scheduledTaskDeletionConfigEvent individual
      if (r.has("User")) {
         Resource user = classes.createIndividual("User", r.get("User"));
         Literal userLiteral = properties.createLiteralProperty(r.get("User"), RDFLangString.rdfLangString);
         properties.addUniqueDataProperty(user, "hasName", userLiteral);
         properties.addUniqueObjectProperty(scheduledTaskDeletionConfigEvent, "hasUser", user);
      }
      // Add log file as a property of the scheduledTaskDeletionConfigEvent individual
      if (r.has("path")) {
         Resource logFile = classes.createIndividual("LogFile", r.getHashed("path"));
         Resource path = classes.createIndividual("Path", r.getHashed("path"));
         // Add path content to the Path individual
         Literal pathLiteral = properties.createLiteralProperty(r.get("path"), RDFLangString.rdfLangString);
         properties.addUniqueDataProperty(path, "hasContent", pathLiteral);

         // Add path to LogFile individual
         properties.addUniqueObjectProperty(logFile, "hasPath", path);
         // Add LogFile to scheduledTaskDeletionConfigEvent individual
         properties.addUniqueObjectProperty(scheduledTaskDeletionConfigEvent, "hasLogFile", logFile);
      }

      //* 2. Link the scheduled task to the indicator removal detection
      properties.addUniqueObjectProperty(scheduledTaskDeletionConfigEvent, "hasDetection", indicatorRemoval);

   }
}
