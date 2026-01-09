package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

import controller.ClientController;
import data.Bill;
import data.Command;
import data.Subscriber;
import data.TypeMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Pay Bill screen.
 * This screen is generic and can be called from the Terminal or the User Dashboard.
 */
public class PayBillController extends BaseTerminalController implements Initializable {

    @FXML
    private VBox searchSection;
    @FXML
    private TextField txtConfCode;
    @FXML
    private Button btnSearch;

    @FXML
    private VBox billDetailsSection;
    @FXML
    private Label lblBillId;
    @FXML
    private Label lblReservationId;
    @FXML
    private Label lblOriginalAmount;
    @FXML
    private Label lblDiscount;
    @FXML
    private Label lblDiscountType;
    @FXML
    private Label lblTotalAmount;
    @FXML
    private ComboBox<String> comboPaymentMethod;
    @FXML
    private Button btnPay;
    @FXML
    private Button btnBackOnly;

    private Subscriber currentUser;
    private UserDashboardController dashboardController;
    private Bill currentBill;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	ClientController.payBillController = this;
    	comboPaymentMethod.setItems(FXCollections.observableArrayList("Credit Card", "Cash", "App"));
        comboPaymentMethod.getSelectionModel().selectFirst();
        
        // Initial UI state
        billDetailsSection.setVisible(false);
        billDetailsSection.setManaged(false);
        searchSection.setVisible(true);
        searchSection.setManaged(true);
        btnBackOnly.setVisible(true);
    }

    @Override
    public void setClient(ClientController client) {
        super.setClient(client);
    }

    /**
     * Sets dependencies when called from Guest Terminal.
     */
    public void setTerminalDependencies(ClientController client) {
        setClient(client);
        
        Platform.runLater(() -> {
            btnBackOnly.setVisible(false);
            btnBackOnly.setManaged(false);
        });
    }

    /**
     * Sets dependencies when called from User Dashboard.
     */
    public void setDependencies(ClientController client, Subscriber user, UserDashboardController dashboard) {
        setClient(client);
        this.currentUser = user;
        this.dashboardController = dashboard;
        
        // Hide terminal back button when inside dashboard
        btnBackOnly.setVisible(false);
        btnBackOnly.setManaged(false);
    }

    /**
     * Handles searching for a bill by confirmation code.
     */
    @FXML
    void handleSearch(ActionEvent event) {
        String codeStr = txtConfCode.getText().trim();
        if (codeStr.isEmpty()) {
            TerminalUtils.showError("Input Error", "Please enter a confirmation code.");
            return;
        }

        try {
            int confCode = Integer.parseInt(codeStr);
            ArrayList<Object> params = new ArrayList<>();
            params.add(confCode);

            // Register this controller with ClientController (assuming static field exists or will be added)
            // For now, we assume ClientController will be updated by the responsible party.
            // ClientController.payBillController = this; 

            client.handleMessageFromBoundary(TypeMessage.BILL, params, Command.SHOW_BILL);
            
            btnSearch.setDisable(true);
            btnSearch.setText("Searching...");
        } catch (NumberFormatException e) {
            TerminalUtils.showError("Input Error", "Confirmation code must be a number.");
        }
    }

    /**
     * Displays the bill details in the UI.
     */
    public void displayBill(Bill bill) {
        Platform.runLater(() -> {
            this.currentBill = bill;
            btnSearch.setDisable(false);
            btnSearch.setText("Fetch Bill");

            if (bill == null) {
                TerminalUtils.showError("Not Found", "No bill found for the provided confirmation code.");
                return;
            }

            if (bill.isPaid()) {
                TerminalUtils.showSuccess("Already Paid", "This bill has already been paid.");
                return;
            }

            lblBillId.setText(String.valueOf(bill.getBillId()));
            lblReservationId.setText(String.valueOf(bill.getReservationId()));
            lblOriginalAmount.setText(String.format("%.2f ₪", bill.getTotalAmount()));
            lblDiscountType.setText(bill.getDiscountType() != null ? bill.getDiscountType() : "Standard");
            lblDiscount.setText(String.format("%.0f%% (-%.2f ₪)", 
                bill.getDiscountSize(), 
                bill.getTotalAmount() - bill.getTotalAmountAfterDiscount()));
            lblTotalAmount.setText(String.format("%.2f ₪", bill.getTotalAmountAfterDiscount()));

            searchSection.setVisible(false);
            searchSection.setManaged(false);
            billDetailsSection.setVisible(true);
            billDetailsSection.setManaged(true);
            btnBackOnly.setVisible(false);
        });
    }

    /**
     * Handles the payment process.
     */
    @FXML
    void handlePay(ActionEvent event) {
        String method = comboPaymentMethod.getValue();
        System.out.println("Debug: Payment method selected: [" + method + "]");
        
        if (method != null && method.trim().equalsIgnoreCase("Credit Card")) {
            simulateCreditCardPopup(event);
        } else {
            processPayment(method);
        }
    }

    /**
     * Simulates a credit card entry popup.
     */
    private void simulateCreditCardPopup(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Credit Card Simulation");
        dialog.setHeaderText("Please enter your card details");
        
        // Set the owner so it pops up in front of the current window
        dialog.initOwner(((Node) event.getSource()).getScene().getWindow());

        ButtonType payButtonType = new ButtonType("Confirm Payment", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(payButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField cardNumber = new TextField();
        cardNumber.setPromptText("Card Number (16 digits)");
        TextField cvv = new TextField();
        cvv.setPromptText("CVV");
        TextField expiry = new TextField();
        expiry.setPromptText("MM/YY");

        grid.add(new Label("Card Number:"), 0, 0);
        grid.add(cardNumber, 1, 0);
        grid.add(new Label("CVV:"), 0, 1);
        grid.add(cvv, 1, 1);
        grid.add(new Label("Expiry:"), 0, 2);
        grid.add(expiry, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == payButtonType) {
            if (cardNumber.getText().isEmpty() || cvv.getText().isEmpty() || expiry.getText().isEmpty()) {
                TerminalUtils.showError("Validation Error", "All fields are required.");
                simulateCreditCardPopup(event); // Re-open
            } else {
                processPayment("Credit");
            }
        }
    }

    /**
     * Sends the payment request to the server.
     */
    private void processPayment(String method) {
        ArrayList<Object> params = new ArrayList<>();
        params.add(currentBill.getBillId());
        
        // Convert to lowercase to match server-side expectations (e.g., "credit", "cash", "app")
        String formattedMethod = (method != null) ? method.trim().toLowerCase() : "credit";
        params.add(formattedMethod);

        if (client != null) {   
            client.handleMessageFromBoundary(TypeMessage.BILL, params, Command.PAY_BILL);
        } else {
            TerminalUtils.showError("Error", "Client is not connected.");
        }
        btnPay.setDisable(true);
        btnPay.setText("Processing...");
    }

    /**
     * Handles the server response for showing bill details.
     */
    public void handleShowBillResponse(Bill bill) {
        Platform.runLater(() -> {
            displayBill(bill);
        });
    }

    /**
     * Handles the server response for the payment process.
     */
    public void handlePayBillResponse(boolean success) {
        Platform.runLater(() -> {
            btnPay.setDisable(false);
            btnPay.setText("PAY NOW");
            if (success) {
                TerminalUtils.showSuccess("Payment Successful", "Thank you! Your bill has been paid and your table is now available.");
                resetUI();
            } else {
                TerminalUtils.showError("Payment Failed", "There was an error processing your payment. Please try again.");
            }
        });
    }

    /**
     * Resets the UI to its initial search state and clears all data.
     */
    private void resetUI() {
        billDetailsSection.setVisible(false);
        billDetailsSection.setManaged(false);
        searchSection.setVisible(true);
        searchSection.setManaged(true);
        
        // Maintain back button visibility based on mode
        btnBackOnly.setVisible(dashboardController == null);
        btnBackOnly.setManaged(dashboardController == null);
        
        txtConfCode.clear();
        lblBillId.setText("-");
        lblReservationId.setText("-");
        lblOriginalAmount.setText("0.00 ₪");
        lblDiscountType.setText("-");
        lblDiscount.setText("0% (-0.00 ₪)");
        lblTotalAmount.setText("0.00 ₪");
        currentBill = null;
    }

    @FXML
    @Override
    protected void handleBack(ActionEvent event) {
        if (billDetailsSection.isVisible()) {
            resetUI();
        } else {
            // If in terminal mode and at search screen, go back to menu
            if (dashboardController == null) {
                super.handleBack(event);
            }
            // In dashboard mode, we don't handle navigation here
        }
    }
}

