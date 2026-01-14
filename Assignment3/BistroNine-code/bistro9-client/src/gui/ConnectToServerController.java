package gui;

import controller.ClientController;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Controller class for the server connection screen.
 * Handles user input for server IP and port, establishes the client connection,
 * and transitions to the main selection menu upon successful connection.
 */
public class ConnectToServerController {

    /** TextField for entering the server's IP address. */
    @FXML
    private TextField ipTxt;

    /** TextField for entering the server's port number. */
    @FXML
    private TextField portTxt;
    
    /** Reference to the shared client controller instance. */
    private ClientController client;

    /**
     * Attempts to connect to the server using the provided IP and port.
     * If successful, navigates the user to the Main Selection screen.
     * 
     * @param event The ActionEvent triggered by clicking the connect button.
     */
    @FXML
    void connectToServer(ActionEvent event) {
        String ip = ipTxt.getText();
        String portStr = portTxt.getText();

        // Validate that both fields are filled
        if (ip.isEmpty() || portStr.isEmpty()) {
            showAlert(AlertType.WARNING, "Input Required", "Please enter IP and Port.");
            return;
        }

        try {
            // Parse port and attempt connection
            int port = Integer.parseInt(portStr);
            client = new ClientController(ip, port);
            
            System.out.println("Connected successfully to " + ip);
            
            // Load the next scene: MainSelection
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainSelection.fxml"));
            Parent root = loader.load();

            // Dependency injection: Pass the established client connection to the next controller
            MainSelectionController selectionController = loader.getController();
            selectionController.setClient(client);

            // Configure and display the new stage
            Stage mainStage = new Stage();
            mainStage.setTitle("BistroNine Client - Main Menu");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            mainStage.setScene(scene);
            mainStage.setResizable(true);

            // Close the current (Connection) stage
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            // Show and center the new stage
            mainStage.show();
            mainStage.centerOnScreen();

        } catch (NumberFormatException nfe) {
            showAlert(AlertType.ERROR, "Invalid Port", "Port must be a number.");
        } catch (Exception e) {
            // Handle connection failures and display error messages to the user
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            showAlert(AlertType.ERROR, "Connection Failed", "Connection failed: " + msg);

            System.err.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Utility method to display standardized JavaFX alert dialogs.
     * 
     * @param type    The type of alert (e.g., ERROR, WARNING, INFORMATION).
     * @param title   The title of the alert window.
     * @param content The message content to be displayed in the alert.
     */
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // Keep the header clean
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Closes the application and terminates the process.
     * 
     * @param event The ActionEvent triggered by clicking the exit button.
     */
    @FXML
    void exitApplication(ActionEvent event) {
        System.out.println("Exiting application...");
        System.exit(0);
    }
}