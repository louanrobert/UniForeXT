package be.ccb_uliege.incd.ontology_ingestion.ingest;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;

import be.ccb_uliege.incd.ontology_ingestion.ingest.implementations.csv.CsvIngester;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.*;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

public class IngestionPipeline {

   public static void run(Model model, Classes classes, Properties properties) {
      CsvIngester csvIngester = new CsvIngester();

      // File/mapper pairs to ingest
      Map<Path, Pair<SourceMapper, Character>> ingestionTasks = new LinkedHashMap<>();
      ingestionTasks.put(
            Path.of("c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\sigma.csv"), 
            Pair.of(new SigmaMapper(classes, properties), ';')
      );
      ingestionTasks.put(
         Path.of("c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\antivirus.csv"),
         Pair.of(new AntivirusMapper(classes, properties), ';')
      );
      ingestionTasks.put(
         Path.of("c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\account_tampering.csv"),
         Pair.of(new AccountTamperingMapper(classes, properties), ';')
      );
      ingestionTasks.put(
         Path.of("c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\indicator_removal.csv"),
         Pair.of(new IndicatorRemovalMapper(classes, properties), ';')
      );

      // Execute ingestion tasks
      ingestionTasks.forEach((file, mapperAndDelimiter) -> {
         System.out.println("Ingesting file: " + file);
         csvIngester.ingest(file, mapperAndDelimiter.getLeft(), model, mapperAndDelimiter.getRight());
      });
   }

}
