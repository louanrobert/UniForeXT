package be.ccb_uliege.incd.semantic_mapper.ingest.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValidateShaclStageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void executePassesWhenDataConformsAndDoesNotMutateDataGraph() throws Exception {
        PipelineContext context = newContext(writeShape("@prefix sh: <http://www.w3.org/ns/shacl#> .\n"
                + "@prefix ex: <" + PipelineTestSupport.BASE + "> .\n"
                + "ex:EventShape a sh:NodeShape ;\n"
                + "  sh:targetClass ex:Event .\n"));
        Resource event = context.getKnowledgeGraph().createIndividual("Event", "1");
        long originalDataSize = context.getKnowledgeGraph().getModel().size();

        new ValidateShaclStage(true).execute(context);

        assertTrue(new ValidateShaclStage(true).isStrictMode());
        assertFalse(new ValidateShaclStage(false).isStrictMode());
        assertTrue(context.getKnowledgeGraph().getModel().containsResource(event));
        assertTrue(context.getKnowledgeGraph().getModel().size() == originalDataSize);
    }

    @Test
    void executeWrapsShapeLoadingFailure() {
        PipelineContext context = newContext(tempDirectory.resolve("missing-shapes.ttl"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> new ValidateShaclStage(true).execute(context));

        assertTrue(exception.getMessage().contains("[ValidateShaclStage] SHACL validation stage failed"));
    }

    @Test
    void executeLogsNonConformingReportsWithoutThrowing() throws Exception {
        PipelineContext context = newContext(writeShape("@prefix sh: <http://www.w3.org/ns/shacl#> .\n"
                + "@prefix ex: <" + PipelineTestSupport.BASE + "> .\n"
                + "ex:EventShape a sh:NodeShape ;\n"
                + "  sh:targetClass ex:Event ;\n"
                + "  sh:property [\n"
                + "    sh:path ex:requiredValue ;\n"
                + "    sh:minCount 1 ;\n"
                + "    sh:message \"required value is missing\" ;\n"
                + "  ] .\n"));
        context.getKnowledgeGraph().createIndividual("Event", "missing-value");

        new ValidateShaclStage(false).execute(context);
    }

    private PipelineContext newContext(Path shapesPath) {
        return new PipelineContext(
                PipelineTestSupport.newKnowledgeGraphFacade(),
                (file, mapper, delimiter, progressListener) -> { },
                "config",
                shapesPath.toString());
    }

    private Path writeShape(String content) throws Exception {
        Path shapesFile = tempDirectory.resolve("shapes-" + System.nanoTime() + ".ttl");
        Files.writeString(shapesFile, content);
        return shapesFile;
    }
}
