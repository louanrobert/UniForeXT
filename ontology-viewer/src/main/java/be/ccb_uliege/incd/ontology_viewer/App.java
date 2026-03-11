package be.ccb_uliege.incd.ontology_viewer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Main JavaFX application entry point.
 * Opens a file chooser to select a Turtle (.ttl) file,
 * then shows the Timeline view. Double-clicking items
 * opens Graph views in separate windows.
 */
public class App extends Application {

    private OntologyService ontologyService;
    private JavaBridge bridge;

    @Override
    public void start(Stage primaryStage) {
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

        ontologyService = new OntologyService(ttlPath);
        bridge = new JavaBridge(ontologyService, this);

        TimelineView timelineView = new TimelineView(bridge);

        primaryStage.setTitle("Ontology Viewer — " + new File(ttlPath).getName());
        Scene scene = new Scene(timelineView.getRoot(), 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
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
