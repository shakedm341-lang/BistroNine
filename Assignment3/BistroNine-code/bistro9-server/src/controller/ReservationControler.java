package controller;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Random;

import data.Message;
import data.Table;
import data.TableReservation;

public class ReservationControler 
{

	private DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	
	/**
	 * Default constructor
	 */
	public ReservationControler() 
	{
	
	}

	/**
	 * Handles messages received from the server and performs corresponding actions.
	 *
	 * @param msg The message received from the server.
	 * @return The result of the action performed, which can vary based on the
	 *         command.
	 */
	public Object handleMessageFromServer(Message msg) 
	{
		
		switch (msg.command) //Checking the type of message sent from the server (what action should be performed in the DB Controller)
		{
	    case GET_ALL_RESERVATIONS:
	    	return getAllReservations(msg);
	    	
	    case UPDATE_RESERVATION_DETAILS:
	    	return updateReservationDetails(msg);
	    
	    case CREATE_NEW_RESERVATION:
	    	return createNewReservation(msg);
	    		
	    case CHECK_TABLE_AVAILABILITY:
	    	return checkingTableAvailability(msg);
	    	
	    default:
	        System.out.println("Unknown task received.");
	        return null;
		}
	}

	/**
	 * Retrieves all table reservations from the database.
	 *
	 * @param msg The message requesting all reservations.
	 * @return An ArrayList of TableReservation objects representing all
	 *         reservations in the database.
	 */
	private ArrayList<TableReservation> getAllReservations(Message msg)
	{
		
		ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();
    	ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();
    	
    	allReservations = DBC.getAllReservationsQuery();//Get all reservations from the DB as a list of lists of objects
    	
    	for (ArrayList<Object> resAsList : allReservations)
    	{//For each reservation in the list of reservations
    		
    		TableReservation resAsTableRes = new TableReservation();//Create a new TableReservation 
    	    
    	    //Set reservation ID
    		if (resAsList.get(0) instanceof Integer) 
    		{
                resAsTableRes.setReservationId((Integer) resAsList.get(0));
            } else {
                System.out.println("Error: Index 0 (ID) is not an Integer! It is: " + resAsList.get(0).getClass().getSimpleName());
                return null; 
            }

            //Set reservation date
            if (resAsList.get(1) instanceof Timestamp) {
                resAsTableRes.setReservationDate((Timestamp) resAsList.get(1));
            } else {
                System.out.println("Error: Index 1 (Date) is not a Timestamp!");
                return null;
            }

            //Set number of diners
            if (resAsList.get(2) instanceof Integer) {
                resAsTableRes.setNumberOfDiners((Integer) resAsList.get(2));
            } else {
                System.out.println("Error: Index 2 (Diners) is not an Integer!");
                return null;
            }

            //Set confirmation code
            if (resAsList.get(3) instanceof Integer) {
                resAsTableRes.setConfirmationCode((Integer) resAsList.get(3));
            } else {
                System.out.println("Error: Index 3 (Code) is not an Integer!");
                return null;
            }

            //Set subscriber ID
            if (resAsList.get(4) instanceof Integer) {
                resAsTableRes.setSubscriberId((Integer) resAsList.get(4));
            } else {
                System.out.println("Error: Index 4 (SubID) is not an Integer!");
                return null;
            }

            //Set date of make reservation
            if (resAsList.get(5) instanceof Timestamp) {
                resAsTableRes.setDateOfMakeReservation((Timestamp) resAsList.get(5));
            } else {
                System.out.println("Error: Index 5 (MakeDate) is not a Timestamp!");
                return null;
            }
    	    
    	    
    	    reservationsListAsTableRes.add(resAsTableRes);//Add the reservation to the list of reservations as TableReservation 
    	}
    	
    	return reservationsListAsTableRes;//Return to server the list of reservations as TableReservation 
	}

	/**
	 * Updates the reservation details in the database based on the information
	 * provided in the message.
	 *
	 * @param msg The message containing the reservation details to be updated. The
	 *            content of the message is expected to be an ArrayList<Object> with
	 *            the following order: [reservationId (Integer), reservationDate
	 *            (Timestamp), numberOfDiners (Integer)]
	 * @return true if the update operation was successful, false otherwise.
	 */
	private boolean updateReservationDetails(Message msg)
	{

		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//Get the reservation details from the message content
		
  
    	TableReservation res = new TableReservation();
    	
    	//Set reservation ID from the list
    	
		if (list.get(0) instanceof Integer) {
    	    res.setReservationId((int) list.get(0));
    	} else {
    	    System.out.println("Error: Index 0 is not a String!");
    	    return false;
    	}
    	
    	//Set reservation date from the list
    	if (list.get(1) instanceof Timestamp) {
    	    res.setReservationDate((Timestamp) list.get(1));
    	} else {
    	    System.out.println("Error: Index 1 is not a String!");
    	    return false;
    	}
    	
    	//Set number of diners from the list
    	if (list.get(2) instanceof Integer) {
    	    res.setNumberOfDiners((int) list.get(2));
    	} else {
    	    System.out.println("Error: Index 2 is not a String!");
    	    return false;
    	}
    	
    	

	    //Updates the order details in the DB
	    //Return to the server whether the update operation was performed correctly or not
	    return DBC.updateReservationDetailsQuery(res); 
	    
	}

	/**
	 * Creates a new table reservation in the database based on the information
	 * provided in the message.
	 *
	 * @param msg The message containing the reservation details to be created. The
	 *            content of the message is expected to be an ArrayList<Object> with
	 *            the following order: [reservationDate (Timestamp), numberOfDiners
	 *            (Integer)]
	 * @return true if the reservation was created successfully, false otherwise.
	 */
	private boolean createNewReservation(Message msg)
	{
		
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		
		TableReservation newRes = new TableReservation();//Creating a new reservation object
		Random rand = new Random();//Random object to generate a random confirmation code
		
		//Setting the reservation date from the list we got from the message content
		if (list.get(0) instanceof Timestamp) 
		{
			newRes.setReservationDate((Timestamp) list.get(0));
		} else 
		{
	    	  System.out.println("Error: Index 0 is not a Timestamp!");
	    	  return false;
		}
		
    	//Setting number of diners from the list we got from the message content
		if (list.get(1) instanceof Integer) {
			newRes.setNumberOfDiners((int) list.get(1));
		} else 
		{
		    	  System.out.println("Error: Index 1 is not a Integer!");
		    	  return false;
		}
		//Setting subscriber ID from the list we got from the message content
		if (list.get(2) instanceof Integer) {
			newRes.setSubscriberId((int) list.get(2));
		} 
		else if (list.get(2) == null) 
		{
            newRes.setSubscriberId(-1); // Assuming -1 indicates a customer without a subscription
        }
		else 
		{
		    System.out.println("Error: Index 2 is not a Integer!");
		    return false;
		}
		
		
		
        int code=0;
        boolean exists=true;

        
        while (exists)
        {
        	code = 100000 + rand.nextInt(900000);
        	exists = DBC.checkIfConfCodeExistsInDB(code);//Check if the generated code already exists in the DB
        }

        newRes.setConfirmationCode(code);//Set the unique confirmation code to the reservation

	    newRes.setDateOfMakeReservation(new Timestamp(System.currentTimeMillis()));

		newRes.setTableId(DBC.catchTable(newRes.getNumberOfDiners(), newRes.getReservationDate()));//Assigning a table ID to the reservation based on the number of diners and reservation date
		

		
		return DBC.createNewReservation(newRes);//Return to server that the reservation was created successfully
		
	}
	
	/**
	 * Checks the availability of tables for a given number of diners and
	 * reservation date.
	 *
	 * @param msg The message containing the details for checking table
	 *            availability. The content of the message is expected to be an
	 *            ArrayList<Object> with the following order: [numberOfDiners
	 *            (Integer), reservationDate (Timestamp)]
	 * @return An ArrayList of Timestamps representing available dates and times for
	 *         the requested number of diners and reservation date.
	 */
	private ArrayList<Object> checkingTableAvailability(Message msg)
	{
	
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		int numberOfDiners = 0;
		Timestamp reservationDate = null;
	
		//Setting number of diners from the list we got from the message content
		if (list.get(0) instanceof Integer) {
				  numberOfDiners=(int) list.get(0);
		} else {
		    	  System.out.println("Error: Index 0 is not a String!");
		    	  return null;
		}
		    	
		//Setting reservation date from the list we got from the message content
		if (list.get(1) instanceof Timestamp) {
			reservationDate=(Timestamp) list.get(1);
		} else {
		    	  System.out.println("Error: Index 1 is not a String!");
		    	  return null;
		}
		
		
		return DBC.checkingTableAvailability(numberOfDiners, reservationDate);//Return to server the list of available dates and times for the requested number of diners and reservation date;
	}
	
	

}
