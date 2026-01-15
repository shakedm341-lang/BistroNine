package controller;

import ocsf.client.AbstractClient;
import java.io.*;
import java.time.LocalTime;
import java.util.ArrayList;

import data.*;
import data.HistoryReservation;
import gui.LoginController;
import gui.MyReservationsController;
import gui.ReservationBoundry;
import gui.ReservationManagementController;
import gui.ProfileController;
import gui.SubscribersViewController;
import gui.TabCurrentDinersController;
import gui.TabActiveReservationController;

/**
 * The ClientController class extends AbstractClient and handles the communication
 * between the client-side application and the server. It manages incoming messages
 * from the server and outgoing messages from the various GUI boundaries.
 * 
 * This class follows the Singleton-like pattern for various GUI controllers to ensure
 * that responses from the server are routed to the correct part of the user interface.
 */
public class ClientController extends AbstractClient {

    // --- Static Fields for GUI Controllers (Route server responses to the correct UI) ---
    /** Flag to indicate if the client is awaiting a response from the server. */
    public static boolean awaitResponse = false;
    
    /** Controller for the login screen. */
    public static LoginController loginController;
    
    /** Controller for the reservation creation boundary. */
    private static ReservationBoundry reservationBoundry;
    
    /**
     * Registers a ReservationBoundry instance to receive server updates.
     * @param boundry The boundary to subscribe.
     */
    public static void subscribeReservationBoundry(ReservationBoundry boundry) {
        reservationBoundry = boundry;
    }

    /**
     * Unregisters a ReservationBoundry instance. 
     * If null is passed, it forces the reference to be cleared.
     * @param boundry The boundary to unsubscribe, or null to force clear.
     */
    public static void unsubscribeReservationBoundry(ReservationBoundry boundry) {
        if (boundry == null || reservationBoundry == boundry) {
            reservationBoundry = null;
        }
    }
    
    /** Controller for viewing personal reservations. */
    public static MyReservationsController MyReservation;
    
    /** Controller for managing reservations (employee view). */
    public static ReservationManagementController reservationManagementController;
    
    /** Controller for client registration. */
    public static gui.RegisterClientController registerClientController;
    
    /** Controller for the user profile view. */
    public static ProfileController profileController;
    
    /** Controller for viewing and managing subscribers. */
    public static SubscribersViewController subscribersViewController;
    
    /** Controller for the current diners tab. */
    public static TabCurrentDinersController tabCurrentDinersController;
    
    /** Controller for the active reservations tab. */
    public static TabActiveReservationController tabActiveReservationController;
    
    /** Controller for application settings. */
    public static gui.SettingsController settingsController;
    
    /** Controller for table management. */
    public static gui.TableManagementController tableManagementController;
    
    /** Controller for the waiting list tab. */
    public static gui.TabWaitingListController tabWaitingListController;
    
    /** Controller for getting a table (check-in). */
    public static gui.GetTableController getTableController;
    
    /** Controller for joining the waiting list. */
    public static gui.JoinWaitlistController joinWaitlistController;
    
    /** Controller for cancel registration (waitlist or reservation). */
    public static gui.CancelRegistrationController cancelRegistrationController;
    
    /** Controller for paying the bill. */
    public static gui.PayBillController payBillController;
    
    /** Controller for the user dashboard. */
    public static gui.UserDashboardController userDashboardController;
    
    /** Controller for viewing reports. */
    public static gui.ReportsController reportsController;
    
    /** Controller for viewing visit history. */
    public static gui.VisitHistoryController visitHistoryController;

    /** Interface for viewing visit history (generic). */
    private gui.IVisitHistory currentVisitHistoryViewer;
    
    /** Interface for viewing reservations. */
    private gui.IReservationViewer currentReservationViewer;
    
    /** Interface for deleting reservations. */
    private gui.IReservationDeleter currentReservationDeleter;
    
    /**
     * Constructs a new ClientController and opens a connection to the server.
     * 
     * @param host The server host address.
     * @param port The server port number.
     * @throws IOException If an I/O error occurs when opening the connection.
     */
    public ClientController(String host, int port) throws IOException {
        super(host, port);
        openConnection();
    }
    
    /**
     * Sets the current visit history viewer interface.
     * 
     * @param viewer The visit history viewer to set.
     */
    public void setVisitHistoryViewer(gui.IVisitHistory viewer) {
        this.currentVisitHistoryViewer = viewer;
    }
    
    /**
     * Sets the current reservation viewer interface.
     * 
     * @param viewer The reservation viewer to set.
     */
    public void setReservationViewer(gui.IReservationViewer viewer) {
        this.currentReservationViewer = viewer;
    }
    
    /**
     * Sets the current reservation deleter interface.
     * 
     * @param deleter The reservation deleter to set.
     */
    public void setReservationDeleter(gui.IReservationDeleter deleter) {
        this.currentReservationDeleter = deleter;
    }

    @Override
    /**
     * Primary entry point for messages received from the server. 
     * It deserializes the raw byte array into a Message object and routes 
     * it to specialized handlers based on the message type.
     * 
     * @param msg The message received from the server (expected to be byte[] from Kryo).
     */
    protected void handleMessageFromServer(Object msg) {
        // Strict check: We only support byte[] (Kryo) for efficiency and consistency
        if (msg instanceof byte[]) {
            // Deserialize the byte array back to a Message object
            Message message = (Message) KryoUtil.deserialize((byte[]) msg);

            // First-level routing: based on the general category of the message
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
                    System.err.println("Client received unknown TypeMessage: " + message.type);
                    break;
            }

        } else {
            System.err.println("Client received non-byte[] message. Ignored.");
        }
    }

    /**
     * Routes reservation-specific commands to their respective implementation methods.
     * 
     * @param message The message containing the reservation command and data.
     */
    private void handleReservationResponse(Message message) {
        switch (message.command) {
            case GET_ALL_RESERVATIONS:
            case GET_RESERVATION_BY_ATTRIBUTE:
            case GET_ALL_RESERVATIONS_BY_DATE_RANGE:
            case GET_HISTORY_RESERVATION_BY_CUSTOMER_ID:
                handleReservationListResponse(message);
                break;

            case GET_ALL_RESERVATIONS_BY_CUSTOMER:
                handleVisitHistoryResponse(message);
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
                System.err.println("CLIENT: Unknown Reservation command: " + message.command);
                break;
        }
    }

    /**
     * Routes customer-related commands to their respective implementation methods.
     * 
     * @param message The message containing the customer command and data.
     */
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
                
            case CHECK_LOGIN_DETAILS_BY_TAG_READER:
                handleTagIdentificationResponse(message);
                break;
             
            case GET_ALL_CONF_CODE_BY_CUSTOMER_ID:
                handleGetCodesResponse(message);
                break;
                
            case LOST_CONF_CODE:
                handleLostCodeResponse(message);
                break;    

            default:
                System.err.println("CLIENT: Unknown Customer command: " + message.command);
                break;
        }
    }

    /**
     * Routes table-related commands to their respective implementation methods.
     * 
     * @param message The message containing the table command and data.
     */
    private void handleTableResponse(Message message) {
        switch (message.command) {
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

            case BROADCAST_UPDATE_TABLE:
                if (reservationBoundry != null) {
                    reservationBoundry.onOpeningHoursChanged();
                }
                break;

            default:
                System.err.println("CLIENT: Unknown Table command: " + message.command);
                break;
        }
    }
    
    /**
     * Routes opening time-related commands to their respective implementation methods.
     * 
     * @param message The message containing the opening time command and data.
     */
    private void handleOpeningTimeResponse(Message message) {
        switch(message.command) {
            case GET_WEEKLY_OPENING_TIME:
                handleGetWeeklyHoursResponse(message);
                break;
                
            case GET_SPECIAL_OPENING_TIME:
                handleGetSpecialHoursResponse(message);
                break;
                
            case UPDATE_OPENING_TIME:
            case UPDATE_SPECIAL_OPENING_TIME:
            case ADD_NEW_OPENING_TIME:
            case ADD_NEW_SPECIAL_OPENING_TIME:
            case DELETE_OPENING_TIME:
            case DELETE_SPECIAL_OPENING_TIME:
            case CLOSE_RESTAURANT_ON_SPECIAL_DAY:
                handleSaveOpeningHoursResponse(message);
                break;

            case BROADCAST_UPDATE_OPENING_TIME:
                if (reservationBoundry != null) {
                    reservationBoundry.onOpeningHoursChanged();
                }
                break;

            default:
                System.err.println("CLIENT: Unknown Opening Time command: " + message.command);
                break;
        }
    }
    
    /**
     * Routes waiting list-related commands to their respective implementation methods.
     * 
     * @param message The message containing the waitlist command and data.
     */
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
                System.err.println("CLIENT: Unknown WaitList command: " + message.command);
                break;
        }
    }
    
    /**
     * Routes bill and payment commands to their respective implementation methods.
     * 
     * @param message The message containing the bill command and data.
     */
    private void handleBillMessage(Message message) {
        switch (message.command) {
            case SHOW_BILL:
                handleShowBillResponse(message);
                break;
                
            case PAY_BILL:
                handlePayBillResponse(message);
                break;

            default:
                System.err.println("CLIENT: Unknown Bill command: " + message.command);
                break;
        }
    }

    // --- Implementation Methods (Logic) ---

    /**
     * Updates the reservation boundary with a list of available hours for a selected date.
     * 
     * @param message The message containing a list of LocalTime objects.
     */
    private void handleTableAvailabilityResponse(Message message) {
        if (reservationBoundry != null) {
            @SuppressWarnings("unchecked")
            ArrayList<LocalTime> availableTimes = (ArrayList<LocalTime>) message.content;
            reservationBoundry.updateAvailableHours(availableTimes); 
        }
    }

    /**
     * Handles the server's response to a reservation creation request.
     * 
     * @param message The message containing the creation result (e.g., success/fail).
     */
    private void handleCreateReservationResponse(Message message) {
        if (reservationBoundry != null) {
            Object response = message.content;
            reservationBoundry.onReservationCreationResponse(response);
        }
    }

    /**
     * Processes the login result from the server.
     * 
     * @param message The message containing the Subscriber object (or null if failed).
     */
    private void handleLoginResponse(Message message) {
        if (loginController != null) {
            Subscriber subscriber = (Subscriber) message.content;
            loginController.handleServerLoginResponse(subscriber); 
        }
    }

    /**
     * Handles a list of reservation history records received from the server.
     * 
     * @param message The message containing an ArrayList of HistoryReservation.
     */
    private void handleReservationListResponse(Message message) {
        if (currentReservationViewer != null) {
            @SuppressWarnings("unchecked")
            ArrayList<HistoryReservation> list = (ArrayList<HistoryReservation>) message.content;
            currentReservationViewer.setReservationsList(list);
            currentReservationViewer = null; // Reset after use
        }
    }

    /**
     * Processes the customer's visit history response.
     * 
     * @param message The message containing an ArrayList of TableReservation.
     */
    private void handleVisitHistoryResponse(Message message) {
        @SuppressWarnings("unchecked")
        ArrayList<TableReservation> list = (ArrayList<TableReservation>) message.content;
        
        // Priority 1: Current generic viewer (for specific searches)
        if (currentVisitHistoryViewer != null) {
            currentVisitHistoryViewer.setReservationsList(list);
            currentVisitHistoryViewer = null; // Reset after use
        } 
        // Priority 2: Legacy static controller reference
        else if (visitHistoryController != null) {
            visitHistoryController.setReservationsList(list);
        }
    }

    /**
     * Handles the response for a reservation deletion request.
     * 
     * @param message The message containing a Boolean indicating success or failure.
     */
    private void handleDeleteReservationResponse(Message message) {
		if (currentReservationDeleter != null) {
			Boolean success = (Boolean) message.content;
			currentReservationDeleter.handleDeleteReservationResponse(success); 
						currentReservationDeleter = null; // Reset after use
		}
	}
    
    /**
     * Handles the registration response for a new subscriber.
     * 
     * @param message The message containing the registration result.
     */
    private void handleRegistrationResponse(Message message) {
        if (registerClientController != null) {
            registerClientController.handleServerResponse(message.content); 
        }
    }
    
    /**
     * Updates the subscriber management view with a fresh list of subscribers.
     * 
     * @param message The message containing an ArrayList of Subscriber.
     */
    private void handleGetAllSubscribersResponse(Message message) {
        if (subscribersViewController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<Subscriber> list = (ArrayList<Subscriber>) message.content;
            System.out.println("DEBUG: Received " + (list != null ? list.size() : 0) + " subscribers from server.");
            subscribersViewController.updateSubscriberTable(list); 
        }
    }
    
    /**
     * Processes the response for a subscriber profile update.
     * 
     * @param message The message containing a Boolean success status.
     */
    private void handleUpdateProfileResponse(Message message) {
        if (profileController != null) {
            Boolean isSuccess = (Boolean) message.content;
            profileController.updateProfileSuccess(isSuccess); 
        }
    }
    
    /**
     * Updates the GUI with the list of diners currently at the restaurant.
     * 
     * @param message The message containing an ArrayList of Customer.
     */
    private void handleGetAllDinersResponse(Message message) {
        if (tabCurrentDinersController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<Customer> list = (ArrayList<Customer>) message.content;
            tabCurrentDinersController.updateTableData(list); 
        }
    }
    
    /**
     * Updates the GUI with the list of currently active reservations.
     * 
     * @param message The message containing an ArrayList of TableReservation.
     */
    private void handleGetActiveReservationsResponse(Message message) {
        if (tabActiveReservationController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<TableReservation> list = (ArrayList<TableReservation>) message.content;
            tabActiveReservationController.updateTableData(list); 
        }
    }
    
    /**
     * Handles the retrieval of standard weekly opening hours.
     * 
     * @param message The message containing an ArrayList of OpeningHours.
     */
    private void handleGetWeeklyHoursResponse(Message message) {
        if (settingsController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<OpeningHours> list = (ArrayList<OpeningHours>) message.content;
            settingsController.updateWeeklyOpeningHours(list); 
        }
    }
    
    /**
     * Handles the retrieval of special/overriding opening hours.
     * 
     * @param message The message containing an ArrayList of OpeningHoursPerDay.
     */
    private void handleGetSpecialHoursResponse(Message message) {
        if (settingsController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<OpeningHoursPerDay> list = (ArrayList<OpeningHoursPerDay>) message.content;
            settingsController.updateSpecialOpeningHours(list); 
        }
    }
    
    /**
     * Processes the result of a save operation for opening hours.
     * 
     * @param message The message containing the save result.
     */
    private void handleSaveOpeningHoursResponse(Message message) {
        if (settingsController != null) {
            Object response = message.content;
            settingsController.onSaveResponse(response);
        }
    }
    
    /**
     * Updates the table management view with all restaurant tables.
     * 
     * @param message The message containing an ArrayList of Table.
     */
    private void handleGetAllTablesResponse(Message message) {
        if (tableManagementController != null) {
            @SuppressWarnings("unchecked")
            ArrayList<Table> list = (ArrayList<Table>) message.content;
            tableManagementController.updateTableList(list); 
        }
    }
    
    /**
     * Processes the result of a table operation (Add, Delete, or Update Seats).
     * 
     * @param message The message containing the operation result.
     */
    private void handleTableOperationResponse(Message message) {
        if (tableManagementController != null) {
            tableManagementController.handleOperationResponse(message.content);
        }
    }
    
    /**
     * Generic helper to refresh the waitlist display in the employee view.
     * 
     * @param message The message containing the current waitlist data.
     */
    private void passToWaitListController(Message message) {
        if (tabWaitingListController != null) {
            tabWaitingListController.updateTableData(message.content);
        }
    }
    
    /**
     * Handles the response for customer identification via tag reader.
     * 
     * @param message The message containing identification result.
     */
    private void handleTagIdentificationResponse(Message message) {
        if (loginController != null) {
            loginController.onIdentificationResponse(message.content); 
        }
    }
    
    /**
     * Handles the retrieval of reservation confirmation codes for a customer.
     * 
     * @param message The message containing the list of codes.
     */
    private void handleGetCodesResponse(Message message) {
        if (getTableController != null) {
            getTableController.onCodesResponse(message.content); 
        }
    }
    
    /**
     * Processes the result of a "lost confirmation code" recovery request.
     * 
     * @param message The message containing the recovery status.
     */
    private void handleLostCodeResponse(Message message) {
        if (getTableController != null) {
            getTableController.onRecoverCodesResponse(message.content); 
        }
    }
    
    /**
     * Handles the response for a walk-in check-in (getting a table).
     * 
     * @param message The message containing the assigned Table or Reservation.
     */
    private void handleCheckInResponse(Message message) {
        if (getTableController != null) {
            getTableController.onCheckInResponse(message.content); 
        }
    }
    
    /**
     * Processes the result of a request to join the waitlist.
     * 
     * @param message The message containing the waitlist entry result.
     */
    private void handleJoinWaitlistResponse(Message message) {
        if (joinWaitlistController != null) {
            joinWaitlistController.onJoinResponse(message.content); 
        }
    }
    
    /**
     * Handles delete responses from the waitlist, routing them to either the 
     * employee view or the kiosk view depending on which is active.
     * 
     * @param message The message containing the deletion result.
     */
    private void handleWaitlistDeleteResponse(Message message) {
        if (tabWaitingListController != null) {
            tabWaitingListController.updateTableData(message.content); 
        }
        if (cancelRegistrationController != null) {
            cancelRegistrationController.onDeleteResponse(message.content); 
        }
        if (userDashboardController != null) {
            userDashboardController.onWaitlistDeleteResponse(message.content);
        }
    }
    
    /**
     * Processes the "Show Bill" response from the server.
     * 
     * @param message The message containing the Bill object.
     */
    private void handleShowBillResponse(Message message) {
        if (payBillController != null) {
            payBillController.handleShowBillResponse((Bill) message.content); 
        }
    }
    
    /**
     * Processes the result of a bill payment attempt.
     * 
     * @param message The message containing a Boolean success status.
     */
    private void handlePayBillResponse(Message message) {
        if (payBillController != null) {
            payBillController.handlePayBillResponse((Boolean) message.content); 
        }
    }

    /**
     * Displays the generated Subscriber Report in the reports view.
     * 
     * @param message The message containing the SubscriberReport object.
     */
    private void handleSubscriberReportResponse(Message message) {
        if (reportsController != null) {
            reportsController.displaySubscriberReport((SubscriberReport) message.content);
        }
    }

    /**
     * Displays the generated Time Report in the reports view.
     * 
     * @param message The message containing the TimeReport object.
     */
    private void handleTimeReportResponse(Message message) {
        if (reportsController != null) {
            reportsController.displayTimeReport((TimeReport) message.content);
        }
    }
    
    /**
     * Sends a message from a GUI boundary to the server.
     * The message is wrapped in a Message object, serialized using Kryo, and sent.
     * 
     * @param type The type of message (e.g., RESERVATION, CUSTOMER).
     * @param content The data payload of the message.
     * @param command The specific command for the server to execute.
     */
    public void handleMessageFromBoundary(TypeMessage type, Object content, Command command) {
        Message msg = new Message();
        msg.type = type;
        msg.content = content;
        msg.command = command;

        try {
            // Ensure connection is open before sending; attempt to reconnect if necessary
            if (!isConnected()) {
                openConnection();
            }

            // Serialize the Message object to a byte array before transmission
            byte[] data = KryoUtil.serialize(msg);
            sendToServer(data);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not send message to server: Terminating client." + e);
            quit(); // Force quit if communication fails
        }
    }

    /**
     * Gracefully closes the connection and terminates the application.
     */
    public void quit() {
        try {
            closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
