package be.ccb_uliege.incd.ontology_viewer;

import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Event Explorer view with:
 * - Histogram of event counts over time (top)
 * - Scrollable event table (bottom-left)
 * - Detail panel for selected event (right)
 */
public class EventExplorerView {

    private final BorderPane root;
    private final WebView webView;
    private final WebEngine webEngine;

    public EventExplorerView(JavaBridge bridge) {

        root = new BorderPane();
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Error is ok bc JSObject will be delivered with JavaFX in the future (JavaFX is already imported)
                @SuppressWarnings("removal")
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);
                webEngine.executeScript("initExplorer()");
            }
        });

        webEngine.loadContent(HtmlLoader.load("event-explorer.html"));
        root.setCenter(webView);
    }

    public BorderPane getRoot() {
        return root;
    }

}
