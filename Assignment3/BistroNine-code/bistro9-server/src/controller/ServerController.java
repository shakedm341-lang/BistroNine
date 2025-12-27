package controller;

import java.io.*;
import java.lang.reflect.Field; // Import for Reflection
import java.net.Socket;
import data.*; 
import ocsf.server.*;
import gui.ServerDashboardController;
import javafx.application.Platform;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class ServerController extends AbstractServer {
	
	final public static int DEFAULT_PORT = 5555;
	
	// references for the controllers:
	// initialized in the constructor.
	private ReservationControler reservationsController;
	private CustomerController customerController;
	private BillController billController;
	private TableController tableController;
	
	// Scheduler for outonomous tasks
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	private ServerDashboardController serverUI;

	public ServerController(int port, String dbPassword, ServerDashboardController serverUI) {
		super(port);
		this.serverUI = serverUI;
		
		DataBaseController.initiateDBC(dbPassword);
		this.reservationsController = new ReservationControler();
		this.customerController = new CustomerController();
		this.billController = new BillController();
		this.tableController=new TableController();
		startAutoTasks();
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////managing messages //////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void handleMessageFromClient(Object msg, ConnectionToClient client) {
		if (msg instanceof byte[]) {
			Message message = (Message) KryoUtil.deserialize((byte[]) msg);

			switch (message.type) {
			case RESERVATION:
				handleReservationRequest(message, client);
				break;
				
			case CUSTOMER:
				handleCustomerRequest(message, client);
				break;
			
			case TABLE:
				handleTableRequest(message, client);
				break;
				
			default:
				System.out.println("Unknown command received: " + message.type);
				break;
			}
		} else {
			System.out.println("Server received non-byte[] message. Ignored.");
		}
	}

	private void handleReservationRequest(Message message, ConnectionToClient client) {
		Object response = reservationsController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void handleCustomerRequest(Message message, ConnectionToClient client) {
		Object response = customerController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleTableRequest(Message message, ConnectionToClient client) 
	{
		Object response = tableController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////initialization Automated tasks//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void startAutoTasks() 
	{
        scheduler.scheduleAtFixedRate(() -> {
            try {
            		
                reservationsController.deleteLateReservations();//clean up reservations that were not confirmed befor 15 minutes
                reservationsController.sendReminderAlertsForReservation();//send reminders for upcoming reservations 2 hours before
                BillController.sendBillReservation();//send bills for reservations 
            } catch (Exception e) {
                System.err.println("Error during auto-cleanup: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES); // initial delay 0, run every 1 minute
    }
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////Client server architecture management methods//////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected void serverStarted() {
		System.out.println("Server listening for connections on port " + getPort());
	}

	@Override
	protected void serverStopped() {
		scheduler.shutdown(); // סגירה מסודרת של התהליכון
        System.out.println("Server and scheduler stopped.");
		System.out.println("Server has stopped listening for connections.");
	}

	@Override
	protected void clientConnected(ConnectionToClient client) {
		super.clientConnected(client);
		
		String clientIp = client.getInetAddress().getHostAddress();
		String hostName = client.getInetAddress().getHostName();
		
		// 1. Get the port using Reflection (works on all OCSF versions)
		String clientPort = getClientPortUsingReflection(client);
		
		// 2. SAVE the info
		client.setInfo("IP", clientIp);
		client.setInfo("Host", hostName);
		client.setInfo("Port", clientPort);
		
		System.out.println("Client Connected: " + clientIp + ":" + clientPort);

		// 3. Update GUI
		if (serverUI != null) {
			final String finalIp = clientIp;
			final String finalHost = hostName;
			final String finalPort = clientPort;
			
			Platform.runLater(() -> {
				serverUI.addClient(finalIp, finalHost, finalPort);
			});
		}
	}

	@Override
	synchronized protected void clientDisconnected(ConnectionToClient client) {
		String clientIp = (String) client.getInfo("IP");
		String clientPort = (String) client.getInfo("Port");
		
		System.out.println("Client Disconnected: " + clientIp);
		
		if (serverUI != null && clientIp != null) {
			Platform.runLater(() -> {
				serverUI.updateClientStatus(clientIp, clientPort, "Disconnected");
			});
		}
	}
	
	@Override
	synchronized protected void clientException(ConnectionToClient client, Throwable exception) {
		String clientIp = (String) client.getInfo("IP");
		String clientPort = (String) client.getInfo("Port");
		
		if (clientIp != null && serverUI != null) {
			Platform.runLater(() -> {
				serverUI.updateClientStatus(clientIp, clientPort, "Aborted");
			});
		}
	}
	
	/**
	 * Uses Reflection to access the private 'clientSocket' field in
	 * ConnectionToClient to retrieve the client's port number.
	 * 
	 * @param client The ConnectionToClient instance.
	 * @return The client's port number as a String, or "Unknown" if it cannot be
	 *         retrieved.
	 */
	private String getClientPortUsingReflection(ConnectionToClient client) {
		try {
			// Access the private variable 'clientSocket' inside ConnectionToClient
			Field field = client.getClass().getDeclaredField("clientSocket");
			field.setAccessible(true); // Make it accessible even though it's private
			Socket socket = (Socket) field.get(client);
			
			if (socket != null) {
				return String.valueOf(socket.getPort());
			}
		} catch (Exception e) {
			// If reflection fails, fallback to "Unknown"
			System.out.println("Could not read private socket: " + e.getMessage());
		}
		return "Unknown";
	}
}