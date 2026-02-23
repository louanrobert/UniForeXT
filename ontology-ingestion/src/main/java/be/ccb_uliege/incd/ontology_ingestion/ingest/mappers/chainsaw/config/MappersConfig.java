package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.config;

import java.util.List;

public class MappersConfig {
    private List<MapperConfig> mappers;

    public List<MapperConfig> getMappers() {
        return mappers;
    }

    public void setMappers(List<MapperConfig> mappers) {
        this.mappers = mappers;
    }
}
