package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import java.util.Collection;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import be.ccb_uliege.incd.semantic_mapper.validation.ShaclShapesValidator;

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

    public boolean isStrictMode() {
        return strictMode;
    }

    @Override
    public void execute(PipelineContext context) {
        try {
            this.log("Loading shapes from: " + context.getShaclShapesPath());
            
            // Load shapes graph
            Shapes shapesGraph = ShaclShapesValidator.loadShapes(context.getShaclShapesPath());

            this.log("Loaded " + shapesGraph.getShapeMap().size() + " shapes. Starting validation...");
            
            // Get the ingested data graph from context
            Model dataGraph = context.getKnowledgeGraph().getModel();
            // Do not mutate the data graph; create a temporary union for validation
            Model unionForValidation = ModelFactory.createDefaultModel();
            unionForValidation.add(context.getKnowledgeGraph().getOntologyModel());
            unionForValidation.add(dataGraph);
            
            // Validate
            this.log("Validating " + unionForValidation.size() + " triples against shapes");
            ValidationReport report = ShaclShapesValidator.validate(unionForValidation, shapesGraph);
            
            // Handle results
            logReport(report);
        } catch (Exception e) {
            throw new RuntimeException(this.getLogPrefix() + "SHACL validation stage failed: " + e.getMessage(), e);
        }
    }

    private void logReport(ValidationReport report) {
        if (report.conforms()) {
            this.log("Data conforms to shapes.");
        } else {
            this.log("Data does not conform to shapes:");
            Collection<ReportEntry> items = report.getEntries();
            for (ReportEntry item : items) {
                String message = item.message();
                String focusNode = item.focusNode() != null ? item.focusNode().toString() : "unknown";
                String severity = item.severity() != null ? item.severity().toString() : "UNKNOWN";

                // Source shape (e.g. sh:NodeShape or sh:PropertyShape IRI)
                String sourceShape = item.source() != null ? item.source().toString() : "unknown";

                // The specific constraint that failed (e.g. sh:minCount, sh:datatype)
                String constraint = item.sourceConstraintComponent() != null
                        ? item.sourceConstraintComponent().toString()
                        : "unknown";

                this.log(String.format("[%s] %s (focusNode: %s, shape: %s, constraint: %s)",
                        severity, message, focusNode, sourceShape, constraint));
            }
        }
    }
}
