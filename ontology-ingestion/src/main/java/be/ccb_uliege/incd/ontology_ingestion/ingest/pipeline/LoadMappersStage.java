package be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline;


import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.YamlMapperRegistry;

/**
 * Loads all source mappers from YAML files in the configuration directory.
 * Each YAML file's generics are isolated and not shared across files.
 */
public class LoadMappersStage extends IngestionStage {

    @Override
    public void execute(PipelineContext context) {
        log("Loading mappers from config directory: " + context.getMapperConfigPath());
        YamlMapperRegistry mapperRegistry = YamlMapperRegistry.fromYamlDirectory(
                context.getMapperConfigPath(),
                context.getKnowledgeGraph());
        context.setMapperRegistry(mapperRegistry);
    }
}
