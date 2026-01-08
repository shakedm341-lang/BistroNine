package controller;

import ocsf.client.AbstractClient;
import java.io.*;
import java.time.LocalTime;
import java.util.ArrayList;

import data.*;
import gui.LoginController;
import gui.MyReservationsController;
import gui.ReservationBoundry;
import gui.ReservationManagementController;
import gui.UpdateReservtionBoundry;
import gui.ProfileController;
import gui.SubscribersViewController;
import gui.TabCurrentDinersController;
import gui.TabActiveReservationController;

public class ClientController extends AbstractClient {

    // Define variables
    public static boolean awaitResponse = false;
    public static UpdateReservtionBoundry updatereservationBoundary;
    public static LoginController loginController;
    public static ReservationBoundry reservationBoundry;
    public static MyReservationsController MyReservation;
    public static ReservationManagementController reservationManagementController;
    public static gui.RegisterClientController registerClientController;
    public static ProfileController profileController;
    public static SubscribersViewController subscribersViewController;
    public static TabCurrentDinersController tabCurrentDinersController;
    public static TabActiveReservationController tabActiveReservationController;
    public static gui.SettingsController settingsController;
    public static gui.TableManagementController tableManagementController;
    public static gui.TabWaitingListController tabWaitingListController;
    public static gui.GetTableController getTableController;
    public static gui.JoinWaitlistController joinWaitlistController;
    public static gui.LeaveWaitlistController leaveWaitlistController;
    public static gui.PayBillController payBillController;
    public static gui.UserDashboardController userDashboardController;
    public static gui.ReportsController reportsController;
    private gui.IReservationViewer currentReservationViewer;
    private gui.IReservationDeleter currentReservationDeleter;
    
    
    

    // Constructor
    public ClientController(String host, int port) throws IOException {
        super(host, port);
        openConnection();
    }
    
    public void setReservationViewer(gui.IReservationViewer viewer) {
        this.currentReservationViewer = viewer;
    }
    public void setReservationDeleter(gui.IReservationDeleter deleter) {
    			this.currentReservationDeleter = deleter;
    }

    @Override
    // Handle message from server
    protected void handleMessageFromServer(Object msg) {
        // Strict check: We only support byte[] (Kryo)
        if (msg instanceof byte[]) {
            // Deserialize the byte array back to a Message object
            Message message = (Message) KryoUtil.deserialize((byte[]) msg);

            // 1. First Switch: Route by TypeMessage
            switch (message.type) {
                case RESERVATION:
                    handleReservationResponse(message);
                    break;

                case CUSTOMER:
                    handleCustomerResponse(message);
                    break;

                case TABLE:
                    handleTableResponse(message);
                    break;
                    
                case OPENING_TIME: 
                    handleOpeningTimeResponse(message);
                    break;    
                    
                case WAITLIST:
                    handleWaitListMessage(message);
                    break;
                    
                case BILL:
                    handleBillMessage(message);
                    break;  

                case SUBSCRIBER_REPORT:
                    handleSubscriberReportResponse(message);
                    break;

                case TIME_REPORT:
                    handleTimeReportResponse(message);
                    break;

                default:
                    System.out.println("Unknown TypeMessage received: " + message.type);
                    break;
            }

        } else {
            System.out.println("Client received non-byte[] message. Ignored.");
        }
    }

    // 2. Second Switch: Handle Reservation related commands
    private void handleReservationResponse(Message message) {
        switch (message.command) {
            case GET_ALL_RESERVATIONS_BY_CUSTOMER:
                handleGetAllReservationsByCustomer(message);
                break;

            case UPDATE_RESERVATION_DETAILS:
                handleUpdateReservationResponse(message);
                break;

            case CREATE_NEW_RESERVATION:
                handleCreateReservationResponse(message);
                break;

          
            case CHECK_TABLE_AVAILABILITY: 
                 handleTableAvailabilityResponse(message);
                 break;
                 
            case DELETE_RESERVATION:
                handleDeleteReservationResponse(message);
                break;
                
            case GET_ALL_DINERS_AT_RESTAURANT:
                handleGetAllDinersResponse(message);
                break;
                
            case GET_ALL_RESERVATIONS_ACTIVE:
                handleGetActiveReservationsResponse(message);
                break;   
                
            default:
                System.out.println("CLIENT:Unknown Reservation command: " + message.command);
                break;
        }
    }

    // 2. Second Switch: Handle Customer related commands
    private void handleCustomerResponse(Message message) {
        switch (message.command) {
            case CHECK_LOGIN_DETAILS:
                handleLoginResponse(message);
                break;
             
            case ADD_NEW_SUBSCRIBER: 
                handleRegistrationResponse(message);
                break;
                
            case GET_ALL_SUBSCRIBERS:
                handleGetAllSubscribersResponse(message);
                break;
                
            case UPDATE_SUBSCRIBER_DETAILS:
                handleUpdateProfileResponse(message);
                break;
                
            case CHECK_LOGIN_DETAILSֹֹ_BY_TAG_READER:
                handleTagIdentificationResponse(message);
                break;
             
            case GET_ALL_CONF_CODE_BY_CUSTOMER_ID:
                handleGetCodesResponse(message);
                break;
                
            case LOST_CONF_CODE:
                handleLostCodeResponse(message);
                break;    

            default:
                System.out.println("Unknown Customer command: " + message.command);
                break;
        }
    }

    // 2. Second Switch: Handle Table related commands
    private void handleTableResponse(Message message) {
        switch (message.command) {
            // If the server sends TypeMessage.TABLE for availability, use this case.
            // Currently mapped in RESERVATION above based on typical flow, but if server uses TABLE:
            case CHECK_TABLE_AVAILABILITY:
                handleTableAvailabilityResponse(message);
                break;
            
            case GET_ALL_AVAILABLE_TABLES:
                handleGetAllTablesResponse(message);
                break;
                
            case DELETE_TABLE:
            case ADD_TABLE:
            case UPDATE_TABLE_SEATS:
            	handleTableOperationResponse(message); 
                break;
                
            case RECEIVE_TABLE_ID:
                handleCheckInResponse(message);
                break;

            default:
                System.out.println("Unknown Table command: " + message.command);
                break;
        }
    }
    
    // 2. Second Switch: Handle Opening Time related commands
    private void handleOpeningTimeResponse(Message message) {
        switch(message.command) {
            case GET_WEEKLY_OPENING_TIME:
                handleGetWeeklyHoursResponse(message);
                break;
                
            case GET_SPECIAL_OPENING_TIME:
                handleGetSpecialHoursResponse(message);
                break;
                
            case UPDATE_OPENING_TIME:
            case ADD_NEW_SPECIAL_OPENING_TIME:
                handleSaveOpeningHoursResponse(message);
                break;
                
            default:
                System.out.println("Unknown Opening Time command: " + message.command);
                break;
        }
    }
    
    // 2. Second Switch: Handle methods for Waiting List
    private void handleWaitListMessage(Message message) {
        switch (message.command) {
            case GET_WAIT_LIST:
                passToWaitListController(message);
                break;
                
            case DELETE_FROM_WAIT_LIST: 
            	handleWaitlistDeleteResponse(message);
            	break;
            	
            case GET_IN_TO_WAIT_LIST:
                handleJoinWaitlistResponse(message);
                break;    

            default:
                System.out.println("Unknown WaitList command: " + message.command);
                break;
        }
    }
    
    // 2. Second Switch: Handle methods for Bill & Payment
    private void handleBillMessage(Message message) {
        switch (message.command) {
            case SHOW_BILL:
                handleShowBillResponse(message);
                break;
                
            case PAY_BILL:
                handlePayBillResponse(message);
                break;

            default:
                System.out.println("Unknown Bill command: " + message.command);
                break;
        }
    }

    // --- Implementation Methods (Logic) ---

    private void handleTableAvailabilityResponse(Message message) {
        if (reservationBoundry != null) {
            @SuppressWarnings("unchecked")
            ArrayList<LocalTime> availableTimes = (ArrayList<LocalTime>) message.content;
            reservationBoundry.updateAvailableHours(availableTimes); // Update available times in boundary
        }
    }

    private void handleCreateReservationResponse(Message message) {
        if (reservationBoundry != null) {
            Object response = message.content;
            reservationBoundry.onReservationCreationResponse(response);
        }
    }

    private void handleLoginResponse(Message message) {
        if (loginController != null) {
            Subscriber subscriber = (Subscriber) message.content;
            loginController.handleServerLoginResponse(subscriber); // Process login response in boundary
        }
    }

    // Helper method for handling the list of reservations
    private void handleGetAllReservationsByCustomer(Message message) {
        if (currentReservationViewer != null) {
            @SuppressWarnings("unchecked")
            ArrayList<TableReservation> list = (ArrayList<TableReservation>) message.content;
            currentReservationViewer.setReservationsList(list);  
            
            currentReservationViewer = null; // Reset after use
            }
    }

    // Helper method for handling the update response
    private void handleUpdateReservationResponse(Message message) {
        if (updatereservationBoundary != null) {
            Boolean success = (Boolean) message.content;
            updatereservationBoundary.showUpdateMessage(success); // Update message in boundary
        }
    }
    
    private void handleDeleteReservationResponse(Message message) {
		if (currentReservationDeleter != null) {
			Boolean success = (Boolean) message.content;
			currentReservationDeleter.handleDeleteReservationResponse(success); // Update message in boundary
						currentReservationDeleter = null; // Reset after use
		}
	}
    
    // Helper method for handling the registration response
    private void handleRegistrationResponse(Message message) {
        if (registerClientController != null) {
            registerClientController.handleServerResponse(message.content); // Pass response to registration controller
        }
    }
    
    // Helper method for handling the list of subscribers response
    private void handleGetAllSubscribersResponse(Message message) {
        if (subscribersViewController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<Subscriber> list = (ArrayList<Subscriber>) message.content;
            subscribersViewController.updateSubscriberTable(list); // Update the subscribers table in the boundary
        }
    }
    
    // Helper method for handling the profile update response
    private void handleUpdateProfileResponse(Message message) {
        if (profileController != null) {
            Boolean isSuccess = (Boolean) message.content;
            profileController.updateProfileSuccess(isSuccess); // Pass success status to profile controller
        }
    }
    
    // Helper method for handling the list of current diners response
    private void handleGetAllDinersResponse(Message message) {
        if (tabCurrentDinersController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<Customer> list = (ArrayList<Customer>) message.content;
            tabCurrentDinersController.updateTableData(list); // Update the diners table in the boundary
        }
    }
    
    // Helper method for handling the list of active reservations response
    private void handleGetActiveReservationsResponse(Message message) {
        if (tabActiveReservationController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<TableReservation> list = (ArrayList<TableReservation>) message.content;
            tabActiveReservationController.updateTableData(list); // Update the active reservations table in the boundary
        }
    }
    
    // Helper method for handling the list of weekly opening hours response
    private void handleGetWeeklyHoursResponse(Message message) {
        if (settingsController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<OpeningHours> list = (ArrayList<OpeningHours>) message.content;
            settingsController.updateWeeklyOpeningHours(list); // Update the weekly opening hours in the boundary
        }
    }
    
    // Helper method for handling the list of special opening hours response
    private void handleGetSpecialHoursResponse(Message message) {
        if (settingsController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<OpeningHoursPerDay> list = (ArrayList<OpeningHoursPerDay>) message.content;
            settingsController.updateSpecialOpeningHours(list); // Update the special opening hours in the boundary
        }
    }
    
    // Helper method for handling the save response (update/add) for opening hours
    private void handleSaveOpeningHoursResponse(Message message) {
        if (settingsController != null) {
            Boolean success = (Boolean) message.content;
            settingsController.onSaveResponse(success);// Notify the boundary about the save operation status
        }
    }
    
    // Helper method for handling the list of tables
    private void handleGetAllTablesResponse(Message message) {
        if (tableManagementController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<Table> list = (ArrayList<Table>) message.content;
            tableManagementController.updateTableList(list); // Update the table list in the boundary
        }
    }
    
    private void handleTableOperationResponse(Message message) {
        if (tableManagementController != null) {
            tableManagementController.handleOperationResponse(message.content);
        }
    }
    
    // Helper method for handling the waiting list (Get or Delete response)   
    private void passToWaitListController(Message message) {
        if (tabWaitingListController != null) {
            tabWaitingListController.updateTableData(message.content);
        }
    }
    
    // Helper method for handling tag identification response 
    private void handleTagIdentificationResponse(Message message) {
        if (loginController != null) {
            loginController.onIdentificationResponse(message.content); // Pass identification result to the Login controller
        }
    }
    
    // Helper method for handling the retrieval of confirmation codes
    private void handleGetCodesResponse(Message message) {
        if (getTableController != null) {
            getTableController.onCodesResponse(message.content); // Update the list of codes
        }
    }
    
    // Helper method for handling the lost code recovery response
    private void handleLostCodeResponse(Message message) {
        if (getTableController != null) {
            getTableController.onRecoverCodesResponse(message.content); // Notify the Kiosk boundary about recovery status
        }
    }

    // Helper method for handling the check-in (get table) response
    private void handleCheckInResponse(Message message) {
        if (getTableController != null) {
            getTableController.onCheckInResponse(message.content); // Pass the check-in result Table/Reservation
        }
    }
    
    // Helper method for handling the join waitlist response
    private void handleJoinWaitlistResponse(Message message) {
        if (joinWaitlistController != null) {
            joinWaitlistController.onJoinResponse(message.content); // Pass the join result Confirmation Code or Table ID to the controller
        }
    }
    
    //Handles delete response for BOTH Employee view and Kiosk view
    private void handleWaitlistDeleteResponse(Message message) {
        if (tabWaitingListController != null) {
            tabWaitingListController.updateTableData(message.content); //If the Employee View is active, pass data to refresh the table
        }
        if (leaveWaitlistController != null) {
            leaveWaitlistController.onDeleteResponse(message.content); //If the Kiosk View is active, pass result to show success/fail message
        }
    }
    
    // Helper method for handling "Show Bill" response
    private void handleShowBillResponse(Message message) {
        if (payBillController != null) {
            payBillController.handleShowBillResponse((Bill) message.content); // Pass the Bill object to display details in the GUI
        }
    }
    
    // Helper method for handling "Pay Bill" response
    private void handlePayBillResponse(Message message) {
        if (payBillController != null) {
            payBillController.handlePayBillResponse((Boolean) message.content); // Pass payment success status to the controller
        }
    }

    private void handleSubscriberReportResponse(Message message) {
        if (reportsController != null) {
            reportsController.displaySubscriberReport((SubscriberReport) message.content);
        }
    }

    private void handleTimeReportResponse(Message message) {
        if (reportsController != null) {
            reportsController.displayTimeReport((TimeReport) message.content);
        }
    }
    
    // Handle message from boundary
    public void handleMessageFromBoundary(TypeMessage type, Object content, Command command) {
        Message msg = new Message();
        msg.type = type;
        msg.content = content;
        msg.command = command;

        try {
            // Ensure connection is open before sending
            if (!isConnected()) {
                openConnection();
            }

            // STRICT: Serialize to byte[] before sending
            byte[] data = KryoUtil.serialize(msg);
            sendToServer(data);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not send message to server: Terminating client." + e);
            quit(); // Quit client
        }
    }

    public void quit() {
        try {
            closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}