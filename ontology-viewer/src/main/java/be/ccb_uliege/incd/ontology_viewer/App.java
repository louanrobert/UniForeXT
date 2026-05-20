package be.ccb_uliege.incd.ontology_viewer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.ccb_uliege.incd.ontology_viewer.views.EventExplorerView;
import be.ccb_uliege.incd.ontology_viewer.views.FastTimelineView;
import be.ccb_uliege.incd.ontology_viewer.views.GraphView;
import be.ccb_uliege.incd.ontology_viewer.views.QueryView;
import be.ccb_uliege.incd.ontology_viewer.views.UndatedEventsView;
import be.ccb_uliege.incd.ontology_viewer.views.ViewSelectorView;

/**
 * Main JavaFX application entry point.
 * Opens a file chooser to select a Turtle (.ttl) file,
 * then shows a view selector. From there the user can open
 * the Timeline view, Event Explorer, or Query view. Double-clicking
 * items opens Graph views in separate windows.
 *
 * Features:
 * - Validates file existence and readability before loading
 * - Shows progress indicator during ontology loading
 * - Provides user-friendly error dialogs for malformed files or I/O errors
 */
public class App extends Application {

    private static final Logger LOG = Logger.getLogger(App.class.getName());
    private static final int DEFAULT_WIDTH = 1400;
    private static final int DEFAULT_HEIGHT = 800;
    private static final Color DARK_SCENE_FILL = Color.web("#020617");

    private KGService kgService;
    private JavaBridge bridge;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Try command line argument first, then file chooser
        String ttlPath = selectTurtleFile();
        
        if (ttlPath == null) {
            // User cancelled without selecting a file
            Platform.exit();
            return;
        }

        // Load the ontology with progress indicator
        loadOntologyAsync(ttlPath);
        
        primaryStage.show();
    }

    /**
     * Prompts user to select a Turtle file via file chooser.
     * Returns the file path, or null if user cancels.
     */
    private String selectTurtleFile() {
        // Try command line argument first
        var params = getParameters().getRaw();
        if (!params.isEmpty()) {
            String cmdLineArg = params.get(0);
            if (validateFileReadable(cmdLineArg)) {
                return cmdLineArg;
            }
        }

        // Fall back to file chooser
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open a knowledge graph");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Knowledge Graph Files", "*.ttl", "*.rdf", "*.owl"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File defaultDir = new File(System.getProperty("user.dir"));
        if (defaultDir.exists()) {
            chooser.setInitialDirectory(defaultDir);
        }
        
        File chosen = chooser.showOpenDialog(primaryStage);
        return chosen != null ? chosen.getAbsolutePath() : null;
    }

    /**
     * Validates that a file exists and is readable.
     * Shows error dialog if validation fails.
     */
    private boolean validateFileReadable(String ttlPath) {
        try {
            File f = new File(ttlPath);
            if (!f.exists()) {
                ErrorDialogUtil.showError("File Not Found", 
                    "The file does not exist: " + ttlPath);
                return false;
            }
            if (!f.isFile()) {
                ErrorDialogUtil.showError("Invalid File", 
                    "The path is not a file: " + ttlPath);
                return false;
            }
            if (!Files.isReadable(Paths.get(ttlPath))) {
                ErrorDialogUtil.showError("Permission Denied", 
                    "The file is not readable: " + ttlPath);
                return false;
            }
            return true;
        } catch (Exception e) {
            ErrorDialogUtil.showError("File Validation Error", 
                "Could not validate file: " + e.getMessage(), e.toString());
            return false;
        }
    }

    /**
     * Loads the Turtle ontology file asynchronously with a progress indicator.
     * Shows appropriate error dialogs if loading fails.
     */
    private void loadOntologyAsync(String ttlPath) {
        // Create progress screen
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(100, 100);
        
        Label statusLabel = new Label("Loading knowledge graph...");
        statusLabel.setStyle("-fx-font-size: 14;");
        
        VBox progressPane = new VBox(20);
        progressPane.setAlignment(Pos.CENTER);
        progressPane.setStyle("-fx-padding: 40;");
        progressPane.getChildren().addAll(progress, statusLabel);
        
        Scene progressScene = new Scene(progressPane, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        progressScene.setFill(DARK_SCENE_FILL);
        primaryStage.setTitle("Ontology Viewer — Loading...");
        primaryStage.setScene(progressScene);

        // Load in background task
        Task<KGService> loadTask = new Task<KGService>() {
            @Override
            protected KGService call() throws Exception {
                LOG.info("Loading knowledge graph from: " + ttlPath);
                return new KGService(ttlPath);
            }
        };

        loadTask.setOnSucceeded(event -> {
            try {
                kgService = loadTask.getValue();
                bridge = new JavaBridge(kgService, this);
                primaryStage.setTitle("Ontology Viewer — " + new File(ttlPath).getName());
                showViewSelector();
                LOG.info("Ontology loaded successfully");
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error initializing after load", e);
                ErrorDialogUtil.showError("Initialization Error", 
                    "Failed to initialize application: " + e.getMessage());
                Platform.exit();
            }
        });

        loadTask.setOnFailed(event -> {
            Throwable ex = loadTask.getException();
            LOG.log(Level.SEVERE, "Failed to load knowledge graph", ex);
            
            String title = "Failed to Load Knowledge Graph";
            String message = "The Turtle file could not be parsed.";
            String details = null;
            
            if (ex != null) {
                if (ex instanceof IllegalArgumentException) {
                    message = ex.getMessage();
                    details = getDetailedErrorInfo(ex);
                } else {
                    message += "\n\n" + ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    details = getStackTraceAsString(ex);
                }
            }
            
            ErrorDialogUtil.showError(title, message, details);
            Platform.exit();
        });

        Thread loaderThread = new Thread(loadTask, "OntologyLoader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    /**
     * Extracts detailed error information from an exception.
     */
    private String getDetailedErrorInfo(Throwable e) {
        if (e.getCause() != null) {
            return e.getCause().getClass().getSimpleName() + ": " + 
                   e.getCause().getMessage() + "\n\n" + 
                   getStackTraceAsString(e.getCause());
        }
        return getStackTraceAsString(e);
    }

    /**
     * Converts exception stack trace to string for display.
     */
    private String getStackTraceAsString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Shows the startup view selector screen.
     */
    public void showViewSelector() {
        ViewSelectorView selectorView = new ViewSelectorView(this);
        primaryStage.setScene(createDarkScene(selectorView.getRoot()));
    }

    /**
     * Navigates to the specified view.
     * Called from the ViewSelectorView when a user picks a view.
     */
    public void navigateToView(String viewName) {
        try {
            switch (viewName) {
                case "timeline-fast" -> {
                    FastTimelineView timelineFastView = new FastTimelineView(bridge);
                    primaryStage.setScene(createDarkScene(timelineFastView.getRoot()));
                }
                case "explorer" -> {
                    EventExplorerView explorerView = new EventExplorerView(bridge);
                    primaryStage.setScene(createDarkScene(explorerView.getRoot()));
                }
                case "undated" -> {
                    UndatedEventsView undatedView = new UndatedEventsView(bridge);
                    primaryStage.setScene(createDarkScene(undatedView.getRoot()));
                }
                case "query" -> {
                    QueryView queryView = new QueryView(bridge);
                    primaryStage.setScene(createDarkScene(queryView.getRoot()));
                }
                default -> LOG.warning("Unknown view: " + viewName);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to open view: " + viewName, e);
            ErrorDialogUtil.showError("View Error", 
                "Failed to open view: " + viewName, e.getMessage());
        }
    }

    /**
     * Opens a new GraphView window for the given individual URI.
     * Called from JavaBridge (via JS bridge) on the FX Application Thread.
     */
    public void openGraphView(String individualUri) {
        try {
            new GraphView(bridge, individualUri);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to open graph view", e);
            ErrorDialogUtil.showError("Graph View Error", 
                "Failed to open graph view: " + e.getMessage());
        }
    }

    private Scene createDarkScene(Parent root) {
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.setFill(DARK_SCENE_FILL);
        return scene;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
