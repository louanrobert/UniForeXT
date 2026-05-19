package be.ccb_uliege.incd.ontology_viewer;

/**
 * Manages the WebView that renders the Vis.js Timeline.
 * Individuals with date properties are shown on the timeline.
 * Double-clicking an item opens its neighborhood graph.
 */
public class TimelineView extends BaseWebView {

    public TimelineView(JavaBridge bridge) {
        super(bridge, "javaBridge", "timeline.html");
    }

    @Override
    protected String getInitFunctionName() {
        return "initTimeline";
    }
}
