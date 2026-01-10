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
import data.WaitList;

public class BillController 
{
	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	/**
	 * Default constructor
	 */
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
	 * @param res The reservation ID for which to send the bill message.
	 */
	private static void sendBillMessage(TableReservation res) 
	{

		Bill bill=new Bill(); 
		bill.setReservationId(res.getReservationId());

		if (DBC.getBillDetails(bill)==null)//update the bill object with the details from the DB
		{
			System.out.println("Error: No bill found for reservation ID " + res.getReservationId());
			return;
		}

		calcBill(bill,res);// calculate the bill details

		Subscriber sub = new Subscriber();
		sub.setCustomerId( res.getCustomerId());
		if (!DBC.getCustomerByCustomerId(sub)) 
		{
			System.out.println("Error: could not find customer for reservation " + res.getConfirmationCode());
			return;
		}

		String formattedAmount = String.format("%.2f", bill.getTotalAmount());
		String formattedTotal = String.format("%.2f", bill.getTotalAmountAfterDiscount());

		int discountAsInt = (int) bill.getDiscountSize();


		if (sub.getType()!=null) 
		{

			EmailSendController.sendEmail(sub.getEmail(), "Your BistroNine bill is ready🧾", "Hi "+sub.getFirstName()+" "+sub.getLastName()+", Thank you for dining with us at Bistro9! It was a pleasure to host you.\r\n"
					+ "\r\n"
					+ "Attached is your bill summary:\r\n"
					+ "Amount before discount: "+formattedAmount+" ₪\r\n"
					+ "Discount: "+discountAsInt+" %\r\n"
					+ "Total to pay: "+formattedTotal+" ₪\r\n"
					+ "We look forward to seeing you again soon, Bistro9 Team 🍷");// Send email reminder to the customer
			SmsSendController.sendSms(sub.getPhoneNumber(), "Your BistroNine bill is ready🧾",
					"Hi " + sub.getFirstName() + " " + sub.getLastName() + ", Thank you for dining with us at Bistro9! It was a pleasure to host you.\r\n"
							+ "\r\n"
							+ "Attached is your bill summary:\r\n"
							+ "Amount before discount: " + formattedAmount + " ₪\r\n"
							+ "Discount: " + discountAsInt + " %\r\n"
							+ "Total to pay: " + formattedTotal + " ₪\r\n"
							+ "We look forward to seeing you again soon, Bistro9 Team 🍷");// Send sms reminder to the customer
		}
		else
		{
			EmailSendController.sendEmail(sub.getEmail(), "Your BistroNine bill is ready🧾", "Hi customer, Thank you for dining with us at Bistro9! It was a pleasure to host you.\r\n"
					+ "\r\n"
					+ "Attached is your bill summary:\r\n"
					+ "Amount before discount: "+formattedAmount+" ₪\r\n"
					+ "Discount: "+discountAsInt+" %\r\n"
					+ "Total to pay: "+formattedTotal+" ₪\r\n"
					+ "We look forward to seeing you again soon, Bistro9 Team 🍷");// Send email reminder to the customer
			SmsSendController.sendSms(sub.getPhoneNumber(), "Your BistroNine bill is ready🧾",
					"Hi customer, Thank you for dining with us at Bistro9! It was a pleasure to host you.\r\n"
							+ "\r\n"
							+ "Attached is your bill summary:\r\n"
							+ "Amount before discount: " + formattedAmount + " ₪\r\n"
							+ "Discount: " + discountAsInt + " %\r\n"
							+ "Total to pay: " + formattedTotal + " ₪\r\n"
							+ "We look forward to seeing you again soon, Bistro9 Team 🍷");// Send sms reminder to the customer
		}
	}

	/**
	 * Processes the payment of a bill.updates the bill as paid in the DB.
	 *
	 * @param bill The bill to be paid.
	 * @return true if the bill was paid successfully, false otherwise.
	 */
	public static boolean payBillProcess(Bill bill) 

	{

		bill.setPaid(true);// set bill as paid

		// payment method  allready set in the bill object


		//update the bill details(isPaid,PaymentMethod) in the DB return true if updated successfully else false
		return	DBC.payBillQuery(bill);
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


	/**
	 * Calculates the bill details for a given bill and table reservation.
	 *
	 * @param bill The bill to be calculated.
	 * @param res  The table reservation associated with the bill.
	 * @return true if the bill was calculated successfully, false otherwise.
	 */
	private static boolean calcBill(Bill bill, TableReservation res) 
	{
		//bill id allready set in the bill object

		//reservation id allready set in the bill object


		// random price generation for demonstration purposes
		double randomPrice = Math.random() * 500;
		bill.setTotalAmount(Math.round(randomPrice * 100.0) / 100.0);


		String type = CustomerController.getCustomerType(res.getCustomerId());

		if (type != null )
		{
			if (type.equals("subscriber") || type.equals("restaurant representative") || type.equals("restaurant manager")) 
			{
				type="subscriber";

			} else {
				type="customer";
			}
		}

		bill.setDiscountType(type); 

		// get discount size based on customer type
		bill.setDiscountSize(DBC.getDiscountQuery(type));

		// calculate final amount after discount
		bill.setTotalAmountAfterDiscount(calcFinalAmount(bill.getDiscountSize(), bill.getTotalAmount()));


		//update the bill amounts(TotalAmount,TotalAmountAfterDiscount,DiscountSize,DiscountType) in the DB return true if updated successfully else false
		if (!DBC.updateBillAmountsQuery(bill))
		{
			return false;
		}


		//is paid false by default

		//payment method null by default

		return true;
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

		Timestamp twoHoursAgo = new Timestamp(System.currentTimeMillis() - (2 * 60 * 60 * 1000));

		//check if customer has been arrived before 2 hours 
		if (res.getArrivalTime().before(twoHoursAgo))
		{
			return bill;
		}

		//else ,customer has not been seated for 2 hours yet , calculate the bill 

		calcBill(bill,res);// calculate the bill details

		return bill;
	}

	/**
	 * Processes the payment of a bill based on the bill ID provided in the message.
	 *!!!!!!call to this method after the method showBill
	 * @param msg The message containing the bill ID. The content of the message is
	 *            expected to be an ArrayList<Object> with the following order:
	 *            [Location 0 :bill ID (int), Location 1: payment method (String)]
	 * @return true if the bill was paid successfully, false otherwise.
	 */
	private boolean payBill(Message msg)
	{
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content;//get bill Id from the message

		Bill bill = new Bill();


		//Set bill Id in the Bill object
		if (list.get(0) instanceof Integer) 
		{
			bill.setBillId((int) list.get(0));
		} 
		else 
		{
			System.out.println("Error: Index 0 is not a Integer!");
			return false;
		}

		//Set payment method in the Bill object
		if (list.get(1) instanceof String) 
		{
			bill.setPaymentMethod((String) list.get(1));
		} else {
			System.out.println("Error: Index 1 is not a String!");
			return false;
		}


		if (!payBillProcess(bill))
		{
			return false;
		}

		int reservationId=DBC.getReservationIdByBillIdQuery(bill);//get reservation id by bill id
		if (reservationId == 0) 
		{
			return false;
		}


		//update reservation status to "completed" and set leaving time to now after payment is successful

		TableReservation res = new TableReservation();

		res.setReservationId(reservationId);

		if (!DBC.getReservationByReservationId(res))//update the reservation object with the details from the DB and return true if found else false
		{
			return  false;
		}

		ReservationControler.updateReservation(res,"status", "completed");
		Timestamp nowTime = java.sql.Timestamp.valueOf(LocalDateTime.now());
		ReservationControler.updateReservation(res,"leavingTime", nowTime);

		Table freeTable=new Table();
		freeTable.setTableId(res.getTableId());

		if (DBC.getTableByTableIdQuery(freeTable)==null)//update table data in table Object else return false
		{
			return false;
		}



		//find match in the waiting list for the freed table
		WaitList waiter = WaitListController.findMatchInWaitingList(freeTable);
		DBC.updateTableStatus(freeTable.getTableId(), "available");// set table status to available 
		if (waiter!=null)
		{
			//status of the table is already  occupied from the last customer that was seated from the waiting list
			TableReservation waiterRes=new TableReservation();
			waiterRes.setReservationId(waiter.getReservationId());
			if (!DBC.getReservationByReservationId(waiterRes))// update the reservation object with the details from the
				// DB and return true if found else false
			{
				System.out.println("Error: could not find reservation for waitlist entry " );
				return false;
			}

			ReservationControler.updateReservation(waiterRes, "reservationDate", new Timestamp(System.currentTimeMillis()));// set reservation date to now


			Subscriber sub = new Subscriber();
			sub.setCustomerId( waiterRes.getCustomerId());

			if (!DBC.getCustomerByCustomerId(sub))
			{
				System.out.println("Error: could not find customer for reservation " + res.getConfirmationCode());
				return false;
			}

			EmailSendController.sendEmail(sub.getEmail(), "Your table is ready 🍽️","Hey "+sub.getFirstName()+" "+sub.getLastName()+", good news! A table has become available for "+ waiterRes.getNumberOfDiners() +" diners at Bistro 9 .\r\n"
					+ "Looking forward to seeing you at the entrance!");// send email to the customer with all his confirmation codes for today

			SmsSendController.sendSms(sub.getPhoneNumber(), "Your table is ready 🍽️","Hey "+sub.getFirstName()+" "+sub.getLastName()+", good news! A table has become available for "+ waiterRes.getNumberOfDiners() +" diners at Bistro 9 .\r\n"
					+ "Looking forward to seeing you at the entrance!");
			return true;
		}

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

				if (minutesSeated >= 119) 
				{


					//check if the duration is 2 hours for sending the bill
					Bill checkBill = new Bill();
					checkBill.setReservationId(res.getReservationId());
					if (DBC.getBillByReservationId(checkBill)) //update the bill object with the details from the DB) 
					{
						// if the total amount is 0 , it means the bill has not been calculated yet
						if (checkBill.getTotalAmount() == 0) {
							sendBillMessage(res); 
						}
					}
				}
			}
		}
	}
}
