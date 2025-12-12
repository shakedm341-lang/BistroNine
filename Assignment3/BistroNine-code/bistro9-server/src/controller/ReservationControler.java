package controller;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Random;

import data.Customer;
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
	    	
	    /*case UPDATE_RESERVATION_DETAILS:
	    	return updateReservationDetails(msg);*/
	    
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
		
		ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();//List to hold all reservations from the DB as a list of lists of objects
    	ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();//List to hold all reservations as TableReservation objects
    	
    	@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
    	int customerId=0;
    	// Setting subscriber ID from the list we got from the message content
    				if (list.get(0) instanceof Integer) {
    					 customerId = (int) list.get(0);
    				} else {
    					System.out.println("Error: Index 0 is not a Integer!");
    					return null;
    				}
    	
    	
    	allReservations = DBC.getAllReservationsQuery(customerId);//Get all reservations from the DB as a list of lists of objects
    	
    	for (ArrayList<Object> resAsList : allReservations)
    	{//For each reservation in the list of reservations
    		
    		TableReservation resAsTableRes = new TableReservation();//Create a new TableReservation 
    	    
    	    //Set reservation ID in the TableReservation object
    		if (resAsList.get(0) instanceof Integer) 
    		{
                resAsTableRes.setReservationId((Integer) resAsList.get(0));
            } else {
                System.out.println("Error: Index 0 is not a Integer!");
                return null; 
            }

    		//Set table id in the TableReservation object
            if (resAsList.get(1) instanceof Integer) {
                resAsTableRes.setTableId((Integer) resAsList.get(1));
            } else {
                System.out.println("Error: Index 1 is not a Integer!");
                return null;
            }
    		
          //Set number of diners in the TableReservation object
            if (resAsList.get(2) instanceof Integer) {
                resAsTableRes.setNumberOfDiners((Integer) resAsList.get(2));
            } else {
                System.out.println("Error: Index 2 is not a Integer!");
                return null;
            }

            //Set confirmation code in the TableReservation object
            if (resAsList.get(3) instanceof Integer) {
                resAsTableRes.setConfirmationCode((Integer) resAsList.get(3));
            } else {
                System.out.println("Error: Index 3 is not a Integer!");
                return null;
            }
            
          //Set customer Id in the TableReservation object
            if (resAsList.get(4) instanceof Integer) {
                resAsTableRes.setCustomerId((Integer) resAsList.get(4));
            } else {
                System.out.println("Error: Index 4 is not a Integer!");
                return null;
            }
            
    		
            //Set reservation date in the TableReservation object
            if (resAsList.get(5) instanceof Timestamp) {
                resAsTableRes.setReservationDate((Timestamp) resAsList.get(5));
            } else {
                System.out.println("Error: Index 5 is not a Timestamp!");
                return null;
            }

            

            

            //Set date of make reservation in the TableReservation object
            if (resAsList.get(6) instanceof Timestamp) {
                resAsTableRes.setDateOfMakeReservation((Timestamp) resAsList.get(6));
            } else {
                System.out.println("Error: Index 6 is not a Timestamp!");
                return null;
            }
    	    
          //Set arrival time in the TableReservation object
            if (resAsList.get(7) instanceof Timestamp) {
                resAsTableRes.setArrivalTime((Timestamp) resAsList.get(7));
            } else {
                System.out.println("Error: Index 7 is not a Timestamp!");
                return null;
            }
            
          //Set leaving Time in the TableReservation object
            if (resAsList.get(8) instanceof Timestamp) {
                resAsTableRes.setLeavingTime((Timestamp) resAsList.get(8));
            } else {
                System.out.println("Error: Index 8 is not a Timestamp!");
                return null;
            }
    	    
          
            
          //Set status  in the Subscriber object
    		if (resAsList.get(9) instanceof String) {
    			resAsTableRes.setStatus((String) resAsList.get(9));
        	} else {
        	    System.out.println("Error: Index 9 is not a String!");
        	    return null;
        	}
            
            
    	    reservationsListAsTableRes.add(resAsTableRes);//Add the reservation to the list of reservations as TableReservation 
    	}
    	
    	return reservationsListAsTableRes;//Return to server the list of reservations as TableReservation 
	}

	
	/**
	 * Updates the details of an existing table reservation in the database.
	 *
	 * @param msg The message containing the updated reservation details. The
	 *            content of the message is expected to be an ArrayList<Object> with
	 *            the following order: [reservationId (Integer), reservationDate
	 *            (Timestamp), numberOfDiners (Integer)]
	 * @return true if the update was successful, false otherwise.
	 */
	/*private boolean updateReservationDetails(Message msg)
	{

		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//Get the reservation details from the message content
		
  
    	TableReservation res = new TableReservation();
    	
    	//Set reservation ID from the list
		if (list.get(0) instanceof Integer) {
    	    res.setReservationId((int) list.get(0));
    	} else {
    	    System.out.println("Error: Index 0 is not a Integer!");
    	    return false;
    	}
    	
    	//Set reservation date from the list
    	if (list.get(1) instanceof Timestamp) {
    	    res.setReservationDate((Timestamp) list.get(1));
    	} else {
    	    System.out.println("Error: Index 1 is not a Timestamp!");
    	    return false;
    	}
    	
    	//Set number of diners from the list
    	if (list.get(2) instanceof Integer) {
    	    res.setNumberOfDiners((int) list.get(2));
    	} else {
    	    System.out.println("Error: Index 2 is not a Integer!");
    	    return false;
    	}
    	
    	

	    //Updates the order details in the DB
	    //Return to the server whether the update operation was performed correctly or not
	    return DBC.updateReservationDetailsQuery(res); 
	    
	}*/

	/**
	 * Creates a new table reservation in the database.
	 *
	 * @param msg The message containing the reservation details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 :typeCustomer (String),Location 1,2: if customer: phone number(String) and/or email(String) if subscribers:customerId ,
	 *            Location 3: numberOfDiners (Integer),Location 4: reservationDate (Timestamp)] 
	 *   
	 * @return The confirmation code (Integer) of the new reservation if the creation succeeded, null otherwise.
	 */
	private Integer  createNewReservation(Message msg)
	{
		
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		
		TableReservation newRes = new TableReservation();//Creating a new reservation object
		Random rand = new Random();//Random object to generate a random confirmation code
		String typeCustomer= new String();
		int customerId=-1;

		
		//Setting customer type from the list we got from the message content
		if (list.get(0) instanceof String) 
		{
			typeCustomer = (String) list.get(0);
		} else 
		{
	    	  System.out.println("Error: Index 0 is not a String!");
	    	  return null;
		}
		
		
		if (typeCustomer.equals("customer")) 
		{
			Customer cust = new Customer();
			// Setting phone Number from the list we got from the message content
			if (list.get(1) instanceof String) 
			{
				cust.setPhoneNumber((String) list.get(1))  ;
				if (list.get(2) instanceof String) {
					cust.setEmail((String) list.get(2));// Setting email from the list we got from the message content
				} else {
					System.out.println("Error: Index 2 is not a String!");
					return null;
				}
				
			}
			else if (list.get(1)==null)//if the phone number is null
			{
				if (list.get(2) instanceof String) 
				{
					cust.setEmail((String) list.get(2))  ;//Setting email from the list we got from the message content
					cust.setPhoneNumber(null);
				}
				else {
					System.out.println("Error: Index 2 is not a String!");
					return null;
				}
			}
			else 
			{
				System.out.println("Error: Index 1 is not a String!");
				return null;
			}
			//return customer ID from the DB . if cust not exists he created in the DB and return his ID else return his ID
			customerId=DBC.getCustomerId(cust);//Getting customer ID from the DB based on the phone number or email provided
		}

		else if (typeCustomer.equals("subscriber")) 
		{
			// Setting subscriber ID from the list we got from the message content
			if (list.get(1) instanceof Integer) {
				customerId = (int) list.get(1);
			} else {
				System.out.println("Error: Index 1 is not a Integer!");
				return null;
			}
		}
		
		//reservation id is auto generated in the DB

		//Setting number of diners from the list we got from the message content
		if (list.get(3) instanceof Integer) 
		{
					newRes.setNumberOfDiners((int) list.get(3));
		} else 
		{
				    System.out.println("Error: Index 3 is not a Integer!");
				    return null;
		}
		
		
		//Setting the reservation date from the list we got from the message content
		if (list.get(4) instanceof Timestamp) 
		{
				newRes.setReservationDate((Timestamp) list.get(4));
		} else 
		{
			    System.out.println("Error: Index 4 is not a Timestamp!");
			    return null;
		}
		
		//Setting table ID based on number of diners and reservation date
		newRes.setTableId(DBC.catchTable(newRes.getNumberOfDiners(), newRes.getReservationDate()));//Assigning a table ID to the reservation based on the number of diners and reservation date
				
		
		
		int code=0;
        boolean exists=true;

        
        while (exists)
        {
        	code = 100000 + rand.nextInt(900000);
        	exists = DBC.checkIfConfCodeExistsInDB(code);//Check if the generated code already exists in the DB
        }

        newRes.setConfirmationCode(code);//Set the unique confirmation code to the reservation

		newRes.setCustomerId(customerId);//Setting customer ID to the reservation
	
		
		
		//date of make reservation is auto generated in the DB
		
        //arrival time and leaving time are set  as null at the beginning

		//status of reservation is auto generated in the DB

		if (DBC.createNewReservation(newRes))//Return that the reservation was created successfully
		{
			return newRes.getConfirmationCode();//Return to server the confirmation code of the new reservation);
		}
		
		return null;
		
	}
	
	/**
	 * Checks table availability for a given number of diners and reservation date.
	 *
	 * @param msg The message containing the details for checking table
	 *            availability. The content of the message is expected to be an
	 *            ArrayList<Object> with the following order: [numberOfDiners
	 *            (Integer), reservationDate (Timestamp)]
	 * @return An ArrayList of Timestamps representing available dates and times for
	 *         the requested number of diners and reservation date.
	 */
	private ArrayList<Timestamp> checkingTableAvailability(Message msg)
	{
	
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		int numberOfDiners = 0;
		Timestamp reservationDate = null;
	
		//Setting number of diners from the list we got from the message content
		if (list.get(0) instanceof Integer) {
				  numberOfDiners=(int) list.get(0);
		} else {
		    	  System.out.println("Error: Index 0 is not a Integer!");
		    	  return null;
		}
		    	
		//Setting reservation date from the list we got from the message content
		if (list.get(1) instanceof Timestamp) {
			reservationDate=(Timestamp) list.get(1);
		} else {
		    	  System.out.println("Error: Index 1 is not a Timestamp!");
		    	  return null;
		}
		
		
		return DBC.checkingTableAvailability(numberOfDiners, reservationDate);//Return to server the list of available dates and times for the requested number of diners and reservation date;
	}
	
	

}
