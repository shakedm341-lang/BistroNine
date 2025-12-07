package controller;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import data.Message;
import data.TableReservation;


//controler

public class ReservationControler 
{

	private DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	
	public ReservationControler() 
	{
	
	}
	
	public Object handleMessageFromServer(Message msg) 
	{
		
		switch (msg.command) //Checking the type of message sent from the server (what action should be performed in the DB Controller)
		{
	    case GET_ALL_RESERVATIONS:
	    	return getAllReservations(msg);
	    	
	    case UPDATE_RESERVATION_DETAILS:
	    	return updateReservationDetails(msg);
	    	
	    default:
	        System.out.println("Unknown task received.");
	        return null;
		}
	}
		
	private ArrayList<TableReservation> getAllReservations(Message msg)
	{
		
    	ArrayList<String> reservationsListAsStr = new ArrayList<>();
    	ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();
    	
    	reservationsListAsStr = DBC.getAllReservationsQuery();//Create a new query in the database that returns all existing orders as a list of strings (each string is a row in the table)
    	
    	for (String resAsStr : reservationsListAsStr)//loop that converts the list of strings to a list of table reservation objects
    	{
    	    //Splitting the string into the columns of the table from the database to take the transferred data
    	    String[] dataRes = resAsStr.split(",");
    	    
    	    //Creating a table reservation object with the data received from the DB
    	    TableReservation resAsTableRes = new TableReservation();
    	    resAsTableRes.setReservationId(dataRes[0]);
    	    resAsTableRes.setReservationDate(dataRes[1]);
    	    resAsTableRes.setNumberOfDiners(dataRes[2]);
    	    resAsTableRes.setConfirmationCode(dataRes[3]);
    	    resAsTableRes.setSubscriberId(dataRes[4]);
    	    resAsTableRes.setDateOfMakeReservation(dataRes[5]);
    	    
    	    //Adding the object we created to the list of table reservation type orders
    	    reservationsListAsTableRes.add(resAsTableRes);
    	}
    	
    	return reservationsListAsTableRes;// Returning the order list to the server as a list of table order objects
	}
	
	private boolean updateReservationDetails(Message msg)
	{
		ArrayList<String> list = (ArrayList<String>) msg.content;//list of strings containing the order from which the information to be updated should be updated.
    	

    	//Create a new table reservation antity with the reservation you want to update and the details to be updated.
    	TableReservation res = new TableReservation();
	    res.setReservationId(list.get(0));
	    res.setReservationDate(list.get(1));
	    res.setNumberOfDiners(list.get(2));

	    //Updates the order details in the DB
	    //Return to the server whether the update operation was performed correctly or not
	    return DBC.updateReservationDetailsQuery(res); 
	    
	}
	

}
