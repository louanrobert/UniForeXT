package be.ccb_uliege.incd.ontology_viewer;

/**
 * Bridge class exposed to JavaScript inside WebView via JSObject.
 * JavaScript code calls methods on this object to fetch data from Jena.
 *
 */
public class JavaBridge {

    private final OntologyService ontologyService;
    private final App app;

    public JavaBridge(OntologyService ontologyService, App app) {
        this.ontologyService = ontologyService;
        this.app = app;
    }

    /**
     * Called from JavaScript to get the neighbors of an individual.
     * Returns a JSON string: { "nodes": [...], "edges": [...] }
     */
    public String getNeighborsJson(String individualUri) {
        return ontologyService.getNeighborsJson(individualUri);
    }

    /**
     * Called from JavaScript to get a lightweight summary of neighbor types/counts.
     * Returns JSON: { totalCount, literalCount, types: [{type, count, color}...] }
     */
    public String getNeighborSummaryJson(String individualUri) {
        return ontologyService.getNeighborSummaryJson(individualUri);
    }

    /**
     * Called from JavaScript to get filtered, limited neighbors.
     * @param allowedTypesJson JSON array of type names
     * @param maxPerType       max nodes per type (0 = unlimited)
     * @param includeLiterals  "true" or "false"
     */
    public String getFilteredNeighborsJson(String individualUri, String allowedTypesJson,
                                           int maxPerType, String includeLiterals) {
        boolean literals = "true".equals(includeLiterals);
        return ontologyService.getFilteredNeighborsJson(individualUri, allowedTypesJson,
                                                        maxPerType, literals);
    }

    /**
     * Called from JavaScript to get all timeline items.
     * Returns a Vis.js-compatible JSON array.
     */
    public String getTimelineItemsJson() {
        return ontologyService.getTimelineItemsJson();
    }

    /**
     * Called from JavaScript to get timeline groups.
     */
    public String getTimelineGroupsJson() {
        return ontologyService.getTimelineGroupsJson();
    }

    /**
     * Called from JavaScript to get undated individuals.
     */
    public String getUndatedIndividualsJson() {
        return ontologyService.getUndatedIndividualsJson();
    }

    /**
     * Called from JavaScript to check if a node has already been explored.
     */
    public boolean isExplored(String uri) {
        return ontologyService.isExplored(uri);
    }

    /**
     * Called from JavaScript when a timeline item is double-clicked.
     * Opens the graph view for that individual.
     */
    public void openGraphForIndividual(String uri) {
        javafx.application.Platform.runLater(() -> app.openGraphView(uri));
    }

    /**
     * Called from JavaScript to get detailed properties of an individual.
     * Used by the Event Explorer detail panel.
     */
    public String getIndividualDetailsJson(String individualUri) {
        return ontologyService.getIndividualDetailsJson(individualUri);
    }

    /**
     * Called from JavaScript to navigate back to the view selector.
     */
    public void navigateBack() {
        javafx.application.Platform.runLater(() -> app.showViewSelector());
    }

    /**
     * Called from JavaScript to log messages to Java console.
     */
    public void log(String message) {
        System.out.println("[JS] " + message);
    }
}
