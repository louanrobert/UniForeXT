package be.ccb_uliege.incd.ontology_ingestion;

import java.io.FileOutputStream;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import be.ccb_uliege.incd.ontology_ingestion.ingest.IngestionPipeline;
import be.ccb_uliege.incd.ontology_ingestion.owl.OntologyFacade;

/**
 * Main application class
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) throws Exception {
        OntologyFacade ontology = new OntologyFacade();
        IngestionPipeline.run(ontology.getDataModel(), ontology.getClasses(), ontology.getProperties());
        RDFDataMgr.write(new FileOutputStream("debug.ttl"), ontology.getDataModel(), RDFFormat.TURTLE_PRETTY);
    }
}
