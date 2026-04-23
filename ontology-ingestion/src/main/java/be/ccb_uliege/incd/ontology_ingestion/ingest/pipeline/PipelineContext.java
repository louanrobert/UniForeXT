package be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.YamlMapperRegistry;
import be.ccb_uliege.incd.ontology_ingestion.owl.kg.KnowledgeGraphFacade;

/**
 * Shared state passed between stages of the ingestion pipeline.
 */
public class PipelineContext {

    private final KnowledgeGraphFacade knowledgeGraph;
    private final SourceIngester sourceIngester;
    private final String mapperConfigPath;

    private YamlMapperRegistry mapperRegistry;
    private final List<IngestionTask> ingestionTasks = new ArrayList<>();

    public PipelineContext(KnowledgeGraphFacade knowledgeGraph, SourceIngester sourceIngester, String mapperConfigPath) {
        this.knowledgeGraph = Objects.requireNonNull(knowledgeGraph, "knowledgeGraph cannot be null");
        this.sourceIngester = Objects.requireNonNull(sourceIngester, "sourceIngester cannot be null");
        this.mapperConfigPath = Objects.requireNonNull(mapperConfigPath, "mapperConfigPath cannot be null");
    }

    public KnowledgeGraphFacade getKnowledgeGraph() {
        return knowledgeGraph;
    }

    public SourceIngester getSourceIngester() {
        return sourceIngester;
    }

    public String getMapperConfigPath() {
        return mapperConfigPath;
    }

    public YamlMapperRegistry getMapperRegistry() {
        return mapperRegistry;
    }

    public void setMapperRegistry(YamlMapperRegistry mapperRegistry) {
        this.mapperRegistry = Objects.requireNonNull(mapperRegistry, "mapperRegistry cannot be null");
    }

    public List<IngestionTask> getIngestionTasks() {
        return Collections.unmodifiableList(ingestionTasks);
    }

    public void addTask(IngestionTask task) {
        ingestionTasks.add(Objects.requireNonNull(task, "task cannot be null"));
    }
}
