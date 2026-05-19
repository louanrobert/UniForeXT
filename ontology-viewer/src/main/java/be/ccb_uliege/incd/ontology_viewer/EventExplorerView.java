package be.ccb_uliege.incd.ontology_viewer;

/**
 * Event Explorer view with:
 * - Histogram of event counts over time (top)
 * - Scrollable event table (bottom-left)
 * - Detail panel for selected event (right)
 */
public class EventExplorerView extends BaseWebView {

    public EventExplorerView(JavaBridge bridge) {
        super(bridge, "javaBridge", "event-explorer.html");
    }

    @Override
    protected String getInitFunctionName() {
        return "initExplorer";
    }
}
