package controller;

import java.io.*;
import java.lang.reflect.Field;
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

	// Controllers
	private ReservationControler reservationsController;
	private CustomerController customerController;
	private BillController billController;
	private TableController tableController;
	private WaitListController waitListController;
	private OpeningTimeController openingTimeController;
	private TimeReportController timeReportController;
	private SubscriberReportController subscriberReportController;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ServerDashboardController serverUI;

	public ServerController(int port, String dbPassword, ServerDashboardController serverUI) {
		super(port);
		this.serverUI = serverUI;
		
		DataBaseController.initiateDBC(dbPassword);
		this.reservationsController = new ReservationControler();
		this.customerController = new CustomerController();
		this.billController = new BillController();
		this.tableController = new TableController();
		this.waitListController = new WaitListController();
		this.openingTimeController = new OpeningTimeController();
		this.timeReportController = new TimeReportController();
		this.subscriberReportController = new SubscriberReportController();
		
		startAutoTasks();
	}


	/**
	 * Updates the server reference to the new Dashboard controller and updates the DB password.
	 * Also repopulates the table with existing clients.
	 */
	public void updateServerDetails(ServerDashboardController newUI, String newDbPassword) {
		this.serverUI = newUI;

		// 1. Update DB Password
		DataBaseController.initiateDBC(newDbPassword);

		// 2. Repopulate the new table with existing clients
		refreshClientList();

		System.out.println("Server UI replaced, DB password updated, and client list refreshed.");
	}

	/**
	 * Iterates over all active connections and adds them to the GUI table.
	 */
	private void refreshClientList() {
		// OCSF method to get all active threads (clients)
		Thread[] clientThreads = getClientConnections();

		for (Thread clientThread : clientThreads) {
			ConnectionToClient client = (ConnectionToClient) clientThread;

			// Retrieve the info we saved earlier
			String clientIp = (String) client.getInfo("IP");
			String hostName = (String) client.getInfo("Host");
			String clientPort = (String) client.getInfo("Port");

			// Safety check: if info is missing, try to fetch it again
			if (clientIp == null) clientIp = client.getInetAddress().getHostAddress();
			if (hostName == null) hostName = client.getInetAddress().getHostName();
			if (clientPort == null) clientPort = getClientPortUsingReflection(client);

			final String fIp = clientIp;
			final String fHost = hostName;
			final String fPort = clientPort;

			// Add to the new Table
			if (serverUI != null) {
				Platform.runLater(() -> {
					serverUI.addClient(fIp, fHost, fPort);
				});
			}
		}
	}
	
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

	private void handleTableRequest(Message message, ConnectionToClient client) {
		Object response = tableController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleBillRequest(Message message, ConnectionToClient client) {
		Object response = billController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleWaitListRequest(Message message, ConnectionToClient client) {
		Object response = waitListController.handleMessageFromServer(message);
		message.content = response;
		try {
			byte[] data = KryoUtil.serialize(message);
			client.sendToClient(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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

	private void startAutoTasks() {
		scheduler.scheduleAtFixedRate(() -> {
			try {
				reservationsController.deleteLateReservations();
				reservationsController.sendReminderAlertsForReservation();
				BillController.sendBillReservation();
			} catch (Exception e) {
				System.err.println("Error during auto-cleanup: " + e.getMessage());
			}
		}, 0, 1, TimeUnit.MINUTES); 

		scheduler.scheduleAtFixedRate(() -> {
			try {
				TimeReportController.timeReportGenerate();
				SubscriberReportController.subscriberReportGenerate();
			} catch (Exception e) {
				System.err.println("Error in daily tasks: " + e.getMessage());
			}
		}, 0, 24, TimeUnit.HOURS);
	}

	@Override
	protected void serverStarted() {
		System.out.println("Server listening for connections on port " + getPort());
	}

	@Override
	protected void serverStopped() {
		scheduler.shutdown();
		System.out.println("Server and scheduler stopped.");
		System.out.println("Server has stopped listening for connections.");
	}

	@Override
	protected void clientConnected(ConnectionToClient client) {
		super.clientConnected(client);

		String clientIp = client.getInetAddress().getHostAddress();
		String hostName = client.getInetAddress().getHostName();
		String clientPort = getClientPortUsingReflection(client);

		// SAVE the info so we can retrieve it later if the UI refreshes
		client.setInfo("IP", clientIp);
		client.setInfo("Host", hostName);
		client.setInfo("Port", clientPort);

		System.out.println("Client Connected: " + clientIp + ":" + clientPort);

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

	private String getClientPortUsingReflection(ConnectionToClient client) {
		try {
			Field field = client.getClass().getDeclaredField("clientSocket");
			field.setAccessible(true); 
			Socket socket = (Socket) field.get(client);

			if (socket != null) {
				return String.valueOf(socket.getPort());
			}
		} catch (Exception e) {
			System.out.println("Could not read private socket: " + e.getMessage());
		}
		return "Unknown";
	}
}