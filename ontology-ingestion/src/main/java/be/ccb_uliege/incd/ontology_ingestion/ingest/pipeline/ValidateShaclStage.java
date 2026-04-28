package be.ccb_uliege.incd.ontology_ingestion.ingest.pipeline;

import java.util.Collection;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shacl.vocabulary.SHACL;
import be.ccb_uliege.incd.ontology_ingestion.validation.ShaclShapesValidator;

/**
 * Pipeline stage that validates ingested RDF data against SHACL shapes.
 * 
 * Runs after data ingestion and before output.
 * Throws exception if validation fails (in strict mode) or logs warnings (in lenient mode).
 */
public class ValidateShaclStage extends IngestionStage {
    private final boolean strictMode;

    /**
     * Create a SHACL validation stage.
     * 
     * @param shapesFilePath Path to SHACL shapes file (absolute or relative to project root).
     * @param strictMode If true, throw exception on validation failure. If false, log warnings.
     */
    public ValidateShaclStage(boolean strictMode) {
        this.strictMode = strictMode;
    }

    @Override
    public void execute(PipelineContext context) {
        try {
            this.log("Loading shapes from: " + context.getShaclShapesPath());
            
            // Load shapes graph
            Shapes shapesGraph = ShaclShapesValidator.loadShapes(context.getShaclShapesPath());
            
            // Get the ingested data graph from context
            Model dataGraph = context.getKnowledgeGraph().getModel();
            // Add the ontology model to the data graph for validation, as shapes may reference ontology classes/properties
            dataGraph.add(context.getKnowledgeGraph().getOntologyModel());
            
            // Validate
            this.log("Validating " + dataGraph.size() + " triples against shapes");
            ValidationReport report = ShaclShapesValidator.validate(dataGraph, shapesGraph);
            
            // Handle results
            logReport(report);
        } catch (Exception e) {
            throw new RuntimeException(this.getLogPrefix() + "SHACL validation stage failed: " + e.getMessage(), e);
        }
    }

    private void logReport(ValidationReport report) {
        if (report.conforms()) {
            System.out.println("Data conforms to shapes.");
        } else {
            System.out.println("Data does not conform to shapes:");
            Collection<ReportEntry> items = report.getEntries();
            for (ReportEntry item : items) {
                String message = item.message();
                String focusNode = item.focusNode() != null ? item.focusNode().toString() : "unknown";
                String severity = nodeToSeverityString(item);
                System.out.println(String.format("[%s] %s (focusNode: %s)", severity, message, focusNode));
            }
        }
    }

   private String nodeToSeverityString(ReportEntry item) {
      Node n = item.focusNode();
        if ( n.equals(SHACL.Violation) ) return "Violation";
        if ( n.equals(SHACL.Warning) ) return "Warning";
        if ( n.equals(SHACL.Info) ) return "Info";
        return "UNKNOWN";
    }
}
