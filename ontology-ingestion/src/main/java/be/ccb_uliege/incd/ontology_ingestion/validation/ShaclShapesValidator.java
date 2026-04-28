package be.ccb_uliege.incd.ontology_ingestion.validation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shacl.validation.ReportItem;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.vocabulary.RDF;

/**
 * Utility for validating RDF data against SHACL shapes.
 * 
 * Typical usage:
 *   Model dataGraph = ...; // your RDF data
 *   Model shapesGraph = ShaclValidator.loadShapesFromResource("shacl/shapes.ttl");
 *   ValidationReport report = ShaclValidator.validate(dataGraph, shapesGraph);
 *   if (!report.conforms()) {
 *     report.getItems().forEach(item -> System.out.println(item.getMessage()));
 *   }
 */
public class ShaclShapesValidator {

    /**
     * Load SHACL shapes from a file or classpath resource.
     * 
     * @param resourcePath Path to the shapes file (e.g., "shacl/unified-forensics-results.shapes.ttl")
     * @return Shaes containing the SHACL shapes.
     */
    public static Shapes loadShapes(String resourcePath) {
        Model model = ModelFactory.createDefaultModel();
        model.read(resourcePath);
        
        return Shapes.parse(model);
    }


    /**
     * Validate a data graph against a shapes graph.
     * 
     * @param dataGraph The RDF data to validate.
     * @param shapesGraph The SHACL shapes to validate against.
     * @return ValidationReport with conforms() and items().
     */
    public static ValidationReport validate(Model data, Shapes shapesGraph) {
        Graph modelAsGraph = data.getGraph();
        return ShaclValidator.get().validate(shapesGraph, modelAsGraph);
    }

    private static List<String> extractErrorMessages(ValidationReport report) {
        List<String> errors = new ArrayList<>();
        for (ReportEntry item : report.getEntries()) {
                String message = item.message();
                String focusNode = item.focusNode() != null ? item.focusNode().toString() : "unknown";
                String severity = item.severity() != null ? item.severity().toString() : "UNKNOWN";
                errors.add(String.format("[%s] %s (focusNode: %s)", severity, message, focusNode));
            }
        return errors;
    }

    /**
     * Print validation report to stdout for debugging.
     * 
     * @param report The ValidationReport to print.
     */
    public static void printReport(ValidationReport report) {
        if (report.conforms()) {
            System.out.println("Data conforms to shapes.");
        } else {
            System.out.println("Data does not conform to shapes:");
            System.out.println(report.toString());
        }
    }
}
