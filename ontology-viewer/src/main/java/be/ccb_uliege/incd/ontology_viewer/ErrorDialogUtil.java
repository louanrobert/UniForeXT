package be.ccb_uliege.incd.ontology_viewer;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import java.util.Optional;

/**
 * Utility for displaying user-friendly error dialogs.
 */
public final class ErrorDialogUtil {

    private ErrorDialogUtil() {
        // utility class
    }

    /**
     * Shows an error dialog with a brief message and optional detailed error information.
     *
     * @param title the dialog title
     * @param message the main error message
     * @param details optional detailed error information (can be null)
     */
    public static void showError(String title, String message, String details) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);

        if (details != null && !details.isEmpty()) {
            TextArea textArea = new TextArea(details);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefHeight(200);
            textArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10;");
            
            VBox expandableContent = new VBox();
            expandableContent.setPadding(new Insets(10));
            expandableContent.setSpacing(10);
            expandableContent.getChildren().add(textArea);
            
            alert.getDialogPane().setExpandableContent(expandableContent);
            alert.getDialogPane().setExpanded(true);
        }

        alert.showAndWait();
    }

    /**
     * Shows an error dialog with just a message (no details).
     */
    public static void showError(String title, String message) {
        showError(title, message, null);
    }

    /**
     * Shows a warning dialog.
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an info dialog.
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a confirmation dialog with Yes/No buttons.
     *
     * @return true if user clicked Yes, false otherwise
     */
    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
