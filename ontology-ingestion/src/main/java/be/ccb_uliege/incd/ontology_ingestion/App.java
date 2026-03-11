package be.ccb_uliege.incd.ontology_ingestion;

import java.io.FileOutputStream;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import be.ccb_uliege.incd.ontology_ingestion.ingest.IngestionPipeline;
import be.ccb_uliege.incd.ontology_ingestion.owl.Classes;
import be.ccb_uliege.incd.ontology_ingestion.owl.Loader;
import be.ccb_uliege.incd.ontology_ingestion.owl.Properties;

/**
 * Main application class
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) throws Exception {
        Loader l = new Loader();

        Classes c = new Classes(l);
        Properties p = new Properties(l);

        IngestionPipeline.run(l.getDataModel(), c, p);
        RDFDataMgr.write(new FileOutputStream("debug.ttl"), l.getDataModel(), RDFFormat.TURTLE_PRETTY);
    }
}
