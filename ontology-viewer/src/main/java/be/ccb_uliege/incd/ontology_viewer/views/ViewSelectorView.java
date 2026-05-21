package be.ccb_uliege.incd.ontology_viewer.views;

import be.ccb_uliege.incd.ontology_viewer.App;

/**
 * Startup view that displays a selection of available views.
 * Users pick which view they want to navigate to.
 */
public class ViewSelectorView extends BaseWebView {

    private final App app;

    public ViewSelectorView(App app) {
        super(new ViewSelectorBridge(app), "viewSelector", "view-selector.html");
        this.app = app;
    }

    /**
     * Bridge exposed to JS so the web page can trigger view navigation.
     */
    public static class ViewSelectorBridge {
        private final App app;

        public ViewSelectorBridge(App app) {
            this.app = app;
        }

        public void openView(String viewName) {
            javafx.application.Platform.runLater(() -> app.navigateToView(viewName));
        }
    }
}
