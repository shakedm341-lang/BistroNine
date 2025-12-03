package controller;

import ocsf.client.AbstractClient;
import java.io.*;
import java.util.ArrayList;

import data.*;
import gui.ReservtionBoundry;
public class ClientController extends AbstractClient {
	
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
			sendToServer(msg);
		} catch (IOException e) {
			System.out.println("Error sending message to server.");
			e.printStackTrace();
		}
	}
}