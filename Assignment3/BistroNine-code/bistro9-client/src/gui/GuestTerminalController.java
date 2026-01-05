package gui;

import controller.ClientController;
import data.Subscriber;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class GuestTerminalController {

    private ClientController client;

    @FXML
    private TabPane guestTabPane;

    @FXML
    private ReservationBoundry reservationScreenController;

    @FXML
    private LeaveWaitlistController leaveWaitlistScreenController;

    @FXML
    private PayBillController payBillScreenController;

    public void setClient(ClientController client) {
        this.client = client;
        
        // Initialize sub-controllers
        if (reservationScreenController != null) {
            reservationScreenController.setClient(client);
            reservationScreenController.initData(null, true, true); // Guest, Customer Mode, Embedded
        }
        
        if (leaveWaitlistScreenController != null) {
            leaveWaitlistScreenController.setClient(client);
            leaveWaitlistScreenController.setTerminalMode(true);
        }
        
        if (payBillScreenController != null) {
            payBillScreenController.setTerminalDependencies(client);
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
            stage.setScene(scene);
            stage.show();
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

