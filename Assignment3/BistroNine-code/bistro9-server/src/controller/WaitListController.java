package controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Random;

import data.Customer;
import data.Message;
import data.Table;
import data.TableReservation;
import data.WaitList;

public class WaitListController 
{
	

	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	
	public WaitListController() 
	{
		// TODO Auto-generated constructor stub
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
		case GET_IN_TO_WAIT_LIST:
			return getInToWaitList(msg);
		
		}
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////Helper methods//////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	////לבדוק אם יש הסתכלות על סטטוס הממתינים כשבודקים את מי להושיב בשולחן שהתפנה או פנוי
	
	public static ArrayList<WaitList> getAllWaitingAsWaitList(ArrayList<ArrayList<Object>> allallWaits)
	{
		ArrayList<WaitList> WaitsListAsWaitList = new ArrayList<>();

		for (ArrayList<Object> WaitAsList : allallWaits)
		{

			WaitList waitAsWaitList = new WaitList();

			//Set waiting_id in the WaitList object
			if (WaitAsList.get(0) instanceof Integer) 
			{
				waitAsWaitList.setWaiting_id((Integer) WaitAsList.get(0));
			} else {
				System.out.println("Error: Index 0 is not a Integer!");
				return null; 
			}

			//Set customer_id in the WaitList object
			if (WaitAsList.get(1) instanceof Integer) 
			{
				waitAsWaitList.setCustomer_id((Integer) WaitAsList.get(1));
			} else {
				System.out.println("Error: Index 1 is not a Integer!");
				return null; 
			}
			
			//Set numberOfDiners in the WaitList object
			if (WaitAsList.get(2) instanceof Integer) 
			{
				waitAsWaitList.setNumberOfDiners((Integer) WaitAsList.get(2));
			} else {
				System.out.println("Error: Index 2 is not a Integer!");
				return null; 
			}
			
			//Set  entry_time in the WaitList object
			if (WaitAsList.get(3) instanceof Timestamp) 
			{
				waitAsWaitList.setEntry_time((Timestamp) WaitAsList.get(3));
			} else {
				System.out.println("Error: Index 3 is not a Timestamp!");
				return null; 
			}
	
			//Set status in the WaitList object
			if (WaitAsList.get(4) instanceof  String ) 
			{
				waitAsWaitList.setStatus(( String ) WaitAsList.get(4));
			} else {
				System.out.println("Error: Index 4 is not a  String !");
				return null; 
			}

			
			WaitsListAsWaitList.add(waitAsWaitList);
		}
		return WaitsListAsWaitList;
	}

	public static boolean findMatchInWaitingList(int freeTable)
	{
	    
		ArrayList<ArrayList<Object>> allWaits = new ArrayList<>();
		
	    allWaits = DBC.getWaitingListQuery(); //return all Waiting List as ArrayList<WaitList> ,the first one on the list is the one that waited the longest. 
	    
	    if (allWaits == null || allWaits.isEmpty()) 
	    {
	        return false; 
	    }

	  //list of all people Waiting
	    ArrayList<WaitList> queue = getAllWaitingAsWaitList(allWaits);
	    
	    Table table=new Table();
	    table.setTableId(freeTable);
	    
	    if (DBC.getTableByTableIdQuery(table)==null)//update table data in table Object else return false
	    {
	    		return false;
	    }

	    for (WaitList waiter : queue) 
	    {
		    	if ( waiter.getStatus().equals("waiting"))
		    	{
		    		// בדיקה: האם השולחן שהתפנה מתאים לכמות הסועדים של הממתין?
		    		if (table.getSeatsNumber() >= waiter.getNumberOfDiners()) 
		    		{
	
		    			int luckyCustomerId = waiter.getCustomerId();
	
		    			DBC.updateStatusInWaitingListQuery(waiter);//change status to seated return true if sucss else false
	
		    			
		    			if (ReservationControler.createReservationWithWait(waiter.getNumberOfDiners(),waiter.getConfirmationCode(), waiter.getCustomerId()))
		    			{
		    				return false;
		    			}
	
		    			return true;
		    		}
		    	}
	    }

	    return false; // לא נמצא ממתין שמתאים לשולחן הספציפי הזה
	}
	
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//מחזיר קוד אישור אם נכנס לרשימת המתנה או מספר שולחן(עם מינוס) אם יש שולחן פנוי ונכנס ישר למסעדה
	private Integer getInToWaitList(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;
		
		WaitList newWait = new WaitList();//Creating a new reservation object
		
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
		
		//waitingId  AUTO_INCREMENT in DB
		
		newWait.setCustomerId(customerId);

		// Setting numberOfDiners from the list we got from the message content
		if (list.get(3) instanceof Integer) {
			newWait.setNumberOfDiners((int) list.get(3));
		} else {
			System.out.println("Error: Index 3 is not a Integer!");
			return null;
		}

        
        //entryTime DEFAULT CURRENT_TIMESTAMP in DB
		
		//status DEFAULT 'waiting', in DB
        
		
		//בדיקה האם יש שולחן פנוי ללקוח שהגע הרגע למסעדה 
		Table bestTable = TableController.findBestTableForNow(newWait.getNumberOfDiners());

        // 3. אם נמצא שולחן פנוי - יוצרים הזמנה במקום כניסה לרשימת המתנה
        if (bestTable != null) 
        {
        	//מחזירה אמת אם ההזמנה נוצרה והשולחן נתפס
        boolean result=ReservationControler.createReservationWithoutWait(bestTable.getTableId(),newWait.getNumberOfDiners(),newWait.getCustomerId());
            
            if (result)
            {
                return -bestTable.getTableId(); 
            }
            else
            {
            	return null;
            }
        }
        
        else//לא נמצא שולחן פנוי אז נכנסים לרשימת המתנה
        {
        
        	int code=0;
            boolean exists=true;

            
            while (exists)
            {
            		code = 100000 + rand.nextInt(900000); // יצירת קוד רנדומלי
                
                // בדיקה 1: האם קיים בטבלת ההזמנות?
                boolean existsInReservations = DBC.checkIfConfCodeExistsInDB(code);
                
                // בדיקה 2: האם קיים בטבלת ההמתנה? (צריך להוסיף את המתודה הזו ב-DBC)
                boolean existsInWaitingList = DBC.checkIfConfCodeExistsInWaitingList(code);//בודקקת אם הקוד אישור כבר קיים ברשימת ההמתנה
                
                // אם קיים באחד מהם - הדגל יהיה true והלולאה תרוץ שוב
                exists = existsInReservations || existsInWaitingList;
            }
    		
            newWait.setConfirmationCode(code);//Set the unique confirmation code 
        
        if (DBC.createNewWaitQuery(newWait))//Return that the Wait List was created successfully in the DB
		{
			return newWait.getConfirmationCode();//Return to server the confirmation code of the new reservation);
		}
		
		return null;//Return null if the reservation was not created successfully in the DB
        }
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Automated tasks//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
}
