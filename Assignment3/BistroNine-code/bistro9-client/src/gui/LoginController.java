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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameTxt;

    @FXML
    private PasswordField passwordTxt;

    private ClientController client;

    // Method to set the client reference
    public void setClient(ClientController client) {
        this.client = client;
     
        ClientController.loginController = this; 
    }

    @FXML
    void getLoginBtn(ActionEvent event) {
        String username = usernameTxt.getText();
        String password = passwordTxt.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "Missing Input", "Please enter both username and password.");
            return;
        }

        
        ArrayList<Object> credentials = new ArrayList<>();
        credentials.add(username); // Index 0
        credentials.add(password); // Index 1

        System.out.println("Sending login request for: " + username);

        
        if (client != null) {
            client.handleMessageFromBoundary(
                TypeMessage.CUSTOMER,    
                credentials, 
                Command.CHECK_LOGIN_DETAILS        
            );
        } else {
            showAlert(AlertType.ERROR, "Connection Error", "Client is not connected.");
        }
    }

    
    public void handleServerLoginResponse(Subscriber subscriber) {
        
        Platform.runLater(() -> {
            if (subscriber == null) {
                // Login failed
                showAlert(AlertType.ERROR, "Login Failed", "Invalid username or password.");
            } else {
                
                System.out.println("Login successful! User type: " + subscriber.getType());
                openDashboard(subscriber); 
            }
        });
    }

    private void openDashboard(Subscriber user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/UserDashboard.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the client and user data
            UserDashboardController controller = loader.getController();
            controller.setClient(client);
            
            controller.loadUserDetails(user); 

            Stage stage = (Stage) usernameTxt.getScene().getWindow();
            stage.setTitle("BistroNine Client - User Dashboard");
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to load the dashboard.");
        }
    }

    @FXML
    void getBackBtn(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainSelection.fxml"));
            Parent root = loader.load();

            MainSelectionController controller = loader.getController();
            controller.setClient(client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine Client - Main Menu");
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
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
            stage.setScene(scene);
            stage.show();
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Could not load guest terminal.");
        }
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}