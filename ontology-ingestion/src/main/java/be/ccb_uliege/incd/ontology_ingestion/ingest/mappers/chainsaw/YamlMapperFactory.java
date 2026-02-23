package be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw;

import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.ontology_ingestion.ingest.mappers.chainsaw.config.MappersConfig;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

@Component
public class YamlMapperFactory {

    private final List<SourceMapper> mappers;

    public YamlMapperFactory(Classes classes, Properties properties) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("chainsaw-mappers.yaml")) {
            if (is == null) {
                throw new IllegalStateException("chainsaw-mappers.yaml not found on classpath");
            }
            MappersConfig config = objectMapper.readValue(is, MappersConfig.class);
            this.mappers = config.getMappers().stream()
                    .map(mc -> (SourceMapper) new YamlSourceMapper(mc, classes, properties))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mapper configuration from chainsaw-mappers.yaml", e);
        }
    }

    public List<SourceMapper> getMappers() {
        return mappers;
    }
}
