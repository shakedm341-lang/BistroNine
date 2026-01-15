package gui;

import java.io.IOException;
import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Controller class for the Guest Terminal interface.
 * This class manages the navigation and interactions for guests using the terminal,
 * allowing them to make new reservations, leave the waitlist, and pay their bills.
 */
public class GuestTerminalController {

    /** The client controller used for communication with the server */
    private ClientController client;

    @FXML
    /** The container where different screens are loaded dynamically */
    private StackPane contentArea;

    /**
     * Sets the client controller and initializes the default view.
     * 
     * @param client The ClientController instance to be used
     */
    public void setClient(ClientController client) {
        this.client = client;
        // Default view: New Order / Reservation
        goToNewReservation(null);
    }

    /**
     * Navigates the guest to the New Reservation screen.
     * Loads the FXML and initializes the ReservationBoundry controller in guest mode.
     * 
     * @param event The action event that triggered this method (can be null)
     */
    @FXML
    void goToNewReservation(ActionEvent event) {
        try {
            // Unsubscribe existing boundary if any before loading new one
            ClientController.unsubscribeReservationBoundry(null);

            // Load the New Reservation FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/NewReservation.fxml"));
            Parent root = loader.load();

            // Configure the controller for guest reservation
            ReservationBoundry controller = loader.getController();
            controller.setClient(client);
            controller.initData(null, true, true); // Parameters: Guest, Customer Mode, Embedded

            // Display the new screen in the content area
            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigates the guest to the Cancel Registration screen.
     * 
     * @param event The action event that triggered this method
     */
    @FXML
    void goToCancelRegistration(ActionEvent event) {
        try {
            // Unsubscribe existing boundary if any
            ClientController.unsubscribeReservationBoundry(null);

            // Load the Cancel Registration FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CancelRegistrationScreen.fxml"));
            Parent root = loader.load();

            // Configure the controller
            CancelRegistrationController controller = loader.getController();
            controller.setClient(client);
            controller.setTerminalMode(true);

            // Display the screen
            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigates the guest to the Pay Bill screen.
     * 
     * @param event The action event that triggered this method
     */
    @FXML
    void goToPayBill(ActionEvent event) {
        try {
            // Unsubscribe existing boundary if any
            ClientController.unsubscribeReservationBoundry(null);

            // Load the Pay Bill FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/PayBillScreen.fxml"));
            Parent root = loader.load();

            // Set up dependencies for the controller
            PayBillController controller = loader.getController();
            controller.setTerminalDependencies(client);

            // Display the screen
            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the exit action, returning the user to the Login screen.
     * 
     * @param event The action event that triggered this method
     */
    @FXML
    void handleExit(ActionEvent event) {
        try {
            // Unsubscribe existing boundary if any
            ClientController.unsubscribeReservationBoundry(null);

            // Load the Login Screen FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();

            // Set the client on the login controller
            LoginController loginController = loader.getController();
            loginController.setClient(client);

            // Get the current stage and switch the scene
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine Client - Login Screen");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            stage.setScene(scene);
            
            // Reset window state
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.show();
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

