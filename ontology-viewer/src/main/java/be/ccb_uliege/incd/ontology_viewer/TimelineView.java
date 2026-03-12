package be.ccb_uliege.incd.ontology_viewer;

import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Manages the WebView that renders the Vis.js Timeline.
 * Individuals with date properties are shown on the timeline.
 * Double-clicking an item opens its neighborhood graph.
 */
public class TimelineView {

    private final BorderPane root;
    private final WebView webView;
    private final WebEngine webEngine;


    public TimelineView(JavaBridge bridge) {

        root = new BorderPane();
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Set up the JS bridge once the page is loaded
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Error is ok bc JSObject will be delivered with JavaFX in the future (JavaFX is already imported)
                @SuppressWarnings("removal")
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);
                // Inject data and initialize
                webEngine.executeScript("initTimeline()");
            }
        });

        webEngine.loadContent(HtmlLoader.load("timeline.html"));
        root.setCenter(webView);
    }

    public BorderPane getRoot() {
        return root;
    }

}
