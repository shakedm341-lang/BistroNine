package gui;

import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;

public class TerminalUtils {

    /**
     * Simulates a barcode scan by opening a text input dialog.
     * @param onScanComplete Callback to execute with the scanned ID.
     */
    public static void simulateBarcodeScan(Consumer<String> onScanComplete) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Barcode Reader Simulator");
        dialog.setHeaderText("Hardware Simulation: Barcode Scanned");
        dialog.setContentText("Please enter Customer ID / Subscriber ID:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(onScanComplete);
    }

    /**
     * Shows a success message to the user.
     */
    public static void showSuccess(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error message to the user.
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

