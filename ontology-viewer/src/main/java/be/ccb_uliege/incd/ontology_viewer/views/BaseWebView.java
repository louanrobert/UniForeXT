package be.ccb_uliege.incd.ontology_viewer.views;

import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import java.util.logging.Logger;

import be.ccb_uliege.incd.ontology_viewer.HtmlLoader;

import java.util.logging.Level;

/**
 * Abstract base class for WebView-based visualization views.
 * Eliminates boilerplate by handling common setup: WebView initialization,
 * JavaScript bridge attachment, and initialization function execution.
 *
 * Subclasses must:
 * 1. Specify the HTML resource file name (e.g., "timeline.html")
 * 2. Optionally override onLoadSuccess() for custom setup after page load
 * 3. Return the root pane via getRoot()
 */
public abstract class BaseWebView {

    private static final Logger LOG = Logger.getLogger(BaseWebView.class.getName());

    protected final BorderPane root;
    protected final WebView webView;
    protected final WebEngine webEngine;

    /**
     * Initialize a web view with a given bridge object.
     * Sets up the WebView, attaches the JavaScript bridge, and loads the HTML resource.
     *
     * @param bridge the object to expose to JavaScript (e.g., JavaBridge, ViewSelectorBridge)
     * @param bridgeName the name to expose the bridge as in JavaScript (e.g., "javaBridge", "viewSelector")
     * @param htmlFileName the HTML resource file name (e.g., "timeline.html")
     */
    protected BaseWebView(Object bridge, String bridgeName, String htmlFileName) {
        root = new BorderPane();
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                onPageLoaded(bridge, bridgeName);
                onLoadSuccess();
            } else if (newState == Worker.State.FAILED) {
                LOG.log(Level.SEVERE, "Failed to load HTML view: " + htmlFileName);
                onLoadFailure(webEngine.getLoadWorker().getException());
            }
        });

        String htmlUrl = HtmlLoader.resourceUrl(htmlFileName);
        if (htmlUrl != null) {
            webEngine.load(htmlUrl);
        } else {
            String htmlContent = HtmlLoader.load(htmlFileName);
            webEngine.loadContent(htmlContent);
        }
        root.setCenter(webView);
    }

    /**
     * Attaches the JavaScript bridge and executes the initialization function.
     * Called automatically when the page loads successfully.
     */
    @SuppressWarnings("removal")
    private void onPageLoaded(Object bridge, String bridgeName) {
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");
            if (window == null) {
                LOG.log(Level.WARNING, "Could not access JavaScript window object");
                return;
            }
            window.setMember(bridgeName, bridge);
            
            // Execute initialization function if subclass specifies one
            String initFunction = getInitFunctionName();
            if (initFunction != null && !initFunction.isEmpty()) {
                webEngine.executeScript(initFunction + "()");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error attaching JavaScript bridge", e);
        }
    }

    /**
     * Override to specify the JavaScript initialization function to call.
     * Default implementation returns null (no initialization).
     * Examples: "initTimeline", "initGraph", "initExplorer", "initQueryView"
     *
     * @return the name of the initialization function, or null
     */
    protected String getInitFunctionName() {
        return null;
    }

    /**
     * Override to add custom logic after the page loads successfully.
     * Called after the JavaScript bridge is attached.
     */
    protected void onLoadSuccess() {
        // Default: do nothing
    }

    /**
     * Override to handle page load failures.
     * Called if the HTML fails to load.
     */
    protected void onLoadFailure(Throwable exception) {
        LOG.log(Level.SEVERE, "Failed to load view", exception);
    }

    /**
     * Returns the root pane containing the WebView.
     */
    public BorderPane getRoot() {
        return root;
    }
}
