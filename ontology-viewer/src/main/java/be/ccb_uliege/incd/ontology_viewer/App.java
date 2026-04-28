package be.ccb_uliege.incd.ontology_viewer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Main JavaFX application entry point.
 * Opens a file chooser to select a Turtle (.ttl) file,
 * then shows a view selector. From there the user can open
 * the Timeline view, Event Explorer, or Query view. Double-clicking
 * items opens Graph views in separate windows.
 */
public class App extends Application {

    private KGService kgService;
    private JavaBridge bridge;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Try command line argument first, then file chooser
        String ttlPath = null;
        var params = getParameters().getRaw();
        if (!params.isEmpty()) {
            ttlPath = params.get(0);
        }

        if (ttlPath == null || !new File(ttlPath).exists()) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Open Turtle (.ttl) Ontology File");
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Turtle Files", "*.ttl"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            // Default to workspace directory
            File defaultDir = new File(System.getProperty("user.dir"));
            if (defaultDir.exists()) {
                chooser.setInitialDirectory(defaultDir);
            }
            File chosen = chooser.showOpenDialog(primaryStage);
            if (chosen == null) {
                System.out.println("No file selected. Exiting.");
                System.exit(0);
                return;
            }
            ttlPath = chosen.getAbsolutePath();
        }

        System.out.println("Loading ontology from: " + ttlPath);

        kgService = new KGService(ttlPath);
        bridge = new JavaBridge(kgService, this);

        primaryStage.setTitle("Ontology Viewer \u2014 " + new File(ttlPath).getName());

        // Show the view selector as the startup screen
        showViewSelector();
        primaryStage.show();
    }

    /**
     * Shows the startup view selector screen.
     */
    public void showViewSelector() {
        ViewSelectorView selectorView = new ViewSelectorView(this);
        primaryStage.setScene(new Scene(selectorView.getRoot(), 1400, 800));
    }

    /**
     * Navigates to the specified view.
     * Called from the ViewSelectorView when a user picks a view.
     */
    public void navigateToView(String viewName) {
        switch (viewName) {
            case "timeline" -> {
                TimelineView timelineView = new TimelineView(bridge);
                primaryStage.setScene(new Scene(timelineView.getRoot(), 1400, 800));
            }
            case "explorer" -> {
                EventExplorerView explorerView = new EventExplorerView(bridge);
                primaryStage.setScene(new Scene(explorerView.getRoot(), 1400, 800));
            }
            case "query" -> {
                QueryView queryView = new QueryView(bridge);
                primaryStage.setScene(new Scene(queryView.getRoot(), 1400, 800));
            }
            default -> System.out.println("Unknown view: " + viewName);
        }
    }

    /**
     * Opens a new GraphView window for the given individual URI.
     * Called from JavaBridge (via JS bridge) on the FX Application Thread.
     */
    public void openGraphView(String individualUri) {
        new GraphView(bridge, individualUri);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
