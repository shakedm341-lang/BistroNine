package controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import data.Bill;
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
	
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////managing messages //////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Handles messages from the server related to table operations.
	 */
	public Object handleMessageFromServer(Message msg) 
	{
		
		switch (msg.command) //Checking the type of message sent from the server (what action should be performed in the DB Controller)
		{
	    case RECEIVE_TABLE_ID:
	    	return null; //receiveTableIdByConfCode(msg);

	    default:
	        System.out.println("Unknown task received.");
	        return null;
		}
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////Helper methods//////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void sendBillMessage(int resId) 
	{
	    Bill bill=new Bill(); 
	    bill.setReservationId(resId);
	    
	    	DBC.getBillDetails(bill);//update the bill object with the details from the DB
	    	
		// Simulating sending the bill to the customer
	    System.out.println("Hello, you have been dining with us for 2 hours. Here is your bill details:\n "+ "Total Amount: $" + bill.getTotalAmountAfterDiscount() + "\n" + "Please proceed to payment at your earliest convenience. Thank you! good day!" );
	    
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
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Automated tasks//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void  sendBillReservation()
	{
		LocalDate today = LocalDate.now();
	    
		//get all today's reservations
	    ArrayList<TableReservation> todayReservations = ReservationControler.getAllReservationsByDay(today, false);

	    if (todayReservations == null || todayReservations.isEmpty()) 
	    {
	        return;
	    }

	    LocalDateTime nowTime = LocalDateTime.now();// get the current time

	    for (TableReservation res : todayReservations) //loop through today's reservations
	    {
	        
	        //check if the reservation status is "arrived"
	        if ("arrived".equalsIgnoreCase(res.getStatus())) 
	        {
	            
	        	 LocalDateTime arrivalTime = res.getArrivalTime().toLocalDateTime();// get the arrival time
	            
	            
	            if (arrivalTime == null) continue; 

	            //calculate the duration since arrival
	            long minutesSeated = java.time.Duration.between(arrivalTime, nowTime).toMinutes();

	            //check if the duration is 2 hours for sending the bill
	            if (minutesSeated >= 120 && minutesSeated < 125) 
	            {

	                sendBillMessage(res.getReservationId());//send the bill message to the customer
	            }
	        }
	    }
	}
}
