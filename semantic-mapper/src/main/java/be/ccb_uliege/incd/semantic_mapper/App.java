package be.ccb_uliege.incd.semantic_mapper;

import java.io.FileOutputStream;
import java.util.Arrays;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import be.ccb_uliege.incd.semantic_mapper.ingest.IngestionPipeline;
import be.ccb_uliege.incd.semantic_mapper.owl.Loader;
import be.ccb_uliege.incd.semantic_mapper.owl.kg.KnowledgeGraphFacade;

/**
 * Main application class
 */
public final class App {
    private static final String SKIP_SHACL_VALIDATION_FLAG = "--skip-shacl-validation";
    private static final String SKIP_SHACL_FLAG = "--skip-shacl";

    private App() {
    }

    public static void main(String[] args) throws Exception {
        Loader loader = new Loader();
        KnowledgeGraphFacade knowledgeGraph = loader.asKnowledgeGraphFacade();
        IngestionPipeline.run(knowledgeGraph, shouldSkipShaclValidation(args));
        RDFDataMgr.write(new FileOutputStream("out.ttl"), knowledgeGraph.getDataModel(), RDFFormat.TURTLE_PRETTY);
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
}
