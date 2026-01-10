package gui;

import java.io.IOException;
import controller.ClientController;
import data.Subscriber;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GuestTerminalController {

    private ClientController client;

    @FXML
    private StackPane contentArea;

    public void setClient(ClientController client) {
        this.client = client;
        // Default view: New Order / Reservation
        goToNewReservation(null);
    }

    @FXML
    void goToNewReservation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/NewReservation.fxml"));
            Parent root = loader.load();

            ReservationBoundry controller = loader.getController();
            controller.setClient(client);
            controller.initData(null, true, true); // Guest, Customer Mode, Embedded

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToLeaveWaitlist(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LeaveWaitlistScreen.fxml"));
            Parent root = loader.load();

            LeaveWaitlistController controller = loader.getController();
            controller.setClient(client);
            controller.setTerminalMode(true);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToPayBill(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/PayBillScreen.fxml"));
            Parent root = loader.load();

            PayBillController controller = loader.getController();
            controller.setTerminalDependencies(client);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleExit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/LoginScreen.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setClient(client);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("BistroNine Client - Login Screen");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.show();
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

