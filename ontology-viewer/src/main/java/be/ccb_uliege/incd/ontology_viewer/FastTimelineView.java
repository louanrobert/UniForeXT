package be.ccb_uliege.incd.ontology_viewer;

import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * High-performance timeline view optimized for very large event sets.
 * Uses a custom canvas renderer and clustering in the HTML layer.
 */
public class FastTimelineView {

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
                // Suppressed because JSObject is available in JavaFX runtime.
                @SuppressWarnings("removal")
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", bridge);
                webEngine.executeScript("initTimelineFast()");
            }
        });

        webEngine.loadContent(HtmlLoader.load("timeline-fast.html"));
    }

    public StackPane getRoot() {
        return root;
    }
}