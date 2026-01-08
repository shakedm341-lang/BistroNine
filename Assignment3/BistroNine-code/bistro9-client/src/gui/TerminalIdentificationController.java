package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TerminalIdentificationController {

    private ClientController client;

    public void setClient(ClientController client) {
        this.client = client;
    }

    @FXML
    void handleSubscriber(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setClient(this.client);
            loginController.setMode(LoginController.Mode.TERMINAL);

            switchScene(event, root, "BistroNine - Subscriber Login");
        } catch (Exception e) {
            System.out.println("Error loading Login Screen for Terminal Mode:");
            e.printStackTrace();
        }
    }

    @FXML
    void handleGuest(ActionEvent event) {
        BaseTerminalController.setUserType(BaseTerminalController.UserType.GUEST);
        goToTerminalMenu(event);
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/MainSelection.fxml"));
            Parent root = loader.load();

            MainSelectionController controller = loader.getController();
            controller.setClient(this.client);

            switchScene(event, root, "BistroNine - Select Mode");
        } catch (Exception e) {
            System.out.println("Error returning to Main Selection:");
            e.printStackTrace();
        }
    }

    private void goToTerminalMenu(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalMenu.fxml"));
            Parent root = loader.load();

            TerminalMenuController controller = loader.getController();
            controller.setClient(this.client);

            switchScene(event, root, "BistroNine - Terminal Mode");
        } catch (Exception e) {
            System.out.println("Error loading Terminal Menu:");
            e.printStackTrace();
        }
    }

    private void switchScene(ActionEvent event, Parent root, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle(title);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
    }
}

