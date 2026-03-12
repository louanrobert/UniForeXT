package be.ccb_uliege.incd.ontology_viewer;

import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

/**
 * Manages a new Stage with a WebView that renders a Vis.js Network graph.
 * Shows the 1-hop neighborhood of a selected individual.
 * Double-clicking a node expands its neighborhood without resetting the graph.
 * Already-explored nodes are visually distinguished with a different border color.
 */
public class GraphView {

    private final Stage stage;
    private final WebView webView;
    private final WebEngine webEngine;
    private final JavaBridge bridge;
    private final String initialUri;

    /** Strong reference to prevent GC of the bridge */
    @SuppressWarnings("unused")
    private JavaBridge bridgeRef;

    @SuppressWarnings("removal")
    public GraphView(JavaBridge bridge, String individualUri) {
        this.bridge = bridge;
        this.bridgeRef = bridge;
        this.initialUri = individualUri;

        stage = new Stage();
        stage.setTitle("Graph: " + OntologyService.localName(individualUri));
        stage.setWidth(1000);
        stage.setHeight(700);

        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);
                // Initialize graph with the individual's neighborhood
                webEngine.executeScript("initGraph('" + escapeJs(individualUri) + "')");
            }
        });

        webEngine.loadContent(HtmlLoader.load("graph-view.html"));

        Scene scene = new Scene(webView);
        stage.setScene(scene);
        stage.show();
    }

    public Stage getStage() {
        return stage;
    }

    private static String escapeJs(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }

}
