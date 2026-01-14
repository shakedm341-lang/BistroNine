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
 * Controller for the Pay Bill screen in the BistroNine application.
 * This class handles the logic for searching, displaying, and processing bill payments.
 * It is designed to be reusable across different contexts, such as the Guest Terminal
 * or the User Dashboard.
 * 
* Key functionalities include:
 * - Searching for bills using a reservation confirmation code.
 * - Displaying detailed bill information including discounts.
 * - Simulating credit card payment processing through a dialog.
 * - Handling communication with the server for bill retrieval and payment.

 */
public class PayBillController extends BaseTerminalController implements Initializable {

    // --- FXML UI Components ---

    /** Section for searching a bill by code */
    @FXML
    private VBox searchSection;
    
    /** Input field for the reservation confirmation code */
    @FXML
    private TextField txtConfCode;
    
    /** Button to trigger the bill search */
    @FXML
    private Button btnSearch;

    /** Section for displaying bill details after a successful search */
    @FXML
    private VBox billDetailsSection;
    
    /** Label to show the unique bill ID */
    @FXML
    private Label lblBillId;
    
    /** Label to show the associated reservation ID */
    @FXML
    private Label lblReservationId;
    
    /** Label for the base amount before discounts */
    @FXML
    private Label lblOriginalAmount;
    
    /** Label showing the calculated discount amount */
    @FXML
    private Label lblDiscount;
    
    /** Label for the type of discount applied (e.g., Subscriber) */
    @FXML
    private Label lblDiscountType;
    
    /** Label for the final amount to be paid */
    @FXML
    private Label lblTotalAmount;
    
    /** Dropdown for selecting the payment method (Credit Card, Cash, etc.) */
    @FXML
    private ComboBox<String> comboPaymentMethod;
    
    /** Button to initiate the payment process */
    @FXML
    private Button btnPay;
    
    /** Back button specifically for the terminal flow */
    @FXML
    private Button btnBackOnly;

    // --- State Variables ---

    /** The currently logged-in subscriber (null if in guest mode) */
    private Subscriber currentUser;
    
    /** Reference to the dashboard controller if called from a user dashboard */
    private UserDashboardController dashboardController;
    
    /** The bill object currently being viewed or paid */
    private Bill currentBill;

    /**
     * Initializes the controller class. Sets up the payment method options
     * and ensures the initial UI state is correct.
     * 
     * @param location  The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	// Register this controller instance with the client for server callbacks
    	ClientController.payBillController = this;
    	
    	// Populate payment method dropdown
    	comboPaymentMethod.setItems(FXCollections.observableArrayList("Credit Card", "Cash", "App"));
        comboPaymentMethod.getSelectionModel().selectFirst();
        
        // Ensure UI starts in the search state
        billDetailsSection.setVisible(false);
        billDetailsSection.setManaged(false);
        searchSection.setVisible(true);
        searchSection.setManaged(true);
        btnBackOnly.setVisible(true);
        btnBackOnly.setManaged(true);
    }

    /**
     * Sets the client controller for server communication.
     * 
     * @param client The ClientController instance.
     */
    @Override
    public void setClient(ClientController client) {
        super.setClient(client);
    }

    /**
     * Configures the controller for use within the Guest Terminal.
     * Hides dashboard-specific UI elements.
     * 
     * @param client The ClientController instance.
     */
    public void setTerminalDependencies(ClientController client) {
        setClient(client);
        
        // Hide terminal back button if we are deep in the terminal flow
        Platform.runLater(() -> {
            btnBackOnly.setVisible(false);
            btnBackOnly.setManaged(false);
        });
    }

    /**
     * Configures the controller for use within a Subscriber's User Dashboard.
     * 
     * @param client             The ClientController instance.
     * @param user               The current Subscriber user.
     * @param dashboard The parent dashboard controller.
     */
    public void setDependencies(ClientController client, Subscriber user, UserDashboardController dashboard) {
        setClient(client);
        this.currentUser = user;
        this.dashboardController = dashboard;
        
        // Navigation within the dashboard is handled by the dashboard controller
        btnBackOnly.setVisible(false);
        btnBackOnly.setManaged(false);
    }

    /**
     * Handles the ActionEvent triggered by the search button.
     * Validates the input code and sends a request to the server to fetch the bill.
     * 
     * @param event The ActionEvent from the search button.
     */
    @FXML
    void handleSearch(ActionEvent event) {
        String codeStr = txtConfCode.getText().trim();
        
        // Input validation: Check if empty
        if (codeStr.isEmpty()) {
            TerminalUtils.showError("Input Error", "Please enter a confirmation code.");
            return;
        }

        try {
            // Parse the code and prepare the message for the server
            int confCode = Integer.parseInt(codeStr);
            ArrayList<Object> params = new ArrayList<>();
            params.add(confCode);

            // Request the bill details from the server
            client.handleMessageFromBoundary(TypeMessage.BILL, params, Command.SHOW_BILL);
            
            // Update UI state to reflect loading
            btnSearch.setDisable(true);
            btnSearch.setText("Searching...");
        } catch (NumberFormatException e) {
            TerminalUtils.showError("Input Error", "Confirmation code must be a number.");
        }
    }

    /**
     * Updates the UI to display the details of the retrieved bill.
     * Handles cases where the bill is not found or already paid.
     * 
     * @param bill The Bill object to display, or null if not found.
     */
    public void displayBill(Bill bill) {
        Platform.runLater(() -> {
            this.currentBill = bill;
            btnSearch.setDisable(false);
            btnSearch.setText("Fetch Bill");

            // Handle missing bill
            if (bill == null) {
                TerminalUtils.showError("Not Found", "No bill found for the provided confirmation code.");
                return;
            }

            // Handle already paid bill
            if (bill.isPaid()) {
                TerminalUtils.showSuccess("Already Paid", "This bill has already been paid.");
                return;
            }

            // Populate text labels with bill data
            lblBillId.setText(String.valueOf(bill.getBillId()));
            lblReservationId.setText(String.valueOf(bill.getReservationId()));
            lblOriginalAmount.setText(String.format("%.2f ₪", bill.getTotalAmount()));
            lblDiscountType.setText(bill.getDiscountType() != null ? bill.getDiscountType() : "Standard");
            
            // Calculate and show the discount details
            double discountAmount = bill.getTotalAmount() - bill.getTotalAmountAfterDiscount();
            lblDiscount.setText(String.format("%.0f%% (-%.2f ₪)", bill.getDiscountSize(), discountAmount));
            lblTotalAmount.setText(String.format("%.2f ₪", bill.getTotalAmountAfterDiscount()));

            // Switch view from Search to Bill Details
            searchSection.setVisible(false);
            searchSection.setManaged(false);
            billDetailsSection.setVisible(true);
            billDetailsSection.setManaged(true);
            btnBackOnly.setVisible(false);
            btnBackOnly.setManaged(false);
        });
    }

    /**
     * Handles the "PAY NOW" button click.
     * Triggers either the credit card simulation popup or direct processing for other methods.
     * 
     * @param event The ActionEvent from the pay button.
     */
    @FXML
    void handlePay(ActionEvent event) {
        String method = comboPaymentMethod.getValue();
        System.out.println("Debug: Payment method selected: [" + method + "]");
        
        // Credit card requires additional simulation steps
        if (method != null && method.trim().equalsIgnoreCase("Credit Card")) {
            simulateCreditCardPopup(event);
        } else {
            processPayment(method);
        }
    }

    /**
     * Displays a simulation dialog for entering credit card details.
     * Validates that all fields are filled before proceeding with payment.
     * 
     * @param event The original pay action event to find the owner window.
     */
    private void simulateCreditCardPopup(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Credit Card Simulation");
        dialog.setHeaderText("Please enter your card details");
        
        // Ensure the dialog appears correctly centered on the app window
        dialog.initOwner(((Node) event.getSource()).getScene().getWindow());

        // Setup dialog buttons
        ButtonType payButtonType = new ButtonType("Confirm Payment", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(payButtonType, ButtonType.CANCEL);

        // Build the simulation form layout
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

        // Process the dialog result
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == payButtonType) {
            // Simple validation for empty fields
            if (cardNumber.getText().isEmpty() || cvv.getText().isEmpty() || expiry.getText().isEmpty()) {
                TerminalUtils.showError("Validation Error", "All fields are required.");
                simulateCreditCardPopup(event); // Re-open the dialog if validation fails
            } else {
                processPayment("Credit");
            }
        }
    }

    /**
     * Sends the finalized payment request to the server.
     * 
     * @param method The payment method used (normalized to lowercase for the server).
     */
    private void processPayment(String method) {
        ArrayList<Object> params = new ArrayList<>();
        params.add(currentBill.getBillId());
        
        // Standardize the method name for the server
        String formattedMethod = (method != null) ? method.trim().toLowerCase() : "credit";
        params.add(formattedMethod);

        if (client != null) {   
            client.handleMessageFromBoundary(TypeMessage.BILL, params, Command.PAY_BILL);
        } else {
            TerminalUtils.showError("Error", "Client is not connected.");
        }
        
        // Disable UI to prevent double-clicks
        btnPay.setDisable(true);
        btnPay.setText("Processing...");
    }

    /**
     * Callback method called by the ClientController when the server returns bill data.
     * 
     * @param bill The bill retrieved from the database.
     */
    public void handleShowBillResponse(Bill bill) {
        Platform.runLater(() -> {
            displayBill(bill);
        });
    }

    /**
     * Callback method called by the ClientController after a payment attempt.
     * 
     * @param success True if the payment was successful, false otherwise.
     */
    public void handlePayBillResponse(boolean success) {
        Platform.runLater(() -> {
            btnPay.setDisable(false);
            btnPay.setText("PAY NOW");
            if (success) {
                TerminalUtils.showSuccess("Payment Successful", 
                    "Thank you! Your bill has been paid and your table is now available.");
                resetUI();
            } else {
                TerminalUtils.showError("Payment Failed", 
                    "There was an error processing your payment. Please try again.");
            }
        });
    }

    /**
     * Resets the UI to its initial search state and clears all displayed data.
     * Used after a successful payment or when going back from the bill view.
     */
    private void resetUI() {
        billDetailsSection.setVisible(false);
        billDetailsSection.setManaged(false);
        searchSection.setVisible(true);
        searchSection.setManaged(true);
        
        // Back button logic depends on whether we are in a dashboard or the terminal
        btnBackOnly.setVisible(dashboardController == null);
        btnBackOnly.setManaged(dashboardController == null);
        
        // Clear all fields
        txtConfCode.clear();
        lblBillId.setText("-");
        lblReservationId.setText("-");
        lblOriginalAmount.setText("0.00 ₪");
        lblDiscountType.setText("-");
        lblDiscount.setText("0% (-0.00 ₪)");
        lblTotalAmount.setText("0.00 ₪");
        currentBill = null;
    }

    /**
     * Handles the back navigation logic. 
     * If the user is viewing a bill, it returns them to the search screen.
     * If they are already on the search screen, it navigates back to the main menu.
     * 
     * @param event The ActionEvent from the back button.
     */
    @FXML
    @Override
    protected void handleBack(ActionEvent event) {
        if (billDetailsSection.isVisible()) {
            // If viewing a bill, return to search
            resetUI();
        } else {
            // If on search screen, use base terminal back logic (e.g., return to Terminal Menu)
            if (dashboardController == null) {
                super.handleBack(event);
            }
            // Navigation inside user dashboard is handled by the dashboard's own menu
        }
    }
}

