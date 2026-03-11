package be.ccb_uliege.incd.ontology_ingestion.ingest;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;

import be.ccb_uliege.incd.ontology_ingestion.ingest.implementations.csv.CsvIngester;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.YamlMapperFactory;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

/**
 * This class orchestrates the ingestion process.
 * It defines the files to be ingested, the mappers to use for each file, and
 * executes the ingestion using the correct ingester implementation.
 * 
 * 
 */
public class IngestionPipeline {

    public static void run(Model model, Classes classes, Properties properties) {
        SourceIngester ingester = new CsvIngester();

        // Load YAML-driven mappers
        YamlMapperFactory yamlMapperFactory = YamlMapperFactory.fromYamlFile("chainsaw-mappers.yaml", classes,
                properties);

        // TODO: This should be externalized to a config file or command-line arguments,
        // rather than hardcoded in the code. For now, this is sufficient for testing
        // and demonstration purposes.
        // File/mapper pairs to ingest
        Map<Path, Pair<SourceMapper, Character>> ingestionTasks = new LinkedHashMap<>();
        ingestionTasks.put(
                Path.of(
                        "c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\sigma.csv"),
                Pair.of(yamlMapperFactory.getMapper("SigmaMapper"), ';'));
        ingestionTasks.put(
                Path.of(
                        "c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\antivirus.csv"),
                Pair.of(yamlMapperFactory.getMapper("AntivirusMapper"), ';'));
        ingestionTasks.put(
                Path.of(
                        "c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\account_tampering.csv"),
                Pair.of(yamlMapperFactory.getMapper("AccountTamperingMapper"), ';'));
        // ingestionTasks.put(
        // Path.of("c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\indicator_removal.csv"),
        // Pair.of(new IndicatorRemovalMapper(classes, properties), ';')
        // );

        // Execute ingestion tasks
        ingestionTasks.forEach((file, mapperAndDelimiter) -> {
            System.out.println("Ingesting file: " + file);
            ingester.ingest(file, mapperAndDelimiter.getLeft(), model, mapperAndDelimiter.getRight());
        });
    }

}
