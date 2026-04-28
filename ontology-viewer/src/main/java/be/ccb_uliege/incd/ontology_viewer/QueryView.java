package be.ccb_uliege.incd.ontology_viewer;

import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * SPARQL Query view.
 * Allows users to run read-only queries against the loaded model.
 */
public class QueryView {

    private final BorderPane root;
    private final WebView webView;
    private final WebEngine webEngine;

    public QueryView(JavaBridge bridge) {
        root = new BorderPane();
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                @SuppressWarnings("removal")
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);
                webEngine.executeScript("initQueryView()");
            }
        });

        webEngine.loadContent(HtmlLoader.load("query-view.html"));
        root.setCenter(webView);
    }

    public BorderPane getRoot() {
        return root;
    }
}