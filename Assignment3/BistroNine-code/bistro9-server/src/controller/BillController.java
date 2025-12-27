package controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import data.Bill;
import data.Message;
import data.Subscriber;
import data.Table;
import data.TableReservation;

public class BillController 
{
	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	
	public BillController() 
	{
		
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////managing messages //////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Handles messages received from the server and performs actions based on the
	 * command type.
	 *
	 * @param msg The message received from the server.
	 * @return An object representing the result of the action performed based on
	 *         the command type.
	 */
	public Object handleMessageFromServer(Message msg) {

		switch (msg.command) // Checking the type of message sent from the server (what action should be
								// performed in the DB Controller)
		{
		case PAY_BILL:
			return payBill(msg);
			
		case SHOW_BILL:
			return showBill(msg);

		default:
			System.out.println("Unknown task received.");
			return null;
		}
	}
	
	
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////Helper methods//////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Sends a bill Automatic to the customer after dining for 2 hours.
	 *
	 * @param resId The reservation ID for which to send the bill message.
	 */
	private static void sendBillMessage(int resId) 
	{
		Bill bill=new Bill(); 
		bill.setReservationId(resId);

		DBC.getBillDetails(bill);//update the bill object with the details from the DB

		// Simulating sending the bill to the customer
		System.out.println("Hello, you have been dining with us for 2 hours. Here is your bill details:\n "+ "Total Amount: $" + bill.getTotalAmountAfterDiscount() + "\n" + "Please proceed to payment at your earliest convenience. Thank you! good day!" );

	}

	/**
	 * Creates a new bill for a given table reservation in DB.
	 *
	 * @param res The table reservation for which to create the bill.
	 * @return true if the bill was created successfully, false otherwise.
	 */
	public static boolean createNewBill(TableReservation res)
	{
		Bill bill = new Bill();
        
        // billId incremente otomatically in the DB
		
        bill.setReservationId(res.getReservationId());    
        
        // random price generation for demonstration purposes
        double randomPrice = Math.random() * 500;
        bill.setTotalAmount(Math.round(randomPrice * 100.0) / 100.0);
        
        // 
        String type = CustomerController.getCustomerType(res.getCustomerId());
        bill.setDiscountType(type);
        
        // 
        bill.setDiscountSize(DBC.getDiscountQuery(type));
        
        // י
        bill.setTotalAmountAfterDiscount(calcFinalAmount(bill.getDiscountSize(), bill.getTotalAmount()));
        
        //paymentMethod defaind in the payBill method when the customer pays the bill
        
        //isPaid  DEFAULT FALSE in the DB
        
        return DBC.createNewBillQuery(bill); //create the new bill in the DB return to server true if created successfully else false
    
	}
	
	/**
	 * Calculates the final amount after applying a discount.
	 *
	 * @param discountSize The size of the discount as a percentage.
	 * @param totalAmount  The total amount before the discount.
	 * @return The final amount after applying the discount, rounded to two decimal
	 *         places.
	 */
	private static double calcFinalAmount(double discountSize, double totalAmount) 
	{
        double discount = discountSize / 100.0;
        double discountValue = totalAmount * discount;
        double calculatedFinal = totalAmount - discountValue;
        return Math.round(calculatedFinal * 100.0) / 100.0;
    }
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves and displays the bill details based on the confirmation code
	 * provided in the message.
	 *
	 * @param msg The message containing the login details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 :confirmation code (int)]
	 * @return The Bill object with the retrieved bill details, or null if not
	 *         found.
	 */
	private Bill showBill(Message msg)
    {
        @SuppressWarnings("unchecked") 
        ArrayList<Object> list = (ArrayList<Object>) msg.content; 

        Bill bill = new Bill();
        int confCode = 0;
        
        // Set confirmation code
        if (list.get(0) instanceof Integer) {
            confCode = (int) list.get(0);
        } else {
            System.out.println("Error: Index 0 is not a Integer!");
            return null;
        }
        
        // find reservation by confirmation code
        TableReservation res = new TableReservation();
        res.setConfirmationCode(confCode);

        if (!DBC.getReservationsByConferenceCodeQuery(res)) //update the reservation object with the details from the DB and return true if found else false
        {
            System.out.println("Error: No reservation found with code " + confCode);
            return null;
        }
        
        // get bill by reservation ID
        bill.setReservationId(res.getReservationId());
        
        if (!DBC.getBillByReservationId(bill)) //update the bill object with the details from the DB and return true if found else false
        {
            System.out.println("Error: No bill found for reservation ID " + res.getReservationId());
            return null;
        }
        
        return bill;
    }
	
	/**
	 * Processes the payment of a bill based on the bill ID provided in the message.calls after the method showBill
	 *
	 * @param msg The message containing the bill ID. The content of the message is
	 *            expected to be an ArrayList<Object> with the following order:
	 *            [Location 0 :bill ID (int)]
	 * @return true if the bill was paid successfully, false otherwise.
	 */
	private boolean payBill(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get bill Id from the message

		int billId = 0;
		
		//Set bill Id in the Bill object
		if (list.get(0) instanceof Integer) 
		{
			billId=(int) list.get(0);
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a Integer!");
    	    		return false;
		}

		
		int reservationId = DBC.payBill(billId,true,"credit");//return reservation Id if the bill is paid successfully (chancg to isPaid=true,paymentMethod=Credit ) in the DB else return 0
		if (reservationId == 0) {
	        return false;
	    }
		

			//update the table status to "available" after paying the bill
			
			TableReservation res = new TableReservation();
			res.setReservationId(reservationId);
		
			if (!DBC.getReservationByReservationId(res))//update the reservation object with the details from the DB and return true if found else false
            {
                return  false;
            }

             ReservationControler.updateReservation(reservationId,"status", "completed");
             Timestamp nowTime = java.sql.Timestamp.valueOf(LocalDateTime.now());
             ReservationControler.updateReservation(reservationId,"leavingTime", nowTime);
                		
             TableController.updateTable(res.getTableId(),"status", "available");
             
             WaitListController.findMatchInWaitingList(res.getTableId());

             return true;
          
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////Automated tasks//////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Sends bill messages to customers who have been seated for 2 hours. This
	 * method checks today's reservations and sends bill messages to customers whose
	 * reservation status is "arrived" and have been seated for 2 hours.
	 */
	public static void  sendBillReservation()
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
