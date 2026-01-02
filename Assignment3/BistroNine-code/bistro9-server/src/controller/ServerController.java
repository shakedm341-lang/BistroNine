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

	/**
	 * Default port number for the server.
	 */
	final public static int DEFAULT_PORT = 5555;

	// Controllers for handling specific functionalities
	private ReservationControler reservationsController;
	private CustomerController customerController;
	private BillController billController;
	private TableController tableController;
	private WaitListController waitListController;
	private OpeningTimeController openingTimeController;
	private TimeReportController timeReportController;
	private SubscriberReportController subscriberReportController;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);// Scheduler for outonomous tasks
	private ServerDashboardController serverUI;// Reference to the server UI controller

	/**
	 * Constructor for ServerController class.
	 * 
	 * @param port       The port number on which the server will listen for client
	 *                   connections.
	 * @param dbPassword The password for the database connection.
	 * @param serverUI   The ServerDashboardController instance for updating the UI.
	 */
	public ServerController(int port, String dbPassword, ServerDashboardController serverUI) {
		super(port);
		this.serverUI = serverUI;
		// Initialize the DB controller and other controllers for handling specific functionalities
		DataBaseController.initiateDBC(dbPassword);
		this.reservationsController = new ReservationControler();
		this.customerController = new CustomerController();
		this.billController = new BillController();
		this.tableController=new TableController();
		this.waitListController=new WaitListController();
		this.openingTimeController=new OpeningTimeController();
		this.timeReportController=new TimeReportController();
		this.subscriberReportController=new SubscriberReportController();
		startAutoTasks();// Start automated tasks
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////managing messages //////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Handles messages received from clients and routes them to the appropriate
	 * controller based on the message type.
	 *
	 * @param msg    The message received from the client.
	 * @param client The connection to the client that sent the message.
	 */
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
				
			case BILL:
				handleBillRequest(message, client);
				break;
				
			case WAITLIST:
				handleWaitListRequest(message, client);
				break;
				
			case OPENING_TIME:
				handleOpeningTimeRequest(message, client);
				break;
			case TIME_REPORT:
				handleTimeReportRequest(message, client);
				break;
			case SUBSCRIBER_REPORT:
				handleSubscriberReportRequest(message, client);
				break;
			default:
				System.out.println("Unknown command received: " + message.type);
				break;
			}
		} else {
			System.out.println("Server received non-byte[] message. Ignored.");
		}
	}

	/**
	 *  Handles reservation-related requests from clients.
	 *
	 * @param message The message containing the reservation request.
	 * @param client  The connection to the client that sent the request.
	 */
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

	/**
	 *  Handles customer-related requests from clients.
	 *
	 * @param message The message containing the customer request.
	 * @param client  The connection to the client that sent the request.
	 */
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

	/**
	 *  Handles table-related requests from clients.
	 *
	 * @param message The message containing the table request.
	 * @param client  The connection to the client that sent the request.
	 */
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

	/**
	 * Handles bill-related requests from clients.
	 *
	 * @param message The message containing the bill request.
	 * @param client  The connection to the client that sent the request.
	 */
	private void handleBillRequest(Message message, ConnectionToClient client) 
	{
		Object response = billController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *  Handles waitlist-related requests from clients.
	 *
	 * @param message The message containing the waitlist request.
	 * @param client  The connection to the client that sent the request.
	 */
	private void handleWaitListRequest(Message message, ConnectionToClient client) 
	{
		Object response = waitListController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *  Handles opening time-related requests from clients.
	 *
	 * @param message The message containing the opening time request.
	 * @param client  The connection to the client that sent the request.
	 */
	private void handleOpeningTimeRequest(Message message, ConnectionToClient client) {
		Object response = openingTimeController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *  Handles time report-related requests from clients.
	 *
	 * @param message The message containing the time report request.
	 * @param client  The connection to the client that sent the request.
	 */
	private void handleTimeReportRequest(Message message, ConnectionToClient client) {
		Object response = timeReportController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * handles subscriber report-related requests from clients.
	 *
	 * @param message The message containing the subscriber report request.
	 * @param client  The connection to the client that sent the request.
	 */
	private void handleSubscriberReportRequest(Message message, ConnectionToClient client) {
		Object response = subscriberReportController.handleMessageFromServer(message);
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
	/**
	 * Starts automated tasks that run at scheduled intervals. Tasks include
	 * cleaning up late reservations, sending reminders, sending bills, and
	 * generating monthly time reports.
	 */
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
	
		
			scheduler.scheduleAtFixedRate(() -> {
		        try {
		            
		            TimeReportController.timeReportGenerate();//generate monthly time report
		            SubscriberReportController.subscriberReportGenerate();//generate monthly Subscriber report
		            
		        } catch (Exception e) {
		            System.err.println("Error in daily tasks: " + e.getMessage());
		        }
		    }, 0, 24, TimeUnit.HOURS);

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////Client server architecture management methods//////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Called when the server has started successfully.
	 */
	@Override
	protected void serverStarted() {
		System.out.println("Server listening for connections on port " + getPort());
	}

	/**
	 * * Called when the server has stopped.
	 */
	@Override
	protected void serverStopped() {
		scheduler.shutdown(); 
		System.out.println("Server and scheduler stopped.");
		System.out.println("Server has stopped listening for connections.");
	}

	/**
	 * * Called when a client connects to the server. Retrieves the client's IP
	 * address, host name, and port number using Reflection, saves this information,
	 * and updates the server UI.
	 * 
	 * @param client The connection to the client that has connected.
	 */
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

	/**
	 * * Called when a client disconnects from the server. Retrieves the client's IP
	 * address and port number, and updates the server UI to reflect the
	 * disconnection.
	 * 
	 * @param client The connection to the client that has disconnected.
	 */
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

	/**
	 * * * Called when a client connection encounters an exception. Retrieves the
	 * client's IP address and port number, and updates the server UI to reflect the
	 * aborted connection.
	 * 
	 * @param client    The connection to the client that encountered the exception.
	 * @param exception The exception that occurred.
	 */
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