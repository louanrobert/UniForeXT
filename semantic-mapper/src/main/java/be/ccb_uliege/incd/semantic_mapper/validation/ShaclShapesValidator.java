package be.ccb_uliege.incd.semantic_mapper.validation;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import java.nio.file.Paths;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;

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
        try {
            Model model = ModelFactory.createDefaultModel();
            // Normalize local paths to file: URIs so Jena reads correctly on Windows
            String uri = Paths.get(resourcePath).toAbsolutePath().toUri().toString();
            RDFDataMgr.read(model, uri);
            return Shapes.parse(model);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load SHACL shapes from: " + resourcePath, e);
        }
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
}
