package be.ccb_uliege.incd.uniforext_viewer.views;

import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.ccb_uliege.incd.uniforext_viewer.HtmlLoader;
import be.ccb_uliege.incd.uniforext_viewer.JavaBridge;

/**
 * High-performance timeline view optimized for very large event sets.
 * Uses a custom canvas renderer and clustering in the HTML layer.
 */
public class FastTimelineView {

    private static final Logger LOG = Logger.getLogger(FastTimelineView.class.getName());
    private static final String DARK_BG_STYLE = "-fx-background-color: #020617;";
    private final StackPane root;
    private final WebView webView;
    private final WebEngine webEngine;

    public FastTimelineView(JavaBridge bridge) {
        webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.setStyle(DARK_BG_STYLE);
        webView.setOpacity(0.0);

        root = new StackPane(webView);
        root.setStyle(DARK_BG_STYLE);

        // Ensure the WebView fills the available space provided by the root container
        webView.prefWidthProperty().bind(root.widthProperty());
        webView.prefHeightProperty().bind(root.heightProperty());

        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // Suppress console noise from the WebView's internal JS engine
        webEngine.setOnError(event -> {});

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    onPageLoaded(bridge);
                    webView.setOpacity(1.0);
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
    private void onPageLoaded(JavaBridge bridge) {
        try {
            @SuppressWarnings("removal") // can be ignored because JavaFX is already updated and ships with JSObject
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