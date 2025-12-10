package controller;

import ocsf.client.AbstractClient;
import java.io.*;
import java.util.ArrayList;

import data.*;
import gui.UpdateReservtionBoundry;

public class ClientController extends AbstractClient {
	
	//define variables
	public static boolean awaitResponse = false;
	public static UpdateReservtionBoundry reservationBoundary;
	
	//constructor
	public ClientController(String host, int port) throws IOException {
		super(host, port);
		openConnection();
	}
	@Override
	//handle message from server
	protected void handleMessageFromServer(Object msg) {
		Message message = (Message) msg;
		
         //define commands
		switch (message.command) {
		case GET_ALL_RESERVATIONS:
			if (reservationBoundary != null) {
				ArrayList<TableReservation> list = (ArrayList<TableReservation>) message.content;
				reservationBoundary.updateReservationTable(list);//update table in boundary
			}
			break;

		case UPDATE_RESERVATION_DETAILS:
			if (reservationBoundary != null) {//update message in boundary
				Boolean success = (Boolean) message.content;
				reservationBoundary.showUpdateMessage(success);
			}
			break;

		}
	}
	//handle message from boundary
	public void handleMessageFromBoundary(TypeMessage type, Object content, Command command) {
		Message msg = new Message();
		msg.type = type;
		msg.content = content;
		msg.command = command;

		try {
			openConnection();// in order to send more than one message
			sendToServer(msg);//send message to server
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not send message to server: Terminating client." + e);
			quit();//quit client
		}

	}

	public void quit() {
		try {
			closeConnection();
		} catch (IOException e) {
		}
		System.exit(0);
	}
}