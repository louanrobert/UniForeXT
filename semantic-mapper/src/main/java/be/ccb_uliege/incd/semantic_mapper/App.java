package be.ccb_uliege.incd.semantic_mapper;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ccb_uliege.incd.semantic_mapper.ingest.IngestionPipeline;
import be.ccb_uliege.incd.semantic_mapper.owl.Loader;
import be.ccb_uliege.incd.semantic_mapper.owl.kg.KnowledgeGraphFacade;

/**
 * Main application class
 */
public final class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private static final String SKIP_SHACL_VALIDATION_FLAG = "--skip-shacl-validation";
    private static final String SKIP_SHACL_FLAG = "--skip-shacl";
    private static final String CONFIG_DIR_FLAG = "--config-dir=";
    private static final String SHAPES_FLAG = "--shapes=";
    private static final String BASE_IRI_FLAG = "--base-iri=";

    private static final String ENV_CONFIG_DIR = "CONFIG_DIR";
    private static final String ENV_SHACL_SHAPES_PATH = "SHACL_SHAPES_PATH";
    private static final String ENV_BASE_IRI = "BASE_IRI";

    private static final String PROP_CONFIG_DIR = "config.dir";
    private static final String PROP_SHACL_SHAPES_PATH = "shapes.path";
    private static final String PROP_BASE_IRI = "base.iri";

    private static final Path DEFAULT_CONFIG_DIR = Path.of("..", "ingestion-config");
    private static final Path DEFAULT_SHAPES_PATH = Path.of("..", "ontology", "shapes.ttl");

    private App() {}

    public static void main(String[] args) {
        try {
            // Resolve configuration inputs
            Path configDir = resolvePathArg(args, CONFIG_DIR_FLAG,
                    System.getProperty(PROP_CONFIG_DIR),
                    System.getenv(ENV_CONFIG_DIR),
                    DEFAULT_CONFIG_DIR);

            Path shapesPath = resolvePathArg(args, SHAPES_FLAG,
                    System.getProperty(PROP_SHACL_SHAPES_PATH),
                    System.getenv(ENV_SHACL_SHAPES_PATH),
                    DEFAULT_SHAPES_PATH);

            String baseIri = resolveStringArg(args, BASE_IRI_FLAG,
                    System.getProperty(PROP_BASE_IRI),
                    System.getenv(ENV_BASE_IRI),
                    Loader.getBase());

            boolean skipShacl = shouldSkipShaclValidation(args);

            LOG.info("Config dir: {}", configDir.toAbsolutePath());
            LOG.info("SHACL shapes: {}", shapesPath.toAbsolutePath());
            LOG.info("Base IRI: {}", baseIri);
            LOG.info("Skip SHACL: {}", skipShacl);

            Loader loader = new Loader(baseIri);
            KnowledgeGraphFacade knowledgeGraph = loader.asKnowledgeGraphFacade();
            IngestionPipeline.run(knowledgeGraph, skipShacl, configDir, shapesPath);

            RDFDataMgr.write(new FileOutputStream("out.ttl"), knowledgeGraph.getDataModel(), RDFFormat.TURTLE_PRETTY);
        } catch (Exception e) {
            LOG.error("Application failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    static boolean shouldSkipShaclValidation(String[] args) {
        return Arrays.stream(args)
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(flag -> flag.equals(SKIP_SHACL_VALIDATION_FLAG)
                        || flag.equals(SKIP_SHACL_FLAG)
                        || flag.equals(SKIP_SHACL_VALIDATION_FLAG + "=true")
                        || flag.equals(SKIP_SHACL_FLAG + "=true"));
    }

    private static Path resolvePathArg(String[] args, String flagPrefix, String sysProp, String env, Path def) {
        String fromCli = Arrays.stream(args)
                .filter(a -> a.startsWith(flagPrefix))
                .map(a -> a.substring(flagPrefix.length()))
                .findFirst().orElse(null);
        String chosen = firstNonBlank(fromCli, sysProp, env);
        return chosen != null && !chosen.isBlank() ? Path.of(chosen) : def;
    }

    private static String resolveStringArg(String[] args, String flagPrefix, String sysProp, String env, String def) {
        String fromCli = Arrays.stream(args)
                .filter(a -> a.startsWith(flagPrefix))
                .map(a -> a.substring(flagPrefix.length()))
                .findFirst().orElse(null);
        String chosen = firstNonBlank(fromCli, sysProp, env);
        return chosen != null && !chosen.isBlank() ? chosen : def;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
