package controller;

import java.util.ArrayList;

import data.Message;
import data.TableReservation;

public class TableController 
{
	private DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	
	/**
	 * constructor for the TableController class
	 */
	public TableController()
	{
		
	}
	
	/**
	 * Handles messages from the server related to table operations.
	 */
	public Object handleMessageFromServer(Message msg) 
	{
		
		switch (msg.command) //Checking the type of message sent from the server (what action should be performed in the DB Controller)
		{
	    case RECEIVE_TABLE_ID:
	    	return receiveTableIdByConfCode(msg);
	    	
	    
	    default:
	        System.out.println("Unknown task received.");
	        return null;
		}
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/*
	private Integer receiveTableIdByConfCode(Message msg)
	{
	
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		int conferenceCode=0;
		
		// Setting conference Code from the list we got from the message content
					if (list.get(0) instanceof Integer) 
					{
						conferenceCode = (int) list.get(0);
					} 
					else {
						System.out.println("Error: Index 0 is not a Integer!");
						return null;
					}
		
					
					
	    return ;
	}*/
}
