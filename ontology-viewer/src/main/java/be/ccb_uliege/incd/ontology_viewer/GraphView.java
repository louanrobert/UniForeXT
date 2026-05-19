package be.ccb_uliege.incd.ontology_viewer;

import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a new Stage with a WebView that renders a Vis.js Network graph.
 * Shows the 1-hop neighborhood of a selected individual.
 * Double-clicking a node expands its neighborhood without resetting the graph.
 * Already-explored nodes are visually distinguished with a different border color.
 */
public class GraphView {

    private static final Logger LOG = Logger.getLogger(GraphView.class.getName());
    private final Stage stage;
    private final WebView webView;
    private final WebEngine webEngine;

    public GraphView(JavaBridge bridge, String individualUri) {
        stage = new Stage();
        stage.setTitle("Graph: " + KGService.localName(individualUri));
        stage.setWidth(1000);
        stage.setHeight(700);

        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    onPageLoaded(bridge, individualUri);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error initializing graph view", e);
                }
            } else if (newState == Worker.State.FAILED) {
                LOG.log(Level.SEVERE, "Failed to load graph-view.html");
            }
        });

        try {
            String htmlContent = HtmlLoader.load("graph-view.html");
            webEngine.loadContent(htmlContent);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to load graph view HTML", e);
            webEngine.loadContent("<html><body><h1>Error loading view</h1><p>" + 
                                e.getMessage() + "</p></body></html>");
        }

        Scene scene = new Scene(webView);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Attaches the JavaScript bridge and initializes the graph with the individual's neighborhood.
     */
    @SuppressWarnings("removal")
    private void onPageLoaded(JavaBridge bridge, String individualUri) {
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");
            if (window != null) {
                window.setMember("javaBridge", bridge);
                webEngine.executeScript("initGraph('" + escapeJs(individualUri) + "')");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error attaching bridge to graph view", e);
        }
    }

    public Stage getStage() {
        return stage;
    }

    /**
     * Escapes a string for safe use in JavaScript code.
     */
    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
