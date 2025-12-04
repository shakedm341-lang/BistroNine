package controller;

import ocsf.client.AbstractClient;
import java.io.*;
import java.util.ArrayList;

import data.*;
import gui.ReservtionBoundry;

public class ClientController extends AbstractClient {

	public static boolean awaitResponse = false;
	public static ReservtionBoundry reservationBoundary;

	public ClientController(String host, int port) throws IOException {
		super(host, port);
		openConnection();
	}

	protected void handleMessageFromServer(Object msg) {
		Message message = (Message) msg;

		switch (message.command) {
		case GET_ALL_RESERVATIONS:
			if (reservationBoundary != null) {
				ArrayList<TableReservation> list = (ArrayList<TableReservation>) message.content;
				reservationBoundary.updateReservationTable(list);
			}
			break;

		case UPDATE_RESERVATION_DETAILS:
			if (reservationBoundary != null) {
				Boolean success = (Boolean) message.content;
				reservationBoundary.showUpdateMessage(success);
			}
			break;

		}
	}

	public void handleMessageFromBoundary(TypeMessage type, Object content, Command command) {
		Message msg = new Message();
		msg.type = type;
		msg.content = content;
		msg.command = command;

		try {
			openConnection();// in order to send more than one message
			//awaitResponse = true;
			sendToServer(msg);
			// wait for response
//			while (awaitResponse) {
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Could not send message to server: Terminating client." + e);
			quit();
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