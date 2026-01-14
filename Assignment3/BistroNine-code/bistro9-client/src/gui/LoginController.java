package gui;

import java.util.ArrayList;

import controller.ClientController;
import data.Command;
import data.Subscriber;
import data.TypeMessage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the Login screen of the BistroNine application.
 * This controller handles both Remote mode (standard login) and Terminal mode (guest/subscriber login).
 * It manages user authentication, identification via barcode scanning, and navigation to dashboards.
 */
public class LoginController {

    /**
     * Defines the operational mode of the login screen.
     */
    public enum Mode {
        /** Remote access mode with full sidebar and dashboard navigation. */
        REMOTE,
        /** Terminal mode for on-site guest or subscriber identification. */
        TERMINAL
    }

    @FXML
    private TextField usernameTxt;

    @FXML
    private PasswordField passwordTxt;

    @FXML
    private Button btnGuest;

    @FXML
    private Button btnScanTag;

    @FXML
    private Button btnBack;

    @FXML
    private HBox orSeparator;

    @FXML
    private VBox sidebar;

    @FXML
    private HBox rootPane;

    /** Reference to the communication controller. */
    private ClientController client;
    
    /** Current UI mode (default is REMOTE). */
    private Mode mode = Mode.REMOTE;

    /**
     * Standard JavaFX initialization method.
     */
    @FXML
    public void initialize() {
        updateUI();
    }

    /**
     * Sets the client reference for communication with the server.
     * Also updates the static login controller reference in ClientController.
     * 
     * @param client The ClientController instance to use.
     */
    public void setClient(ClientController client) {
        this.client = client;
     
        ClientController.loginController = this; 
    }

    /**
     * Configures the UI mode (Remote or Terminal).
     * 
     * @param mode The desired Mode.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
        updateUI();
    }

    /**
     * Adjusts visibility and properties of UI elements based on the current mode.
     * TERMINAL mode hides the sidebar and guest button, and enables tag scanning.
     * REMOTE mode shows the full layout for standard user login.
     */
    private void updateUI() {
        if (mode == Mode.TERMINAL) {
            // Adjust layout for Terminal Mode (modal/small window)
            if (sidebar != null) {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            }
            if (rootPane != null) {
                rootPane.setPrefWidth(520.0);
                rootPane.setPrefHeight(500.0);
            }
            if (btnGuest != null) {
                btnGuest.setVisible(false);
                btnGuest.setManaged(false);
            }
            if (orSeparator != null) {
                orSeparator.setVisible(true);
                orSeparator.setManaged(true);
            }
            if (btnScanTag != null) {
                btnScanTag.setVisible(true);
                btnScanTag.setManaged(true);
                btnScanTag.setText("Scan Subscriber Card");
            }
            if (btnBack != null) {
                btnBack.setText("Close");
            }
        } else {
            // Adjust layout for Remote Mode (full screen dashboard login)
            if (sidebar != null) {
                sidebar.setVisible(true);
                sidebar.setManaged(true);
            }
            if (rootPane != null) {
                rootPane.setPrefWidth(900.0);
                rootPane.setPrefHeight(550.0);
            }
            if (btnGuest != null) {
                btnGuest.setVisible(true);
                btnGuest.setManaged(true);
            }
            if (orSeparator != null) {
                orSeparator.setVisible(true);
                orSeparator.setManaged(true);
            }
            if (btnScanTag != null) {
                btnScanTag.setVisible(false);
                btnScanTag.setManaged(false);
            }
            if (btnBack != null) {
                btnBack.setText("Back to Main Menu");
            }
        }
    }

    /**
     * Handles the scan tag button click. Simulates a barcode scan and 
     * sends an identification request to the server.
     * 
     * @param event The ActionEvent triggering this method.
     */
    @FXML
    void handleScanTag(ActionEvent event) {
        TerminalUtils.simulateBarcodeScan(id -> {
            if (id != null && !id.trim().isEmpty()) {
                try {
                    int subId = Integer.parseInt(id.trim());
                    ArrayList<Object> content = new ArrayList<>();
                    content.add(subId);
                    
                    if (client != null) {
                        // Send identification request to server
                        client.handleMessageFromBoundary(TypeMessage.CUSTOMER, content, 
                            Command.CHECK_LOGIN_DETAILSֹֹ_BY_TAG_READER);
                    } else {
                        showAlert(AlertType.ERROR, "Connection Error", "Client is not connected.");
                    }
                } catch (NumberFormatException e) {
                    showAlert(AlertType.ERROR, "Input Error", "Subscriber ID must be a number.");
                }
            }
        });
    }

    /**
     * Callback for when the server responds to an identification request (by tag).
     * 
     * @param response The response from the server, expected to be a Subscriber object.
     */
    public void onIdentificationResponse(Object response) {
        Platform.runLater(() -> {
            if (response instanceof Subscriber) {
                Subscriber sub = (Subscriber) response;
                handleServerLoginResponse(sub);
            } else {
                showAlert(AlertType.ERROR, "Identification Failed", "Could not identify subscriber. Please check the ID and try again.");
            }
        });
    }

    /**
     * Handles the standard login button click. Validates inputs and sends 
     * credentials to the server.
     * 
     * @param event The ActionEvent triggering this method.
     */
    @FXML
    void getLoginBtn(ActionEvent event) {
        String username = usernameTxt.getText();
        String password = passwordTxt.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "Missing Input", "Please enter both username and password.");
            return;
        }

        // Prepare credentials for the server
        ArrayList<Object> credentials = new ArrayList<>();
        credentials.add(username); // Index 0
        credentials.add(password); // Index 1

        System.out.println("Sending login request for: " + username);

        if (client != null) {
            // Send login request to server
            client.handleMessageFromBoundary(
                TypeMessage.CUSTOMER,    
                credentials, 
                Command.CHECK_LOGIN_DETAILS        
            );
        } else {
            showAlert(AlertType.ERROR, "Connection Error", "Client is not connected.");
        }
    }

    /**
     * Processes the login response from the server.
     * If successful, redirects the user based on the current mode.
     * 
     * @param subscriber The Subscriber object if login was successful, null otherwise.
     */
    public void handleServerLoginResponse(Subscriber subscriber) {
        Platform.runLater(() -> {
            if (subscriber == null) {
                // Login failed - invalid credentials or subscriber not found
                showAlert(AlertType.ERROR, "Login Failed", "Invalid username or password.");
            } else {
                // Login successful
                System.out.println("Login successful! User type: " + subscriber.getType());
                if (mode == Mode.TERMINAL) {
                    enterTerminalMenu(subscriber);
                } else {
                    openDashboard(subscriber); 
                }
            }
        });
    }

    /**
     * Transition logic for Terminal mode after a successful login.
     * Sets global session data and closes the login modal.
     * 
     * @param subscriber The logged-in Subscriber.
     */
    private void enterTerminalMenu(Subscriber subscriber) {
        // Set the subscriber details in the base terminal controller for session persistence
        BaseTerminalController.setUserType(BaseTerminalController.UserType.SUBSCRIBER);
        BaseTerminalController.currentSubscriberId = String.valueOf(subscriber.getCustomerId());
        BaseTerminalController.currentSubscriberName = subscriber.getFirstName() + " " + subscriber.getLastName();

        // When in TERMINAL mode (modal popup), we just close the stage.
        // The TerminalIdentificationController will detect the login success and switch the parent scene.
        Stage stage = (Stage) usernameTxt.getScene().getWindow();
        stage.close();
    }

    /**
     * Transition logic for Remote mode after a successful login.
     * Loads the User Dashboard and sets the scene.
     * 
     * @param user The logged-in Subscriber.
     */
    private void openDashboard(Subscriber user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/UserDashboard.fxml"));
            Parent root = loader.load();

            UserDashboardController controller = loader.getController();
            controller.setClient(client);
            controller.loadUserDetails(user);

            // Get current stage
            Stage currentStage = (Stage) usernameTxt.getScene().getWindow();
            
            // If this stage has an owner (it's a modal popup from Terminal Mode)
            Stage ownerStage = (Stage) currentStage.getOwner();
            
            if (ownerStage != null) {
                // Case: Login was opened as a modal from Terminal identification screen
                // Switch the owner (main window) to the Dashboard
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
                ownerStage.setScene(scene);
                ownerStage.setTitle("BistroNine Client - User Dashboard");
                ownerStage.setMaximized(true);
                ownerStage.show();
                
                // Close the login popup
                currentStage.close();
            } else {
                // Case: Standard standalone Remote mode login
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
                currentStage.setScene(scene);
                currentStage.setTitle("BistroNine Client - User Dashboard");
                
                // Transition the standalone window to a dashboard layout
                currentStage.setResizable(true);
                currentStage.setMaximized(true);
                currentStage.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to load the dashboard.");
        }
    }

    /**
     * Handles the back button click. Redirects to the main menu or closes 
     * the modal depending on the current mode.
     * 
     * @param event The ActionEvent triggering this method.
     */
    @FXML
    void getBackBtn(ActionEvent event) {
        // Get current stage
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // Case 1: Terminal Mode Modal Popup (it has an owner) - just close the login popup
        if (currentStage.getOwner() != null) {
            currentStage.close();
            return;
        }

        // Case 2: Standalone Remote Window - return to main selection screen
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainSelection.fxml"));
            Parent root = loader.load();

            MainSelectionController controller = loader.getController();
            controller.setClient(client);

            // Create a new Main Selection stage
            Stage mainStage = new Stage();
            mainStage.setTitle("BistroNine Client - Main Menu");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            mainStage.setScene(scene);
            mainStage.setResizable(true);
            mainStage.show();
            mainStage.centerOnScreen();

            // Close the current Remote stage
            currentStage.close();

        } catch (Exception e) {
            System.out.println("Error returning to Main Selection:");
            e.printStackTrace();
        }
    }
    
    /**
     * Handles the 'Enter as Guest' button click. Loads the Guest Terminal screen.
     * 
     * @param event The ActionEvent triggering this method.
     */
    @FXML
    void enterAsGuest(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GuestTerminalScreen.fxml"));
            Parent root = loader.load();

            GuestTerminalController terminalController = loader.getController();
            terminalController.setClient(this.client);
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine - Guest Terminal");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreen(false);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Could not load guest terminal.");
        }
    }

    /**
     * Utility method to display alerts to the user.
     * 
     * @param type    The type of alert.
     * @param title   The title of the alert window.
     * @param content The message content.
     */
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}