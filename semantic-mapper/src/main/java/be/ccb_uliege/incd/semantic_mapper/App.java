package be.ccb_uliege.incd.semantic_mapper;

import java.io.FileOutputStream;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import be.ccb_uliege.incd.semantic_mapper.ingest.IngestionPipeline;
import be.ccb_uliege.incd.semantic_mapper.owl.Loader;
import be.ccb_uliege.incd.semantic_mapper.owl.kg.KnowledgeGraphFacade;

/**
 * Main application class
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) throws Exception {
        Loader loader = new Loader();
        KnowledgeGraphFacade knowledgeGraph = loader.asKnowledgeGraphFacade();
        IngestionPipeline.run(knowledgeGraph);
        RDFDataMgr.write(new FileOutputStream("debug.ttl"), knowledgeGraph.getDataModel(), RDFFormat.TURTLE_PRETTY);
    }
}
