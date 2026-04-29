package be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline;

import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.YamlSourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.YamlMapperRegistry;

/**
 * Defines file-to-mapper ingestion tasks.
 */
public class DefineIngestionTasksStage extends IngestionStage {

    @Override
    public void execute(PipelineContext context) {
        YamlMapperRegistry mapperRegistry = context.getMapperRegistry();
        if (mapperRegistry == null) {
            throw new IllegalStateException("Mapper registry must be loaded before defining ingestion tasks");
        }

        for (var mapper : mapperRegistry.getMappers().values()) {
            if (!(mapper instanceof YamlSourceMapper yamlSourceMapper)) {
                throw new IllegalStateException("Unsupported mapper implementation: " + mapper.getClass().getName());
            }

            IngestionTask task = new IngestionTask(
                    yamlSourceMapper.getFilePath(),
                    yamlSourceMapper,
                    ';');
            context.addTask(task);
        }
    }
}
