package be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline;

import java.nio.file.Path;
import java.util.List;

import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.YamlMapperRegistry;

/**
 * Defines file-to-mapper ingestion tasks.
 */
public class DefineIngestionTasksStage extends IngestionStage {

    // TODO: Externalize these definitions to a file or command-line arguments.
    private static final List<TaskDefinition> TASK_DEFINITIONS = List.of(
            new TaskDefinition(
                    "c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\sigma.csv",
                    "SigmaMapper", ';'),
            new TaskDefinition(
                    "c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\antivirus.csv",
                    "AntivirusMapper", ';'),
            new TaskDefinition(
                    "c:\\Users\\Robert_Louan\\Downloads\\DFIR_Automation_Results_2\\wks02\\QuickWins\\Chainsaw_results\\account_tampering.csv",
                    "AccountTamperingMapper", ';'));

    @Override
    public void execute(PipelineContext context) {
        YamlMapperRegistry mapperRegistry = context.getMapperRegistry();
        if (mapperRegistry == null) {
            throw new IllegalStateException("Mapper registry must be loaded before defining ingestion tasks");
        }

        for (TaskDefinition taskDefinition : TASK_DEFINITIONS) {
            IngestionTask task = new IngestionTask(
                    Path.of(taskDefinition.filePath()),
                    mapperRegistry.getMapper(taskDefinition.mapperName()),
                    taskDefinition.delimiter());
            context.addTask(task);
        }
    }

    private record TaskDefinition(String filePath, String mapperName, Character delimiter) {
    }
}
