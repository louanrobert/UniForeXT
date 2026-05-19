package be.ccb_uliege.incd.ontology_viewer;

/**
 * SPARQL Query view.
 * Allows users to run read-only queries against the loaded model.
 */
public class QueryView extends BaseWebView {

    public QueryView(JavaBridge bridge) {
        super(bridge, "javaBridge", "query-view.html");
    }

    @Override
    protected String getInitFunctionName() {
        return "initQueryView";
    }
}