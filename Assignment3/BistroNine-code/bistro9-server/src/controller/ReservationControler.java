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
import data.Subscriber;
import data.Table;
import data.TableReservation;
import data.TimeSlot;

public class ReservationControler 
{

	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	
	/**
	 * Default constructor
	 */
	public ReservationControler() 
	{
	
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////managing messages //////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
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
	    case GET_ALL_RESERVATIONS_BY_CUSTOMER:
	    	return getAllReservationsByCustomerId(msg);
	    	
	    case GET_ALL_RESERVATIONS:
	    	return getAllReservations();
	    	
	    case CREATE_NEW_RESERVATION:
	    	return createNewReservation(msg);
	    		
	    case CHECK_TABLE_AVAILABILITY:
	    	return checkingTableAvailability(msg);
	    	
	    case DELETE_RESERVATION:
	    	return deleteReservation(msg);
	    	
	    	case GET_ALL_RESERVATIONS_ACTIVE:
	    		return 	getAllReservationsActive();
	    		
		case GET_ALL_DINERS_AT_RESTAURANT:
				return getAllDinersAtRestaurant(msg);
		
	    default:
	        System.out.println("Unknown task received.");
	        return null;
		}
	}

	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////Helper methods//////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
	/**
	 * Creates a new table reservation with waitlist in the database when the
	 * customer needs to wait for a table.
	 *
	 * @param numberOfDiners The number of diners for the reservation.
	 * @param customerId     The ID of the customer making the reservation.
	 * @return The TableReservation object if the reservation was created
	 *         successfully, null otherwise.
	 */
	public static 	TableReservation createReservationWithWait( int numberOfDiners, int customerId )
	{
		Random rand = new Random();//Random object to generate a random confirmation code
		TableReservation newRes = new TableReservation();//Creating a new reservation object
		
		//reservationId giveing by DB auto increment
		
		// table ID set when the customer is get in to the restaurant
		newRes.setNumberOfDiners(numberOfDiners);//Setting number of diners to the reservation
		int code=0;
        boolean exists=true;

        while (exists)
        {
        		code = 100000 + rand.nextInt(900000); 

            boolean existsInReservations = DBC.checkIfConfCodeExistsInDB(code);

            exists = existsInReservations ;
        }

        newRes.setConfirmationCode(code);//Set the unique confirmation code to the reservation
        newRes.setCustomerId(customerId);//Setting customer ID to the reservation
        
        newRes.setReservationDate(new Timestamp(System.currentTimeMillis()));//Setting reservation date to the reservation
		

		//DateOfMakeReservation giveing by DB auto CURRENT_TIMESTAMP
        

		//ArrivalTime and leavingTime not set at the beginning,changed when the customer get into and leaves the restaurant
		
        newRes.setStatus("waiting");//Setting status to the reservation to "active" because the customer did not arrive yet
		
		if (DBC.createNewReservation(newRes))//Return that the reservation was created successfully in the DB
		{
			
			return newRes;//Return the confirmation code of the new reservation
			//No need to update table status because the table is not assigned yet
			//no need to create bill because the customer did not arrive yet
				
            
		}
		return null;//Return false if the reservation was not created successfully in the DB
	}
	
	/**
	 * Creates a new table reservation without waitlist in the database when the
	 * customer arrives at the restaurant immediately
	 * .occupies  table immediately for the reservation.
	 * .creates a bill for the reservation.
	 *
	 * @param tableId        The ID of the table to be reserved.
	 * @param numberOfDiners The number of diners for the reservation.
	 * @param customerId     The ID of the customer making the reservation.
	 * @param status         The status of the reservation.
	 * @return true if the reservation was created successfully, false otherwise.
	 */
	public static boolean createReservationWithoutWait(int tableId, int numberOfDiners, int customerId )
	{
		Random rand = new Random();//Random object to generate a random confirmation code
		TableReservation newRes = new TableReservation();//Creating a new reservation object
		
		//reservationId giveing by DB auto increment
		newRes.setTableId(tableId);//Setting table ID to the reservation
		newRes.setNumberOfDiners(numberOfDiners);//Setting number of diners to the reservation
		
		
		int code=0;
        boolean exists=true;

        
        while (exists)
        {
        	code = 100000 + rand.nextInt(900000);//Generate a random 6-digit confirmation code
        	exists = DBC.checkIfConfCodeExistsInDB(code);//Check if the generated code already exists in the DB
        }

        newRes.setConfirmationCode(code);//Set the unique confirmation code to the reservation
        newRes.setCustomerId(customerId);//Setting customer ID to the reservation
        
        newRes.setReservationDate(new Timestamp(System.currentTimeMillis()));//Setting reservation date to the reservation
		

		//DateOfMakeReservation giveing by DB auto CURRENT_TIMESTAMP
        
       
        newRes.setArrivalTime(new Timestamp(System.currentTimeMillis()));
        
        
		
		//leavingTime not set at the beginning,changed when the customer leaves the restaurant
		
        newRes.setStatus("arrived");//Setting status to the reservation to "arrived" because the customer is already at the restaurant
		
		if (DBC.createNewReservation(newRes))//Return that the reservation was created successfully in the DB
		{
			if (TableController.updateTable(newRes.getTableId(),"status", "occupied"))//Update the status of the table to "occupied" in the DB
            {
			
				if (BillController.createNewBill(newRes))
				{
					return true;//Return to server the confirmation code of the new reservation);
				}
				else
				{
					return false;//Return false if bill was not created successfully in the DB
				}
            }
			else
            {
                return false;//Return false if table status was not updated successfully in the DB
            }
		}
		return false;//Return false if the reservation was not created successfully in the DB
		
	}
	
	
	/**
	 * Converts a list of reservations from the database into TableReservation
	 * objects.
	 *
	 * @param allReservations A list of reservations from the database, where each
	 *                        reservation is represented as a list of objects.
	 * @return An ArrayList of TableReservation objects representing the
	 *         reservations.
	 */
	public static ArrayList<TableReservation> getAllReservationsAsTableReservation(ArrayList<ArrayList<Object>> allReservations)
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
            }
            else if (resAsList.get(7) == null) // if arrival time is null
            {
                resAsTableRes.setArrivalTime(null);
            }
            else {
                System.out.println("Error: Index 7 is not a Timestamp!");
                return null;
            }
            
          //Set leaving Time in the TableReservation object
            if (resAsList.get(8) instanceof Timestamp) {
                resAsTableRes.setLeavingTime((Timestamp) resAsList.get(8));
            }
			else if (resAsList.get(8) == null) // if leaving time is null
			{
				resAsTableRes.setLeavingTime(null);
			}
            else {
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
	public static ArrayList<TableReservation> getAllReservationsByDay(LocalDate day, boolean includePreviousDay)
	{
		ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();//List to hold all reservations from the DB as a list of lists of objects
		
	    
	    if (includePreviousDay)
	    {
	    		ArrayList<ArrayList<Object>> allReservationsThisDay =DBC.getAllReservationsQueryByDay(day);
	    		ArrayList<ArrayList<Object>> allReservationsPreviousDay = DBC.getAllReservationsQueryByDay(day.minusDays(1));
	    		
	    		if (allReservationsPreviousDay != null) 
	    		{
	    			allReservations.addAll(allReservationsPreviousDay);
	    		}

	     	if (allReservationsThisDay != null) 
	     	{
	    			allReservations.addAll(allReservationsThisDay);
	    		}
	    		
	    }
	    else 
	    {
	        
	    		allReservations = DBC.getAllReservationsQueryByDay(day);
	    }

		ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();//List to hold all reservations as TableReservation objects
			
	    
	    	reservationsListAsTableRes=	getAllReservationsAsTableReservation(allReservations);//Convert the list of reservations from the DB into list of TableReservation objects
	    	if (reservationsListAsTableRes == null) 
	    	{
	    		                System.out.println("error return all reservayion by day" + day.toString());
	    		                return null;
	    	
	    	}
	    	
	    	if (includePreviousDay)
		 {
	    		ArrayList<TableReservation> filtered = new ArrayList<>();

	            LocalDateTime startOfToday = day.atStartOfDay();

	            for (TableReservation res : reservationsListAsTableRes) {
	                
	                LocalDateTime resEnd = res.getReservationDate().toLocalDateTime().plusHours(2);
	                
	                
	                if (resEnd.isAfter(startOfToday)) {
	                    filtered.add(res);
	                }
	            }
	            return filtered;
	        }
		    
	    	
	    	
	    	
	    return reservationsListAsTableRes;
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
	public static void sortReservationsByDinersDescending(ArrayList<TableReservation> reservations) 
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

	
	/**
	 * Updates a specific column of a reservation in the database.
	 *
	 * @param reservationId The ID of the reservation to be updated.
	 * @param columnName    The name of the column to be updated.
	 * @param newValue      The new value to be set for the specified column.
	 * @return true if the update was successful, false otherwise.
	 */
	public static boolean updateReservation(int reservationId, String columnName, Object newValue) 
	{
		
		if (columnName.equals("status")) 
        {
			if (newValue instanceof String) 
			{
				return DBC.updateReservationStatus(reservationId, (String) newValue);
			} 
			else 
			{
				System.out.println("Error: newValue is not a String!");
				return false;
			}
        }
		else if (columnName.equals("leavingTime")) 
        {
            if (newValue instanceof Timestamp) 
            {
                return DBC.updateReservationLeavingTime(reservationId, (Timestamp) newValue);
            } 
            else 
            {
                System.out.println("Error: newValue is not a Timestamp!");
                return false;
            }
        }
		return false;
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Retrieves all table reservations from the database.
	 *
	 * 
	 * @return An ArrayList of TableReservation objects representing all
	 *         reservations in the database.
	 */
	private ArrayList<TableReservation> getAllReservations()
    {
        ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();//List to hold all reservations from the DB as a list of lists of objects
        ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();//List to hold all reservations as TableReservation objects
        
        allReservations = DBC.getAllReservationsQuery();//Get all reservations from the DB as a list of lists of objects
        reservationsListAsTableRes=	getAllReservationsAsTableReservation(allReservations);//Convert the list of reservations from the DB into list of TableReservation objects

        return reservationsListAsTableRes;//Return to server the list of reservations as TableReservation 
    }
	
	/**
	 * Retrieves all active,arrived table reservations from the database.
	 *
	 * 
	 * @return An ArrayList of TableReservation objects representing all active
	 *         reservations in the database.
	 */
	public static  ArrayList<TableReservation> getAllReservationsActive()
	{
		ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();//List to hold all reservations from the DB as a list of lists of objects
		ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();//List to hold all reservations as TableReservation objects
		
		allReservations = DBC.getAllReservationsActiveQuery();//Get all reservations active,arrived from the DB as a list of lists of objects
		reservationsListAsTableRes=	getAllReservationsAsTableReservation(allReservations);//Convert the list of reservations from the DB into list of TableReservation objects

		return reservationsListAsTableRes;//Return to server the list of reservations as TableReservation 
	}
	
	/**
	 * Retrieves all diners currently at the restaurant.
	 *
	 * @param msg The message not containing any specific details.
	 * @return An ArrayList of Customer objects representing all diners at the
	 *         restaurant.
	 */
	private ArrayList<Customer> getAllDinersAtRestaurant(Message msg)
	{
		ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();//List to hold all reservations from the DB as a list of lists of objects
		ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();//List to hold all reservations as TableReservation objects
		ArrayList<Customer> subscribersAtRestaurant = new ArrayList<>();//List to hold all subscribers at the restaurant
		
		allReservations = DBC.getAllReservationsQueryByDay(LocalDate.now());//Get all reservations of today from the DB as a list of lists of objects
		reservationsListAsTableRes=	getAllReservationsAsTableReservation(allReservations);//Convert the list of reservations from the DB into list of TableReservation objects
		
		for (TableReservation res : reservationsListAsTableRes) 
		{
			if (res.getStatus().equals("arrived")) 
			{
				Subscriber sub=new Subscriber();

				sub.setCustomerId(res.getCustomerId());
				DBC.getCustomerByCustomerId(sub);//update subscriber/customer details in Subscriber object from the DB based on customer ID in the reservation
				
				if (sub.getUsername() == null) //if the subscriber is actually a regular customer
				{
	                Customer cust = new Customer();
	                cust.setCustomerId(sub.getCustomerId());
	                cust.setPhoneNumber(sub.getPhoneNumber());
	                cust.setEmail(sub.getEmail());
	                
	                subscribersAtRestaurant.add(cust);
	            } else {
	                // זה מנוי אמיתי - נוסיף אותו כמו שהוא
	            	subscribersAtRestaurant.add(sub);
	            } 
			}
		}
		return subscribersAtRestaurant;

	}
	
	/**
	 * Retrieves all table reservations for a specific customer from the database.
	 *
	 * @param msg The message containing the customer details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 :typeCustomer (String), if
	 *            customer Location 1,2: phone number(String) and/or email(String) if 
	 *            subscribers Location 1:customerId ]
	 * 
	 * @return An ArrayList of TableReservation objects representing all
	 *         reservations for the specified customer.
	 */
	private ArrayList<TableReservation> getAllReservationsByCustomerId(Message msg)
	{
		
	ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();//List to hold all reservations from the DB as a list of lists of objects
    	ArrayList<TableReservation> reservationsListAsTableRes = new ArrayList<>();//List to hold all reservations as TableReservation objects
    	
    	@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
    	int customerId=0;
    	
    	String typeCustomer= new String();
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
		
		ArrayList<TableReservation> allReservationsforSpecificDay = getAllReservationsByDay( reservationDay,true);//Get all reservations for the specific date from the DB);
		if (allReservationsforSpecificDay == null) {
			System.out.println("error return all reservayion" + reservationDay.toString());
			return null;
		}
		ArrayList<Table> tables = TableController.getTableInRestaurant();
		if (tables==null) 
        {
            System.out.println("No tables found for " + numberOfDiners + " diners.");
            return null;
        }
		ArrayList<TimeSlot> OpeningTime = OpeningTimeController .getOpeningTime(reservationDay);
		if (OpeningTime==null) 
        {
            System.out.println("The restaurant is closed on " + reservationDay.toString() );
            return null;
        }
		else 
		{
			
		    for (TimeSlot slot : OpeningTime) //For each time slot in the opening hours
		    {

		        LocalDateTime startDateTime = LocalDateTime.of(reservationDay, slot.getOpen());// Get the start time of the slot as LocalDateTime
		        LocalDateTime endDateTime = LocalDateTime.of(reservationDay, slot.getClose());// Get the end time of the slot as LocalDateTime
		        
		        //If the closing time is past midnight, adjust the endDateTime to the next day
		        if (endDateTime.isBefore(startDateTime)) 
		        {
		            endDateTime = endDateTime.plusDays(1);
		        }
		        
		        LocalDateTime currentCheckDateTime = startDateTime;// Initialize the current check time to the start of the slot
		        
		        //Check every half hour within the time slot
		        while (!currentCheckDateTime.plusHours(2).isAfter(endDateTime)) //While the current check time plus two hours (duration of the reservation) is not after the closing time of the slot
		        {
		            
		           
		            
		            ArrayList<TableReservation> overlappingRes = new ArrayList<>();//List to hold overlapping reservations with the requested time
		            
		            // Check existing reservations to see if they overlap with the requested time
		            for (TableReservation res : allReservationsforSpecificDay) 
		            {
		            	    // Check only active or arrived reservations                        
		                if (res.getStatus().equalsIgnoreCase("active") || res.getStatus().equalsIgnoreCase("arrived")) 
		                {
		                    
		                    LocalDateTime existingResTime = res.getReservationDate().toLocalDateTime();// Get the start time of the existing reservation as LocalDateTime
		                    
		                    // Calculate the time difference in minutes between the existing reservation and the requested time
		                    long minutesBetween = Math.abs(java.time.Duration.between(existingResTime,currentCheckDateTime).toMinutes());
		                    
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
		                    availableTime.add(currentCheckDateTime.toLocalTime());// Add the current check time to the list of available times
		                }
		            }
		            
		            // Move to the next half-hour slot
		            currentCheckDateTime = currentCheckDateTime.plusMinutes(30);
		        }
		    }
		    if (availableTime.isEmpty()) 
		    {
		        return null;
		    }
		    
		    return availableTime;//Return to server the list of available times in the requested date else return null      
		}      
	}          
	
	/**
	 * Deletes a table reservation from the DB based on the confirmation code.
	 *
	 * @param msg The message containing the confirmation code of the reservation to
	 *            be deleted. The content of the message is expected to be an
	 *            ArrayList<Object> with the following order: [Location 0 :
	 *            confirmationCode (Integer)]
	 * @return true if the reservation was deleted successfully, false otherwise.
	 */
	private 	boolean  deleteReservation(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get the reservation details from the message content
		int confirmationCode=0;
	
		//Setting confirmation code from the list we got from the message content
		if (list.get(0) instanceof Integer) {
				confirmationCode=(int) list.get(0);
		} else {
		    	  System.out.println("Error: Index 0 is not a Integer!");
		    	  return false;
		}
		//d1
		//return true if the reservation was deleted successfully from the DB or false otherwise
		return DBC.deleteReservationByConfCode(confirmationCode);//Return to server the result of the deletion operation
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Automated tasks//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Deletes late reservations from the DB. A reservation is considered late
	 * if the customer has not arrived within 15 minutes of the reservation time.
	 * This method checks today's reservations and updates the status of late
	 * reservations to "canceled".
	 * this method is called periodically by schedulere to run every minute at the server.
	 */
	//האם לשלוח עדכון למשתמש שההזמנה בוטלה?
	public 	void  deleteLateReservations()
	{
		//get today reservations
	    LocalDate today = LocalDate.now();
	    ArrayList<TableReservation> todayReservations = getAllReservationsByDay(today, false);
	    //if there are no reservations for today, exit the method
	    if (todayReservations == null || todayReservations.isEmpty()) {
	        return;
	    }

	    LocalDateTime nowTime = LocalDateTime.now();

	    // Check each reservation and cancel if the customer is late
	    for (TableReservation res : todayReservations) 
	    {
	        if (res.getStatus().equals("active")) // check active reservations
	        {
	            LocalDateTime resTime = res.getReservationDate().toLocalDateTime();// Get the reservation time as LocalDateTime
	            
	            // If the current time is more than 15 minutes past the reservation time
	            if (resTime.plusMinutes(15).isBefore(nowTime)) 
	            {
	                // Update the reservation status to "canceled" in the DB
	                //d2
	            	DBC.updateReservationStatus(res.getConfirmationCode(), "cancelled");
	                System.out.println("Reservation " + res.getConfirmationCode() + " was auto-canceled due to late arrival.");
	            }
	        }
	    }
		
	}
	
	/**
	 * Sends email to customers for upcoming reservations. This method
	 * checks today's reservations and sends reminders to customers whose
	 * reservations are scheduled to start in 2 hours. this method is called
	 * periodically by schedulere to run every minute at the server.
	 */
	public void sendReminderAlertsForReservation() 
	{
	    LocalDate today = LocalDate.now();
	    ArrayList<TableReservation> todaysRes = getAllReservationsByDay(today,false);//get today reservations
	    
	    if (todaysRes == null) return;

	    LocalDateTime nowTime = LocalDateTime.now();//get current time

	    for (TableReservation res : todaysRes) 
	    {
	        
	        if (res.getStatus().equals("active")) 
	        {
	            LocalDateTime resTime = res.getReservationDate().toLocalDateTime();// Get the reservation time as LocalDateTime
	            
	            // Calculate the time difference in minutes between now and the reservation time
	            long minutesUntilRes = java.time.Duration.between(nowTime, resTime).toMinutes();
	            
	            // If the reservation is 2 hours away
	            if (minutesUntilRes >= 119 && minutesUntilRes <= 120) 
	            {
	            	Subscriber sub = new Subscriber();
	            	sub.setCustomerId( res.getCustomerId());
	            	
	            	if (!DBC.getCustomerByCustomerId(sub))
	            	{
						System.out.println("Error: could not find customer for reservation " + res.getConfirmationCode());
						return;
	            	}

	            		EmailSendController.sendEmail(sub.getEmail(), "bistro9 is waiting for you! 🎉🥂🎉", "Hi"+ sub.getFirstName()+ " " +sub.getLastName() + ", we just wanted to remind you that your table for "+res.getNumberOfDiners()+" diners at Bistro9 is reserved for today in 2 hours ("+res.getReservationDate()+").\r\n"
	            				+ "The restaurant staff is already preparing for you.\r\n"
	            				+ "We look forward to seeing you and wish you an enjoyable meal.");// Send email reminder to the customer
	            		SmsSendController.sendSms(sub.getPhoneNumber(), "bistro9 is waiting for you! 🎉🥂🎉","Hi"+ sub.getFirstName()+ " " +sub.getLastName() + ", we just wanted to remind you that your table for "+res.getNumberOfDiners()+" diners at Bistro9 is reserved for today in 2 hours ("+res.getReservationDate()+").\r\n"
	            				+ "The restaurant staff is already preparing for you.\r\n"
	            				+ "We look forward to seeing you and wish you an enjoyable meal.");// Send sms reminder to the customer
	            
	            }
	        }
	    }
	}
	
}
