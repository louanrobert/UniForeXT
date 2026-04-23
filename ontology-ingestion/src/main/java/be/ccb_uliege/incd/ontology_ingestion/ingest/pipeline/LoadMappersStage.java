package be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline;

import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.YamlMapperRegistry;

/**
 * Loads all source mappers from YAML into the pipeline context.
 */
public class LoadMappersStage implements IngestionStage {

    @Override
    public void execute(PipelineContext context) {
        YamlMapperRegistry mapperRegistry = YamlMapperRegistry.fromYamlFile(
                context.getMapperConfigPath(),
                context.getKnowledgeGraph());
        context.setMapperRegistry(mapperRegistry);
    }
}
