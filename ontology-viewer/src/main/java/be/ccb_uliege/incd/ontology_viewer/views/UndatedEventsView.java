package be.ccb_uliege.incd.ontology_viewer.views;

import be.ccb_uliege.incd.ontology_viewer.JavaBridge;

/**
 * Undated Events view showing all events without a timestamp/date,
 * grouped and organized by event type for quick reference.
 */
public class UndatedEventsView extends BaseWebView {

    public UndatedEventsView(JavaBridge bridge) {
        super(bridge, "javaBridge", "undated-events.html");
    }

    @Override
    protected String getInitFunctionName() {
        return "initUndatedEvents";
    }
}
