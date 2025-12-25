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

public class ClientController extends AbstractClient {

    // Define variables
    public static boolean awaitResponse = false;
    public static UpdateReservtionBoundry updatereservationBoundary;
    public static LoginController loginController;
    public static ReservationBoundry reservationBoundry;
    public static MyReservationsController MyReservation;
    public static ReservationManagementController reservationManagementController;
    public static gui.RegisterClientController registerClientController;
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
            case GET_ALL_RESERVATIONS:
                handleGetAllReservations(message);
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

            default:
                System.out.println("Unknown Reservation command: " + message.command);
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

            default:
                System.out.println("Unknown Table command: " + message.command);
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
    private void handleGetAllReservations(Message message) {
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