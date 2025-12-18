package controller;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Random;

import data.Customer;
import data.Message;
import data.OpeningHoursPerDay;
import data.Table;
import data.TableReservation;
import data.TimeSlot;

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
	    	return getAllReservationsByCustomerId(msg);
	    	
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

	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////Regular methods//////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Converts a list of reservations from the database into TableReservation
	 * objects.
	 *
	 * @param allReservations A list of reservations from the database, where each
	 *                        reservation is represented as a list of objects.
	 * @return An ArrayList of TableReservation objects representing the
	 *         reservations.
	 */
	private ArrayList<TableReservation> getAllReservationsAsTableReservation(ArrayList<ArrayList<Object>> allReservations)
	{
		
	
		ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();//List to hold all reservations as TableReservation objects
		
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
    	
    	return reservationsListAsTableRes;
	}
		
	/**
	 * Retrieves all table reservations for a specific date from the database.
	 *
	 * @param date The date for which to retrieve reservations.
	 * @return An ArrayList of TableReservation objects representing all
	 *         reservations for the specified date.
	 */
	private ArrayList<TableReservation> getAllReservationsByDay(LocalDate day)
	{
		ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();//List to hold all reservations from the DB as a list of lists of objects
		ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();//List to hold all reservations as TableReservation objects
			//b1
	    	allReservations = DBC.getAllReservationsQueryByDay(day);//return all reservations from the DB at specific day as a list of lists of objects else return empty list
	    	reservationsListAsTableRes=	getAllReservationsAsTableReservation(allReservations);//Convert the list of reservations from the DB into list of TableReservation objects
	
	    return reservationsListAsTableRes;
	}

	/**
	 * Retrieves the opening time slots for a specific date from the database.
	 *
	 * @param date The date for which to retrieve opening time slots.
	 * @return An ArrayList of TimeSlot objects representing the opening time slots
	 *         for the specified date.
	 */
	private ArrayList<TimeSlot> getOpeningTime(LocalDate day)
	{
		OpeningHoursPerDay openingHours = new OpeningHoursPerDay(day);
		//b2
		DBC.getOpeningHoursByDate(openingHours);//update opening hours for the specific date from the DB in the OpeningHoursPerDay object else put null in the slots list
		// getOpeningHoursByDate(OpeningHoursPerDay openingHours)
		return openingHours.getSlots();
	}
	
	/**
	 * Retrieves all tables in the restaurant from the database.
	 *
	 * @return An ArrayList of Table objects representing all tables in the
	 *         restaurant.
	 */
	private ArrayList<Table> getTableInRestaurant()
	{
		ArrayList<ArrayList<Object>> allTables = new ArrayList<>();// List to hold all tables from the DB as a list of
																	// lists of objects
		ArrayList<Table> tablesListAsTable = new ArrayList<>();// List to hold all tables as Table objects
		//b3
		allTables = DBC.getAllTablesInRestaurant();//return all tables  from the DB as a list of lists of objects else return null
		for (ArrayList<Object> tableAsList : allTables) {// For each table in the list of tables
			Table table = new Table();// Create a new Table object

			// Set table ID in the Table object
			if (tableAsList.get(0) instanceof Integer) {
				table.setTableId((Integer) tableAsList.get(0));
			} else {
				System.out.println("Error: Index 0 is not a Integer!");
				return null;
			}

			// Set number of seats in the Table object
			if (tableAsList.get(1) instanceof Integer) {
				table.setSeatsNumber((Integer) tableAsList.get(1));
			} else {
				System.out.println("Error: Index 1 is not a Integer!");
				return null;
			}

			// Set location in the Table object
			if (tableAsList.get(2) instanceof String) {
				table.setLocation((String) tableAsList.get(2));
			} else {
				System.out.println("Error: Index 2 is not a String!");
				return null;
			}

			//set status in the Table object
			if (tableAsList.get(3) instanceof String) {
				table.setStatus((String) tableAsList.get(3));
			} else {
				System.out.println("Error: Index 3 is not a String!");
				return null;
			}
			
			tablesListAsTable.add(table);// Add the table to the list of tables as Table objects
		}

		return tablesListAsTable;
	}
		    
	/**
	* Finds the best fit table for a given number of diners from a list of tables.
	*
	* @param tables The list of available tables.
	* @param diners The number of diners to be accommodated.
	* @return The best fit Table object, or null if no suitable table is found.
	*/
	private Table findBestFitTable(ArrayList<Table> tables, int diners) 
	{
		Table bestFit = null;
		for (Table t : tables) // For each table in the list of tables
		{
			if (t.getSeatsNumber() >= diners) 
			{
			    // Check if this table is a better fit than the current best fit
			    if (bestFit == null || t.getSeatsNumber() < bestFit.getSeatsNumber()) 
			    {
			        bestFit = t;
			    }
			}
		}
		return bestFit;
	}
	
	/**
	* Sorts a list of TableReservation objects in descending order based on the
	* number of diners.
	*
	* @param reservations The list of TableReservation objects to be sorted.
	*/
	private void sortReservationsByDinersDescending(ArrayList<TableReservation> reservations) 
	{
		int n = reservations.size();
			for (int i = 0; i < n; i++) {
				for (int j = i + 1; j < n; j++) 
				{
				    // Compare number of diners and swap if necessary
				    if (reservations.get(i).getNumberOfDiners() < reservations.get(j).getNumberOfDiners()) 
				    {
				        TableReservation temp = reservations.get(i);
				        reservations.set(i, reservations.get(j));
				        reservations.set(j, temp);
				    }
				}
			}
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// Special methods//////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves all table reservations from the database.
	 *
	 * @param msg The message requesting all reservations.
	 * @return An ArrayList of TableReservation objects representing all
	 *         reservations in the database.
	 */
	private ArrayList<TableReservation> getAllReservationsByCustomerId(Message msg)
	{
		
	ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();//List to hold all reservations from the DB as a list of lists of objects
    	ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();//List to hold all reservations as TableReservation objects
    	
    	@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
    	int customerId=0;
    	// Setting customer ID from the list we got from the message content
    				if (list.get(0) instanceof Integer) {
    					 customerId = (int) list.get(0);
    				} else {
    					System.out.println("Error: Index 0 is not a Integer!");
    					return null;
    				}
    	
    	
    	allReservations = DBC.getAllReservationsQueryByCustomerId(customerId);//Get all reservations from the DB as a list of lists of objects
    	reservationsListAsTableRes=	getAllReservationsAsTableReservation(allReservations);//Convert the list of reservations from the DB into list of TableReservation objects

    	
    return reservationsListAsTableRes;//Return to server the list of reservations as TableReservation 
	}

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
			Customer customer = new Customer();
			
			if (list.get(1) instanceof String) 
			{
				customer.setPhoneNumber((String) list.get(1))  ;// Setting phone Number from the list we got from the message content
				if (list.get(2) instanceof String) 
				{
					customer.setEmail((String) list.get(2));// Setting email from the list we got from the message content
				}
				else if (list.get(2)==null)//if the email is null
                 {
                        customer.setEmail(null);
                 }
				else 
				{
					System.out.println("Error: Index 2 is not a String!");
					return null;
				}
			}
			else if (list.get(1)==null)//if the phone number is null
			{
				customer.setPhoneNumber(null);
				if (list.get(2) instanceof String) 
				{
					customer.setEmail((String) list.get(2))  ;//Setting email from the list we got from the message content
				}
				else 
				{
					System.out.println("Error: Index 2 is not a String!");
					return null;
				}
			}
			else 
			{
				System.out.println("Error: Index 1 is not a String!");
				return null;
			}

			//return customer ID from the DB . if customer not exists he created in the DB and return his ID else return his ID
			customerId=DBC.getCustomerId(customer);//Getting customer ID from the DB based on the phone number or email provided
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
		
		//Setting table ID to null at the beginning .chosen when the customer arrives at the restaurant
		
		int code=0;
        boolean exists=true;

        
        while (exists)
        {
        	code = 100000 + rand.nextInt(900000);//Generate a random 6-digit confirmation code
        	exists = DBC.checkIfConfCodeExistsInDB(code);//Check if the generated code already exists in the DB
        }

        newRes.setConfirmationCode(code);//Set the unique confirmation code to the reservation

		newRes.setCustomerId(customerId);//Setting customer ID to the reservation
	
		
		
		//date of make reservation is auto generated in the DB
		
        //arrival time and leaving time are set  as null at the beginning,changed when the customer arrives at the restaurant

		//status of reservation is auto generated in the DB

		if (DBC.createNewReservation(newRes))//Return that the reservation was created successfully in the DB
		{
			return newRes.getConfirmationCode();//Return to server the confirmation code of the new reservation);
		}
		
		return null;//Return null if the reservation was not created successfully in the DB
		
	}
	
	/**
	 * Checks table availability for a specific number of diners and reservation
	 * day.
	 *
	 * @param msg The message containing the reservation details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 : numberOfDiners (Integer), Location 1:
	 *            reservationDay (LocalDate)]
	 * @return An ArrayList of LocalTime objects representing available times for
	 *         the requested number of diners on the specified date, or null if no
	 *         times are available.
	 */
	private ArrayList<LocalTime> checkingTableAvailability(Message msg)
	{
		
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		
		ArrayList<LocalTime> availableTime = new ArrayList<>();//List to hold available dates and times for the requested number of diners and reservation date
		int numberOfDiners=0;
		LocalDate reservationDay=null;
	
		//Setting number of diners from the list we got from the message content
		if (list.get(0) instanceof Integer) {
				  numberOfDiners=(int) list.get(0);
		} else {
		    	  System.out.println("Error: Index 0 is not a Integer!");
		    	  return null;
		}
		    	
		//Setting reservation day from the list we got from the message content
		if (list.get(1) instanceof LocalDate) {
			reservationDay= (LocalDate) list.get(1);
		} else {
		    	  System.out.println("Error: Index 1 is not a LocalDate!");
		    	  return null;
		}
		
		ArrayList<TableReservation> allReservationsforSpecificDay = getAllReservationsByDay( reservationDay);
		
		ArrayList<Table> tables = getTableInRestaurant();
		if (tables==null) 
        {
            System.out.println("No tables found for " + numberOfDiners + " diners.");
            return null;
        }
		ArrayList<TimeSlot> OpeningTime = getOpeningTime(reservationDay);
		if (OpeningTime==null) 
        {
            System.out.println("The restaurant is closed on " + reservationDay.toString() );
            return null;
        }
		else 
		{
			
		    for (TimeSlot slot : OpeningTime) //For each time slot in the opening hours
		    {
		        LocalTime currentCheckTime = slot.getOpen();//Starting from the opening time of the slot

		        //Check every half hour within the time slot
		        while (!currentCheckTime.plusHours(2).isAfter(slot.getClose())) //While the current check time plus two hours (duration of the reservation) is not after the closing time of the slot
		        {
		            
		            LocalDateTime checkTime = LocalDateTime.of(reservationDay, currentCheckTime);//Combine the reservation date with the current check time to get the start time of the reservation
		            
		            ArrayList<TableReservation> overlappingRes = new ArrayList<>();
		            
		            // Check existing reservations to see if they overlap with the requested time
		            for (TableReservation res : allReservationsforSpecificDay) 
		            {
		            	    // Check only active or arrived reservations                        
		                if (res.getStatus().equalsIgnoreCase("active") || res.getStatus().equalsIgnoreCase("arrived")) 
		                {
		                    
		                    LocalDateTime existingResTime = res.getReservationDate().toLocalDateTime();// Get the start time of the existing reservation as LocalDateTime
		                    
		                    // Calculate the time difference in minutes between the existing reservation and the requested time
		                    long minutesBetween = Math.abs(java.time.Duration.between(existingResTime, checkTime).toMinutes());
		                    
		                    if (minutesBetween < 120) 
		                    {
			                    
		                    		overlappingRes.add(res);// Add the overlapping reservation to the list

		                    }
		                }
		            }
		            
		            //sort the overlapping reservations by number of diners in descending order
		            sortReservationsByDinersDescending(overlappingRes);
		            
		            ArrayList<Table> copyOfTables = new ArrayList<>(tables);// Create a copy of the tables in the restaurant for simulation
		            boolean canFitAllExistRes = true;
		            
		         //try to fit all existing  reservations into the available tables in the restaurant
		            for (TableReservation res : overlappingRes) 
		            {
		                Table bestFit = findBestFitTable(copyOfTables, res.getNumberOfDiners());// Find the best fit table for the existing reservation
		                if (bestFit != null) // If a suitable table is found
		                {
		                    copyOfTables.remove(bestFit);// Remove the table from the available tables in the simulation
		                } else 
		                {
		                    canFitAllExistRes = false; //no possibility to fit all existing reservations in the restaurant
		                    break;
		                }
		            }
		            
		         // If all existing reservations can be accommodated
		            if (canFitAllExistRes) 
		            {
		                Table tableForNewGuest = findBestFitTable(copyOfTables, numberOfDiners);// Find the best fit table for the new reservation request
		                if (tableForNewGuest != null) 
		                {
		                    availableTime.add(currentCheckTime);// Add the current check time to the list of available times
		                }
		            }
		            
		            // Move to the next half-hour slot
		            currentCheckTime = currentCheckTime.plusMinutes(30);
		        }
		    }
		    if (availableTime.isEmpty()) 
		    {
		        return null;
		    }
		    
		    return availableTime;//Return to server the list of available times in the requested date else return null      
		}      
	}          
		            
		            
	

}
