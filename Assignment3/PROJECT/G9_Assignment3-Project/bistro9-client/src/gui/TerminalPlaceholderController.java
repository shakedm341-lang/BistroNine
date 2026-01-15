package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A placeholder controller for terminal screens that are under development.
 * This class provides basic navigation functionality to return to the main terminal menu.
 * 
 * It is used as a fallback for features that have UI buttons but no dedicated
 * functional screens yet, ensuring the user can always navigate back.
 */
public class TerminalPlaceholderController {

    /**
     * Reference to the client controller for communication with the server.
     * This allows the client state to be preserved across scene transitions.
     */
    private ClientController client;

    /**
     * Sets the client controller for this view.
     * 
     * @param client The ClientController instance to use.
     */
    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * Navigates the user back to the Terminal Menu screen.
     * 
     * This method is triggered by the "Back" button in the placeholder UI.
     * It performs the following steps:
     * 1. Loads the TerminalMenu FXML.
     * 2. Injects the client controller into the new TerminalMenuController.
     * 3. Updates the stage title and scene.
     * 4. Re-applies the global CSS stylesheet.
     * 
     * @param event The action event triggered by the "Back" button.
     */
    @FXML
    void handleBack(ActionEvent event) {
        try {
            // Initialize the loader for the Terminal Menu FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalMenu.fxml"));
            Parent root = loader.load();

            // Retrieve the controller of the newly loaded scene and inject the shared client instance
            TerminalMenuController controller = loader.getController();
            controller.setClient(this.client);

            // Get the current window (Stage) from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // Set the window title for the terminal mode
            stage.setTitle("BistroNine - Terminal Mode");
            
            // Create a new scene with the loaded root layout
            Scene scene = new Scene(root);
            
            // Ensure the global stylesheet is applied for consistent visual style
            String cssPath = getClass().getResource("/gui/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            
            // Switch the stage to the new scene and center it on the screen
            stage.setScene(scene);
            stage.show();
            stage.centerOnScreen();
            
        } catch (Exception e) {
            // Log the error for debugging purposes if the transition fails
            System.err.println("Error returning to Terminal Menu from Placeholder:");
            e.printStackTrace();
        }
    }
}
