package controller;

import java.time.LocalDate;
import java.util.ArrayList;

import data.Message;
import data.SubscriberReport;



public class SubscriberReportController 
{
	private static DataBaseController DBC=DataBaseController.getInstance();//Interfacing with the DB Controller
	/**
	 * Default constructor
	 */
	public SubscriberReportController() 
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
		case GET_SUBSCRIBER_REPORT_BY_RANGE_DATE:
			return getSubscriberReportByRangeDate( msg);

		default:
			System.out.println("Unknown task received.");
			return null;
		}
	}	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////Logic methods//////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a subscriber report for a specified date range from the database.
	 *
	 * @param msg The message containing the login details. The content of the
	 *            message is expected to be an ArrayList<Object> with the following
	 *            order: [Location 0 : LocalDate startDay(YYYY-MM-01), Location 1 : LocalDate endDay(YYYY-MM-28/29/30/31)]
	 * @return A SubscriberReport object containing the report data, or null if an
	 *         error occurred.
	 */
	private SubscriberReport getSubscriberReportByRangeDate(Message msg)
	{	
		@SuppressWarnings("unchecked") 
		ArrayList<Object> list = (ArrayList<Object>) msg.content; 

		SubscriberReport report = new SubscriberReport();

		// Set start Day in report object
		if (list.get(0) instanceof LocalDate) 
		{
			report.setStartDay((LocalDate) list.get(0));
		} else {
			System.out.println("Error: Index 0 is not a LocalDate!");
			return null;
		}

		// Set end Day in report object
		if (list.get(1) instanceof LocalDate) 
		{
			report.setEndDay((LocalDate) list.get(1));
		} else {
			System.out.println("Error: Index 1 is not a LocalDate!");
			return null;
		}

		if (!DBC.getSubscriberReportByRangeDateQuery(report)) // updating the report object with the data from the DB return false if an error occurred or true if success
		{
			System.out.println("Error retrieving time report from the database.");
			return null;
		} 
		else 
		{
			return report;
		}

	}



	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////Automated tasks//////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Generates a monthly subscriber report on the 1st day of each month. The
	 * report includes total reservations and total waiting counts for each day of
	 * the previous month. The report is then added to the database. if the bistro
	 * was closed on a specific day, the totals for that day will be recorded as 0.
	 * if the current day is not the 1st of the month, the method exits without generating a report.
	 * if an error occurs while retrieving data for any day, an error message is printed,the totals for that day will be recorded as -1.
	 */
	public static void  subscriberReportGenerate()
	{LocalDate today = LocalDate.now();

	SubscriberReport report = new SubscriberReport();

	//calculate first and last day of last month
	LocalDate lastMonth = today.minusMonths(1);//last month
	int daysInMonth = lastMonth.lengthOfMonth();//amount of days in last month (28/29/30/31)

	LocalDate startOfMonth = lastMonth.withDayOfMonth(1);//first day in the month (1)
	LocalDate endOfMonth = lastMonth.withDayOfMonth(daysInMonth); // last day in the month (28/29/30/31)

	// Check if a report for this specific range and type already exists in the database.
	// return true if yes else false
	if (DBC.checkReportExistsQuery(startOfMonth, endOfMonth, "subscriber")) 
	{
		return;
	}


	report.setStartDay(startOfMonth);
	report.setEndDay(endOfMonth);

	//generatedAt DEFAULT CURRENT_TIMESTAMP in DB 

	report.setReportRange("monthly");
	report.setReportType("subscriber");


	for (int i = 1; i <= daysInMonth; i++) 
	{

		LocalDate currentDate = lastMonth.withDayOfMonth(i);

		int totalReservations = DBC.getDailyTotalReservationsQuery(currentDate);//return the total Reservations amount (just in status completed) for the day else -1 if an error occurred if bistro close return 0

		if (totalReservations == -1) {
			System.out.println("Error retrieving total Reservations for date: " + currentDate);
		}

		int totalWaiting = DBC.getDailyTotalWaitingQuery(currentDate);//return the total Waiting amount (just in status seated) for the day else -1 if an error occurred if bistro close return 0

		if (totalWaiting == -1) 
		{
			System.out.println("Error retrieving total Waiting for date: " + currentDate);

		}

		report.addRow(currentDate, totalReservations, totalWaiting);
	}

	if (!DBC.addSubscriberReportQuery(report)) //adding the report to the DB in report_manager and subscriber_report return false if an error occurred or true if success
	{
		System.out.println("Error adding subscriber report to the database.");
	} else {
		System.out.println("subscriber report successfully added to the database.");
	}

	}
}
