package be.ccb_uliege.incd.ontology_viewer;

import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Startup view that displays a selection of available views.
 * Users pick which view they want to navigate to.
 */
public class ViewSelectorView {

    private final BorderPane root;
    private final WebView webView;
    private final WebEngine webEngine;
    private final App app;

    @SuppressWarnings("unused")
    private ViewSelectorBridge bridgeRef;

    @SuppressWarnings("removal")
    public ViewSelectorView(App app) {
        this.app = app;
        this.bridgeRef = new ViewSelectorBridge();

        root = new BorderPane();
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("viewSelector", bridgeRef);
            }
        });

        webEngine.loadContent(HtmlLoader.load("view-selector.html"));
        root.setCenter(webView);
    }

    public BorderPane getRoot() {
        return root;
    }

    /**
     * Bridge exposed to JS so the web page can trigger view navigation.
     */
    public class ViewSelectorBridge {
        public void openView(String viewName) {
            javafx.application.Platform.runLater(() -> app.navigateToView(viewName));
        }
    }

}
