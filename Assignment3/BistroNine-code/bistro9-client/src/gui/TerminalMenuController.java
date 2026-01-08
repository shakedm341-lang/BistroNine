package gui;

import controller.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class TerminalMenuController {

    private ClientController client;

    @FXML
    private Button btnGetTable;

    @FXML
    private Button btnJoinWaitlist;

    @FXML
    private Button btnPayBill;

    @FXML
    private Button btnExit;

    @FXML
    private Label lblMode;

    public void setClient(ClientController client) {
        this.client = client;
        updateModeLabel();
    }

    private void updateModeLabel() {
        if (lblMode != null) {
            if (BaseTerminalController.currentUserType == BaseTerminalController.UserType.SUBSCRIBER) {
                String subId = BaseTerminalController.currentSubscriberId;
                lblMode.setText("Mode: Subscriber" + (subId != null ? " (ID: " + subId + ")" : " (Not Identified)"));
            } else {
                lblMode.setText("Mode: Guest");
            }
        }
    }

    @FXML
    void handleGetTable(ActionEvent event) {
        switchScene(event, "/gui/GetTableScreen.fxml", "Terminal - Get a Table");
    }

    @FXML
    void handleJoinWaitlist(ActionEvent event) {
        switchScene(event, "/gui/JoinWaitlistScreen.fxml", "Terminal - Join Waiting List");
    }

    @FXML
    void handleLeaveWaitlist(ActionEvent event) {
        switchScene(event, "/gui/LeaveWaitlistScreen.fxml", "Terminal - Leave Waiting List");
    }

    @FXML
    void handlePayBill(ActionEvent event) {
        switchScene(event, "/gui/PayBillScreen.fxml", "Terminal - Pay Bill");
    }

    @FXML
    void handleExit(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TerminalIdentificationScreen.fxml"));
            Parent root = loader.load();

            TerminalIdentificationController controller = loader.getController();
            controller.setClient(this.client);

            switchScene(event, root, "BistroNine - Terminal Mode");
        } catch (Exception e) {
            System.out.println("Error returning to Terminal Identification:");
            e.printStackTrace();
        }
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof BaseTerminalController) {
                ((BaseTerminalController) controller).setClient(this.client);
            }

            switchScene(event, root, title);
        } catch (Exception e) {
            System.err.println("Error loading screen: " + fxmlPath);
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

