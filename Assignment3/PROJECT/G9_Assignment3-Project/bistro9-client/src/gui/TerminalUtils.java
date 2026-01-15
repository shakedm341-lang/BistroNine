package gui;

import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;

/**
 * Utility class for terminal-related operations.
 * 
 * This class provides reusable helper methods designed to maintain consistency
 * across the restaurant terminal interface. It includes functionality for:
 * 1. Hardware Simulation: Mimicking peripheral devices like barcode scanners.
 * 2. User Feedback: Displaying standardized success and error notifications.
 * 
 * All methods in this class are static to allow easy access from any controller.
 */
public class TerminalUtils {

    /**
     * Simulates a barcode scan by opening a text input dialog.
     * 
     * In a production environment, this would interface with actual scanner hardware.
     * In the prototype, it prompts the user to manually enter an ID (e.g., Subscriber ID or Customer ID)
     * to simulate the scanning of a physical card or QR code.
     * 
     * @param onScanComplete A {@link Consumer} callback that is executed with the 
     *                       scanned/entered ID string if the user confirms the dialog.
     */
    public static void simulateBarcodeScan(Consumer<String> onScanComplete) {
        // Initialize a text input dialog to represent the scanning interface
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Barcode Reader Simulator");
        dialog.setHeaderText("Hardware Simulation: Barcode Scanned");
        dialog.setContentText("Please enter Customer ID / Subscriber ID:");

        // Show the dialog and wait for user input
        Optional<String> result = dialog.showAndWait();
        
        // If the user provided input and clicked OK, pass the result to the callback
        result.ifPresent(onScanComplete);
    }

    /**
     * Displays a standardized success notification to the user.
     * 
     * This method uses a JavaFX INFORMATION alert type and ensures consistent 
     * styling by omitting the header text.
     * 
     * @param title   The title text for the alert window.
     * @param message The detailed success message to display in the alert content.
     */
    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // Keep the header clean for simple notifications
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Displays a standardized error notification to the user.
     * 
     * This method uses a JavaFX ERROR alert type to signal failures or invalid 
     * operations to the user.
     * 
     * @param title   The title text for the error window.
     * @param message The detailed error description to display in the alert content.
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null); // Consistent clean header style
        alert.setContentText(message);
        alert.showAndWait();
    }
}
