package be.ccb_uliege.incd.ontology_viewer.views;

import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.ccb_uliege.incd.ontology_viewer.HtmlLoader;
import be.ccb_uliege.incd.ontology_viewer.JavaBridge;

/**
 * High-performance timeline view optimized for very large event sets.
 * Uses a custom canvas renderer and clustering in the HTML layer.
 */
public class FastTimelineView {

    private static final Logger LOG = Logger.getLogger(FastTimelineView.class.getName());
    private final StackPane root;
    private final WebView webView;
    private final WebEngine webEngine;

    public FastTimelineView(JavaBridge bridge) {
        webView = new WebView();
        webView.setContextMenuEnabled(false);

        // Let the WebView fill whatever space is given to it
        webView.prefWidthProperty().bind(webView.widthProperty());
        webView.prefHeightProperty().bind(webView.heightProperty());

        root = new StackPane(webView);
        root.setStyle("-fx-background-color: #0b0f14;");

        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Suppress console noise from the WebView's internal JS engine
        webEngine.setOnError(event -> {});

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    onPageLoaded(bridge);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error initializing fast timeline view", e);
                }
            } else if (newState == Worker.State.FAILED) {
                LOG.log(Level.SEVERE, "Failed to load timeline-fast.html");
            }
        });

        try {
            String htmlUrl = HtmlLoader.resourceUrl("timeline-fast.html");
            if (htmlUrl != null) {
                webEngine.load(htmlUrl);
            } else {
                String htmlContent = HtmlLoader.load("timeline-fast.html");
                webEngine.loadContent(htmlContent);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to load fast timeline HTML", e);
            webEngine.loadContent("<html><body><h1>Error loading view</h1><p>" + 
                                e.getMessage() + "</p></body></html>");
        }
    }

    /**
     * Attaches the JavaScript bridge and initializes the fast timeline.
     */
    @SuppressWarnings("removal")
    private void onPageLoaded(JavaBridge bridge) {
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");
            if (window != null) {
                window.setMember("javaBridge", bridge);
                webEngine.executeScript("initTimelineFast()");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error attaching bridge to fast timeline view", e);
        }
    }

    public StackPane getRoot() {
        return root;
    }
}